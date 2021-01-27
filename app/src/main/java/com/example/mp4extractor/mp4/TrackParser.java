package com.example.mp4extractor.mp4;
import android.media.MediaFormat;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.example.mp4extractor.util.LogU;
import com.example.mp4extractor.mp4.codec.AvcCodecConfig;
import com.example.mp4extractor.mp4.codec.CodecConfig;
import com.example.mp4extractor.mp4.codec.HevcCodecConfig;
import com.example.mp4extractor.mp4.codec.PCMConfig;
import com.googlecode.mp4parser.FileDataSourceImpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Description: box tree as follow：
 *            -moov
 *              |-mvhd
 *              |-trak
 *              |    |-tkhd (width, height, duration, track-id)
 *              |    |-edts
 *              |    |   |-elst
 *              |    |
 *              |    |-mdia
 *              |        |-mdhd (timescale, duration, language)
 *              |        |-hdlr (media type vide or soun)
 *              |        |-minf
 *              |           |-vmhd
 *              |           |-hdlr
 *              |           |-dinf
 *              |           |    |-dref
 *              |           |        |-url
 *              |           |-stbl
 *              |               |-stsd
 *              |               |   |-hvc1/avc1
 *              |               |       |-hvcC /avcc (vps, sps, pps, profile, level )
 *              |               |-stts (sample time)
 *              |               |-stss (sync frame)
 *              |               |-stsc (sample to chunk)
 *              |               |-stsz (sample size)
 *              |               |-stco (chunk offset)
 *              |-trak
 *              |-udta
 *
 * Created by liyang on 2019/11/25.
 */
public class TrackParser {
    private final static String TAG = "TrackParser";

    private IsoFile mIsoFile = null;
    private TrackBox mTrackBox = null;
    private MediaFormat mMediaFormat = null;
    private long mTimeScale;
    private long mDurationUs;
    private FileDataSourceImpl dataSource = null;
    private STBLBoxParser mSTBLBoxParser = null;

    private final Semaphore lock = new Semaphore(1, true);
    private int currentFrame = 0;
    private int prevFrameSize = 0;
    private List<MP4Frame> frames = new ArrayList<MP4Frame>();
    private int mTrackId;

    public TrackParser(FileDataSourceImpl dataSource, IsoFile isoFile, List<TrackBox> trackBoxes, int trackId) {
        if(trackId > trackBoxes.size()) {
            throw new RuntimeException("trackId is larger then track count");
        }
        mTrackId = trackId;

        this.dataSource = dataSource;
        mIsoFile = isoFile;
        mTrackBox = trackBoxes.get(trackId);
    }

    public MediaFormat getFormat() {
        if(mMediaFormat == null) {
            mMediaFormat = parseTrackFormat();
        }
        return mMediaFormat;
    }

    private MediaFormat parseTrackFormat() {
        MediaFormat format = new MediaFormat();

        TrackHeaderBox tkhd = mTrackBox.getTrackHeaderBox(); // tkhd
        if (tkhd != null) {
//            LogU.d("Track id: "+ tkhd.getTrackId() + " tkhd "+tkhd);
            int width = (int) tkhd.getWidth();
            int height = (int) tkhd.getHeight();
            long duration = tkhd.getDuration();
//            LogU.d("Width= "+  width+ " Height= "+ height);
            format.setInteger(MediaFormat.KEY_WIDTH, width);
            format.setInteger(MediaFormat.KEY_HEIGHT, height);
            format.setLong(MediaFormat.KEY_DURATION, duration*1000);
            format.setInteger(MediaFormat.KEY_TRACK_ID, (int)tkhd.getTrackId());
        }

        MediaBox mdia = mTrackBox.getMediaBox(); // mdia
        if(mdia != null) {
            MediaHeaderBox mdhd = mdia.getMediaHeaderBox(); // mdhd
            if (mdhd != null) {
                // this will be for either video or audio depending media info
                mTimeScale = mdhd.getTimescale();
                mDurationUs = mdhd.getDuration()* 1000000 /mdhd.getTimescale() ;
                LogU.d("Time scale = "+ mTimeScale + " mDurationUs "+ mDurationUs);
                format.setLong(MediaFormat.KEY_DURATION, mDurationUs);
            }

            SampleTableBox stbl = mTrackBox.getSampleTableBox(); // mdia/minf/stbl
            if (stbl != null) {
                SampleDescriptionBox stsd = stbl.getSampleDescriptionBox(); // mdia/minf/stbl/stsd
                if(stsd != null) {
                    SampleEntry entry = stsd.getSampleEntry();
                    if (entry != null) {
                        String codecName = entry.getType();
                        CodecConfig codecConfig = null;
                        LogU.d(" codecName= "+ codecName);
                        switch (codecName) {
                            case VisualSampleEntry.TYPE3: //avc1
                            case VisualSampleEntry.TYPE4: //avc3
                                codecConfig = new AvcCodecConfig(entry);
                                break;
                            case VisualSampleEntry.TYPE6:  //hvc1
                            case VisualSampleEntry.TYPE7:  //hev1
                                codecConfig = new HevcCodecConfig(entry);
                                break;
                            case "sowt": //pcm
                                codecConfig = new PCMConfig(entry);
                                break;
                            case AudioSampleEntry.TYPE3: //mp4a

                                break;
                        }
                        if(codecConfig != null) {
                            codecConfig.config(format);
                        } else {
                            LogU.e(" codec type has not support!");
                        }

                    }
                }

                mSTBLBoxParser = new STBLBoxParser(stbl);

                HandlerBox hdlr = mdia.getHandlerBox(); // mdia/hdlr
                if (hdlr != null) {
                    String type = hdlr.getHandlerType();
                    if("vide".equals(type)) {
                        int frameRate = (int)(mSTBLBoxParser.getSampleCount()*1000000/mDurationUs);
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
                    }
                }
            }
        }

        return format;
    }

    public void prepareFramesInfo() {
        if(mSTBLBoxParser != null) {
            frames.clear();
            for (int i = 1; i<mSTBLBoxParser.getChunkCount()+1; i++) {
                //create a frame
                MP4Frame frame = new MP4Frame();
                frame.setKeyFrame(mSTBLBoxParser.isKeyFrame(i));
                frame.setOffset(mSTBLBoxParser.getChunkOffset(i));
                frame.setSize((int) mSTBLBoxParser.getChunkSize(i));
                frame.setTime(getTimestamp(i-1));
                if(getFormat().getString(MediaFormat.KEY_MIME).startsWith("video")) {
                    frame.setType(IoConstants.TYPE_VIDEO);
                } else {
                    frame.setType(IoConstants.TYPE_AUDIO);
                }
                frames.add(frame);
//                LogU.d("prepareFramesInfo i="+i+" frame  "+ frame);
            }
        }
    }

    public Tag readTag() {
        LogU.d("readTag currentFrame "+ currentFrame + " track-id " +mTrackId+" frames.size() "+ frames.size());
        Tag tag = null;
        if(currentFrame >= frames.size()) {
            return tag;
        }

        try {
            lock.acquire();

            //get the current frame
            MP4Frame frame = frames.get(currentFrame);
            if (frame != null) {
//                LogU.d("currentFrame= " + currentFrame + " frame "+  frame);
                int sampleSize = frame.getSize();
                int time = (int) Math.round(frame.getTime() * 1000.0);
                //Log.d(TAG,"Read tag - dst: {} base: {} time: " + new Object[]{frameTs, baseTs, time});
                long samplePos = frame.getOffset();
                //Log.d(TAG,"Read tag - samplePos " + samplePos);
                //determine frame type and packet body padding
                byte type = frame.getType();

                //create a byte buffer of the size of the sample
                ByteBuffer data = ByteBuffer.allocate(sampleSize);
                try {
                    //prefix is different for keyframes

                    // do we need to add the mdat offset to the sample position?
                    dataSource.position(samplePos);
                    // read from the channel
                    dataSource.read(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // chunk the data
                ByteBuffer payload = ByteBuffer.wrap(data.array());
                // create the tag
                tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
//                LogU.d("Read tag  body size:"+ tag.getBodySize() );
//                for(int i=0; i< 10; i++) {
//                    LogU.d(" i= "+i + " "+ data.get(i));
//                }
                // increment the frame number
//                currentFrame++;
                // set the frame / tag size
                prevFrameSize = tag.getBodySize();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.release();
        }
        LogU.d("read tag currentFrame "+ currentFrame + " finish frame size " + tag.getBodySize());
        return tag;
    }

    public boolean advance() {
//        LogU.d(" advance 111 "+ currentFrame);
        currentFrame ++;
        if(currentFrame >= frames.size()) {
            return false;
        }
//        LogU.d(" advance 222 "+ currentFrame);
        return true;
    }

    public long getDurationUs() {
        return mDurationUs;
    }

    /**
     *
     * @return timestamp us
     */
    private long getTimestamp(int index) {
        long timestamp = 0;
        if(mSTBLBoxParser != null) {
            int sampleIndex = mSTBLBoxParser.getSampleIndexFromChunk(index+1);
            timestamp = sampleIndex*mSTBLBoxParser.getFrameDelta()*1000000/mTimeScale;
//            LogU.d(" getTimestamp index= "+ (index+1) + " sampleIndex= "+sampleIndex + " timestamp="+ timestamp);
        }
        return timestamp;
    }

    public long getTimestamp() {
        return getTimestamp(currentFrame);
    }

    public boolean seekTo(long timeUs) {
        int frameIndex = 0;
        if(mSTBLBoxParser != null) {
            frameIndex = (int)(timeUs * mTimeScale/mSTBLBoxParser.getFrameDelta()/10000);
            frameIndex = mSTBLBoxParser.getPreKeyFrameIndex(frameIndex);
        }
        currentFrame = frameIndex;
        return true;
    }

}
