package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.List;

/** Represents a gitlet commit object.
 *
 *  does at a high level.
 *
 *  @author Jonathan Sun
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private String timestamp;
    private HashMap<String, String> metadata;
    private List<String> parent;

    public Commit(String m, List<String> p, HashMap<String, String> meta) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        if (p == null) {
            timestamp = formatter.format(new Date(0));
        } else {
            timestamp = formatter.format(new Date());
        }
        message = m;
        parent = p;
        metadata = meta;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<String> getParent() {
        return parent;
    }

    public HashMap<String, String> getMetadata() {
        return metadata;
    }
}
