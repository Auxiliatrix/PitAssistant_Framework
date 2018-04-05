package objects;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.Pair;
import pairs.PairComparator;
import processors.Brain;
import utilities.EntryNotExistException;
import utilities.LevenshteinDistanceCalculator;
import utilities.LocationsExceedItemsException;
import utilities.NameExistsException;

@Deprecated
public class Container {

	public String name;

	public boolean originalTeam; // boolean
	public ArrayList<Item> items;
	
	public Container(String name) {
		this(name, false);
	}
	
	public Container(String name, boolean originalTeam) {
		this.name = name;
		this.originalTeam = originalTeam;
	}

	public void addOrigin(Item item) {
		if( Brain.data.itemExists( item.name ) ) {
			try {
				Brain.data.addItemOriginContainer( item.name, this.name );
			} catch (EntryNotExistException e) {
				e.printStackTrace();
			} catch (LocationsExceedItemsException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Brain.data.newItem( item.name, this.name, this.name );
			} catch (NameExistsException e1) {
				e1.printStackTrace();
			}
			
			try { // Add the other names too
				Brain.data.addItemName( item.name, item.aliases );
			} catch( EntryNotExistException e ) {
				e.printStackTrace();
			}
		}
		item.originalContainer = this.name;
		item.originalTeam = this.originalTeam;
		add(item);
	}

	public void add(Item item) {
		if( Brain.data.itemExists( item.name ) ) {
			try {
				Brain.data.addItemContainer( item.name, this.name );
			} catch (EntryNotExistException e) {
				e.printStackTrace();
			} catch (LocationsExceedItemsException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Brain.data.newItem( item.name, this.name, this.name );
			} catch (NameExistsException e1) {
				e1.printStackTrace();
			}
			
			try { // Add the other names too
				Brain.data.addItemName( item.name, item.aliases );
			} catch( EntryNotExistException e ) {
				e.printStackTrace();
			}
		}

		item.currentContainer = this.name;
		items.add(item);
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

	public ArrayList<Pair> getExacts( String query ) {
		ArrayList<Pair> exacts = new ArrayList<Pair>();
		String[][] itemNames = Brain.data.getItems( name );
		for( String[] i : itemNames ) {
			for( String n : i ) {
				if( n.equalsIgnoreCase(query) ) {
					exacts.add( new Pair(n, 0) );
				}
			}
		}
		return exacts;
	}

	public ArrayList<Pair> getPartials( String query ) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<Pair> partials = new ArrayList<Pair>();
		String[][] itemNames = Brain.data.getItems( name );

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
