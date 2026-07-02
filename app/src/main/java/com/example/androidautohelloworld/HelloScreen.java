package com.example.androidautohelloworld;

import android.media.MediaPlayer;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloScreen extends Screen {

    private static final String VOICEVOX_URL = "https://api.tts.quest/v3/voicevox";
    private static final int SPEAKER_ID = 3;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();

    public HelloScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    private void speakHelloWorld() {
        executorService.execute(() -> {
            try {
                JSONObject jsonResponse = requestSynthesis("Hello World!");
                if (jsonResponse != null) {
                    String audioStatusUrl = jsonResponse.optString("audioStatusUrl", null);
                    String downloadUrl = jsonResponse.optString("mp3DownloadUrl", null);
                    
                    if (audioStatusUrl != null) {
                        boolean isReady = waitForAudioReady(audioStatusUrl);
                        if (isReady && downloadUrl != null) {
                            byte[] audioData = downloadAudio(downloadUrl);
                            playAudio(audioData);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("HelloScreen", "Error in VOICEVOX API", e);
            }
        });
    }

    private JSONObject requestSynthesis(String text) throws IOException, JSONException {
        String url = VOICEVOX_URL + "/synthesis?speaker=" + SPEAKER_ID + "&text=" + java.net.URLEncoder.encode(text, "UTF-8");
        Log.d("HelloScreen", "Requesting synthesis: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            Log.d("HelloScreen", "Response code: " + response.code());
            if (response.code() == 429) {
                Log.e("HelloScreen", "VOICEVOX API Rate Limit Exceeded (429)");
                return null;
            }
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "empty body";
                Log.e("HelloScreen", "Request failed: " + errorBody);
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            Log.d("HelloScreen", "Response body: " + responseBody);
            return new JSONObject(responseBody);
        }
    }

    private boolean waitForAudioReady(String audioStatusUrl) throws IOException, JSONException, InterruptedException {
        Log.d("HelloScreen", "Waiting for audio ready: " + audioStatusUrl);
        for (int i = 0; i < 30; i++) {
            Request request = new Request.Builder()
                    .url(audioStatusUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("HelloScreen", "Status check failed: " + response.code());
                    throw new IOException("Unexpected code " + response);
                }
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                
                boolean isAudioReady = jsonResponse.optBoolean("isAudioReady", false);
                boolean isAudioError = jsonResponse.optBoolean("isAudioError", false);
                
                Log.d("HelloScreen", "Poll " + i + ": isAudioReady=" + isAudioReady);

                if (isAudioError) {
                    Log.e("HelloScreen", "Audio synthesis error: " + jsonResponse.optString("status", "unknown"));
                    return false;
                }
                
                if (isAudioReady) {
                    return true;
                }
            }
            
            Thread.sleep(2000);
        }
        Log.e("HelloScreen", "Timeout waiting for audio ready");
        return false;
    }

    private byte[] downloadAudio(String downloadUrl) throws IOException {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().bytes();
        }
    }

    private void playAudio(byte[] audioData) {
        Log.d("HelloScreen", "playAudio called with data size: " + audioData.length);
        try {
            java.io.File tempFile = java.io.File.createTempFile("voicevox", ".mp3", getCarContext().getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();
            Log.d("HelloScreen", "Temp file created: " + tempFile.getAbsolutePath());

            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d("HelloScreen", "MediaPlayer started");

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d("HelloScreen", "MediaPlayer completed");
                mp.release();
                tempFile.delete();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("HelloScreen", "MediaPlayer error: what=" + what + " extra=" + extra);
                mp.release();
                tempFile.delete();
                return true;
            });
        } catch (Exception e) {
            Log.e("HelloScreen", "Error playing audio", e);
        }
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        speakHelloWorld();
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
