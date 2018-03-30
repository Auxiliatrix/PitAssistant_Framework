package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;

import constants.Calibration;

public class InventoryDatabase {
	private static SimpleDateFormat sdf = new SimpleDateFormat( Calibration.DATEFORMAT );

	String name;
	Connection connection = null;
	Statement statement = null;

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
	
	/* Create the database */
	
	public boolean init() {
		// Adds required tables and columns to the database
		/*
		 * TABLES:
		 * Item
		 *   Time added / modified
		 *   Name
		 *   ID
		 *   Container
		 *   Owner
		 *   Location
		 * Container
		 *   Time added / modified
		 *   Name
		 *   ID
		 *   Inventory
		 * Inventory
		 *   Time added / modified
		 *   Name
		 *   ID
		 * Group
		 *   ID
		 *   Name
		 *   Time added / modified
		 */
		try {
			statement.executeUpdate( "PRAGMA foreign_keys = ON ;" );
			
			/* Create nessessary tables */
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS team ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS inventory ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "time integer NOT NULL );" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS container ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "inventory integer NOT NULL, time integer NOT NULL);" );
			statement.executeUpdate( "CREATE TABLE IF NOT EXISTS item ( id integer PRIMARY KEY NOT NULL, name text NOT NULL, "
					+ "container integer NOT NULL, owner integer NOT NULL, location text, time integer NOT NULL, "
					+ "FOREIGN KEY (container) REFERENCES container(id), FOREIGN KEY (owner) REFERENCES team(id) );" );
			
			/* Add default values to tables */
			statement.executeUpdate("INSERT INTO team ( name, time ) VALUES ('default', " + getTime() + ");"); // Create default team
			statement.executeUpdate("INSERT INTO inventory ( name, time ) VALUES ('default', " + getTime() + ");"); // Create default inventory
			ResultSet result = statement.executeQuery("SELECT id FROM inventory WHERE name = 'default';"); // Get the id of the default inventory
			statement.executeUpdate("INSERT INTO container ( name, inventory, time ) VALUES ('default', " + result.getInt("id") + ", " + getTime() + ");"); // Create default container

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		
		return true;
	}
	
	/* Use the database */
	
	/* Private helper functions */
	
	private long getTime() {
		return Clock.systemUTC().millis(); // Should use SQLite date and time functions
	}
}
