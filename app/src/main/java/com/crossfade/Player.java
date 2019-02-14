package com.crossfade;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class Player {

    private final String _TAG = "PlayerTag";
    private Context context;
    private final MyMediaPlayer mediaPlayer0;
    private final MyMediaPlayer mediaPlayer1;
    private MediaMetadataRetriever metaRetriever1;
    private MediaMetadataRetriever metaRetriever0;
    private boolean prepared0 = false;
    private boolean prepared1 = false;
    private int crossfadeDuration = 10;
    private Disposable timer0;
    private Disposable timer1;
    private CompositeDisposable disposables;

    public Player(Context context) {
        this.context = context;

        mediaPlayer0 = new MyMediaPlayer();
        mediaPlayer1 = new MyMediaPlayer();
        mediaPlayer0.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer1.setAudioStreamType(AudioManager.STREAM_MUSIC);

        disposables = new CompositeDisposable();
    }

    public void setCurTrack(Uri source, TrackLoadedListener listener) {
        prepared0 = false;
        mediaPlayer0.reset();
        try {
            mediaPlayer0.setDataSource(context, source);
            mediaPlayer0.setOnPreparedListener(mp -> {
                prepared0 = true;
                metaRetriever0 = new MediaMetadataRetriever();
                metaRetriever0.setDataSource(context, source);
                listener.onSuccess();
            });
            mediaPlayer0.setOnErrorListener((mp, what, extra) -> {
                listener.onError(new Exception("error code = "+what+" extra = "+extra));
                return false;
            });
            mediaPlayer0.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(e);
        }
    }

    public void setNextTrack(Uri source, TrackLoadedListener listener) {
        prepared1 = false;
        mediaPlayer1.reset();
        try {
            mediaPlayer1.setDataSource(context, source);
            mediaPlayer1.setOnPreparedListener(mp -> {
                prepared1 = true;
                metaRetriever1 = new MediaMetadataRetriever();
                metaRetriever1.setDataSource(context, source);
                listener.onSuccess();
            });
            mediaPlayer1.setOnErrorListener((mp, what, extra) -> {
                listener.onError(new Exception("error code = "+what+" extra = "+extra));
                return false;
            });
            mediaPlayer1.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(e);
        }
    }

    public void setCrossfadeDuration(int crossfadeDuration) {
        int minDuration = getCurTrackDuration();
        if (minDuration > getNextTrackDuration())
            minDuration = getNextTrackDuration();
        if (crossfadeDuration > minDuration / 2)
            crossfadeDuration = minDuration / 2;
        if (crossfadeDuration == 0) crossfadeDuration = 10;
        this.crossfadeDuration = crossfadeDuration;
    }

    public void play() {
        if (isPlaying()) return;
        if (prepared0 && prepared1) {
            mediaPlayer0.setVolume(1);
            mediaPlayer1.setVolume(0);
            mediaPlayer0.start();
            setUpCrossfade();
        }
    }

    public void stop() {
        mediaPlayer0.setVolume(0);
        mediaPlayer1.setVolume(0);
        if (mediaPlayer0.isPlaying()) mediaPlayer0.pause();
        if (mediaPlayer1.isPlaying()) mediaPlayer1.pause();
        mediaPlayer0.seekTo(0);
        mediaPlayer1.seekTo(0);
        disposables.clear();
    }

    public int getCurTrackDuration() {
        return mediaPlayer0.getDuration();
    }

    public int getNextTrackDuration() {
        return mediaPlayer1.getDuration();
    }

    public String getCurTrackName() {
        String title = metaRetriever0.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) + " - " +
                metaRetriever0.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        return title;
    }

    public String getNextTrackName() {
        String title = metaRetriever1.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) + " - " +
                metaRetriever1.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        return title;
    }

    public boolean isCurTrackLoaded() {
        return prepared0;
    }

    public boolean isNextTrackLoaded() {
        return prepared1;
    }

    public boolean isPlaying() {
        return mediaPlayer0.isPlaying() || mediaPlayer1.isPlaying();
    }

    @SuppressLint("CheckResult")
    private void setUpCrossfade() {

        int initDelay0 = getCurTrackDuration() - crossfadeDuration;
        int period = getCurTrackDuration() + getNextTrackDuration() - crossfadeDuration * 2;
        int initDelay1 = period;
        float volumeStep = 10f / crossfadeDuration;
        Observable.interval(initDelay0, period, TimeUnit.MILLISECONDS)
                .doOnSubscribe(d -> disposables.add(d))
                .subscribe(t -> {
                    mediaPlayer1.start();
                    //zero fades, one rises
                    if (timer0 != null && !timer0.isDisposed()) timer0.dispose();
                    timer0 = Observable.interval(0, 10, TimeUnit.MILLISECONDS)
                            .doOnDispose(() -> Log.d(_TAG, "sound0: disposed"))
                            .takeUntil(a -> mediaPlayer1.getVolume() >= 1f)
                            .subscribe(t1 -> {
                                mediaPlayer0.setVolume(mediaPlayer0.getVolume() - volumeStep);
                                mediaPlayer1.setVolume(mediaPlayer1.getVolume() + volumeStep);
                                Log.d(_TAG, "vol: m0 " + mediaPlayer0.getVolume());
                                Log.d(_TAG, "vol: m1 " + mediaPlayer1.getVolume());
                            });
                    disposables.add(timer0);
                });

        Observable.interval(initDelay1, period, TimeUnit.MILLISECONDS)
                .doOnSubscribe(d -> disposables.add(d))
                .subscribe(t -> {
                    mediaPlayer0.start();
                    //zero rises, one fades
                    if (timer1 != null && !timer1.isDisposed()) timer1.dispose();
                    timer1 = Observable.interval(0, 10, TimeUnit.MILLISECONDS)
                            .doOnDispose(() -> Log.d(_TAG, "sound1: disposed"))
                            .takeUntil(a -> mediaPlayer0.getVolume() >= 1f)
                            .subscribe(t1 -> {
                                mediaPlayer1.setVolume(mediaPlayer1.getVolume() - volumeStep);
                                mediaPlayer0.setVolume(mediaPlayer0.getVolume() + volumeStep);
                                Log.d(_TAG, "vol: m0 " + mediaPlayer0.getVolume());
                                Log.d(_TAG, "vol: m1 " + mediaPlayer1.getVolume());
                            });
                    disposables.add(timer1);
                });

    }

    public void release() {
        mediaPlayer0.release();
        mediaPlayer1.release();
        disposables.dispose();
    }

    public interface TrackLoadedListener {
        void onSuccess();

        void onError(Throwable e);
    }
}
