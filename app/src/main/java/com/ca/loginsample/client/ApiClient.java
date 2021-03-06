package com.ca.loginsample.client;

import com.ca.loginsample.interfaces.ApiInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static final String BASE_URL = "http://51.68.109.219:9294";

    public static final String BASE_URL_2 = "http://51.89.235.135";

    public static final String BASE_VOX = "https://proxy.vox-cpaas.com";

    private static Retrofit retrofit,retrofit2 = null;

    public static ApiInterface getInstance(String url) {

        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            retrofit =
                    new Retrofit.Builder()
                            .baseUrl(url)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .client(getRequestHeader())
                            .build();
        }

        return retrofit.create(ApiInterface.class);
    }

    public static ApiInterface getInstanceVOX(String url) {

        if (retrofit2 == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            retrofit2 =
                    new Retrofit.Builder()
                            .baseUrl(url)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .client(getRequestHeader())
                            .build();
        }

        return retrofit2.create(ApiInterface.class);
    }
    private static OkHttpClient getRequestHeader() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(interceptor)
                        .connectTimeout(40, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .build();
        return httpClient;
    }
}