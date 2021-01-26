package com.example.mp4extractor;

import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mp4extractor.util.GLUtil;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Li Yang on 2021/1/25.
 */
public class PlayActivity extends AppCompatActivity {

    private GLSurfaceView mSurfaceView = null;
    private SphereShape mShape = null;
    private MediaPlayer mMediaPlayer = null;
    private Surface mSurface = null;
    private SurfaceTexture mSurfaceTexture = null;

    private boolean mFrameAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        mSurfaceView = findViewById(R.id.gl_surfaceview);
        setupGLSurfaceView();
    }

    private void setupGLSurfaceView() {
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 8, 8);
        mSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                mShape = new SphereShape();
                int textureId = GLUtil.generateOESTexture();
                mSurfaceTexture = new SurfaceTexture(textureId);
                mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mFrameAvailable = true;
                    }
                });
                mSurface = new Surface(mSurfaceTexture);
                mShape.setTextureId(textureId);
                setupPlayer();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                mShape.setSurfaceSize(i, i1);
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                if (mFrameAvailable) {
                    mSurfaceTexture.updateTexImage();
                    mFrameAvailable = false;
                }
                mShape.draw();
            }
        });
    }

    static final float PX_PER_DEGREES = 25;
    static final float MAX_PITCH_DEGREES = 90;
    private final PointF previousTouchPointPx = new PointF();
    private final PointF accumulatedTouchOffsetDegrees = new PointF();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Initialize drag gesture.
                previousTouchPointPx.set(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                float touchX = (event.getX() - previousTouchPointPx.x) / PX_PER_DEGREES;
                float touchY = (event.getY() - previousTouchPointPx.y) / PX_PER_DEGREES;
                previousTouchPointPx.set(event.getX(), event.getY());

                accumulatedTouchOffsetDegrees.x -= touchX;
                accumulatedTouchOffsetDegrees.y += touchY;
                accumulatedTouchOffsetDegrees.y = Math.max(-MAX_PITCH_DEGREES, Math.min(MAX_PITCH_DEGREES, accumulatedTouchOffsetDegrees.y));

                mShape.setPitchOffset(accumulatedTouchOffsetDegrees.y);
                mShape.setYawOffset(accumulatedTouchOffsetDegrees.x);
                return true;
            default:
                return false;
        }
    }

    private void setupPlayer() {
        try {
            mMediaPlayer = MediaPlayer.create(this, R.raw.testvideo_mono);
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mMediaPlayer.start();
            }
        });
    }

    String videoPath = "";
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setupMp4Player() {
        VideoDecoder decoder = new VideoDecoder(mSurface);
        decoder.startDecoder(videoPath);
    }
}
