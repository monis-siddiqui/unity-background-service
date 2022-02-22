package com.kdg.toast.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class autostart extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent arg1)
    {
        Log.i("Autostart", "inside on boot event");
        Intent intent = new Intent(context,PedometerService.class);
        Log.i("Autostart", "Attempting to start");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        Log.i("Autostart", "started");
    }
}