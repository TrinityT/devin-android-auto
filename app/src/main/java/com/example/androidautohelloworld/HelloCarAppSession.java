package com.example.androidautohelloworld;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

public class HelloCarAppSession extends Session {

    @NonNull
    @Override
    public Screen onCreateScreen(@NonNull Intent intent) {
        Log.d("HelloCarAppSession", "onCreateScreen called");
        return new HelloScreen(getCarContext());
    }
}
