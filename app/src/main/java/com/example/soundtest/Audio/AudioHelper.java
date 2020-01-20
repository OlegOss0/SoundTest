package com.example.soundtest.Audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.soundtest.App;
import com.example.soundtest.MainActivity;

import java.util.ArrayList;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioTrack.MODE_STREAM;


public class AudioHelper implements MainActivity.SwitchDeviceListener {

    private final String TAG = this.getClass().getSimpleName();
    private static AudioHelper INSTANCE;
    private static final Object lock = new Object();
    private ArrayList<AudioDeviceDescriptor> outDevices = new ArrayList<>();
    private ArrayList<AudioDeviceDescriptor> inDevices = new ArrayList<>();
    private HandlerThread audioThread;
    private Handler handler;
    private AudioManager mAudioManager;
    private AudioDeviceCallback audioDeviceCallback;
    private AudioDeviceDescriptor currentIn = null;
    private AudioDeviceDescriptor curentOut = null;
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;

    private enum STATE {IDLE, RUNNING}

    ;
    private STATE mState = STATE.IDLE;


    private AudioHelper() {
        audioThread = new HandlerThread("Audio Thread");
        audioThread.start();
        handler = new Handler(audioThread.getLooper());
    }

    public static AudioHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                INSTANCE = new AudioHelper();
            }
        }
        return INSTANCE;
    }

    public void register(final Context mContext) {
        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                for (AudioDeviceInfo adI : addedDevices) {
                    Log.e(TAG, "onAudioDevicesAdded = " + new AudioDeviceDescriptor(adI).getName());
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                for (AudioDeviceInfo adI : removedDevices) {
                    Log.e(TAG, "onAudioDevicesRemoved = " + new AudioDeviceDescriptor(adI).getName());
                }
            }
        };
        mAudioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
    }


    public void createAudioDeviceList(Context mContext) {
        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] allDevicesList = mAudioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        if (allDevicesList == null) {
            return;
        }
        for (AudioDeviceInfo ad : allDevicesList) {
            if (ad.isSource()) {
                inDevices.add(new AudioDeviceDescriptor(ad));
            } else {
                outDevices.add(new AudioDeviceDescriptor(ad));
            }
        }
        currentIn = inDevices.get(0);
        curentOut = outDevices.get(0);
    }

    public ArrayList<AudioDeviceDescriptor> getDeviceIn() {
        return inDevices;
    }

    public ArrayList<AudioDeviceDescriptor> getDeviceOut() {
        return outDevices;
    }

    @Override
    public void onDeviceSwitched(AudioDeviceDescriptor audioDeviceDescriptor) {
        if (!audioDeviceDescriptor.isBadParams()) {
            if (audioDeviceDescriptor.getDeviceType() == AudioDeviceDescriptor.DeviceType.isSink) {
                Log.i(TAG, "onDeviceSwitched, " + (curentOut != null ? curentOut.getName() : "Empty")
                        + " -> " + audioDeviceDescriptor.getName());
                curentOut = audioDeviceDescriptor;
            } else if (audioDeviceDescriptor.getDeviceType() == AudioDeviceDescriptor.DeviceType.isSource) {
                Log.i(TAG, "onDeviceSwitched, " + (currentIn != null ? currentIn.getName() : "Empty")
                        + " -> " + audioDeviceDescriptor.getName());
                currentIn = audioDeviceDescriptor;
            }
        }
    }

    public AudioDeviceDescriptor getCurrentIn() {
        return currentIn;
    }

    public AudioDeviceDescriptor getCurentOut() {
        return curentOut;
    }

    public void unregister(final Context mContext) {
        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
    }

    public void destroy() {
        synchronized (lock) {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }

            if (audioThread != null) {
                synchronized (lock) {
                    audioThread.quitSafely();
                    audioThread.quit();
                    audioThread = null;
                }
            }
            INSTANCE = null;
        }
    }

    public void startPlay() {
        AudioManager mAudioManager = (AudioManager) App.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        final int minBuff = curentOut.getBuffSize();
        AudioAttributes audioAttributes = curentOut.getAudioAttributes();
        AudioFormat format = curentOut.getAudioFormat();
        mAudioTrack = new AudioTrack(audioAttributes, format, minBuff, MODE_STREAM, mAudioManager.generateAudioSessionId());

        int resource = MediaRecorder.AudioSource.MIC;
        mAudioRecord = new AudioRecord(resource, currentIn.getCurrRate(), 12, 2, minBuff);
        mAudioRecord.setPreferredDevice(curentOut.getAudioDeviceInfo());
        mAudioRecord.startRecording();
        mState = STATE.RUNNING.RUNNING;
        handler.post(() ->{
            while (mState == STATE.RUNNING) {
                byte[] buff = new byte[minBuff];
                int result = mAudioRecord.read(buff, 0, minBuff);
                if (result > 0) {
                    handler.postDelayed(() -> {
                        mAudioTrack.write(buff, 0, buff.length);
                        mAudioTrack.play();
                    }, 1);
                }
            }
            try {
                Thread.currentThread().sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void stopPlay() {
        mState = STATE.IDLE;
        handler.removeCallbacksAndMessages(null);
        mAudioRecord.release();
        mAudioTrack.release();
        mAudioRecord = null;
        mAudioTrack = null;
    }
}
