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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.FtcEventLoopHandler;
import com.qualcomm.hardware.logitech.LogitechGamepadF310;
import com.qualcomm.hardware.microsoft.MicrosoftGamepadXbox360;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.Heartbeat;
import com.qualcomm.robotcore.robocol.PeerDiscoveryManager;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.util.BatteryChecker;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.RollingAverage;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;
import org.firstinspires.ftc.robotcore.internal.ui.GamepadUser;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class FtcDriverStationActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, OpModeSelectionDialogFragment.OpModeSelectionDialogListener
{

	public static final double ASSUME_DISCONNECT_TIMER = 2.0; // in seconds

	//for Gson deserialization
	final static Type listOfOpmodeMetaType = new TypeToken<List<OpModeMeta>>(){}.getType();

	protected static final int MAX_COMMAND_ATTEMPTS = 10;
	protected static final int MAX_LOG_SIZE = 2048;

	protected boolean clientConnected = false;

	protected String startTimedDefaultText;

	@SuppressLint("UseSparseArrays")
	protected Map<Integer, Gamepad> gamepads = new HashMap<>();
	protected Map<GamepadUser, Integer> userToGamepadMap = new HashMap<>(); // maps user to OS id of gamepad

	protected Heartbeat heartbeatSend = new Heartbeat();
	protected Heartbeat heartbeatRecv = new Heartbeat();

	protected ScheduledExecutorService sendLoopService = Executors.newSingleThreadScheduledExecutor();
	protected ScheduledFuture<?> sendLoopFuture;

	protected ExecutorService recvLoopService;

	protected InetAddress remoteAddr;
	protected RobocolDatagramSocket socket;

	protected OpModeMeta queuedOpMode = new OpModeMeta(OpModeManager.DEFAULT_OP_MODE_NAME);
	protected List<OpModeMeta> opModes = new ArrayList<OpModeMeta>();
	protected boolean opModeUseTimer = false; //used by onClickButtonStart to communicate whether the timer should be used
	protected OpModeCountDownTimer opModeCountDown = new OpModeCountDownTimer();
	protected RobotState robotState;

	protected RollingAverage pingAverage = new RollingAverage(10);
	protected ElapsedTime lastUiUpdate = new ElapsedTime();
	protected ElapsedTime lastRecvPacket = new ElapsedTime();

	protected Set<Command> pendingCommands = Collections.newSetFromMap(new ConcurrentHashMap<Command, Boolean>());
	
	protected TextView textWifiDirectStatus;
	protected TextView textPingStatus;
	protected TextView textRCBatteryPercent;
	protected TextView textRobotBatteryVoltage;
	protected TextView textOpModeLabel;
	protected TextView textOpModeName;
	protected TextView textTelemetry;

	//gamepad info TextViews
	protected TextView textuser1;
	protected TextView textuser2;

	protected Button buttonStart;
	protected Button buttonStartTimed;
	protected Button buttonSelect;
	protected Button buttonStop;
	protected Button buttonInit;

	protected PeerDiscoveryManager peerDiscoveryManager;

	//these need to be class scope so that we can cancel them
	protected ScaleAnimation user1ScaleAnimation;
	protected ScaleAnimation user2ScaleAnimation;

	protected Context context;
	protected SharedPreferences preferences;

	protected Typeface pixelFont;

	protected boolean showTelemetryKeys;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ftc_driver_station);

		context = this;

		textPingStatus = (TextView) findViewById(R.id.textPingStatus);
		textWifiDirectStatus = (TextView) findViewById(R.id.textWifiDirectStatus);
		textRCBatteryPercent = (TextView) findViewById(R.id.textRCBatteryPercent);
		textRobotBatteryVoltage = (TextView) findViewById(R.id.textRobotBatteryVoltage);
		textOpModeLabel = (TextView) findViewById(R.id.textOpModeLabel);
		textOpModeName = (TextView) findViewById(R.id.textOpModeName);
		textTelemetry = (TextView) findViewById(R.id.textTelemetry);
		textuser1 = (TextView) findViewById(R.id.user1);
		textuser2 = (TextView) findViewById(R.id.user2);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStartTimed = (Button) findViewById(R.id.buttonStartTimed);
		buttonSelect = (Button) findViewById(R.id.buttonSelect);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonInit = (Button) findViewById(R.id.buttonInit);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		showTelemetryKeys = preferences.getBoolean(getString(R.string.pref_show_telemetry_keys_key), true);


		pixelFont = Typeface.createFromAsset(getAssets(), "fonts/Minecraftia-Regular.ttf");
		startTimedDefaultText = getString(R.string.label_start_timed);

		user1ScaleAnimation = animateAddController(textuser1);
		user2ScaleAnimation = animateAddController(textuser2);

		preferences.registerOnSharedPreferenceChangeListener(this);
		
		textOpModeName.setText("---");
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		assumeClientDisconnect();
		RobotLog.onApplicationStart();

		RobotLog.i("App Started");
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		// close the old event loops
		shutdown();

		RobotLog.i("App Stopped");
		RobotLog.cancelWriteLogcatToDisk();
	}

	//found http://stackoverflow.com/questions/23695626/making-textview-loop-a-growing-and-shrinking-animation
	public ScaleAnimation animateAddController(TextView view)
	{


		if(preferences.getBoolean(getString(R.string.pref_animate_add_joystick_key), true))
		{
			view.setRotation(-15);
			view.setTypeface(pixelFont);
			view.setTextSize(12.0F);

			ScaleAnimation animation = new ScaleAnimation(0.85f, 1.10f, 0.85f, 1.10f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1.0f);
			animation.setDuration(800);
			animation.setRepeatCount(-1);
			animation.setRepeatMode(Animation.REVERSE);
			animation.setInterpolator(new AccelerateInterpolator());
			animation.setAnimationListener(new Animation.AnimationListener()
			{

				@Override
				public void onAnimationStart(Animation animation)
				{
				}

				@Override
				public void onAnimationEnd(Animation animation)
				{
				}

				@Override
				public void onAnimationRepeat(Animation animation)
				{
				}
			});
			view.setAnimation(animation);
			return animation;
		}
		return null;
	}

	public void unanimateAddController(TextView view, ScaleAnimation currentAnimation)
	{

		view.setRotation(0);
		view.setTextColor(Color.BLACK);
		view.setTextSize(10.0F); //default text size

		view.setTypeface(Typeface.DEFAULT);
		if(currentAnimation != null && !(currentAnimation.hasEnded()))
		{
			currentAnimation.cancel();
		}
	}

	public void showToast(final String msg, final int duration)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(context, msg, duration).show();
			}
		});
		RobotLog.i(msg);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO: enable different pref for each user
		if(key.equals(context.getString(R.string.pref_gamepad_type_key)))
		{
			gamepads.clear();
			userToGamepadMap.clear();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ftc_driver_station, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_about:
				startActivity(new Intent(getBaseContext(), DSAboutActivity.class));
				return true;
			case R.id.action_settings:
				startActivity(new Intent(getBaseContext(), SettingsActivity.class));
				return true;
			case R.id.action_restart_robot:
				pendingCommands.add(new Command(CommandList.CMD_RESTART_ROBOT));
				return true;
			case R.id.action_pair_with_robot:
				startActivity(new Intent(getBaseContext(), FtcPairWifiDirectActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event)
	{
		if(!Gamepad.isGamepadDevice(event.getDeviceId()))
		{
			return super.dispatchGenericMotionEvent(event);
		}
		handleGamepadEvent(event);
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if(Gamepad.isGamepadDevice(event.getDeviceId()))
		{
			handleGamepadEvent(event);
			return true;
		}

		return super.dispatchKeyEvent(event);
	}

	public void onClickButtonInit(View view)
	{
		handleOpModeInit();
	}

	public void onClickButtonStart(View view)
	{
		this.opModeUseTimer = false;
		handleOpModeStart();
	}

	public void onClickButtonStartTimed(View view)
	{
		this.opModeUseTimer = true;

		this.opModeCountDown.setCountdown(30);

		handleOpModeStart();
	}

	public void onClickButtonSelect(View view)
	{

		opModeCountDown.stop();
		// create an array of op modes
		String[] opModeStringArray = new String[opModes.size()];

		for(int index = 0; index < opModeStringArray.length; ++index)
		{
			OpModeMeta currMode = opModes.get(index);

			if(currMode.group.equals(OpModeMeta.DefaultGroup))
			{
				opModeStringArray[index] = currMode.name;
			}
			else
			{
				opModeStringArray[index] = currMode.group + " :: " + currMode.name;
			}
		}

		// display dialog to user
		OpModeSelectionDialogFragment opModeSelectionDialogFragment = new OpModeSelectionDialogFragment();
		opModeSelectionDialogFragment.setOnSelectionDialogListener(this);
		opModeSelectionDialogFragment.setOpModes(opModeStringArray);
		opModeSelectionDialogFragment.show(getFragmentManager(), "op_mode_selection");

		this.pendingCommands.add(new Command(CommandList.CMD_INIT_OP_MODE, OpModeManager.DEFAULT_OP_MODE_NAME));
	}

	public void onClickButtonStop(View view)
	{
		handleOpModeStop();
	}

	// Callback method, will be called when the user selects an option from the op mode selection dialog
	@Override
	public void onSelectionClick(int selectionIndex)
	{
		handleOpModeQueued(selectionIndex);

		this.opModeCountDown.setCountdown(30);
		uiWaitingForInitEvent();
	}

	protected void shutdown()
	{
		if(recvLoopService != null)
		{
			recvLoopService.shutdownNow();
		}
		if(sendLoopFuture != null && !sendLoopFuture.isDone())
		{
			sendLoopFuture.cancel(true);
		}
		if(this.peerDiscoveryManager != null)
		{
			this.peerDiscoveryManager.stop();
		}

		// close the socket as well
		if(socket != null)
		{
			socket.close();
		}

		// reset the client
		remoteAddr = null;

		// reset quick status
		pingStatus("");
	}


	protected void heartbeatEvent(RobocolDatagram packet)
	{
		try
		{
			heartbeatRecv.fromByteArray(packet.getData());
			double elapsedTime = heartbeatRecv.getElapsedSeconds();
			pingAverage.addNumber((int)(elapsedTime * 1000));

			robotState = RobotState.fromByte(this.heartbeatRecv.getRobotState());

			// greater than .5 second since last UI update?
			if(lastUiUpdate.time() > 0.5)
			{
				//lastUiUpdate.reset();

				//pingStatus(String.format(Locale.getDefault(), "Ping: %3dms", pingAverage.getAverage()));
			}
		}
		catch(RobotCoreException e)
		{
			RobotLog.logStacktrace(e);
		}
	}

	protected void commandEvent(RobocolDatagram packet)
	{
		try
		{
			Command command = new Command(packet.getData());

			if(command.isAcknowledged())
			{
				pendingCommands.remove(command);
				return;
			}

			// we are to handle this command
			RobotLog.i(" processing command: " + command.getName());
			command.acknowledge();
			pendingCommands.add(command);

			String name = command.getName();
			String extra = command.getExtra();

			RobotLog.i(name);

			if(name.equals(CommandList.CMD_NOTIFY_OP_MODE_LIST))
			{
				handleCommandRequestOpModeListResp(extra);
			}
			else if(name.equals(CommandList.CMD_NOTIFY_INIT_OP_MODE))
			{
				handleCommandInitOpModeResp(extra);
			}
			else if(name.equals(CommandList.CMD_NOTIFY_RUN_OP_MODE))
			{
				handleCommandStartOpModeResp(extra);
			}
			//else if(name.equals(CommandList.CMD_SUGGEST_OP_MODE_LIST_REFRESH))
			//{
			//	handleCommandSuggestOpModeRefresh(extra);
			//}
			else
			{
				RobotLog.i("Unable to process command " + name);
			}

		}
		catch(RobotCoreException e)
		{
			RobotLog.logStacktrace(e);
		}
	}


	protected void peerDiscoveryEvent(RobocolDatagram packet)
	{
		if(packet.getAddress().equals(remoteAddr))
		{
			return; // no action needed
		}

		// update remoteAddr with latest address
		remoteAddr = packet.getAddress();
		RobotLog.i("new remote peer discovered: " + remoteAddr.getHostAddress());

		try
		{
			socket.connect(remoteAddr);
		}
		catch(SocketException e)
		{
			RobotLog.e("Unable to connect to peer:" + e.toString());
		}

		// start send loop, if needed
		if(sendLoopFuture == null || sendLoopFuture.isDone())
		{
			sendLoopFuture = sendLoopService.scheduleAtFixedRate(new SendLoopRunnable(), 0, 40, TimeUnit.MILLISECONDS);
		}

		assumeClientConnect();
	}

	protected void telemetryEvent(RobocolDatagram packet)
	{
		TelemetryMessage message;
		SortedSet<String> keys;

		try
		{
			message = new TelemetryMessage(packet.getData());
		}
		catch(RobotCoreException e)
		{
			RobotLog.logStackTrace(e);
			return;
		}

		//parse out system telemetry messages
		String tag = message.getTag();

		//Log.d("OSDS", "Telemetry Event Received: " + tag);

		if(tag.equals(EventLoopManager.SYSTEM_ERROR_KEY))
		{
			String errorMsg = message.getDataStrings().get(message.getTag());
			RobotLog.e("System Errror Telemetry: " + errorMsg);
			RobotLog.setGlobalErrorMsg(errorMsg);
			setTextView(textTelemetry, "Robot is hosed. To recover, please restart it. Error: " + errorMsg);
			uiRobotNeedsRestart();
		}
		else if(tag.equals(EventLoopManager.SYSTEM_WARNING_KEY))
		{
			String warnMsg = message.getDataStrings().get(message.getTag());
			RobotLog.e("System Warning Telemetry: " + warnMsg);
			setTextView(textWifiDirectStatus, "Robot warning: " + warnMsg);
		}
		else if(tag.equals(EventLoopManager.SYSTEM_NONE_KEY))
		{
			//print nothing
			setTextView(textTelemetry, "");
		}
		else if(tag.equals(EventLoopManager.RC_BATTERY_STATUS_KEY))
		{
			String statusString = message.getDataStrings().get(message.getTag());
			RobotLog.i("RC battery Telemetry event: " + statusString);

			BatteryChecker.BatteryStatus battStatus = BatteryChecker.BatteryStatus.deserialize(statusString);

			setTextView(textRCBatteryPercent, String.format(Locale.getDefault(), "%2.00f%%", battStatus.percent)
							+ (battStatus.isCharging ? getString(R.string.label_RC_battery_charging) : ""));
		}
		else if(tag.equals(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY))
		{
			String voltage = message.getDataStrings().get(message.getTag());
			RobotLog.i("Robot Battery Telemetry event: " + voltage);

			if(voltage.equals(FtcEventLoopHandler.NO_VOLTAGE_SENSOR))
			{
				setTextView(textRobotBatteryVoltage, getString(R.string.label_unknown_robot_battery));
			} else
			{
				setTextView(textRobotBatteryVoltage, voltage);
			}
		}
		else
		{
			StringBuilder telemetryStringBuilder = new StringBuilder();
			Map<String, String> strings = message.getDataStrings();

			keys = new TreeSet<String>(strings.keySet());
			for(String key : keys)
			{
				//for some reason untagged telemetry has this too
				if(key.equals(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY))
				{
					continue;
				}

				if(showTelemetryKeys)
				{
					telemetryStringBuilder.append(key);
					telemetryStringBuilder.append(": ");
				}

				telemetryStringBuilder.append(strings.get(key));
				telemetryStringBuilder.append('\n');
			}
			String telemetryString = telemetryStringBuilder.toString();

			Map<String, Float> numbers = message.getDataNumbers();
			keys = new TreeSet<String>(numbers.keySet());
			for(String key : keys)
			{
				telemetryString += key + ": " + numbers.get(key) + "\n";
			}
			setTextView(textTelemetry, telemetryString);
		}

	}

	protected void uiRobotNeedsRestart()
	{
		//currently does the same thing
		uiRobotControllerIsDisconnected();

		Log.d("OSDS UI Change", "uiRobotNeedsRestart()");
	}

	protected void uiRobotControllerIsDisconnected()
	{
		setEnabled(this.buttonSelect, false);
		setEnabled(this.buttonInit, false);
		setEnabled(this.buttonStart, false);
		setEnabled(this.buttonStop, false);
		setEnabled(this.buttonStartTimed, false);
		Log.d("OSDS UI Change", "uiRobotControllerIsDisconnected()");

	}


	protected void uiWaitingForOpModeSelection()
	{
		setEnabled(this.buttonSelect, true);
		setEnabled(this.buttonInit, false);
		setEnabled(this.buttonStart, false);
		setEnabled(this.buttonStop, false);
		setEnabled(this.buttonStartTimed, false);
		setTextView(textOpModeName, "");
		Log.d("OSDS UI Change", "uiWaitingForOpModeSelection()");

	}

	protected void uiWaitingForInitEvent()
	{
		if(queuedOpMode.name.equals(OpModeManager.DEFAULT_OP_MODE_NAME))
		{
			setEnabled(this.buttonInit, false);
		} else
		{
			setTextView(textOpModeName, queuedOpMode.name);
			setEnabled(this.buttonInit, true);
		}
		setEnabled(this.buttonSelect, true);
		setEnabled(this.buttonStart, false);
		setEnabled(this.buttonStartTimed, false);
		setEnabled(this.buttonStop, false);
		Log.d("OSDS UI Change", "uiWaitingForInitEvent()");

	}

	protected void uiWaitingForStartEvent()
	{
		setTextView(textOpModeName, queuedOpMode.name);
		setEnabled(this.buttonSelect, true);
		setEnabled(this.buttonInit, false);
		setEnabled(this.buttonStop, false);
		setEnabled(this.buttonStartTimed, true);
		setEnabled(this.buttonStart, true);
		Log.d("OSDS UI Change", "uiWaitingForStartEvent()");

	}

	protected void uiWaitingForStopEvent()
	{
		setTextView(textOpModeName, "");
		setEnabled(this.buttonSelect, true);
		setEnabled(this.buttonStop, true);
		setEnabled(this.buttonStart, false);
		setEnabled(this.buttonInit, false);
		setEnabled(this.buttonStartTimed, false);
		setEnabled(this.buttonStart, false);
		Log.d("OSDS UI Change", "uiWaitingForStopEvent()");

	}


	protected void assumeClientConnect()
	{
		RobotLog.i("Assuming client connected");
		clientConnected = true;
		uiWaitingForOpModeSelection();
		// request a list of available op modes
		pendingCommands.add(new Command(CommandList.CMD_REQUEST_UI_STATE));
	}

	protected void assumeClientDisconnect()
	{
		RobotLog.i("Assuming client disconnected");
		clientConnected = false;

		opModeUseTimer = false;
		opModeCountDown.stop();
		queuedOpMode = new OpModeMeta(OpModeManager.DEFAULT_OP_MODE_NAME);
		opModes.clear();

		pingStatus("");
		pendingCommands.clear();
		remoteAddr = null;

		setTextView(textRobotBatteryVoltage, getString(R.string.label_unknown_robot_battery));
		setTextView(textOpModeName, "");
		setTextView(buttonStop, getString(R.string.label_stop));
		setTextView(textTelemetry, "");

		setButtonText(buttonStartTimed, startTimedDefaultText);

		setEnabled(buttonSelect, false);
		setEnabled(buttonStop, false);
		setEnabled(buttonStart, false);
		setEnabled(buttonStartTimed, false);

		RobotLog.clearGlobalErrorMsg();
		uiRobotControllerIsDisconnected();
	}

	protected void handleOpModeQueued(int queuedOpModeIndex)
	{
		this.queuedOpMode = opModes.get(queuedOpModeIndex);
	}

	protected void handleOpModeStop()
	{
		this.opModeCountDown.stop();
		if(!buttonStartTimed.getText().toString().equals(startTimedDefaultText))
		{
			opModeCountDown.setCountdown(Long.parseLong(buttonStartTimed.getText().toString()));
		}
		uiWaitingForInitEvent();
		this.pendingCommands.add(new Command(CommandList.CMD_INIT_OP_MODE, OpModeManager.DEFAULT_OP_MODE_NAME));
	}

	protected void clearInfo()
	{
		setTextView(this.textTelemetry, "");
	}

	protected void handleOpModeInit()
	{
		this.opModeCountDown.stop();
		uiWaitingForStartEvent();
		this.pendingCommands.add(new Command(CommandList.CMD_INIT_OP_MODE, this.queuedOpMode.name));
		clearInfo();
	}

	protected void handleOpModeStart()
	{
		opModeCountDown.stop();
		uiWaitingForStopEvent();
		pendingCommands.add(new Command(CommandList.CMD_RUN_OP_MODE, queuedOpMode.name));
		clearInfo();
	}

	protected void handleCommandRequestOpModeListResp(String extra)
	{
		opModes = new Gson().fromJson(extra, listOfOpmodeMetaType);

		RobotLog.i("Received the following op modes: " + opModes.toString());
		pendingCommands.add(new Command(CommandList.CMD_INIT_OP_MODE, OpModeManager.DEFAULT_OP_MODE_NAME));
		uiWaitingForOpModeSelection();
	}

	protected void handleCommandInitOpModeResp(String extra)
	{
		RobotLog.i("Robot Controller initializing op mode: " + extra);
		if(!extra.equals(OpModeManager.DEFAULT_OP_MODE_NAME))
		{
			uiWaitingForStartEvent();
		}
		else if(this.queuedOpMode.equals(OpModeManager.DEFAULT_OP_MODE_NAME))
		{
			uiWaitingForOpModeSelection();
		}
		else
		{
			uiWaitingForInitEvent();
			this.pendingCommands.add(new Command(CommandList.CMD_RUN_OP_MODE, OpModeManager.DEFAULT_OP_MODE_NAME));
		}
	}

	protected void handleCommandStartOpModeResp(String extra)
	{
		RobotLog.i("Robot Controller starting op mode: " + extra);
		if(!extra.equals(OpModeManager.DEFAULT_OP_MODE_NAME))
		{
			uiWaitingForStopEvent();
		}
		if(this.opModeUseTimer && !extra.equals(OpModeManager.DEFAULT_OP_MODE_NAME))
		{
			this.opModeCountDown.start();
		}
	}

	//this seems to be sent after the robot is restarted
	protected void handleCommandSuggestOpModeRefresh(String extra)
	{
		RobotLog.i("Refreshing opmode list as requested...");
		uiWaitingForOpModeSelection();
		pendingCommands.add(new Command(CommandList.CMD_NOTIFY_OP_MODE_LIST));
	}

	protected void wifiDirectStatus(final String status)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				textWifiDirectStatus.setText(status);
			}
		});
	}

	protected void setButtonText(final Button button, final String text)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				button.setText(text);
			}
		});
	}

	protected void setTextView(final TextView textView, final String text)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				textView.setText(text);
			}
		});
	}

	protected void setEnabled(final View view, final boolean enabled)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				view.setEnabled(enabled);
			}
		});
	}

	protected void setVisibility(final View view, final int visibility)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				view.setVisibility(visibility);
			}
		});
	}

	protected void pingStatus(final String status)
	{
		setTextView(textPingStatus, status);
	}

	// needs to be synchronized since multiple gamepad events can come in at the same time
	protected synchronized void handleGamepadEvent(MotionEvent event)
	{
		Gamepad gamepad = gamepads.get(event.getDeviceId());
		if(gamepad == null)
		{
			return; // we aren't tracking this gamepad
		}

		gamepad.update(event);
		indicateGamepad(gamepad, event);
	}

	protected void indicateGamepad(Gamepad gamepad, InputEvent event)
	{

		String info = gamepad.toString();
		info = info.substring(16);
		info = info.substring(0, 20) + "\n" + info.substring(20, 40) + "\n" + info.substring(40, 57) + "\n" + info.substring(58);
		info = String.format(Locale.US, "Gamepad detected as %s (ID %d)", gamepads.get(event.getDeviceId()).type(), event.getDeviceId()) + "\n" + info;

		for(Map.Entry<GamepadUser, Integer> entry : userToGamepadMap.entrySet())
		{
			if(entry.getValue() == event.getDeviceId())
			{
				if(entry.getKey() == GamepadUser.ONE)
				{
					animateInfo(textuser1, info, Color.argb(255, 0, 255, 144));
				}
				else if(entry.getKey() == GamepadUser.TWO)
				{
					animateInfo(textuser2, info, Color.argb(255, 0, 111, 255));
				}
			}
		}
	}

	protected void animateInfo(TextView user, String info, int color)
	{
		user.setText(info);
		ObjectAnimator colorFade = ObjectAnimator.ofObject(user, "backgroundColor", new ArgbEvaluator(), color, Color.argb(255, 255, 255, 255));
		colorFade.setDuration(2000);
		colorFade.start();
	}

	// needs to be synchronized since multiple gamepad events can come in at the same time
	protected synchronized void handleGamepadEvent(KeyEvent event)
	{
		if(!gamepads.containsKey(event.getDeviceId()))
		{
			gamepads.put(event.getDeviceId(), new Gamepad());
		}

		Gamepad gamepad = gamepads.get(event.getDeviceId());

		gamepad.update(event);
		indicateGamepad(gamepad, event);

		if(gamepad.start && (gamepad.a || gamepad.b))
		{
			GamepadUser user;
			if(gamepad.a)
			{
				user = GamepadUser.ONE;
			} else
			{
				user = GamepadUser.TWO;
			}
			assignNewGamepad(user, event.getDeviceId());
		}
	}

	protected void initGamepad(GamepadUser user, int gamepadId)
	{
		String key = "";

		switch(user)
		{
			// TODO: different pref for user 1 and 2
			case ONE:
				key = getString(R.string.pref_gamepad_type_key);
				unanimateAddController(textuser1, user1ScaleAnimation);
				break;
			case TWO:
				key = getString(R.string.pref_gamepad_type_key);
				unanimateAddController(textuser2, user2ScaleAnimation);
				break;
		}

		String gamepadType = preferences.getString(key, getString(R.string.gamepad_default));

		Gamepad gamepad;

		if(gamepadType.equals(getString(R.string.gamepad_logitech_f310)))
		{
			gamepad = new LogitechGamepadF310();
		} else if(gamepadType.equals(getString(R.string.gamepad_microsoft_xbox_360)))
		{
			gamepad = new MicrosoftGamepadXbox360();
		} else
		{
			gamepad = new Gamepad();
		}

		gamepad.id = gamepadId;
		gamepad.timestamp = SystemClock.uptimeMillis();

		gamepads.put(gamepadId, gamepad);
	}

	protected void assignNewGamepad(GamepadUser user, int gamepadId)
	{

		// search for duplicates and remove
		Set<GamepadUser> duplicates = new HashSet<>();
		for(Map.Entry<GamepadUser, Integer> entry : userToGamepadMap.entrySet())
		{
			if(entry.getValue() == gamepadId)
			{
				duplicates.add(entry.getKey());
			}
		}
		for(GamepadUser keyToRemove : duplicates)
		{
			userToGamepadMap.remove(keyToRemove);
		}

		// add user to mapping and init gamepad
		userToGamepadMap.put(user, gamepadId);
		initGamepad(user, gamepadId);

		String msg = String.format(Locale.US, "Gamepad %d detected as %s (ID %d)", user, gamepads.get(gamepadId).type(), gamepadId);
		RobotLog.i(msg);
	}


	protected class SendLoopRunnable implements Runnable
	{
		private static final long GAMEPAD_UPDATE_THRESHOLD = 1000; // in milliseconds

		@Override
		public void run()
		{
			try
			{
				long now = SystemClock.uptimeMillis();

				// skip if we haven't received a packet in a while
				if(lastRecvPacket.time() > ASSUME_DISCONNECT_TIMER)
				{
					if(clientConnected)
					{
						assumeClientDisconnect();
					}
					return;
				}

				// send heartbeat
				if(heartbeatSend.getElapsedSeconds() > 0.1)
				{
					// generate a new heartbeat packet and send it
					heartbeatSend = new Heartbeat();
					RobocolDatagram packetHeartbeat = new RobocolDatagram(heartbeatSend);
					socket.send(packetHeartbeat);
				}

				// send gamepads
				for(Map.Entry<GamepadUser, Integer> userEntry : userToGamepadMap.entrySet())
				{

					GamepadUser user = userEntry.getKey();
					int id = userEntry.getValue();

					Gamepad gamepad = gamepads.get(id);
					gamepad.setUser(user);

					// don't send stale gamepads
					if(now - gamepad.timestamp > GAMEPAD_UPDATE_THRESHOLD && gamepad.atRest())
					{
						continue;
					}

					RobocolDatagram packetGamepad = new RobocolDatagram(gamepad);
					socket.send(packetGamepad);
				}

				// send commands
				Iterator<Command> i = pendingCommands.iterator();
				while(i.hasNext())
				{
					// using an iterator so we can change the set while looping through all elements
					Command command = i.next();

					// if this command has exceeded max attempts, give up
					if(command.getAttempts() > MAX_COMMAND_ATTEMPTS)
					{
						String msg = String.format(Locale.US, "Giving up on command %s after %d attempts",
								command.getName(), MAX_COMMAND_ATTEMPTS);
						showToast(msg, Toast.LENGTH_SHORT);
						i.remove();
						continue;
					}

					// log commands we initiated
					if(!command.isAcknowledged())
					{
						RobotLog.i("	sending command: " + command.getName() + ", attempt: " + command.getAttempts());
					}

					// send the command
					RobocolDatagram packetCommand = new RobocolDatagram(command);
					socket.send(packetCommand);

					// if this is a command we handled, remove it
					if(command.isAcknowledged())
					{
						pendingCommands.remove(command);
					}
				}
			}
			catch(RobotCoreException e)
			{
				e.printStackTrace();
			}
		}
	}

	protected class RecvLoopRunnable implements Runnable
	{
		@Override
		public void run()
		{
			while(true)
			{
				RobocolDatagram packet = socket.recv();

				if(packet == null)
				{
					if(socket.isClosed())
					{
						return;
					}
					Thread.yield();
					continue;
				}
				lastRecvPacket.reset();

				switch(packet.getMsgType())
				{
					case PEER_DISCOVERY:
						peerDiscoveryEvent(packet);
						break;
					case HEARTBEAT:
						heartbeatEvent(packet);
						break;
					case COMMAND:
						commandEvent(packet);
						break;
					case TELEMETRY:
						telemetryEvent(packet);
						break;
					default:
						RobotLog.i("Unhandled message type: " + packet.getMsgType().name());
						break;
				}
			}
		}
	}

	private class OpModeCountDownTimer
	{
		public static final long COUNTDOWN_INTERVAL = 30;
		public static final long TICK = 1000;
		private long countdown;
		boolean running;
		CountDownTimer timer;

		/* renamed from: com.qualcomm.ftcdriverstation.FtcDriverStationActivity.OpModeCountDownTimer.1 */
		class TimerInstantiator implements Runnable
		{

			/* renamed from: com.qualcomm.ftcdriverstation.FtcDriverStationActivity.OpModeCountDownTimer.1.1 */
			class OpModeTimer extends CountDownTimer
			{
				OpModeTimer(long timeInFuture, long countDownInterval)
				{
					super(timeInFuture, countDownInterval);
				}

				public void onTick(long timeRemaining)
				{
					long timeRemainingInSeconds = timeRemaining / OpModeCountDownTimer.TICK;
					setButtonText(buttonStartTimed, String.valueOf(timeRemainingInSeconds));
					RobotLog.i("Running current op mode for " + timeRemainingInSeconds + " seconds");
				}

				public void onFinish()
				{
					OpModeCountDownTimer.this.running = false;
					RobotLog.i("Stopping current op mode, timer expired");
					setCountdown(OpModeCountDownTimer.COUNTDOWN_INTERVAL);
					setButtonText(buttonStartTimed, startTimedDefaultText);
					//setImageResource(FtcDriverStationActivity.this.buttonStartTimed, R.drawable.icon_timeroff);

					setEnabled(buttonStop, false);
					FtcDriverStationActivity.this.handleOpModeStop();
				}
			}

			TimerInstantiator()
			{
			}

			public void run()
			{
				OpModeCountDownTimer.this.timer = new OpModeTimer(OpModeCountDownTimer.this.countdown, OpModeCountDownTimer.TICK).start();
			}
		}

		private OpModeCountDownTimer()
		{
			this.countdown = 30000;
			this.timer = null;
			this.running = false;
		}

		public void start()
		{
			RobotLog.i("Running current op mode for " + getTimeRemainingInSeconds() + " seconds");
			this.running = true;
			FtcDriverStationActivity.this.runOnUiThread(new TimerInstantiator());
		}

		public void stop()
		{
			if(this.running)
			{
				this.running = false;
				RobotLog.i("Stopping current op mode timer");
				if(this.timer != null)
				{
					this.timer.cancel();
				}
			}
		}

		public boolean isRunning()
		{
			return this.running;
		}

		public long getTimeRemainingInSeconds()
		{
			return this.countdown / TICK;
		}

		public void setCountdown(long remaining)
		{
			this.countdown = TICK * remaining;
		}
	}
}
