package modules;

public interface Module {
	public String getInvoker();
	public String process(String input);
}
