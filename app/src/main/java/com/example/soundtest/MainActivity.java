package com.example.soundtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.soundtest.Audio.AudioDeviceDescriptor;
import com.example.soundtest.Audio.AudioHelper;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private AudioHelper mAudioHelper;
    private RadioGroup rgIn, rgOut;
    private Context mContext;
    private SwitchDeviceListener mSwitchDeviceListener;
    private Button btnStart, btnStop;
    private RunTimeReceiver mReceiver;
    private final static int REQUEST_CAMERA_PERMISSION = 51;
    private String[] ids;
    private boolean ringtonPlaying = false;
    private MediaPlayer mMp;

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
            btnStart.setEnabled(!btnStart.isEnabled());
            btnStop.setEnabled(!btnStop.isEnabled());
            AudioHelper.getInstance().startPlay();
        }

    }
    public void onClickStop(View view) {
        btnStart.setEnabled(!btnStart.isEnabled());
        btnStop.setEnabled(!btnStop.isEnabled());
        AudioHelper.getInstance().stopPlay();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mReceiver = new RunTimeReceiver();
        mReceiver.register(this);
        checkRecordPermission();
        AudioHelper.getInstance().register(mContext);
        mAudioHelper = AudioHelper.getInstance();
        setContentView(R.layout.activity_main);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnStop.setEnabled(!btnStop.isEnabled());

        rgIn = findViewById(R.id.rg_in);
        rgOut = findViewById(R.id.rg_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUI(false);
        mSwitchDeviceListener = AudioHelper.getInstance();
        rgIn.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rgOut.setOnCheckedChangeListener(mOnCheckedChangeListener);
        if(ContextCompat.checkSelfPermission(App.getAppContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            getCamerasList();
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCamerasList();
            }
        }
    }

    private void getCamerasList() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            ids = manager.getCameraIdList();
            for (String id : ids){
                CameraCharacteristics info = manager.getCameraCharacteristics(id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void initUI(boolean clear) {
        if(clear){
           rgIn.removeAllViews();
           rgOut.removeAllViews();
        }
        mAudioHelper.createAudioDeviceList(getApplicationContext());
        ArrayList<AudioDeviceDescriptor> deviceIn = mAudioHelper.getDeviceIn();
        ArrayList<AudioDeviceDescriptor> deviceOut = mAudioHelper.getDeviceOut();
        if(deviceIn != null && deviceIn.size() > 0){
            fillRadioGroup(deviceIn, rgIn, true);
        }
        if(deviceOut != null && deviceOut.size() > 0){
            fillRadioGroup(deviceOut, rgOut, true);
        }
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
        mReceiver.unregister();
        super.onDestroy();
    }



    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
    }

    private void fillRadioGroup(ArrayList<AudioDeviceDescriptor> devices, RadioGroup rg, boolean isFirstTime){
        int i = 0;
        for(AudioDeviceDescriptor device : devices){
            if(device.isBadParams())
                return;
            final RadioButton rb = new RadioButton(this);
            String name = device.getName();
            rb.setText(device.getName());
            rb.setId(i);
            rg.addView(rb, i);
            if(i == 0 && isFirstTime){
                rg.check(i);
            }
            i++;
        }
    }

    public void onClickPlayRington(View view) {
        if(mMp == null){
            mMp = new MediaPlayer();
        }
        if(!ringtonPlaying){
            mMp.reset();
            Uri rington = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            final Resources res = this.getResources();
            final AssetFileDescriptor afd = res
                    .openRawResourceFd(R.raw.ring);
            try {
                mMp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                        afd.getLength());
                final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

                if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                    mMp.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mMp.setLooping(true);
                    mMp.prepare();
                    mMp.start();
                }
                //mMp.start();
                ringtonPlaying = true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            mMp.stop();
            mMp.release();
            mMp = null;
            ringtonPlaying = false;
        }


    }

    public interface SwitchDeviceListener{
        void onDeviceSwitched(AudioDeviceDescriptor audioDeviceDescriptor);
    }

}
