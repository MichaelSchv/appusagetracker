package com.example.appusagetracker.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.appusagetracker.Data.AppUsageData;
import com.example.appusagetracker.R;
import com.example.appusagetracker.Service.AppUsageLimitService;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PieChart usage_CRT_piechart;
    private FrameLayout usage_CRD_cardContainer;
    private View cardViewLayout;
    private ShapeableImageView card_IMG_icon;
    private MaterialTextView card_LBL_name;
    private MaterialTextView card_LBL_usage;
    private CheckBox usage_CHBX_screenOff;
    private AppCompatSpinner usage_SPN_timeSelector;

    private MaterialButton usage_BTN_limit;




    private AppUsageData appUsageData;
    private long otherUsageTime = 0;
    private long totalScreenOffTime=0;
    private long periodMillis;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appUsageData = new AppUsageData(this);
        findViews();
        handleSpinner();
        setupPieChart();
        startAppUsageLimitService();

        usage_BTN_limit.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, LimitActivity.class);
            startActivity(intent);
        });
        usage_CHBX_screenOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!appUsageData.hasUsageStatsPermission()) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } else {
                    clearPieChartHighlight();
                    hideCardView();
                    String timePeriod = usage_SPN_timeSelector.getSelectedItem().toString();
                    loadPieChartData(timePeriod);
                }
            }
        });



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

    private void startAppUsageLimitService() {
        Intent serviceIntent = new Intent(this, AppUsageLimitService.class);
        startService(serviceIntent);
    }

    private void handleSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_period_options, R.layout.spinner_item_dropdown);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usage_SPN_timeSelector.setAdapter(adapter);
        usage_SPN_timeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearPieChartHighlight();
                hideCardView();
                String selectedItem = parent.getItemAtPosition(position).toString();
                loadPieChartData(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showCardView(PieEntry entry) {
        String selectedAppName = entry.getLabel();
        AppUsageData.AppUsageInfo selectedAppInfo = null;

        String timePeriod = usage_SPN_timeSelector.getSelectedItem().toString();
        if (timePeriod.equals("Weekly"))
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_WEEK;
        else if (timePeriod.equals("Monthly"))
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_MONTH;
        else
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_DAY;

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
            for (AppUsageData.AppUsageInfo appUsageInfo : appUsageData.getAppUsageStats(timePeriod)) {
                if (appUsageInfo.getAppName().equals(selectedAppName)) {
                    selectedAppInfo = appUsageInfo;
                    break;
                }
            }
        }
        if (selectedAppInfo != null) {
            usage_CRD_cardContainer.setVisibility(View.VISIBLE);
            card_LBL_name.setText(selectedAppInfo.getAppName()); // Correct name
            card_LBL_usage.setText("Time: " + selectedAppInfo.getFormattedUsageTime(periodMillis));
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
        usage_CRT_piechart.setCenterTextSize(15);
        usage_CRT_piechart.getDescription().setEnabled(false);
        usage_CRT_piechart.setRotationEnabled(false);
        usage_CRT_piechart.getLegend().setEnabled(false);
        usage_CRT_piechart.setHighlightPerTapEnabled(true);
    }

    private void loadPieChartData(String timePeriod) {
        // Fetch fresh app usage data for the selected time period
        List<AppUsageData.AppUsageInfo> appUsageInfoList = appUsageData.getAppUsageStats(timePeriod);
        ArrayList<PieEntry> entries = new ArrayList<>();
        long totalUsageTime = 0;
        double thresholdPercentage = 0.01; // 1% threshold
        otherUsageTime = 0;

        if(timePeriod.equals("Weekly"))
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_WEEK;
        else if (timePeriod.equals("Monthly"))
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_MONTH;
        else
            periodMillis = AppUsageData.TOTAL_MILLIS_IN_DAY;

        // Recompute the total usage time
        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageInfoList) {
            totalUsageTime += appUsageInfo.getUsageTimeInMillis();
        }

        totalScreenOffTime = periodMillis - totalUsageTime;

        if (usage_CHBX_screenOff.isChecked() && totalScreenOffTime > 0) {
            entries.add(new PieEntry(totalScreenOffTime, "Screen Off"));
        }

        // Accumulate and categorize small usage times into "Other"
        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageInfoList) {
            double usagePercentage = (double) appUsageInfo.getUsageTimeInMillis() / totalUsageTime;
            if (usagePercentage < thresholdPercentage) { // Less than 1% usage
                otherUsageTime += appUsageInfo.getUsageTimeInMillis();
            } else {
                entries.add(new PieEntry(appUsageInfo.getUsageTimeInMillis(), appUsageInfo.getAppName()));

            }
        }

        // Add "Other" category if it exists
        if (otherUsageTime > 0) {
            entries.add(new PieEntry(otherUsageTime, "Other"));
        }

        // Sort entries by value (descending order)
        entries.sort((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));

        // Apply the sorted entries to the PieDataSet and refresh the chart
        PieDataSet dataSet = new PieDataSet(entries, "App Usage");
        dataSet.setColors(getCustomColors()); // Ensure colors are correctly applied
        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueTextSize(12f);
        data.setValueTextColor(android.R.color.black);

        usage_CRT_piechart.setData(data);
        usage_CRT_piechart.invalidate(); // Refresh the chart
    }


    private void hideCardView() {
        if (cardViewLayout != null) {
            cardViewLayout.setVisibility(View.GONE);
        }
    }

    private void clearPieChartHighlight() {
        if (usage_CRT_piechart != null) {
            usage_CRT_piechart.highlightValues(null); // Clears all highlights
        }
    }


    private void findViews() {
        usage_CRT_piechart = findViewById(R.id.usage_CRT_piechart);
        usage_SPN_timeSelector =findViewById(R.id.usage_SPN_timeSelector);
        usage_CHBX_screenOff = findViewById(R.id.usage_CHBX_screenOff);
        usage_BTN_limit = findViewById(R.id.usage_BTN_limit);
        LayoutInflater inflater = LayoutInflater.from(this);
        cardViewLayout = inflater.inflate(R.layout.card_app_usage,null,false);
        usage_CRD_cardContainer = findViewById(R.id.usage_CRD_cardContainer);
        usage_CRD_cardContainer.addView(cardViewLayout);
        usage_CRD_cardContainer.setVisibility(View.INVISIBLE);
        card_IMG_icon = cardViewLayout.findViewById(R.id.card_IMG_icon);
        card_LBL_name = cardViewLayout.findViewById(R.id.card_LBL_name);
        card_LBL_usage = cardViewLayout.findViewById(R.id.card_LBL_usage);
    }
    private int[] getCustomColors(){
        return new int[] {
                ContextCompat.getColor(this, R.color.orange),
                ContextCompat.getColor(this, R.color.brown),
                ContextCompat.getColor(this, R.color.blue),
                ContextCompat.getColor(this, R.color.green),
                ContextCompat.getColor(this, R.color.amber),
                ContextCompat.getColor(this, R.color.purple),
                ContextCompat.getColor(this, R.color.light_blue),
                ContextCompat.getColor(this, R.color.pink),
                ContextCompat.getColor(this, R.color.light_green),
                ContextCompat.getColor(this, R.color.deep_orange),
                ContextCompat.getColor(this, R.color.deep_purple),
                ContextCompat.getColor(this, R.color.cyan),
                ContextCompat.getColor(this, R.color.forest_green),
                ContextCompat.getColor(this, R.color.yellow),
                ContextCompat.getColor(this, R.color.indigo),
                ContextCompat.getColor(this, R.color.teal),
                ContextCompat.getColor(this, R.color.blue_grey),
                ContextCompat.getColor(this, R.color.red),
                ContextCompat.getColor(this, R.color.dark_pink),
                ContextCompat.getColor(this, R.color.dark_brown),
        };
    }
}
