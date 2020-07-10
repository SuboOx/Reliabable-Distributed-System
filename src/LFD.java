/*
 * Local Fault Detector.
 *
 * Check if server on one port is alive evert now and then.
 */

import java.net.*;
import java.io.*;


public class LFD {
    private static int heartbeatCount = 0;
    private static int LFDID;
    private static String hostName;

    public static void main(String[] args) throws InterruptedException {

        if (args.length != 5 && args.length != 4) {
            System.err.println("Usage: java LFD <LFD id> <host name> <serverID> <time out> (<heartbeat freq>)");
            System.exit(1);
        }
        hostName = args[1];
        LFDID = Integer.parseInt(args[0]);
        int serverID = Integer.parseInt(args[2]);
        int port = serverConstant.portNumber[serverID];
        int timeout = Integer.parseInt(args[3]);
        int heartbeatFreq = 2000;
        boolean laststatus = false;
        boolean nowstatus;

        //choose default or set a heartbeat frequency
        if (args.length == 5) {
            heartbeatFreq = Integer.parseInt(args[4]);// Start from 0
        }

        //Wait the server to on line
        while (!isAlive(hostName, port, timeout)) {
            System.out.println("Waiting for Server to be launched");
            laststatus = false;
        }

        //Check if server is alive
        while (true) {
            nowstatus = log(isAlive(hostName, port, timeout));

            if (nowstatus ^ laststatus) {
                //send message to GFD
                try (Socket s = new Socket(hostName, serverConstant.GFDport);) {
                    System.err.println("Successfully connected to " + hostName + " : " + serverConstant.GFDport);
                    InputStream is = s.getInputStream();
                    OutputStream os = s.getOutputStream();
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                    if (!laststatus) {
                        //Add replica
                        bw.write(Protocol.LFDPack(LFDID, "add", serverID));
                        bw.flush();
                    } else {
                        //Delete replica
                        bw.write(Protocol.LFDPack(LFDID, "delete", serverID));
                        bw.flush();
                    }
                } catch (UnknownHostException e) {
                    System.err.println("Unknown host " + hostName);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + serverConstant.GFDport);
                }

            }
            //send2GFD(nowstatus);
            laststatus = nowstatus;

            Thread.sleep(heartbeatFreq);
        }
    }

    /**
     * @param hostName host name
     * @param port     port number
     * @return true when alive, otherwise false
     */
    public static boolean isAlive(String hostName, int port, int timeout) {
        boolean isAlive = false;

        // Creates a socket address from a hostname and a port number
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();

        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;

        } catch (Exception ignored) {
        }
        return isAlive;
    }

    /**
     * Print the log.
     */
    private static boolean log(boolean isAlive) {
        System.out.println("Heartbeat Count: " + heartbeatCount++);
        System.out.println("Alive: " + isAlive);
        return isAlive;
    }

}
