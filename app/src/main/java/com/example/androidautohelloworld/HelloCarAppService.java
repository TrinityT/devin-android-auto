package com.example.androidautohelloworld;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

public class HelloCarAppService extends CarAppService {

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        Log.d("HelloCarAppService", "onCreateSession(SessionInfo) called: " + sessionInfo);
        return new HelloCarAppSession();
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        Log.d("HelloCarAppService", "onCreateSession() called");
        return new HelloCarAppSession();
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }
}
