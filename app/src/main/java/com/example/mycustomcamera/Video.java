package com.example.mycustomcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Video#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Video extends Fragment implements SurfaceHolder.Callback, Handler.Callback {
    private final static int CAMERA_PERMISSION_CODE = 0;
    private static String CAMERA_ID = "0";
    private final static int MSG_SURFACE_CREATED = 0;
    private final static int MSG_CAMERA_OPENED = 1;
    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";
    private Handler mHandler = new Handler(this);
    private Surface mCameraSurface;
    private boolean mIsCameraSurfaceCreated;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private boolean mIsRecordingVideo;
    private MediaRecorder mMediaRecorder;

    public Video() {
        // Required empty public constructor
    }

    public static Video newInstance() {
        Video fragment = new Video();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);

        SurfaceView mSurfaceView = rootView.findViewById(R.id.vidSurfaceView);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        FloatingActionButton mButton = rootView.findViewById(R.id.vidButton);
        mButton.setOnClickListener(new View.OnClickListener() { // step 1: define and display recording button and manage UI
            @Override
            public void onClick(View v) {
                if (!mIsRecordingVideo) {
                    startVideoRecording();
                } else {
                    stopVideoRecording();
                }
            }
        });

        return rootView;
    }

    private void stopVideoRecording() {
        Log.d("***********************", "stopVideoRecording");
        closeCameraSession();
        configureCamera();
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mIsRecordingVideo = false;
            }
        });
    }

    private void startVideoRecording() {
        Log.d("***********************", "handleVideoRecording");

        if (mCameraDevice != null) {

            closeCameraSession();

            List<Surface> surfaceList = new ArrayList<>();
            try {
                setupMediaRecorder();
            } catch (IOException e) {
                e.printStackTrace();
            }

            final CaptureRequest.Builder recordingBuilder;
            try {
                recordingBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                surfaceList.add(mCameraSurface);
                recordingBuilder.addTarget(mCameraSurface);

                surfaceList.add(mMediaRecorder.getSurface());
                recordingBuilder.addTarget(mMediaRecorder.getSurface());

                Log.d("***********************", "surfaces added");
                mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d("***********************", "recording configured");
                        mCameraCaptureSession = session;

                        try {
                            mCameraCaptureSession.setRepeatingRequest(recordingBuilder.build(), null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("***********************", "user entered recording UI");
                                mMediaRecorder.start();
                                mIsRecordingVideo = true;
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d("***********************", "Recording onConfigureFailed");
                    }
                }, mHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupMediaRecorder() throws IOException {
        Log.d("***********************", "setupMediaRecorder");
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        mMediaRecorder.setOutputFile(getOutputFile().getAbsolutePath());

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setOrientationHint(90);

        mMediaRecorder.prepare();
    }

    private File getOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory().toString(), "MyVideos");
        if (!dir.exists()) {
            dir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File imageFile = new File(dir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        Log.d("***********************", "imagefilename=" + imageFile.getAbsolutePath());
        return imageFile;
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("***********************", "1 surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("***********************", "2 surfaceChanged");
        mCameraSurface = holder.getSurface();
        mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);
        mIsCameraSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("***********************", "surfaceDestroyed");
        mIsCameraSurfaceCreated = false;
    }

    @SuppressLint("MissingPermission")
    private void handleCamera() {

        Log.d("****************************", "3 handle camera");

        CameraManager mCameraManager = (CameraManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                Log.e("******************************", "cameraId=" + cameraId);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        CameraDevice.StateCallback mCameraStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d("********************************", "4 onOpened -" + camera.getId());
                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d("********************************", "onDisconnected -" + camera.getId());

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d("********************************", "onDisconnected -" + camera.getId());
            }
        };

        try {
            mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
            mCameraManager.setTorchMode(CAMERA_ID, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        android.util.Log.e("****************************", "msg.what=" + msg.what);
        android.util.Log.e("****************************", "mIsCameraSurfaceCreated=" + mIsCameraSurfaceCreated);
        android.util.Log.e("****************************", "mCameraDevice=" + mCameraDevice);
        switch (msg.what) {
            case MSG_SURFACE_CREATED:
            case MSG_CAMERA_OPENED:
                if (mIsCameraSurfaceCreated && (mCameraDevice != null)) {
                    mIsCameraSurfaceCreated = false;
                    configureCamera();
                }
        }
        return true;
    }

    private void configureCamera() {
        Log.d("****************************", "4 configureCamera");

        List<Surface> surfaceList = new ArrayList<>();
        surfaceList.add(mCameraSurface); // surface to be viewed

        CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.d("***************************", "onConfigured");
                mCameraCaptureSession = session;

                try {
                    CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(mCameraSurface);

                    mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    Log.d("****************************", "5 setRepeatingRequest");
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d("***************************", "onConfigureFailed");
            }
        };

        try {
            mCameraDevice.createCaptureSession(surfaceList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length!=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleCamera();
            }
        }

    }

    private void closeCameraSession() {
        Log.d("****************************", "closeCameraSession");
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraCaptureSession.close();
            mCameraCaptureSession = null;

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        closeCameraSession();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCameraDevice.close();
        mCameraDevice = null;
    }
}