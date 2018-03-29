package utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import constants.Calibration;

public class LevenshteinDistanceCalculator {
	public LevenshteinDistanceCalculator() {}
	
	public double optimalComparison(String a, String b) {
		double characterComparison = characterCompare(a, b);
		double wordComparison = wordCompare(a, b);
		return Math.min(characterComparison, wordComparison);
	}
	
	public double optimalComparison(String a, ArrayList<String> b) {
		double min = Double.MAX_VALUE;
		for( String s : b ) {
			double value = optimalComparison(a, s);
			min = Math.min(min, value);
		}
		return min;
	}
	
	public double characterCompare(CharSequence a, CharSequence b) {
		int[][] tracker = new int[a.length() + 1][b.length() + 1];
		 
	    for( int f = 0; f < a.length()+1; f++ ) {
	        for( int g = 0; g < b.length()+1; g++ ) {
	            if (f == 0) {
	                tracker[f][g] = g;
	            } else if (g == 0) {
	                tracker[f][g] = f;
	            } else {
	                tracker[f][g] = Math.min(tracker[f-1][g-1] + ((""+a.charAt(f-1)).equalsIgnoreCase(""+b.charAt(g-1)) ? 0 : 1), Math.min(tracker[f-1][g]+1, tracker[f][g-1]+1));
	            }
	        }
	    }
	    return tracker[a.length()][b.length()];
	}
	
	@SuppressWarnings("resource")
	public double wordCompare(String a, String b) {
		Scanner aTokenizer = new Scanner(a);
		Scanner bTokenizer = new Scanner(b);
		aTokenizer.useDelimiter(" |, |,|\n");
		bTokenizer.useDelimiter(" |, |,|\n");
		ArrayList<String> aTokens = new ArrayList<String>();
		ArrayList<String> bTokens = new ArrayList<String>();
		while( aTokenizer.hasNext() ) {
			aTokens.add(aTokenizer.next());
		}
		while( bTokenizer.hasNext() ) {
			bTokens.add(bTokenizer.next());
		}
		
		int count = 0;
		for( String s : aTokens ) {
			if( levenshteinContains(s, bTokens) ) {
				count++;
			}
		}
		return count == 0 ? Double.MAX_VALUE : 1/count;
	}
	
	private boolean levenshteinContains(String string, Collection<String> collection) {
		for( String s : collection ) {
			if( characterCompare(string, s) < Calibration.LEVENSHTEIN_TOLERANCE) {
				return true;
			}
		}
		return false;
	}
}
