/*
 * Global Fault Detector.
 *
 * Receive membership from local fault detectors.
 */

import java.net.*;

public class GFD {
    /*Database of membership*/
    private static Map<String,Integer> membership = new HashMap<>();
    private static int size=membership.size();

    /*Each LFD will be served by a thread in GFD, all of the threads can update membership*/
    static class GFDThread extends Thread {
        protected Socket LFDSocket;

        public GFDThread(Socket LFDSocket) {
            this.LFDSocket = LFDSocket;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(LFDSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(LFDSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Unable to create buffer");
                return;
            }

            String inputLine;

            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        parseResult parsed = protocol.serverUnpack(inputLine);
                        if(parsed.serverID != serverID){
                            System.err.println("The message have been sent to the wrong server!");
                            System.exit(1);
                        }
                        System.out.println("Received msg from client " + parsed.clientID + ":" + inputLine);
                        System.out.println("Unpacked msg: " + parsed.var + "=" + parsed.value + ", id: " + parsed.clientID + ":req" + parsed.reqID);
                        db.setVariable(parsed.var, parsed.value);
                        String respond = protocol.serverPack(db.toString(),parsed.clientID,parsed.serverID,parsed.reqID);
                        out.println(respond);
                        //print database
                        System.out.println("Current database:");
                        System.out.println(db.toString());
                    } else {
                       LFDSocket.close();
                        return;
                    }
                } catch (IOException e) {
                    System.err.println("Unable to read membership.");
                    return;
                }
            }

        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Server <Server id> <port number> (server only available for 0/1/2)");
            System.exit(1);
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
            new ServerThread(clientSocket).start();
        }
    }
}
