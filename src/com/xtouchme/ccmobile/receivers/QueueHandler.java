package com.xtouchme.ccmobile.receivers;

import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;

public class QueueHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CC Mobile Service");
		
		wakeLock.acquire();
		
		//Remove the user from the queue
		try {
			SharedPreferences data = context.getSharedPreferences(MessageHandler.PREFERENCES, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = data.edit();
			String userQueue = data.getString("user_queue", "");
			int queue = data.getInt("queue_count", 0);
			Bundle bundle = intent.getExtras();
			
			userQueue = removeFromQueue(userQueue, bundle.getString("queued_user"));
			
			editor.putString("user_queue", userQueue);
			editor.putInt("queue_count", --queue);
			editor.commit();
		} finally {
			wakeLock.release();
		}
	}

	private String removeFromQueue(String queue, String remove) {
		String newQueue = "";
		
		queue = queue.replaceAll(Pattern.quote(remove), "");
		
		for(String s : queue.split(";")) {
			if(s.equals("")) continue;
			newQueue += s;
		}
		
		if(newQueue.equals("")) return newQueue;
		return newQueue.substring(1);
	}
	
	public void handle(Context context, String number, String time){
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, QueueHandler.class);
		intent.putExtra("queued_user", number);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		long queueTime = Long.parseLong(time);
		
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+queueTime, pi);
    }
}
