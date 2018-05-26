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
package com.qualcomm.robotcore.hardware.configuration;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.qualcomm.robotcore.hardware.DeviceManager;

import org.firstinspires.ftc.robotcore.internal.opmode.OnBotJavaClassLoader;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * {@link UserConfigurationType} contains metadata regarding classes which have been declared as
 * user-defined sensor implementations.
 */
@SuppressWarnings("WeakerAccess")
public abstract class UserConfigurationType implements ConfigurationType, Serializable // final because of serialization concerns
{
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public enum Flavor {I2C, MOTOR}

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected
    @Expose
    @NonNull
    final Flavor flavor;
    protected
    @Expose
    @NonNull
    String xmlTag;
    protected
    @Expose
    @NonNull
    String name = "";
    protected
    @Expose
    boolean isOnBotJava;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UserConfigurationType(Class clazz, @NonNull Flavor flavor, @NonNull String xmlTag) {
        this.flavor = flavor;
        this.xmlTag = xmlTag;
        this.isOnBotJava = OnBotJavaClassLoader.isOnBotJava(clazz);
    }

    // used by gson deserialization
    protected UserConfigurationType(@NonNull Flavor flavor) {
        this.flavor = flavor;
        this.xmlTag = "";
        this.isOnBotJava = false;
    }

    //----------------------------------------------------------------------------------------------
    // Serialization (used in local marshalling during configuration editing)
    //----------------------------------------------------------------------------------------------

    protected static class SerializationProxy implements Serializable {
        protected String xmlTag;

        public SerializationProxy(UserConfigurationType userConfigurationType) {
            this.xmlTag = userConfigurationType.xmlTag;
        }

        private Object readResolve() {
            return UserConfigurationTypeManager.getInstance().configurationTypeFromTag(xmlTag);
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream in) throws InvalidObjectException {
        throw new InvalidObjectException("proxy required"); // attack threat paranoia
    }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public
    @NonNull
    Flavor getFlavor() {
        return this.flavor;
    }

    public
    @NonNull
    String getName() {
        return this.name;
    }

    //----------------------------------------------------------------------------------------------
    // ConfigurationType
    //----------------------------------------------------------------------------------------------

    @Override
    @NonNull
    public String getDisplayName(DisplayNameFlavor flavor, Context context) {
        return this.name;
    }

    @Override
    @NonNull
    public String getXmlTag() {
        return this.xmlTag;
    }

    @Override
    @NonNull
    public DeviceManager.DeviceType toUSBDeviceType() {
        return DeviceManager.DeviceType.FTDI_USB_UNKNOWN_DEVICE;
    }

    @Override
    public boolean isDeviceFlavor(UserConfigurationType.Flavor flavor) {
        return this.flavor == flavor;
    }
}
