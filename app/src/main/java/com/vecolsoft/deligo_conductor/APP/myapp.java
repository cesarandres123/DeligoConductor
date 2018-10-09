package com.vecolsoft.deligo_conductor.APP;

import android.app.Application;
import android.os.SystemClock;

public class myapp extends Application {



    @Override
    public void onCreate() {
        super.onCreate();

        SystemClock.sleep(500);


    }
}
