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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.robocol.PeerDiscoveryManager;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

/**
 * LAN version of the Driver Station which just connects to a specified IP address
 */
public class FtcDriverStationLanActivity extends FtcDriverStationActivity implements DialogInterface.OnDismissListener
{
	InetAddress robotControllerAddress;

	final static int IP_DIALOG_ID = 1;

	@Override
	protected Dialog onCreateDialog(int id)
	{
		if(id == IP_DIALOG_ID)
		{
			IPAddressDialog ipDialog
					= new IPAddressDialog(this, preferences);
			ipDialog.setOnDismissListener(this);
			return ipDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String ipString = preferences.getString(IPAddressDialog.IP_ADDRESS_PREFERENCE, "");
		try
		{
			robotControllerAddress = InetAddress.getByName(ipString);
		}
		catch (UnknownHostException e)
		{
			Log.w("FtcDriverStationLan", "Somehow, the stored IP address preferences is not valid!");
		}
		displayConnectionNotice();
	}

	private void displayConnectionNotice()
	{
		wifiDirectStatus("Connecting to " + robotControllerAddress.getHostAddress());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_set_ip:
				showDialog(IP_DIALOG_ID);
				return true;
			case R.id.action_switch_to_wd:
				//set the default to WD DS
				preferences.edit().putBoolean(FtcDriverStationWdActivity.PREF_USE_LAN_DS, false).apply();
				startActivity(new Intent(getBaseContext(), FtcDriverStationWdActivity.class));
				return true;
			//from common menu
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void assumeClientConnect()
	{
		super.assumeClientConnect();
		wifiDirectStatus(getString(R.string.check_connected_to) + " " + robotControllerAddress.getHostAddress());
	}

	@Override
	protected void assumeClientDisconnect()
	{
		super.assumeClientDisconnect();
		wifiDirectStatus("Disconnected.");
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		new Thread(new SetupRunnable()).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ftc_driver_station, menu);
		getMenuInflater().inflate(R.menu.lan_driver_station, menu);
		return true;
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		String newIP = preferences.getString(IPAddressDialog.IP_ADDRESS_PREFERENCE, "");
		if(Patterns.IP_ADDRESS.matcher(newIP).matches())
		{
			try
			{
				robotControllerAddress = InetAddress.getByName(newIP);
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
			assumeClientDisconnect();
			shutdown();
			new Thread(new SetupRunnable()).start();
		}
		else
		{
			Log.w("FtcDriverStationLan", "Somehow, the stored IP address preferences is not valid!");
		}
	}
	
	protected class SetupRunnable implements Runnable {
		@Override
		public void run() {

			try
			{
				if (FtcDriverStationLanActivity.this.socket != null) {
					FtcDriverStationLanActivity.this.socket.close();
				}
				FtcDriverStationLanActivity.this.socket = new RobocolDatagramSocket();
				FtcDriverStationLanActivity.this.socket.listen(robotControllerAddress);
				FtcDriverStationLanActivity.this.socket.connect(robotControllerAddress);
			}
			catch (SocketException e) {
				DbgLog.error("Failed to open socket: " + e.toString());
			}


			if (FtcDriverStationLanActivity.this.peerDiscoveryManager != null) {
				FtcDriverStationLanActivity.this.peerDiscoveryManager.stop();
			}
			FtcDriverStationLanActivity.this.peerDiscoveryManager = new PeerDiscoveryManager(FtcDriverStationLanActivity.this.socket);
			FtcDriverStationLanActivity.this.peerDiscoveryManager.start(robotControllerAddress);
			FtcDriverStationLanActivity.this.recvLoopService = Executors.newSingleThreadExecutor();
			FtcDriverStationLanActivity.this.recvLoopService.execute(new RecvLoopRunnable());
			DbgLog.msg("Setup complete");
		}
	}

}
