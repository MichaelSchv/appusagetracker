package com.example.appusagetracker.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.appusagetracker.Data.AppUsageData;
import com.example.appusagetracker.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PieChart usage_CRT_piechart;
    private FrameLayout usage_CRD_cardContainer;
    private View cardViewLayout;
    private ShapeableImageView card_IMG_icon;
    private MaterialTextView card_LBL_name;
    private MaterialTextView card_LBL_usage;
    private AppUsageData appUsageData;
    private long otherUsageTime = 0;
    private long totalScreenOffTime=0;
    private final long TOTAL_MILLIS_IN_DAY = 86400000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appUsageData = new AppUsageData(this);
        findViews();

        setupPieChart();

        if (!appUsageData.hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            loadPieChartData();
        }

        usage_CRT_piechart.setOnChartValueSelectedListener(new OnChartValueSelectedListener(){

            @Override
            public void onValueSelected(Entry e, Highlight h) {
                showCardView((PieEntry) e);
            }

            @Override
            public void onNothingSelected() {
                cardViewLayout.setVisibility(View.GONE);
            }
        });

    }

    private void showCardView(PieEntry entry) {
        String selectedAppName = entry.getLabel();
        AppUsageData.AppUsageInfo selectedAppInfo = null;

        if("Other".equals(selectedAppName)){
            selectedAppInfo = new AppUsageData.AppUsageInfo();
            selectedAppInfo.setAppName("Other");
            selectedAppInfo.setUsageTimeInMillis(otherUsageTime);
            selectedAppInfo.setAppIcon(ContextCompat.getDrawable(this,R.drawable.other));
        }
        else if ("Screen Off".equals(selectedAppName)) {
            selectedAppInfo = new AppUsageData.AppUsageInfo();
            selectedAppInfo.setAppName("Screen Off");
            selectedAppInfo.setUsageTimeInMillis(totalScreenOffTime);
            selectedAppInfo.setAppIcon(ContextCompat.getDrawable(this,R.drawable.blackscreen));
        }
        else
        {
            for (AppUsageData.AppUsageInfo appUsageInfo : appUsageData.getAppUsageStats()) {
                if (appUsageInfo.getAppName().equals(selectedAppName)) {
                    selectedAppInfo = appUsageInfo;
                    break;
                }
            }
        }
        if (selectedAppInfo != null) {
            usage_CRD_cardContainer.setVisibility(View.VISIBLE);
            card_LBL_name.setText(selectedAppInfo.getAppName()); // Correct name
            card_LBL_usage.setText("Time: " + selectedAppInfo.getFormattedUsageTime());
            card_IMG_icon.setImageDrawable(selectedAppInfo.getAppIcon()); // Correct icon
            cardViewLayout.setVisibility(View.VISIBLE);
        }
    }


    private void setupPieChart() {
        usage_CRT_piechart.setDrawHoleEnabled(true);
        usage_CRT_piechart.setHoleColor(android.R.color.transparent);
        usage_CRT_piechart.setUsePercentValues(true);
        usage_CRT_piechart.setEntryLabelTextSize(12);
        usage_CRT_piechart.setEntryLabelColor(android.R.color.black);
        usage_CRT_piechart.setCenterText("App Usage");
        usage_CRT_piechart.setCenterTextSize(24);
        usage_CRT_piechart.getDescription().setEnabled(false);
        usage_CRT_piechart.setRotationEnabled(false);
        usage_CRT_piechart.getLegend().setEnabled(false);
        usage_CRT_piechart.setHighlightPerTapEnabled(true);
    }

    private void loadPieChartData() {
        List<AppUsageData.AppUsageInfo> appUsageInfoList = appUsageData.getAppUsageStats();
        ArrayList<PieEntry> entries = new ArrayList<>();
        long totalUsageTime = 0;
        double thresholdPercentage = 0.01; // 1% threshold

        // Calculate total usage time
        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageInfoList) {
            totalUsageTime += appUsageInfo.getUsageTimeInMillis();
        }

        if(totalScreenOffTime > 0)
            entries.add(new PieEntry(totalScreenOffTime, "Screen Off"));

        // Group small usage times into "Other"
        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageInfoList) {
            double usagePercentage = (double) appUsageInfo.getUsageTimeInMillis() / totalUsageTime;
            if (usagePercentage < thresholdPercentage) {
                otherUsageTime += appUsageInfo.getUsageTimeInMillis();
            } else {
                entries.add(new PieEntry(appUsageInfo.getUsageTimeInMillis(), appUsageInfo.getAppName()));
            }
        }

        // Add the "Other" category if necessary
        if (otherUsageTime > 0) {
            entries.add(new PieEntry(otherUsageTime, "Other"));
        }

        // Create the PieDataSet and PieData
        PieDataSet dataSet = new PieDataSet(entries, "App Usage");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueTextSize(12f);
        data.setValueTextColor(android.R.color.black);

        usage_CRT_piechart.setData(data);
        usage_CRT_piechart.invalidate(); // Refresh the chart
    }


    private void findViews() {
        usage_CRT_piechart = findViewById(R.id.usage_CRT_piechart);
        LayoutInflater inflater = LayoutInflater.from(this);
        cardViewLayout = inflater.inflate(R.layout.card_app_usage,null,false);
        usage_CRD_cardContainer = findViewById(R.id.usage_CRD_cardContainer);
        usage_CRD_cardContainer.addView(cardViewLayout);
        usage_CRD_cardContainer.setVisibility(View.INVISIBLE);
        card_IMG_icon = cardViewLayout.findViewById(R.id.card_IMG_icon);
        card_LBL_name = cardViewLayout.findViewById(R.id.card_LBL_name);
        card_LBL_usage = cardViewLayout.findViewById(R.id.card_LBL_usage);

    }
}
