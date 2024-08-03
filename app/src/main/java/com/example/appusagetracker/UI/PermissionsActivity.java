package com.example.appusagetracker.UI;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.example.appusagetracker.R;
import com.example.appusagetracker.UI.MainActivity;
import com.google.android.material.button.MaterialButton;

public class PermissionsActivity extends AppCompatActivity {
    private MaterialButton permissions_BTN_allowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        permissions_BTN_allowButton = findViewById(R.id.permissions_BTN_allowButton);
        permissions_BTN_allowButton.setOnClickListener(v->{
            requestUsageStatsPermission();
        });
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(hasUsageStatsPermission()){
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}