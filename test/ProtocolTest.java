import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {
    @Test
    public void testPack() {
        String line = "xa=150";
        String ans = "$xa$#150#";
        Protocol p = new Protocol();
        System.out.println("Packed: " + p.pack(line));
        assertTrue(p.pack(line).contains(ans));
    }

    @Test
    public void testUnpack() {
        String line = "?id?$var$#value#";
        String id = "id";
        String var = "var";
        String value = "value";

        Protocol p = new Protocol();
        parseResult result = p.unpack(line);
        assertEquals(id, result.id);
        assertEquals(var, result.var);
        assertEquals(value, result.value);
    }

    @Test
    void testFormatChecker() {
        Protocol p = new Protocol();
        assertTrue(p.packFormatChecker("x==1"));
        assertTrue(p.packFormatChecker("x"));
        assertTrue(p.packFormatChecker("x="));
        assertTrue(p.packFormatChecker("=1"));
        assertTrue(p.packFormatChecker("="));
        assertTrue(p.packFormatChecker(""));
    }
}