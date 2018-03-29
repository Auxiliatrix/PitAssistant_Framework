package constants;

import java.util.HashMap;

public class Calibration {
	public static final String TEAM = "604";
	public static final String FILE_NAME = "inventory.txt";
	public static final int LEVENSHTEIN_TOLERANCE = 3;
	public static final HashMap<String, String> REPLACEMENTS;
	static
    {
		REPLACEMENTS = new HashMap<String, String>();
        REPLACEMENTS.put("*", "");
        REPLACEMENTS.put("/", " ");
    }
	
}
