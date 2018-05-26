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
package org.firstinspires.ftc.robotcore.internal.opengl.shaders;

import android.opengl.GLES20;
import android.support.annotation.RawRes;

import com.qualcomm.robotcore.R;

import org.firstinspires.ftc.robotcore.internal.opengl.models.MeshObject;

public class CubeMeshVertexShader extends SimpleVertexShader {
    protected final int a_vertexNormal;
    protected final int a_vertexTexCoord;

    @RawRes
    public static final int resourceId = R.raw.cube_mesh_vertex_shader;

    public CubeMeshVertexShader(int programId) {
        super(programId);
        a_vertexNormal = GLES20.glGetAttribLocation(programId, "vertexNormal");
        a_vertexTexCoord = GLES20.glGetAttribLocation(programId, "vertexTexCoord");
    }

    public void setCoordinates(MeshObject meshObject) {
        GLES20.glVertexAttribPointer(a_vertexPosition, 3, GLES20.GL_FLOAT, false, 0, meshObject.getVertices());
        GLES20.glVertexAttribPointer(a_vertexNormal, 3, GLES20.GL_FLOAT, false, 0, meshObject.getNormals());
        GLES20.glVertexAttribPointer(a_vertexTexCoord, 2, GLES20.GL_FLOAT, false, 0, meshObject.getTexCoords());

        GLES20.glEnableVertexAttribArray(a_vertexPosition);
        GLES20.glEnableVertexAttribArray(a_vertexNormal);
        GLES20.glEnableVertexAttribArray(a_vertexTexCoord);
    }

    public void disableAttributes() {
        super.disableAttributes();
        GLES20.glDisableVertexAttribArray(a_vertexNormal);
        GLES20.glDisableVertexAttribArray(a_vertexTexCoord);
    }
}
