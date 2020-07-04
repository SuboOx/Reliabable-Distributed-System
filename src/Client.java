/*
 * @file Client.java
 * A simple client support sending message to server via socket.
 * */

//import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;


public class Client {

    private static int clientID;
    private static String hostName;
    private static int workedServer = 0;
    private static HashSet<Integer> validPort = new HashSet<>();

    static class ClientThread extends Thread {
        int portNumber;
        String fromUser;

        public ClientThread(int portNumber, String fromUser) {
            this.portNumber = portNumber;
            this.fromUser = fromUser;
        }

        @Override
        public void run() {
            Protocol protocol = new Protocol();
            String fromServer;
            try (Socket kkSocket = new Socket(hostName, portNumber);
                 PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
                String msg2send = protocol.pack(fromUser, clientID);
                if (msg2send != null) {
                    System.out.println("Sending msg: " + msg2send);
                    out.println(msg2send);
                    System.out.println("Bone " + msg2send);
                } else {
                    System.out.println("Illegal input, input should be var=value");
                    return;
                }
                //TODO: when no msg send, no msg received fro, server, Client will stop at this line waiting for server.
                if ((fromServer = in.readLine()) != null) {
                    System.out.println("Server respond: " + fromServer);
                }
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
                synchronized (this){
                    workedServer --;
                    validPort.remove(portNumber);
                }
                System.out.println("There are " + workedServer + " well-functional server now.");
                if(workedServer == 0){
                    System.out.println("No server available! Exit!");
                    System.exit(1);
                }

            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + portNumber);
                synchronized (this){
                    workedServer --;
                    validPort.remove(portNumber);
                }
                System.out.println("There are " + workedServer + " well-functional server now.");
                if(workedServer == 0){
                    System.out.println("No server available! Exit!");
                    System.exit(1);
                }
            }
        }

    }




    public static void main(String[] args) {

        if (args.length != 5) {
            System.err.println("Usage: java Client <client id> <host name> <port number 1> <port number 2> <port number 3> ");
            System.exit(1);
        }

        hostName = args[1];
        clientID = Integer.parseInt(args[0]);
        int[] portNumber = new int[3];
        portNumber[0] = Integer.parseInt(args[2]);
        portNumber[1] = Integer.parseInt(args[3]);
        portNumber[2] = Integer.parseInt(args[4]);

        for (int i = 0; i < 3; i++) {
            try (Socket kkSocket = new Socket(hostName, portNumber[i]);) {
                workedServer++;
                validPort.add(portNumber[i]);
                System.err.println("Successfully connected to " + hostName + " : " + portNumber[i]);

            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + portNumber[i]);
            }
        }
        System.out.println("There are "+workedServer+" well-functional server");
        while (true){
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try {
                String fromUser = stdIn.readLine();
                if (fromUser.equals("exit"))
                    return;
                for (int i = 0; i < 3; i++) {
                    if(!validPort.contains(portNumber[i]))
                        continue;
                    new Client.ClientThread(portNumber[i],fromUser).start();
                }

            } catch (IOException e) {
                System.err.println("Can't read from the client");
                System.exit(1);
            }
        }

    }
}


