import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.HashSet;

public class ReplicaManager {
    static HashSet<Integer> membership = new HashSet<>();
    // Server that once was here
    static HashSet<Integer> showedUpMember = new HashSet<>();

    static class serveGFDThread extends Thread {
        protected Socket GFDSocket;

        public serveGFDThread(Socket GFDSocket) {
            this.GFDSocket = GFDSocket;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(GFDSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(GFDSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Unable to create buffer");
                return;
            }

            String inputLine;
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        final parseResult parsed = Protocol.RMUnpack(inputLine);
                        System.out.println("Received msg from GFD :" + parsed.operation + " "
                                + parsed.serverID);

                        if (parsed.operation.equals("add")) {
                            membership.add(parsed.serverID);
                            if (showedUpMember.contains(parsed.serverID))
                                sendRecoverMsg(membership.iterator().next(), parsed.serverID);
                            showedUpMember.add(parsed.serverID);
                        } else if (parsed.operation.equals("delete")) {
                            membership.remove(parsed.serverID);
                        } else {
                            System.err.println("The message contains wrong operation!");
                            System.exit(1);
                        }

                    } else {
                        GFDSocket.close();
                        return;
                    }
                } catch (IOException e) {
                    System.err.println("Unable to read line.");
                    return;
                }
            }
        }
    }

    static void sendRecoverMsg(int sendServerID, int receiveServerID) {

        try (Socket kkSocket = new Socket(serverConstant.serverHostname[sendServerID], serverConstant.portNumber[sendServerID]);
             PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
            String msg2send = Protocol.RMCommandPack(sendServerID, receiveServerID, -2);
            if (msg2send != null) {
                System.out.println("Recover msg sent to " + sendServerID + "sending" + msg2send);
                out.println(msg2send);
            } else {
                System.out.println("Illegal input, input should be var=value");
                return;
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + serverConstant.serverHostname[sendServerID]);

        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + serverConstant.serverHostname[sendServerID] + " : " + serverConstant.portNumber[sendServerID]);
        }
    }

    public static void main(String[] args) {
        System.out.println("<--- Replica Manager Started!--->");

        ServerSocket serverSocket = null;
        Socket GFDSocket = null;

        try {
            serverSocket = new ServerSocket(RMConstant.portNumber);
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + RMConstant.portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        while (true) {
            //Serve GFD
            try {
                GFDSocket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
            // new thread for a client or server (when accepting checkpoint messages)
            new ReplicaManager.serveGFDThread(GFDSocket).start();
        }

//        while (true) {
//            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
//            try {
//                String commandLine = stdIn.readLine();
//                String[] ss;
//                if (commandLine.equals("exit"))
//                    break;
//
//                if (!commandLine.contains(":")) {
//                    System.out.println("Illegal input, input should be add:id:portnumber");
//                    continue;
//                }
//
//                ss = commandLine.split(":");
//                if (ss.length != 3) {
//                    System.out.println("Illegal input, input should be add:id:portnumber");
//                    continue;
//                }
//                if (ss[0].equals("add")) {
//                    try {
//                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start java Server " + Integer.parseInt(ss[1]) + " " + Integer.parseInt(ss[2])});
//                        System.out.println("Server" + Integer.parseInt(ss[1]) + "launched");
//                    } catch (Exception e) {
//                        System.out.println("Can't launch Server");
//                    }
//                }
//            } catch (IOException e) {
//                System.err.println("Unable to create buffer");
//                return;
//            }
//
//        }
        // Exit
        //System.out.println("Bye!");
    }

}
