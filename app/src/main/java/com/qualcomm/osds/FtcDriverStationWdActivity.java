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

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.robocol.PeerDiscoveryManager;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant;
import com.qualcomm.robotcore.wifi.WifiDirectAssistant.WifiDirectAssistantCallback;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;

/**
 * Wifi Direct (WD) version of the Driver Station
 */
public class FtcDriverStationWdActivity extends FtcDriverStationActivity
	implements WifiDirectAssistantCallback{

  protected boolean clientConnected = false;

  protected InetAddress remoteAddr;
  protected WifiDirectAssistant wifiDirect;
	protected String groupOwnerMac;

	protected boolean setupNeeded = true;

	@Override
  protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wifiDirect = WifiDirectAssistant.getWifiDirectAssistant(getApplicationContext());
		wifiDirect.setCallback(this);

	  String notSetValue =  getString(R.string.pref_driver_station_mac_default);

	  if(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_driver_station_mac),notSetValue).equals(notSetValue))
	  {
		  startActivity(new Intent(this, FtcPairWifiDirectActivity.class));
	  }
  }

  @Override
  protected void onStart()
  {
	  super.onStart();

	  this.groupOwnerMac = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_driver_station_mac), getString(R.string.pref_driver_station_mac_default));

	  wifiDirectStatus("Wifi Direct - Disconnected");
	  this.wifiDirect.enable();
	  if (!this.wifiDirect.isConnected())
	  {
		  this.wifiDirect.discoverPeers();
	  }
	  else if (!this.groupOwnerMac.equalsIgnoreCase(this.wifiDirect.getGroupOwnerMacAddress()))
	  {
		  DbgLog.error("Wifi Direct - connected to " + this.wifiDirect.getGroupOwnerMacAddress() + ", expected " + this.groupOwnerMac);
		  wifiDirectStatus("Error: Connected to wrong device");
		  WifiDirectReconfigurer.reconfigureWifi(this);
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
				startActivity(new Intent(getBaseContext(), FtcDriverStationLanActivity.class));
				return true;
			//from common menu
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void onWifiDirectEvent(WifiDirectAssistant.Event event) {
		String msg;
		switch (event)
		{
		  case PEERS_AVAILABLE:
				if (this.wifiDirect.getConnectStatus() != WifiDirectAssistant.ConnectStatus.CONNECTED && this.wifiDirect.getConnectStatus() != WifiDirectAssistant.ConnectStatus.CONNECTING) {
					if (this.groupOwnerMac.equals(getString(R.string.pref_driver_station_mac_default))) {
						wifiDirectStatus("Not Paired");
					} else {
						wifiDirectStatus("Searching");
					}
					for (WifiP2pDevice peer : this.wifiDirect.getPeers()) {
						if (peer.deviceAddress.equalsIgnoreCase(this.groupOwnerMac))
						{
							this.wifiDirect.connect(peer);
							return;
						}
					}
				}
			case GROUP_CREATED:
				DbgLog.error("Wifi Direct - connected as Group Owner, was expecting Peer");
				wifiDirectStatus("Error: Connected as Group Owner");
				//WifiDirectReconfigurer.reconfigureWifi(this);
		  case CONNECTING:
				wifiDirectStatus("Connecting");
				this.wifiDirect.cancelDiscoverPeers();
			case CONNECTED_AS_PEER:
				this.wifiDirect.cancelDiscoverPeers();
				wifiDirectStatus("Connected");
			case CONNECTED_AS_GROUP_OWNER:
				wifiDirectStatus(getString(R.string.check_connected_to) + " " + this.wifiDirect.getGroupOwnerName());
				if (this.groupOwnerMac.equalsIgnoreCase(this.wifiDirect.getGroupOwnerMacAddress())) {
					synchronized (this) {
						if (this.wifiDirect.isConnected() && this.setupNeeded) {
							this.setupNeeded = false;
							new Thread(new SetupRunnable()).start();
						}
						break;
					}
				}
				DbgLog.error("Wifi Direct - connected to \"" + this.wifiDirect.getGroupOwnerMacAddress() + "\", expected \"" + this.groupOwnerMac + '\"');
				wifiDirectStatus("Error: Connected to wrong device");
				//WifiDirectReconfigurer.reconfigureWifi(this);
			case DISCONNECTED:
				msg = "Disconnected";
				wifiDirectStatus(msg);
				DbgLog.msg("Wifi Direct - " + msg);
				this.wifiDirect.discoverPeers();
			case ERROR:
				if(wifiDirect.getFailureReason().equals("BUSY"))
				{
					msg = "Waiting to Connect";
				}
				else
				{
					msg = "Error: " + this.wifiDirect.getFailureReason();
				}
				wifiDirectStatus(msg);
				DbgLog.msg("Wifi Direct - " + msg);
			default:
		}
	}

	protected class SetupRunnable implements Runnable {
		@Override
		public void run() {
			try {
				if (FtcDriverStationWdActivity.this.socket != null) {
					FtcDriverStationWdActivity.this.socket.close();
				}
				FtcDriverStationWdActivity.this.socket = new RobocolDatagramSocket();
				FtcDriverStationWdActivity.this.socket.listen(FtcDriverStationWdActivity.this.wifiDirect.getGroupOwnerAddress());
				FtcDriverStationWdActivity.this.socket.connect(FtcDriverStationWdActivity.this.wifiDirect.getGroupOwnerAddress());
			} catch (SocketException e) {
				DbgLog.error("Failed to open socket: " + e.toString());
			}
			if (FtcDriverStationWdActivity.this.peerDiscoveryManager != null) {
				FtcDriverStationWdActivity.this.peerDiscoveryManager.stop();
			}
			FtcDriverStationWdActivity.this.peerDiscoveryManager = new PeerDiscoveryManager(FtcDriverStationWdActivity.this.socket);
			FtcDriverStationWdActivity.this.peerDiscoveryManager.start(FtcDriverStationWdActivity.this.wifiDirect.getGroupOwnerAddress());
			FtcDriverStationWdActivity.this.recvLoopService = Executors.newSingleThreadExecutor();
			FtcDriverStationWdActivity.this.recvLoopService.execute(new RecvLoopRunnable());
			DbgLog.msg("Setup complete");
		}
	}
  
}
