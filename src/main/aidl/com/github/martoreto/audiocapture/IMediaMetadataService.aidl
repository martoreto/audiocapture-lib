package com.github.martoreto.audiocapture;

// Declare any non-default types here with import statements
import com.github.martoreto.audiocapture.IMediaMetadataCallback;

interface IMediaMetadataService {
    void registerCallback(IMediaMetadataCallback callback);
    void unregisterCallback(IMediaMetadataCallback callback);
}
