package com.example.appusagetracker.Data;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AppUsageData {

    private Context context;
    private UsageStatsManager usageStatsManager;
    private PackageManager packageManager;

    public AppUsageData(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = context.getPackageManager();
    }

    public boolean hasUsageStatsPermission() {
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60 * 60 * 24, time);
        return !(appList == null || appList.isEmpty());
    }

    public List<AppUsageInfo> getAppUsageStats() {
        long time = System.currentTimeMillis();
        long oneDayAgo = time - 1000 * 60 * 60 * 24;
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, oneDayAgo, time);

        List<AppUsageInfo> appUsageInfoList = new ArrayList<>();
        Set<String> uniqueApps = new HashSet<>();

        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            for (UsageStats usageStats : usageStatsList) {
                if (usageStats.getTotalTimeInForeground() > 0) {
                    String packageName = usageStats.getPackageName();
                    if (!uniqueApps.contains(packageName)) {
                        uniqueApps.add(packageName);

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
                        appUsageInfoList.add(new AppUsageInfo(appName, packageName, appIcon, usageTimeInMillis));
                    }
                }
            }
        }

        // Sort the list by usage time in descending order
        Collections.sort(appUsageInfoList, new Comparator<AppUsageInfo>() {
            @Override
            public int compare(AppUsageInfo o1, AppUsageInfo o2) {
                return Long.compare(o2.getUsageTimeInMillis(), o1.getUsageTimeInMillis());
            }
        });

        return appUsageInfoList;
    }


    public static class AppUsageInfo {
        private String appName;
        private long usageTimeInMillis;
        private String packageName;
        private Drawable appIcon;

        public AppUsageInfo(String appName,String packageName, Drawable appIcon, long usageTimeInMillis) {
            this.appName = appName;
            this.usageTimeInMillis = usageTimeInMillis;
            this.packageName = packageName;
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

        public String getFormattedUsageTime() {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(usageTimeInMillis),
                    TimeUnit.MILLISECONDS.toMinutes(usageTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(usageTimeInMillis) % TimeUnit.MINUTES.toSeconds(1));
        }

        public Drawable getAppIcon() {
            return appIcon;
        }
    }
}