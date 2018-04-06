package modules.invokers;

import java.util.ArrayList;
import java.util.LinkedList;

import pairs.Pair;
import processors.Brain;
import utilities.EntryNotExistException;
import utilities.NameExistsException;
import utilities.Search;

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
			LinkedList<Pair> results = Search.searchInventory(tokens[0]); // We could do getFirst here
			if( results.isEmpty() ) {
				response += "No results found for \"" + tokens[0] + "\"\n";
			} else if( results.get(0).value == 0 ) {
				try {
					Brain.data.addItemName(results.get(0).name, tokens[1]);// FIXME This should add an alias, not a primary name 
				} catch (EntryNotExistException | NameExistsException e) {
					e.printStackTrace();
				}
				
				response += "\"" + tokens[1] + "\" added as an alias to \"" + tokens[0] + "\"";
			} else {
				response += "I couldn't find \"" + tokens[0] + "\". Did you mean:\n";
				for( Pair p : results ) {
					lines.add(p.name);
				}
				response += consolidate(lines);
			}
		}
		return response;
	}
}
