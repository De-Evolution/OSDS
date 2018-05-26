package com.qualcomm.osds;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Dialog box which allows users to enter an IP address, and verifies that it is reachable.
 */
public class IPAddressDialog extends Dialog implements TextWatcher, View.OnClickListener
{
	public static final String IP_ADDRESS_PREFERENCE = "ip_address";

	private EditText ipTextbox;

	private TextView validText;
	private TextView pingableText;

	private Button okButton;
	private Button cancelButton;

	private InetAddress currentIP;

	private Thread pingerThread;

	//need this so that we can use Activity.runOnUiThread()
	private Activity activity;
	private SharedPreferences preferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_ip_address);

		ipTextbox = (EditText) findViewById(R.id.editTextIPAddress);
		validText = (TextView) findViewById(R.id.textIsValid);
		pingableText = (TextView) findViewById(R.id.textIsPingable);
		okButton = (Button) findViewById(R.id.buttonOk);
		okButton.setOnClickListener(this);
		cancelButton = (Button) findViewById(R.id.buttonCancel);
		cancelButton.setOnClickListener(this);
		ipTextbox.addTextChangedListener(this);

		setTitle(R.string.ip_dialog_title);

	}

	@Override
	protected void onStart()
	{
		super.onStart();

		//this is in onStart() so that the old IP will be releaded if the dialog was previously canceled
		ipTextbox.setText(preferences.getString(IP_ADDRESS_PREFERENCE, ""));
	}

	public IPAddressDialog(Activity activity, SharedPreferences preferences)
	{
		super(activity, android.R.style.Theme_Holo_Light_Dialog);

		this.activity = activity;

		this.preferences = preferences;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		//do nothing
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		//do nothing
	}


	@Override
	public void afterTextChanged(Editable s)
	{
		//check that the IP is valid
		String ipString = s.toString();
		if(Patterns.IP_ADDRESS.matcher(s.toString()).matches())
		{
			validText.setText(R.string.is_valid);
			okButton.setEnabled(true);
		}
		else
		{
			validText.setText(R.string.not_valid);
			pingableText.setText(R.string.not_pingable);
			okButton.setEnabled(false);
			return;
		}

		//stop the pinger thread
		if(pingerThread != null && pingerThread.isAlive())
		{
			long startTime = System.currentTimeMillis();
			pingerThread.interrupt();
			try
			{
				//for some reason, this takes a long time
				//it's not supposed to.	More investigation is needed.
				pingerThread.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		try
		{
			currentIP = InetAddress.getByName(ipString);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			validText.setText(R.string.not_valid);
			return;
		}

		//start the ping test
		pingerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				activity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						pingableText.setText(R.string.checking_pingable);
					}
				});

				boolean reachable = false;
				try
				{
					reachable = currentIP.isReachable(750);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				//since it is accessed from an inner class, reachable needs to be final.
				//but, it also needs to be set from within a try-catch block, so it can't be.
				//so, we just make another boolean which IS final and assign reachable to it.
				//Sigh...	java, java
				final boolean pingable = reachable;

				//user changed the text
				if(Thread.currentThread().isInterrupted())
				{
					return;
				}
				activity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						pingableText.setText(pingable ? R.string.is_pingable : R.string.not_pingable);
					}
				});

			}
		});

		pingerThread.start();
	}


	@Override
	public void onClick(View v)
	{
		if(v.getId() == okButton.getId())
		{
			dismiss();
			preferences.edit().putString(IP_ADDRESS_PREFERENCE, ipTextbox.getText().toString()).apply();
		}
		else if(v.getId() == cancelButton.getId())
		{
			dismiss();
		}

	}
}
