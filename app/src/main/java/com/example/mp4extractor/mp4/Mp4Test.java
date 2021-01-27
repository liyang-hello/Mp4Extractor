package com.example.mp4extractor.mp4;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.example.mp4extractor.util.LogU;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Mp4Test {

    public void testMp4Extractor(final String path) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(path);
            for(int i=0; i<mediaExtractor.getTrackCount(); i++) {
                mediaExtractor.selectTrack(i);
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                LogU.d("mediaFormat "+ mediaFormat);
                ByteBuffer csd_0 = mediaFormat.getByteBuffer("csd-0");
                if(csd_0 != null) {
                    String buf = bytesToHex(csd_0.array(), csd_0.limit());
                    LogU.d("track id csd_0 "+ i+ " buf "+ buf);
                }
                ByteBuffer csd_1 = mediaFormat.getByteBuffer("csd-1");
                if(csd_0 != null) {
                    String buf = bytesToHex(csd_1.array(), csd_1.limit());
                    LogU.d("track id csd_1 "+ i+ " buf "+ buf);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                IExtractor extractor = new Mp4Extractor();
                try {
                    extractor.setDataSource(path);

                    for (int i=0; i<extractor.getTrackCount(); i++) {
                        MediaFormat format = extractor.getTrackFormat(i);
                        LogU.d(" format "+ format);
                        ByteBuffer csd_0 = format.getByteBuffer("csd-0");
                        if(csd_0 != null) {
                            String buf = bytesToHex(csd_0.array(), csd_0.limit());
                            LogU.d("track id "+ i+ " buf "+ buf);
                        }

                        ByteBuffer csd_1 = format.getByteBuffer("csd-1");
                        if(csd_0 != null) {
                            String buf = bytesToHex(csd_1.array(), csd_1.limit());
                            LogU.d("track id csd_1 "+ i+ " buf "+ buf);
                        }

                        extractor.selectTrack(i);
                        extractor.seekTo(0,0);
                        ByteBuffer byteBuffer = ByteBuffer.allocate(4*1024*1024);
                        byteBuffer.position(0);
                        int readSize = 1;
                        String path = "/sdcard/Test/track_"+i;

                        out = null;
                        if(csd_0 != null) {
                            path = path+ ".265";
                            saveFile(csd_0.array(), csd_0.limit(), false, path);
                        } else {
                            path = path+".wav";
                        }

                        while (true) {
                            byteBuffer.position(0);
                            readSize = extractor.readSampleData(byteBuffer,0);
                            LogU.d("readSize "+ readSize);
                            if(readSize > 0) {
                                saveFile(byteBuffer.array(), readSize, false, path);
                            } else {
                                break;
                            }
                            extractor.advance();
                        }

                        saveFile(byteBuffer.array(), 0, true, path);
                        LogU.d("save "+path + " successfully");
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private OutputStream out = null;
            public void saveFile(byte[] byteBuffer, int size, boolean bEnd, String path){
                if(byteBuffer!=null){
                    if(out == null){
                        try {
                            out = new FileOutputStream(path);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        out.write(byteBuffer,0 ,size);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(bEnd){
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }).start();



    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int length) {
        char[] hexChars = new char[length * 2];
        for ( int j = 0; j < length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
//000000016742C028D90078079A10000003001000000303C0F1832480
//000000016742C028D90078079A10000003001000000303C0F1832480
//0000000168CB83CB20
//0000000168CB83CB20