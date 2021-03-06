/*
 * Global Fault Detector.
 *
 * Receive membership from local fault detectors.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class GFD {
    /*Database of membership*/
    private static Set<Integer> membership = new HashSet<>();
    private static int portNumber;

    /*Each LFD will be served by a thread in GFD, all of the threads can update membership*/
    static class GFDThread extends Thread {
        protected Socket LFDSocket;

        public GFDThread(final Socket LFDSocket) {
            this.LFDSocket = LFDSocket;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(LFDSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(LFDSocket.getInputStream()));
            } catch (final IOException e) {
                System.err.println("Unable to create buffer");
                return;
            }

            String inputLine;

            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        final parseResult parsed = Protocol.GFDUnpack(inputLine);
                        System.out.println("Received msg from LFD " + parsed.LFDID + ":" + parsed.operation + " "
                                + parsed.serverID);

                        new communicate2RM(RMConstant.hostName, RMConstant.portNumber).send(parsed.operation, parsed.serverID);

                        if (parsed.operation.equals("add")) {
                            membership.add(parsed.serverID);
                        } else if (parsed.operation.equals("delete")) {
                            membership.remove(parsed.serverID);
                            // send message to rm to create new one
                        } else {
                            System.err.println("The message contains wrong operation!");
                            System.exit(1);
                        }

                        final int size = membership.size();
                        System.out.println("GFD: " + size + " members: ");
                        for (int value : membership) {
                            System.out.println(value);
                        }
                    } else {
                        LFDSocket.close();
                        return;
                    }
                } catch (final IOException e) {
                    System.err.println("Unable to read membership changed.");
                    return;
                }
            }

        }
    }

    static class communicate2RM {
        int RMPortNumber;
        String hostName;

        public communicate2RM(String hostName, int RMPortNumber) {
            this.RMPortNumber = RMPortNumber;
            this.hostName = hostName;
        }

        public void send(String addOrRemove, int serverID) {
            try (Socket kkSocket = new Socket(hostName, RMPortNumber);
                 PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
                String msg2send = Protocol.GFDPack(addOrRemove, serverID);

                if (msg2send != null) {
                    //System.out.println(" Sent membership to Replica Manager: " + Arrays.toString(membership.toArray()));
                    out.println(msg2send);
                } else {
                    System.err.println("Detected null msg to send");
                    return;
                }
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to RM at" + hostName);
            }

        }
    }

    static class GFDHelper {
        static void readConsoleInput(String[] args) {
            if (args.length != 1) {
                System.err.println("Usage: GFD <port number>, Default: 8891");
                System.exit(1);
            }
            portNumber = Integer.parseInt(args[0]);

        }

        static void printStartMsg() {
            System.out.println("<----GFD started on port " + portNumber + "---->");
        }
    }

    public static void main(final String[] args) {

        GFDHelper.readConsoleInput(args);
        GFDHelper.printStartMsg();


        ServerSocket GFDSocket = null;
        Socket LFDSocket = null;

        try {
            GFDSocket = new ServerSocket(portNumber);
        } catch (final IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        while (true) {
            try {
                LFDSocket = GFDSocket.accept();
            } catch (final IOException e) {
                System.out.println("Error: " + e);
            }
            // new thread for a LFD
            executor.execute(new GFDThread(LFDSocket));
        }
    }
}
