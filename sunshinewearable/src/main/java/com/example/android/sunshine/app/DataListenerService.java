package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.sarahehabm.common.Utility;
import com.sarahehabm.common.WearableConstants;

public class DataListenerService extends WearableListenerService {
    private static final String TAG = DataListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(WearableConstants.PATH_MAX_TEMP)) {
            final String message = new String(messageEvent.getData());
            Log.v(TAG, "Max temp path on watch: " + messageEvent.getPath());
            Log.v(TAG, "Max temp content on watch: " + message);

            Utility.putString(this, WearableConstants.MAX_TEMP, message);

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(WearableConstants.MAX_TEMP, message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        } else if (messageEvent.getPath().equals(WearableConstants.PATH_MIN_TEMP)) {
            final String message = new String(messageEvent.getData());
            Log.v(TAG, "Min temp path on watch: " + messageEvent.getPath());
            Log.v(TAG, "Min temp content on watch: " + message);

            Utility.putString(this, WearableConstants.MIN_TEMP, message);

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(WearableConstants.MIN_TEMP, message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        } else if (messageEvent.getPath().equals(WearableConstants.PATH_RES_ID)) {
            final String message = new String(messageEvent.getData());
            Log.v(TAG, "Res ID path on watch: " + messageEvent.getPath());
            Log.v(TAG, "Res ID content on watch: " + message);

            Utility.putString(this, WearableConstants.RES_ID, message);

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra(WearableConstants.RES_ID, message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        } else
            super.onMessageReceived(messageEvent);
    }
}
