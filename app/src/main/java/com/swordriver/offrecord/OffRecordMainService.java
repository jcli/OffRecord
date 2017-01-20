package com.swordriver.offrecord;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OffRecordMainService extends Service {
    public OffRecordMainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
