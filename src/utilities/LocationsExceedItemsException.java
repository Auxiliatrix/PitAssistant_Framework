package utilities;

public class LocationsExceedItemsException extends Exception {
	
	public LocationsExceedItemsException() {
		super("The number of locations that the item exists in is larger than the number of items in storage");
	}
	
	public LocationsExceedItemsException( String item ) {
		super("The number of locations that the item " + item + " exists in is larger than the number of items in storage");
	}

}
