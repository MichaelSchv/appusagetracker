package com.example.appusagetracker.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

import android.content.Intent;
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

        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageData.getAppUsageStats()) {
            if (appUsageInfo.getAppName().equals(selectedAppName)) {
                selectedAppInfo = appUsageInfo;
                break;
            }
        }

        if (selectedAppInfo != null) {
            usage_CRD_cardContainer.setVisibility(View.VISIBLE);
            card_LBL_name.setText(selectedAppName);
            card_LBL_usage.setText("Time: " + selectedAppInfo.getFormattedUsageTime());
            try {
                Drawable appIconDrawable = getPackageManager().getApplicationIcon(selectedAppInfo.getAppName());
                card_IMG_icon.setImageDrawable(appIconDrawable);
            }
            catch (PackageManager.NameNotFoundException e) {
                card_IMG_icon.setImageResource(R.drawable.ic_launcher_background);
            }

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

        for (AppUsageData.AppUsageInfo appUsageInfo : appUsageInfoList) {
            entries.add(new PieEntry(appUsageInfo.getUsageTimeInMillis(), appUsageInfo.getAppName()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "App Usage");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueTextSize(12f);
        data.setValueTextColor(android.R.color.black);

        usage_CRT_piechart.setData(data);
        usage_CRT_piechart.invalidate();
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
