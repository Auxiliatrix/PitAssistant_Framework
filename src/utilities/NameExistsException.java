package utilities;

public class NameExistsException extends Exception {
	
	public NameExistsException() {
		super("The name exists within the database");
	}
	
	public NameExistsException( String name ) {
		super("The name " + name + " exists within the database");
	}
	
	public NameExistsException( String name, String table ) {
		super("The name " + name + " exists within the database table " + table);
	}
	
}
