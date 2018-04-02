package modules.invokers;

public class AliasInvoker extends Invoker {

	public AliasInvoker() {}
	
	@Override
	public String getInvoker() {
		return "!alias";
	}

	@Override
	public String process(String input) {
		String response = "";
		String[] tokens = input.split(", ");
		if( tokens.length < 2 ) {
			response += "You need to give me an item name, and then a nickname.\n";
		} else {
			
		}
		return response;
	}
	

}
