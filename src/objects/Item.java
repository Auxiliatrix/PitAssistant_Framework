package objects;

import java.util.ArrayList;

import constants.Calibration;

public class Item {
	public String name;
	public String originalContainer;
	public String originalTeam;
	public ArrayList<String> aliases;
	
	public Item(String name) {
		this.name = name;
		originalContainer = "";
		originalTeam = "";
		aliases = new ArrayList<String>();
		aliases.add(name);
	}

	@Override
	public String toString() {
		return name + " : " + (originalTeam.equals(Calibration.TEAM) ? "in" : "borrowed from") + " " + originalContainer;
	}
}
