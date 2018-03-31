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
	
	public ArrayList<Container> containers;
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
	
	
	public ArrayList<ContainerPair> containerSearch (String query) {
		LevenshteinDistanceCalculator ldc = new LevenshteinDistanceCalculator();
		ArrayList<ContainerPair> matches = new ArrayList<ContainerPair>();
		boolean exact = false;
		ArrayList<ContainerPair> results = new ArrayList<ContainerPair>();
		for( Container container : containers ) {
			if( !exact ) {
				double distance = ldc.optimalComparison(query, container.name);
				if( distance == 0 ) {
					exact = true;
					results.clear();
				}
				if( distance < Calibration.LEVENSHTEIN_TOLERANCE ) {
					results.add(new ContainerPair(container, distance));
				}
			} else {
				if( container.name.equalsIgnoreCase(query) ) {
					results.add(new ContainerPair(container, 0));
				}
			}
		}
		for( ContainerPair result : results ) {
			matches.add(result);
		}
		if( exact ) {
			return matches;
		} else {
			ContainerPair[] pairs = matches.toArray(new ContainerPair[matches.size()]);
			Arrays.sort(pairs, new ContainerPairComparator());
			return new ArrayList<ContainerPair>(Arrays.asList(pairs));
		}
	}
}
