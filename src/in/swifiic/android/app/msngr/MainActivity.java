package in.swifiic.android.app.msngr;

import in.swifiic.android.app.lib.AppEndpointContext;
import in.swifiic.android.app.lib.Helper;
import in.swifiic.android.app.lib.ui.SwifiicActivity;
import in.swifiic.android.app.lib.ui.UserChooserActivity;
import in.swifiic.android.app.lib.xml.Action;
import in.swifiic.android.app.lib.xml.Notification;

import java.util.Date;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class MainActivity extends SwifiicActivity {
    
    private static final int SELECT_USER = 1;
    
    private static final String TAG = "MainActivity";
	
    private EditText messageToSend = null;
    private ListView conversation = null;
    private ImageButton b = null;
    
    private AppEndpointContext aeCtx = new AppEndpointContext("Msngr", "0.1", "1");
    
    DatabaseHelper db;
    CustomCursorAdapter customAdapter;
    
    /**
     * Mandatory implementation to receive swifiic notifications
     */
    public MainActivity() {
    	super();
    	
    	// This is a must for all applications - hook to get notification from GenericService
    	mDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("notification")) {
                	String payload= intent.getStringExtra("notification");
                	
                    Log.d(TAG, "Handling incoming message: " + payload);
                    Notification notif = Helper.parseNotification(payload);
                	// Checking for opName of Notification
                    if(notif.getNotificationName() == "DeliverMessage") {
                    	Msg msg = new Msg(notif);
                    	db = new DatabaseHelper(getApplicationContext());
                    	db.addMessage(msg);
                    	db.closeDB();
                    }
                } else {
                    Log.d(TAG, "Broadcast Receiver ignoring message - no notification found");
                }
            }
        };      
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.itemSelectUser) {
			Intent selectNeighbor = new Intent(this, UserChooserActivity.class);
			startActivityForResult(selectNeighbor, SELECT_USER);
			return true;
		} else if (itemId == R.id.settings) {
			Intent selectedSettings = new Intent(this, SettingsActivity.class);
			startActivity(selectedSettings);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
    }

    /**
     * Called when activity exits from "startActivityForResult"
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SELECT_USER == requestCode) {
            if ((data != null)){
            	String userName = "";
            	if (data.hasExtra("userName")) {
            		userName = data.getStringExtra("userName");
            	}
            	ActionBar actionBar = getActionBar();
            	actionBar.setTitle(userName);
            	b.setEnabled(true);
            	Log.d("ActivityResult", "Got user: " + userName);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /** 
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_msngr);
        
        messageToSend = (EditText)findViewById(R.id.msgTextToSend);
        conversation = (ListView)findViewById(R.id.conversation);
        b = (ImageButton)findViewById(R.id.buttonSendMsg);
        
        // Assigning an action to the send button
        
        b.setEnabled(false);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	ActionBar actionBar = getActionBar();
            	if (messageToSend.getText().toString().equals("")){
            		// Do nothing if the message is empty
            	}
            	else {
            		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(v.getContext());
            		String message = messageToSend.getText().toString();
            		String toUser = actionBar.getTitle().toString();
            		String fromUser = sharedPref.getString("my_identity", "UnknownUser");
            		Date date = new Date();
            		String sentAt = "" + date.getTime();
                    
            		Action act = new Action("SendMessage", aeCtx);
                    act.addArgument("message", message);
                    act.addArgument("toUser", toUser);
                    act.addArgument("fromUser", fromUser);
                    act.addArgument("sentAt", "" + sentAt);   
                    
                    // Loading hub address from preferences
                    String hubAddress = sharedPref.getString("hub_address", "");
                    
                    Helper.sendAction(act, hubAddress + "/messenger", v.getContext());
                    
                    Msg msg = new Msg();
                    msg.setMsg(message);
                    msg.setUser(toUser);
                    msg.setIsInbound(0);
                    msg.setSentAtTime(sentAt);
                    
                    db = new DatabaseHelper(v.getContext());
                    db.addMessage(msg);
                    Log.d(TAG, "Inserted a row: " + msg.getMsg());
                    db.closeDB();
                    EditText msgInput = (EditText) findViewById(R.id.msgTextToSend);
                    msgInput.setText("");
                    customAdapter.changeCursor(db.getMessagesForUser(actionBar.getTitle().toString()));
                    conversation.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            conversation.setSelection(conversation.getCount());
                            conversation.smoothScrollToPosition(conversation.getCount());
                        }
                    }, 100);
            	}
            }
        });
    }

    public void onResume() {
        super.onResume();
        
        conversation.setOnItemClickListener(new OnItemClickListener() {
        	@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.d("MainActivity", "User clicked on item!");
			}
        });
        
        final ActionBar actionBar = getActionBar();
        
        if(!actionBar.getTitle().toString().equals("Select User")) {
        	final DatabaseHelper dbh = new DatabaseHelper(this);
        	new Handler().post(new Runnable() {
	        	@Override
	            public void run() {
	        		customAdapter = new CustomCursorAdapter(MainActivity.this, dbh.getMessagesForUser(actionBar.getTitle().toString()));
	                conversation.setAdapter(customAdapter);
	            }
        	});
        }
        
        conversation.postDelayed(new Runnable() {
            @Override
            public void run() {
                conversation.setSelection(conversation.getCount());
                conversation.smoothScrollToPosition(conversation.getCount());
            }
        }, 100);
    }
}
