import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server {
    /*Database and protocol, currently very naive*/
    private static DataBase db = new DataBase();
    private static int serverID;
    private static ArrayList<String> logging = new ArrayList<>();

    /*Each client will be served by a thread in server, all of the threads shares database and protocol*/
    static class ServerThread extends Thread {
        protected Socket clientSocket;
        protected boolean isBackup;

        public ServerThread(Socket clientSocket,boolean isBackup) {
            this.isBackup = isBackup;
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

                        //if this server is a backup
                        if(isBackup){
                            if(ischeckponit(inputLine)) {
                                parseResult parsed = Protocol.checkpointUnpack();
                                db.update(parsed.db);
                            } else {
                                logging.add(inputLine);
                            }
                            return;
                        }

                        //if the server is not a backup
                        parseResult parsed = Protocol.serverUnpack(inputLine);
                        if(parsed.serverID != serverID){
                            System.err.println("The message have been sent to the wrong server!");
                            System.exit(1);
                        }
                        System.out.println("Received msg from client " + parsed.clientID + ":" + inputLine);
                        System.out.println("Unpacked msg: " + parsed.var + "=" + parsed.value + ", id: " + parsed.clientID + ":req" + parsed.reqID);
                        db.setVariable(parsed.var, parsed.value);
                        String respond = Protocol.serverPack(db.toString(),parsed.clientID,parsed.serverID,parsed.reqID);
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


    static class CheckpointThread extends Thread {
        protected int backupServerID;

        public CheckpointThread(int serverID) {
            this.backupServerID = serverID;
        }

        @Override
        public void run() {
            String checkpoint;
            try (Socket kkSocket = new Socket(serverConstant.serverHostname[backupServerID], serverConstant.portNumber[backupServerID]);
                 PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {

                //use synchronized to implement quiescence
                synchronized (db){
                    String msg2send = Protocol.checkpointPack(serverID, backupServerID, -1, db);
                    if (msg2send != null) {
                        System.out.println("Sending checkpoint msg to " + backupServerID);
                        out.println(msg2send);
                    } else {
                        System.out.println("Illegal input, input should be var=value");
                        return;
                    }
                }
            } catch (UnknownHostException e) {
                System.err.println("Unknown host for server " + backupServerID);

            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + serverConstant.serverHostname[backupServerID] + " : " + serverConstant.portNumber[backupServerID]);
            }
        }
    }

    public static void main(String[] args) {
        boolean isPassive = false;
        boolean isBackup = false;
        int ckpt_freq = 4000;
        int ckpt_timer = 0;
        if (args.length != 2 || args.length != 3 || args.length != 4) {
            System.err.println("Usage: java Server <Server id> <port number> (<p for primary or b for backup>) (checkpoint frequency)");
            System.exit(1);
        }
        if(args.length > 2){
            isPassive = true;
            if("p".equals(args[2])){
                isBackup = false;
            }
            else if("b".equals(args[2])){
                isBackup = true;
            }
            else {
                System.err.println("Usage: java Server <Server id> <port number> (<primary ot backup>) (option: p for primary, b for back up))");
                System.exit(1);
            }
        }
        if(args.length == 4){
            ckpt_freq = Integer.parseInt(args[3]);
        }
        int portNumber = Integer.parseInt(args[1]);
        serverID = Integer.parseInt(args[0]);
        System.out.println("<----Server " + serverID +" started on port " + portNumber + "---->");

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
            new ServerThread(clientSocket,isBackup).start();

            //if this is a primary server,send checkpoint periodic
            if(isPassive && ckpt_timer++ == ckpt_freq){
                ckpt_timer = 0;
                for(int i = 0; i < serverConstant.serverNumber; i ++){
                    if(i != serverID){
                        new CheckpointThread(i).start();
                    }
                }
            }
        }
    }
}