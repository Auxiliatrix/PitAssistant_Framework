package modules.invokers;

import constants.Calibration;
import processors.Brain;

public class BackupInvoker extends Invoker {
	
	public BackupInvoker() {}
	
	@Override
	public String getInvoker() {
		return "!save";
	}
	
	@Override
	public String process( String input ) {
		String response = "Preparing to backup database, please wait...\n";
		
		if( input.isEmpty() ) {
			response += "Using the default backup file: " + Calibration.Database.DATABASE_NAME;
			Brain.data.backup();
		} else {
			response += "Custom backup locations are currently unsupported.\n";
		}
		
		return response;		
	}

}
