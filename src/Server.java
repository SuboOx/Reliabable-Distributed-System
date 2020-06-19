import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        System.out.println("<----Server started on port " + portNumber + "---->");

        /*Database and protocol here*/
        System.out.println("Initializing database and protocol...");
        DataBase db = new DataBase();
        Protocol protocol = new Protocol();
        System.out.println("Done.");

        try (ServerSocket serverSocket = new ServerSocket(portNumber);
             Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                parseResult parsed = protocol.unpack(inputLine);
                System.out.println("Received msg from client " + parsed.clientID + ":" + inputLine);
                System.out.println("Unpacked msg: " + parsed.var + "=" + parsed.value + ", id: " + parsed.id);
                db.setVariable(parsed.var, parsed.value);
                //print database
                System.out.println("Current database:");
                db.listAll();
            }
            clientSocket.close();
            System.out.println("Ready for next msg");
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

    }
}