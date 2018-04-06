package modules.invokers;

import java.util.ArrayList;

import constants.Calibration;
import pairs.Pair;
import processors.Brain;
import utilities.Search;

public class ListInvoker extends Invoker {

	public ListInvoker() {}
	
	@Override
	public String getInvoker() {
		return "!list";
	}

	@Override
	public String process(String input) {
		String response = "";
		ArrayList<String> lines = new ArrayList<String>();
		if( input.isEmpty() ) {
			response += "These are the containers we have: \n";
			for( String container : Brain.data.getAllContainters() ) {
				response += "* " + container + "\n";
			}
		} else {
			ArrayList<Pair> containers = Search.findContainer(input);
			if( containers.isEmpty() ) {
				response += "I couldn't find anything for " + input + ".\n";
			} else {
				Pair first = containers.get(0);
				if( first.value == 0 ) {
					response += "Here's what is in " + first.name + "\n";
					for( String[] i : Brain.data.getItems(first.name) ) {
						String owner = Brain.data.getItemOwner(i[0]);
						lines.add("* " + i[0] + ( owner.equalsIgnoreCase( Calibration.USER_TEAM_NAME ) ? "" : " borrowed from: " + owner ));
					}
				} else {
					response += "We don't have a container called " + input + ". Did you mean:\n";
					for( Pair container : containers ) {
						response += "* " + container.name + "\n";
					}
				}
			}
		}
		return response + consolidate(lines);
	}

}
