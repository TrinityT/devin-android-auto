package com.example.androidautohelloworld;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;

public class HelloScreen extends Screen {

    public HelloScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        try {
            return new MessageTemplate.Builder("Welcome to Android Auto!")
                    .setTitle("Hello World!")
                    .setHeaderAction(Action.BACK)
                    .build();
        } catch (Exception e) {
            Log.e("HelloScreen", "Error in onGetTemplate", e);
            throw e;
        }
    }
}
