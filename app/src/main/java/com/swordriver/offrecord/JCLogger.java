package com.swordriver.offrecord;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.EnumSet;
import java.util.HashSet;

/**
 * Created by jcli on 4/8/16.
 */
public class JCLogger {
    public enum LogAreas{
        UNKNOWN,
        LIFECYCLE,
        BACKGROUND,
        UI,
        GOOGLEAPI,
        SECURE_NOTES;
        public String s(){
            return this.name();
        }
    }

    public enum LogLevel{
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARNING(5),
        ERROR(6);
        private int value;
        private LogLevel(int value){
            this.value=value;
        }
        public int getValue(){return this.value;}
    }
    private static EnumSet<LogAreas> mCurrentAreas= EnumSet.noneOf(LogAreas.class);
    private static final HashSet<String> mCurrentAreasString = new HashSet<String>();
    private static LogLevel mCurrentLogLevel= LogLevel.ERROR;
    private static boolean mPrintThread=false;
    private static int mStackOffset=0;
    private static final LogLevel[] mLogLevels = LogLevel.values();

    //public static void out()
    public static void enableLogArea(LogAreas newArea){
        mCurrentAreas.add(newArea);
        mCurrentAreasString.add(newArea.toString());
    }

    public static void disableLogArea(LogAreas oldArea){
        mCurrentAreas.remove(oldArea);
        mCurrentAreasString.remove(oldArea.toString());
    }

    public static void setCurrentLogLevel(LogLevel level){
        mCurrentLogLevel=level;
    }

    public static void clearAllLogAreas(){
        mCurrentAreas.clear();
        mCurrentAreasString.clear();
    }

    public static void printCurrentAreas(){
        for (LogAreas area: mCurrentAreas){
            Log.v("JCLogger", area.s());
        }
    }

    public static void printThreadInfo(boolean set){
        mPrintThread = set;
    }

    public static void setStackOffset(int offset){
        mStackOffset=offset;
    }

    public static void log(LogLevel level, LogAreas area, String message){

    }

    public static void log(int level, String logArea, String message){
        if (logArea==null) logArea= LogAreas.UNKNOWN.s();
        if (level>= mCurrentLogLevel.getValue() && mCurrentAreasString.contains(logArea)) {
            String threadInfo="";
            if (mPrintThread){
                threadInfo = ",thread(" + Thread.currentThread().getName() + ")";
            }
            Exception ex = new Exception();
            String callerClass;
            String callerMethod;
            int lineNumber=0;
            if (ex.getStackTrace().length>(1+mStackOffset)){
                callerClass = ex.getStackTrace()[1+mStackOffset].getClassName();
                callerClass = callerClass.substring(callerClass.lastIndexOf(".")+1); // hope it doesn't end with "."
                callerMethod = ex.getStackTrace()[1+mStackOffset].getMethodName();
                lineNumber = ex.getStackTrace()[1+mStackOffset].getLineNumber();
            }else{
                callerClass="UNKNOWN";
                callerMethod="UNKNOWN";
            }
            String tag = JCLogger.class.getSimpleName();
            message = callerClass + "::" + callerMethod + ":" + lineNumber + ",LogArea(" + logArea + ")" + threadInfo + ": " + message;
            Log.println(level, tag, message);
        }
    }

    public static void log(int level, String logArea, String message, @NonNull Throwable exception){
        mStackOffset++;
        String stackTrace = Log.getStackTraceString(exception);
        String newMsg = message + "\n" + stackTrace;
        log(level, logArea, newMsg);
        mStackOffset--;
    }
}
