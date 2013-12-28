package com.xtouchme.ccmobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.xtouchme.ccmobile.receivers.MessageHandler;
import com.xtouchme.ccmobile.services.MobileService;

public class CCMobile extends Activity {

	private CompoundButton service;
	private CompoundButton reply;
	private TextView failed;
	private TextView closed;
	private TextView success;
	private TextView queue;
	private TextView lastMessage;
	
	private SharedPreferences data;
	private SharedPreferences.OnSharedPreferenceChangeListener dataListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		data 		= getSharedPreferences(MessageHandler.PREFERENCES, Context.MODE_PRIVATE);
		service		= (CompoundButton)	findViewById(R.id.start_switch);
		reply		= (CompoundButton)	findViewById(R.id.reply_switch);
		failed		= (TextView)		findViewById(R.id.failed_count);
		closed		= (TextView)		findViewById(R.id.closed_count);
		success		= (TextView)		findViewById(R.id.success_count);
		queue		= (TextView)		findViewById(R.id.queue_count);
		lastMessage	= (TextView)		findViewById(R.id.last_message);
		
		service.setChecked(data.getBoolean("service_status", false));
		service.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean listen;
				SharedPreferences.Editor editor = data.edit();
		
				editor.putBoolean("service_status", listen = service.isChecked());
				
				editor.commit();
				
				//Register/Unregister broadcast listener
				//TODO: move broadcast listener to service, replace with service here
				if(listen) {
					/*listener = new MessageHandler();
					registerReceiver(listener, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));*/
					startService(new Intent(CCMobile.this, MobileService.class));
					Toast.makeText(CCMobile.this, "Service started!", Toast.LENGTH_SHORT).show();
				}
				else {
//					unregisterReceiver(listener);
					stopService(new Intent(CCMobile.this, MobileService.class));
					Toast.makeText(CCMobile.this, "Service stopped!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		reply.setChecked(data.getBoolean("reply_status", false));
		reply.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = data.edit();
				editor.putBoolean("reply_status", reply.isChecked());
				
				editor.commit();
			}
		});
		failed.setText(String.format("%d", data.getInt("failed_count", 0)));
		closed.setText(String.format("%d", data.getInt("closed_count", 0)));
		success.setText(String.format("%d", data.getInt("success_count", 0)));
		queue.setText(String.format("%d", data.getInt("queue_count", 0)));
		
		String lastResult = data.getString("last_result", "");
		String userQueue = data.getString("user_queue", "");
		
		lastMessage.setText(String.format("Last Message Status: %s%nQueue:%n%s", lastResult, userQueue.replaceAll(";", "; ")));
		
		dataListener = new OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if(key.equals("failed_count")) {
					failed.setText(String.format("%d", data.getInt("failed_count", 0)));
					failed.invalidate();
				}
				else if(key.equals("closed_count")) {
					closed.setText(String.format("%d", data.getInt("closed_count", 0)));
					closed.invalidate();
				}
				else if(key.equals("success_count")) {
					success.setText(String.format("%d", data.getInt("success_count", 0)));
					success.invalidate();
				}
				else if(key.equals("queue_count")) {
					queue.setText(String.format("%d", data.getInt("queue_count", 0)));
					queue.invalidate();
				}
				else if(key.equals("user_queue") || key.equals("last_result")) {
					String lastResult = data.getString("last_result", "");
					String userQueue = data.getString("user_queue", "");
					
					lastMessage.setText(String.format("Last Message Status: %s%nQueue:%n%s", lastResult, userQueue.replaceAll(";", "; ")));
					lastMessage.invalidate();
				}
			}
		};
		data.registerOnSharedPreferenceChangeListener(dataListener);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_settings:
			//Show settings
			try {
				Class<?> settings = Class.forName("com.xtouchme.ccmobile.Settings");
				startActivity(new Intent(CCMobile.this, settings));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
}
