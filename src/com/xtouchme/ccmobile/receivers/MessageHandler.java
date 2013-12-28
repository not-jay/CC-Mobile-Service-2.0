package com.xtouchme.ccmobile.receivers;

import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xtouchme.ccmobile.CarolinianConfessions;
import com.xtouchme.ccmobile.Settings;
import com.xtouchme.ccmobile.constants.College;

public class MessageHandler extends BroadcastReceiver {

	public static final String PREFERENCES = "com.xtouchme.ccmobile.PREFERENCES";
	
	private SharedPreferences data;

	private static class Message {
		protected String message	= "";
		protected String number		= "";
		
		private Message(String message, String number) {
			this.message = message;
			this.number = number;
		}
		
		public static Message getMessage(Intent intent) {
			Bundle bundle			= intent.getExtras();
			SmsMessage messages[]	= null;
			String message			= "";
			String number			= "";
			
			if(bundle == null) return null;
			
			Object pdus[] = (Object[]) bundle.get("pdus");
			messages = new SmsMessage[pdus.length];
			
			for(int x = 0; x < messages.length; x++) {
				messages[x] = SmsMessage.createFromPdu((byte[])pdus[x]);
				message += String.format(" %s", messages[x].getDisplayMessageBody());
				number = messages[x].getOriginatingAddress();
			}
			
			return new Message(message.substring(1), number);
		}
	}
	
	private static class Confession {
		public String text = "";
		public String college = "";
		public String ismis = "";
		
		private Confession(String c, String co, String ismis) {
			this.text = c;
			this.college = co;
			this.ismis = ismis;
		}
		
		public static Confession parseMessage(String message, String ans) {
			String newMessage;
			String text = "";
			String college = "";
			String ismis = "";
			
			if(message.toLowerCase(Locale.US).startsWith(ans.toLowerCase(Locale.US))) {
				message = message.substring(ans.length()+1);
				ismis = ans;
			} else
				ismis = "";
			
			newMessage = message.replaceAll("[\\s|\\r|\\n|\\r\\n]", " ");
			
			//Ensures that there are no empty strings in-between spaces
			for(String s : newMessage.split("\\s")) {
				if(s.equals("")) continue;
				newMessage += " "+s;
			}
			
			college = newMessage.substring(newMessage.lastIndexOf(' ')+1);
			
			text = message.substring(0, message.indexOf(college));
			
			return new Confession(text, college, ismis);
		}
	}
	
	private class PostConfession extends AsyncTask<Object, Void, Integer> {
		private String number = "";
		private Context context;
		
		@Override
		protected Integer doInBackground(Object... params) {
			number = (String)params[4];
			context = (Context)params[5];
			Log.v("CC Mobile Service", String.format("confession: %s%ncollege: %s%nismis: %s%nans: %s", (String)params[0], (College)params[1], (String)params[2], (String)params[3]));
			
			return CarolinianConfessions.postSecretConfession((String)params[0], (College)params[1], (String)params[2], (String)params[3]);
		}
		@Override
		protected void onPostExecute(Integer result) {
			SharedPreferences.Editor editor = data.edit();
			
			int failed = data.getInt("failed_count", 0);
			int closed = data.getInt("closed_count", 0);
			int success = data.getInt("success_count", 0);
			
			switch(result) {
			case CarolinianConfessions.FAILED:
				editor.putInt("failed_count", ++failed);
				editor.putString("last_result", "Failed");
				//Log.v("CC-Mobile", String.format("Sending failed. #%d", failed));
				break;
			case CarolinianConfessions.CLOSED:
				editor.putInt("closed_count", ++closed);
				editor.putString("last_result", "Closed");
				//Log.v("CC-Mobile", String.format("Form Closed. #%d", closed));
				break;
			case CarolinianConfessions.SUCCESS:
				editor.putInt("success_count", ++success);
				editor.putString("last_result", "Success");
				//Log.v("CC-Mobile", String.format("Sending successful. #%d", success));
				break;
			case CarolinianConfessions.ISMIS:
				editor.putInt("failed_count", ++failed);
				editor.putString("last_result", "Failed (ISMIS)");
				break;
			}
			editor.commit();
			
			//If we should reply, then update user about the result
			String toSend = "";
			String value = "";
			
			value = data.getString(Settings.KEY_WAIT_QUEUE, Settings.DurationValues.TWO_MINUTES_AND_A_HALF);
			if(value.equals(Settings.DurationValues.FIFTEEN_SECONDS)) value = Settings.DurationLabel.FIFTEEN_SECONDS;
			if(value.equals(Settings.DurationValues.THIRTY_SECONDS)) value = Settings.DurationLabel.THIRTY_SECONDS;
			if(value.equals(Settings.DurationValues.A_MINUTE)) value = Settings.DurationLabel.A_MINUTE;
			if(value.equals(Settings.DurationValues.TWO_MINUTES_AND_A_HALF)) value = Settings.DurationLabel.TWO_MINUTES_AND_A_HALF;
			if(value.equals(Settings.DurationValues.FIVE_MINUTES)) value = Settings.DurationLabel.FIVE_MINUTES;
			if(value.equals(Settings.DurationValues.TEN_MINUTES)) value = Settings.DurationLabel.TEN_MINUTES;
			
			switch(result) {
			case CarolinianConfessions.FAILED:
				toSend = "Confession was not sent because it contains an invalid/empty confession or college/school.";
				break;
			case CarolinianConfessions.ISMIS:
				toSend = "You answered the security question incorrectly. Please try again after "+value+".";
				break;
			case CarolinianConfessions.CLOSED:
				toSend = "Confession was not sent because we are not currently accepting confessions.";
				break;
			case CarolinianConfessions.SUCCESS:
				toSend = "Your confession was successfully sent!\r\nThank you for using CC's 'Confessions on the go' service.";
				break;
			}
			sendSms(number, toSend, true, context, data);
			
			new QueueHandler().handle(context, number, data.getString(Settings.KEY_WAIT_QUEUE, Settings.DurationValues.TWO_MINUTES_AND_A_HALF));
		}
		
	}
	
	private void sendSms(String number, String message, boolean addToThread, Context context, SharedPreferences prefs) {
		if(!prefs.getBoolean("reply_status", false)) return;
		SmsManager sms = SmsManager.getDefault();
		sms.sendMultipartTextMessage(number, null, sms.divideMessage(message), null, null);
		
		if(!addToThread) return;
		ContentValues values = new ContentValues();
		values.put("address", number);
		values.put("body", message);
		context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
	}
	
	private boolean hasInternetConnection(Context context) {
		boolean hasWifi		= false;
		boolean hasMobile	= false;
		
		ConnectivityManager connection = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] info = connection.getAllNetworkInfo();
		
		for(NetworkInfo network : info) {
			if(network.getTypeName().equalsIgnoreCase("WIFI") && network.isConnected()) hasWifi = true;
			if(network.getTypeName().equalsIgnoreCase("MOBILE") && network.isConnected()) hasMobile = true;
			if(hasWifi || hasMobile) break;
		}
		
		return hasWifi || hasMobile;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		data = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		Message sms = Message.getMessage(intent);
		String user_queue = data.getString("user_queue", "");
		
		Log.v("CC Mobile Servce", "Recieved message!");
		
		String number			= sms.number;
		String answer			= data.getString(Settings.KEY_CHECKPOINT, "No ISMIS found!");
		Confession confession 	= Confession.parseMessage(sms.message.substring(3), answer);
		College college 		= null;
		
		if(!sms.message.toLowerCase(Locale.US).startsWith("cc")) return;
		if(!hasInternetConnection(context)) {
			sendSms(number, "The service is not connected to the internet.\n"
						  + "Our team of highly professional monkeys are on the case.",
					true, context, data);
			return;
		}
		if(user_queue.contains(sms.number)) {
			String value = data.getString(Settings.KEY_WAIT_QUEUE, Settings.DurationValues.TWO_MINUTES_AND_A_HALF);
			if(value.equals(Settings.DurationValues.FIFTEEN_SECONDS)) value = Settings.DurationLabel.FIFTEEN_SECONDS;
			if(value.equals(Settings.DurationValues.THIRTY_SECONDS)) value = Settings.DurationLabel.THIRTY_SECONDS;
			if(value.equals(Settings.DurationValues.A_MINUTE)) value = Settings.DurationLabel.A_MINUTE;
			if(value.equals(Settings.DurationValues.TWO_MINUTES_AND_A_HALF)) value = Settings.DurationLabel.TWO_MINUTES_AND_A_HALF;
			if(value.equals(Settings.DurationValues.FIVE_MINUTES)) value = Settings.DurationLabel.FIVE_MINUTES;
			if(value.equals(Settings.DurationValues.TEN_MINUTES)) value = Settings.DurationLabel.TEN_MINUTES;
			sendSms(number, 
					String.format("You have already sent a confession. Please wait %s.", value),
					true, context, data);
			
			return;
		} else {
			SharedPreferences.Editor editor = data.edit();
			int queue = data.getInt("queue_count", 0);
			String userQueue = data.getString("user_queue", "");
			String fixedQueue = "";
			
			editor.putInt("queue_count", ++queue); //Add to queue, no matter the status
			userQueue += ";"+number;
			for(String s : userQueue.split(";")) {
				if(s.equals("")) continue;
				fixedQueue += ";"+s;
			}
			userQueue = fixedQueue.substring(1);
			editor.putString("user_queue", userQueue);
			
			editor.commit();
		}
		
		if(confession.college.equalsIgnoreCase("engineering") ||
		   confession.college.equalsIgnoreCase("engg")) {
			college = College.Engineering;
		} else if(confession.college.equalsIgnoreCase("cafa")) {
			college = College.CAFA;
		} else if(confession.college.equalsIgnoreCase("cas")) {
			college = College.CAS;
		} else if(confession.college.equalsIgnoreCase("education") ||
				  confession.college.equalsIgnoreCase("edu")) {
			college = College.Education;
		} else if(confession.college.equalsIgnoreCase("sbe")) {
			college = College.SBE;
		} else if(confession.college.equalsIgnoreCase("shcp")) {
			college = College.SHCP;
		} else if(confession.college.equalsIgnoreCase("slg")) {
			college = College.SLG;
		} else {
			college = null;
		}
		
		new PostConfession().execute(confession.text, college, confession.ismis, answer, number, context);
	}
	
}
