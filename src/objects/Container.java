package objects;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.ItemPair;
import pairs.ItemPairComparator;
import utilities.LevenshteinDistanceCalculator;

public class Container {
	
	public String name;
	public String originalTeam;
	public ArrayList<Item> items;
	
	public Container(String name) {
		this(name, Calibration.TEAM);
	}
	
	public Container(String name, String originalTeam) {
		this.name = name;
		this.originalTeam = originalTeam;
		items = new ArrayList<Item>();
	}
	
	public void addOrigin(Item item) {
		item.originalContainer = this.name;
		item.originalTeam = this.originalTeam;
		items.add(item);
	}
	
	public void add(Item item) {
		items.add(item);
	}
	
	public ArrayList<ItemPair> search(String query) {
		ArrayList<ItemPair> matches;
		ArrayList<ItemPair> exacts;
		ArrayList<ItemPair> partials;
		
		exacts = getExacts( query );
		partials = getPartials(query);
		if( !exacts.isEmpty() ) {
			matches = exacts;
		} else if( !partials.isEmpty() ) {
			matches = partials;
		} else {
			matches = new ArrayList<ItemPair>();
		}
		return matches;
	}
	
	public ArrayList<ItemPair> getExacts(String query) {
		ArrayList<ItemPair> exacts = new ArrayList<ItemPair>();
		for( Item i : items ) {
			if( i.name.equalsIgnoreCase(query) ) {
				exacts.add(new ItemPair(i, 0));
			}
		}
		return exacts;
	}
	
	public ArrayList<ItemPair> getPartials(String query) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<ItemPair> partials = new ArrayList<ItemPair>();
		for( Item i : items ) {
			double distance = ldc.optimalComparison(query, i.aliases);
			
			if( distance <= Calibration.LEVENSHTEIN_TOLERANCE ) {
				partials.add(new ItemPair(i, distance));
			}
		}
		ItemPair[] pairs = partials.toArray(new ItemPair[partials.size()]);
		Arrays.sort(pairs, new ItemPairComparator());
		return new ArrayList<ItemPair>(Arrays.asList(pairs));
	}
	
}
