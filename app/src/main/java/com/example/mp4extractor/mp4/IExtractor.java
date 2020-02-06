package com.example.mp4extractor.mp4;

import android.content.res.AssetFileDescriptor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Description:
 * Created by liyang on 2019/11/29.
 */
public interface IExtractor {
    void setDataSource(String path) throws IOException;

    void setDataSource(@NonNull AssetFileDescriptor afd);

    int getTrackCount();

    void selectTrack(int trackId);

    MediaFormat getTrackFormat(int trackId);

    int readSampleData(@NonNull ByteBuffer byteBuf, int offset);

    long getSampleTime();

    boolean advance();

    void seekTo(long timeUs, int mode);

    void release();
}
