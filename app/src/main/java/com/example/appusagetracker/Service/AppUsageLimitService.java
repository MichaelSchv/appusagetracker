package com.example.appusagetracker.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.appusagetracker.Data.AppUsageData;
import com.example.appusagetracker.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppUsageLimitService extends Service {
    private static final String TAG = "AppUsageLimitServiceKKK";
    private Handler handler;
    private Runnable checkUsageRunnable;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AppUsageLimitService","Service started");
        handler = new Handler();
        prefs = getSharedPreferences("AppLimits", MODE_PRIVATE);
        startUsageChecks();
    }

    private void startUsageChecks() {
        Log.d(TAG, "Starting usage checks");
        checkUsageRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Running checkAppUsageLimits");
                checkAppUsageLimits();
                handler.postDelayed(this, 1000); // Check every second
            }
        };
        handler.post(checkUsageRunnable);
    }

    private void checkAppUsageLimits() {
        AppUsageData.AppUsageInfo appUsageInfo = new AppUsageData.AppUsageInfo();

        for (String key : getMonitoredApps()) {
            String packageName = key.split("_")[0];
            long limitInMillis = prefs.getLong(packageName + "_limitMillis", 0);

            long currentUsageInMillis = getCurrentAppUsage(packageName);
            Log.d(TAG, "App: " + packageName + ", Current usage: " + currentUsageInMillis + "ms, Limit: " + limitInMillis + "ms");

            // Retrieve snooze and mute end times
            long snoozeEndTime = prefs.getLong(packageName + "_snoozeEnd", 0);
            long muteEndTime = prefs.getLong(packageName + "_muteEnd", 0);
            long currentTime = System.currentTimeMillis();

            // Skip notification if within snooze or mute period
            if (currentTime < snoozeEndTime) {
                Log.d(TAG, packageName + " is snoozed until " + snoozeEndTime);
                continue;
            }

            if (currentTime < muteEndTime) {
                Log.d(TAG, packageName + " is muted until " + muteEndTime);
                continue;
            }

            if (currentUsageInMillis > limitInMillis) {
                String appName = appUsageInfo.getAppNameByPackage(getApplicationContext(), packageName);
                showLimitExceededNotification(appName);
            } else {
                Log.d(TAG, "App " + packageName + " is within the usage limit.");
            }
        }
    }


    private long getCurrentAppUsage(String packageName) {
        long time = System.currentTimeMillis();
        long startTime = time - (24 * 60 * 60 * 1000); // Last 24 hours

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, time);

        for (UsageStats usageStats : usageStatsList) {
            if (usageStats.getPackageName().equals(packageName)) {
                return usageStats.getTotalTimeInForeground();
            }
        }
        return 0;
    }

    private Set<String> getMonitoredApps() {
        // Return the list of apps that are being monitored
        Log.d("Monitored", prefs.getAll().keySet().toString());
        return prefs.getAll().keySet();
    }



    private int[] getUsageLimit(String packageName) {
        int hours = prefs.getInt(packageName + "_hours", 0);
        int minutes = prefs.getInt(packageName + "_minutes", 0);
        int seconds = prefs.getInt(packageName + "_seconds", 0);
        return new int[]{hours, minutes, seconds};
    }

    private long getCurrentAppUsage(String packageName, long startTime) {
        long time = System.currentTimeMillis();

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, time);

        for (UsageStats usageStats : usageStatsList) {
            if (usageStats.getPackageName().equals(packageName)) {
                long currentUsage = usageStats.getTotalTimeInForeground();
                return currentUsage; // Total time in the foreground in milliseconds
            }
        }
        return 0;
    }



    private void showLimitExceededNotification(String appName) {
        String channelId = "usage_limit_channel";
        String channelName = "Usage Limit Notifications";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            // Check if the notification manager is null
            Log.e("Notification", "NotificationManager is null");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent snoozeIntent = new Intent(this, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra("appName", appName);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent muteIntent = new Intent(this, NotificationActionReceiver.class);
        muteIntent.setAction(NotificationActionReceiver.ACTION_MUTE);
        muteIntent.putExtra("appName", appName);
        PendingIntent mutePendingIntent = PendingIntent.getBroadcast(this, 0, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this)
                    .setPriority(Notification.PRIORITY_DEFAULT); // For older versions, set priority
        }

        NotificationCompat.Builder build = new NotificationCompat.Builder(this,channelId)
                .setContentTitle("Usage Limit Exceeded")
                .setContentText(appName + " has exceeded its daily usage limit.")
                .setSmallIcon(R.drawable.ic_notification) // Ensure this resource exists
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification))
                .setAutoCancel(true) // Automatically remove the notification when clicked
                .addAction(0,"Snooze for 2 minutes", snoozePendingIntent)
                .addAction(0,"Mute for 3 hours", mutePendingIntent);

        notificationManager.notify(appName.hashCode(), build.build());
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checkUsageRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
