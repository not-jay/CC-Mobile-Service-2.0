package com.xtouchme.ccmobile.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.xtouchme.ccmobile.CCMobile;
import com.xtouchme.ccmobile.R;
import com.xtouchme.ccmobile.receivers.MessageHandler;

public class MobileService extends Service {

	private MessageHandler handler;
	
	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("CC Mobile Service");
		thread.start();
		
		handler = new MessageHandler();
		registerReceiver(handler, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentIntent(PendingIntent.getActivity(this, 6969, new Intent(this, CCMobile.class), PendingIntent.FLAG_CANCEL_CURRENT))
			   .setSmallIcon(R.drawable.ic_launcher)
			   .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
			   .setTicker("Starting service...")
			   .setWhen(System.currentTimeMillis())
			   .setAutoCancel(true)
			   .setContentTitle("CC Mobile Service")
			   .setContentText("Service started");
		
		Notification notif = builder.build();
		
		startForeground(6969, notif);
	}
	
	@Override
	public void onDestroy() {
		if(handler != null) unregisterReceiver(handler);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
