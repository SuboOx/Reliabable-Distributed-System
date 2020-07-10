import java.net.*;
import java.io.*;

public class Server {
    /*Database and protocol, currently very naive*/
    private static DataBase db = new DataBase();
    private static int serverID;

    /*Each client will be served by a thread in server, all of the threads shares database and protocol*/
    static class ServerThread extends Thread {
        protected Socket clientSocket;

        public ServerThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            //TODO: Maybe send a receipt
            BufferedReader in = null;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Unable to create buffer");
                return;
            }

            String inputLine;

            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        parseResult parsed = Protocol.serverUnpack(inputLine);
                        if (parsed.serverID != serverID) {
                            System.err.println("The message have been sent to the wrong server!");
                            System.exit(1);
                        }
                        System.out.println("Received msg from client " + parsed.clientID + ":" + inputLine);
                        System.out.println("Unpacked msg: " + parsed.var + "=" + parsed.value + ", id: " + parsed.clientID + ":req" + parsed.reqID);
                        db.setVariable(parsed.var, parsed.value);
                        String respond = Protocol.serverPack(db.toString(), parsed.clientID, parsed.serverID, parsed.reqID);
                        out.println(respond);
                        //print database
                        System.out.println("Current database:");
                        System.out.println(db.toString());
                    } else {
                        clientSocket.close();
                        return;
                    }
                } catch (IOException e) {
                    System.err.println("Unable to read line.");
                    return;
                }
            }

        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Server <Server id> <port number> <primary/replica(optional)>");
            System.err.println("                   When no <primary/replica(optional)> is given, works in active replication mode.");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[1]);
        serverID = Integer.parseInt(args[0]);
        int chkptInterval = 10000;// Default Checkpoint interval
        String passiveReplicationMode = "OFF";

        System.out.println("<----Server " + serverID + " started on port " + portNumber + "---->");
        //If the server is the primary one
        if (args.length > 2) {
            if (args[2].equals("primary")) {
                if (args.length == 4) {
                    chkptInterval = Integer.parseInt(args[3]);
                }
                System.out.println("Server works as primary, checkpoint interval: " + chkptInterval + " ms");
                passiveReplicationMode = "PRIMARY";
            } else if (args[2].equals("replica")) {
                System.out.println("Server works as replica.");
                passiveReplicationMode = "REPLICA";
            } else {
                System.err.println("Argument 3 is either primary or replica, default: active replication.");
                System.exit(1);
            }
        } else {
            System.out.println("Server works in active replication mode.");
        }

        /*
         * TODO: use passiveReplicationMode
         * */

        /*Database and protocol here*/
        System.out.println("Initializing database and protocol...");
        System.out.println("Done.");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        while (true) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
            // new thread for a client
            new ServerThread(clientSocket).start();
        }
    }
}