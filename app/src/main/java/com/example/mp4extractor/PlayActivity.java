package com.example.mp4extractor;

import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mp4extractor.util.GLUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


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
        mSurfaceView.setEGLConfigChooser(8,8,8,8,8,8);
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
    // Touch input won't change the pitch beyond +/- 45 degrees. This reduces awkward situations
    // where the touch-based pitch and gyro-based pitch interact badly near the poles.
    static final float MAX_PITCH_DEGREES = 45;
    // With every touch event, update the accumulated degrees offset by the new pixel amount.
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
                // Calculate the touch delta in screen space.
                float touchX = (event.getX() - previousTouchPointPx.x) / PX_PER_DEGREES;
                float touchY = (event.getY() - previousTouchPointPx.y) / PX_PER_DEGREES;
                previousTouchPointPx.set(event.getX(), event.getY());

                float r = 0;  // Copy volatile state.
                float cr = (float) Math.cos(r);
                float sr = (float) Math.sin(r);
                // To convert from screen space to the 3D space, we need to adjust the drag vector based
                // on the roll of the phone. This is standard rotationMatrix(roll) * vector math but has
                // an inverted y-axis due to the screen-space coordinates vs GL coordinates.
                // Handle yaw.
                accumulatedTouchOffsetDegrees.x -= cr * touchX - sr * touchY;
                // Handle pitch and limit it to 45 degrees.
                accumulatedTouchOffsetDegrees.y += sr * touchX + cr * touchY;
                accumulatedTouchOffsetDegrees.y =
                        Math.max(-MAX_PITCH_DEGREES,
                                Math.min(MAX_PITCH_DEGREES, accumulatedTouchOffsetDegrees.y));

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
}
