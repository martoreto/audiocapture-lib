package com.github.martoreto.audiocapture;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class MetadataMonitor {
    private static final String TAG = "MetadataMonitor";

    private MediaSessionManager mMediaSessionManager;
    private Handler mHandler = new Handler();
    private Map<MediaSession.Token, SessionInfo> mMediaSessions = new HashMap<>();
    private SessionInfo mActiveSession;
    private List<Callback> mCallbacks = new ArrayList<>();

    public interface Callback {
        void onMetadataChanged(@Nullable String packageName, @Nullable MediaMetadata metadata);
        void onPlaybackStateChanged(@Nullable PlaybackState state);
    }

    private static class SessionInfo {
        MediaController controller;
        MediaMetadata metadata;
        PlaybackState playbackState;
        boolean hasNewMetadata;

        public SessionInfo(MediaController controller) {
            this.controller = controller;
        }
    }

    public MetadataMonitor(Context context, ComponentName componentName) {
        mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        assert mMediaSessionManager != null;
        mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionListener, componentName, mHandler);
        updateActiveSessions(mMediaSessionManager.getActiveSessions(componentName));
    }

    private MediaSessionManager.OnActiveSessionsChangedListener mSessionListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> list) {
            updateActiveSessions(list);
        }
    };

    public void release() {
        mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionListener);
        mCallbacks.clear();
    }

    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void dispatchState() {
        updateState(true);
    }

    private void updateActiveSessions(List<MediaController> list) {
        Set<MediaSession.Token> tokens = new HashSet<>();
        for (MediaController controller: list) {
            MediaSession.Token token = controller.getSessionToken();
            tokens.add(token);
            if (!mMediaSessions.containsKey(token)) {
                Log.d(TAG, "New media controller: " + token);
                SessionInfo sessionInfo = new SessionInfo(controller);
                sessionInfo.metadata = controller.getMetadata();
                sessionInfo.playbackState = controller.getPlaybackState();
                sessionInfo.hasNewMetadata = true;
                controller.registerCallback(new MediaControllerCallback(sessionInfo), mHandler);
                mMediaSessions.put(token, sessionInfo);
            }
        }
        List<MediaSession.Token> toDelete = new ArrayList<>();
        for (MediaSession.Token token: mMediaSessions.keySet()) {
            if (!tokens.contains(token)) {
                toDelete.add(token);
            }
        }
        for (MediaSession.Token token: toDelete) {
            Log.d(TAG, "Removed media controller: " + token);
            mMediaSessions.remove(token);
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {
        private SessionInfo mSessionInfo;

        public MediaControllerCallback(SessionInfo sessionInfo) {
            this.mSessionInfo = sessionInfo;
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "New playback state: " + state);
            mSessionInfo.playbackState = state;
            postUpdateState();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                Log.d(TAG, "New metadata: null");
            } else {
                Log.d(TAG, "New metadata: keys=" + TextUtils.join(",", metadata.keySet()) + ", description: "
                        + metadata.getDescription());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG, "    uri=" + metadata.getDescription().getMediaUri());
                }
            }
            mSessionInfo.metadata = metadata;
            mSessionInfo.hasNewMetadata = true;
            postUpdateState();
        }
    }

    private void postUpdateState() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateState();
            }
        });
    }

    private void updateState() {
        updateState(false);
    }

    private void updateState(boolean fullUpdate) {
        if (mActiveSession != null && !mMediaSessions.containsValue(mActiveSession)) {
            mActiveSession = null;
            fullUpdate = true;
        }

        for (SessionInfo si: mMediaSessions.values()) {
            if (si.playbackState != null && si != mActiveSession && (
                    si.playbackState.getState() == PlaybackState.STATE_PLAYING
                        || (mActiveSession == null && si.playbackState.getState() != PlaybackState.STATE_NONE))) {
                mActiveSession = si;
                fullUpdate = true;
                break;
            }
        }

        if (mActiveSession != null && mActiveSession.hasNewMetadata) {
            fullUpdate = true;
        }

        if (fullUpdate) {
            String packageName;
            MediaMetadata metadata;
            if (mActiveSession == null) {
                packageName = null;
                metadata = null;
            } else {
                packageName = mActiveSession.controller.getPackageName();
                metadata = mActiveSession.metadata;
                mActiveSession.hasNewMetadata = false;
            }
            for (Callback callback: mCallbacks) {
                try {
                    callback.onMetadataChanged(packageName, metadata);
                } catch (Exception e) {
                    Log.w(TAG, "Exception from onMetadataChanged", e);
                }
            }
        }

        for (Callback callback: mCallbacks) {
            try {
                callback.onPlaybackStateChanged(mActiveSession == null ? null : mActiveSession.playbackState);
            } catch (Exception e) {
                Log.w(TAG, "Exception from onPlaybackStateChanged", e);
            }
        }
    }
}
