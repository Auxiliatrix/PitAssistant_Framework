package processors;

import java.util.ArrayList;
import java.util.Scanner;

import constants.Calibration;
import modules.SearchModule;
import modules.invokers.AliasInvoker;
import modules.invokers.Invoker;
import modules.invokers.ListInvoker;
import utilities.InventoryDatabase;
import utilities.InventoryLoader;

public class Brain {

	public static Scanner sc = new Scanner(System.in);
	public static InventoryLoader loader = new InventoryLoader();
	public static InventoryDatabase data = new InventoryDatabase( Calibration.DATABASE_NAME );

	public static ArrayList<Invoker> invokers = new ArrayList<Invoker>();

	public static SearchModule searcher = new SearchModule();

	public static void main(String[] args) {
		init();
		System.out.println("STATUS: READY\n");
		while( true ) {
			String input = sc.nextLine();
			boolean handled = false;
			String response = "";
			for( Invoker i : invokers ) {
				if( input.startsWith(i.getInvoker()) ) {
					handled = true;
					if( input.length() == i.getInvoker().length() ) {
						response = i.process("");
					} else {
						response = i.process(input.substring(i.getInvoker().length()+1));
					}
				}
			}

			if( !handled ) {
				if( input.startsWith("!") ) {
					response = "Command not recognized.";
				} else {
					response = searcher.process(input);
				}
			}
			
			System.out.println(response);
		}
	}

	private static void init() {
		// Load database
		if( !data.isInitialized() ) {
			System.out.println("Creating Database File...");
			data.init();
		} else {
			System.out.println("Database exists with required table names");
		}
		
		System.out.println("Loading presets...");
		loadStandardData();
		
		System.out.println("Loading inventory...");
		loader.load(Calibration.FILE_NAME);

		System.out.println("Loading invokers...");
		loadInvokers();
	}
	
	public static void loadInvokers() {
		invokers.add(new ListInvoker());
		invokers.add(new AliasInvoker());
	}
	
	private static void loadStandardData() { // Later fetch from TBA
		Brain.data.newInventory("inventory", Calibration.USER_TEAM_NUMBER);
		Brain.data.newTeam("quixilver", 604); // All lower case, don't want any wierdness right now
	}
}
