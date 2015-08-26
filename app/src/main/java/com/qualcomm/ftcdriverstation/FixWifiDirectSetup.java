package com.qualcomm.ftcdriverstation;

import android.net.wifi.WifiManager;

public class FixWifiDirectSetup {
    public static final int WIFI_TOGGLE_DELAY = 2000;

    public static void fixWifiDirectSetup(WifiManager wifiManager) throws InterruptedException {
        m247a(false, wifiManager);
        m247a(true, wifiManager);
    }

    private static void m247a(boolean z, WifiManager wifiManager) throws InterruptedException {
        wifiManager.setWifiEnabled(z);
        Thread.sleep(2000);
    }
}
