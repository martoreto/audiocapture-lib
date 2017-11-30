package com.github.martoreto.audiocapture;

// Declare any non-default types here with import statements
import android.os.ParcelFileDescriptor;

oneway interface IAudioCaptureCallback {
    void onChannelReady(in ParcelFileDescriptor fd);
    void onStop();
}
