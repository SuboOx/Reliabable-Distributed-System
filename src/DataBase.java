import java.util.HashMap;
import java.util.Set;

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
     */
    public void setVariable(String var, String value) {
        /* Note that when key var already exists, it will be automatically updated.*/
        memory.put(var, value);
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
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (String key : memory.keySet()) {
            sb.append("[").append(key).append(":").append(memory.get(key)).append("],");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    /**
     * @update the memory
     */
    public void update(DataBase newdb){
        this.memory.clear();
        this.memory.putAll(newdb.memory);

    /**
     * @return all keys in a set
     */
    Set<String> getAllKeys() {
        return memory.keySet();2ef
    }

    DataBase() {
        this.memory = new HashMap<>();
    }
}
