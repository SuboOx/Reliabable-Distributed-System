/*
 * @file Client.java
 * A simple client support sending message to server via socket.
 * */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.HashSet;


public class Client {
    private static int clientID;
    private static String hostName;
    private static HashSet<Integer> validPort = new HashSet<>();
    private static int reqCount = 0;
    private static HashSet<Integer> logging = new HashSet<>();
    private static int[] serverIDs;

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
            String fromServer;
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            try (Socket kkSocket = new Socket(hostName, serverConstant.portNumber[serverID]);
                 PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) {
                String msg2send = Protocol.clientPack(fromUser, clientID, serverID, reqID);
                if (msg2send != null) {
                    System.out.println("[" + timestamp.toString() + "]" + " Sent message" + reqID + "to" + serverID + " : " + fromUser);
                    out.println(msg2send);
                } else {
                    System.out.println("Illegal input, input should be var=value");
                    return;
                }
                //TODO: when no msg send, no msg received fro, server, Client will stop at this line waiting for server.
                if ((fromServer = in.readLine()) != null) {
                    parseResult parsed = Protocol.clientUnpack(fromServer);
                    if (parsed.clientID != clientID) {
                        System.err.println("The message have been sent to the wrong client!");
                        System.exit(1);
                    }
                    synchronized (logging) {
                        if (logging.contains(parsed.reqID)) {
                            System.out.println("msg_num " + parsed.reqID + " : Duplicate response received from replica S" + parsed.serverID);
                        } else {
                            logging.add(parsed.reqID);
                            System.out.println("[" + timestamp.toString() + "]" + " Received message " + parsed.reqID + " from server " + parsed.serverID + " : " + parsed.var);
                        }
                    }
                }
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + hostName);
                synchronized (validPort) {
                    validPort.remove(serverConstant.portNumber[serverID]);
                    System.out.println("There are " + validPort.size() + " well-functional server now.");
                    if (validPort.size() == 0) {
                        System.out.println("No server available! Exit!");
                        System.exit(1);
                    }
                }

            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + serverConstant.portNumber[serverID]);
                synchronized (this) {
                    validPort.remove(serverConstant.portNumber[serverID]);
                    System.out.println("There are " + validPort.size() + " well-functional server now.");
                    if (validPort.size() == 0) {
                        System.out.println("No server available! Exit!");
                        System.exit(1);
                    }
                }
            }
        }

    }

    static class clientHelper {
        static void readConsoleInput(String[] args) {
            if (args.length != 5) {
                System.err.println("Usage: java Client <client id> <host name> <serverID 1> <severID 2> <serverID 3> ");
                System.exit(1);
            }

            hostName = args[1];
            clientID = Integer.parseInt(args[0]);
            serverIDs = new int[serverConstant.serverNumber];

            serverIDs[0] = Integer.parseInt(args[2]);
            serverIDs[1] = Integer.parseInt(args[3]);
            serverIDs[2] = Integer.parseInt(args[4]);

        }

        static void connectMsg() {
            //check how many available server can be connected
            for (int i = 0; i < serverConstant.serverNumber; i++) {
                try (Socket kkSocket = new Socket(hostName, serverConstant.portNumber[serverIDs[i]]);) {
                    validPort.add(serverConstant.portNumber[serverIDs[i]]);
                    System.err.println("Successfully connected to " + hostName + " : " + serverConstant.portNumber[serverIDs[i]]);
                } catch (UnknownHostException e) {
                    System.err.println("Unknown host " + hostName);
                } catch (IOException e) {
                    System.err.println("Couldn't get I/O for the connection to " + hostName + " : " + serverConstant.portNumber[serverIDs[i]]);
                }
            }
            System.out.println("There are " + validPort.size() + " well-functional server");
        }
    }


    public static void main(String[] args) {
        clientHelper.readConsoleInput(args);
        clientHelper.connectMsg();
        //send messages to server
        while (true) {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try {
                String fromUser = stdIn.readLine();
                if (fromUser.equals("exit"))
                    return;
                for (int i = 0; i < serverConstant.serverNumber; i++) {
                    new Client.ClientThread(serverIDs[i], fromUser, reqCount).start();
                }
            } catch (IOException e) {
                System.err.println("Can't read from the client");
                System.exit(1);
            }
            reqCount++;
        }
    }
}


