/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftccommon;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;

import java.io.File;
import java.util.ArrayList;

/**
 * List of RobotCore Robocol commands used by the FIRST apps
 */
@SuppressWarnings("WeakerAccess")
public class CommandList extends RobotCoreCommandList {

    public static final String CMD_RESTART_ROBOT = "CMD_RESTART_ROBOT";

    public static final String CMD_INIT_OP_MODE = "CMD_INIT_OP_MODE";
    public static final String CMD_RUN_OP_MODE = "CMD_RUN_OP_MODE";

    public static final String CMD_SCAN = "CMD_SCAN";
    public static final String CMD_SCAN_RESP = "CMD_SCAN_RESP";

    public static final String CMD_REQUEST_CONFIGURATIONS = "CMD_REQUEST_CONFIGURATIONS";
    public static final String CMD_REQUEST_CONFIGURATIONS_RESP = "CMD_REQUEST_CONFIGURATIONS_RESP";

    public static final String CMD_REQUEST_CONFIGURATION_TEMPLATES = "CMD_REQUEST_CONFIGURATION_TEMPLATES";
    public static final String CMD_REQUEST_CONFIGURATION_TEMPLATES_RESP = "CMD_REQUEST_CONFIGURATION_TEMPLATES_RESP";

    public static final String CMD_REQUEST_PARTICULAR_CONFIGURATION = "CMD_REQUEST_PARTICULAR_CONFIGURATION"; // also works for (resource-based) templates
    public static final String CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP = "CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP";

    public static final String CMD_ACTIVATE_CONFIGURATION = "CMD_ACTIVATE_CONFIGURATION";

    public static final String CMD_SAVE_CONFIGURATION = "CMD_SAVE_CONFIGURATION";
    public static final String CMD_DELETE_CONFIGURATION = "CMD_DELETE_CONFIGURATION";

    public static final String CMD_DISCOVER_LYNX_MODULES = "CMD_DISCOVER_LYNX_MODULES";
    public static final String CMD_DISCOVER_LYNX_MODULES_RESP = "CMD_DISCOVER_LYNX_MODULES_RESP";

    public static final String CMD_REQUEST_REMEMBERED_GROUPS = "CMD_REQUEST_REMEMBERED_GROUPS";
    public static final String CMD_REQUEST_REMEMBERED_GROUPS_RESP = "CMD_REQUEST_REMEMBERED_GROUPS_RESP";

    /**
     * Command to start programming mode (blocks).
     */
    public static final String CMD_START_PROGRAMMING_MODE = "CMD_START_PROGRAMMING_MODE";
    public static final String CMD_START_DS_PROGRAM_AND_MANAGE = "CMD_START_DS_PROGRAM_AND_MANAGE";

    /**
     * Response to a command to start programming mode (blocks).
     * <p>
     * Programming mode connection information will be in extra data.
     */
    public static final String CMD_START_PROGRAMMING_MODE_RESP = "CMD_START_PROGRAMMING_MODE_RESP";
    public static final String CMD_START_DS_PROGRAM_AND_MANAGE_RESP = "CMD_START_DS_PROGRAM_AND_MANAGE_RESP";

    /**
     * Notification that a message was logged during programming mode (blocks).
     * <p>
     * Log message will be in extra data.
     */
    public static final String CMD_PROGRAMMING_MODE_LOG_NOTIFICATION = "CMD_PROGRAMMING_MODE_LOG_NOTIFICATION";

    /**
     * Notification that the programming mode (blocks) server received a ping request.
     * <p>
     * PingDetails (encoded as json) will be in extra data.
     */
    public static final String CMD_PROGRAMMING_MODE_PING_NOTIFICATION = "CMD_PROGRAMMING_MODE_PING_NOTIFICATION";

    /**
     * Command to stop programming mode (blocks).
     */
    public static final String CMD_STOP_PROGRAMMING_MODE = "CMD_STOP_PROGRAMMING_MODE";

    //------------------------------------------------------------------------------------------------
    // Lynx firmware update suppport
    //------------------------------------------------------------------------------------------------

    public static final String CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES = "CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES";
    public static final String CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES_RESP = "CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES_RESP";

    public static class LynxFirmwareImagesResp {
        /**
         * used to prompt user as to where to load images for updating
         */
        File firstFolder = AppUtil.FIRST_FOLDER;
        /**
         * currently available images. files or assets.
         */
        ArrayList<FWImage> firmwareImages = new ArrayList<FWImage>();

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static LynxFirmwareImagesResp deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareImagesResp.class);
        }
    }

    public static final String CMD_GET_USB_ACCESSIBLE_LYNX_MODULES = "CMD_GET_USB_ACCESSIBLE_LYNX_MODULES";

    public static class USBAccessibleLynxModulesRequest {
        public boolean includeModuleNumbers = false;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static USBAccessibleLynxModulesRequest deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, USBAccessibleLynxModulesRequest.class);
        }
    }

    public static final String CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP = "CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP";

    public static class USBAccessibleLynxModulesResp {
        ArrayList<USBAccessibleLynxModule> modules = new ArrayList<USBAccessibleLynxModule>();

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static USBAccessibleLynxModulesResp deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, USBAccessibleLynxModulesResp.class);
        }
    }

    public static final String CMD_LYNX_FIRMWARE_UPDATE = "CMD_LYNX_FIRMWARE_UPDATE";

    public static class LynxFirmwareUpdate {

        SerialNumber serialNumber;
        FWImage firmwareImageFile;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static LynxFirmwareUpdate deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareUpdate.class);
        }
    }

    public static final String CMD_LYNX_FIRMWARE_UPDATE_RESP = "CMD_LYNX_FIRMWARE_UPDATE_RESP";

    public static class LynxFirmwareUpdateResp {

        boolean success;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static LynxFirmwareUpdateResp deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareUpdateResp.class);
        }
    }

    public static final String CMD_LYNX_ADDRESS_CHANGE = "CMD_LYNX_ADDRESS_CHANGE";

    public static class LynxAddressChangeRequest {

        public static class AddressChange {
            SerialNumber serialNumber;
            int oldAddress;
            int newAddress;
        }

        ArrayList<AddressChange> modulesToChange;

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
        }

        public static LynxAddressChangeRequest deserialize(String serialized) {
            return SimpleGson.getInstance().fromJson(serialized, LynxAddressChangeRequest.class);
        }
    }
}
