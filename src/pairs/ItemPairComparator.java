package pairs;

import java.util.Comparator;

public class ItemPairComparator implements Comparator<ItemPair> {

	public ItemPairComparator() {}
	
	@Override
	public int compare(ItemPair a, ItemPair b) {
		return a.value > b.value ? 1 : -1;
	}
	
}
