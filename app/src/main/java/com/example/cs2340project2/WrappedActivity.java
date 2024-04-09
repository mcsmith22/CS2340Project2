package com.example.cs2340project2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WrappedActivity {

    private static final String API_URL = "https://api.spotify.com/v1/me/player/recently-played";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public enum TimeRange {
        WEEK,
        MONTH,
        YEAR
    }

    public WrappedActivity() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void fetchUserInfo(TimeRange timeRange) {
        try {
            String formattedStartTime, formattedEndTime;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Calendar calendar = Calendar.getInstance();

            switch (timeRange) {
                case WEEK:
                    calendar.add(Calendar.DAY_OF_YEAR, -7);
                    break;
                case MONTH:
                    calendar.add(Calendar.MONTH, -1);
                    break;
                case YEAR:
                    calendar.add(Calendar.YEAR, -1);
                    break;
            }

            formattedStartTime = sdf.format(calendar.getTime());
            formattedEndTime = sdf.format(new Date());

            String accessToken = getToken();

            URL url = new URL(API_URL + "?limit=50&start_time=" + formattedStartTime + "&end_time=" + formattedEndTime);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            List<SongInfo> songList = extractSongInfo(items);

            // Save songList to Firebase Database
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                DatabaseReference userRef = mDatabase.child("users").child(currentUser.getUid()).child("songs");
                userRef.setValue(songList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("your call failed :/");
        }
    }

    private String getToken() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid(); // Replace with your logic to retrieve the access token
        }
        return null;
    }

    private List<SongInfo> extractSongInfo(JSONArray items) throws JSONException {
        List<SongInfo> songList = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject track = item.getJSONObject("track");
            String songName = track.getString("name");

            JSONArray artists = track.getJSONArray("artists");
            JSONObject artist = artists.getJSONObject(0);
            String artistName = artist.getString("name");

            long durationMs = track.getLong("duration_ms");
            long listeningTimeInSeconds = durationMs / 1000;
            songList.add(new SongInfo(songName, artistName, listeningTimeInSeconds));
        }
        return songList;
    }

    private static class SongInfo {
        private String name;
        private String artist;
        private long listeningTimeInSeconds;

        public SongInfo(String name, String artist, long listeningTimeInSeconds) {
            this.name = name;
            this.artist = artist;
            this.listeningTimeInSeconds = listeningTimeInSeconds;
        }

        public String getName() {
            return name;
        }

        public long getListeningTimeInSeconds() {
            return listeningTimeInSeconds;
        }
    }
}