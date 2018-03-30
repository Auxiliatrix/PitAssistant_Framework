package modules;

import java.util.ArrayList;

import pairs.ItemPair;
import processors.Brain;

public class SearchModule extends Module {

	public SearchModule() {}
	
	@Override
	public String process(String input) {
		String response = "";
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<ItemPair> results = Brain.inventory.search(input);
		if( results.isEmpty() ) {
			response += "No results found.\n";
		} else if( results.get(0).value == 0 ) {
			response += "Here's what I found:\n";
		} else {
			response += "I couldn't find \"" + input + "\". Did you mean:\n";
		}
		for( ItemPair p : results ) {
			lines.add(p.item.toString());
		}
		return response + consolidate(lines);
	}
	
}
