/*
Copyright (c) 2016 Robert Atkinson

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
package com.qualcomm.hardware.lynx.commands.core;

import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;

/**
 * Created by bob on 2016-03-07.
 */
public class LynxSetMotorChannelCurrentAlertLevelCommand extends LynxDekaInterfaceCommand<LynxAck> {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final int cbPayload = 3;

    private byte motor;
    private short mACurrentLimit;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetMotorChannelCurrentAlertLevelCommand(LynxModuleIntf module) {
        super(module);
    }

    public LynxSetMotorChannelCurrentAlertLevelCommand(LynxModuleIntf module, int motorZ, int mACurrentLimit) {
        this(module);
        LynxConstants.validateMotorZ(motorZ);
        this.motor = (byte) motorZ;
        this.mACurrentLimit = (short) mACurrentLimit;
        if (TypeConversion.unsignedShortToInt(this.mACurrentLimit) != mACurrentLimit) {
            throw new IllegalArgumentException(String.format("illegal current limit: %d mA", mACurrentLimit));
        }
    }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean isResponseExpected() {
        return false;
    }

    @Override
    public byte[] toPayloadByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);
        buffer.put(this.motor);
        buffer.putShort(this.mACurrentLimit);
        return buffer.array();
    }

    @Override
    public void fromPayloadByteArray(byte[] rgb) {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.motor = buffer.get();
        this.mACurrentLimit = buffer.getShort();
    }

}
