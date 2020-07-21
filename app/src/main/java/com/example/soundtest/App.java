package com.example.soundtest;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class App extends Application {
    private static App INSTANCE;
    private static Handler uiHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        uiHandler = new Handler();
    }

    public static Context getAppContext(){
        return INSTANCE.getApplicationContext();
    }

    public static Handler getHandler(){
        return uiHandler;
    }


}
