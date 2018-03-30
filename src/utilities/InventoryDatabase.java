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
	 * Item
	 *   Time added / modified
	 *   Name
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

		this.name = name;

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

	public boolean backup() {
		/* Useful if using a memory database */
		return backup( Calibration.BACKUP_DATABASE );
	}

	public boolean backup( String file ) {
		try {
			statement.executeUpdate( "backup to " + file );
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean restore() {
		return restore( Calibration.BACKUP_DATABASE );
	}

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

	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/* CREATE DATABASE */

	public boolean init() {
		// Adds required tables and columns to the database
		try {
			// Require that key relations are good
			statement.executeUpdate( "PRAGMA foreign_keys = ON ;" );

			/* Create nessessary tables */
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS team ( id integer NOT NULL, name text NOT NULL, "
					+ "team integer NOT NULL, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS inventory ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "team integer NOT NULL, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS container ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "inventory integer NOT NULL, team integer NOT NULL, time integer NOT NULL);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS item ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "container integer NOT NULL, owner integer NOT NULL, location text, time integer NOT NULL, "
					+ "FOREIGN KEY (container) REFERENCES container(id), FOREIGN KEY (owner) REFERENCES team(id) );" );

			/* Add default values to tables */
			ResultSet rs;
			statement.executeUpdate("INSERT INTO team ( name, time ) VALUES ('default', " + getTime() + ");"); // Create default team
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
	public boolean itemExists( String item ) {
		try {
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM item WHERE id = " + getItemID( item ) + ";");
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean containerExists( String container ) {
		try {
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM container WHERE id = " + getContainerID( container ) + ";");			
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean inventoryExists( String inventory ) {
		try {
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM inventory WHERE id = " + getInventoryID( inventory ) + ";");
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean teamExists( String team ) {
		try {
			ResultSet rs = statement.executeQuery("SELECT count(*) FROM team WHERE id = " + getTeamID( team ) + ";");
			return (rs.getMetaData().getColumnCount() == 0 ? true : false );

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
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

	public String[] getAllItems() {
		List<String> result = new ArrayList<String>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT name FROM item;");

			while( rs.next() ) {
				result.add( rs.getString("name") );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result.toArray( new String[ result.size() ] );
	}

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

	public String[] getItems( String container ) {
		List<String> result = new ArrayList<String>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT name FROM item WHERE container = " + getContainerID( container ) + ";");

			while( rs.next() ) {
				result.add( rs.getString("name") );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result.toArray( new String[ result.size() ] );
	}

	/* Get all info about the thing */
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

	public Map<String, Object> getItem( String item ){
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSet rs;

		try {
			rs =  statement.executeQuery("SELECT * FROM item WHERE id = " + getItemID( item ) + ";");
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

	public String getItemOwner( String item ) {
		String owner = "";
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT team FROM item WHERE id = " + getItemID( item ) + ";");
			rs.next();
			owner = rs.getString("team");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return owner;
	}

	/* Get where the object is */
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

	public String getItemLocation( String item ) {
		String location = "";
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT container FROM item WHERE id = " + getItemID( item ) + ";");
			rs.next();
			location = rs.getString("inventory");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/* Get last edit time */
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

	public long getItemTime( String item ) {
		long location = -1;
		ResultSet rs;

		try {
			rs = statement.executeQuery("SELECT time FROM item WHERE id = " + getItemID( item ) + ";");
			rs.next();
			location = rs.getLong("time");
		} catch( SQLException e ) {
			e.printStackTrace();
		}

		return location;
	}

	/* SET DATABASE VALUES */

	public boolean setContainerInventory( String container, String inventory ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		}
		
		try {
			statement.executeUpdate("UPDATE container SET inventory = " + getInventoryID(inventory) + " WHERE id = " + getContainerID(container) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setItemContainer( String item, String container ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET container = " + getContainerID(container) + " WHERE id = " + getItemID(item) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		
		return true;
	}

	public boolean setItemOwner( String item, String team ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET team = " + getTeamID(team) + " WHERE id = " + getItemID(item) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setContainerOwner( String container, String team ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}
		
		try {
			statement.executeUpdate("UPDATE container SET team = " + getTeamID(team) + " WHERE id = " + getContainerID(container) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setInventoryOwner( String inventory, String team ) throws EntryNotExistException {
		if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		} else if( !teamExists( team ) ) {
			throw new EntryNotExistException( team );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET team = " + getTeamID(team) + " WHERE id = " + getInventoryID(inventory) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setItemName( String oldName, String newName ) throws EntryNotExistException {
		if( !itemExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET name = '" + newName + "' WHERE id = " + getItemID(oldName) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

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

	public boolean setInventoryName( String oldName, String newName ) throws EntryNotExistException {
		if( !inventoryExists( oldName ) ) {
			throw new EntryNotExistException( oldName );
		}
		
		try {
			statement.executeUpdate("UPDATE inventory SET name = '" + newName + "' WHERE id = " + getInventoryID(oldName) + ";");
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
			statement.executeUpdate("UPDATE item SET container = " + defaultContainer + " WHERE id = " + getItemID(item) + ";");
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
			statement.executeUpdate("UPDATE container SET inventory = " + defaultInventory + " WHERE id = " + getContainerID(container) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setItemTeamToDefault( String item ) throws EntryNotExistException {
		if( !itemExists( item ) ) {
			throw new EntryNotExistException( item );
		}
		
		try {
			statement.executeUpdate("UPDATE item SET team = " + defaultTeam + " WHERE id = " + getItemID(item) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setContainerTeamToDefault( String container ) throws EntryNotExistException {
		if( !containerExists( container ) ) {
			throw new EntryNotExistException( container );
		}
		
		try {
			statement.executeUpdate("UPDATE container SET team = " + defaultTeam + " WHERE id = " + getContainerID(container) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean setInventoryTeamToDefault( String inventory ) throws EntryNotExistException {
		if( !inventoryExists( inventory ) ) {
			throw new EntryNotExistException( inventory );
		}
		
		try {
			statement.executeUpdate("UPDATE inventory SET team = " + defaultTeam + " WHERE id = " + getInventoryID(inventory) + ";");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/* CREATE OBJECTS */

	public void newInventory( String name, String team ) {
		try {
			statement.executeUpdate("INSERT INTO inventory ( name, team, time ) VALUES '" + name + "', '" + team + "', " + getTime() + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void newContainer( String name, String inventory, String team ) {
		try {
			statement.executeUpdate("INSERT INTO container ( name, inventory, team, time ) VALUES '" + name + "', '" + inventory + "', " + team + "', " + getTime() + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void newItem( String name, String container, String team ) {
		try {
			statement.executeUpdate("INSERT INTO item ( name, container, team, time ) VALUES '" + name + "', '" + container + "', " + team + "', " + getTime() + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void newItem( String name, String container, String location, String team ) {

	}

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
		return 0;
	}

	private long getContainerID( String container ) {
		return 0;
	}

	private long getItemID( String item ) {
		return 0;
	}
	
	private long getTeamID( String team ) {
		return 0;
	}
}
