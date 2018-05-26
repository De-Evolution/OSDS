/*
Copyright (c) 2016-2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotcore.internal.network;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.ui.ProgressParameters;

import java.io.File;

/**
 * {@link RobotCoreCommandList} contains network commands that are accessible in the RobotCore module
 */
public class RobotCoreCommandList {
    //----------------------------------------------------------------------------------------------
    // User interface remoting
    //----------------------------------------------------------------------------------------------

    public static final String CMD_SHOW_TOAST = "CMD_SHOW_TOAST";

    static public class ShowToast {
        public int duration;
        public String message;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static ShowToast deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, ShowToast.class);
        }
    }

    public static final String CMD_SHOW_PROGRESS = "CMD_SHOW_PROGRESS";

    static public class ShowProgress extends ProgressParameters {
        public String message;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static ShowProgress deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, ShowProgress.class);
        }
    }

    public static final String CMD_DISMISS_PROGRESS = "CMD_DISMISS_PROGRESS";

    public static final String CMD_SHOW_DIALOG = "CMD_SHOW_DIALOG";

    static public class ShowDialog {
        public String uuidString;
        public String title;
        public String message;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static ShowDialog deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, ShowDialog.class);
        }
    }

    public static final String CMD_DISMISS_ALL_DIALOGS = "CMD_DISMISS_ALL_DIALOGS";
    public static final String CMD_DISMISS_DIALOG = "CMD_DISMISS_DIALOG";

    static public class DismissDialog {
        public String uuidString;

        public DismissDialog(String uuidString) {
            this.uuidString = uuidString;
        }

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static DismissDialog deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, DismissDialog.class);
        }
    }

    public static final String CMD_REQUEST_INSPECTION_REPORT = "CMD_REQUEST_INSPECTION_REPORT";
    public static final String CMD_REQUEST_INSPECTION_REPORT_RESP = "CMD_REQUEST_INSPECTION_REPORT_RESP";

    //----------------------------------------------------------------------------------------------
    // Robot semantics and management
    //----------------------------------------------------------------------------------------------

    public static final String CMD_NOTIFY_INIT_OP_MODE = "CMD_NOTIFY_INIT_OP_MODE";
    public static final String CMD_NOTIFY_RUN_OP_MODE = "CMD_NOTIFY_RUN_OP_MODE";

    public static final String CMD_REQUEST_UI_STATE = "CMD_REQUEST_UI_STATE";
    public static final String CMD_NOTIFY_ACTIVE_CONFIGURATION = "CMD_NOTIFY_ACTIVE_CONFIGURATION";
    public static final String CMD_NOTIFY_OP_MODE_LIST = "CMD_NOTIFY_OP_MODE_LIST";
    public static final String CMD_NOTIFY_USER_DEVICE_LIST = "CMD_NOTIFY_USER_DEVICE_LIST";
    public static final String CMD_NOTIFY_ROBOT_STATE = "CMD_NOTIFY_ROBOT_STATE";

    // Used for sending a (pref,value) pair either from a RC to a DS or the other way around.
    // The pair is always a setting of the robot controller. When sent to the RC, it is a request
    // to update the setting; when sent from the RC, it is an announcement of the current value
    // of the setting.
    public static final String CMD_ROBOT_CONTROLLER_PREFERENCE = "CMD_ROBOT_CONTROLLER_PREFERENCE";

    //----------------------------------------------------------------------------------------------
    // Wifi management
    //----------------------------------------------------------------------------------------------

    public static final String CMD_CLEAR_REMEMBERED_GROUPS = "CMD_CLEAR_REMEMBERED_GROUPS";
    public static final String CMD_NOTIFY_WIFI_DIRECT_REMEMBERED_GROUPS_CHANGED = "CMD_NOTIFY_WIFI_DIRECT_REMEMBERED_GROUPS_CHANGED";

    public static final String CMD_DISCONNECT_FROM_WIFI_DIRECT = "CMD_DISCONNECT_FROM_WIFI_DIRECT";

    //----------------------------------------------------------------------------------------------
    // Update management
    //----------------------------------------------------------------------------------------------

    /**
     * For the moment (perhaps forever), firmware images can only either be files or assets
     */
    public static class FWImage {
        public File file;
        public boolean isAsset;

        public FWImage(File file, boolean isAsset) {
            this.file = file;
            this.isAsset = isAsset;
        }

        public String getName() {
            return file.getName();
        }
    }
}
