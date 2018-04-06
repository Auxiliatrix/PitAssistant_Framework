package utilities;

public class ItemNoContainerException extends Exception {
	
	public ItemNoContainerException() {
		super("The item has no holding container");
	}
	
	public ItemNoContainerException( String item ) {
		super("The item " + item + " has no holding container");
	}

}
