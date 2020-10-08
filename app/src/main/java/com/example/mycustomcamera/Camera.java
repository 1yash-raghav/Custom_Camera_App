package com.example.mycustomcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.Image;
import android.media.ImageReader;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Camera#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Camera extends Fragment implements SurfaceHolder.Callback, Handler.Callback {

    private final static int CAMERA_PERMISSION_CODE = 0;
    private final static int MSG_SURFACE_CREATED = 0;
    private final static int MSG_CAMERA_OPENED = 1;
    private static String CAMERA_ID = "0";
    private static CameraCaptureSession mCameraCaptureSession;
    CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.d("***********************", "onCaptureStarted");
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d("***********************", "onCaptureProgressed");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d("***********************", "onCaptureCompleted");
        }
    };
    boolean status = false;
    private Handler mHandler = new Handler(this);
    private Surface mCameraSurface;
    private boolean mIsCameraSurfaceCreated;
    private CameraDevice mCameraDevice;
    private ImageReader mCaptureImageReader = null;
    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d("***********************", "onConfigured");
            mCameraCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // try with different parameters
                previewRequestBuilder.addTarget(mCameraSurface);
                mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                Log.d("***********************", "5 setRepeatingRequest");
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d("***********************", "onConfigureFailed");
        }
    };

    public Camera() {
        // Required empty public constructor
    }

    public static Camera newInstance() {
        Camera fragment = new Camera();
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
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        SurfaceView mSurfaceView = rootView.findViewById(R.id.camSurfaceView);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        if (mCaptureImageReader == null) {
            mCaptureImageReader = ImageReader.newInstance(500, 500, ImageFormat.JPEG, 2);
            mCaptureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.d("***********************", "OnImageAvailable");
                    saveImage(reader);
                }
            }, mHandler);
        }

        FloatingActionButton mButton = rootView.findViewById(R.id.camButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCaptureImage();
            }
        });
        FloatingActionButton mFlashButton = rootView.findViewById(R.id.camflash);
        mFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status = !status;
                Log.d("*************FLASH***", ""+status);
                switchFlashLight();
            }
        });

        return rootView;
    }

    public void switchFlashLight() {
        CameraManager mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            mCameraManager.setTorchMode(mCameraManager.getCameraIdList()[0], status);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ShowToast")
    private void saveImage(ImageReader reader) {
        android.util.Log.e("************************************", "SAVE IMAGE");
        Image image = reader.acquireLatestImage();
        byte[] bytes = getJpegData(image); // step 8: get the jpeg data
        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix m = new Matrix();
        CameraManager cameraManager = (CameraManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            assert cameraManager != null;
            characteristics = cameraManager.getCameraCharacteristics(CAMERA_ID);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        assert characteristics != null;
        Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        m.setRotate((float) orientation, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), m, true);

        File f = getOutputFile();
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(f);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            Toast.makeText(getActivity(), "SAVED!!", Toast.LENGTH_LONG);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert fos != null;
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] getJpegData(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return byteArray;
    }

    private File getOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory().toString(), "MyPictures");
        if (!dir.exists()) {
            dir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir.getPath() + File.separator + "PIC_" + timeStamp + ".jpg");
    }

    private void handleCaptureImage() {
        if (mCameraDevice != null) {
            if (mCameraCaptureSession != null) {
                try {
                    CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequestBuilder.addTarget(mCaptureImageReader.getSurface());
                    mCameraCaptureSession.capture(captureRequestBuilder.build(), mCaptureCallback, mHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        android.util.Log.e("***********************", "msg.what=" + msg.what);
        android.util.Log.e("***********************", "mIsCameraSurfaceCreated=" + mIsCameraSurfaceCreated);
        android.util.Log.e("***********************", "mCameraDevice=" + mCameraDevice);
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
        Log.d("***********************", "4 configureCamera");

        List<OutputConfiguration> outputConfigurationList = new ArrayList<>();
        OutputConfiguration previewStream = new OutputConfiguration(mCameraSurface);
        OutputConfiguration captureStream = new OutputConfiguration(mCaptureImageReader.getSurface());
        outputConfigurationList.add(previewStream);
        outputConfigurationList.add(captureStream);

        try {
            mCameraDevice.createCaptureSessionByOutputConfigurations(outputConfigurationList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleCamera();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void handleCamera() {
        Log.d("***********************", "3 Handle Camera");
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        CameraManager mCameraManager = (CameraManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);
        CameraDevice.StateCallback mCameraStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d("***********************", "4 onOpened -" + camera.getId());
                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d("***********************", "onDisconnected -" + camera.getId());
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d("***********************", "onDisconnected -" + camera.getId());
            }
        };

        try {
            assert mCameraManager != null;
            mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCameraCaptureSession.close();
        mCameraDevice.close();
        mCameraCaptureSession = null;
        mCameraDevice = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("***********************", "SurfaceDestroyed");
        mIsCameraSurfaceCreated = false;
    }

}