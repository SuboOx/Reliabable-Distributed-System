import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.TreeSet;

public class ReplicaManager {
    static TreeSet<Integer> membership = new TreeSet<>();
    // Server that once showed up
    static HashSet<Integer> showedUpMember = new HashSet<>();
    static boolean isPassive = false;
    // primary server will be the first server connected to replica manager
    static int primaryServerID = -1;

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

            // while (true) {
            try {
                inputLine = in.readLine();
                if (inputLine != null) {
                    final parseResult parsed = Protocol.RMUnpack(inputLine);
                    System.out.println("Received msg from GFD :" + parsed.operation + " "
                            + parsed.serverID);

                    if (parsed.operation.equals("add")) {
                        // Set primary server [Passive]
                        if (isPassive && primaryServerID == -1) {
                            primaryServerID = parsed.serverID;
                            System.out.println("Server" + primaryServerID + "is the primary server.");
                        }
                        // Recover new server [Active]
                        if (showedUpMember.contains(parsed.serverID) && !isPassive)
                            sendRecoverMsg(membership.last(), parsed.serverID, -2);

                        membership.add(parsed.serverID);
                        showedUpMember.add(parsed.serverID);
                    } else if (parsed.operation.equals("delete")) {
                        membership.remove(parsed.serverID);
                        if (isPassive && parsed.serverID == primaryServerID) {
                            int newPrimaryServerID = membership.iterator().next();
                            sendRecoverMsg(newPrimaryServerID, -1, -3);
                            primaryServerID = newPrimaryServerID;
                        }

                    } else {
                        System.err.println("The message contains wrong operation!");
                        System.exit(1);
                    }
                    System.out.println(membership.toString());

                } else {
                    GFDSocket.close();
                    return;
                }
            } catch (IOException e) {
                System.err.println("Unable to read line.");
                return;
            }
            //}
        }
    }

    //reqId = -2 when active, -3 when passive
    static void sendRecoverMsg(int sendServerID, int receiveServerID, int reqID) {

        try (Socket kkSocket = new Socket(serverConstant.serverHostname[sendServerID], serverConstant.portNumber[sendServerID]);
             PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
            String msg2send = Protocol.RMCommandPack(sendServerID, receiveServerID, reqID);
            if (msg2send != null) {
                System.out.println("ServerID: " + sendServerID);
                if (reqID == -2)
                    System.out.println("Recover msg sent to " + sendServerID + " RAW: " + msg2send);
                else
                    System.out.println("Designating " + sendServerID + " as new primary server");
                out.println(msg2send);
            } else {
                System.out.println("msg is null");
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

        if (args.length > 1) {
            System.err.println("Too many arguments!");
            System.exit(1);
        } else if (args.length == 1) {
            if (args[0].equals("active") || args[0].equals("a"))
                isPassive = false;
            else if (args[0].equals("passive") || args[0].equals("p"))
                isPassive = true;
            else {
                System.err.println("Arguments can only be [p]assive or [a]ctive");
                System.exit(1);
            }
        }

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

    }
}
