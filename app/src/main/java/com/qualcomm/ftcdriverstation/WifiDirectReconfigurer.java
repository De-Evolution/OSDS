package com.qualcomm.ftcdriverstation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.qualcomm.ftccommon.DbgLog;

public class WifiDirectReconfigurer implements Runnable {

    Activity app;
	ProgressDialog dialog;

    class DialogShower implements Runnable {

        public void run() {

	        ProgressDialog dalog = WifiDirectReconfigurer.this.dialog;
            WifiDirectReconfigurer.this.dialog = new ProgressDialog(WifiDirectReconfigurer.this.app, android.R.style.Theme_Holo_Dialog);
            dialog.setMessage("Please wait");
            dialog.setTitle("\"Troubleshooting\" Wifi Direct by resetting Wifi driver");
            dialog.setIndeterminate(true);
            dialog.show();
        }
    }

    class DialogHider implements Runnable {

        public void run() {
	        WifiDirectReconfigurer.this.dialog.dismiss();
        }
    }

    public WifiDirectReconfigurer(Activity app) {
	    this.app = app;
    }

    public void run() {
        DbgLog.msg("attempting to reconfigure Wifi Direct");
        app.runOnUiThread(new DialogShower());
        try {
            FixWifiDirectSetup.fixWifiDirectSetup((WifiManager) app.getSystemService(Context.WIFI_SERVICE));
        } catch (InterruptedException e) {
            DbgLog.msg("Cannot fix wifi setup - interrupted");
        }
        app.runOnUiThread(new DialogHider());
    }
}
