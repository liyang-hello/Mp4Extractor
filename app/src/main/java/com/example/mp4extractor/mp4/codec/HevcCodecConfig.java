package com.example.mp4extractor.mp4.codec;
import android.media.MediaFormat;

import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.mp4parser.iso14496.part15.HevcConfigurationBox;
import com.mp4parser.iso14496.part15.HevcDecoderConfigurationRecord;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Description:
 * Created by liyang on 2019/11/25.
 */
public class HevcCodecConfig extends CodecConfig {
    HevcConfigurationBox hevcBox = null;
    HevcDecoderConfigurationRecord hevcConfig = null;
    ByteBuffer csd_0;
    byte CODEC_PREFIX[] = new byte[]{0x0, 0x0, 0x0, 0x1};

    public HevcCodecConfig(SampleEntry entry) {
        super(entry);
        hevcBox = entry.getBoxes(HevcConfigurationBox.class).get(0);
        hevcConfig = hevcBox.getHevcDecoderConfigurationRecord();
    }

    @Override
    public void config(MediaFormat format) {
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_HEVC);
        List<HevcDecoderConfigurationRecord.Array> arrays = hevcBox.getArrays();
        ByteBuffer tempBuf = ByteBuffer.allocate(400);
        tempBuf.position(0);
        for (int i=0; i< arrays.size(); i++) {
            HevcDecoderConfigurationRecord.Array array = arrays.get(i);
//            LogU.d("array " + array);
            if(array.nal_unit_type < 35 && array.nal_unit_type >31) {
                tempBuf.put(CODEC_PREFIX);
                tempBuf.put(array.nalUnits.get(0),0, array.nalUnits.get(0).length);
            }

        }
        csd_0 = ByteBuffer.allocate(tempBuf.position());
        csd_0.put(tempBuf.array(), 0, tempBuf.position());
        csd_0.flip();
        format.setByteBuffer("csd-0", csd_0);

    }
}
