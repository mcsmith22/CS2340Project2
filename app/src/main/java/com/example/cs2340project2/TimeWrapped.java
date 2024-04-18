package com.example.cs2340project2;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cs2340project2.ui.wraps.WrappedActivity;
import com.example.cs2340project2.utils.ArtistInfo;
import com.example.cs2340project2.utils.SongInfo;
import com.example.cs2340project2.utils.SpotifyAuthentication;
import com.example.cs2340project2.utils.WrapData;
import com.example.cs2340project2.utils.Wrapped;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// TODO: change UI

public class TimeWrapped extends AppCompatActivity {
    private Button pastMonth;
    private Button past6Months;
    private Button pastYear;
    private FirebaseFirestore fstore;
    private String userID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_wrapped);

        pastMonth = findViewById(R.id.past_month_btn);
        past6Months = findViewById(R.id.past_6_month_btn);
        pastYear = findViewById(R.id.past_year_btn);
    }

    @Override
    public void onStart() {
        super.onStart();

        pastMonth.setOnClickListener(view -> {
            launchWrapped("short_term");
        });

        past6Months.setOnClickListener(view -> {
            launchWrapped("medium_term");
        });

        pastYear.setOnClickListener(view -> {
            launchWrapped("long_term");
        });
    }

    private void launchWrapped(String timeRange) {
        SpotifyAuthentication.refreshToken(new SpotifyAuthentication.AccessTokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                Wrapped wrapped = new Wrapped(accessToken);
                Intent intent = new Intent(getBaseContext(), WrappedActivity.class);
                intent.putExtra("timeRange", timeRange);
                wrapped.getTopArtists(timeRange, new Wrapped.TopArtistsCallback() {
                    @Override
                    public void onSuccess(List<ArtistInfo> topArtists) {
                        if (topArtists.size() != 5) {
                            Snackbar.make(findViewById(R.id.time_wrapped_container), "Not enough artists listened to", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        intent.putExtra("topArtists", (Serializable) topArtists);
                        wrapped.getTopSongs(timeRange, new Wrapped.TopSongsCallback() {
                            @Override
                            public void onSuccess(List<SongInfo> topSongs) {
                                if (topSongs.size() != 5) {
                                    Snackbar.make(findViewById(R.id.time_wrapped_container), "Not enough songs listened to", Snackbar.LENGTH_SHORT).show();
                                    return;
                                }
                                intent.putExtra("topSongs", (Serializable) topSongs);

                                String date = new SimpleDateFormat("M/d/yy", Locale.getDefault()).format(new Date());
                                intent.putExtra("generatedDate", date);

                                fstore = FirebaseFirestore.getInstance();
                                userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                DocumentReference documentReference = fstore.collection("pastwraps").document(userID);

                                WrapData wrap = new WrapData(timeRange, date , topArtists, topSongs);
                                Map<String, Object> map = new HashMap<>();

                                final int[] numWraps = new int[1];
                                documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                numWraps[0] = document.getData().size();
                                                map.putAll(document.getData());
                                            } else {
                                                numWraps[0] = 0;
                                            }
                                        } else {
                                            Log.d(TAG, "get failed with ", task.getException());
                                        }


                                        map.put(Integer.toString(numWraps[0]), wrap);
                                        documentReference.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Log.d(TAG, "onSuccess: Past Wrap Uploaded to Database");
                                            }
                                        });

                                        startActivity(intent);
                                    }
                                });

                            }

                            @Override
                            public void onFailure(String errorMessage) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {

                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {

            }
        });
    }
}
