package utilities;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.Pair;
import pairs.PairComparator;
import processors.Brain;

public class Search {

	public static LinkedList<Pair> searchInventory( String query ) {
		String[] containers = Brain.data.getContainters(Calibration.Database.INVENTORY); // TODO proper multiple inventory management
		LinkedList<Pair> matches = new LinkedList<Pair>();
		boolean exact = false;
		LinkedList<LinkedList<Pair>> results = new LinkedList<LinkedList<Pair>>();

		for( String container : containers ) {
			LinkedList<Pair> subResults = searchContainer( container, query);

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

		for( LinkedList<Pair> result : results ) {
			matches.addAll(result);
		}

		if( exact ) {
			return matches;
		} else {
			Pair[] pairs = matches.toArray( new Pair[ matches.size() ] );
			Arrays.sort( pairs, new PairComparator()) ;
			return new LinkedList<Pair>( Arrays.asList(pairs) );
		}
	}

	public static LinkedList<Pair> searchContainer( String container, String query ) {
		LinkedList<Pair> matches;
		LinkedList<Pair> exacts;
		LinkedList<Pair> partials;

		exacts = getExactItem( container, query );
		partials = getPartialItems( container, query );
		if( !exacts.isEmpty() ) {
			matches = exacts;
		} else if( !partials.isEmpty() ) {
			matches = partials;
		} else {
			matches = new LinkedList<Pair>();
		}
		return matches;
	}

	public static LinkedList<Pair> getExactItem( String container, String query ) {
		LinkedList<Pair> exacts = new LinkedList<Pair>();
		String[][] itemNames = Brain.data.getItems( container );

		for( String[] i : itemNames ) {
			for( String n : i ) {
				if( n != null && !n.isEmpty() && n.equalsIgnoreCase(query) ) {
					exacts.add( new Pair(n, 0) );
				}
			}
		}
		return exacts;
	}

	public static LinkedList<Pair> getPartialItems( String container, String query ) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		LinkedList<Pair> partials = new LinkedList<Pair>();
		String[][] itemNames = Brain.data.getItems( container );

		for( String[] i : itemNames ) {
			if( i != null && !( i.length == 0 ) ) {
				double distance = ldc.optimalComparison( query, i );

				if( distance <= Calibration.LEVENSHTEIN_TOLERANCE ) {
					partials.add( new Pair( i[0], distance ) ); // Use the first name. Could be better...
				}
			}
		}

		Pair[] pairs = partials.toArray( new Pair[ partials.size() ] );
		Arrays.sort( pairs, new PairComparator() );
		return new LinkedList<Pair>( Arrays.asList(pairs)) ;
	}

	public static ArrayList<Pair> findContainer( String query ) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		String[] containers = Brain.data.getContainters(Calibration.Database.INVENTORY);
		ArrayList<Pair> matches = new ArrayList<Pair>();
		boolean exact = false;
		ArrayList<Pair> results = new ArrayList<Pair>();
		for( String container : containers ) {
			if( !exact ) {
				double distance = ldc.optimalComparison(query, container);
				if( distance == 0 ) {
					exact = true;
					results.clear();
				}
				if( distance < Calibration.LEVENSHTEIN_TOLERANCE ) {
					results.add(new Pair(container, distance));
				}
			} else {
				if( container.equalsIgnoreCase(query) ) {
					results.add(new Pair(container, 0));
				}
			}
		}
		for( Pair result : results ) {
			matches.add(result);
		}
		if( exact ) {
			return matches;
		} else {
			Pair[] pairs = matches.toArray(new Pair[matches.size()]);
			Arrays.sort(pairs, new PairComparator());
			return new ArrayList<Pair>(Arrays.asList(pairs));
		}
	}

}
