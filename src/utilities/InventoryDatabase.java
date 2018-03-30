package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;
import java.util.Map;

import constants.Calibration;

public class InventoryDatabase {
	private static SimpleDateFormat sdf = new SimpleDateFormat( Calibration.DATEFORMAT );

	private String name;
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
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS team ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "team integer NOT NULL, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS inventory ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "team integer NOT NULL, time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS container ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "inventory integer NOT NULL, team integer NOT NULL, time integer NOT NULL);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS item ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "container integer NOT NULL, owner integer NOT NULL, location text, time integer NOT NULL, "
					+ "FOREIGN KEY (container) REFERENCES container(id), FOREIGN KEY (owner) REFERENCES team(id) );" );

			/* Add default values to tables */
			statement.executeUpdate("INSERT INTO team ( name, time ) VALUES ('default', " + getTime() + ");"); // Create default team
			defaultTeam = statement.executeQuery("SELECT id FROM team WHERE name = 'default';").getLong("id"); // ID of default team

			statement.executeUpdate("INSERT INTO inventory ( name, team, time ) VALUES ('default', " + defaultTeam + ", " + getTime() + ");"); // Create default inventory
			defaultInventory = statement.executeQuery("SELECT id FROM inventory WHERE name = 'default';").getLong("id"); // Get the id of the default inventory

			statement.executeUpdate("INSERT INTO container ( name, inventory, team, time ) VALUES ('default', " + defaultInventory + ", " + defaultTeam + ", " + getTime() + ");"); // Create default container
			defaultContainer = statement.executeQuery("SELECT id FROM container WHERE name = 'default';").getLong("id"); // Get the id of the default container

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/* QUERY DATABASE */

	/* Get all subvalues of database */
	public String getInventories() {
		return "";
	}

	public String getContainters( String inventory ) {
		return "";
	}

	public String getItems( String container ) {
		return "";
	}

	/* Get all info about the thing */
	public Map<String, Object> getInventory( String inventory ){
		return null;
	}

	public Map<String, Object> getContainer( String container ){
		return null;
	}

	public Map<String, Object> getItem( String item ){
		return null;
	}
	
	/* Get who owns the object */
	public String getInventoryOwner( String inventory ) {
		return "";
	}
	
	public String getContainerOwner( String container ) {
		return "";
	}
	
	public String getItemOwner( String item ) {
		return "";
	}
	
	/* Get where the object is */
	public String getContainerLocation( String container ) {
		return "";
	}
	
	public String getItemLocation( String item ) {
		return "";
	}
	
	/* Get last edit time */
	public long getInventoryTime( String inventory ) {
		return 0;
	}

	public long getContainerTime( String container ) {
		return 0;
	}
	
	public long getItemTime( String item ) {
		return 0;
	}
	
	/* PRIVATE HELPER FUNCTIONS */

	private long getTime() {
		return Clock.systemUTC().millis(); // Should use SQLite date and time functions
	}
}
