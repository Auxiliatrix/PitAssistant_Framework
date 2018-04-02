package objects;

import java.util.ArrayList;

@Deprecated
public class Item {
	public String name;
	public String originalContainer;
	public String currentContainer;
	public boolean originalTeam;
	public ArrayList<String> aliases;
	
	public Item(String name) {
		this.name = name;
		originalContainer = "";
		originalTeam = false;
		aliases = new ArrayList<String>();
		aliases.add(name);
	}

	@Override
	public String toString() {
		String ret = "";
		ret += name + " : ";
		if( originalTeam ) {
			if( currentContainer.equals(originalContainer) ) {
				ret += " in " + currentContainer;
			} else {
				ret += " borrowed from " + originalContainer;
			}
		} else {
			if( currentContainer.equals(originalContainer) ) {
				ret += " at " + currentContainer;
			} else {
				ret += " lent to " + currentContainer;
			}
		}
		return ret;
	}
}
