package com.example.mp4extractor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.mp4extractor.mp4.Mp4Test;

public class MainActivity extends AppCompatActivity {
    String path = "/sdcard/Test/4kx4kx2streams_pcm.MP4";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.verifyStoragePermissions(this);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.btn_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Mp4Test().testMp4Extractor(path);
            }
        });
    }
}
