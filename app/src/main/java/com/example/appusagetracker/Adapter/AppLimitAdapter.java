package com.example.appusagetracker.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appusagetracker.Data.AppUsageData;
import com.example.appusagetracker.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class AppLimitAdapter extends RecyclerView.Adapter<AppLimitAdapter.ViewHolder> {
    private List<AppUsageData.AppUsageInfo> appList;
    private SharedPreferences prefs;


    public AppLimitAdapter(List<AppUsageData.AppUsageInfo> appList, Context context) {
        this.appList=appList;
        this.prefs = context.getSharedPreferences("AppLimits", Context.MODE_PRIVATE);

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_app_limit,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppLimitAdapter.ViewHolder holder, int position) {
        final AppUsageData.AppUsageInfo app = appList.get(position);
        holder.cardLimit_IMG_icon.setImageDrawable(app.getAppIcon());
        holder.cardLimit_LBL_name.setText(app.getAppName());


        int[] limits = getUsageLimit(app.getAppName());

        if (limits[0] > 0 && limits[1] > 0 && limits[2] > 0) // The app is already limited
        {
            holder.cardLimit_BTN_limit.setText("Adjust Limit");
            holder.cardLimit_BTN_unlimit.setVisibility(View.VISIBLE);  // Show the "Unlimit" button
            holder.cardLimit_BTN_limit.setOnClickListener(v -> showSetLimitDialog(v.getContext(), app.getAppName(), limits));

            holder.cardLimit_BTN_unlimit.setOnClickListener(v -> {
                // Remove the limit from SharedPreferences
                removeUsageLimit(app.getAppName());
                // Update the UI after unlimiting
                holder.cardLimit_BTN_limit.setText("Limit");
                holder.cardLimit_BTN_unlimit.setVisibility(View.GONE);
                notifyItemChanged(holder.getAdapterPosition());
            });
        } else {
            // The app is not limited
            holder.cardLimit_BTN_limit.setText("Limit");
            holder.cardLimit_BTN_unlimit.setVisibility(View.GONE);  // Hide the "Unlimit" button
            holder.cardLimit_BTN_limit.setOnClickListener(v -> showSetLimitDialog(v.getContext(), app.getAppName(), null));
        }

    }

    private void showSetLimitDialog(Context context, final String appName, int[] limits) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set daily usage limit for " + appName);

        final View customLayout = LayoutInflater.from(context).inflate(R.layout.dialog_set_limit, null);
        builder.setView(customLayout);

        EditText hoursInput = customLayout.findViewById(R.id.limit_hours);
        EditText minutesInput = customLayout.findViewById(R.id.limit_minutes);
        EditText secondsInput = customLayout.findViewById(R.id.limit_seconds);

        // Pre-fill with existing limits if they exist
        if (limits != null) {
            hoursInput.setText(String.valueOf(limits[0]));
            minutesInput.setText(String.valueOf(limits[1]));
            secondsInput.setText(String.valueOf(limits[2]));
        }

        builder.setPositiveButton("Set", (dialogInterface, i) -> {
            int hours = hoursInput.getText().toString().isEmpty() ? 0 : Integer.parseInt(hoursInput.getText().toString());
            int minutes = minutesInput.getText().toString().isEmpty() ? 0 : Integer.parseInt(minutesInput.getText().toString());
            int seconds = secondsInput.getText().toString().isEmpty() ? 0 : Integer.parseInt(secondsInput.getText().toString());

            saveUsageLimit(appName, hours, minutes, seconds);
            Log.d("LIMITT", "Limit set for " + appName + ": " + hours + "h " + minutes + "m " + seconds + "s");
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();

        // Validate input before allowing dialog to close
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);

            TextWatcher inputWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String hoursStr = hoursInput.getText().toString();
                    String minutesStr = minutesInput.getText().toString();
                    String secondsStr = secondsInput.getText().toString();

                    int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
                    int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
                    int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);

                    boolean isValid = hours >= 0 && hours <= 23 &&
                            minutes >= 0 && minutes <= 59 &&
                            seconds >= 0 && seconds <= 59;

                    positiveButton.setEnabled(isValid);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            hoursInput.addTextChangedListener(inputWatcher);
            minutesInput.addTextChangedListener(inputWatcher);
            secondsInput.addTextChangedListener(inputWatcher);
        });

        dialog.show();
    }


    /*private void showSetLimitDialog(Context context, final String appName, int[] limits) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set daily usage limit for " + appName);

        final View customLayout = LayoutInflater.from(context).inflate(R.layout.dialog_set_limit, null);
        builder.setView(customLayout);

        builder.setPositiveButton("Set", (dialogInterface, i) -> {
            // The dialog automatically closes here, so input validation
            // would need to be done before this point.
        });

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();

        // Validate input before allowing dialog to close
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);

            EditText hoursInput = customLayout.findViewById(R.id.limit_hours);
            EditText minutesInput = customLayout.findViewById(R.id.limit_minutes);
            EditText secondsInput = customLayout.findViewById(R.id.limit_seconds);

            TextWatcher inputWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String hoursStr = hoursInput.getText().toString();
                    String minutesStr = minutesInput.getText().toString();
                    String secondsStr = secondsInput.getText().toString();

                    int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
                    int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
                    int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);

                    boolean isValid = hours >= 0 && hours <= 23 &&
                            minutes >= 0 && minutes <= 59 &&
                            seconds >= 0 && seconds <= 59;

                    positiveButton.setEnabled(isValid);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            hoursInput.addTextChangedListener(inputWatcher);
            minutesInput.addTextChangedListener(inputWatcher);
            secondsInput.addTextChangedListener(inputWatcher);
        });

        dialog.show();
    }*/

    @Override
    public int getItemCount() {
        return appList.size();
    }

    private void saveUsageLimit(String appName, int hours, int minutes, int seconds) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(appName + "_hours", hours);
        editor.putInt(appName + "_minutes", minutes);
        editor.putInt(appName + "_seconds", seconds);
        editor.apply(); // Apply changes asynchronously
    }

    private int[] getUsageLimit(String appName) {
        int hours = prefs.getInt(appName + "_hours", 0);
        int minutes = prefs.getInt(appName + "_minutes", 0);
        int seconds = prefs.getInt(appName + "_seconds", 0);
        return new int[]{hours, minutes, seconds};
    }

    private void removeUsageLimit(String appName) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(appName + "_hours");
        editor.remove(appName + "_minutes");
        editor.remove(appName + "_seconds");
        editor.apply();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ShapeableImageView cardLimit_IMG_icon;
        private MaterialTextView cardLimit_LBL_name;
        public MaterialButton cardLimit_BTN_unlimit;
        private MaterialButton cardLimit_BTN_limit;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardLimit_IMG_icon = itemView.findViewById(R.id.cardLimit_IMG_icon);
            cardLimit_LBL_name = itemView.findViewById(R.id.cardLimit_LBL_name);
            cardLimit_BTN_limit = itemView.findViewById(R.id.cardLimit_BTN_limit);
            cardLimit_BTN_unlimit = itemView.findViewById(R.id.cardLimit_BTN_unlimit);
        }
    }
}
