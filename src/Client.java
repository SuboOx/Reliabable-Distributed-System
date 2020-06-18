/*
 * @file Client.java
 * A simple client support sending message to server via socket.
 * */

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(@NotNull String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        System.out.println("<----Client started, sending message to " + hostName + ":" + portNumber + "---->");
        Protocol protocol = new Protocol();

        try (Socket kkSocket = new Socket(hostName, portNumber);
             PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {

            System.out.println("Connection established.");

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            while (true) {
                fromUser = stdIn.readLine();
//                fromServer = in.readLine();
                if (fromUser.equals("exit"))
                    break;
//                if(fromServer.equals("$RECV$")){
//                    System.out.println("Received receipt from server.");
//                    break;
//                }
                if (fromUser != null) {
                    String msg2send = protocol.pack(fromUser);
                    System.out.println("Sending msg: " + msg2send);
                    out.println(msg2send);
                }
            }
            // Exit
            System.out.println("Bye!");

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}
