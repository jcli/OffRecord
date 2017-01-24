package com.swordriver.offrecord;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.swordriver.offrecord.JCLogger.LogAreas;

import java.util.Observable;
import java.util.Observer;

import swordriver.com.googledrivemodule.GoogleApiModel;
import swordriver.com.googledrivemodule.GoogleApiModelSecure;
import timber.log.Timber;

public class OffRecordMainService extends Service {
    public OffRecordMainService() {
    }

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
        return mBinder;
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
        super.onDestroy();
        // TODO: disconnect from services
    }

    /////////////////////////////////////////////
    // public methods
    /////////////////////////////////////////////

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
    // google api reference and lifecycle
    /////////////////////////////////////////////
    private DataSourceNotes mNotesDataSource;

    public DataSourceNotes getNotesDataSource(){
        if (mNotesDataSource==null){
            mNotesDataSource = new DataSourceNotes();
        }
        return mNotesDataSource;
    }
}
