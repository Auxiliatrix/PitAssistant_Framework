package utilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Converter to turn CSV files into an <code>ArrayList</code> of <code>Strings</code>.
 * @author Auxiliatrix
 *
 */
public class CSVReader {
	
	/**
	 * Creates a Reader object.
	 */
	public CSVReader() {}
	
	/**
	 * Converts a text file into an ArrayList of Strings.
	 * @param fileName - String name of the text file to retrieve information from
	 * @return list of Strings for each line in the text file.
	 */
	public ArrayList<String> convert(String fileName) {
		ArrayList<String> lines = new ArrayList<String>();
        String line = null;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            while((line = bufferedReader.readLine()) != null)
            {
                lines.add(line);
            }
        }
        catch (FileNotFoundException E)
        {
            System.err.println("Error: Cannot find file "+fileName);
            return null;
        }
        catch (IOException E)
        {
            return null;
        }
        return lines;
	}
	
	public ArrayList<String> parse(String line) {
		ArrayList<String> tokens = new ArrayList<String>();
		line += " ";
		char[] charArray = line.toCharArray();
		String temp = "";
		boolean openQuote = false;
		boolean openParen = false;
		for( char c : charArray ) {
			if( c == ',' && !openQuote && !openParen ) {
				tokens.add(format(temp));
				temp = "";
			} else if( c == '(' ) {
				openParen = true;
			} else if( c == ')' ) {
				openParen = false;
			} else if( c == '"' && !openParen ) {
				if( openQuote ) {
					openQuote = false;
					tokens.add(format(temp));
					temp = "";
				} else {
					tokens.add(format(temp));
					temp = "";
					openQuote = true;
				}
			} else if( !openParen ) {
				temp += c;
			}
		}
		return tokens;
	}
	
	private String format(String input) {
		while( input.startsWith(" ") ) {
			input = input.substring(1);
		}
		while( input.endsWith(" ") ) {
			input = input.substring(0, input.length()-1);
		}
		return input;
	}
}