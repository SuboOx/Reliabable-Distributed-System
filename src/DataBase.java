import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * This file provide interface for server database.
 * Currently just one public method provided with support for modifying several variables.
 */
@SuppressWarnings("unused")
public class DataBase {
    private HashMap<String, String> memory;

    /**
     * @param var   key of the hash map
     * @param value value of the hash map
     * @return TODO
     */
    public int setVariable(String var, String value) {
        /* Note that when key var already exists, it will be automatically updated.*/
        memory.put(var, value);
        return 0;
    }

    /**
     * @param var key of the hash map
     * @return null when no key found, otherwise the value found
     */
    public String getVariable(String var) {
        if (memory.containsKey(var)) {
            return memory.get(var);
        }
        return null;
    }

    /**
     * List all data in database
     */
    public void listAll() {
        System.out.println(Collections.singletonList(memory));
    }

    DataBase() {
        this.memory = new HashMap<>();
    }
}