package com.example.mp4extractor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.mp4extractor.mp4.Mp4Test;
import com.example.mp4extractor.util.PermissionUtil;

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
            }
        });
    }
}
