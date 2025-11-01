package com.pojie.wifipojie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PojieActivity extends AppCompatActivity {

    private EditText EtSSID, EtPass, EtTime;
    private TextView TvStatus, TvDictPath;
    private Button BtnStart, BtnSelectDict;
    private RadioButton RadioApi, RadioApi29, RadioRoot;
    private CheckBox CheckUsePass;

    private static final String APP_CONFIG = "app_config";
    private static final String TASK_STATE = "task_state";
    private static final int DICT_REQUEST_CODE = 1002;
    private String currentDictionaryPath = "";

    private MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojie);

        TvStatus = findViewById(R.id.TvStatus);
        TvDictPath = findViewById(R.id.TvDictPath);
        EtSSID = findViewById(R.id.EtSSID);
        EtPass = findViewById(R.id.EtPass);
        EtTime = findViewById(R.id.EtTime);
        BtnStart = findViewById(R.id.BtnStart);
        BtnSelectDict = findViewById(R.id.BtnSelectDict);
        RadioApi = findViewById(R.id.RadioApi);
        RadioApi29 = findViewById(R.id.RadioApi29);
        RadioRoot = findViewById(R.id.RadioRoot);
        CheckUsePass = findViewById(R.id.CheckUsePass);

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.pojie.wifipojie.UPDATE_UI");
        registerReceiver(myReceiver, intentFilter);

        loadConfiguration();
        checkPausedTask();

        BtnStart.setOnClickListener(v -> handleStartButton());
        BtnSelectDict.setOnClickListener(v -> {
            Intent intent = new Intent(PojieActivity.this, DictionaryManagerActivity.class);
            startActivityForResult(intent, DICT_REQUEST_CODE);
        });

        CheckUsePass.setOnCheckedChangeListener((buttonView, isChecked) -> EtPass.setEnabled(isChecked));
    }

    private void handleStartButton() {
        if ("Pause".equals(BtnStart.getText().toString())) {
            Intent intent = new Intent(this, WifiPojieService.class);
            intent.setAction("com.pojie.wifipojie.ACTION_PAUSE");
            startService(intent);
        } else { // Handle "Start" and "Resume"
            startCracking();
        }
    }

    private void startCracking() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        } catch (Exception e) { e.printStackTrace(); }

        String ssid = EtSSID.getText().toString();
        if (TextUtils.isEmpty(ssid)) {
            Toast.makeText(this, "SSID cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!CheckUsePass.isChecked() && TextUtils.isEmpty(currentDictionaryPath)) {
            Toast.makeText(this, "Please select a dictionary file", Toast.LENGTH_SHORT).show();
            return;
        }

        saveConfiguration();

        Intent intent = new Intent(this, WifiPojieService.class);
        intent.setAction("com.pojie.wifipojie.ACTION_START");
        intent.putExtra("ssid", ssid);
        intent.putExtra("password", EtPass.getText().toString());
        intent.putExtra("dicpath", CheckUsePass.isChecked() ? "" : currentDictionaryPath);
        intent.putExtra("timeout", Integer.parseInt(EtTime.getText().toString()));
        if (RadioApi.isChecked()) intent.putExtra("connectType", 0);
        else if (RadioApi29.isChecked()) intent.putExtra("connectType", 1);
        else if (RadioRoot.isChecked()) intent.putExtra("connectType", 2);
        startService(intent);
    }

    private void saveConfiguration() {
        SharedPreferences.Editor editor = getSharedPreferences(APP_CONFIG, MODE_PRIVATE).edit();
        editor.putString("last_ssid", EtSSID.getText().toString());
        editor.putString("last_dict_path", currentDictionaryPath);
        editor.putString("last_dict_name", TvDictPath.getText().toString());
        editor.putString("last_timeout", EtTime.getText().toString());
        int type = RadioApi.isChecked() ? 0 : (RadioApi29.isChecked() ? 1 : 2);
        editor.putInt("last_connect_type", type);
        editor.apply();
    }

    private void loadConfiguration() {
        SharedPreferences prefs = getSharedPreferences(APP_CONFIG, MODE_PRIVATE);
        EtSSID.setText(prefs.getString("last_ssid", ""));
        currentDictionaryPath = prefs.getString("last_dict_path", "");
        TvDictPath.setText(prefs.getString("last_dict_name", "No dictionary selected"));
        EtTime.setText(prefs.getString("last_timeout", "10"));
        int type = prefs.getInt("last_connect_type", 0);
        if (type == 0) RadioApi.setChecked(true);
        else if (type == 1) RadioApi29.setChecked(true);
        else RadioRoot.setChecked(true);
    }

    private void checkPausedTask() {
        SharedPreferences prefs = getSharedPreferences(TASK_STATE, MODE_PRIVATE);
        boolean isPaused = prefs.getBoolean("is_paused", false);
        if (isPaused) {
            String pausedSsid = prefs.getString("paused_ssid", "");
            EtSSID.setText(pausedSsid);
            currentDictionaryPath = prefs.getString("paused_dict_path", "");
            if(!TextUtils.isEmpty(currentDictionaryPath)) {
                TvDictPath.setText(new File(currentDictionaryPath).getName());
            }
            int progress = prefs.getInt("paused_index", 0);
            TvStatus.setText("Task paused at progress: " + progress);
            BtnStart.setText("Resume");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DICT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            currentDictionaryPath = data.getStringExtra("selected_dictionary_path");
            String dictName = data.getStringExtra("selected_dictionary_name");
            TvDictPath.setText(dictName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            String uiState = intent.getStringExtra("ui_state");
            TvStatus.setText(status);

            if (uiState != null) {
                switch (uiState) {
                    case "RUNNING":
                        BtnStart.setText("Pause");
                        setInputsEnabled(false);
                        break;
                    case "PAUSED":
                        BtnStart.setText("Resume");
                        setInputsEnabled(true);
                        break;
                    case "STOPPED":
                    default:
                        BtnStart.setText("Start");
                        setInputsEnabled(true);
                        // Clear paused task state from prefs if task is finished/stopped
                        getSharedPreferences(TASK_STATE, MODE_PRIVATE).edit().clear().apply();
                        break;
                }
            }
        }
    }

    private void setInputsEnabled(boolean enabled) {
        EtSSID.setEnabled(enabled);
        BtnSelectDict.setEnabled(enabled);
        EtTime.setEnabled(enabled);
        RadioApi.setEnabled(enabled);
        RadioApi29.setEnabled(enabled);
        RadioRoot.setEnabled(enabled);
        CheckUsePass.setEnabled(enabled);
        if (enabled) {
            // Re-check checkbox state if enabling inputs
            EtPass.setEnabled(CheckUsePass.isChecked());
        } else {
            EtPass.setEnabled(false);
        }
    }
}
