package com.example.cs2340project2.utils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Wrapped {
    private final String accessToken;
    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private Call mCall;
    private static final String API_URL = "https://api.spotify.com/v1/me/top/";

    public interface TopSongsCallback {
        void onSuccess(List<SongInfo> topSongs);
        void onFailure(String errorMessage);
    }

    public interface TopArtistsCallback {
        void onSuccess(List<ArtistInfo> topArtists);
        void onFailure(String errorMessage);
    }

    public Wrapped(String accessToken) {
        this.accessToken = accessToken;
    }

    public void getTopSongs(String timeRange, TopSongsCallback callback) {
        final Request request = new Request.Builder()
                .url(API_URL + "tracks?limit=5&time_range=" + timeRange)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        cancelCall();

        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray items = jsonObject.getJSONArray("items");

                    List<SongInfo> songList = new ArrayList<>();

                    // Extract song info and add to the songList
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String songName = item.getString("name");

                        JSONArray artists = item.getJSONArray("artists");
                        JSONObject artist = artists.getJSONObject(0);
                        String artistName = artist.getString("name");

                        // Retrieve image URL for the first artist
                        String imageUrl = null;
                        JSONObject album = item.getJSONObject("album");
                        JSONArray images = album.getJSONArray("images");
                        if (images.length() > 0) {
                            JSONObject imageObject = images.getJSONObject(0);
                            imageUrl = imageObject.getString("url");
                        }

                        String clip = item.getString("preview_url");

                        songList.add(new SongInfo(songName, artistName, imageUrl, clip));
                    }
                    callback.onSuccess(songList);

                } catch (JSONException e) {
                    callback.onFailure(e.getMessage());
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    public void getTopArtists(String timeRange, TopArtistsCallback callback) {
        final Request request = new Request.Builder()
                .url(API_URL + "artists?limit=5&time_range=" + timeRange)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        cancelCall();

        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray items = jsonObject.getJSONArray("items");

                    List<ArtistInfo> artistList = new ArrayList<>();

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject artistObject = items.getJSONObject(i);
                        String artistName = artistObject.getString("name");

                        String artistImageUrl = null;
                        JSONArray images = artistObject.optJSONArray("images");
                        if (images != null && images.length() > 0) {
                            JSONObject imageObject = images.getJSONObject(0);
                            artistImageUrl = imageObject.getString("url");
                        }

                        artistList.add(new ArtistInfo(artistName, artistImageUrl));
                    }

                    callback.onSuccess(artistList);
                } catch (JSONException e) {
                    callback.onFailure(e.getMessage());
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }
}
