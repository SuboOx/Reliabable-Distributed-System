/*
 * Local Fault Detector.
 *
 * Check if server on one port is alive evert now and then.
 */

import java.net.*;

public class LFD {
    private static int heartbeatCount = 0;

    public static void main(String[] args) throws InterruptedException {

        if (args.length != 4) {
            System.err.println("Usage: java LFD <host name> <port number> <time out> <heartbeat freq>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        int heartbeatFreq = Integer.parseInt(args[3]);

        //Wait the server to on line
        while(!isAlive(hostName, portNumber, timeout)){
            System.out.println("Waiting for Server to be launched");
        }

        //Check if server is alive
        while (true) {
            if(!log(isAlive(hostName, portNumber, timeout))){
                break;
            }
            //log(isAlive(hostName, portNumber, timeout));
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

        } catch (Exception e) {
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
