package com.swordriver.offrecord;

import com.orhanobut.logger.Logger;

import timber.log.Timber;

/**
 * Created by jcli on 10/16/16.
 */

public class OffRecordLogger {
    private static boolean initialized = false;
    private enum LoggerUsed{
        ORHANOBUT_LOGGER,
        JC_LOGGER
    }
    //private static final LoggerUsed loggerUsed = LoggerUsed.ORHANOBUT_LOGGER;
    private static final LoggerUsed loggerUsed = LoggerUsed.JC_LOGGER;

    public static synchronized void init(){
        if (!initialized){
            // connect JCLogger
            JCLogger.printThreadInfo(true);
            JCLogger.enableLogArea(JCLogger.LogAreas.UNKNOWN);
            JCLogger.enableLogArea(JCLogger.LogAreas.LIFECYCLE);
            JCLogger.enableLogArea(JCLogger.LogAreas.BACKGROUND);
            JCLogger.enableLogArea(JCLogger.LogAreas.UI);
            JCLogger.enableLogArea(JCLogger.LogAreas.GOOGLEAPI);
            JCLogger.enableLogArea(JCLogger.LogAreas.SECURE_NOTES);
            JCLogger.setStackOffset(4);

            if (BuildConfig.DEBUG) {
                // connect Orhanobut
                Logger.init()
                        .methodCount(2)                 // default 2
                        .methodOffset(4)                // default 0
                        .hideThreadInfo();               // default shown

                JCLogger.setCurrentLogLevel(JCLogger.LogLevel.VERBOSE);

                switch (loggerUsed){
                    case ORHANOBUT_LOGGER:
                        Timber.plant(new Timber.DebugTree(){
                            @Override
                            protected void log(int priority, String tag, String message, Throwable t) {
                                Logger.log(priority, tag, message, t);
                            }
                        });
                        break;
                    case JC_LOGGER:
                        setJCLogger();
                        break;
                }
            }else{
                JCLogger.setCurrentLogLevel(JCLogger.LogLevel.WARNING);
                setJCLogger();
            }
            initialized=true;
        }
    }
    private OffRecordLogger() {
        throw new AssertionError("No instances.");
    }

    /////////////////////////////////////////////////////////////////////
    // private helper
    /////////////////////////////////////////////////////////////////////

    private static void setJCLogger(){
        Timber.plant(new Timber.Tree(){
            @Override
            protected void log(int priority, String tag, String message, Throwable t) {
                if (t==null) {
                    JCLogger.log(priority, tag, message);
                }else{
                    JCLogger.log(priority, tag, message, t);
                }
            }
        });
    }
}
