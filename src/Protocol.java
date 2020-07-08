/*
 * Client side: Format command;
 * Server side: Unpack command;
 *
 * Protocol: x=1 -----> ?ID?$x$#1#
 * */

import java.util.concurrent.atomic.AtomicLong;

public class Protocol {
    final private String Spliter = "#";

    private static AtomicLong idCounter = new AtomicLong();

    public static String createID() {
        return String.valueOf(idCounter.getAndIncrement());
    }

    /**
     * TODO: checker is still a naive one, it does not check if line is legitimate
     *
     * @param line input string to parse, format: var=value
     * @return null when error happens, otherwise packed string
     */
    public String clientPack(String line, int clientID, int serverID, int reqID) {
        //Handle exception
        if (line == null || packFormatChecker(line))
            return null;

        String uniqueID = clientID + Spliter + serverID + Spliter + reqID;
        //Locate =
        int loc = line.indexOf('=');
        String varString = line.substring(0, loc);
        String valueString = line.substring(loc + 1);

        return uniqueID + Spliter + varString + Spliter + valueString;
    }


    public String serverPack(String memory, int clientID, int serverID, int reqID) {
        String uniqueID = clientID + Spliter + serverID + Spliter + reqID;
        return uniqueID + Spliter + memory;
    }

    /**
     * TODO: checker is still a naive one, it does not check if line is legitimate
     *
     * @param line input string to unpack, format: var=value
     * @return null when error happens, otherwise unpacked variables
     * @apiNote
     */
    public parseResult clientUnpack(String line) {
        String[] infos = line.split(Spliter);

        if (infos.length != 4) {
            System.err.println("Invalid protocol message!");
            System.exit(1);
        }

        String clientId = infos[0];
        String serverId = infos[1];
        String reqId = infos[2];
        String message = infos[3];

        return new parseResult(clientId, serverId, reqId, message, null);
    }


    public parseResult serverUnpack(String line) {
        String[] infos = line.split(Spliter);

        if (infos.length != 5) {
            System.err.println("Invalid protocol message!");
            System.exit(1);
        }

        String clientId = infos[0];
        String serverId = infos[1];
        String reqId = infos[2];
        String var = infos[3];
        String value = infos[4];

        return new parseResult(clientId, serverId, reqId, var, value);
    }

    public String LFDPack(int LFDId, String operation, int serverID) {
        //String uniqueID = LFDId + Spliter + operation + Spliter + serverID;

        return LFDId + Spliter + operation + Spliter + serverID;
    }

    //GFD receive message from LFD
    public parseResult GFDUnpack(String line) {
        String[] infos = line.split(Spliter);

        if (infos.length != 3) {
            System.err.println("Invalid protocol message!");
            System.exit(1);
        }

        String LFDId = infos[0];
        String operation = infos[1];
        String serverId = infos[2];

        return new parseResult(LFDId, operation, serverId);
    }


    /**
     * @param inputLine string to check format
     * @return true when not passed check, otherwise false
     */
    public boolean packFormatChecker(String inputLine) {
        // Make sure there is one and only one =
        if (inputLine.chars().filter(ch -> ch == '=').count() != 1)
            return true;
        // Make sure there is var and value
        int idx = -1;
        if (((idx = inputLine.indexOf('=')) == 0) || idx + 1 == inputLine.length())
            return true;

        return false;
    }
}

/**
 * Class for returning multiple return value.
 */
final class parseResult {
    public int clientID;
    public int serverID;
    public int reqID;
    public String var;
    public String value;


    parseResult(String clientId, String serverId, String reqId, String var, String value) {
        this.clientID = Integer.valueOf(clientId);
        this.serverID = Integer.valueOf(serverId);
        this.reqID = Integer.valueOf(reqId);
        this.var = var;
        this.value = value;
    }

    public int LFDId;
    public String operation;
    public int serverId;

    parseResult(String LFDId, String operation, String serverId) {
        this.LFDId = Integer.valueOf(LFDId);
        this.operation = operation;
        this.serverID = Integer.valueOf(serverId);
    }
}
