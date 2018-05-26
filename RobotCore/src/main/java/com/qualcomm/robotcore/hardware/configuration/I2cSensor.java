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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * {@link I2cSensor} annotations may be placed on classes which are implementations of I2C sensors.
 * Such classes will be configurable using the configuration UI, and instances thereof will appear
 * in the hardware map in the uncategorized mapping. Classes on which this annotation is placed must
 * have a constructor with at least one of the following signatures:
 * <ol>
 * <li>ctor(I2cDeviceSyncSimple i2cDeviceSynch)</li>
 * <li>ctor(I2cDeviceSync i2cDeviceSynch)</li>
 * <li>ctor(I2cDevice i2cDevice)</li>
 * <li>ctor(I2cController i2cController, int port)</li>
 * </ol>
 *
 * @see HardwareMap#put(String, HardwareDevice)
 * @see HardwareMap#get(Class, String)
 * @see HardwareMap#get(String)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface I2cSensor {
    /**
     * Returns the XML tag to use to represent configured instances of the annotated
     * class as recorded in saved robot configurations. You should choose a tag that
     * is different than any other sensor type.
     *
     * @return the XML tag to use for instances of the annotated sensor class
     */
    String xmlTag() default "";

    /**
     * Returns the name of the annotated class to use when describing the class in
     * configuration user interfaces.
     *
     * @return the name to use for this kind of sensor in user interfaces
     */
    String name() default "";

    /**
     * Returns a brief descriptive phrase that describes this kind of sensor.
     *
     * @return a brief descriptive phrase that describes this kind of sensor.
     */
    String description() default "an I2c sensor";
}
