/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.osds;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import com.qualcomm.robotcore.robocol.PeerDiscoveryManager;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.wifi.NetworkConnection;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;

/**
 * Wifi Direct (WD) version of the Driver Station
 */
public class FtcDriverStationWdActivity extends FtcDriverStationActivity implements NetworkConnection.NetworkConnectionCallback{

	protected WifiDirectAssistant wifiDirect;
	protected String groupOwnerMac;

	protected boolean setupNeeded = true;

	public final static String PREF_USE_LAN_DS = "use_lan_ds";

	final static int AUTO_LAN_DS_REQUEST_CODE = 7;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		wifiDirect = WifiDirectAssistant.getWifiDirectAssistant(getApplicationContext());
		wifiDirect.setCallback(this);

		String notSetValue = getString(R.string.pref_driver_station_mac_default);

		if (PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_driver_station_mac), notSetValue).equals(notSetValue))
		{
			startActivity(new Intent(this, FtcPairWifiDirectActivity.class));
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		this.groupOwnerMac = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_driver_station_mac), getString(R.string.pref_driver_station_mac_default));

		if(preferences.getBoolean(PREF_USE_LAN_DS, false))
		{
			//automatically open the LAN DS
			startActivityForResult(new Intent(this, FtcDriverStationLanActivity.class), AUTO_LAN_DS_REQUEST_CODE);
		}
		else
		{
			wifiDirectStatus("Wifi Direct - Disconnected");
			this.wifiDirect.enable();
			if (!this.wifiDirect.isConnected())
			{
				this.wifiDirect.discoverPeers();
			} else if (!this.groupOwnerMac.equalsIgnoreCase(this.wifiDirect.getConnectionOwnerMacAddress()))
			{
				RobotLog.e("Wifi Direct - connected to " + this.wifiDirect.getConnectionOwnerMacAddress() + ", expected " + this.groupOwnerMac);
				wifiDirectStatus("Error: Connected to wrong device");
				WifiDirectReconfigurer.reconfigureWifi(this);
			}
		}
	}

	@Override
	protected void shutdown()
	{
		super.shutdown();
		// reset need for setup
		setupNeeded = true;
	}

	@Override
	protected void onResume() {

		super.onResume();
		this.setupNeeded = true;
	}

	@Override
	protected void onPause() {
	super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		wifiDirect.disable();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ftc_driver_station, menu);
		getMenuInflater().inflate(R.menu.wd_driver_station, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_switch_to_lan:
				//set the default to LAN
				preferences.edit().putBoolean(PREF_USE_LAN_DS, true).apply();
				startActivityForResult(new Intent(getBaseContext(), FtcDriverStationLanActivity.class), AUTO_LAN_DS_REQUEST_CODE);
				return true;
			//from common menu
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		//if the LAN DS was auto-opened and then closed by the user, close the whole app.
		if(requestCode == AUTO_LAN_DS_REQUEST_CODE)
		{
			finish();
		}
	}

	public CallbackResult onNetworkConnectionEvent(WifiDirectAssistant.Event event) {
		String msg;
		switch (event) {
			case PEERS_AVAILABLE:
				if (this.wifiDirect.getConnectStatus() != WifiDirectAssistant.ConnectStatus.CONNECTED && this.wifiDirect.getConnectStatus() != WifiDirectAssistant.ConnectStatus.CONNECTING) {
					if (this.groupOwnerMac.equals(getString(R.string.pref_driver_station_mac_default))) {
						wifiDirectStatus("Not Paired");
					} else {
						wifiDirectStatus("Searching");
					}
					for (WifiP2pDevice peer : this.wifiDirect.getPeers()) {
						if (peer.deviceAddress.equalsIgnoreCase(this.groupOwnerMac)) {
							this.wifiDirect.connect(peer.deviceAddress);
							return CallbackResult.HANDLED;
						}
					}
				}
				break;
			case GROUP_CREATED:
				RobotLog.e("Wifi Direct - connected as Group Owner, was expecting Peer");
				wifiDirectStatus("Error: Connected as Group Owner");
				WifiDirectReconfigurer.reconfigureWifi(this);
				return CallbackResult.HANDLED;

			case CONNECTING:
				wifiDirectStatus("Connecting");
				this.wifiDirect.cancelDiscoverPeers();
				return CallbackResult.HANDLED;

			case CONNECTED_AS_PEER:
			case CONNECTED_AS_GROUP_OWNER:

				RobotLog.e("Connected...");
				this.wifiDirect.cancelDiscoverPeers();
				if (this.groupOwnerMac.equalsIgnoreCase(this.wifiDirect.getConnectionOwnerMacAddress())) {
					wifiDirectStatus("Connected");

					wifiDirectStatus(getString(R.string.check_connected_to) + " " + this.wifiDirect.getGroupOwnerName());
					synchronized (this) {
						if (this.wifiDirect.isConnected() && this.setupNeeded) {
							this.setupNeeded = false;
							new Thread(new SetupRunnable()).start();
						}
					}
					return CallbackResult.HANDLED;
				}
				else
				{
					RobotLog.e("Wifi Direct - connected to \"" + this.wifiDirect.getConnectionOwnerMacAddress() + "\", expected \"" + this.groupOwnerMac + '\"');
					wifiDirectStatus("Error: Connected to wrong device");
					WifiDirectReconfigurer.reconfigureWifi(this);
					return CallbackResult.HANDLED;
				}

			case DISCONNECTED:
				msg = "Disconnected";
				wifiDirectStatus(msg);
				RobotLog.i("Wifi Direct - " + msg);
				this.wifiDirect.discoverPeers();
				return CallbackResult.HANDLED;
			case ERROR:
				if (wifiDirect.getFailureReason().equals("BUSY")) {
					msg = "Waiting to Connect";
				} else {
					msg = "Error: " + this.wifiDirect.getFailureReason();
				}
				wifiDirectStatus(msg);
				RobotLog.i("Wifi Direct - " + msg);
				return CallbackResult.HANDLED;
		}

		return CallbackResult.NOT_HANDLED;
	}

	protected class SetupRunnable implements Runnable {
		@Override
		public void run() {
			InetAddress groupOwnerAddr = FtcDriverStationWdActivity.this.wifiDirect.getGroupOwnerAddress();

			try {
				if (FtcDriverStationWdActivity.this.socket != null) {
					FtcDriverStationWdActivity.this.socket.close();
				}


				FtcDriverStationWdActivity.this.socket = new RobocolDatagramSocket();
				FtcDriverStationWdActivity.this.socket.listenUsingDestination(groupOwnerAddr);
				FtcDriverStationWdActivity.this.socket.connect(groupOwnerAddr);
			} catch (SocketException e) {
				RobotLog.ee("DriverStationWd", e, "Failed to open socket");
			}
			if (FtcDriverStationWdActivity.this.peerDiscoveryManager != null) {
				FtcDriverStationWdActivity.this.peerDiscoveryManager.stop();
			}
			FtcDriverStationWdActivity.this.peerDiscoveryManager = new PeerDiscoveryManager(FtcDriverStationWdActivity.this.socket, groupOwnerAddr);
			FtcDriverStationWdActivity.this.recvLoopService = Executors.newSingleThreadExecutor();
			FtcDriverStationWdActivity.this.recvLoopService.execute(new RecvLoopRunnable());
			RobotLog.i("Setup complete");
		}
	}

}
