package modules;

import java.util.ArrayList;
import java.util.HashMap;

import processors.Brain;
import utilities.InventoryDatabase;

public class Module {
	
	private InventoryDatabase data = Brain.data;
	
	public Module() {}
	
	public String process(String input) {
		return null;
	}
	
	protected String consolidate(ArrayList<String> lines) {
		String ret = "";
		HashMap<String, Integer> countMap = new HashMap<String, Integer>();
		ArrayList<String> order = new ArrayList<String>();
		while( !lines.isEmpty() ) {
			String line = lines.remove(0);
			if( countMap.containsKey(line) ) {
				countMap.put(line, countMap.get(line)+1);
			} else {
				countMap.put(line, 1);
				order.add(line);
			}
		}
		for( String line : order ) {
			ret += "* " + countMap.get(line) + "x " + line + "\n";
		}
		return ret;
	}
}
