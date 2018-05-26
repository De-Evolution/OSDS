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
package org.firstinspires.ftc.robotcore.internal.network;

import android.content.Context;

import com.qualcomm.robotcore.R;

/**
 * {@link NetworkStatus} enumerates the various states we notice about our network
 */
public enum NetworkStatus {
    UNKNOWN,
    INACTIVE,
    ACTIVE,
    ENABLED,
    ERROR,
    CREATED_AP_CONNECTION,
    LAN_SERVER_CREATED;

    public String toString(Context context, Object... args) {
        switch (this) {
            case UNKNOWN:
                return context.getString(R.string.networkStatusUnknown);
            case ACTIVE:
                return context.getString(R.string.networkStatusActive);
            case INACTIVE:
                return context.getString(R.string.networkStatusInactive);
            case ENABLED:
                return context.getString(R.string.networkStatusEnabled);
            case ERROR:
                return context.getString(R.string.networkStatusError);
            case CREATED_AP_CONNECTION:
                return String.format(context.getString(R.string.networkStatusCreatedAPConnection), args);
            case LAN_SERVER_CREATED:
                return String.format(context.getString(R.string.networkStatusLANServerCreated), args);
            default:
                return context.getString(R.string.networkStatusInternalError);
        }
    }
}
