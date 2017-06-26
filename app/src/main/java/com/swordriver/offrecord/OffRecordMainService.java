package com.swordriver.offrecord;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.data.DataBufferObserver;
import com.swordriver.offrecord.JCLogger.LogAreas;

import java.util.Observable;
import java.util.Observer;

import swordriver.com.googledrivemodule.GoogleApiModel;
import swordriver.com.googledrivemodule.GoogleApiModelSecure;
import timber.log.Timber;

public class OffRecordMainService extends Service{
    public OffRecordMainService() {
    }

    public enum OffRecordServiceState {
        GDRIVE_INIT,
        INITIALIZED
    }

    OffRecordServiceState mState = OffRecordServiceState.GDRIVE_INIT;

    //////////////////////////////////////////////
    //Binder and life cycle events
    //////////////////////////////////////////////
    public class LocalBinder extends Binder {
        public OffRecordMainService getService(){
            return OffRecordMainService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        return mBinder;
    }

    static final int EXIT_TIMEOUT = 5000;

    CountDownTimer mExitTimer=null;

    @Override
    public void onRebind(Intent intent) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        if (mExitTimer!=null) mExitTimer.cancel();
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        mExitTimer = new CountDownTimer(EXIT_TIMEOUT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                stopSelf();
            }
        }.start();
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        // The service is starting, due to a call to startService()
        // check if the current service state is valid everytime onStart is called.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.tag(LogAreas.LIFECYCLE.s()).v("called.");
        super.onDestroy();
        // TODO: disconnect from services
    }

    /////////////////////////////////////////////
    // public methods
    /////////////////////////////////////////////
    public OffRecordServiceState getState(){
        return mState;
    }

    /////////////////////////////////////////////
    // google api reference and lifecycle
    /////////////////////////////////////////////
    private Observer mGoogleObserver = new Observer() {
        @Override
        public void update(Observable observable, Object data) {
            GoogleApiModel.GoogleApiStatus status = (GoogleApiModel.GoogleApiStatus)(data);
            switch (status){
                case DISCONNECTED:
                    break;
                case CONNECTED_UNINITIALIZED:
                    break;
                case INITIALIZED:
                    //TODO: start processing
                    mState=OffRecordServiceState.INITIALIZED;
                    break;
            }
        }
    };

    private GoogleApiModelSecure mGoogleApiModel =null;
    public GoogleApiModelSecure getGoogleApiModel(AppCompatActivity callerActivity){
        if (mGoogleApiModel==null && callerActivity!=null){
            mGoogleApiModel = new GoogleApiModelSecure(this, callerActivity, LogAreas.GOOGLEAPI.s(), getString(R.string.server_client_id));
            mGoogleApiModel.addObserver(mGoogleObserver);
        }
        return mGoogleApiModel;
    }

    /////////////////////////////////////////////
    // Note datasource reference and lifecycle
    /////////////////////////////////////////////
    private DataSourceNotes mNotesDataSource;

    public DataSourceNotes getNotesDataSource(){
        if (mNotesDataSource==null){
            mNotesDataSource = new DataSourceNotes();
        }
        return mNotesDataSource;
    }

}
