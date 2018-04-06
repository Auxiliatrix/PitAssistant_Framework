package utilities;

public class EntryNotExistException extends Exception {
	
	public EntryNotExistException( String entry, String table ) {
		super("The entry " + entry + " does not exist in table " + table);
	}
	
	public EntryNotExistException( String entry ) {
		super("The entry " + entry + " does not exist");
	}
	
	public EntryNotExistException( long entry ) {
		super("The entry " + entry + " does not exist");
	}
	
	public EntryNotExistException() {
		super("The accessed entry does not exist");
	}
}
