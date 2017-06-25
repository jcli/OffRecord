package com.swordriver.offrecord;

import android.app.Application;

import timber.log.Timber;

import static java.lang.System.exit;

/**
 * Created by jcli on 1/19/17.
 */

public class OffRecordApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize logging
        OffRecordLogger.init();
        Timber.tag(JCLogger.LogAreas.LIFECYCLE.s()).v("called.");

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                Timber.e(e, "Uncaught Exception!!");
                exit(1);
            }
        });
    }

}