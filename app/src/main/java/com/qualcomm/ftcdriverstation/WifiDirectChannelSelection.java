/*
 * Copyright (c) 2014 Qualcomm Technologies Inc
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

package com.qualcomm.ftcdriverstation;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.RunShellCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WifiDirectChannelSelection {

  private final static String wpaFile = "wpa_supplicant.conf";
  private final static String p2pFile = "p2p_supplicant.conf";

  private final String saveDir;
  private final String getCurrentWifiDirectStatusFile;
  private final String configWifiDirectFile;

  private final static int CONF_FILE_MAX_SIZE = 8 * 1024;


  private final WifiManager wifiManager;

  private final RunShellCommand shell = new RunShellCommand();

  public WifiDirectChannelSelection(Context context, WifiManager wifiManager) {

    this.saveDir = context.getFilesDir().getAbsolutePath() + "/";
    this.wifiManager = wifiManager;

    getCurrentWifiDirectStatusFile = saveDir + "get_current_wifi_direct_staus";
    configWifiDirectFile = saveDir + "config_wifi_direct";

    shell.enableLogging(true);
  }

  public void config(int wifiClass, int wifiChannel) throws IOException {
    try {
      wifiManager.setWifiEnabled(false);

      writeScripts();

      shell.runAsRoot(getCurrentWifiDirectStatusFile);

      configureP2p(wifiClass, wifiChannel);
      forgetNetworks();

      shell.runAsRoot(configWifiDirectFile);

      wifiManager.setWifiEnabled(true);
    } finally {
      removeScripts();
    }
  }

  private int getWpaSupplicantPid() throws RuntimeException {
    // This method has a heavy dependency on the Android version of 'ps'.
    String psOutput = shell.run("/system/bin/ps");
    for (String line : psOutput.split("\n")) {
      if (line.contains("wpa_supplicant")) {
        String[] tokens = line.split("\\s+");
        return Integer.parseInt(tokens[1]); // if 'ps' changes format this call will fail
      }
    }

    throw new RuntimeException("could not find wpa_supplicant PID");
  }

  private void forgetNetworks() {
    try {
      char[] buffer = new char[4 * 1024];

      FileReader fr = new FileReader(saveDir + wpaFile);
      int length = fr.read(buffer);
      fr.close();

      String s = new String(buffer, 0, length);
      RobotLog.v("WPA FILE: \n" + s);

      // remove all saved AP's
      s = s.replaceAll("(?s)network\\s*=\\{.*\\}", "");

      // remove blank lines
      s = s.replaceAll("(?m)^\\s+$", "");

      RobotLog.v("WPA REPLACE: \n" + s);

      FileWriter fw = new FileWriter(saveDir + wpaFile);
      fw.write(s);
      fw.close();
    } catch (FileNotFoundException e) {
      RobotLog.e("File not found: " + e.toString());
      e.printStackTrace();
    } catch (IOException e) {
      RobotLog.e("FIO exception: " + e.toString());
      e.printStackTrace();
    }
  }

  private void configureP2p(int wifiClass, int wifiChannel) {
    try {
      char[] buffer = new char[CONF_FILE_MAX_SIZE];

      FileReader fr = new FileReader(saveDir + p2pFile);
      int length = fr.read(buffer);
      fr.close();

      String s = new String(buffer, 0, length);
      RobotLog.v("P2P FILE: \n" + s);

      // remove any old p2p settings
      s = s.replaceAll("p2p_listen_reg_class\\w*=.*", "");
      s = s.replaceAll("p2p_listen_channel\\w*=.*", "");
      s = s.replaceAll("p2p_oper_reg_class\\w*=.*", "");
      s = s.replaceAll("p2p_oper_channel\\w*=.*", "");
      s = s.replaceAll("p2p_pref_chan\\w*=.*", "");

      // remove all saved networks
      s = s.replaceAll("(?s)network\\s*=\\{.*\\}", "");

      // remove blank lines
      s = s.replaceAll("(?m)^\\s+$", "");

      // add our config items
      s += "p2p_oper_reg_class=" + wifiClass + "\n";
      s += "p2p_oper_channel=" + wifiChannel + "\n";
      s += "p2p_pref_chan=" + wifiClass + ":" + wifiChannel + "\n";

      RobotLog.v("P2P REPLACE: \n" + s);

      FileWriter fw = new FileWriter(saveDir + p2pFile);
      fw.write(s);
      fw.close();
    } catch (FileNotFoundException e) {
      RobotLog.e("File not found: " + e.toString());
      e.printStackTrace();
    } catch (IOException e) {
      RobotLog.e("FIO exception: " + e.toString());
      e.printStackTrace();
    }
  }

  /*
   * Write scripts out to app data location
   *
   * Most of these commands need to be ran as root. Many
   * systems are designed to display all commands ran as root as a toast to the user. We will hide
   * the commands in these scripts and run this script as root. removeScripts() should be called as
   * soon as these scripts are no longer needed
   */
  private void writeScripts() throws IOException {
    final String scriptGetCurrentWifiDirectStatus = String.format(
        "cp /data/misc/wifi/wpa_supplicant.conf %s/wpa_supplicant.conf \n" +
        "cp /data/misc/wifi/p2p_supplicant.conf %s/p2p_supplicant.conf \n" +
        "chmod 666 %s/*supplicant* \n",
        saveDir, saveDir, saveDir);

    final String scriptConfigWifiDirect = String.format(
        "cp %s/p2p_supplicant.conf /data/misc/wifi/p2p_supplicant.conf \n" +
        "cp %s/wpa_supplicant.conf /data/misc/wifi/wpa_supplicant.conf \n" +
        "rm %s/*supplicant* \n" +
        "chown system.wifi /data/misc/wifi/wpa_supplicant.conf \n" +
        "chown system.wifi /data/misc/wifi/p2p_supplicant.conf \n" +
        "kill -HUP %d \n",
        saveDir, saveDir, saveDir, getWpaSupplicantPid());

    FileWriter fw;

    fw = new FileWriter(getCurrentWifiDirectStatusFile);
    fw.write(scriptGetCurrentWifiDirectStatus);
    fw.close();

    fw = new FileWriter(configWifiDirectFile);
    fw.write(scriptConfigWifiDirect);
    fw.close();

    shell.run("chmod 700 " + getCurrentWifiDirectStatusFile);
    shell.run("chmod 700 " + configWifiDirectFile);
  }

  /*
   * Remove scripts from app data location
   *
   * This method should be called as soon as the scripts are no longer needed
   */
  private void removeScripts() {
    File file;

    file = new File(getCurrentWifiDirectStatusFile);
    file.delete();

    file = new File(configWifiDirectFile);
    file.delete();
  }
}
