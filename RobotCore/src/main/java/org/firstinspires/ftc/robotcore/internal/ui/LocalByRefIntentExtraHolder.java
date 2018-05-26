/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link LocalByRefIntentExtraHolder} can be used to pass an object reference by reference into
 * a newly launched activity. That activity, of course, MUST be in the same process as the
 * caller who launched it.
 * <p>
 * The key thing is that the creation of the holder and the retrieval of the parameter must
 * happen in the same process. It's ok that any intent to which this parameter were a black
 * box be in any process in the system.
 */
@SuppressWarnings("WeakerAccess")
public class LocalByRefIntentExtraHolder implements Parcelable {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private static final Map<UUID, Object> map = new ConcurrentHashMap<>();

    protected UUID uuid;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LocalByRefIntentExtraHolder(Object target) {
        this.uuid = UUID.randomUUID();
        map.put(uuid, target);
    }

    private LocalByRefIntentExtraHolder(Parcel parcel) {
        uuid = UUID.fromString(parcel.readString());
    }

    public Object getTargetAndForget() {
        Object result = map.get(uuid);
        map.remove(uuid);
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Parcelable
    //----------------------------------------------------------------------------------------------

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(uuid.toString());
    }

    public static final Parcelable.Creator<LocalByRefIntentExtraHolder> CREATOR = new Parcelable.Creator<LocalByRefIntentExtraHolder>() {
        public LocalByRefIntentExtraHolder createFromParcel(Parcel parcel) {
            return new LocalByRefIntentExtraHolder(parcel);
        }

        public LocalByRefIntentExtraHolder[] newArray(int size) {
            return new LocalByRefIntentExtraHolder[size];
        }
    };

}
