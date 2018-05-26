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

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.qualcomm.robotcore.util.ClassUtil;

import org.firstinspires.ftc.robotcore.external.navigation.Rotation;

/**
 * {@link MotorConfigurationType} contains the amalgamated set of information that
 * is known about a given type of motor.
 */
@SuppressWarnings("WeakerAccess")
@SuppressLint("DefaultLocale")
public final class MotorConfigurationType extends UserConfigurationType implements Cloneable // final because of serialization concerns
{
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private
    @Expose
    double ticksPerRev;
    private
    @Expose
    double gearing;
    private
    @Expose
    double maxRPM;
    private
    @Expose
    double achieveableMaxRPMFraction;
    private
    @Expose
    Rotation orientation;

    private
    @Expose
    @NonNull
    DistributorInfoState distributorInfo = new DistributorInfoState();
    private
    @Expose
    @NonNull
    ModernRoboticsMotorControllerParamsState modernRoboticsParams = new ModernRoboticsMotorControllerParamsState();
    private
    @Expose
    @NonNull
    ExpansionHubMotorControllerParamsState hubVelocityParams = new ExpansionHubMotorControllerParamsState();
    private
    @Expose
    @NonNull
    ExpansionHubMotorControllerParamsState hubPositionParams = new ExpansionHubMotorControllerParamsState();

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public double getTicksPerRev() {
        return ticksPerRev;
    }

    public double getAchieveableMaxTicksPerSecond() {
        final double encoderTicksPerRev = this.getTicksPerRev();
        final double maxRPM = this.getMaxRPM() * this.getAchieveableMaxRPMFraction();
        final double secondsPerMinute = 60;
        return encoderTicksPerRev * maxRPM / secondsPerMinute;
    }

    public int getAchieveableMaxTicksPerSecondRounded() {
        return (int) Math.round(getAchieveableMaxTicksPerSecond());
    }

    public void setTicksPerRev(double ticksPerRev) {
        this.ticksPerRev = ticksPerRev;
    }

    public double getGearing() {
        return gearing;
    }

    public void setGearing(double gearing) {
        this.gearing = gearing;
    }

    public double getMaxRPM() {
        return maxRPM;
    }

    public void setMaxRPM(double maxRPM) {
        this.maxRPM = maxRPM;
    }

    public double getAchieveableMaxRPMFraction() {
        return achieveableMaxRPMFraction;
    }

    public void setAchieveableMaxRPMFraction(double achieveableMaxRPMFraction) {
        this.achieveableMaxRPMFraction = achieveableMaxRPMFraction;
    }

    public Rotation getOrientation() {
        return orientation;
    }

    public void setOrientation(Rotation orientation) {
        this.orientation = orientation;
    }

    public boolean hasModernRoboticsParams() {
        return !modernRoboticsParams.isDefault();
    }

    public
    @NonNull
    ModernRoboticsMotorControllerParamsState getModernRoboticsParams() {
        return modernRoboticsParams;
    }

    public boolean hasExpansionHubVelocityParams() {
        return !hubVelocityParams.isDefault();
    }

    public ExpansionHubMotorControllerParamsState getHubVelocityParams() {
        return hubVelocityParams;
    }

    public boolean hasExpansionHubPositionParams() {
        return !hubPositionParams.isDefault();
    }

    public ExpansionHubMotorControllerParamsState getHubPositionParams() {
        return hubPositionParams;
    }

    public
    @NonNull
    DistributorInfoState getDistributorInfo() {
        return distributorInfo;
    }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static MotorConfigurationType getUnspecifiedMotorType() {
        return UserConfigurationTypeManager.getInstance().getUnspecifiedMotorType();
    }

    public static MotorConfigurationType getMotorType(Class<?> clazz) {
        return (MotorConfigurationType) UserConfigurationTypeManager.getInstance().userTypeFromClass(Flavor.MOTOR, clazz);
    }

    public MotorConfigurationType(Class clazz, String xmlTag) {
        super(clazz, Flavor.MOTOR, xmlTag);
    }

    // Used by gson deserialization
    public MotorConfigurationType() {
        super(Flavor.MOTOR);
    }

    public MotorConfigurationType clone() {
        try {
            MotorConfigurationType result = (MotorConfigurationType) super.clone();
            result.distributorInfo = distributorInfo.clone();
            result.modernRoboticsParams = modernRoboticsParams.clone();
            result.hubVelocityParams = hubVelocityParams.clone();
            result.hubPositionParams = hubPositionParams.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("internal error: Parameters not cloneable");
        }
    }

    public void processAnnotation(@Nullable MotorType motorType) {
        if (motorType != null) {
            if (name == null || name.isEmpty()) {
                name = ClassUtil.decodeStringRes(motorType.name().trim());
            }

            ticksPerRev = motorType.ticksPerRev();
            gearing = motorType.gearing();
            maxRPM = motorType.maxRPM();
            achieveableMaxRPMFraction = motorType.achieveableMaxRPMFraction();
            orientation = motorType.orientation();
        }
    }

    public void processAnnotation(@Nullable ModernRoboticsMotorControllerParams params) {
        if (params != null) {
            modernRoboticsParams = new ModernRoboticsMotorControllerParamsState(params);
        }
    }

    public void processAnnotation(@Nullable ExpansionHubMotorControllerVelocityParams params) {
        if (params != null) {
            hubVelocityParams = new ExpansionHubMotorControllerParamsState(params);
        }
    }

    public void processAnnotation(@Nullable ExpansionHubMotorControllerPositionParams params) {
        if (params != null) {
            hubPositionParams = new ExpansionHubMotorControllerParamsState(params);
        }
    }

    public void processAnnotation(@Nullable DistributorInfo info) {
        if (info != null) {
            if (name == null || name.isEmpty()) {
                String distributor = ClassUtil.decodeStringRes(info.distributor().trim());
                String model = ClassUtil.decodeStringRes(info.model().trim());
                if (!distributor.isEmpty() && !model.isEmpty()) {
                    name = distributor + " " + model;
                }
            }

            distributorInfo = DistributorInfoState.from(info);
        }
    }

    public void finishedAnnotations(Class clazz) {
        if (name == null || name.isEmpty()) {
            name = clazz.getSimpleName();
        }
    }

    //----------------------------------------------------------------------------------------------
    // Serialization (used in local marshalling during configuration editing)
    //----------------------------------------------------------------------------------------------

    private Object writeReplace() {
        return new SerializationProxy(this);
    }
}
