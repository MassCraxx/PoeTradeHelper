package de.crass.poetradehelper.web;

import de.crass.poetradehelper.LogManager;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpManager {
    private static HttpManager instance;

    private OkHttpClient client = new OkHttpClient();

    public static HttpManager getInstance() {
        if(instance == null){
            instance = new HttpManager();
        }
        return instance;
    }

    public String get(String url, String params) throws IOException {
        String get = url + params;
        Request request = new Request.Builder()
                .url(get)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if(response.code() == 404 || !response.isSuccessful()){
            return null;
        }
        return response.body().string();
    }

    public JSONObject postJSON(String url, String requestBody) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();

        okhttp3.RequestBody body = RequestBody.create(JSON, requestBody);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return new JSONObject(response.body().string());
    }

    public JSONObject getJson(String fetchURL, String params) throws IOException, JSONException {
        String json = get(fetchURL, params);
        if(json == null || json.isEmpty()){
            LogManager.getInstance().log(getClass(), "getJson from "+fetchURL+params+" failed! Response was empty.");
            return null;
        }
        return new JSONObject(json);
    }

    private String readAll(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }

            is.close();
            rd.close();

            return sb.toString();
        }

    }

    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        return new JSONObject(readAll(url));
    }
}
