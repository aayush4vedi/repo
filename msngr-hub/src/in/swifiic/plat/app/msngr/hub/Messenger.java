package in.swifiic.plat.app.msngr.hub;

import ibrdtn.api.ExtendedClient;
import ibrdtn.example.api.DTNClient;
import in.swifiic.plat.helper.hub.Base;
import in.swifiic.plat.helper.hub.DatabaseHelper;
import in.swifiic.plat.helper.hub.Helper;
import in.swifiic.plat.helper.hub.xml.Action;
import in.swifiic.plat.helper.hub.xml.Notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Messenger extends Base  {
	
	private static final Logger logger = LogManager.getLogManager().getLogger("");
	 public static org.apache.logging.log4j.Logger log=org.apache.logging.log4j.LogManager.getLogger();

    private DTNClient dtnClient;
    
    protected ExecutorService executor = Executors.newCachedThreadPool();
    
    // Following is the name of the endpoint to register with
    protected String PRIMARY_EID = "Msngr";
    
    public Messenger() {
        // Initialize connection to daemon
    	  this.executor = Executors.newCachedThreadPool();
      	
         	this.client = new ExtendedClient(); 
      	this.dtnClient = new DTNClient(PRIMARY_EID, PAYLOAD_TYPE, HANDLER_TYPE, this, client);
    	//this.dtnClient = new DTNClient(PRIMARY_EID, PAYLOAD_TYPE, HANDLER_TYPE, this, client);
        logger.log(Level.INFO, dtnClient.getConfiguration());
        log.info(dtnClient.getConfiguration());
    }
    
    public static void main(String args[]) throws IOException {
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	Messenger messenger = new Messenger();
    	String input;
    	while(true) {
		    System.out.print("Enter \"exit\" to exit application: ");
	    	input = br.readLine();
	    	if(input.equalsIgnoreCase("exit")) {
	    		messenger.exit();
	    	}
	    }
    }


	public void endPayload(String payload) {
		super.endPayload();
		final String message = payload;
		System.out.println("Got Message:" + payload);
		executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                	Action action = Helper.parseAction(message);
                	Notification notif = new Notification(action);
                	notif.updateNotificatioName("DeliverMessage");
                	
                	String toUser = action.getArgument("toUser");
                	String fromUser=action.getArgument("fromUser");
                	
              
                	
                	// A user may have multiple devices
                	List<String> deviceList = Helper.getDevicesForUser(toUser);
                	String response = Helper.serializeNotification(notif);
                	for(int i = deviceList.size()-1; i >= 0; --i) {
                		send(deviceList.get(i) + "/in.swifiic.app.msngr.andi" /*"/in.swifiic.android.app.msngr"*/, response);
                		// Mark bundle as delivered...                    
                        logger.log(Level.INFO, "Attempted to send to {1}, had received \n{0}\n and responsed with \n {2}", 
                        				new Object[] {message, deviceList.get(i) + "/in.swifiic.app.msngr.andi" /*"/in.swifiic.android.app.msngr"*/, response});
                	}
                	boolean status = Helper.debitUser(fromUser);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unable to process message and send response\n" + e.getMessage());
                }
            }
        });
		
	}
	
}