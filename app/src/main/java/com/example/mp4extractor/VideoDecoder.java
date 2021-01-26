package com.example.mp4extractor;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {

    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;
    private MediaExtractor mExtractor = null;
    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int id) {
            if(mExtractor== null) {
                Log.e(TAG, "extractor is null");
            }
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(id);
            inputBuffer.clear();
            int length = mExtractor.readSampleData(inputBuffer, 0);
            mediaCodec.queueInputBuffer(id, 0, length, mExtractor.getSampleTime(), 0);
            mExtractor.advance();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
            if (mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0) {
                byte[] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
            }
            mMediaCodec.releaseOutputBuffer(id, true);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "------> onError");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "------> onOutputFormatChanged");
        }
    };

    public VideoDecoder(@NonNull Surface surface) {
        this.mSurface = surface;
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderHandlerThread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startDecoder(String path) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String mimeType = null;
        for (int i=0; i<mExtractor.getTrackCount(); i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            if(format.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                mMediaFormat = format;
                mimeType = format.getString(MediaFormat.KEY_MIME);
                break;
            }
        }

        if(mimeType == null) {
            Log.e(TAG, "has no video track");
            return;
        }

        try {
            mMediaCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        if (mMediaCodec != null && mSurface != null) {
            mMediaCodec.setCallback(mCallback, mVideoDecoderHandler);
            mMediaCodec.configure(mMediaFormat, mSurface, null, CONFIGURE_FLAG_DECODE);
            mMediaCodec.start();
        } else {
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

    public void stopDecoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release() {
        if (mMediaCodec != null) {
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
