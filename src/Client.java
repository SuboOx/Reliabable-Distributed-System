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
    private static final int serverNumber = 3;

    private static int clientID;
    private static String hostName;
    private static HashSet<Integer> validPort = new HashSet<>();
    private static int[] portNumber = new int[serverNumber]; 
    private static int reqcount = 0;
    private static HashSet<Integer> logging = new HashSet<>();
    
    private static void initServerInfo(){
        portNumber[0] = 8888;
        portNumber[1] = 8889;
        portNumber[2] = 8890;
        
    }

    static class ClientThread extends Thread {
        int serverID;
        String fromUser;
        int reqID;

        public ClientThread(int serverID, String fromUser, int reqID) {
            this.serverID = serverID;
            this.fromUser = fromUser;
            this.reqID = reqID;
        }

        @Override
        public void run() {
            Protocol protocol = new Protocol();
            String fromServer;
            try (Socket kkSocket = new Socket(hostName, portNumber[serverID]);
                 PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
                String msg2send = protocol.clientPack(fromUser,clientID,serverID,reqID);
                if (msg2send != null) {
                    System.out.println("Sending msg: " + msg2send);
                    out.println(msg2send);
                } else {
                    System.out.println("Illegal input, input should be var=value");
                    return;
                }
                //TODO: when no msg send, no msg received fro, server, Client will stop at this line waiting for server.
                if ((fromServer = in.readLine()) != null) {
                    parseResult parsed = protocol.clientUnpack(fromServer);
                    if(parsed.clientID != clientID){
                        System.err.println("The message have been sent to the wrong client!");
                        System.exit(1);
                    }
                    synchronized (logging){
                        if(logging.contains(parsed.reqID)){
                            System.out.println("The message has been received, duplication depress message from server" + parsed.serverID);
                        } else {
                            logging.add(parsed.reqID);
                            System.out.println("Received message from server "+parsed.serverID +" : " + parsed.var);
                        }
                    }
                }
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
                synchronized (validPort) {
                    validPort.remove(portNumber[serverID]);
                    System.out.println("There are " + validPort.size() + " well-functional server now.");
                    if (validPort.size() == 0) {
                        System.out.println("No server available! Exit!");
                        System.exit(1);
                    }
                }

            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + portNumber[serverID]);
                synchronized (this) {
                    validPort.remove(portNumber[serverID]);
                    System.out.println("There are " + validPort.size() + " well-functional server now.");
                    if (validPort.size() == 0) {
                        System.out.println("No server available! Exit!");
                        System.exit(1);
                    }
                }
            }
        }

    }




    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: java Client <client id> <host name> <serverID 1> <severID 2> <serverID 3> ");
            System.exit(1);
        }
    
        initServerInfo();
        hostName = args[1];
        clientID = Integer.parseInt(args[0]);
        int[] serverIDs = new int[3];
        
        serverIDs[0] = Integer.parseInt(args[2]);
        serverIDs[1] = Integer.parseInt(args[3]);
        serverIDs[2] = Integer.parseInt(args[4]);
        

        for (int i = 0; i < 3; i++) {
            try (Socket kkSocket = new Socket(hostName, portNumber[serverIDs[i]]);) {
                validPort.add(portNumber[serverIDs[i]]);
                System.err.println("Successfully connected to " + hostName + " : " + portNumber[serverIDs[i]]);

            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + portNumber[serverIDs[i]]);
            }
        }
        System.out.println("There are "+validPort.size()+" well-functional server");
        while (true){
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try {
                String fromUser = stdIn.readLine();
                if (fromUser.equals("exit"))
                    return;
                for (int i = 0; i < 3; i++) {
                    if(!validPort.contains(portNumber[serverIDs[i]]))
                        continue;
                    new Client.ClientThread(serverIDs[i],fromUser,reqcount).start();
                }

            } catch (IOException e) {
                System.err.println("Can't read from the client");
                System.exit(1);
            }
            reqcount ++;
        }

    }
}


