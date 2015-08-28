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
import com.qualcomm.osds.R;
import com.qualcomm.robotcore.BuildConfig;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant.Event;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant.WifiDirectAssistantCallback;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FtcPairWifiDirectActivity extends Activity implements OnClickListener, WifiDirectAssistantCallback {
    private static final int WIFI_SCAN_RATE = 10000;
    private String driverStationMac;
    private SharedPreferences sharedPref;
    private WifiDirectAssistant wifiDirect;
    private Handler wifiDirectHandler;
    private WifiDirectRunnable wifiDirectRunnable;

    /* renamed from: FtcPairWifiDirectActivity.1 */
    static /* synthetic */ class SwitchMap
    {
        static final /* synthetic */ int[] map;

        static {
            map = new int[Event.values().length];
            map[Event.PEERS_AVAILABLE.ordinal()] = 1;
        }
    }

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

    public void onWifiDirectEvent(Event event) {
        switch (SwitchMap.map[event.ordinal()]) {
            case com.qualcomm.ftccommon.BuildConfig.VERSION_CODE /*1*/:
                updateDevicesList(this.wifiDirect.getPeers());
            default:
        }
    }

    private void updateDevicesList(List<WifiP2pDevice> peers) {
        RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupDevices);
        rg.clearCheck();
        rg.removeAllViews();
        PeerRadioButton b = new PeerRadioButton(this);
        String none = getString(R.string.pref_driver_station_mac_default);
        b.setId(0);
        b.setText("None\nDo not pair with any device");
        b.setPadding(0, 0, 0, 24);
        b.setOnClickListener(this);
        b.setPeerMacAddress(none);
        if (this.driverStationMac.equalsIgnoreCase(none)) {
            b.setChecked(true);
        }
        rg.addView(b);
        int i = 1;
        Map<String, String> namesAndAddresses = buildMap(peers);
        for (String name : namesAndAddresses.keySet()) {
            String deviceAddress = (String) namesAndAddresses.get(name);
            b = new PeerRadioButton(this);
            int i2 = i + 1;
            b.setId(i);
            b.setText(name + "\n" + deviceAddress);
            b.setPadding(0, 0, 0, 24);
            b.setPeerMacAddress(deviceAddress);
            if (deviceAddress.equalsIgnoreCase(this.driverStationMac)) {
                b.setChecked(true);
            }
            b.setOnClickListener(this);
            rg.addView(b);
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
