package com.crossfade;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    private static final int SET_TRACK_0 = 1;
    private static final int SET_TRACK_1 = 2;
    private static final int READ_EXTERNAL_REQUEST = 0;
    @BindView(R.id.b_play)
    ImageButton playButton;
    @BindView(R.id.b_select_0)
    Button selectTrack0;
    @BindView(R.id.b_select_1)
    Button selectTrack1;
    @BindView(R.id.seekBar)
    SeekBar seekBar;
    @BindView(R.id.loading_view)
    View loadingView;
    @BindView(R.id.time)
    TextView timeView;

    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        player = new Player(getApplicationContext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress += 2;
                player.setCrossfadeDuration(progress * 1000);
                timeView.setText(secondsToText(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        selectTrack0.setSelected(true);
        selectTrack1.setSelected(true);
        setUpSeekBar();
        updatePlayButton();
    }


    @OnClick(R.id.b_play)
    void play() {
        if (!player.isCurTrackLoaded() || !player.isNextTrackLoaded()) {
            Toast.makeText(this, R.string.select_tracks, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!player.isPlaying()) {
            player.play();
        } else {
            player.stop();
        }
        updatePlayButton();
    }


    @OnClick({R.id.b_select_0, R.id.b_select_1})
    void select(View v) {
        if (!isPermissionGranted()) return;
        player.stop();
        updatePlayButton();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.no_required_app, Toast.LENGTH_SHORT).show();
            return;
        }
        if (v.getId() == R.id.b_select_0) {
            startActivityForResult(intent, SET_TRACK_0);
        } else if (v.getId() == R.id.b_select_1) {
            startActivityForResult(intent, SET_TRACK_1);
        }

    }

    private void updatePlayButton() {
        if (player.isPlaying()) {
            playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop, null));
        } else {
            playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow, null));
        }
    }

    private void setUpSeekBar() {
        if (!player.isCurTrackLoaded() || !player.isNextTrackLoaded()) {
            seekBar.setEnabled(false);
            return;
        }
        seekBar.setEnabled(true);
        int minDuration = player.getCurTrackDuration();
        if (minDuration > player.getNextTrackDuration())
            minDuration = player.getNextTrackDuration();
        int max = minDuration / 2 / 1000;
        if (max > 8) max = 8;
        seekBar.setMax(max);
    }

    private String secondsToText(int s) {
        int sec = s % 60;
        int min = (s / 60) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", min, sec);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadingView.setVisibility(View.VISIBLE);
            if ((data != null) && (data.getData() != null)) {
                Uri audioFileUri = data.getData();
                if (requestCode == SET_TRACK_0) {
                    player.setCurTrack(audioFileUri,
                            new Player.TrackLoadedListener() {
                                @Override
                                public void onSuccess() {
                                    selectTrack0.setText(player.getCurTrackName());
                                    loadingView.setVisibility(View.GONE);
                                    setUpSeekBar();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(MainActivity.this, R.string.file_not_loaded, Toast.LENGTH_SHORT).show();
                                    selectTrack0.setText(R.string.select_track_1);
                                    loadingView.setVisibility(View.GONE);
                                    setUpSeekBar();
                                }
                            });
                } else if (requestCode == SET_TRACK_1) {
                    player.setNextTrack(audioFileUri,
                            new Player.TrackLoadedListener() {
                                @Override
                                public void onSuccess() {
                                    selectTrack1.setText(player.getNextTrackName());
                                    loadingView.setVisibility(View.GONE);
                                    setUpSeekBar();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(MainActivity.this, R.string.file_not_loaded, Toast.LENGTH_SHORT).show();
                                    selectTrack1.setText(R.string.select_track_2);
                                    loadingView.setVisibility(View.GONE);
                                    setUpSeekBar();
                                }
                            }
                    );
                }
            }
        }

    }

    private boolean isPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, R.string.storage_access_req, Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_REQUEST);
            }
            return false;
        } else {
            return true;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
