package modules;

import java.util.ArrayList;
import java.util.LinkedList;

import pairs.Pair;
import utilities.Search;

public class SearchModule extends Module {

	public SearchModule() {}
	
	@Override
	public String process( String input ) {
		String response = "";
		ArrayList<String> lines = new ArrayList<String>();
		LinkedList<Pair> results = Search.searchInventory( input );

		if( results.isEmpty() ) {
			response += "No results found.\n";
		} else if( results.get(0).value == 0 ) {
			response += "Here's what I found:\n";
		} else {
			response += "I couldn't find \"" + input + "\". Did you mean:\n";
		}

		for( Pair p : results ) {
			lines.add( p.name );
		}
		
		return response + consolidate(lines);
	}

}
