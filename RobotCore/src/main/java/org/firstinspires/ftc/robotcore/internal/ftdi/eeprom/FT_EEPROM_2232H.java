/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi.eeprom;

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FT_EEPROM_2232H extends FT_EEPROM {
    public boolean AL_SlowSlew = false;
    public boolean AL_SchmittInput = false;
    public byte AL_DriveCurrent = 0;
    public boolean AH_SlowSlew = false;
    public boolean AH_SchmittInput = false;
    public byte AH_DriveCurrent = 0;
    public boolean BL_SlowSlew = false;
    public boolean BL_SchmittInput = false;
    public byte BL_DriveCurrent = 0;
    public boolean BH_SlowSlew = false;
    public boolean BH_SchmittInput = false;
    public byte BH_DriveCurrent = 0;
    public boolean A_UART = false;
    public boolean B_UART = false;
    public boolean A_FIFO = false;
    public boolean B_FIFO = false;
    public boolean A_FIFOTarget = false;
    public boolean B_FIFOTarget = false;
    public boolean A_FastSerial = false;
    public boolean B_FastSerial = false;
    public boolean PowerSaveEnable = false;
    public boolean A_LoadVCP = false;
    public boolean B_LoadVCP = false;
    public boolean A_LoadD2XX = false;
    public boolean B_LoadD2XX = false;
    public int TPRDRV = 0;

    public FT_EEPROM_2232H() {
    }

    public static final class DRIVE_STRENGTH {
        static final byte DRIVE_4mA = 0;
        static final byte DRIVE_8mA = 1;
        static final byte DRIVE_12mA = 2;
        static final byte DRIVE_16mA = 3;

        public DRIVE_STRENGTH() {
        }
    }
}
