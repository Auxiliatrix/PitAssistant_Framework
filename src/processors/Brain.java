package processors;

import java.util.ArrayList;
import java.util.Scanner;

import constants.Calibration;
import modules.SearchModule;
import modules.invokers.Invoker;
import objects.Inventory;
import pairs.Pair;
import utilities.InventoryDatabase;
import utilities.InventoryLoader;

public class Brain {

	public static Scanner sc = new Scanner(System.in);
	public static InventoryLoader loader = new InventoryLoader();
	public static InventoryDatabase data = new InventoryDatabase( Calibration.DATABASE_NAME );

	public static Inventory inventory = new Inventory();
	public static ArrayList<Invoker> invokers = new ArrayList<Invoker>();

	public static SearchModule searcher = new SearchModule();

	public static void main(String[] args) {
		init();
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

			if( handled ) {
				System.out.println(response);
			} else {
				ArrayList<Pair> results = inventory.search(input);
				if( results.isEmpty() ) {
					System.out.println("No results found.");
				} else if( results.get(0).value == 0 ) {
					System.out.println("Here's what I found: ");
				} else {
					System.out.println("I couldn't find \"" + input + "\". Did you mean: " );
				}
				for( Pair p : results ) {
					System.out.println("* " + p.name);
				}

				if( !handled ) {
					response = searcher.process(input);
				}
				System.out.println(response);
			}
		}
	}

	private static void init() {
		// Load database
		if( !data.isInitialized() ) {
			System.out.println("Creating Database File...");
			data.init();
			System.out.println("Done.");
		}
		
		loadStandardData();
		
		loader.load(Calibration.FILE_NAME);

		loadInvokers();
	}

	private static void loadInvokers() {
		// TODO: Add invokers
	}
	
	private static void loadStandardData() { // Later fetch from TBA
		Brain.data.newInventory("inventory", Calibration.INITIAL_OBJECT_OWNER);
		Brain.data.newTeam("quixilver", 604); // All lower case, don't want any wierdness right now
	}
}
