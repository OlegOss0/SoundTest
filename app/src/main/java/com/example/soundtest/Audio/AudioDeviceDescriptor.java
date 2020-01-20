package com.example.soundtest.Audio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import com.example.soundtest.App;

import java.lang.reflect.Method;
import java.util.UUID;

public class AudioDeviceDescriptor {


    public int getBuffSize() {
        int buff = 0;
        if(isBluetooth){

        }else{
            int[] rates = mAudioDeviceInfo.getSampleRates();
            int[] channels = mAudioDeviceInfo.getChannelMasks();
            int[] channelsCount = mAudioDeviceInfo.getChannelCounts();
            int[] encoding = mAudioDeviceInfo.getEncodings();
            this.currRate = rates[rates.length - 1];
            buff = AudioTrack.getMinBufferSize(currRate, (mAudioDeviceInfo.getChannelMasks())[0], (mAudioDeviceInfo.getEncodings())[0]);

        }
        return buff;
    }

    private boolean isValid(){
        boolean b = mAudioDeviceInfo.getSampleRates().length > 0;
        return b;
    }

    public AudioAttributes getAudioAttributes(){
        return  new AudioAttributes.Builder() .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
    }

    public AudioFormat getAudioFormat(){
        return new AudioFormat.Builder()
                .setChannelMask(mAudioDeviceInfo.getChannelMasks()[0])
                .setEncoding(mAudioDeviceInfo.getEncodings()[0])
                .setSampleRate(currRate)
                .build();
    }

    public enum DeviceType {isSink, isSource}

    private int currRate;
    private int sessionId = -1;
    private UUID uuid;
    private final String TAG = this.getClass().getSimpleName();
    private DeviceType deviceType;
    private int mType = -1;
    private int mId = -1;
    private String mTypeString = "";
    private String mName = "";
    private boolean isBadParams = false;
    private AudioDeviceInfo mAudioDeviceInfo;
    private boolean isBluetooth = false;
    private String mAddress = "";
    private BluetoothDevice bluetoothDevice;

    public AudioDeviceDescriptor(final AudioDeviceInfo audioDeviceInfo) {
        try {
            this.mAudioDeviceInfo = audioDeviceInfo;
            this.mType = audioDeviceInfo.getType();
            this.isBluetooth = mType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || mType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
            if(!isValid()){
                isBadParams = true;
                return;
            }
            this.mAddress = getAddress(audioDeviceInfo);
            if (isBluetooth) {
                BluetoothAdapter.getDefaultAdapter().getProfileProxy(App.getAppContext(), new BluetoothProfile.ServiceListener() {
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        Log.i(TAG, "onServiceConnected");
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        Log.i(TAG, "onServiceDisconnected");
                    }
                }, audioDeviceInfo.getType());
                this.bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddress);
            }
            this.mId = audioDeviceInfo.getType();
            this.mName = audioDeviceInfo.getProductName().toString();
            this.deviceType = audioDeviceInfo.isSink() ? DeviceType.isSink : DeviceType.isSource;
        } catch (Exception e) {
            e.printStackTrace();
            isBadParams = true;
        }
    }

    private String getAddress(AudioDeviceInfo audioDeviceInfo) {
        String address = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            address = audioDeviceInfo.getAddress();
        } else {
            try {
                Method method = audioDeviceInfo.getClass().getMethod("getAddress");
                method.setAccessible(true);
                address = (String) method.invoke(audioDeviceInfo);
            } catch (Exception e) {

            }
        }
        return address;
    }

    public int getCurrRate(){
        return currRate;
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" ").append(getTypeString(mType));
        return sb.toString();
    }

    public int[] getSampleRates() {
        return mAudioDeviceInfo.getSampleRates();
    }

    public boolean isBadParams() {
        return isBadParams;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public AudioDeviceInfo getAudioDeviceInfo(){
        return mAudioDeviceInfo;
    }

    public static String getTypeString(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "AUX_LINE";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "BLUETOOTH_A2DP";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "BLUETOOTH_SCO";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "BUILTIN_EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "BUILTIN_MIC";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "BUILTIN_SPEAKER";
            case AudioDeviceInfo.TYPE_BUS:
                return "BUS";
            case AudioDeviceInfo.TYPE_DOCK:
                return "DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "FM";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "FM_TUNER";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "HDMI_ARC";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "HEARING_AID";
            case AudioDeviceInfo.TYPE_IP:
                return "IP";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "LINE_ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "LINE_DIGITAL";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "TELEPHONY";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TV_TUNER";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB ACCESSORY";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB_DEVICE";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "WIRED_HEADPHONES";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "WIRES_HEADSET";
            default:
                return "UNKNOWN";
        }
    }
}
