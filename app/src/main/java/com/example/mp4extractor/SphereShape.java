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

/**
 * Created by Li Yang on 2021/1/25.
 */
public class SphereShape {

    int SECTOR = 40;
    int STACK = 40;
    float RADIUS = 50;

    private int mTextureId = 0;
    private int mProgram = -1;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mMVPHandle;
    private FloatBuffer mVertices, mTexCoord;
    private ShortBuffer mIndices;
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
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
        Matrix.perspectiveM(mProjectionMatrix, 0, 70, ratio, 0.1f, 100);
        Matrix.setLookAtM(mViewMatrix, 0, 0,0,0, 0, 0,-1, 0,1,0);
        // create program
        mProgram = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        mMVPHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        LogU.d("init mTexCoordHandle "+ mTexCoordHandle);

        genVBO(SECTOR, STACK, RADIUS);
        setPitchOffset(90);
        setYawOffset(0);
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
    private void genVBO(int sectorCount, int stackCount, float radius) {
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
                    mVertices.put(p[indices[k]].z);

                    mTexCoord.put(p[indices[k]].u);
                    mTexCoord.put(p[indices[k]].v);
                }
            }
        }
        mVertices.position(0);
        mTexCoord.position(0);

    }

    public void setSurfaceSize(int with, int height) {
        float ratio = (float) with/height;
        Matrix.perspectiveM(mProjectionMatrix, 0, 80, ratio, 0.1f, 100);
    }

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    public void draw() {
        draw(mTextureId);
    }

    private float[] mMVPMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    public void draw(int textureId) {
        GLES20.glUseProgram(mProgram);

        Matrix.multiplyMM(mModelMatrix, 0, touchYawMatrix, 0, touchPitchMatrix, 0);
        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
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
