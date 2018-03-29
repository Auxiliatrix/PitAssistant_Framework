package objects;

import java.util.ArrayList;
import java.util.Arrays;

import constants.Calibration;
import pairs.ContainerPair;
import pairs.ContainerPairComparator;
import pairs.ItemPair;
import pairs.ItemPairComparator;
import utilities.LevenshteinDistanceCalculator;

public class Inventory {
	
	private ArrayList<Container> containers;
	private String originalTeam;
	
	public Inventory(String team) {
		containers = new ArrayList<Container>();
		this.originalTeam = team;
	}
	
	public void addOrigin(Container container) {
		container.originalTeam = this.originalTeam;
		containers.add(container);
	}
	
	public void add(Container container) {
		containers.add(container);
	}
	
	public ArrayList<ItemPair> search(String query) {
		ArrayList<ItemPair> matches = new ArrayList<ItemPair>();
		boolean exact = false;
		ArrayList<ArrayList<ItemPair>> results = new ArrayList<ArrayList<ItemPair>>();
		for( Container container : containers ) {
			ArrayList<ItemPair> subResults = container.search(query);
			if( !subResults.isEmpty() ) {
				if( !exact ) {
					if( subResults.get(0).value == 0 ) {
						exact = true;
						results.clear();
					}
					results.add(subResults);
				} else {
					if( subResults.get(0).value == 0 ) {
						results.add(subResults);
					}
				}
			}
		}
		for( ArrayList<ItemPair> result : results ) {
			matches.addAll(result);
		}
		if( exact ) {
			return matches;
		} else {
			ItemPair[] pairs = matches.toArray(new ItemPair[matches.size()]);
			Arrays.sort(pairs, new ItemPairComparator());
			return new ArrayList<ItemPair>(Arrays.asList(pairs));
		}
	}
	
	
	public ArrayList<ContainerPair> surfaceSearch (String query) {
		ArrayList<ContainerPair> matches;
		ArrayList<ContainerPair> exacts;
		ArrayList<ContainerPair> partials;
		
		exacts = getExacts( query );
		partials = getPartials(query);
		if( !exacts.isEmpty() ) {
			matches = exacts;
		} else if( !partials.isEmpty() ) {
			matches = partials;
		} else {
			matches = new ArrayList<ContainerPair>();
		}
		return matches;
	}
	
	private ArrayList<ContainerPair> getExacts(String query) {
		ArrayList<ContainerPair> exacts = new ArrayList<ContainerPair>();
		for( Container c : containers ) {
			if( c.name.equalsIgnoreCase(query) ) {
				exacts.add(new ContainerPair(c, 0));
			}
		}
		return exacts;
	}
	
	private ArrayList<ContainerPair> getPartials(String query) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<ContainerPair> partials = new ArrayList<ContainerPair>();
		for( Container c : containers ) {
			double distance = ldc.optimalComparison(query, c.name);
			if( distance <= Calibration.LEVENSHTEIN_TOLERANCE ) {
				partials.add(new ContainerPair(c, distance));
			}
		}
		ContainerPair[] pairs = partials.toArray(new ContainerPair[partials.size()]);
		Arrays.sort(pairs, new ContainerPairComparator());
		return new ArrayList<ContainerPair>(Arrays.asList(pairs));
	}
}
