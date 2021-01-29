package com.example.mp4extractor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.mp4extractor.mp4.Mp4Test;
import com.example.mp4extractor.util.PermissionUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    String path = "/sdcard/testvideo_mono.mp4";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.verifyStoragePermissions(this);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.btn_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                new Mp4Test().testMp4Extractor(path);
                startActivity(new Intent(MainActivity.this, PlayActivity.class));
//                saveBitmap();
            }
        });
    }

    private void saveBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        ByteBuffer byteBuffer = ByteBuffer.allocate(512*512*4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(0);
        for (int i=0; i<512; i++) {
            for (int j=0; j< 512; j++) {
                byteBuffer.put((byte) 0x30);
                byteBuffer.put((byte) 0x40);
                byteBuffer.put((byte) 0x50);
                byteBuffer.put((byte) 0xFF);

            }
        }
        byteBuffer.position(0);

        bitmap.copyPixelsFromBuffer(byteBuffer);
        Log.d("", "finish ");
    }
}
