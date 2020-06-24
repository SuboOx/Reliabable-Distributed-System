/*
 * Client side: Format command;
 * Server side: Unpack command;
 *
 * Protocol: x=1 -----> ?ID?$x$#1#
 * */

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Protocol {
    final private char idSpecifier = '?';
    final private char varSpecifier = '$';
    final private char valueSpecifier = '#';

    private static AtomicLong idCounter = new AtomicLong();

    public static String createID()
    {
        return String.valueOf(idCounter.getAndIncrement());
    }

    /**
     * TODO: checker is still a naive one, it does not check if line is legitimate
     *
     * @param line input string to parse, format: var=value
     * @return null when error happens, otherwise packed string
     */
    public String pack(String line, int clientID) {
        //Handle exception
        if (line == null || packFormatChecker(line))
            return null;

        String uniqueID = clientID*10000 + createID();
        //Locate =
        int loc = line.indexOf('=');
        String varString = line.substring(0, loc);
        String valueString = line.substring(loc + 1);

        return idSpecifier + uniqueID + idSpecifier + varSpecifier + varString + varSpecifier + valueSpecifier + valueString + valueSpecifier + clientID;
    }

    /**
     * TODO: checker is still a naive one, it does not check if line is legitimate
     *
     * @param line input string to unpack, format: var=value
     * @return null when error happens, otherwise unpacked variables
     * @apiNote
     */
    public parseResult unpack(String line) {
        String id = line.substring(line.indexOf(idSpecifier) + 1, line.lastIndexOf(idSpecifier));
        String var = line.substring(line.indexOf(varSpecifier) + 1, line.lastIndexOf(varSpecifier));
        String value = line.substring(line.indexOf(valueSpecifier) + 1, line.lastIndexOf(valueSpecifier));
        String clientID = line.substring(line.lastIndexOf(valueSpecifier) + 1);

        return new parseResult(id, var, value, clientID);
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
    public String id;
    public String var;
    public String value;
    public String clientID;

    parseResult(String id, String var, String value, String clientID) {
        this.id = id;
        this.var = var;
        this.value = value;
        this.clientID = clientID;
    }
}
