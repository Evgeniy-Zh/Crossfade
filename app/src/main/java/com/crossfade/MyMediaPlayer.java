package com.crossfade;

import android.media.MediaPlayer;


public class MyMediaPlayer extends MediaPlayer {
    private float volumeLeft;
    private float volumeRight;
    private float volume;

    public void setVolume(float volume) {
        this.volume = volume;
        setVolume(volume, volume);
    }

    public float getVolumeLeft() {
        return volumeLeft;
    }

    public float getVolumeRight() {
        return volumeRight;
    }

    public float getVolume() {
        return volume;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        super.setVolume(leftVolume, rightVolume);
        volumeLeft = leftVolume;
        volumeRight = rightVolume;
    }
}
