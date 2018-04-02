package modules;

import processors.Brain;
import utilities.InventoryDatabase;

public interface Module {
	public String getInvoker();
	public String process(String input);
	InventoryDatabase data = Brain.data;
}
