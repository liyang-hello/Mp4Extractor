package com.example.mp4extractor.mp4.codec;
import android.media.MediaFormat;

import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;

/**
 * Description:
 * Created by liyang on 2019/11/25.
 */
public class PCMConfig extends CodecConfig {
    AudioSampleEntry mAudioSampleEntry;
    public PCMConfig(SampleEntry entry) {
        super(entry);
        mAudioSampleEntry = (AudioSampleEntry)entry;
    }

    @Override
    public void config(MediaFormat format) {
        int channelCount  = mAudioSampleEntry.getChannelCount();
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_RAW);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, (int)mAudioSampleEntry.getSampleRate());
//        super.config(format);
    }
}
