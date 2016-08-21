package com.qualcomm.osds;

import android.net.wifi.WifiManager;

public class FixWifiDirectSetup {
	public static final int WIFI_TOGGLE_DELAY = 2000;

	public static void fixWifiDirectSetup(WifiManager wifiManager) throws InterruptedException {
		changeWifiAndWait(false, wifiManager);
		changeWifiAndWait(true, wifiManager);
	}

	private static void changeWifiAndWait(boolean on, WifiManager wifiManager) throws InterruptedException {
		wifiManager.setWifiEnabled(on);
		Thread.sleep(WIFI_TOGGLE_DELAY);
	}
}
