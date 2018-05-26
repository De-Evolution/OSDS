package com.qualcomm.robotcore.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

import com.qualcomm.robotcore.util.RobotLog;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LanAssistant extends NetworkConnection
{
    /**
     * BroadcastReceiver which updates the UI when the wifi IP address has changed
     */
    public class WifiIPUpdaterReceiver extends BroadcastReceiver
    {

        public WifiIPUpdaterReceiver(Context context)
        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            context.registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            // resend the appropriate connection event
            if(connectionTypeKnown)
            {
                if(isDriverStation)
                {
                    callback.onNetworkConnectionEvent(Event.CONNECTED_AS_LAN_SERVER);
                }
                else
                {
                    callback.onNetworkConnectionEvent(Event.CONNECTED_AS_LAN_PEER);
                }
            }
        }
    }

    private static LanAssistant instance = null;
    private NetworkConnectionCallback callback;
    private WifiManager wifiService;

    private String controllerIPAddressString;
    private InetAddress controllerIPAddress; //only set if this is a driver station and

    //whether discoverPotentialConnections() or createConnection() has been called yet
    private boolean connectionTypeKnown = false;
    private boolean isDriverStation = false; //if true, we connect to the robot controller using the saved IP.  If false, we bind a socket to the wildcard address.

    public static synchronized LanAssistant getLanAssistant(Context context)
    {
        synchronized (LanAssistant.class)
        {
            if (instance == null)
            {
                instance = new LanAssistant(context);
            }
        }
        return instance;
    }

    private LanAssistant(Context context) {
        this.callback = null;
        wifiService = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        controllerIPAddressString = PreferenceManager.getDefaultSharedPreferences(context).getString("ip_address", "");
    }

    @Override
    public NetworkType getNetworkType()
    {
        return NetworkType.LAN;
    }

    @Override
    public void enable()
    {

    }

    @Override
    public void disable()
    {

    }

    @Override
    public void setCallback(@NonNull NetworkConnectionCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public void discoverPotentialConnections()
    {
        setIsDriverStation(true);

        try
        {
            controllerIPAddress = InetAddress.getByName(controllerIPAddressString);
        }
        catch(UnknownHostException e)
        {
            RobotLog.e("Failed to parse robot controller IP address preference");
            e.printStackTrace();
        }

        if(callback != null)
        {
            callback.onNetworkConnectionEvent(Event.CONNECTED_AS_LAN_PEER);
        }
    }

    @Override
    public void cancelPotentialConnections()
    {

    }

    @Override
    public void createConnection()
    {
        // we are being told to create connections, like a robot controller does
        setIsDriverStation(false);

        callback.onNetworkConnectionEvent(Event.GROUP_CREATED); // signal EventLoop that it's supposed to make its socket wait for connections
        callback.onNetworkConnectionEvent(Event.CONNECTED_AS_LAN_SERVER);

    }

    /**
     * Called once we find out what side of the connection we're supposed to be.
     * @param isDS whether or not this phone is a driver station
     */
    private void setIsDriverStation(boolean isDS)
    {
        connectionTypeKnown = true;
        isDriverStation = isDS;
    }

    @Override
    public void connect(String var1)
    {
        //never called
    }

    @Override
    public void connect(String var1, String var2)
    {
        //never called
    }

    @Override
    public InetAddress getConnectionOwnerAddress()
    {
        if(isDriverStation)
        {
            if(controllerIPAddress == null)
            {
                RobotLog.e("LanAssistant: do not have RC IP address to give to the network library!");
                return null;
            }
            else
            {
                return controllerIPAddress;
            }
        }
        else
        {
            // wildcard address
            try
            {
                // this is the secret sauce that makes this whole thing work.
                // It tells RobocolSocket to wait until any other device tries to connect to us
                return Inet4Address.getByName("0.0.0.0");
            }
            catch(UnknownHostException e)
            {
                //should never happen
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String getConnectionOwnerName() {
        if (isDriverStation) {
            return controllerIPAddress.getHostAddress();
        } else {
            // return the string to be shown as the IP address in the UP
            if (wifiService.getConnectionInfo() == null) {
                return "<no wifi connection>";
            } else {
                return Formatter.formatIpAddress(wifiService.getConnectionInfo().getIpAddress());
            }
        }
    }

    @Override
    public String getConnectionOwnerMacAddress()
    {
        //do not verify mac address
        return null;
    }

    @Override
    public boolean isConnected()
    {
        return getConnectStatus() == ConnectStatus.CONNECTED;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getDeviceName()
    {
        if(wifiService.getConnectionInfo() == null)
        {
            return "<no wifi connection>";
        }
        else
        {
            return "Wifi IP: " + Formatter.formatIpAddress(wifiService.getConnectionInfo().getIpAddress());
        }
    }

    @Override
    public String getInfo()
    {
        return "";
    }

    @Override
    public String getFailureReason()
    {
        if(isDriverStation && controllerIPAddress == null)
        {
            return "Failed to parse IP address preference data \"" + controllerIPAddressString + '\"';
        }
        else
        {
            return "";
        }
    }

    @Override
    public String getPassphrase()
    {
        return "";
    }

    @Override
    public ConnectStatus getConnectStatus()
    {
        if(connectionTypeKnown)
        {
            if(isDriverStation)
            {
                if(controllerIPAddress == null)
                {
                    return ConnectStatus.ERROR;
                }
                else
                {
                    return ConnectStatus.CONNECTED;
                }
            }
            else
            {
                return ConnectStatus.CONNECTED;
            }
        }
        else
        {
            return ConnectStatus.NOT_CONNECTED;
        }
    }
}