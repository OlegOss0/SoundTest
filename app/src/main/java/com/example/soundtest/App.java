package com.example.soundtest;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    private static Context context;
    private static App INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static Context getAppContext(){
        return INSTANCE.getApplicationContext();
    }
}
