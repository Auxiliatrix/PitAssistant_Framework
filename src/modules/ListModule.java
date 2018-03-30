package modules;

import java.util.ArrayList;

import objects.Container;
import pairs.ContainerPair;
import processors.Brain;

public class ListModule implements Module {

	public ListModule() {}
	
	@Override
	public String getInvoker() {
		return "!list";
	}

	@Override
	public String process(String input) {
		String response = "";
		if( input.isEmpty() ) {
			response += "These are the totes we have: \n";
			for( Container container : Brain.inventory.containers ) {
				response += "* " + container.name + "\n";
			}
		}
		ArrayList<ContainerPair> containers = Brain.inventory.containerSearch(input);
		for( ContainerPair c : containers ) {
			response += (c.container.name + "\n");
		}
		return response;
	}

}
