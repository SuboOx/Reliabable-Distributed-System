import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataBaseTest {
    private DataBase db = new DataBase();

    @Test
    void testPutAndGet() {
//        Test set var and value, get by var (key)
        this.db.setVariable("xa", "150");
        assertEquals("150", this.db.getVariable("xa"));
    }

    @Test
    void testModify() {
        //        Test modifying value
        this.db.setVariable("xa", "150");
        this.db.setVariable("xa", "10");
        assertEquals("10", this.db.getVariable("xa"));
    }

    @Test
    void testListAll() {
        this.db.setVariable("xa", "150");
        this.db.setVariable("xb", "10");
    }
}