package processors;

import java.util.ArrayList;
import java.util.Scanner;

import constants.Calibration;
import modules.Module;
import objects.Inventory;
import pairs.ItemPair;
import utilities.InventoryDatabase;
import utilities.InventoryLoader;

public class Brain {

	public static Scanner sc = new Scanner(System.in);
	public static InventoryLoader loader = new InventoryLoader();
	public static InventoryDatabase data = new InventoryDatabase( Calibration.DATABASE_NAME );
	
	public static Inventory inventory = new Inventory(Calibration.TEAM);
	public static ArrayList<Module> modules = new ArrayList<Module>();
	
	public static void main(String[] args) {
		init();
		while( true ) {
			String input = sc.nextLine();
			boolean handled = false;
			String response = "";
			for( Module m : modules ) {
				if( input.startsWith(m.getInvoker()) ) {
					handled = true;
					response = m.process(input.substring(m.getInvoker().length()+1));
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
				for( ItemPair p : results ) {
					System.out.println("* " + p.item.toString());
				}
			}
		}
	}
	
	public static void init() {
		loader.load(Calibration.FILE_NAME);
		
		// Load database
		if( !data.isInitialized() ) {
			data.init();
		}
		
		loadModules();
	}
	
	public static void loadModules() {
		// TODO: Add modules
	}
}
