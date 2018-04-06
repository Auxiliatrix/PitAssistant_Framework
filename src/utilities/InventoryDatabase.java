package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constants.Calibration;

public class InventoryDatabase {
	private static SimpleDateFormat sdf = new SimpleDateFormat( Calibration.DATEFORMAT );

	private Connection connection = null;
	private Statement statement = null;
	private PreparedStatement prep = null;
	private long defaultTeam;
	private long defaultInventory;
	private long defaultContainer;

	public InventoryDatabase() {
		this( sdf.format( Calendar.getInstance().getTime() ) );
	}

	public InventoryDatabase( String name ) {
		try {
			Class.forName("org.sqlite.JDBC"); // Do i need this?
			connection = DriverManager.getConnection( name );

			statement = connection.createStatement();
			statement.setQueryTimeout( Calibration.DEFAULT_SQL_TIMEOUT );  // set timeout to 30 sec.
		} catch( SQLException e ) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/* Useful utilites */

	public boolean isInitialized() {
		// Checks to see if the database is correct
		// Needs work
		try {
			ResultSet rs;

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='team';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='item';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='itemname';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='container';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='inventory';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='itemcontainer';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

			rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='itemorigincontainer';");
			if( rs.getFetchSize() == 0 ) {
				return false;
			}

		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Copies the loaded database to the location specified in the configuration
	 * @return boolean Whether the operation was successful, or threw an error
	 */
	public boolean backup() {
		/* Useful if using a memory database */
		return backup( Calibration.BACKUP_DATABASE );
	}

	/**
	 * Copies the loaded database to the location specified by file.
	 * @param file The file to copy the loaded database to
	 * @return boolean Whether the operation was successful, or threw an error
	 */
	public boolean backup( String file ) {
		try {
			statement.executeUpdate( "backup to " + file );
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Loads the database stored in the location given in the configuration file.
	 * @return boolean Wheter the operation was successful, or threw an error
	 */
	public boolean restore() {
		return restore( Calibration.BACKUP_DATABASE );
	}

	/**
	 * Loads the database stored in the location given by name.
	 * @param name The file to load info from
	 * @return boolean Wheter the operation was successful, or threw an error
	 */
	public boolean restore( String name ) {
		/* Load database, useful for in-memory databases */
		try {
			statement.executeUpdate( "restore from " + name );
			return true;
		} catch( SQLException e ) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Shut down the currently accessed database.
	 * This should be called before stopping the program.
	 */
	public void close() {
		try {
			prep.close();
			statement.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* CREATE DATABASE */

	/**
	 * Builds the database in the file.
	 * It is reccomended to check if the database has already been created, first.
	 * @return If the operation was successful
	 */
	public boolean init() {
		// Adds required tables and columns to the database
		try {
			// Require that key relations are good
			statement.executeUpdate( "PRAGMA foreign_keys = ON ;" );

			/* Create nessessary tables */
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS team ( id integer UNIQUE NOT NULL, name text UNIQUE NOT NULL, "
					+ "time integer);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS inventory ( id integer PRIMARY KEY NOT NULL, name text UNIQUE NOT NULL, "
					+ "team integer NOT NULL DEFAULT -1, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS container ( id integer PRIMARY KEY NOT NULL, name text UNIQUE NOT NULL, "
					+ "inventory integer NOT NULL, team integer NOT NULL, time integer NOT NULL,"
					+ "FOREIGN KEY (inventory) REFERENCES inventory (id));" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS item ( id integer PRIMARY KEY NOT NULL, "
					+ "team integer NOT NULL DEFAULT -1, origincontainer integer, count integer NOT NULL DEFAULT 1, "
					+ "time integer, "
					+ "FOREIGN KEY (team) REFERENCES team(id) );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS itemname ( id integer NOT NULL, name text UNIQUE NOT NULL, "
					+ "time integer NOT NULL DEFAULT -1,"
					+ "FOREIGN KEY (id) REFERENCES item(id) );" );
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS itemcontainer ( id integer NOT NULL, container NOT NULL, "
					+ "FOREIGN KEY (id) REFERENCES item (id), FOREIGN KEY (container) REFERENCES container (id) )");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS itemorigincontainer ( id integer NOT NULL, container NOT NULL, "
					+ "FOREIGN KEY (id) REFERENCES item (id), FOREIGN KEY (container) REFERENCES container (id) )");

			/* Add default values to tables */
			ResultSet rs;
			prep = connection.prepareStatement("INSERT INTO team ( id , name, time ) VALUES ( -1, 'default', ? );");
			prep.setLong( 1, getTime() );
			prep.execute();
			rs = statement.executeQuery("SELECT id FROM team WHERE name = 'default';");
			if( rs.next() ) {
				defaultTeam = rs.getLong("id"); // ID of default team
			}

			prep = connection.prepareStatement("INSERT INTO inventory ( name, team, time ) VALUES ( 'default', ?, ? );"); // Create default inventory
			prep.setLong( 1, defaultTeam );
			prep.setLong( 2, getTime() );
			prep.execute();
			rs = statement.executeQuery("SELECT id FROM inventory WHERE name = 'default';"); // Get the id of the default inventory
			if( rs.next() ) {
				defaultInventory = rs.getLong("id");
			}

			prep = connection.prepareStatement("INSERT INTO container ( name, inventory, team, time ) VALUES ( 'default', ?, ?, ? );"); // Create default container
			prep.setLong( 1, defaultInventory );
			prep.setLong( 2, defaultTeam );
			prep.setLong( 3, getTime() );
			prep.execute();
			rs = statement.executeQuery("SELECT id FROM container WHERE name = 'default';"); // Get the id of the default container
			if( rs.next() ) {
				defaultContainer = rs.getLong("id");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/* QUERY DATABASE */

	// Check if entry name exists

	/**
	 * Checks if the given item is stored in the database
	 * @param team The item to check for existance
	 * @return Whether the given item exists
	 */
	public boolean itemExists( String item ) {
		try {
			prep = connection.prepareStatement("SELECT count(*) FROM item WHERE id = ?;");
			prep.setLong( 1, getid(item) );
			ResultSet rs = prep.executeQuery();
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks if the given container is stored in the database
	 * @param team The container to check for existance
	 * @return Whether the given container exists
	 */
	public boolean containerExists( String container ) {
		try {
			prep = connection.prepareStatement("SELECT count(*) FROM container WHERE id = ?;");
			prep.setLong(1, getContainerID(container));
			ResultSet rs = prep.executeQuery();		
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks if the given inventory is stored in the database
	 * @param team The inventory to check for existance
	 * @return Whether the given inventory exists
	 */
	public boolean inventoryExists( String inventory ) {
		try {
			prep = connection.prepareStatement("SELECT count(*) FROM inventory WHERE id = ?;");
			prep.setLong(1, getInventoryID(inventory));
			ResultSet rs = prep.executeQuery();
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks if the given team name is stored in the database
	 * @param team The team name to check for existance
	 * @return Whether the given team name exists
	 */
	public boolean teamExists( String team ) {
		try {
			prep = connection.prepareStatement("SELECT count(*) FROM team WHERE id = ?;");
			prep.setLong(1, getTeamID( team ));
			ResultSet rs = prep.executeQuery();
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks if the given team id is stored in the database
	 * @param team The team id to check for existance
	 * @return Whether the given team id exists
	 */
	public boolean teamExists( long team ) {
		try {
			prep = connection.prepareStatement("SELECT count(*) FROM team WHERE id = ?;");
			prep.setLong(1, team);
			ResultSet rs = prep.executeQuery();
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/* Get all subvalues of database */

	/**
	 * Gets every inventory in the database
	 * @return A String array of every inventory name
	 */
	public String[] getAllInventories() {
		List<String> result = new ArrayList<String>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT name FROM inventory;");

			while( rs.next() ) {
				result.add( rs.getString("name") );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result.toArray( new String[ result.size() ] );
	}

	/**
	 * Gets every container in the database
	 * @return A String array of every container name
	 */
	public String[] getAllContainters() {
		List<String> result = new ArrayList<String>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT name FROM container;");

			while( rs.next() ) {
				result.add( rs.getString("name") );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result.toArray( new String[ result.size() ] );
	}

	/**
	 * Gets every item within the database
	 * @return A 2D String array, with every name of each item included
	 */
	public String[][] getAllItems() {
		List<List<String>> result = new ArrayList<List<String>>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			rs =  statement.executeQuery("SELECT id FROM item;");

			int index = 0;
			while( rs.next() ) {
				result.set( index, new ArrayList<String>() );
				prep = connection.prepareStatement("SELECT name FROM itemname WHERE id = ?;");
				prep.setLong(1, rs.getLong("id"));
				ResultSet rs2 = prep.executeQuery();

				while( rs2.next() ) {
					result.get(index).add( rs2.getString("name") );
				}

				index++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toArray( result );
	}

	/**
	 * Gets all the containers contained within the inventory
	 * @param container The name of the inventory to list
	 * @return A 2D String array, with all of the names of each container
	 */
	public String[] getContainters( String inventory ) {
		List<String> result = new ArrayList<String>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT name FROM container WHERE inventory = ?;");
			prep.setLong(1, getInventoryID( inventory ));
			rs =  prep.executeQuery();

			while( rs.next() ) {
				result.add( rs.getString("name") );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result.toArray( new String[ result.size() ] );
	}

	/**
	 * Gets all the items contained within the container
	 * @param container The name of the container to list
	 * @return A 2D String array, with all of the names of each object
	 */
	public String[][] getItems( String container ) {
		List<List<String>> result = new ArrayList<List<String>>();
		PreparedStatement prep;

		try {
			ResultSet rs;
			prep = connection.prepareStatement("SELECT id FROM itemcontainer WHERE container = ?;");
			prep.setLong(1, getContainerID(container));
			rs = prep.executeQuery();

			int index = 0;
			while( rs.next() ) {
				result.add( index, new ArrayList<String>() );
				long id = rs.getLong("id");

				ResultSet rs2;
				prep = connection.prepareStatement("SELECT name FROM itemname WHERE id = ?;");
				prep.setLong(1, id);
				rs2 = prep.executeQuery();
				while( rs2.next() ) {
					result.get(index).add( rs2.getString("name") );
				}

				index++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toArray( result );
	}

	/* Get all names for the items */

	/**
	 * Retrieves all of the names of the given item
	 * @param name Name of the item to get names for
	 * @return A String array containing all of the names of the given item
	 */
	public String[] getItemNames( String name ) {
		List<String> names = new ArrayList<String>();
		PreparedStatement prep;

		try {
			long id = -1;
			prep = connection.prepareStatement("SELECT id FROM itemname WHERE name = ?;");
			prep.setString(1, name);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				id = rs.getLong("id");
			}

			prep = connection.prepareStatement("SELECT name FROM itemname WHERE id = ?;");
			prep.setLong(1, id);
			rs = prep.executeQuery();
			while( rs.next() ) {
				names.add( rs.getString("name") );
			}

		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return names.toArray( new String[ names.size() ] );
	}

	/* Get all info about the thing */

	/**
	 * Retrieves all the values of the given inventory.
	 * @param inventory The inventory to retrieve values of
	 * @return A map where the column names are keys, and the values are stored
	 */
	public Map<String, Object> getInventory( String inventory ) {
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT * FROM inventory WHERE id = ?;");
			prep.setLong(1, getInventoryID( inventory ));
			rs =  prep.executeQuery();
			int colCount = rs.getMetaData().getColumnCount();
			while( rs.next() ) {
				for( int i = 0; i <= colCount; i++ ) {
					result.put( rs.getMetaData().getColumnName( i ), rs.getObject( i ) );
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Retrieves all the values of the given container.
	 * @param container The container to retrieve values of
	 * @return A map where the column names are keys, and the values are stored
	 */
	public Map<String, Object> getContainer( String container ){
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT * FROM container WHERE id = ?;");
			prep.setLong(1, getContainerID( container ));
			rs =  prep.executeQuery();
			int colCount = rs.getMetaData().getColumnCount();
			while( rs.next() ) {
				for( int i = 0; i <= colCount; i++ ) {
					result.put( rs.getMetaData().getColumnName( i ), rs.getObject( i ) );
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Retrieves all the values of the given item.
	 * Does not retrieve all names.
	 * This is a known issue.
	 * @param item The item to retrieve values of
	 * @return A map where the column names are keys, and the values are stored
	 */
	@Deprecated
	public Map<String, Object> getItem( String item ){ // Does not play well with aliases
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT * FROM item WHERE id = ?;");
			prep.setLong(1, getid( item ));
			rs =  prep.executeQuery();
			int colCount = rs.getMetaData().getColumnCount();
			while( rs.next() ) {
				for( int i = 0; i <= colCount; i++ ) {
					result.put( rs.getMetaData().getColumnName( i ), rs.getObject( i ) );
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	/* Get who owns the object */

	/**
	 * Gets the team number that owns the given inventory
	 * @param inventory Name of the inventory to query
	 * @return Team number of the owner of the inventory
	 */
	public String getInventoryOwner( String inventory ) {
		String owner = "";
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT team FROM inventory WHERE id = ?;");
			prep.setLong(1, getInventoryID( inventory ));
			rs = prep.executeQuery();
			if( rs.next() ) {
				owner = rs.getString("team");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return owner;
	}

	/**
	 * Gets the team number that owns the given container
	 * @param container Name of the container to query
	 * @return Team number of the owner of the container
	 */
	public String getContainerOwner( String container ) {
		String owner = "";
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT team FROM container WHERE id = ?;");
			prep.setLong(1, getContainerID( container ));
			rs = prep.executeQuery();
			if( rs.next() ) {
				owner = rs.getString("team");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return owner;
	}

	/**
	 * Gets the team number that owns the given item
	 * @param item Name of the item to query
	 * @return Team number of the owner of the item
	 */
	public String getItemOwner( String item ) {
		String owner = "";
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT team FROM item WHERE id = ?;");
			prep.setLong(1, getid( item ));
			rs = prep.executeQuery();
			if( rs.next() ) {
				owner = rs.getString("team");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return owner;
	}

	/* Get where the object is */

	/**
	 * Retrieve the current inventory of the given container.
	 * @param item The name of the container to get the inventory location of
	 * @return The current inventory location of the given container
	 */
	public String getContainerLocation( String container ) {
		String location = "";
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT inventory FROM container WHERE id = ?;");
			prep.setLong(1, getContainerID( container ));
			rs = prep.executeQuery();
			if( rs.next() ) {
				location = rs.getString("inventory");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/**
	 * Retrieve the current container of the given item.
	 * This does not represent the origin container of the item.
	 * @param item The name of the item to get the container of
	 * @return The current container of the given item
	 */
	public String[] getItemLocation( String item ) { // TODO
		List<String> location = new ArrayList<>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT container FROM itemcontainer WHERE id = ?;");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			while( rs.next() ) {
				location.add( rs.getString("container") );
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location.toArray( new String[ location.size() ] );
	}

	/**
	 * Retrieve the origin container of the given item.
	 * This does not represent the current location of the item.
	 * @param item The name of the item to get the origin container of
	 * @return The origin container of the given item
	 */
	public String[] getItemOriginLocation( String item ) {
		List<String> location = new ArrayList<>();
		ResultSet rs;
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("SELECT container FROM itemorigincontainer WHERE id = ?;");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			while( rs.next() ) {
				location.add( rs.getString("container") );
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location.toArray( new String[ location.size() ] );
	}

	/* SET DATABASE VALUES */

	/**
	 * Sets the given item's current container to be equal to the given container.
	 * The current container should be condsidered current location of the item.
	 * It does not nessessarily reflect the item's proper storage location.
	 * @param container The name of the container to alter
	 * @param inventory The inventory name to set as the inventory of the item
	 * @return Whether setting the item comtainer was successful or not
	 * @throws EntryNotExistException Thrown when the given container or inventory is not found in the database
	 */
	public boolean setContainerInventory( String container, String inventory ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE container SET inventory = ? WHERE id = ?;");
			prep.setLong(1, getInventoryID(inventory));
			prep.setLong(2, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean moveItem( String item, String originalContainer, String newContainer ) throws EntryNotExistException, ItemNoContainerException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( originalContainer ) ) {
			throw new EntryNotExistException( originalContainer );
		} else if( !containerExists( newContainer ) ) {
			throw new EntryNotExistException(newContainer);
		}

		if( !removeItemContainer( item, originalContainer ) ) {
			return false;
		}
		try {
			addItemContainer( item, newContainer );
			return true;
		} catch (LocationsExceedItemsException e) {
			e.printStackTrace();
			return false;
			// This should never happen. Ever.
		}
	}

	/**
	 * Sets the given item's current container to be equal to the given container.
	 * The current container should be condsidered current location of the item.
	 * It does not nessessarily reflect the item's proper storage location.
	 * @param item The name of the item to alter
	 * @param container The container name to add as the current container of the item
	 * @return Whether adding the item comtainer was successful or not
	 * @throws EntryNotExistException Thrown when the given item or container is not found in the database
	 */
	public boolean addItemContainer( String item, String container ) throws EntryNotExistException, LocationsExceedItemsException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( itemLocationsEqualTotal( item ) ) {
			throw new LocationsExceedItemsException(item);
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("INSERT INTO itemcontainer ( id, container ) VALUES ( ?, ? );");
			prep.setLong(1, getItemNameID(item));
			prep.setLong(2, getContainerID(container));
			prep.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}


		return true;
	}

	/**
	 * 
	 * @param item
	 * @param container
	 * @return
	 * @throws EntryNotExistException
	 */
	public boolean removeItemContainer( String item, String container ) throws EntryNotExistException, ItemNoContainerException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( containersIsOne( item ) ) {
			throw new ItemNoContainerException(item);
		}

		PreparedStatement prep = null;

		try {
			prep = connection.prepareStatement("DELETE FROM itemcontainer WHERE id = ? AND container = ?"); // are1 those the right column names?
			prep.setLong(1, getItemNameID(item));
			prep.setLong(2, getContainerID(container));
			prep.execute();
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Adds to the given item's original container list the given container.
	 * The original container should be condsidered one of the proper place for the item, when stored properly.
	 * It does not nessessarily reflect the item's current location(s).
	 * @param item The name of the item to alter
	 * @param container The container name to add as the origin of the item
	 * @return Whether adding the item origin was successful or not
	 * @throws EntryNotExistException Thrown when the given item or container is not found in the database
	 */
	public boolean addItemOriginContainer( String item, String container ) throws EntryNotExistException, LocationsExceedItemsException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( itemOriginLocationsEqualTotal(item) ) {
			throw new LocationsExceedItemsException(item);
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("INSERT INTO itemorigincontainer ( id, container ) VALUES ( ?, ? );");
			prep.setLong(1, getItemNameID(item));
			prep.setLong(2, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Adds to the given item's original container list the given container.
	 * The original container should be condsidered one of the proper place for the item, when stored properly.
	 * It does not nessessarily reflect the item's current location(s).
	 * @param item The name of the item to alter
	 * @param container The container name to add as the origin of the item
	 * @return Whether adding the item origin was successful or not
	 * @throws EntryNotExistException Thrown when the given item or container is not found in the database
	 */
	public boolean removeItemOriginContainer( String item, String container ) throws EntryNotExistException, ItemNoContainerException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( containersOriginIsOne(item) ) {
			throw new ItemNoContainerException(item);
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("DELETE FROM itemorigincontainer WHERE id = ? AND container = ?;");
			prep.setLong(1, getItemNameID(item));
			prep.setLong(2, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given item to the team name given
	 * @param inventory The item name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given item or team is not found in the database
	 */
	public boolean setItemOwner( String item, String team ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE item SET team = ? WHERE id = ?;");
			prep.setLong(1, getTeamID(team));
			prep.setLong(2, getid(item));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given container to the team name given
	 * @param inventory The container name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given container or team is not found in the database
	 */
	public boolean setContainerOwner( String container, String team ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE container SET team = ? WHERE id = ?;");
			prep.setLong(1, getTeamID(team));
			prep.setLong(2, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given inventory to the team name given
	 * @param inventory The inventory name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given inventory or team is not found in the database
	 */
	public boolean setInventoryOwner( String inventory, String team ) throws EntryNotExistException {
		if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE item SET team = ? WHERE id = ?;");
			prep.setLong(1, getTeamID(team));
			prep.setLong(2, getInventoryID(inventory));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given item to the team id given
	 * @param inventory The item name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given item or team is not found in the database
	 */
	public boolean setItemOwner( String item, long team ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE item SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, team);
			prep.setLong(2, getTime());
			prep.setLong(3, getid(item));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given container to the team id given
	 * @param inventory The container name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given container or team is not found in the database
	 */
	public boolean setContainerOwner( String container, long team ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE container SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, team);
			prep.setLong(2, getTime());
			prep.setLong(3, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Changes the owner of the given inventory to the team id given
	 * @param inventory The inventory name of the entry to alter
	 * @param team The team owner to set the entry to
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given inventory or team is not found in the database
	 */
	public boolean setInventoryOwner( String inventory, long team ) throws EntryNotExistException {
		if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE item SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, team);
			prep.setLong(2, getTime());
			prep.setLong(3, getInventoryID(inventory));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Removes the name given from the item it corresponds to.
	 * Beware that if you remove the only remaining name from the item, it will become impossible to refrence the item
	 * @param item The name to remove from the matching item
	 * @return Whether removing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given item is not found in the database
	 */
	public boolean removeItemName( String item ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("REMOVE FROM itemname WHERE name = ?;");
			prep.setString(1, item);
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Adds a new name to the item that possesses the old name
	 * @param oldName The original name of the item
	 * @param newName The new name to add to the item
	 * @return Whether adding the name was successful or not
	 * @throws EntryNotExistException Thrown when the given item is not found in the database
	 */
	public boolean addItemName( String oldName, String newName ) throws EntryNotExistException, NameExistsException {
		if( !itemExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}

		PreparedStatement prep;

		try {

			prep = connection.prepareStatement("SELECT COUNT(*) FROM itemname WHERE name = ?");
			prep.setString(1, newName);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				throw new NameExistsException(newName);
			}

			long rowID = -1;
			prep = connection.prepareStatement("SELECT id FROM itemname WHERE name = ?;");
			prep.setString(1, oldName);
			rs = prep.executeQuery();
			if( rs.next() ) {
				rowID = rs.getLong("id");
			}

			prep = connection.prepareStatement("INSERT INTO itemname ( id, name ) VALUES ( ?, ? );");
			prep.setLong(1, rowID);
			prep.setString(2, newName);
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Replaces the old name of the item with the new name
	 * @param oldName The original name of the item
	 * @param newName The new name to set the item to
	 * @return Whether changing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given item is not found in the database
	 */
	public boolean addItemName( String oldName, String[] newName ) throws EntryNotExistException {
		if( !itemExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}

		PreparedStatement prep;

		try {
			long rowID = -1;
			prep = connection.prepareStatement("SELECT id FROM itemname WHERE name = ?;");
			prep.setString(1, oldName);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				rowID = rs.getLong("id");
			}

			for( String i : newName ) {
				prep = connection.prepareStatement("INSERT INTO itemname ( id, name ) VALUES ?, ?;");
				prep.setLong(1, rowID);
				prep.setString(2, i);
				prep.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean addItemName( String oldName, List<String> newName ) throws EntryNotExistException {
		return addItemName( oldName, newName.toArray( new String[ newName.size() ] ) );
	}

	/**
	 * Replaces the old name of the container with the new name
	 * @param oldName The original name of the container
	 * @param newName The new name to set the container to
	 * @return Whether changing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given container is not found in the database
	 */
	public boolean setContainerName( String oldName, String newName ) throws EntryNotExistException {
		if( !containerExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE container SET name = ? WHERE id = ?;");
			prep.setString(1, newName);
			prep.setLong(2, getContainerID(oldName));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Replaces the old name of the inventory with the new name
	 * @param oldName The original name of the inventory
	 * @param newName The new name to set the inventory to
	 * @return Whether changing the name was successful or not
	 * @throws EntryNotExistException Thrown when the given inventory is not found in the database
	 */
	public boolean setInventoryName( String oldName, String newName ) throws EntryNotExistException {
		if( !inventoryExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE inventory SET name = ?, time = ? WHERE id = ?;");
			prep.setString(1, newName);
			prep.setLong(2, getTime());
			prep.setLong(3, getInventoryID(oldName));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Sets the given item to the default owner, which is set to be a nonexistent team
	 * @param item The item to modify
	 * @return Whether the action succeeded or not
	 * @throws EntryNotExistException Thrown when the given item is not found in the database
	 */
	public boolean setItemTeamToDefault( String item ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE item SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, defaultTeam);
			prep.setLong(2, getTime());
			prep.setLong(3, getid(item));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Sets the given container to the default owner, which is set to be a nonexistent team
	 * @param container The container to modify
	 * @return Whether the action succeeded or not
	 * @throws EntryNotExistException Thrown when the given container is not found in the database
	 */
	public boolean setContainerTeamToDefault( String container ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE container SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, defaultTeam);
			prep.setLong(2, getTime());
			prep.setLong(3, getContainerID(container));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Sets the given inventory to the default owner, which is set to be a nonexistent team
	 * @param inventory The inventory to modify
	 * @return Whether the action succeeded or not
	 * @throws EntryNotExistException Thrown when the given inventory is not found in the database
	 */
	public boolean setInventoryTeamToDefault( String inventory ) throws EntryNotExistException {
		if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		}

		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("UPDATE inventory SET team = ?, time = ? WHERE id = ?;");
			prep.setLong(1, defaultTeam);
			prep.setLong(2, getTime());
			prep.setLong(3, getInventoryID(inventory));
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/* CREATE OBJECTS */

	/**
	 * Creates a new inventory entry, for holding containers
	 * @param name The name of the inventory
	 * @param team The team that owns the inventory
	 */
	public void newInventory( String name, String team ) {
		newInventory(name, getTeamID(team));
	}

	/**
	 * Creates a new inventory entry, for holding containers
	 * @param name The name of the inventory
	 * @param team The team id that owns the inventory
	 */
	public void newInventory( String name, long team ) {
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("INSERT INTO inventory ( name, team, time ) VALUES ( ?, ?, ? );");
			prep.setString(1, name);
			prep.setLong(2, team);
			prep.setLong(3, getTime());
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new container entry, for holding items
	 * @param name The name of the container
	 * @param inventory The inventory that this container resides within
	 * @param team The name of the team that owns the container
	 */
	public void newContainer( String name, String inventory, String team ) {
		newContainer( name, inventory, getTeamID(team) );
	}

	/**
	 * Creates a new container entry, for holding items
	 * @param name The name of the container
	 * @param inventory The inventory that this container resides within
	 * @param team The team id that owns the container
	 */
	public void newContainer( String name, String inventory, long team ) {
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("INSERT INTO container ( name, inventory, team, time ) VALUES ( ?, ?, ?, ? );");
			prep.setString(1, name);
			prep.setLong(2, getInventoryID(inventory));
			prep.setLong(3, team);
			prep.setLong(4, getTime());
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new item entry, where the conainer and origin container values are the same
	 * @param name The name of the item
	 * @param container The container the item resides within, same as the origin container
	 * @param team The name of the team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, String container, String team ) throws NameExistsException {
		newItem( name, container, container, team );
	}

	/**
	 * Creates a new item entry, where the conainer and origin container values are the same
	 * @param name The name of the item
	 * @param container The container the item resides within, same as the origin container
	 * @param team The id of the team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, String container, long team ) throws NameExistsException {
		newItem( name, container, container, team );
	}


	/**
	 * Creates a new item entry, where the conainer and origin container values are the same
	 * @param names All of the possible names used to refrence the item
	 * @param container The container the item resides within, same as the origin container
	 * @param team The team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, String container, String team ) throws NameExistsException {
		newItem( names, 1, container, container, getTeamID(team) );
	}

	/**
	 * Creates a new item entry
	 * @param name The name of the item
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, String container, String originContainer, String team ) throws NameExistsException {
		newItem( name, 1, container, originContainer, getTeamID(team) );
	}

	/**
	 * Creates a new item entry
	 * @param name The name of the item
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The id of the team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, String container, String originContainer, long team ) throws NameExistsException {
		newItem( name, 1, container, originContainer, team );
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, String container, String originContainer, String team ) throws NameExistsException {
		newItem(names, 1, container, originContainer, team);
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param count A integer representing the number of duplicate objects in existance
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, long count, String container, String originContainer, String team ) throws NameExistsException {
		newItem( names, count, container, originContainer, getTeamID(team) );
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param count A integer representing the number of duplicate objects in existance
	 * @param container The current container the item resides within, and its proper location
	 * @param team The team name that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, long count, String container, String team ) throws NameExistsException {
		newItem( names, count, container, container, getTeamID(team) );
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param count A integer representing the number of duplicate objects in existance
	 * @param container The current container the item resides within, and its proper location
	 * @param team The team id that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, long count, String container, long team ) throws NameExistsException {
		newItem( names, count, container, container, team );
	}

	/**
	 * Creates a new item entry
	 * @param name The name of the item
	 * @param count A integer representing the number of duplicate objects in existance
	 * @param container The current container the item resides within, and its proper location
	 * @param team The team id that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, long count, String container, long team ) throws NameExistsException {
		newItem( name, count, container, container, team );
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param count A integer representing the number of duplicate objects in existance
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team id that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String[] names, long count, String container, String originContainer, long team ) throws NameExistsException {
		PreparedStatement prep;

		try {
			long rowID = -1;
			ResultSet rs;
			long time = getTime();

			for( String name : names ) {
				prep = connection.prepareStatement("SELECT COUNT(*) FROM itemname WHERE name = ?");
				prep.setString(1, name);
				rs = prep.executeQuery();
				if( rs.next() ) {
					throw new NameExistsException(name);
				}
			}

			prep = connection.prepareStatement("INSERT INTO item ( container, origincontainer, team, time, count ) VALUES ( ?, ?, ?, ?, ? );");
			prep.setLong(1, getContainerID(container));
			prep.setLong(2, getContainerID(originContainer));
			prep.setLong(3, (team));
			prep.setLong(4, time);
			prep.setLong(5, count);
			prep.executeUpdate();
			prep.close();

			prep = connection.prepareStatement("SELECT id FROM item WHERE container = ?, origincontainer = ?, team = ?, time = ?, count = ?;");
			prep.setLong(1, getContainerID(container));
			prep.setLong(2, getContainerID(originContainer));
			prep.setLong(3, team);
			prep.setLong(4, time);
			prep.setLong(5, count);
			rs = prep.executeQuery();
			if( rs.next() ) {
				rowID = rs.getLong("id");
			}

			prep.close();

			for( String name : names ) {
				prep = connection.prepareStatement("INSERT INTO itemname ( id, name ) VALUES ( ?, ? );");
				prep.setLong(1, rowID);
				prep.setString(2, name);
				prep.executeUpdate();
				prep.close();
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new item entry
	 * @param name The name of the item
	 * @param count The number of the items in existence
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The id of the team that owns the item
	 * @throws NameExistsException If the name already is present in the item database
	 */
	public void newItem( String name, long count, String container, String originContainer, long team ) throws NameExistsException {
		PreparedStatement prep;

		try {
			ResultSet rs = null;
			long rowID = -1;
			long time = getTime();

			prep = connection.prepareStatement("SELECT COUNT(*) AS rowcount FROM itemname WHERE name = ?");
			prep.setString(1, name);
			rs = prep.executeQuery();
			if( rs.next() ) {
				if( rs.getLong("rowcount") > 0 ) {
					throw new NameExistsException(name);
				}
			}

			prep = connection.prepareStatement("INSERT INTO item ( team, time, count ) VALUES ( ?, ?, ? );");
			prep.setLong(1, team);
			prep.setLong(2, time);
			prep.setLong(3, count);
			prep.executeUpdate();
			prep.close();

			prep = connection.prepareStatement("SELECT id FROM item WHERE team = ? AND time = ? AND count = ?;");
			prep.setLong(1, team);
			prep.setLong(2, time);
			prep.setLong(3, count);
			rs = prep.executeQuery();
			if( rs.next() ) {
				rowID = rs.getLong("id");
			}

			prep.close();
			
			prep = connection.prepareStatement("INSERT INTO itemcontainer ( id, container ) VALUES ( ?, ? );");
			prep.setLong(1, rowID);
			prep.setLong(2, getContainerID(container));
			prep.execute();
			prep.close();
			
			prep = connection.prepareStatement("INSERT INTO itemorigincontainer ( id, container ) VALUES ( ?, ? );");
			prep.setLong(1, rowID);
			prep.setLong(2, getContainerID(originContainer));
			prep.execute();
			prep.close();

			prep = connection.prepareStatement("INSERT INTO itemname ( id, name ) VALUES ( ?, ? );");
			prep.setLong(1, rowID);
			prep.setString(2, name);
			prep.executeUpdate();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new team entry
	 * @param name String representing the team name
	 * @param id long representing the team number
	 */
	public void newTeam( String name, long id ) {
		PreparedStatement prep;

		try {
			prep = connection.prepareStatement("INSERT INTO team ( id, name, time ) VALUES ( ?, ?, ? );");
			prep.setLong(1, id);
			prep.setString(2, name);
			prep.setLong(3, getTime());
			prep.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* PRIVATE HELPER FUNCTIONS */

	private long getTime() {
		return Clock.systemUTC().millis(); // Should use SQLite date and time functions
	}

	private long getInventoryID( String inventory ) { // Should these be public?
		PreparedStatement prep;
		long id = -1;

		try {
			prep = connection.prepareStatement("SELECT id FROM inventory WHERE name = ?;");
			prep.setString(1, inventory);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				id = rs.getLong("id");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getContainerID( String container ) {
		PreparedStatement prep;
		long id = -1;

		try {
			prep = connection.prepareStatement("SELECT id FROM container WHERE name = ?;");
			prep.setString(1, container);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				id = rs.getLong("id");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getid( String item ) {
		return getItemNameID( item ); // The item name database uses the item id as the key
	}

	private long getTeamID( String team ) {
		PreparedStatement prep;
		long id = -1;

		try {
			prep = connection.prepareStatement("SELECT id FROM team WHERE name = ?;");
			prep.setString(1, team);
			ResultSet rs = prep.executeQuery();
			if( rs.next() ) {
				id = rs.getLong("id");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getItemNameID( String item ) {
		PreparedStatement prep;
		ResultSet rs;
		try {
			prep = connection.prepareStatement("SELECT id FROM itemname WHERE name = ?;");
			prep.setString(1, item);
			rs = prep.executeQuery();
			if( rs.next() ) {
				return rs.getLong("id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private boolean itemLocationsEqualTotal( String item ) {
		PreparedStatement prep = null;
		ResultSet rs = null;
		int loc = 0;
		int tot = 0;
		try {
			prep = connection.prepareStatement("SELECT count FROM item WHERE id = ?;");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				tot = rs.getInt("count");
			}

			rs = null;

			prep = connection.prepareStatement("SELECT COUNT(*) AS rowcount FROM itemcontainer WHERE id = ?");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				loc = rs.getInt("rowcount");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return (loc == tot ? true : false );
	}

	private boolean itemOriginLocationsEqualTotal( String item ) {
		PreparedStatement prep = null;
		ResultSet rs = null;
		int loc = 0;
		int tot = 0;
		try {
			prep = connection.prepareStatement("SELECT count FROM item WHERE id = ?;");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				tot = rs.getInt("count");
			}

			rs = null;

			prep = connection.prepareStatement("SELECT COUNT(*) AS rowcount FROM itemorigincontainer WHERE id = ?;");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				loc = rs.getInt("rowcount");
			}

		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return (loc == tot ? true : false );
	}

	private boolean containersIsOne( String item ) {
		PreparedStatement prep = null;
		ResultSet rs = null;
		int count = 0;

		try {
			prep = connection.prepareStatement("SELECT COUNT(*) AS rowcount FROM itemcontainer WHERE id = ?");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				count = rs.getInt("rowcount");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return (count <= 1 ? true : false );
	}

	private boolean containersOriginIsOne( String item ) {
		PreparedStatement prep = null;
		ResultSet rs = null;
		int count = 0;

		try {
			prep = connection.prepareStatement("SELECT COUNT(*) AS rowcount FROM itemorigincontainer WHERE id = ?");
			prep.setLong(1, getItemNameID(item));
			rs = prep.executeQuery();
			if( rs.next() ) {
				count = rs.getInt("rowcount");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return (count <= 1 ? true : false );
	}

	private String[][] toArray( List<List<String>> list ){
		int size = 0;
		for( List<String> l : list ) {
			if( l.size() > size ) {
				size = l.size();
			}
		}

		String[][] result = new String[ list.size() ][ size ];

		int index = 0;
		for( List<String> l : list ) {
			int index2 = 0;

			for( String s : l ) {
				result[index][index2] = s;
				index2++;
			}

			index++;
		}
		return result;
	}
}
