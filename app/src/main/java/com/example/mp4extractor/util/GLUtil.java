package com.example.mp4extractor.util;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by Li Yang on 2021/1/25.
 */
public class GLUtil {

    public static int generateOESTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        // Can't do mipmapping with camera source
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // Clamp to edge is the only option
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return textures[0];
    }

    public static int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        // load vertex shader
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        // load fragment shader
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }

        // create program
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("ES20_ERROR", "Could not link program: ");
                Log.e("ES20_ERROR", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    public static void checkGLError(final String tag) {
        int loopCnt = 0;
        for (int err = GLES20.glGetError(); loopCnt < 10 && err != GLES20.GL_FALSE; err = GLES20.glGetError(), ++loopCnt) {
            String msg;
            switch (err) {
                case GLES20.GL_INVALID_ENUM:
                    msg = "invalid enum";
                    break;
                case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
                    msg = "invalid framebuffer operation";
                    break;
                case GLES20.GL_INVALID_OPERATION:
                    msg = "invalid operation";
                    break;
                case GLES20.GL_INVALID_VALUE:
                    msg = "invalid value";
                    break;
                case GLES20.GL_OUT_OF_MEMORY:
                    msg = "out of memory";
                    break;
                default:
                    msg = "unknown error";
            }
            Log.e("GLUtil",  String.format("After tag \"%s\" glGetError %s(0x%x) ", tag, msg, err));
        }
    }
}
