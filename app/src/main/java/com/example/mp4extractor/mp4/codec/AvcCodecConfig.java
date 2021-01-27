package com.example.mp4extractor.mp4.codec;

import android.media.MediaFormat;

import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.mp4parser.iso14496.part15.AvcConfigurationBox;

import java.nio.ByteBuffer;

/**
 * Description:
 * Created by liyang on 2019/11/25.
 */
public class AvcCodecConfig extends CodecConfig {
    AvcConfigurationBox avc1;
    ByteBuffer csd_0;
    ByteBuffer csd_1;
    byte CODEC_PREFIX[] = new byte[]{0x0, 0x0, 0x0, 0x1};

    public AvcCodecConfig(SampleEntry entry) {
        super(entry);
        avc1 = entry.getBoxes(AvcConfigurationBox.class).get(0);
    }

    @Override
    public void config(MediaFormat format) {
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
        int csd0_size = avc1.getSequenceParameterSets().get(0).length;
        csd_0 = ByteBuffer.allocate(csd0_size+4);
        csd_0.put(CODEC_PREFIX);
        csd_0.put(avc1.getSequenceParameterSets().get(0));
        csd_0.position(0);
        int csd1_size = avc1.getPictureParameterSets().get(0).length;
        csd_1 = ByteBuffer.allocate(csd1_size+4);
        csd_1.put(CODEC_PREFIX);
        csd_1.put(avc1.getPictureParameterSets().get(0));
        csd_1.position(0);
        format.setByteBuffer("csd-0", csd_0);
        format.setByteBuffer("csd-1", csd_1);

    }
}
