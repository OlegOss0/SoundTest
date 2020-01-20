package com.example.soundtest;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;

import static android.media.AudioFormat.CHANNEL_IN_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioTrack.MODE_STREAM;

class AudioDeviceList {
    private static AudioDeviceList INSTANCE;
    private AudioRecord mAudioRecord;
    private MediaPlayer mAudioPlayer;
    // private AudioManager mAudioManager;
    private ArrayList<AudioDeviceInfo> outDevList = new ArrayList<>();
    private ArrayList<AudioDeviceInfo> inDevList = new ArrayList<>();
    private HandlerThread audioThread;
    private Handler audioHandler;
    private AudioManager mAudioManager;
    private AudioTrack mAudioTrack;
    private final int mySession = 1;

    private enum state {IDLE, RUN}

    ;
    private static AudioDeviceList.state CURRENT_STATE = state.IDLE;


    private AudioDeviceList() {
        audioThread = new HandlerThread("Audio Thread");
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper());
    }

    public static AudioDeviceList getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new AudioDeviceList();
        }
        return INSTANCE;
    }

    public void createAudioDeviceList(final Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audioDeviceInfos = mAudioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo dev : audioDeviceInfos) {
            if (dev.isSink()) {
                outDevList.add(dev);
            } else if (dev.isSource()) {
                inDevList.add(dev);
            }
        }
    }

    public ArrayList<AudioDeviceInfo> getOut() {
        return outDevList;
    }


    public ArrayList<AudioDeviceInfo> getIn() {
        return inDevList;
    }

    public void startPlaying() {
        final int FORMAT = ENCODING_PCM_16BIT;
        final int MONO = AudioFormat.CHANNEL_IN_MONO;
        AudioDeviceInfo audioDeviceInfoMIC = inDevList.get(0);
        AudioDeviceInfo audioDeviceInfoOut = outDevList.get(2);

        int[] out_rates = audioDeviceInfoOut.getSampleRates();
        int[] outputChannelConfigArr = audioDeviceInfoOut.getChannelMasks();
        int[] outputEncodingArr = audioDeviceInfoOut.getEncodings();

        int outBuff = -1;
        for (int rate : out_rates){
            outBuff = AudioTrack.getMinBufferSize(out_rates[0], CHANNEL_IN_STEREO, outputEncodingArr[0]);
            if (outBuff > 0) break;
        }
        AudioAttributes audioAttributes = new AudioAttributes.Builder() .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setChannelMask(outputChannelConfigArr[0])
                .setEncoding(outputEncodingArr[0])
                .setSampleRate(out_rates[0])
                .build();
        mAudioTrack = new AudioTrack(audioAttributes, format, outBuff, MODE_STREAM, mySession);
        mAudioTrack.play();


        int type = audioDeviceInfoMIC.getType();
        int resource = MediaRecorder.AudioSource.MIC;
        int[] rates = audioDeviceInfoMIC.getSampleRates();
        int[] chanals = audioDeviceInfoMIC.getChannelCounts();
        int[] enc = audioDeviceInfoMIC.getEncodings();
        int minBuf = AudioRecord.getMinBufferSize(rates[0], AudioFormat.CHANNEL_IN_MONO, FORMAT);
        mAudioRecord = new AudioRecord(resource, rates[0], MONO, FORMAT, minBuf);
        mAudioRecord.setPreferredDevice(outDevList.get(2));
        mAudioRecord.startRecording();
        CURRENT_STATE = state.RUN;
        new Thread(() -> {
            while (CURRENT_STATE == state.RUN) {
                byte[] buff = new byte[minBuf];
                int result = mAudioRecord.read(buff, 0, minBuf);
                if (result > 0) {
                    mAudioTrack.write(buff, 0, buff.length);
                    mAudioTrack.play();
                }
            }
            try {
                Thread.currentThread().sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

    }

    public void stopPlaying() {

    }

}
