package com.example.mp4extractor.mp4;

import android.content.res.AssetFileDescriptor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.example.mp4extractor.util.LogU;
import com.googlecode.mp4parser.FileDataSourceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Description:
 * Created by liyang on 2019/11/24.
 */
public class Mp4Extractor implements IExtractor {

    /**
     * File dataSource / channel
     */
    private FileDataSourceImpl dataSource;

    /**
     * Provider of boxes
     */
    private IsoFile isoFile;

    MovieBox mMovieBox;

    Map<Integer, TrackParser> mTrackParserMap = new HashMap<>();

    TrackParser mTrackParser;

    List<TrackBox> mTracks = null; // trak
    byte CODEC_PREFIX[] = new byte[]{0x0, 0x0, 0x0, 0x1};

    public void setDataSource(String path) throws IOException {
        LogU.d(" path "+ path);
        File f = new File(path);
        if (f.exists() && f.canRead()) {
            dataSource = new FileDataSourceImpl(f);
            isoFile = new IsoFile(dataSource);

//            long mStartTime = System.currentTimeMillis();
            mMovieBox = isoFile.getBoxes(MovieBox.class).get(0);
//            long costTime = System.currentTimeMillis() - mStartTime;
//            LogU.d("decode header costTime(ms):  = " + costTime);
        } else {
            LogU.w("Reader was passed an unreadable or non-existant file");
        }

    }

    @Override
    public void setDataSource(@NonNull AssetFileDescriptor afd) {

    }

    public int getTrackCount() {
        int trackCount = 0;
        if(mMovieBox != null) {
            trackCount = mMovieBox.getTrackCount();
            if(mTracks == null) {
                mTracks = mMovieBox.getBoxes(TrackBox.class);
            }
        }
        return trackCount;
    }

    public void selectTrack(int trackId) {
        mTrackParser = getTrack(trackId);
        if(mTrackParser != null) {
            mTrackParser.prepareFramesInfo();
        }
    }

    public MediaFormat getTrackFormat(int trackId) {
        MediaFormat format = null;
        TrackParser trackParser = getTrack(trackId);
        format = trackParser.getFormat();
        return format;
    }

    public int readSampleData(@NonNull ByteBuffer byteBuf, int offset) {
//        LogU.d("readSampleData start ");
        int readSize = -1;
        Tag tag = mTrackParser.readTag();
        if(tag != null) {
            byteBuf.position(offset);
//            LogU.d(" tag type "+ tag.getDataType());
            if(tag.getDataType() == IoConstants.TYPE_VIDEO) {
//                LogU.d(" replace start cocde ");
                int nalLen = 0;
                int srcOffset = 0;
                while(srcOffset < tag.getBodySize()) {
                    nalLen = toInt(tag.getBody().array(), srcOffset, 4);
                    byteBuf.put(CODEC_PREFIX);
                    srcOffset += 4;
                    byteBuf.put(tag.getBody().array(), srcOffset, nalLen);
                    srcOffset += nalLen;
                }
            } else {
                byteBuf.put(tag.getBody().array(), 0, tag.getBodySize());
            }

            readSize = tag.getBodySize();
        }
//        LogU.d("readSampleData end ");
//        LogU.e("readSampleData///------");
//        for(int i=0; i< 10; i++) {
//            LogU.d(" i= "+i + " "+ byteBuf.get(i));
//        }
        return readSize;
    }

    private int toInt(byte[] bytes, int offset, int length){
        return ByteBuffer.wrap(bytes, offset, length).getInt();
    }

    public long getSampleTime() {
        long sampleTime = 0;
        sampleTime = mTrackParser.getTimestamp();
        return sampleTime;
    }

    public boolean advance() {
        if(mTrackParser != null) {
            return mTrackParser.advance();
        }
        return true;
    }

    public void seekTo(long timeUs, int mode) {
        if(mTrackParser != null) {
            mTrackParser.seekTo(timeUs);
        }
    }

    public void release() {
        if(dataSource != null) {
            try {
                dataSource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataSource = null;
        }
    }

    private TrackParser getTrack(int trackId) {
        if(trackId > getTrackCount()) {
            throw new RuntimeException("invalid track id");
        }

        TrackParser trackParser = mTrackParserMap.get(trackId);
        if(trackParser == null) {
            trackParser = new TrackParser(dataSource, isoFile, mTracks, trackId);
            mTrackParserMap.put(trackId, trackParser);
        }

        return trackParser;
    }

}
