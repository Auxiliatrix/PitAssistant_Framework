package processors;

import java.util.ArrayList;
import java.util.Scanner;

import constants.Calibration;
import modules.SearchModule;
import modules.invokers.Invoker;
import objects.Inventory;
import utilities.InventoryLoader;

public class Brain {

	public static Scanner sc = new Scanner(System.in);
	public static InventoryLoader loader = new InventoryLoader();
	
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
			if( !handled ) {
				response = searcher.process(input);
			}
			System.out.println(response);
		}
	}
	
	public static void init() {
		loader.load(Calibration.FILE_NAME);
		loadInvokers();
	}
	
	public static void loadInvokers() {
		// TODO: Add invokers
	}
}
