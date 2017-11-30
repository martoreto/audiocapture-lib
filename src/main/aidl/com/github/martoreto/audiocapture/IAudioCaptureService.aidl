package com.github.martoreto.audiocapture;

// Declare any non-default types here with import statements
import com.github.martoreto.audiocapture.IAudioCaptureCallback;

interface IAudioCaptureService {
    void startAudioCapture(IAudioCaptureCallback callback);
    void stopAudioCapture(IAudioCaptureCallback callback);
}
