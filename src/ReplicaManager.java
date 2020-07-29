import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ReplicaManager {
    public static void main(String[] args) {
        System.out.println("<--- Replica Manager Started!--->");
        System.out.println("Usage: add:replica_id:portnumber");


        while (true) {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            try {
                String commandLine = stdIn.readLine();
                String[] ss;
                if (commandLine.equals("exit"))
                    break;

                if (!commandLine.contains(":")) {
                    System.out.println("Illegal input, input should be add:id:portnumber");
                    continue;
                }

                ss = commandLine.split(":");
                if (ss.length != 3) {
                    System.out.println("Illegal input, input should be add:id:portnumber");
                    continue;
                }
                if (ss[0].equals("add")) {
                    try {
                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start java Server " + Integer.parseInt(ss[1]) + " " + Integer.parseInt(ss[2])});
                        System.out.println("Server" + Integer.parseInt(ss[1]) + "launched");
                    } catch (Exception e) {
                        System.out.println("Can't launch Server");
                    }
                }
            } catch (IOException e) {
                System.err.println("Unable to create buffer");
                return;
            }

            //TODO: when no msg send, no msg received from server, Client will stop at this line waiting for server.
        }
        // Exit
        System.out.println("Bye!");
    }

}
