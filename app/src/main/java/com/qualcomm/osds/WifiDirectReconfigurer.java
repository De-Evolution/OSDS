package com.qualcomm.osds;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.RobotLog;

public class WifiDirectReconfigurer {


	public static void reconfigureWifi(final Activity app) {
		RobotLog.i("attempting to reconfigure Wifi Direct");
		final ProgressDialog dialog = new ProgressDialog(app, android.R.style.Theme_Holo_Dialog);
		dialog.setTitle("Please wait");
		dialog.setMessage("\"Troubleshooting\" Wifi Direct by resetting Wifi driver");
		dialog.setIndeterminate(true);
		dialog.show();

		Thread wifiFixThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try {
					FixWifiDirectSetup.fixWifiDirectSetup((WifiManager) app.getApplicationContext().getSystemService(Context.WIFI_SERVICE));
				} catch (InterruptedException e) {
					RobotLog.i("Cannot fix wifi setup - interrupted");
				}

				app.runOnUiThread(new Runnable(){

					@Override
					public void run()
					{
						if(dialog.isShowing())
						{
							dialog.dismiss();
						}
					}
				});
			}
		});

		wifiFixThread.start();


	}
}
