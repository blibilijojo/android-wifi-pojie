package com.pojie.wifipojie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WifiPojieService extends Service {

    private WifiPojie wifiPojie;
    private String currentSsid, currentDictPath;
    private static final String TASK_STATE = "task_state";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "com.pojie.wifipojie.ACTION_START":
                    startCracking(intent);
                    break;
                case "com.pojie.wifipojie.ACTION_PAUSE":
                    pauseTask();
                    break;
                case "com.pojie.wifipojie.ACTION_STOP":
                    stopTask();
                    break;
            }
        }
        return START_NOT_STICKY; // Use NOT_STICKY to prevent auto-restart
    }

    private void startCracking(Intent intent) {
        // Stop any existing task before starting a new one
        if (wifiPojie != null) {
            wifiPojie.destroy(false);
            wifiPojie = null;
        }

        Notification notification = new NotificationCompat.Builder(this, "WifiPojieChannel")
                .setContentTitle("Wi-Fi Cracking Service")
                .setContentText("Cracking in progress...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, notification);

        this.currentSsid = intent.getStringExtra("ssid");
        this.currentDictPath = intent.getStringExtra("dicpath");
        String password = intent.getStringExtra("password");
        int timeout = intent.getIntExtra("timeout", 10);
        int connectType = intent.getIntExtra("connectType", 0);

        SharedPreferences prefs = getSharedPreferences(TASK_STATE, MODE_PRIVATE);
        boolean isPaused = prefs.getBoolean("is_paused", false);
        int startIndex = 0;

        if (isPaused && currentSsid.equals(prefs.getString("paused_ssid", ""))
                   && (currentDictPath != null && currentDictPath.equals(prefs.getString("paused_dict_path", "")))) {
            startIndex = prefs.getInt("paused_index", 0);
            Log.d("WifiPojieService", "Resuming task from index: " + startIndex);
        }

        // Clear task state once we start/resume
        prefs.edit().clear().apply();

        wifiPojie = new WifiPojie(this, currentSsid, currentDictPath, password, timeout, connectType, startIndex,
            new WifiPojie.OnPojieListener() {
                @Override
                public void onStatusUpdate(String message) {
                    Log.d("WifiPojieService", message);
                    sendUpdateBroadcast(message, "RUNNING");
                }

                @Override
                public void onFinished(boolean isSuccess, String password) {
                    Log.d("WifiPojieService", "Finished. Success: " + isSuccess);
                    String finalMessage = isSuccess ? "Success! Password: " + password : "Failed to find password.";
                    sendUpdateBroadcast(finalMessage, "STOPPED");
                    stopSelf();
                }
            });
        wifiPojie.start();
    }

    private void pauseTask() {
        if (wifiPojie != null) {
            int pauseIndex = wifiPojie.getCurrentTryIndex();
            wifiPojie.destroy(false);
            wifiPojie = null;

            SharedPreferences.Editor editor = getSharedPreferences(TASK_STATE, MODE_PRIVATE).edit();
            editor.putBoolean("is_paused", true);
            editor.putString("paused_ssid", currentSsid);
            editor.putString("paused_dict_path", currentDictPath);
            editor.putInt("paused_index", pauseIndex);
            editor.apply();

            String status = "Task paused at progress: " + pauseIndex;
            sendUpdateBroadcast(status, "PAUSED");
            Log.d("WifiPojieService", status);
            stopForeground(true);
        }
    }

    private void stopTask() {
        if (wifiPojie != null) {
            wifiPojie.destroy(true);
            wifiPojie = null;
        }
        getSharedPreferences(TASK_STATE, MODE_PRIVATE).edit().clear().apply();
        sendUpdateBroadcast("Task stopped by user.", "STOPPED");
        stopForeground(true);
        stopSelf();
    }

    private void sendUpdateBroadcast(String status, String uiState) {
        Intent intent = new Intent("com.pojie.wifipojie.UPDATE_UI");
        intent.putExtra("status", status);
        intent.putExtra("ui_state", uiState);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        stopTask();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "WifiPojieChannel",
                    "Wi-Fi Pojie Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
