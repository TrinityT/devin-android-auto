package com.example.androidautohelloworld;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloScreen extends Screen {

    private static final String VOICEVOX_URL = "https://api.tts.quest/v3/voicevox";
    private static final int SPEAKER_ID = 1; // ずんだもん
    private static final String WEATHER_API_KEY = "f710028e87868b49b1551ecc205f6978";
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final FusedLocationProviderClient fusedLocationClient;

    private String weatherText = null;
    private boolean weatherRequested = false;

    public HelloScreen(@NonNull CarContext carContext) {
        super(carContext);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(carContext);
    }

    private void speakText(String text) {
        executorService.execute(() -> {
            try {
                JSONObject jsonResponse = requestSynthesis(text);
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

    private void updateWeather(String text) {
        getCarContext().getMainExecutor().execute(() -> {
            weatherText = text;
            invalidate();
        });
        speakText(text);
    }

    private void speakWeather() {
        executorService.execute(() -> {
            try {
                getCurrentLocationAndSpeakWeather();
            } catch (Exception e) {
                Log.e("HelloScreen", "Error in weather", e);
            }
        });
    }

    private void getCurrentLocationAndSpeakWeather() {
        if (ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("HelloScreen", "Location permission not granted");
            updateWeather("位置情報の許可が必要なのだ");
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    Log.d("HelloScreen", "Location: " + lat + ", " + lon);
                    fetchWeatherAndSpeak(lat, lon);
                } else {
                    Log.e("HelloScreen", "Location is null");
                    updateWeather(getMockWeather());
                }
            })
            .addOnFailureListener(e -> {
                Log.e("HelloScreen", "Failed to get location", e);
                updateWeather(getMockWeather());
            });
    }

    private void fetchWeatherAndSpeak(double lat, double lon) {
        executorService.execute(() -> {
            try {
                String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + 
                             "&lon=" + lon + "&appid=" + WEATHER_API_KEY + 
                             "&units=metric&lang=ja";
                Log.d("HelloScreen", "Weather API URL: " + url);
                
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = httpClient.newCall(request).execute()) {
                    Log.d("HelloScreen", "Weather API response code: " + response.code());
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("HelloScreen", "Weather API response: " + responseBody);
                        String parsedWeather = parseWeatherResponse(responseBody);
                        updateWeather(parsedWeather);
                    } else {
                        Log.e("HelloScreen", "Weather API failed: " + response.code());
                        updateWeather(getMockWeather());
                    }
                }
            } catch (Exception e) {
                Log.e("HelloScreen", "Error in weather API", e);
                updateWeather(getMockWeather());
            }
        });
    }

    private String parseWeatherResponse(String json) throws JSONException {
        JSONObject response = new JSONObject(json);
        JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        JSONObject main = response.getJSONObject("main");
        double temp = main.getDouble("temp");
        double feelsLike = main.getDouble("feels_like");
        int humidity = main.getInt("humidity");
        
        String cityName = response.optString("name", "現在の地点");
        
        return cityName + "の天気は" + description + "なのだ。気温は" + (int)temp + "度、体感温度は" + (int)feelsLike + "度なのだ。湿度は" + humidity + "パーセントなのだ";
    }

    private String getMockWeather() {
        return "天気情報の取得に失敗したのだ。現在の地点の天気は晴れなのだ。気温は23度なのだ";
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
        // アプリ起動時に一度だけ天気を取得して読み上げる
        if (!weatherRequested) {
            weatherRequested = true;
            speakWeather();
        }

        String message = (weatherText != null) ? weatherText : "天気を取得中なのだ...";

        return new MessageTemplate.Builder(message)
                .setTitle("ずんだもん天気予報")
                .setHeaderAction(Action.APP_ICON)
                .build();
    }
}
