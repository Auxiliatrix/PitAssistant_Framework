package utilities;

import java.util.ArrayList;
import java.util.HashMap;

import constants.Calibration;
import objects.Container;
import objects.Item;
import processors.Brain;

public class InventoryLoader {
	
	public InventoryLoader() {}
	
	public void load(String fileName) {
		String[][] tokenMatrix = convert(fileName);
		String[] containerDeclarations = tokenMatrix[0];
		HashMap<Integer, Container> containers = new HashMap<Integer, Container>();
		for( int f=0; f<containerDeclarations.length; f++ ) {
			String token = containerDeclarations[f];
			if( !token.isEmpty() && !token.equalsIgnoreCase("quantity")) {
				containers.put(f, new Container(token));
			}
		}
		for( int f=1; f<tokenMatrix.length; f++ ) {
			String[] tokens = tokenMatrix[f];
			for( int g=0; g<tokens.length; g+=2 ) {
				String item = tokens[g];
				if( !item.isEmpty() ) {
					String quantityString = tokens[g+1];
					int quantity = quantityString.isEmpty() ? 1 : Integer.parseInt(quantityString);
					for( int h=0; h<quantity; h++ ) {
						containers.get(g).addOrigin(new Item(item));
					}
				}
			}
		}
		for( Container container : containers.values() ) {
			Brain.inventory.addOrigin(container);
		}
	}
	
	private String[][] convert(String fileName) {
		CSVReader csvr = new CSVReader();
		ArrayList<String> lines = csvr.convert(fileName);
		ArrayList<ArrayList<String>> tokens = new ArrayList<ArrayList<String>>();
		for( String line : lines ) {
			for( String target : Calibration.REPLACEMENTS.keySet() ) {
				line = line.replace(target, Calibration.REPLACEMENTS.get(target));
			}
			tokens.add(csvr.parse(line));
		}
		String[][] tokenMatrix = new String[tokens.size()][tokens.get(0).size()];
		for( int f=0; f<tokens.size(); f++ ) {
			ArrayList<String> tokenList = tokens.get(f);
			String[] tokenArray = new String[tokenList.size()];
			for( int g=0; g<tokenList.size(); g++ ) {
				tokenArray[g] = tokenList.get(g);
			}
			tokenMatrix[f] = tokenArray;
		}
		return tokenMatrix;
	}
	
}
