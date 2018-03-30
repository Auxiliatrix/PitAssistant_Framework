package constants;

import java.util.HashMap;

public class Calibration {
	public static final String TEAM = "604";
	public static final String FILE_NAME = "inventory.txt";
	public static final String DATEFORMAT = "yyyyMMddhhmm";
	public static final int DEFAULT_SQL_TIMEOUT = 30;
	public static final String BACKUP_DATABASE = "jdbc:sqlite:backup.db";
	public static final int LEVENSHTEIN_TOLERANCE = 3;
	public static final int LEVENSHTEIN_WORD_TOLERANCE = 1;
	public static final HashMap<String, String> REPLACEMENTS;
	static
    {
		REPLACEMENTS = new HashMap<String, String>();
        REPLACEMENTS.put("*", "");
        REPLACEMENTS.put("/", " ");
    }
	
}
