package modules.invokers;

import java.util.ArrayList;

import objects.Container;
import objects.Item;
import pairs.ContainerPair;
import processors.Brain;

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
			for( Container container : Brain.inventory.containers ) {
				response += "* " + container.name + "\n";
			}
		} else {
			ArrayList<ContainerPair> containers = Brain.inventory.containerSearch(input);
			if( containers.isEmpty() ) {
				response += "I couldn't find anything for " + input + ".\n";
			} else {
				ContainerPair first = containers.get(0);
				if( first.value == 0 ) {
					response += "Here's what" + (first.container.originalTeam ? "'s in " : " we borrowed from ") + first.container.name + "\n";
					for( Item i : first.container.items ) {
						lines.add("* " + i.name);
					}
				} else {
					response += "We don't have a container called " + input + ". Did you mean:\n";
					for( ContainerPair container : containers ) {
						response += "* " + container.container.name + "\n";
					}
				}
			}
		}
		return response + consolidate(lines);
	}

}
