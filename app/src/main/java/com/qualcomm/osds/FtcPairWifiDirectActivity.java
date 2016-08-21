package com.qualcomm.osds;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.BuildConfig;
import com.qualcomm.robotcore.wifi.NetworkConnection;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FtcPairWifiDirectActivity extends Activity implements OnClickListener, NetworkConnection.NetworkConnectionCallback {
	private static final int WIFI_SCAN_RATE = 10000;
	private String driverStationMac;
	private SharedPreferences sharedPref;
	private WifiDirectAssistant wifiDirect;
	private Handler wifiDirectHandler;
	private WifiDirectRunnable wifiDirectRunnable;


	public static class PeerRadioButton extends RadioButton {
		private String peerMacAddress;

		public PeerRadioButton(Context context) {
			super(context);
			this.peerMacAddress = BuildConfig.VERSION_NAME;
		}

		public String getPeerMacAddress() {
			return this.peerMacAddress;
		}

		public void setPeerMacAddress(String peerMacAddress) {
			this.peerMacAddress = peerMacAddress;
		}
	}

	public class WifiDirectRunnable implements Runnable {
		public void run() {
			FtcPairWifiDirectActivity.this.wifiDirect.discoverPeers();
			FtcPairWifiDirectActivity.this.wifiDirectHandler.postDelayed(FtcPairWifiDirectActivity.this.wifiDirectRunnable, 10000);
		}
	}

	public FtcPairWifiDirectActivity() {
		this.wifiDirectHandler = new Handler();
		this.wifiDirectRunnable = new WifiDirectRunnable();
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ftc_pair_wifi_direct);
		this.wifiDirect = WifiDirectAssistant.getWifiDirectAssistant(this);
	}

	public void onStart() {
		super.onStart();
		DbgLog.msg("Starting Pairing with Driver Station activity");
		this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		this.driverStationMac = this.sharedPref.getString(getString(R.string.pref_driver_station_mac), getString(R.string.pref_driver_station_mac_default));
		this.wifiDirect.enable();
		this.wifiDirect.setCallback(this);
		updateDevicesList(this.wifiDirect.getPeers());
		this.wifiDirectHandler.postDelayed(this.wifiDirectRunnable, 10000);
	}

	public void onStop() {
		super.onStop();
		this.wifiDirectHandler.removeCallbacks(this.wifiDirectRunnable);
		this.wifiDirect.cancelDiscoverPeers();
		this.wifiDirect.disable();
	}

	public void onClick(View view) {
		if (view instanceof PeerRadioButton) {
			PeerRadioButton button = (PeerRadioButton) view;
			if (button.getId() == 0) {
				this.driverStationMac = getString(R.string.pref_driver_station_mac_default);
			} else {
				this.driverStationMac = button.getPeerMacAddress();
			}
			Editor editor = this.sharedPref.edit();
			editor.putString(getString(R.string.pref_driver_station_mac), this.driverStationMac);
			editor.commit();
			DbgLog.msg("Setting Driver Station MAC address to " + this.driverStationMac);
		}
	}

	public void onClickButtonBack(View view) {
		finish();
	}

	public CallbackResult onNetworkConnectionEvent(WifiDirectAssistant.Event event) {
		switch (event) {
			case PEERS_AVAILABLE:
				updateDevicesList(this.wifiDirect.getPeers());
				return CallbackResult.HANDLED;
		}

		return CallbackResult.NOT_HANDLED;
	}

	private void updateDevicesList(List<WifiP2pDevice> peers) {
		RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupDevices);
		rg.clearCheck();
		rg.removeAllViews();
		PeerRadioButton button = new PeerRadioButton(this);
		String none = getString(R.string.pref_driver_station_mac_default);
		button.setId(0);
		button.setText("None\nDo not pair with any device");
		button.setPadding(0, 0, 0, 24);
		button.setOnClickListener(this);
		button.setPeerMacAddress(none);
		if (this.driverStationMac.equalsIgnoreCase(none)) {
			button.setChecked(true);
		}
		rg.addView(button);

		int i = 1;
		Map<String, String> namesAndAddresses = buildMap(peers);
		for (String name : namesAndAddresses.keySet()) {
			String deviceAddress = (String) namesAndAddresses.get(name);
			button = new PeerRadioButton(this);
			int i2 = i + 1;
			button.setId(i);
			button.setText(name + "\n" + deviceAddress);
			button.setPadding(0, 0, 0, 24);
			button.setPeerMacAddress(deviceAddress);
			if (deviceAddress.equalsIgnoreCase(this.driverStationMac)) {
				button.setChecked(true);
			}
			button.setOnClickListener(this);
			rg.addView(button);
			i = i2;
		}
	}

	public Map<String, String> buildMap(List<WifiP2pDevice> peers) {
		Map<String, String> map = new TreeMap();
		for (WifiP2pDevice peer : peers) {
			map.put(peer.deviceName, peer.deviceAddress);
		}
		return map;
	}
}
