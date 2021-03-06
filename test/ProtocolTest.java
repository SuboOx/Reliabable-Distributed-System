import org.junit.jupiter.api.Test;

import javax.xml.crypto.Data;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    //    public void testPack() {
//        String line = "xa=150";
//        int clientID = 15;
//        String ans = "$xa$#150#15";
//        Protocol p = new Protocol();
//        System.out.println("Packed: " + p.pack(line, clientID));
//        assertTrue(p.pack(line, clientID).contains(ans));
//    }
//
//
//    public void testUnpack() {
//        String line = "?id?$var$#value#15";
//        String id = "id";
//        String var = "var";
//        String value = "value";
//        String clientID = "15";
//
//        Protocol p = new Protocol();
//        parseResult result = p.unpack(line);
//        assertEquals(id, result.id);
//        assertEquals(var, result.var);
//        assertEquals(value, result.value);
//        assertEquals(clientID, result.clientID);
//    }
//
//
//
//
//    void testFormatChecker() {
//        Protocol p = new Protocol();
//        assertTrue(p.packFormatChecker("x==1"));
//        assertTrue(p.packFormatChecker("x"));
//        assertTrue(p.packFormatChecker("x="));
//        assertTrue(p.packFormatChecker("=1"));
//        assertTrue(p.packFormatChecker("="));
//        assertTrue(p.packFormatChecker(""));
//    }
    @Test
    public void testCheckpointPack() {
        String s = Protocol.checkpointPack(1, 1, -1, new DataBase());

        assertEquals("1#1#-1#", s);

    }
}