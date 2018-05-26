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

package com.qualcomm.ftccommon.configuration;

import android.view.View;
import android.widget.Spinner;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;

import java.util.Arrays;
import java.util.Comparator;

/**
 * {@link EditI2cDevicesActivityAbstract} manages a possibly-growable list of I2c devices. The set of
 * legal devices is passed in as the id of a string array in EditParameters.getResourceId().
 */
public abstract class EditI2cDevicesActivityAbstract<ITEM_T extends DeviceConfiguration> extends EditPortListSpinnerActivity<ITEM_T> {
    ConfigurationType[] configurationTypes = new ConfigurationType[0];

    public EditI2cDevicesActivityAbstract() {
        this.layoutMain = R.layout.i2cs;
        this.idListParentLayout = R.id.item_list_parent;
        this.layoutItem = R.layout.i2c_device;
        this.idItemRowPort = R.id.row_port_i2c;
        this.idItemSpinner = R.id.choiceSpinner;
        this.idItemEditTextResult = R.id.editTextResult;
        this.idItemPortNumber = R.id.port_number;
    }

    @Override
    protected void deserialize(EditParameters parameters) {
        super.deserialize(parameters);
        if (parameters.getConfigurationTypes() != null) {
            this.configurationTypes = parameters.getConfigurationTypes();
        }
    }

    @Override
    protected void localizeSpinner(View itemView) {
        Spinner spinner = (Spinner) itemView.findViewById(this.idItemSpinner);

        Comparator<ConfigurationType> comparator = new Comparator<ConfigurationType>() {
            @Override
            public int compare(ConfigurationType lhs, ConfigurationType rhs) {
                // Make sure 'nothing' is first
                if (lhs == rhs) {
                    return 0;
                }
                if (lhs == BuiltInConfigurationType.NOTHING) {
                    return -1;
                }
                if (rhs == BuiltInConfigurationType.NOTHING) {
                    return 1;
                }
                return 0;   // they'll be distinguished using an outer level comparator
            }
        };

        localizeConfigTypeSpinnerTypes(ConfigurationType.DisplayNameFlavor.Normal, spinner, Arrays.asList(this.configurationTypes), comparator);
    }
}