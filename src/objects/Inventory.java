package objects;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.Pair;
import pairs.PairComparator;
import utilities.LevenshteinDistanceCalculator;

@Deprecated
public class Inventory {
	
	public ArrayList<Container> containers;
	
	public Inventory() {
		containers = new ArrayList<Container>();
	}
	
	public void addOrigin(Container container) {
		container.originalTeam = true;
		containers.add(container);
	}
	
	public void add(Container container) {
		containers.add(container);
	}
	
	public ArrayList<Pair> search(String query) {
		ArrayList<Pair> matches = new ArrayList<Pair>();
		boolean exact = false;
		ArrayList<ArrayList<Pair>> results = new ArrayList<ArrayList<Pair>>();
		for( Container container : containers ) {
			ArrayList<Pair> subResults = container.search(query);
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
	
	public ArrayList<Pair> searchExacts(String query) {
		ArrayList<Pair> matches = new ArrayList<Pair>();
		for( Container c : containers ) {
			for( Item i : c.items ) {
				if( i.name.equalsIgnoreCase(query) ) {
					matches.add( new Pair(i.name, 0) );
				}
			}
		}
		return matches;
	}
	
	public ArrayList<Pair> containerSearch (String query) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<Pair> matches = new ArrayList<Pair>();
		boolean exact = false;
		ArrayList<Pair> results = new ArrayList<Pair>();
		for( Container container : containers ) {
			if( !exact ) {
				double distance = ldc.optimalComparison(query, container.name);
				if( distance == 0 ) {
					exact = true;
					results.clear();
				}
				if( distance < Calibration.LEVENSHTEIN_TOLERANCE ) {
					results.add(new Pair(container.name, distance));
				}
			} else {
				if( container.name.equalsIgnoreCase(query) ) {
					results.add(new Pair(container.name, 0));
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
