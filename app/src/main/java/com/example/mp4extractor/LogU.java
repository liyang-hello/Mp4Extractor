package com.example.mp4extractor;
import android.util.Log;

import java.util.Objects;

/**
 * Created by luofei on 2017/4/25.
 */

public class LogU {
    public static final String TAG = LogU.class.getSimpleName();
    public static final int V = 1;
    public static final int D = 2;
    public static final int I = 3;
    public static final int W = 4;
    public static final int E = 5;
    public static boolean isShowLog = true;  //是否显示log日志
    public static String defaultMsg = "";

    public static void closeLog(){
        isShowLog = false;
    }
    public static void init(boolean isShowLog) {
        LogU.isShowLog = isShowLog;
    }

    public static void init(boolean isShowLog, String defaultMsg) {
        LogU.isShowLog = isShowLog;
        LogU.defaultMsg = defaultMsg;
    }

    public static void v() {
        llog(V, null, defaultMsg);
    }

    public static void llog(int type, String tagStr, Object obj) {
        String msg;
        if (!isShowLog) {
            return;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int index = 4;
        String className = stackTrace[index].getFileName();//获取类名
        String methodName = stackTrace[index].getMethodName();//获取方法名
        int lineNumber = stackTrace[index].getLineNumber();//获取代码行数

        String tag = (tagStr == null ? className : tagStr);
        methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);

        StringBuilder sb = new StringBuilder();
        sb.append("[ (").append(className).append(":").append(lineNumber).append(")#").append(methodName).append("]");

        if (obj == null) {
            msg = "Log with null object";
        } else {
            msg = obj.toString();
        }
        if (msg != null) {
            sb.append(msg);
        }

        String logStr = sb.toString();

        switch (type) {
            case V:
                Log.v(tag, logStr);
                break;
            case D:
                Log.d(tag, logStr);
                break;
            case I:
                Log.i(tag, logStr);
                break;
            case W:
                Log.w(tag, logStr);
                break;
            case E:
                Log.e(tag, logStr);
                break;
        }
    }

    public static void v(Object obj) {
        llog(V, null, obj);
    }

    public static void v(String tag, Objects obj) {
        llog(V, tag, obj);
    }

    public static void d() {
        llog(D, null, defaultMsg);
    }

    public static void d(Object obj) {
        llog(D, null, obj);
    }

    public static void d(String tag, Objects obj) {
        llog(D, tag, obj);
    }

    public static void i() {
        llog(I, null, defaultMsg);
    }

    public static void i(Object obj) {
        llog(I, null, obj);
    }

    public static void i(String tag, Objects obj) {
        llog(I, tag, obj);
    }

    public static void w() {
        llog(W, null, defaultMsg);
    }

    public static void w(Object obj) {
        llog(W, null, obj);
    }

    public static void w(String tag, Objects obj) {
        llog(W, tag, obj);
    }

    public static void e() {
        llog(E, null, defaultMsg);
    }

    public static void e(Object obj) {
        llog(E, null, obj);
    }

    public static void e(String tag, Objects obj) {
        llog(E, tag, obj);
    }

    public static void printFloatArray( String TAG, float[] array) {
        String result= TAG + " ";
        for (int i=0; i< array.length; i++ ) {
            if(i%4 == 0) {
                result = result + " -- \n";
            }
            result = result + " "+ array[i];
        }

        llog(D, null, result);
    }
    public static void printIntArray( String TAG, int[] array) {
        String result= TAG + " ";
        for (int i=0; i< array.length; i++ ) {
            if(i%4 == 0) {
                result = result + " -- ";
            }
            result = result + " "+ array[i];
        }

        llog(D, null, result);
    }

    public static void printByteArray(String TAG, byte array[], int length) {
        String result= TAG + " byte array start--------------------------------------------------";
        if(array==null){
            return;
        }
        int len = length<array.length ? length : array.length;
        for (int i=0; i< len; i++ ) {
            if(i%16 == 0) {
                result = result + " \n ";
            }
            result = result + String.format(" 0x%02x ", array[i]);
        }

        llog(D, null, result);
    }
}
