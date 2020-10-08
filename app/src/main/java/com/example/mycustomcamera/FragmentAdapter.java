package com.example.mycustomcamera;
import android.hardware.camera2.CameraCaptureSession;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class FragmentAdapter extends FragmentPagerAdapter {
    public int mode = 0;
    Fragment mCam;
    Fragment mVid;
    public FragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
        mCam = new Camera();
        mVid = new Video();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "Camera";
        else return "Video";
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if(position == 0)
            return mCam;
        else
            return mVid;
    }

    @Override
    public int getCount() {
        return 2;
    }

    public void changeMode(){
        if(mode==0){
            mode =1;
            mCam.onStop();
            mVid.onStart();
            Log.d("******************", "Video fragment now");
        }
        else{
            mode = 0;
            mVid.onStop();
            mCam.onStart();
            Log.d("*******************", "Camera fragment now");
        }
    }
}