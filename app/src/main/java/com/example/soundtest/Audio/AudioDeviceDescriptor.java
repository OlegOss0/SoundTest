package com.example.soundtest.Audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.soundtest.App;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class AudioDeviceDescriptor {


    public int getEncoding(){
        return encoding;
    }

    public int getChannelConfig() {
        return curChannel;
    }

    public enum DeviceType {isSink, isSource}

    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int currRate;
    private int curChannel;
    private int channelsCount;
    private int encoding;
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
    private MyServiceListener mServiceListener;


    public AudioDeviceDescriptor(final AudioDeviceInfo audioDeviceInfo) {
        try {
            this.mAudioDeviceInfo = audioDeviceInfo;
            this.mType = audioDeviceInfo.getType();
            this.isBluetooth = mType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || mType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
            this.mAddress = getAddress(audioDeviceInfo);
            this.deviceType = audioDeviceInfo.isSink() ? DeviceType.isSink : DeviceType.isSource;

            if (isBluetooth) {
                check();
            }
            if(deviceType != DeviceType.isSink){
                if (!isBluetooth & !isValid()) {
                    isBadParams = true;
                    return;
                }
            }
            this.mId = audioDeviceInfo.getType();
            this.mName = audioDeviceInfo.getProductName().toString();
            this.deviceType = audioDeviceInfo.isSink() ? DeviceType.isSink : DeviceType.isSource;
        } catch (Exception e) {
            e.printStackTrace();
            isBadParams = true;
        }
    }
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    public void initParams(){
        int[] rates = mAudioDeviceInfo.getSampleRates();
        this.currRate = rates[rates.length - 1];
        int[] channels = mAudioDeviceInfo.getChannelMasks();
        this.curChannel = channels[channels.length -1];
        int[] channelsCount = mAudioDeviceInfo.getChannelCounts();
        this.channelsCount = channelsCount[channelsCount.length -1];
        int[] encoding = mAudioDeviceInfo.getEncodings();
        this.encoding =  encoding[encoding.length - 1];
    }


    public int getBuffSize() {
        int buff = 0;
        if (isBluetooth) {

        } else {
            if(deviceType == DeviceType.isSource){
                buff = AudioRecord.getMinBufferSize(this.currRate, curChannel, encoding);
            }else{
                buff = AudioTrack.getMinBufferSize(this.currRate, curChannel, encoding);
            }

        }
        return buff;
    }

    private boolean isValid() {
        boolean b = mAudioDeviceInfo.getSampleRates().length > 0 && mAudioDeviceInfo.getChannelMasks().length > 0
                && mAudioDeviceInfo.getChannelCounts().length > 0 && mAudioDeviceInfo.getEncodings().length > 0;
        return b;
    }



    public AudioAttributes getAudioAttributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat.Builder()
                .setChannelMask(this.curChannel)
                .setEncoding(this.encoding)
                .setSampleRate(this.currRate)
                .build();
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

    private void check() throws IOException {
        if (isBluetooth) {
            //final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mAddress);
            ParcelUuid[] Uuid = bluetoothDevice.getUuids();
            BluetoothSocket bluetoothSocket = null;
            mServiceListener = new MyServiceListener();
            bluetoothAdapter.getProfileProxy(App.getAppContext(), mServiceListener, BluetoothProfile.A2DP);
            int i = 0;
            while (true) {
                try {
                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(Uuid[i].getUuid());
                    bluetoothSocket.connect();
                    Log.e(TAG, "CONNECTION DONE!!!");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    i++;
                }
            }

                /*if (this.deviceType == DeviceType.isSink) {
                    try {
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        outputStream.write(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (this.deviceType == DeviceType.isSource) {
                    byte[] b = new byte[1];
                    try {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        inputStream.read(b);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }*/


        }
    }

    public int getCurrRate() {
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

    public AudioDeviceInfo getAudioDeviceInfo() {
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

    private class MyServiceListener implements ServiceListener {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(proxy instanceof BluetoothA2dp){
                BluetoothA2dp bluetoothA2dp = (BluetoothA2dp)proxy;
            }
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "onServiceDisconnected");

        }
    }
}