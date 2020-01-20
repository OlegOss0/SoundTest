package com.example.soundtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.soundtest.Audio.AudioDeviceDescriptor;
import com.example.soundtest.Audio.AudioHelper;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private AudioHelper mAudioHelper;
    private RadioGroup rgIn, rgOut;
    private Context mContext;
    private SwitchDeviceListener mSwitchDeviceListener;

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int inId = rgIn.getId();
            int outId = rgOut.getId();
            if(mSwitchDeviceListener != null){
                if(radioGroup.getId() == inId){
                    mSwitchDeviceListener.onDeviceSwitched(AudioHelper.getInstance().getDeviceIn().get(i));
                }else if(radioGroup.getId() == outId){
                    mSwitchDeviceListener.onDeviceSwitched(AudioHelper.getInstance().getDeviceOut().get(i));
                }
            }
        }
    };

    public void onClickStart(View view) {
        if(AudioHelper.getInstance().getCurrentIn() != null && AudioHelper.getInstance().getCurentOut() != null){
            AudioHelper.getInstance().startPlay();
        }

    }
    public void onClickStop(View view) {
        AudioHelper.getInstance().stopPlay();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        checkRecordPermission();
        mAudioHelper = AudioHelper.getInstance();
        mAudioHelper.createAudioDeviceList(getApplicationContext());
        setContentView(R.layout.activity_main);

        rgIn = findViewById(R.id.rg_in);
        rgOut = findViewById(R.id.rg_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<AudioDeviceDescriptor> deviceIn = mAudioHelper.getDeviceIn();
        ArrayList<AudioDeviceDescriptor> deviceOut = mAudioHelper.getDeviceOut();
        if(deviceIn != null && deviceIn.size() > 0){
            fiilRadioGroup(deviceIn, rgIn, true);
        }
        if(deviceOut != null && deviceOut.size() > 0){
            fiilRadioGroup(deviceOut, rgOut, true);
        }
        AudioHelper.getInstance().register(mContext);
        mSwitchDeviceListener = AudioHelper.getInstance();

        rgIn.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rgOut.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }




    @Override
    protected void onPause() {
        mSwitchDeviceListener = null;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        AudioHelper.getInstance().unregister(mContext);
        AudioHelper.getInstance().destroy();
        super.onDestroy();
    }



    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
    }

    private void fiilRadioGroup(ArrayList<AudioDeviceDescriptor> devices, RadioGroup rg, boolean isFirstTime){
        int i = 0;
        for(AudioDeviceDescriptor device : devices){
            if(device.isBadParams())
                return;
            final RadioButton rb = new RadioButton(this);
            rb.setText(device.getName());
            rb.setId(i);
            rg.addView(rb, i);
            if(i == 0 && isFirstTime){
                rg.check(i);
            }
            i++;
        }
    }

    public interface SwitchDeviceListener{
        void onDeviceSwitched(AudioDeviceDescriptor audioDeviceDescriptor);
    }

}
