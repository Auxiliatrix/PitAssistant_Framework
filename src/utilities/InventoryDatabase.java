package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
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
	private long defaultTeam;
	private long defaultInventory;
	private long defaultContainer;

	/*
	 * TABLES:
	 * ItemNames:
	 *   id <-- Same as id of item it represents
	 *   name <-- the string based name
	 * Item
	 *   Time added / modified
	 *   ID
	 *   Container
	 *   Team // Who owns the thing
	 *   Location
	 * Container
	 *   Time added / modified
	 *   Name
	 *   ID
	 *   Inventory
	 *   Team // Who owns the thing
	 * Inventory
	 *   Time added / modified
	 *   Name
	 *   ID
	 *   Team // Who owns the thing
	 * Team
	 *   ID
	 *   Name
	 *   Time added / modified
	 */

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
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS team ( id integer NOT NULL, name text NOT NULL, "
					+ "time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS inventory ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "team integer NOT NULL, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS container ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "inventory integer NOT NULL, team integer NOT NULL, time integer NOT NULL);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS itemname ( id integer NOT NULL, name text NOT NULL, "
					+ "time integer NOT NULL);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS item ( id integer PRIMARY KEY NOT NULL, name integer NOT NULL, "
					+ "container integer NOT NULL, owner integer NOT NULL, origincontainer integer, time integer NOT NULL, "
					+ "FOREIGN KEY (container) REFERENCES container(id), FOREIGN KEY (owner) REFERENCES team(id), "
					+ "FOREIGN KEY (name) REFERENCES itemname(id) );" );

			/* Add default values to tables */
			ResultSet rs;
			statement.executeUpdate("INSERT INTO team ( id , name, time ) VALUES ( -1, 'default', " + getTime() + ");"); // Create default team
			rs = statement.executeQuery("SELECT id FROM team WHERE name = 'default';");
			rs.next();
			defaultTeam = rs.getLong("id"); // ID of default team

			statement.executeUpdate("INSERT INTO inventory ( name, team, time ) VALUES ('default', " + defaultTeam + ", " + getTime() + ");"); // Create default inventory
			rs = statement.executeQuery("SELECT id FROM inventory WHERE name = 'default';"); // Get the id of the default inventory
			rs.next();
			defaultInventory = rs.getLong("id");

			statement.executeUpdate("INSERT INTO container ( name, inventory, team, time ) VALUES ('default', " + defaultInventory + ", " + defaultTeam + ", " + getTime() + ");"); // Create default container
			rs = statement.executeQuery("SELECT id FROM container WHERE name = 'default';"); // Get the id of the default container
			rs.next();
			defaultContainer = rs.getLong("id");

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
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM item WHERE id = " + getid( item ) + ";");
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
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM container WHERE id = " + getContainerID( container ) + ";");			
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
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM inventory WHERE id = " + getInventoryID( inventory ) + ";");
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
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM team WHERE id = " + getTeamID( team ) + ";");
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
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM team WHERE id = " + team + ";");
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

		try {
			rs =  statement.executeQuery("SELECT id FROM item;");
			
			int index = 0;
			while( rs.next() ) {
				result.set( index, new ArrayList<String>() );
				ResultSet rs2 = statement.executeQuery("SELECT name FROM itemname WHERE id = " + rs.getLong("id") + ";");
				
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

		try {
			rs =  statement.executeQuery("SELECT name FROM container WHERE inventory = " + getInventoryID( inventory ) + ";");

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

		try {
			ResultSet rs;
			rs = statement.executeQuery("SELECT id FROM item WHERE container = " + getContainerID(container) + ";");

			int index = 0;
			while( rs.next() ) {
				result.add( index, new ArrayList<String>() );
				long id = rs.getLong("id");

				ResultSet rs2;
				rs2 = statement.executeQuery("SELECT name FROM itemname WHERE id = " + id + ";");
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

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM itemname WHERE name = '" + name + "';");
			rs.next();
			long id = rs.getLong("id");

			rs = statement.executeQuery("SELECT name FROM itemname WHERE id = " + id + ";");
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

		try {
			rs =  statement.executeQuery("SELECT * FROM inventory WHERE id = " + getInventoryID( inventory ) + ";");
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

		try {
			rs =  statement.executeQuery("SELECT * FROM container WHERE id = " + getContainerID( container ) + ";");
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
	public Map<String, Object> getItem( String item ){ // Does not play well with aliases
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT * FROM item WHERE id = " + getid( item ) + ";");
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

		try {
			rs = statement.executeQuery("SELECT team FROM inventory WHERE id = " + getInventoryID( inventory ) + ";");
			rs.next();
			owner = rs.getString("team");
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

		try {
			rs = statement.executeQuery("SELECT team FROM container WHERE id = " + getContainerID( container ) + ";");
			rs.next();
			owner = rs.getString("team");
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

		try {
			rs = statement.executeQuery("SELECT team FROM item WHERE id = " + getid( item ) + ";");
			rs.next();
			owner = rs.getString("team");
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

		try {
			rs = statement.executeQuery("SELECT inventory FROM container WHERE id = " + getContainerID( container ) + ";");
			rs.next();
			location = rs.getString("inventory");
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
	public String getItemLocation( String item ) {
		String location = "";
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT container FROM item WHERE id = " + getid( item ) + ";");
			rs.next();
			long locationID = rs.getLong("origincontainer");
			
			ResultSet rs2 = statement.executeQuery("SELECT name FROM container WHERE id = " + locationID + ";");
			rs2.next();
			location = rs2.getString("name");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}
	
	/**
	 * Retrieve the origin container of the given item.
	 * This does not represent the current location of the item.
	 * @param item The name of the item to get the origin container of
	 * @return The origin container of the given item
	 */
	public String getItemOriginLocation( String item ) {
		String location = "";
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT origincontainer FROM item WHERE id = " + getid( item ) + ";");
			rs.next();
			long locationID = rs.getLong("origincontainer");
			
			ResultSet rs2 = statement.executeQuery("SELECT name FROM container WHERE id = " + locationID + ";");
			rs2.next();
			location = rs2.getString("name");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/* Get last edit time */
	
	/**
	 * Retrieves the most recent time that the given inventory was altered
	 * @param inventory The inventory name to fetch the time of
	 * @return The last modified time of the given inventory
	 */
	public long getInventoryTime( String inventory ) {
		long location = -1;
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT time FROM inventory WHERE id = " + getInventoryID( inventory ) + ";");
			rs.next();
			location = rs.getLong("time");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/**
	 * Retrieves the most recent time that the given container was altered
	 * @param container The container name to fetch the time of
	 * @return The last modified time of the given container
	 */
	public long getContainerTime( String container ) {
		long location = -1;
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT time FROM container WHERE id = " + getContainerID( container ) + ";");
			rs.next();
			location = rs.getLong("time");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/**
	 * Retrieves the most recent time that the given item was altered
	 * @param item The item name to fetch the time of
	 * @return The last modified time of the given item
	 */
	public long getItemTime( String item ) {
		long location = -1;
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT time FROM item WHERE id = " + getid( item ) + ";");
			rs.next();
			location = rs.getLong("time");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
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

		try {
			statement.executeUpdate("UPDATE container SET inventory = " + getInventoryID(inventory) + ", time = " + getTime() + " WHERE id = " + getContainerID(container) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Sets the given item's current container to be equal to the given container.
	 * The current container should be condsidered current location of the item.
	 * It does not nessessarily reflect the item's proper storage location.
	 * @param item The name of the item to alter
	 * @param container The container name to set as the current container of the item
	 * @return Whether setting the item comtainer was successful or not
	 * @throws EntryNotExistException Thrown when the given item or container is not found in the database
	 */
	public boolean setItemContainer( String item, String container ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}

		try {
			statement.executeUpdate("UPDATE item SET container = " + getContainerID(container) + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}


		return true;
	}
	
	/**
	 * Sets the given item's original container to be equal to the given container.
	 * The original container should be condsidered the proper place for the item, when stored properly.
	 * It does not nessessarily reflect the item's current location.
	 * @param item The name of the item to alter
	 * @param container The container name to set as the origin of the item
	 * @return Whether setting the item origin was successful or not
	 * @throws EntryNotExistException Thrown when the given item or container is not found in the database
	 */
	public boolean setItemOriginContainer( String item, String container ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET origincontainer = " + getContainerID(container) + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
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

		try {
			statement.executeUpdate("UPDATE item SET team = " + getTeamID(team) + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
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

		try {
			statement.executeUpdate("UPDATE container SET team = " + getTeamID(team) + ", time = " + getTime() + " WHERE id = " + getContainerID(container) + ";");
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

		try {
			statement.executeUpdate("UPDATE item SET team = " + getTeamID(team) + ", time = " + getTime() + " WHERE id = " + getInventoryID(inventory) + ";");
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

		try {
			statement.executeUpdate("UPDATE item SET team = " + team + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
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

		try {
			statement.executeUpdate("UPDATE container SET team = " + team + ", time = " + getTime() + " WHERE id = " + getContainerID(container) + ";");
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

		try {
			statement.executeUpdate("UPDATE item SET team = " + team + ", time = " + getTime() + " WHERE id = " + getInventoryID(inventory) + ";");
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

		try {
			statement.executeUpdate("REMOVE FROM itemname WHERE name = '" + item + "';");
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
	public boolean addItemName( String oldName, String newName ) throws EntryNotExistException {
		if( !itemExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM itemname WHERE name = '" + oldName + "';");
			rs.next();
			long rowID = rs.getLong("id");

			statement.executeUpdate("INSERT INTO itemname ( id, name ) VALUES " + rowID + ", " + newName + ";");
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

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM itemname WHERE name = '" + oldName + "';");
			rs.next();
			long rowID = rs.getLong("id");

			for( String i : newName ) {
				statement.executeUpdate("INSERT INTO itemname ( id, name ) VALUES " + rowID + ", " + i + ";");
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

		try {
			statement.executeUpdate("UPDATE container SET name = '" + newName + "' WHERE id = " + getContainerID(oldName) + ";");
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

		try {
			statement.executeUpdate("UPDATE inventory SET name = '" + newName + "', time = " + getTime() + " WHERE id = " + getInventoryID(oldName) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean setItemLocToDefault( String item ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		}

		try {
			statement.executeUpdate("UPDATE item SET container = " + defaultContainer + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean setContainerLocToDefault( String container ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}

		try {
			statement.executeUpdate("UPDATE container SET inventory = " + defaultInventory + ", time = " + getTime() + " WHERE id = " + getContainerID(container) + ";");
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

		try {
			statement.executeUpdate("UPDATE item SET team = " + defaultTeam + ", time = " + getTime() + " WHERE id = " + getid(item) + ";");
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

		try {
			statement.executeUpdate("UPDATE container SET team = " + defaultTeam + ", time = " + getTime() + " WHERE id = " + getContainerID(container) + ";");
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

		try {
			statement.executeUpdate("UPDATE inventory SET team = " + defaultTeam + ", time = " + getTime() + " WHERE id = " + getInventoryID(inventory) + ";");
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
	 * @param team The team tbat owns the inventory
	 */
	public void newInventory( String name, String team ) {
		try {
			statement.executeUpdate("INSERT INTO inventory ( name, team, time ) VALUES '" + name + "', '" + team + "', " + getTime() + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new container entry, for holding items
	 * @param name The name of the container
	 * @param inventory The inventory that this container resides within
	 * @param team The team that owns the container
	 */
	public void newContainer( String name, String inventory, String team ) {
		try {
			statement.executeUpdate("INSERT INTO container ( name, inventory, team, time ) VALUES '" + name + "', '" + inventory + "', " + team + "', " + getTime() + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new item entry, where the conainer and origin container values are the same
	 * @param name The name of the item
	 * @param container The container the item resides within, same as the origin container
	 * @param team The team that owns the item
	 */
	public void newItem( String name, String container, String team ) {
		newItem( name, container, container, team );
	}

	/**
	 * Creates a new item entry, where the conainer and origin container values are the same
	 * @param names All of the possible names used to refrence the item
	 * @param container The container the item resides within, same as the origin container
	 * @param team The team that owns the item
	 */
	public void newItem( String[] names, String container, String team ) {
		newItem( names, container, container, team );
	}
	
	/**
	 * Creates a new item entry
	 * @param name The name of the item
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team that owns the item
	 */
	public void newItem( String name, String container, String originContainer, String team ) {
		try {
			long time = getTime();
			statement.executeUpdate("INSERT INTO item ( container, origincontainer, team, time ) VALUES " + getContainerID(container) + ", " + getContainerID(originContainer) + ", " + team + "', " + time + ";");
			ResultSet rs = statement.executeQuery("SELECT id FROM item WHERE container = " + container + ", team = " + team + "', time = " + time + ";" );
			rs.next();
			long rowID = rs.getLong("id");
			statement.executeUpdate("INSERT INTO itemname ( id, name ) VALUES " + rowID + ", '" + name + "';");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new item entry
	 * @param names All of the possible names used to refrence the item
	 * @param container The current container the item resides within
	 * @param originContainer The container the item belongs to
	 * @param team The team that owns the item
	 */
	public void newItem( String[] names, String container, String originContainer, String team ) {
		try {
			long rowID = -1;
			ResultSet rs;
			long time = getTime();

			statement.executeUpdate("INSERT INTO item ( container, origincontainer, team, time ) VALUES " + getContainerID(container) + ", " + getContainerID(originContainer) + ", " + team + "', " + time + ";");

			rs = statement.executeQuery("SELECT id FROM item WHERE container = " + getContainerID(container) + ", origincontainer = " + getContainerID(originContainer) + ", team = " + team + "', time = " + time + ";" );
			rs.next();
			rowID = rs.getLong("id");

			for( String name : names ) {
				statement.executeUpdate("INSERT INTO itemname ( id, name ) VALUES " + rowID + ", '" + name + "';");
			}
		} catch( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a new team entry
	 * @param name String representing the team name
	 * @param id long representing the team number
	 */
	public void newTeam( String name, long id ) {
		try {
			statement.executeUpdate("INSERT INTO team ( id, name ) VALUES " + id + ", '" + name + "';");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* PRIVATE HELPER FUNCTIONS */

	private long getTime() {
		return Clock.systemUTC().millis(); // Should use SQLite date and time functions
	}

	private long getInventoryID( String inventory ) { // Should these be public?
		long id = -1;

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM inventory WHERE name = '" + inventory + "';");
			rs.next();
			id = rs.getLong("id");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getContainerID( String container ) {
		long id = -1;

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM container WHERE name = '" + container + "';");
			rs.next();
			id = rs.getLong("id");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getid( String item ) {
		return getItemNameID( item ); // The item name database uses the item id as the key
	}

	private long getTeamID( String team ) {
		long id = -1;

		try {
			ResultSet rs = statement.executeQuery("SELECT id FROM team WHERE name = '" + team + "';");
			rs.next();
			id = rs.getLong("id");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	private long getItemNameID( String item ) {
		ResultSet rs;
		try {
			rs = statement.executeQuery("SELECT id FROM itemname WHERE name = '" + item + "';");
			rs.next();
			return rs.getLong("id");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
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
