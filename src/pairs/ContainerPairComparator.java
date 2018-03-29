package pairs;

import java.util.Comparator;

public class ContainerPairComparator implements Comparator<ContainerPair> {

	public ContainerPairComparator() {}
	
	@Override
	public int compare(ContainerPair a, ContainerPair b) {
		return a.value > b.value ? 1 : -1;
	}
	
}
