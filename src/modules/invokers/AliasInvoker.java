package modules.invokers;

import java.util.ArrayList;

import pairs.ItemPair;
import processors.Brain;

public class AliasInvoker extends Invoker {

	public AliasInvoker() {}
	
	@Override
	public String getInvoker() {
		return "!alias";
	}

	@Override
	public String process(String input) {
		String response = "";
		String[] tokens = input.split(", ");
		if( tokens.length < 2 ) {
			response += "You need to give me an item name, and then a nickname.\n";
		} else {
			ArrayList<String> lines = new ArrayList<String>();
			ArrayList<ItemPair> results = Brain.inventory.search(tokens[0]);
			if( results.isEmpty() ) {
				response += "No results found for \"" + tokens[0] + "\"\n";
			} else if( results.get(0).value == 0 ) {
				results.get(0).item.aliases.add(tokens[1]);
				response += "\"" + tokens[1] + "\" added as an alias to \"" + tokens[0] + "\"";
			} else {
				response += "I couldn't find \"" + tokens[0] + "\". Did you mean:\n";
				for( ItemPair p : results ) {
					lines.add(p.item.toString());
				}
				response += consolidate(lines);
			}
		}
		return response;
	}
}
