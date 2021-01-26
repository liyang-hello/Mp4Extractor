package com.example.mp4extractor;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.example.mp4extractor.util.GLUtil;
import com.example.mp4extractor.util.LogU;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class SphereShape {

    int SECTOR = 40;
    int STACK = 40;
    float RADIUS = 1;

    private int mTextureId = 0;
    private int mProgram = -1;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mMVPHandle;
    private FloatBuffer mVertices, mTexCoord;
    private ShortBuffer mIndices;
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private final float[] touchPitchMatrix = new float[16];
    private final float[] touchYawMatrix = new float[16];

    class Point{
        float x, y, z;
        float u,v;
    }

    private final static String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 a_Position;\n" +
                    "attribute vec2 aTexCoor;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform float uAngle;\n" +
                    "void main(){\n" +
                    "    gl_Position = uMVPMatrix * a_Position;\n" +
                    "    vTextureCoord = aTexCoor;\n" +
                    "}\n";
    private final static String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES uTexture;\n" +
                    "uniform mat4 uColorFilterMatrix;\n" +
                    "void main() {\n" +
                    "     gl_FragColor = texture2D(uTexture, vTextureCoord); \n" +
                    "}\n";

    public SphereShape() {
        init();
    }


    protected void init() {

        Matrix.setIdentityM(mModelMatrix, 0);
        float ratio = 9.0f/16;
//        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 0.1f, 100);
        Matrix.perspectiveM(mProjectionMatrix, 0, 60, ratio, 0.1f, 100);
        // create program
        mProgram = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        mMVPHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        LogU.d("init mTexCoordHandle "+ mTexCoordHandle);

        genVBO2(SECTOR, STACK, RADIUS);
    }

    private void genVBO(int sectorCount, int stackCount, float radius) {
        int count = (sectorCount+1) * (stackCount+1);
        ByteBuffer vbb = ByteBuffer.allocateDirect(count * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertices = vbb.asFloatBuffer();
        mVertices.position(0);
        ByteBuffer tbb = ByteBuffer.allocateDirect(count * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexCoord = tbb.asFloatBuffer();
        mTexCoord.position(0);

        float x, y, z, xy;                              // vertex position
        float s, t;                                     // vertex texCoord

        float sectorStep = (float)(2 * Math.PI / sectorCount);
        float stackStep = (float)( Math.PI / stackCount);
        float sectorAngle, stackAngle;

        for(int i = 0; i < stackCount; ++i)
        {
            stackAngle = (float) Math.PI / 2 - i * stackStep;        // starting from pi/2 to -pi/2
            xy =(float)(radius * Math.cos(stackAngle));             // r * cos(u)
            z = (float)(radius * Math.sin(stackAngle));              // r * sin(u)

            // add (sectorCount+1) vertices per stack
            // the first and last vertices have same position and normal, but different tex coords
            for(int j = 0; j <= sectorCount; ++j) {
                sectorAngle = j * sectorStep;           // starting from 0 to 2pi

                // vertex position (x, y, z)
                x =(float)( xy * Math.cos(sectorAngle));             // r * cos(u) * cos(v)
                y = (float)( xy * Math.sin(sectorAngle));             // r * cos(u) * sin(v)
                mVertices.put(x);
                mVertices.put(y);
                mVertices.put(z);

                // vertex tex coord (s, t) range between [0, 1]
                s = (float)j / sectorCount;
                t = (float)i / stackCount;
                mTexCoord.put(s);
                mTexCoord.put(t);
            }
        }
        mVertices.position(0);
        mTexCoord.position(0);

        // generate CCW index list of sphere triangles
        // k1--k1+1
        // |  / |
        // | /  |
        // k2--k2+1
        ByteBuffer ibb = ByteBuffer.allocateDirect(stackCount*sectorCount * 6 * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndices = ibb.asShortBuffer();
        mIndices.position(0);
        short k1, k2;
        for(int i = 0; i < stackCount; ++i)
        {
            k1 = (short)(i * (sectorCount + 1));     // beginning of current stack
            k2 = (short)(k1 + sectorCount + 1);      // beginning of next stack

            for(int j = 0; j < sectorCount; ++j, ++k1, ++k2) {
                // 2 triangles per sector excluding first and last stacks
                // k1 => k2 => k1+1
                if(i != 0) {
                    mIndices.put(k1);
                    mIndices.put(k2);
                    mIndices.put((short)( k1 + 1));
                }

                // k1+1 => k2 => k2+1
                if(i != (stackCount-1)) {
                    mIndices.put((short)( k1 + 1));
                    mIndices.put(k2);
                    mIndices.put((short)( k1 + 1));
                }

            }
        }
        LogU.d("mIndices "+ mIndices);
        mIndices.position(0);
    }

    private Point caculatePoint(int i, int j, float sectorStep, float stackStep, float radius, int sectorCount, int stackCount) {
        Point p = new Point();
        float sectorAngle, stackAngle;
        float xy;
        stackAngle = (float) Math.PI / 2 - i * stackStep;        // starting from pi/2 to -pi/2
        sectorAngle = j * sectorStep;

        xy =(float)(radius * Math.cos(stackAngle));             // r * cos(u)
        p.z = (float)(radius * Math.sin(stackAngle));              // r * sin(u)
        p.x =(float)( xy * Math.cos(sectorAngle));             // r * cos(u) * cos(v)
        p.y = (float)( xy * Math.sin(sectorAngle));             // r * cos(u) * sin(v)

        p.u = (float)j / sectorCount;
        p.v = (float)i / stackCount;
        return p;
    }

    int indices[] = {0, 1, 2, 2, 1, 3};
    /** 0     2
        |  / /
        | / /
        1  ----3*/
    private void genVBO2(int sectorCount, int stackCount, float radius) {
        int count = (sectorCount+1) * (stackCount+1);
        ByteBuffer vbb = ByteBuffer.allocateDirect(count * 3 * 4 *6);
        vbb.order(ByteOrder.nativeOrder());
        mVertices = vbb.asFloatBuffer();
        mVertices.position(0);
        ByteBuffer tbb = ByteBuffer.allocateDirect(count * 2 * 4 *6);
        tbb.order(ByteOrder.nativeOrder());
        mTexCoord = tbb.asFloatBuffer();
        mTexCoord.position(0);

        float sectorStep = (float)(2 * Math.PI / sectorCount);
        float stackStep = (float)( Math.PI / stackCount);
        Point p[] = new Point[4];
        for(int i = 0; i < stackCount; ++i)
        {
            for(int j = 0; j < sectorCount; ++j) {

                p[0] = caculatePoint(i,      j, sectorStep, stackStep, radius, sectorCount, stackCount);
                p[1] = caculatePoint(i,   j+1, sectorStep, stackStep, radius, sectorCount, stackCount);
                p[2] = caculatePoint(i+1, j, sectorStep, stackStep, radius, sectorCount, stackCount);
                p[3] = caculatePoint(i+1, j+1, sectorStep, stackStep, radius, sectorCount, stackCount);

                for(int k=0; k<indices.length; k++) {
                    mVertices.put(p[indices[k]].x);
                    mVertices.put(p[indices[k]].y);
                    mVertices.put(p[indices[k]].z-1);

                    mTexCoord.put(p[indices[k]].u);
                    mTexCoord.put(p[indices[k]].v);
                }
            }
        }
        mVertices.position(0);
        mTexCoord.position(0);

    }


    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    public void draw() {
        draw(mTextureId);
    }

    private float[] tempMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] tempMatrix2 = new float[16];
    public void draw(int textureId) {
//        Log.d("RectShape", "draw "+ textureId);
        GLES20.glUseProgram(mProgram);

//        Matrix.multiplyMM(tempMatrix, 0, mModelMatrix, 0, touchYawMatrix, 0);
        Matrix.multiplyMM(tempMatrix, 0, touchYawMatrix, 0, touchPitchMatrix, 0);
        Matrix.multiplyMM(tempMatrix2, 0, mModelMatrix, 0, tempMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, tempMatrix2, 0);
        GLES20.glUniformMatrix4fv(mMVPHandle, 1, false, mMVPMatrix, 0);

        if (textureId > 0) {
            // render texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        }
        // draw triangles
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertices);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoord);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertices.capacity() / 3);
    }

    public void setPitchOffset(float pitch) {
        Matrix.setRotateM(touchPitchMatrix, 0, pitch, 1, 0, 0);
    }

    public void setYawOffset(float yaw) {
        Matrix.setRotateM(touchYawMatrix, 0, -yaw, 0, 1, 0);
    }

    public void release() {
        if (mProgram >= 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }
}
