package com.github.martoreto.audiocapture;

// Declare any non-default types here with import statements
import android.media.MediaMetadata;
import android.media.session.PlaybackState;

oneway interface IMediaMetadataCallback {
    void onMetadataChanged(String packageName, in MediaMetadata metadata);
    void onPlaybackStateChanged(in PlaybackState state);
}
