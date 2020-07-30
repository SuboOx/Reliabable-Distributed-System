import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Server {
    /*Database and protocol, currently very naive*/
    private static DataBase db = new DataBase();
    private static ArrayList<String> logging = new ArrayList<>();
    /*Default configurations for server and necessary variables*/
    private static int serverID;
    private static boolean isPassive = false;
    private static boolean isBackup = false;
    private static boolean isRecovery = false;
    private static int ckptFreq = 10000;
    private static int portNumber;

    /*Each client will be served by a thread in server, all of the threads shares database and protocol*/
    static class ServerThread extends Thread {
        protected Socket clientSocket;

        public ServerThread(Socket clientSocket, boolean isBackup) {
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
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

//            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        /* Mark 1 for Active replica , Mark 2 for Passive Replica */
                        /* 1 - if this is a recovery replica */
                        if (isRecovery) {
                            Recover.recoverFromLog(inputLine);
                            return;
                        }
                        /* 1 - if it need to send a message to recovery replica */
                        if (Protocol.isCommandFromRM(inputLine)) {
                            new CheckpointThread(Protocol.getRecoveryID(inputLine), ckptFreq,true).start();
                            return;
                        }

                        /* 2 - if it become new primary */
                        if(Protocol.isNewPrimary(inputLine)){
                            Recover.becomePrimary();
                            isBackup = false;
                            startCkptThread();
                            return;
                        }

                        /* 2 - if this server is a backup */
                        if (isBackup) {
                            if (Protocol.isCheckpointMsg(inputLine)) {
                                parseResult parsed = Protocol.checkpointUnpack(inputLine);
                                db.update(parsed.db);
                                System.out.println("Received checkpoint msg from server " + parsed.serverID);
                                //print database
                                System.out.println("Current database:");
                                System.out.println(db.toString());
                            } else {
                                logging.add(inputLine);
                            }
                            return;
                        }



                        /* 1.2 - if the server is not a backup() primary or active */
                        parseResult parsed = Protocol.serverUnpack(inputLine);
                        if (parsed.serverID != serverID) {
                            System.err.println("The message have been sent to the wrong server!");
                            System.exit(1);
                        }
                        System.out.println("[" + timestamp.toString() + "]" + " Received msg from client " + parsed.clientID + ":" + inputLine);
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
//            }

        }
    }

    static class CheckpointThread extends Thread {
        protected int backupServerID;
        protected int ckptFreq;
        protected boolean forRecovery;

        public CheckpointThread(int serverID, int ckptFreq, boolean forRecovery) {
            this.backupServerID = serverID;
            this.ckptFreq = ckptFreq;
            this.forRecovery = forRecovery;
        }

        @Override
        public void run() {
            while (true) {
                String checkpoint;
                try (Socket kkSocket = new Socket(serverConstant.serverHostname[backupServerID], serverConstant.portNumber[backupServerID]);
                     PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {

                    //use synchronized to implement quiescence
                    synchronized (db) {
                        String msg2send = Protocol.checkpointPack(serverID, backupServerID, -1, db);
                        if (msg2send != null) {
                            System.out.println("Sending checkpoint msg to " + backupServerID);
                            System.out.println("Checkpoint msg : " + db.toString() + "Raw Msg: " + msg2send);
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
                try {
                    Thread.sleep(ckptFreq);
                    if(forRecovery){
                        return;
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupted!");
                    System.exit(1);
                }
            }
        }
    }

    static class Recover {
        // A naive recover from log way
        /* Call this when a msg has been received. */
        static public void recoverFromLog(String inputLine) {
            /* Recover state from logging */
            System.out.println("Recovering from log, hold tight...");
            // TODO: implement logging prone


            /* if receive a ckpt message, process it */
            /* else return and wait ofr tthe ckpt message */
            synchronized (db) {
                if (Protocol.isCheckpointMsg(inputLine)) {
                    parseResult parsed = Protocol.checkpointUnpack(inputLine);
                    db.update(parsed.db);
                    System.out.println("Received checkpoint msg from server " + parsed.serverID);
                    //print database
                    System.out.println("Current database:");
                    System.out.println(db.toString());
                } else {
                    logging.add(inputLine);
                    return;
                }
            }

            /* process the message in the new logging */
            synchronized (db) {
                for (String log : logging) {
                    parseResult parsed = Protocol.serverUnpack(log);
                    db.setVariable(parsed.var, parsed.value);
                    System.out.println(db.toString());
                }
            }
            isRecovery = false;
            return;

        }

        static public void becomePrimary() {
            /* Recover state from logging */
            System.out.println("Recovering from log, hold tight...");
            // TODO: implement logging prone

            /* process the message in the new logging */
            synchronized (db) {
                for (String log : logging) {
                    parseResult parsed = Protocol.serverUnpack(log);
                    db.setVariable(parsed.var, parsed.value);
                    System.out.println(db.toString());
                }
            }
        }
//
//        static public boolean amINewPrimary(String line) {
//            return line.equals("YOU_ARE_PRI");
//        }
    }

    static class Helper {
        static void readConsoleInput(String[] args) {
            if (args.length != 2 && args.length != 3 && args.length != 4) {
                System.err.println("Usage: java Server <Server id> <port number> (<p for primary or b for backup>) (checkpoint frequency)");
                System.err.println("                   When no <p/b(optional)> is given, works in active replication mode.");
                System.err.println("                   When no <checkpoint frequency> is given, default 20s.");
                System.exit(1);
            }
            if (args.length > 2) {
                isPassive = true;

                if ("primary".equals(args[2]) || "p".equals(args[2])) {
                    isBackup = false;
                } else if ("backup".equals(args[2]) || "b".equals(args[2])) {
                    isBackup = true;
                } else if ("recovery".equals(args[2]) || "r".equals(args[2])){
                    isRecovery = true;
                    isPassive = false;
                } else {
                    System.err.println("Usage: java Server <Server id> <port number> (<primary ot backup>) (option: p for primary, b for back up))");
                    System.exit(1);
                }
            }
            if (args.length == 4) {
                ckptFreq = Integer.parseInt(args[3]);
            }
            portNumber = Integer.parseInt(args[1]);
            serverID = Integer.parseInt(args[0]);
        }

        static void printStartMsg() {
            System.out.println("<----Server " + serverID + " started on port " + portNumber + "---->");
            System.out.println("Initializing database and protocol...");
            System.out.println("Done.");
        }
    }

    private static void startCkptThread(){
        if (isPassive && !isBackup) {
            for (int i = 0; i < serverConstant.serverNumber; i++) {
                if (i != serverID) {
                    // New thread for checkpoint
                    new CheckpointThread(i, ckptFreq,false).start();
                }
            }
        }
    }

    public static void main(String[] args) {

        Helper.readConsoleInput(args);
        Helper.printStartMsg();

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        /* Launch checkpoint thread for each back up server */
        startCkptThread();


        while (true) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
            // new thread for a client or server (when accepting checkpoint messages)
            new ServerThread(clientSocket, isBackup).start();
//            System.out.println("new thread");
        }
    }
}