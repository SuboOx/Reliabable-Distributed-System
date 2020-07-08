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
import java.util.HashSet;
import java.util.Set;

public class GFD {
    /*Database of membership*/
    private static Set<Integer> membership = new HashSet<>();
    private static Protocol protocol = new Protocol();

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
            final Protocol protocol = new Protocol();

            while (true) {
                try {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        final parseResult parsed = protocol.GFDUnpack(inputLine);
                        System.out.println("Received msg from LFD " + parsed.LFDID + ":" + parsed.operation + " "
                                + parsed.serverID);
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

    public static void main(final String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: GFD <port number>, Default: 8891");
            System.exit(1);
        }
        final int portNumber = Integer.parseInt(args[0]);
        System.out.println("<----GFD started on port " + portNumber + "---->");

        ServerSocket GFDSocket = null;
        Socket LFDSocket = null;

        try {
            GFDSocket = new ServerSocket(portNumber);
        } catch (final IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        while (true) {
            try {
                LFDSocket = GFDSocket.accept();
            } catch (final IOException e) {
                System.out.println("Error: " + e);
            }
            // new thread for a LFD
            new GFDThread(LFDSocket).start();
        }
    }
}