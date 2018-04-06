package constants;

import java.util.HashMap;

public class Calibration {
	public static final String FILE_NAME = "inventory.csv";
	public static final String DATEFORMAT = "yyyyMMddhhmm";
	public static final int USER_TEAM_NUMBER = 604;
	public static final String USER_TEAM_NAME = "quixilver";
	public static final int LEVENSHTEIN_TOLERANCE = 2;
	public static final int LEVENSHTEIN_WORD_TOLERANCE = 1;
	public static final HashMap<String, String> REPLACEMENTS;
	static
    {
		REPLACEMENTS = new HashMap<String, String>();
        REPLACEMENTS.put("*", "");
        REPLACEMENTS.put("/", " ");
    }
	
	public class Database {
		public static final int DEFAULT_SQL_TIMEOUT = 30;
		public static final String DATABASE_NAME = "jdbc:sqlite:inventory.db";
		public static final String MEMORY_DATABASE = "jdbc:sqlite:";
		public static final String INVENTORY = "default";
	}
	
}
