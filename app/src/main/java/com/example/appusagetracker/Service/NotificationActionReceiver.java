package com.example.appusagetracker.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.appusagetracker.Data.AppUsageData;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_SNOOZE = "com.example.appusagetracker.ACTION_SNOOZE";
    public static final String ACTION_MUTE = "com.example.appusagetracker.ACTION_MUTE";
    private static final long SNOOZE_DURATION_MS = 10*1000;
    //private static final long SNOOZE_DURATION_MS = 2*60*1000;
    private static final long MUTE_DURATION_MS = 3*60*60*1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String appName = intent.getStringExtra("appName");

        // Get the package name using the app name
        AppUsageData.AppUsageInfo appUsageInfo = new AppUsageData.AppUsageInfo();
        String packageName = appUsageInfo.getPackageNameByAppName(context, appName);

        SharedPreferences prefs = context.getSharedPreferences("AppLimits", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (ACTION_SNOOZE.equals(action)) {
            // Handle snooze action
            Log.d("NotificationAction", "Snooze clicked for: " + appName);
            Toast.makeText(context, "Snoozed " + appName + " for 2 minutes", Toast.LENGTH_SHORT).show();

            long snoozeEndTime = System.currentTimeMillis() + SNOOZE_DURATION_MS;
            editor.putLong(packageName + "_snoozeEnd", snoozeEndTime);
            editor.apply();

        } else if (ACTION_MUTE.equals(action)) {
            // Handle mute action
            Log.d("NotificationAction", "Mute clicked for: " + appName);
            Toast.makeText(context, "Muted " + appName + " for 3 hours", Toast.LENGTH_SHORT).show();

            long muteEndTime = System.currentTimeMillis() + MUTE_DURATION_MS;
            editor.putLong(packageName + "_muteEnd", muteEndTime);
            editor.apply();
        }
    }


}
