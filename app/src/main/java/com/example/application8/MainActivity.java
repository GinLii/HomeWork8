package com.example.application8;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.example.application8.PathUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private ImageView mImageView;
    private VideoView mVideoView;
    private Button btn1;
    private Button btn2;
    private Camera.PictureCallback mPictureCallback;
    private MediaRecorder mMediaRecorder;
    private String mp4Path;
    private boolean isRecording;

    private static String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
    private static int PERMISSION_CODE = 1;

    private void requestForPermission(){
        if(PackageManager.PERMISSION_GRANTED != getPackageManager().checkPermission(permissions[0], getPackageName())
                || PackageManager.PERMISSION_GRANTED != getPackageManager().checkPermission(permissions[1], getPackageName()))
            ActivityCompat.requestPermissions(this,permissions,PERMISSION_CODE);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceView);
        mImageView = findViewById(R.id.imageView);
        mVideoView = findViewById(R.id.videoView);
        btn1 = findViewById(R.id.buttonCamera);
        btn2 = findViewById(R.id.buttonVideo);
        requestForPermission();
        initCamera();
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new PlayerCallBack());
        mPictureCallback= new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] data, Camera camera){
                FileOutputStream fos = null;
                String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + "1.jpg";
                File file = new File(filePath);
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    Bitmap rotateBitmap = PathUtils.rotateImage(bitmap,filePath);
                    mImageView.setVisibility(View.VISIBLE);
                    mVideoView.setVisibility(View.GONE);
                    mImageView.setImageBitmap(rotateBitmap);
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    mCamera.startPreview();
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null,null,mPictureCallback);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record();
            }
        });
    }

    private void initCamera(){
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
        parameters.set("orientation","portrait");
        parameters.set("rotation",90);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
    }

    private class PlayerCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try{
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(holder.getSurface() == null){
                return;
            }
            //停止预览效果
            mCamera.stopPreview();
            //重新设置预览效果
            try{
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mCamera == null){
            initCamera();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mCamera.stopPreview();
    }

    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mp4Path = getOutputMediaPath();
        mMediaRecorder.setOutputFile(mp4Path);
        mMediaRecorder.setOrientationHint(90);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            mMediaRecorder.release();
            return false;
        }
        return true;
    }

    public void record(){
        if(isRecording){
            btn2.setText("录制");
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();

            mImageView.setVisibility(View.INVISIBLE);
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setVideoPath(mp4Path);
            mVideoView.start();
        }else{
            if(prepareVideoRecorder()){
                btn2.setText("暂停");
                mMediaRecorder.start();
            }
        }
        isRecording = !isRecording;
    }

    public String getOutputMediaPath(){
        File mediaDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaDir,"IMG_" + timeStamp + ".mp4");
        if(!mediaFile.exists()){
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }
}