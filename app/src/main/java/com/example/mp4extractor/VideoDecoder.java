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

import com.example.mp4extractor.mp4.Mp4Extractor;
import com.example.mp4extractor.util.LogU;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {

    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;
    private Mp4Extractor mExtractor = null;
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
            if(length >0) {
//                for(int i=0; i< 10; i++) {
//                     LogU.d(" i= "+i + " "+ inputBuffer.get(i));
//                }
                Log.d(TAG, "id= "+ id + " len= "+ length + " timestamp= "+ mExtractor.getSampleTime());
                mediaCodec.queueInputBuffer(id, 0, length, mExtractor.getSampleTime(), 0);
                mExtractor.advance();
            }

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
        mExtractor = new Mp4Extractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        LogU.d("trackCount  "+ mExtractor.getTrackCount());
        String mimeType = null;
        for (int i=0; i<mExtractor.getTrackCount(); i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            LogU.d("format "+ format);
            if(format.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                mMediaFormat = format;
                mimeType = format.getString(MediaFormat.KEY_MIME);
                mExtractor.selectTrack(i);
                break;
            }
        }

//        MediaExtractor extractor = new MediaExtractor();
//        try {
//            extractor.setDataSource(path);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//        LogU.d("trackCount  "+ extractor.getTrackCount());
//        for (int i=0; i<extractor.getTrackCount(); i++) {
//            MediaFormat format = extractor.getTrackFormat(i);
//            LogU.d("22 format "+ format);
//            if(format.getString(MediaFormat.KEY_MIME).startsWith("video")) {
//                mMediaFormat = format;
//                break;
//            }
//        }

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

    public void stop() {
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

        if(mExtractor != null) {
            mExtractor.release();
        }
    }
}
