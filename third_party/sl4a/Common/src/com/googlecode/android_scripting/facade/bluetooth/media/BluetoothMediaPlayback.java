package com.googlecode.android_scripting.facade.bluetooth.media;

import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;

//import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothMediaFacade;
import com.googlecode.android_scripting.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is a UI-less MediaPlayer that is used in testing Bluetooth Media related test cases.
 *
 * This class handles media playback commands coming from the MediaBrowserService.
 * This is responsible for dealing with getting the media content and creating a MediaPlayer
 * on the MediaBrowserService's MediaSession.
 * This codepath would be exercised an an Audio source (Phone).
 *
 * The nested MusicProvider utility class takes care of reading the media files and maintaining
 * the Playing Queue.  It expects the media files to have been pushed to /sdcard/Music/test
 */

public class BluetoothMediaPlayback {
    private MediaPlayer mMediaPlayer = null;
    private MediaSession playbackSession = null;
    private MusicProvider musicProvider = null;
    private int queueIndex;
    private static final String TAG = "BluetoothMediaPlayback";
    private int mState;
    private long mCurrentPosition = 0;

    // Passing in the Resources
    public BluetoothMediaPlayback() {
        queueIndex = 0;
        musicProvider = new MusicProvider();
        mState = PlaybackState.STATE_NONE;
    }

    /**
     * MediaPlayer Callback for Completion. Used to move to the next track.
     */
    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    queueIndex++;
                    // If we were playing the last item in the Queue, reset back to the first
                    // item.
                    if (queueIndex >= musicProvider.getNumberOfItemsInQueue()) {
                        queueIndex = 0;
                    }
                    mCurrentPosition = 0;
                    play();
                }
            };

    /**
     * MediaPlayer Callback for Error Handling
     */
    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG + " MediaPlayer Error " + what);
            // Release the resources
            mMediaPlayer.stop();
            releaseMediaPlayer();
            mMediaPlayer.release();
            mMediaPlayer = null;
            return false;
        }
    };

    /**
     * Build & Return the AudioAtrributes for the MediaPlayer.
     *
     * @return {@link AudioAttributes}
     */
    private AudioAttributes createAudioAttributes(int contentType, int usage) {
        AudioAttributes.Builder builder = new AudioAttributes.Builder();
        return builder.setContentType(contentType).setUsage(usage).build();
    }

    /**
     * Update the Current Playback State on the Media Session
     *
     * @param state - the state to set to.
     */
    private void updatePlaybackState(int state) {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
        Log.d(TAG + " Update Playback Status Curr Posn: " + mCurrentPosition);
        stateBuilder.setState(state, mCurrentPosition, 1.0f, SystemClock.elapsedRealtime());
        stateBuilder.setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        playbackSession.setPlaybackState(stateBuilder.build());
    }

    /**
     * The core method that handles loading the media file from the raw resources
     * and sets up and prepares the MediaPlayer to play the file.
     *
     * @param newTrack - the MediaMetadata to update the MediaSession with.
     */
    private void handlePlayMedia(MediaMetadata newTrack) {
        createMediaPlayerIfNeeded();
        // Updates the MediaBrowserService's MediaSession's metadata
        playbackSession.setMetadata(newTrack);
        String url = newTrack.getString(MusicProvider.CUSTOM_URL);
        try {
            mMediaPlayer.setDataSource(
                    BluetoothSL4AAudioSrcMBS.getAvrcpMediaBrowserService().getApplicationContext(),
                    Uri.parse(url));
            mMediaPlayer.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.d(TAG + " MediaPlayer Start");
        mMediaPlayer.start();
    }

    /**
     * Sets the MediaSession to operate on
     */
    public void setMediaSession(MediaSession session) {
        playbackSession = session;
    }

    /**
     * Create MediaPlayer on demand if necessary.
     * It also sets the appropriate callbacks for Completion and Error Handling
     */
    public void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Release the current Media Player
     */
    public void releaseMediaPlayer() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    /**
     * Sets the Volume for the MediaSession
     */
    public void setVolume(float leftVolume, float rightVolume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    /**
     * Gets the item to play from the MusicProvider's PlayQueue
     * Also dispatches a "I received a Play Command" acknowledgement through the Facade.
     */
    public void play() {
        Log.d(TAG + " play queIndex: " + queueIndex);
        BluetoothMediaFacade.dispatchPlaybackStateChanged(PlaybackState.STATE_PLAYING);
        MediaMetadata newMetaData = musicProvider.getItemToPlay(queueIndex);
        if (newMetaData == null) {
            //Error logged in getItemToPlay already.
            return;
        }
        handlePlayMedia(newMetaData);
        updatePlaybackState(PlaybackState.STATE_PLAYING);

    }

    /**
     * Gets the currently playing MediaItem to pause
     * Also dispatches a "I received a Pause Command" acknowledgement through the Facade.
     */
    public void pause() {
        BluetoothMediaFacade.dispatchPlaybackStateChanged(PlaybackState.STATE_PAUSED);
        if (mMediaPlayer == null) {
            Log.d(TAG + " MediaPlayer not yet created.");
            return;
        }
        mMediaPlayer.pause();
        // Cache the current position to use when play resumes
        mCurrentPosition = mMediaPlayer.getCurrentPosition();
        updatePlaybackState(PlaybackState.STATE_PAUSED);
    }

    /**
     * Skips to the next item in the MusicProvider's PlayQueue
     * Also dispatches a "I received a SkipNext Command" acknowledgement through the Facade
     */
    public void skipNext() {
        BluetoothMediaFacade.dispatchPlaybackStateChanged(PlaybackState.STATE_SKIPPING_TO_NEXT);
        queueIndex++;
        if (queueIndex >= musicProvider.getNumberOfItemsInQueue()) {
            queueIndex = 0;
        }
        Log.d(TAG + " skipNext queIndex: " + queueIndex);
        MediaMetadata newMetaData = musicProvider.getItemToPlay(queueIndex);
        if (newMetaData == null) {
            //Error logged in getItemToPlay already.
            return;
        }
        mCurrentPosition = 0;
        handlePlayMedia(newMetaData);

    }

    /**
     * Skips to the previous item in the MusicProvider's PlayQueue
     * Also dispatches a "I received a SkipPrev Command" acknowledgement through the Facade.
     */

    public void skipPrev() {
        BluetoothMediaFacade.dispatchPlaybackStateChanged(PlaybackState.STATE_SKIPPING_TO_PREVIOUS);
        queueIndex--;
        if (queueIndex < 0) {
            queueIndex = 0;
        }
        Log.d(TAG + " skipPrev queIndex: " + queueIndex);
        MediaMetadata newMetaData = musicProvider.getItemToPlay(queueIndex);
        if (newMetaData == null) {
            //Error logged in getItemToPlay already.
            return;
        }
        mCurrentPosition = 0;
        handlePlayMedia(newMetaData);

    }

    /**
     * Resets and releases the MediaPlayer
     */

    public void stop() {
        queueIndex = 0;
        releaseMediaPlayer();
        updatePlaybackState(PlaybackState.STATE_STOPPED);
    }


    /**
     * Utility Class to abstract retrieving and providing Playback with the appropriate MediaFile
     * This looks for Media files used for the test to be present in /sdcard/Music/test directory
     * It is the responsibility of the client side to push the media files to the above directory
     * before or as part of the test.
     */
    private class MusicProvider {
        List<String> mediaFilesPath;
        HashMap musicResources;
        public static final String CUSTOM_URL = "__MUSIC_URL__";
        private static final String TAG = "BluetoothMediaMusicProvider";
        // The test samples for the test is expected to be in the /sdcard/Music/test directory
        private static final String MEDIA_TEST_PATH = "/Music/test";

        public MusicProvider() {
            mediaFilesPath = new ArrayList<String>();
            // Get the Media file names from the Music directory
            List<String> mediaFileNames = new ArrayList<String>();
            String musicPath =
                    Environment.getExternalStorageDirectory().toString() + MEDIA_TEST_PATH;
            File musicDir = new File(musicPath);
            if (musicDir != null) {
                if (musicDir.listFiles() != null) {
                    for (File f : musicDir.listFiles()) {
                        if (f.isFile()) {
                            mediaFileNames.add(f.getName());
                        }
                    }
                }
                musicResources = new HashMap();
                // Extract the metadata from the media files and build a hashmap
                // of <filename, mediametadata> called musicResources.
                for (String song : mediaFileNames) {
                    String songPath = musicPath + "/" + song;
                    mediaFilesPath.add(songPath);
                    Log.d(TAG + " Retrieving Meta Data for " + songPath);
                    MediaMetadata track = retrieveMetaData(songPath);
                    musicResources.put(songPath, track);
                }
                Log.d(TAG + "MusicProvider Num of Songs : " + mediaFilesPath.size());
            } else {
                Log.e(TAG + " No media files found");
            }
        }

        /**
         * Opens the Media File from the resources and retrieves the Metadata information
         *
         * @param song - the resource path of the file
         * @return {@link MediaMetadata} corresponding to the media file loaded.
         */
        private MediaMetadata retrieveMetaData(String song) {
            MediaMetadata.Builder newMetaData = new MediaMetadata.Builder();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(
                    BluetoothSL4AAudioSrcMBS.getAvrcpMediaBrowserService().getApplicationContext(),
                    Uri.parse(song));

            // Extract from the mediafile and build the MediaMetadata
            newMetaData.putString(MediaMetadata.METADATA_KEY_TITLE,
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            Log.d(TAG + " Retriever : " + retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_TITLE));
            newMetaData.putString(MediaMetadata.METADATA_KEY_ALBUM,
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            newMetaData.putString(MediaMetadata.METADATA_KEY_ARTIST,
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            newMetaData.putLong(MediaMetadata.METADATA_KEY_DURATION, Long.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
            newMetaData.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, Long.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)));
            //newMetaData.putLong(CUSTOM_MUSIC_PROVIDER_RESOURCE_ID, resourceId);
            newMetaData.putString(CUSTOM_URL, song);
            return newMetaData.build();

        }

        /**
         * Returns the MediaMetadata of the song that corresponds to the index in the Queue.
         *
         * @return {@link MediaMetadata}
         */
        public MediaMetadata getItemToPlay(int queueIndex) {
            // We have 2 data structures in this utility class -
            // 1. A String List called mediaFilesPath - holds the file names (incl path) of the
            // media files
            // 2. A hashmap called musicResources that has been built where the keys are from
            // the List mediaFilesPath above and the values are the corresponding extracted
            // MediaMetadata.
            // mediaFilesPath doubles up as the Playing Queue.  The index that is passed here
            // is used to retrieve the filename which is then keyed into the musicResources
            // to return the MediaMetadata.
            if (mediaFilesPath.size() == 0) {
                Log.e(TAG + " No Media to play");
                return null;
            }
            String song = mediaFilesPath.get(queueIndex);
            MediaMetadata track = (MediaMetadata) musicResources.get(song);
            return track;
        }

        /**
         * Number of items we have in the Play Queue
         *
         * @return Number of items.
         */
        public int getNumberOfItemsInQueue() {
            return musicResources.size();
        }

    }
}