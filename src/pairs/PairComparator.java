package pairs;

import java.util.Comparator;

public class PairComparator implements Comparator<Pair> {
	
	public PairComparator() {}
	
	@Override
	public int compare(Pair a, Pair b) {
		return a.value > b.value ? 1 : -1;
	}

}
