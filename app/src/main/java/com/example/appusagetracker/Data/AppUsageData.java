package com.example.appusagetracker.Data;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.example.appusagetracker.UI.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AppUsageData {

    private Context context;
    private UsageStatsManager usageStatsManager;
    private PackageManager packageManager;
    public static final long TOTAL_MILLIS_IN_DAY = 86400000;
    public static final long TOTAL_MILLIS_IN_WEEK = 7*TOTAL_MILLIS_IN_DAY;
    public static final long TOTAL_MILLIS_IN_MONTH = 30*TOTAL_MILLIS_IN_DAY;

    public AppUsageData(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = context.getPackageManager();
    }

    public boolean hasUsageStatsPermission() {
        long time = System.currentTimeMillis();
        long startTime = time - AppUsageData.TOTAL_MILLIS_IN_MONTH;
        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, time);
        return !(appList == null || appList.isEmpty());
    }

    public List<AppUsageInfo> getAppUsageStats(String timePeriod) {
        long time = System.currentTimeMillis();
        long startTime;
        long intervalMillis;

        if (timePeriod.equals("Weekly")) {
            intervalMillis = AppUsageData.TOTAL_MILLIS_IN_DAY;
            startTime = time - AppUsageData.TOTAL_MILLIS_IN_WEEK;
        } else if (timePeriod.equals("Monthly")) {
            intervalMillis = AppUsageData.TOTAL_MILLIS_IN_DAY;
            startTime = time - AppUsageData.TOTAL_MILLIS_IN_MONTH;
        } else {
            // Default to daily
            intervalMillis = AppUsageData.TOTAL_MILLIS_IN_DAY;
            startTime = time - AppUsageData.TOTAL_MILLIS_IN_DAY;
        }

        List<AppUsageInfo> appUsageInfoList = new ArrayList<>();
        Map<String, AppUsageInfo> appUsageMap = new HashMap<>();

        // Aggregate data across the period
        for (long dayStart = startTime; dayStart < time; dayStart += intervalMillis) {
            long dayEnd = dayStart + intervalMillis;

            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd);

            if (usageStatsList != null && !usageStatsList.isEmpty()) {
                for (UsageStats usageStats : usageStatsList) {
                    if (usageStats.getTotalTimeInForeground() > 0) {
                        String packageName = usageStats.getPackageName();

                        AppUsageInfo existingAppUsageInfo = appUsageMap.get(packageName);
                        if (existingAppUsageInfo == null) {
                            // New app entry
                            String appName;
                            Drawable appIcon;
                            try {
                                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                                appName = packageManager.getApplicationLabel(appInfo).toString();
                                appIcon = packageManager.getApplicationIcon(appInfo);
                            } catch (PackageManager.NameNotFoundException e) {
                                appName = packageName;
                                appIcon = context.getDrawable(android.R.drawable.sym_def_app_icon);
                            }

                            long usageTimeInMillis = usageStats.getTotalTimeInForeground();
                            AppUsageInfo newAppUsageInfo = new AppUsageInfo(appName, appIcon, usageTimeInMillis);
                            appUsageMap.put(packageName, newAppUsageInfo);
                        } else {
                            // Accumulate the usage time for the existing app
                            existingAppUsageInfo.setUsageTimeInMillis(
                                    existingAppUsageInfo.getUsageTimeInMillis() + usageStats.getTotalTimeInForeground());
                        }
                    }
                }
            }
        }

        // Convert map to list
        appUsageInfoList.addAll(appUsageMap.values());

        // Sort the list by usage time in descending order
        Collections.sort(appUsageInfoList, (o1, o2) -> Long.compare(o2.getUsageTimeInMillis(), o1.getUsageTimeInMillis()));

        return appUsageInfoList;
    }


    public static class AppUsageInfo {
        private String appName;
        private long usageTimeInMillis;
        private Drawable appIcon;

        public AppUsageInfo(String appName, Drawable appIcon, long usageTimeInMillis) {
            this.appName = appName;
            this.usageTimeInMillis = usageTimeInMillis;
            this.appIcon = appIcon;
        }

        public AppUsageInfo() {
            this.appName = null;
            this.usageTimeInMillis = 0;
            this.appIcon = null;
        }

        public AppUsageInfo setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public AppUsageInfo setUsageTimeInMillis(long usageTimeInMillis) {
            this.usageTimeInMillis = usageTimeInMillis;
            return this;
        }

        public AppUsageInfo setAppIcon(Drawable appIcon) {
            this.appIcon = appIcon;
            return this;
        }

        public String getAppName() {
            return appName;
        }

        public long getUsageTimeInMillis() {
            return usageTimeInMillis;
        }

        public String getFormattedUsageTime(long periodMillis) {
            if(periodMillis == AppUsageData.TOTAL_MILLIS_IN_DAY){
                return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(usageTimeInMillis),
                        TimeUnit.MILLISECONDS.toMinutes(usageTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(usageTimeInMillis) % TimeUnit.MINUTES.toSeconds(1));
            }
            else
            {
                return String.format(Locale.getDefault(), "%02d:%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toDays(usageTimeInMillis),
                        TimeUnit.MILLISECONDS.toHours(usageTimeInMillis) % TimeUnit.DAYS.toHours(1),
                        TimeUnit.MILLISECONDS.toMinutes(usageTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(usageTimeInMillis) % TimeUnit.MINUTES.toSeconds(1));
            }




        }

        public Drawable getAppIcon() {
            return appIcon;
        }


    }
}