package modules;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.Pair;
import pairs.PairComparator;
import processors.Brain;
import utilities.LevenshteinDistanceCalculator;

public class SearchModule extends Module {

	public SearchModule() {}
	
	@Override
	public String process( String input ) {
		String response = "";
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<Pair> results = searchInventory( input );

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
	
	private ArrayList<Pair> searchInventory( String query ) {
		String[] containers = Brain.data.getAllContainters();
		ArrayList<Pair> matches = new ArrayList<Pair>();
		boolean exact = false;
		ArrayList<ArrayList<Pair>> results = new ArrayList<ArrayList<Pair>>();
		for( String container : containers ) {
			ArrayList<Pair> subResults = searchContainer( container, query);
			if( !subResults.isEmpty() ) {
				if( !exact ) {
					if( subResults.get(0).value == 0 ) { // A perfect match
						exact = true; // Only add perfect values
						results.clear(); // Remove all the worse matches
					}
					results.add(subResults);
				} else {
					if( subResults.get(0).value == 0 ) {
						results.add(subResults);
					}
				}
			}
		}
		for( ArrayList<Pair> result : results ) {
			matches.addAll(result);
		}
		if( exact ) {
			return matches;
		} else {
			Pair[] pairs = matches.toArray( new Pair[ matches.size() ] );
			Arrays.sort( pairs, new PairComparator()) ;
			return new ArrayList<Pair>( Arrays.asList(pairs) );
		}
	}
	
	private ArrayList<Pair> searchContainer( String container, String query ) {
		ArrayList<Pair> matches;
		ArrayList<Pair> exacts;
		ArrayList<Pair> partials;

		exacts = getExactItem( container, query );
		partials = getPartialItems( container, query );
		if( !exacts.isEmpty() ) {
			matches = exacts;
		} else if( !partials.isEmpty() ) {
			matches = partials;
		} else {
			matches = new ArrayList<Pair>();
		}
		return matches;
	}

	private ArrayList<Pair> getExactItem( String container, String query ) {
		ArrayList<Pair> exacts = new ArrayList<Pair>();
		String[][] itemNames = Brain.data.getItems( container );
		for( String[] i : itemNames ) {
			for( String n : i ) {
				if( n.equalsIgnoreCase(query) ) {
					exacts.add( new Pair(n, 0) );
				}
			}
		}
		return exacts;
	}

	private ArrayList<Pair> getPartialItems( String container, String query ) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<Pair> partials = new ArrayList<Pair>();
		String[][] itemNames = Brain.data.getItems( container );

		for( String[] i : itemNames ) {
			double distance = ldc.optimalComparison( query, i );

			if( distance <= Calibration.LEVENSHTEIN_TOLERANCE ) {
				partials.add( new Pair( i[0], distance ) ); // Use the first name. Could be better...
			}
		}

		Pair[] pairs = partials.toArray( new Pair[ partials.size() ] );
		Arrays.sort( pairs, new PairComparator() );
		return new ArrayList<Pair>( Arrays.asList(pairs)) ;
	}
}
