package com.example.appusagetracker.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.example.appusagetracker.Adapter.AppLimitAdapter;
import com.example.appusagetracker.Data.AppUsageData;
import com.example.appusagetracker.R;

import java.util.List;

public class LimitActivity extends AppCompatActivity {

    private RecyclerView limit_RCV_appList;
    private AppLimitAdapter adapter;
    private AppUsageData appUsageData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_limit);
        findViews();
        appUsageData = new AppUsageData(this);
        limit_RCV_appList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppLimitAdapter(getAppUsageList(), this);
        limit_RCV_appList.setAdapter(adapter);
    }

    private List<AppUsageData.AppUsageInfo> getAppUsageList() {
        return appUsageData.getAppUsageStats("Daily");
    }

    private void findViews() {
        limit_RCV_appList = findViewById(R.id.limit_RCV_appList);
    }
}