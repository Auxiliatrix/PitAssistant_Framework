package objects;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.Pair;
import pairs.PairComparator;
import processors.Brain;
import utilities.EntryNotExistException;
import utilities.LevenshteinDistanceCalculator;

public class Container {

	public String name;
	public String originalTeam;

	public Container(String name) {
		this(name, Calibration.TEAM);
	}

	public Container(String name, String originalTeam) {
		this.name = name;
		this.originalTeam = originalTeam;
	}

	public void addOrigin(Item item) {
		if( Brain.data.itemExists( item.name ) ) {
			try {
				Brain.data.setItemOriginContainer( item.name, this.name );
			} catch (EntryNotExistException e) {
				e.printStackTrace();
			}
		} else {
			Brain.data.newItem( item.name, this.name, this.name, this.originalTeam );
			
			try { // Add the other names too
				Brain.data.addItemName( item.name, item.aliases );
			} catch( EntryNotExistException e ) {
				e.printStackTrace();
			}
		}
	}

	public void add(Item item) {
		if( Brain.data.itemExists( item.name ) ) {
			try {
				Brain.data.setItemContainer( item.name, this.name );
			} catch (EntryNotExistException e) {
				e.printStackTrace();
			}
		} else {
			Brain.data.newItem( item.name, this.name, this.name, this.originalTeam );
			
			try { // Add the other names too
				Brain.data.addItemName( item.name, item.aliases );
			} catch( EntryNotExistException e ) {
				e.printStackTrace();
			}
		}
	}

	public ArrayList<Pair> search(String query) {
		ArrayList<Pair> matches;
		ArrayList<Pair> exacts;
		ArrayList<Pair> partials;

		exacts = getExacts( query );
		partials = getPartials( query );
		if( !exacts.isEmpty() ) {
			matches = exacts;
		} else if( !partials.isEmpty() ) {
			matches = partials;
		} else {
			matches = new ArrayList<Pair>();
		}
		return matches;
	}

	public ArrayList<Pair> getExacts( String container, String query ) {
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

	public ArrayList<Pair> getPartials( String container, String query ) {
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
