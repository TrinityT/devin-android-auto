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
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.IconCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloScreen extends Screen {

    private static final String VOICEVOX_URL = "https://api.tts.quest/v3/voicevox";
    private static final int SPEAKER_ID = 1; // ずんだもん
    private static final String WEATHER_API_KEY = "f710028e87868b49b1551ecc205f6978";
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 天気・音声データのキャッシュ有効期間(5分)

    // 天気データ・音声データはアプリ再起動をまたいで使えるようstaticでキャッシュする
    private static String cachedWeatherText = null;
    private static int cachedWeatherIcon = R.drawable.ic_weather_clear;
    private static long cachedWeatherTime = 0;
    private static final Map<String, byte[]> audioCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> audioCacheTime = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final FusedLocationProviderClient fusedLocationClient;

    private String weatherText = null;
    private int weatherIconRes = R.drawable.ic_weather_clear;
    private int parsedIconRes = R.drawable.ic_weather_clear;
    private boolean weatherRequested = false;

    public HelloScreen(@NonNull CarContext carContext) {
        super(carContext);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(carContext);
    }

    // 音声データの準備が完了してから画面に天気情報を表示する
    private void announce(String text, int iconRes) {
        executorService.execute(() -> {
            byte[] audioData = null;
            try {
                audioData = getCachedAudio(text);
                if (audioData == null) {
                    audioData = synthesizeAudio(text);
                    if (audioData != null) {
                        putCachedAudio(text, audioData);
                    }
                }
            } catch (Exception e) {
                Log.e("HelloScreen", "Error in VOICEVOX API", e);
            }
            // 音声データの作成が終わったので画面に天気情報を表示する
            presentWeather(text, iconRes);
            if (audioData != null) {
                playAudio(audioData);
            }
        });
    }

    private byte[] synthesizeAudio(String text) throws IOException, JSONException, InterruptedException {
        JSONObject jsonResponse = requestSynthesis(text);
        if (jsonResponse == null) {
            return null;
        }
        String audioStatusUrl = jsonResponse.optString("audioStatusUrl", null);
        String downloadUrl = jsonResponse.optString("mp3DownloadUrl", null);
        if (audioStatusUrl == null || downloadUrl == null) {
            return null;
        }
        if (!waitForAudioReady(audioStatusUrl)) {
            return null;
        }
        return downloadAudio(downloadUrl);
    }

    private void presentWeather(String text, int iconRes) {
        getCarContext().getMainExecutor().execute(() -> {
            weatherText = text;
            weatherIconRes = iconRes;
            invalidate();
        });
    }

    private static byte[] getCachedAudio(String text) {
        Long time = audioCacheTime.get(text);
        if (time != null && System.currentTimeMillis() - time < CACHE_DURATION_MS) {
            return audioCache.get(text);
        }
        audioCache.remove(text);
        audioCacheTime.remove(text);
        return null;
    }

    private static void putCachedAudio(String text, byte[] data) {
        audioCache.put(text, data);
        audioCacheTime.put(text, System.currentTimeMillis());
    }

    private static synchronized boolean isWeatherCacheValid() {
        return cachedWeatherText != null && System.currentTimeMillis() - cachedWeatherTime < CACHE_DURATION_MS;
    }

    private static synchronized void cacheWeather(String text, int iconRes) {
        cachedWeatherText = text;
        cachedWeatherIcon = iconRes;
        cachedWeatherTime = System.currentTimeMillis();
    }

    private void updateWeather(String text) {
        announce(text, R.drawable.ic_weather_clear);
    }

    private void speakWeather() {
        executorService.execute(() -> {
            try {
                if (isWeatherCacheValid()) {
                    Log.d("HelloScreen", "Using cached weather");
                    announce(cachedWeatherText, cachedWeatherIcon);
                } else {
                    getCurrentLocationAndSpeakWeather();
                }
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
                        cacheWeather(parsedWeather, parsedIconRes);
                        announce(parsedWeather, parsedIconRes);
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
        parsedIconRes = iconForWeatherId(weather.optInt("id", 800));
        JSONObject main = response.getJSONObject("main");
        double temp = main.getDouble("temp");
        double feelsLike = main.getDouble("feels_like");
        int humidity = main.getInt("humidity");
        
        String cityName = response.optString("name", "現在の地点");
        
        return cityName + "の天気は" + description + "なのだ。気温は" + (int)temp + "度、体感温度は" + (int)feelsLike + "度なのだ。湿度は" + humidity + "パーセントなのだ";
    }

    private int iconForWeatherId(int id) {
        if (id >= 200 && id < 300) return R.drawable.ic_weather_thunder;
        if (id >= 300 && id < 600) return R.drawable.ic_weather_rain;
        if (id >= 600 && id < 700) return R.drawable.ic_weather_snow;
        if (id >= 700 && id < 800) return R.drawable.ic_weather_mist;
        if (id == 800) return R.drawable.ic_weather_clear;
        if (id > 800) return R.drawable.ic_weather_clouds;
        return R.drawable.ic_weather_clear;
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

        String message = (weatherText != null) ? weatherText : "天気を準備中なのだ...";

        CarIcon weatherIcon = new CarIcon.Builder(
                IconCompat.createWithResource(getCarContext(), weatherIconRes)).build();

        return new MessageTemplate.Builder(message)
                .setTitle("ずんだもん天気予報")
                .setHeaderAction(Action.APP_ICON)
                .setIcon(weatherIcon)
                .build();
    }
}
