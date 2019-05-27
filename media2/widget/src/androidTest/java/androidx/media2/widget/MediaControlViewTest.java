/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media2.widget;

import static android.content.Context.KEYGUARD_SERVICE;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.UriMediaItem;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaControlView}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControlViewTest {
    private static final String TAG = "MediaControlViewTest";
    // Expected success time
    private static final int WAIT_TIME_MS = 1000;
    private static final long FFWD_MS = 30000L;
    private static final long REW_MS = 10000L;

    private Context mContext;
    private Executor mMainHandlerExecutor;
    private Instrumentation mInstrumentation;

    private Activity mActivity;
    private VideoView mVideoView;
    private Uri mFileSchemeUri;
    private MediaItem mFileSchemeMediaItem;
    private List<MediaController> mControllers = new ArrayList<>();

    @Rule
    public ActivityTestRule<MediaControlViewTestActivity> mActivityRule =
            new ActivityTestRule<>(MediaControlViewTestActivity.class);

    @Before
    public void setup() throws Throwable {
        mContext = ApplicationProvider.getApplicationContext();
        mMainHandlerExecutor = ContextCompat.getMainExecutor(mContext);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mFileSchemeUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_file_scheme_video);
        mFileSchemeMediaItem = createTestMediaItem2(mFileSchemeUri);

        setKeepScreenOn();
        checkAttachedToWindow();
    }

    @After
    public void tearDown() throws Throwable {
        for (int i = 0; i < mControllers.size(); i++) {
            mControllers.get(i).close();
        }
    }

    @UiThreadTest
    @Test
    public void testConstructor() {
        new MediaControlView(mActivity);
        new MediaControlView(mActivity, null);
        new MediaControlView(mActivity, null, 0);
    }

    @Test
    public void testPlayPauseButtonClick() throws Throwable {
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController controller,
                            int state) {
                        if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                            latchForPausedState.countDown();
                        } else if (state == SessionPlayer.PLAYER_STATE_PLAYING) {
                            latchForPlayingState.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mFileSchemeMediaItem);
            }
        });
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForPlayingState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFfwdButtonClick() throws Throwable {
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForFfwd = new CountDownLatch(1);
        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    @Override
                    public void onSeekCompleted(@NonNull MediaController controller,
                            long position) {
                        if (position >= FFWD_MS) {
                            latchForFfwd.countDown();
                        }
                    }

                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController controller,
                            int state) {
                        if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                            latchForPausedState.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mFileSchemeMediaItem);
            }
        });
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.ffwd), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRewButtonClick() throws Throwable {
        final CountDownLatch latchForFfwd = new CountDownLatch(1);
        final CountDownLatch latchForRew = new CountDownLatch(1);
        createController(new MediaController.ControllerCallback() {
            long mExpectedPosition = FFWD_MS;
            final long mDelta = 1000L;

            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller,
                    int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    mExpectedPosition = FFWD_MS;
                    controller.seekTo(mExpectedPosition);
                }
            }

            @Override
            public void onSeekCompleted(@NonNull MediaController controller,
                    long position) {
                // Ignore the initial seek. Internal MediaPlayer behavior can be changed.
                if (position == 0 && mExpectedPosition == FFWD_MS) {
                    return;
                }
                assertTrue(equalsSeekPosition(mExpectedPosition, position, mDelta));
                if (mExpectedPosition == FFWD_MS) {
                    mExpectedPosition = position - REW_MS;
                    latchForFfwd.countDown();
                } else {
                    latchForRew.countDown();
                }
            }

            private boolean equalsSeekPosition(long expected, long actual, long delta) {
                return (actual < expected + delta) && (actual > expected - delta);
            }
        });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mFileSchemeMediaItem);
            }
        });
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.rew), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForRew.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetMetadataForNonMusicFile() throws Throwable {
        final long duration = 49056L;
        final String title = "BigBuckBunny";
        final CountDownLatch durationLatch = new CountDownLatch(1);
        final CountDownLatch titleLatch = new CountDownLatch(1);
        final MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title).build();
        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                            @Nullable MediaItem item) {
                        if (item != null) {
                            MediaMetadata metadata = item.getMetadata();
                            if (metadata != null) {
                                if (metadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                                    assertEquals(title, metadata.getString(
                                            MediaMetadata.METADATA_KEY_TITLE));
                                    titleLatch.countDown();
                                }
                                if (metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
                                    assertEquals(duration, metadata.getLong(
                                            MediaMetadata.METADATA_KEY_DURATION));
                                    durationLatch.countDown();
                                }
                            }
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mFileSchemeMediaItem);
            }
        });
        assertTrue(durationLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        mFileSchemeMediaItem.setMetadata(metadata);
        assertTrue(titleLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testButtonVisibilityForMusicFile() throws Throwable {
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_music);
        final MediaItem uriMediaItem = createTestMediaItem2(uri);

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    @Override
                    public void onTrackInfoChanged(@NonNull MediaController controller,
                            @NonNull List<TrackInfo> trackInfos) {
                        latch.countDown();
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(uriMediaItem);
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testUpdateAndSelectSubtitleTrack() throws Throwable {
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.testvideo_with_2_subtitle_tracks);

        final int subtitleTrackCount = 2;
        final String subtitleTrackOffText = mContext.getResources().getString(
                R.string.MediaControlView_subtitle_off_text);
        final String subtitleTrack1Text = mContext.getResources().getString(
                R.string.MediaControlView_subtitle_track_number_text, 1);

        final MediaItem mediaItem = createTestMediaItem2(uri);

        final CountDownLatch latchForTrackUpdate = new CountDownLatch(1);
        final CountDownLatch latchForSubtitleSelect = new CountDownLatch(1);
        final CountDownLatch latchForSubtitleDeselect = new CountDownLatch(1);
        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    private List<TrackInfo> mTrackInfos;
                    private int mFirstSubtitleIndex = -1;

                    @Override
                    public void onTrackInfoChanged(@NonNull MediaController controller,
                            @NonNull List<TrackInfo> trackInfos) {
                        int subtitleCount = 0;
                        int firstSubtitleIndex = -1;
                        for (int i = 0; i < trackInfos.size(); i++) {
                            if (trackInfos.get(i).getTrackType()
                                    == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                                if (firstSubtitleIndex == -1) {
                                    firstSubtitleIndex = i;
                                }
                                subtitleCount++;
                            }
                        }
                        if (subtitleCount == subtitleTrackCount) {
                            mFirstSubtitleIndex = firstSubtitleIndex;
                            mTrackInfos = trackInfos;
                            latchForTrackUpdate.countDown();
                        }
                    }

                    @Override
                    public void onTrackSelected(@NonNull MediaController controller,
                            @NonNull TrackInfo trackInfo) {
                        assertEquals(mTrackInfos.get(mFirstSubtitleIndex), trackInfo);
                        latchForSubtitleSelect.countDown();
                    }

                    @Override
                    public void onTrackDeselected(@NonNull MediaController controller,
                            @NonNull TrackInfo trackInfo) {
                        assertEquals(mTrackInfos.get(mFirstSubtitleIndex), trackInfo);
                        latchForSubtitleDeselect.countDown();
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mediaItem);
            }
        });
        controller.play();
        assertTrue(latchForTrackUpdate.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        onView(withId(R.id.subtitle)).check(matches(isClickable()));
        onView(withId(R.id.subtitle)).perform(click());
        onView(withText(subtitleTrack1Text)).inRoot(isPlatformPopup())
                .check(matches(isCompletelyDisplayed()));
        onView(withText(subtitleTrack1Text)).inRoot(isPlatformPopup()).perform(click());
        assertTrue(latchForSubtitleSelect.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        onView(withId(R.id.subtitle)).check(matches(isClickable()));
        onView(withId(R.id.subtitle)).perform(click());
        onView(withText(subtitleTrackOffText)).inRoot(isPlatformPopup())
                .check(matches(isCompletelyDisplayed()));
        onView(withText(subtitleTrackOffText)).inRoot(isPlatformPopup()).perform(click());
        assertTrue(latchForSubtitleDeselect.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCheckMediaItemIsFromHttp() throws Throwable {
        testCheckMediaItemIsFromNetwork(Uri.parse("http://localhost/dummy.mp4"), true);
    }

    @Test
    public void testCheckMediaItemIsFromHttps() throws Throwable {
        testCheckMediaItemIsFromNetwork(Uri.parse("https://localhost/dummy.mp4"), true);
    }

    @Test
    public void testCheckMediaItemIsFromRtsp() throws Throwable {
        testCheckMediaItemIsFromNetwork(Uri.parse("rtsp://localhost/dummy.mp4"), true);
    }

    @Test
    public void testCheckMediaItemIsFromFile() throws Throwable {
        testCheckMediaItemIsFromNetwork(Uri.parse("file:///dummy.mp4"), false);
    }

    @Test
    public void testFullScreenListener() throws Throwable {
        onView(withId(R.id.fullscreen)).check(matches(not(isDisplayed())));

        final CountDownLatch latchOn = new CountDownLatch(1);
        final CountDownLatch latchOff = new CountDownLatch(1);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.getMediaControlView().setOnFullScreenListener(
                        new MediaControlView.OnFullScreenListener() {
                            @Override
                            public void onFullScreen(@NonNull View view, boolean fullScreen) {
                                if (fullScreen) {
                                    latchOn.countDown();
                                } else {
                                    latchOff.countDown();
                                }
                            }
                        });
            }
        });
        onView(withId(R.id.fullscreen)).check(matches(isCompletelyDisplayed()));

        onView(withId(R.id.fullscreen)).perform(click());
        assertTrue(latchOn.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.fullscreen)).perform(click());
        assertTrue(latchOff.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.getMediaControlView().setOnFullScreenListener(null);
            }
        });
        onView(withId(R.id.fullscreen)).check(matches(not(isDisplayed())));
    }

    private void testCheckMediaItemIsFromNetwork(Uri uri, boolean isNetwork) throws Throwable {
        final MediaItem mediaItem = createTestMediaItem2(uri);
        final CountDownLatch latch = new CountDownLatch(1);

        final MediaController controller =
                createController(new MediaController.ControllerCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                            @Nullable MediaItem item) {
                        if (item == mediaItem) {
                            latch.countDown();
                        }
                    }
                });

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem(mediaItem);
            }
        });
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(mVideoView.getMediaControlView().isCurrentMediaItemFromNetwork(), isNetwork);
    }

    private void setKeepScreenOn() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 27) {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mActivity.setTurnScreenOn(true);
                    mActivity.setShowWhenLocked(true);
                    KeyguardManager keyguardManager = (KeyguardManager)
                            mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(mActivity, null);
                } else {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void checkAttachedToWindow() throws Exception {
        if (!mVideoView.isAttachedToWindow()) {
            final CountDownLatch latch = new CountDownLatch(1);
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    latch.countDown();
                }
                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            };
            mVideoView.addOnAttachStateChangeListener(listener);
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }

    private MediaItem createTestMediaItem2(Uri uri) {
        return new UriMediaItem.Builder(uri).build();
    }

    private MediaController createController(MediaController.ControllerCallback callback) {
        MediaController controller = new MediaController.Builder(mVideoView.getContext())
                .setSessionToken(mVideoView.getSessionToken())
                .setControllerCallback(mMainHandlerExecutor, callback)
                .build();
        mControllers.add(controller);
        return controller;
    }
}
