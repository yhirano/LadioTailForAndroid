/* 
 * Copyright (c) 2011 Y.Hirano
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uraroji.garage.android.ladiotail;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

/**
 * メディア再生サービス
 */
public class MediaPlayService extends Service {

    /**
     * 再生開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED = 0;

    /**
     * 再生完了メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED = 1;

    /**
     * 再生停止メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED = 2;

    /**
     * 再生開始失敗メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START = 3;

    /**
     * Media player
     */
    private MediaPlayer mMediaPlayer;

    /**
     * 再生中のパス。再生していない場合はnull。
     */
    private String mPlayingPath;

    /**
     * Notificationに表示するタイトル。局名や番組名などを入れる。
     */
    private String mNotificationTitle;

    /**
     * Notificationに表示するタイトル。アーティスト名などを入れる。
     */
    private String mNotificationContent;

    /**
     * ロックオブジェクト
     */
    private final Object mlock = new Object();

    /**
     * 再生状態が変化した通知をするコールバックのリスト
     */
    private final RemoteCallbackList<PlayStateChangedCallbackInterface> playStateChangedCallbackList = new RemoteCallbackList<PlayStateChangedCallbackInterface>();

    @Override
    public IBinder onBind(Intent intent) {
        return mInterfaceImpl;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        boolean isPlayed;
        synchronized (mlock) {
            if (mMediaPlayer == null) {
                return;
            }

            isPlayed = mMediaPlayer.isPlaying();
            if (isPlayed == true) {
                mMediaPlayer.stop();
                mPlayingPath = null;
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (isPlayed == true) {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);
        }
    }

    /**
     * 再生を開始する
     * 
     * @param path 再生する音声のパス
     * @param notificationTitle Notificationに表示するタイトル。局名や番組名などを入れる。
     * @param notificationContent Notificationに表示するタイトル。アーティスト名などを入れる。
     */
    private void play(String path, String notificationTitle,
            String notificationContent) {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "trying to play " + path + ".");
        }

        if (path == null) {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START);
            return;
        }

        boolean isPlayed;
        synchronized (mlock) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer
                        .setOnCompletionListener(new OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                synchronized (mlock) {
                                    mMediaPlayer.stop();
                                    mPlayingPath = null;
                                    mNotificationTitle = null;
                                    mNotificationContent = null;
                                    mMediaPlayer.reset();
                                }
                                notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED);
                            }
                        });
            }

            // 同じ番組が再生中の場合は何もしない
            if (mMediaPlayer.isPlaying() == true && mPlayingPath != null
                    && mPlayingPath.equals(path) == true) {
                return;
            }

            isPlayed = mMediaPlayer.isPlaying();

            if (isPlayed == true) {
                mMediaPlayer.stop();
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
                mMediaPlayer.reset();
            }
        }
        if (isPlayed == true) {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);
        }

        boolean isStarted = true;
        synchronized (mlock) {
            try {
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mPlayingPath = path;
                mNotificationTitle = notificationTitle;
                mNotificationContent = notificationContent;
            } catch (IOException e) {
                Log.i(C.TAG, "MediaPlayer occurred IOException(" + e.toString()
                        + ").");
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
                mMediaPlayer.reset();
                isStarted = false;
            }
        }

        if (isStarted == true) {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED);
        } else {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START);
        }
    }

    /**
     * 再生を停止する
     */
    public void stop() {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "trying to stop playing.");
        }

        boolean isPlayed;
        synchronized (mlock) {
            if (mMediaPlayer == null) {
                return;
            }

            isPlayed = mMediaPlayer.isPlaying();

            mMediaPlayer.stop();
            mPlayingPath = null;
            mNotificationTitle = null;
            mNotificationContent = null;
            mMediaPlayer.reset();
        }

        if (isPlayed == true) {
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);
        }
    }

    /**
     * 再生中のパスを取得する
     * 
     * @return 再生中のパス。再生していない場合はnull。
     */
    public String getPlayingPath() {
        synchronized (mlock) {
            return mPlayingPath;
        }
    }

    /**
     * 再生中かを取得する
     * 
     * @return 再生中の場合はtrue、そうでない場合はfalse
     */
    public boolean isPlaying() {
        synchronized (mlock) {
            if (mMediaPlayer == null) {
                return false;
            }

            return mMediaPlayer.isPlaying();
        }
    }

    /**
     * 再生状態が変化したので、登録済みのコールバックを実行する。
     * 
     * @param changedState 変化後の状態
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START
     */
    private void notifyPlayStateChanged(int changedState) {
        // コールバックを実行する
        {
            int n = playStateChangedCallbackList.beginBroadcast();

            for (int i = 0; i < n; ++i) {
                final PlayStateChangedCallbackInterface callback = playStateChangedCallbackList
                        .getBroadcastItem(i);
                if (callback != null) {
                    try {
                        callback.changed(changedState);
                    } catch (RemoteException e) {
                        // 例外はどうしようもないので無視しておく
                        Log.w(C.TAG, "Occurd RemoteException(" + e.toString() + ").");
                    }
                }
            }

            playStateChangedCallbackList.finishBroadcast();
        }

        // Notificationを更新する
        {
            switch (changedState) {
                case MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED: {
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    Notification n = new Notification(R.drawable.icon,
                            mNotificationTitle, System.currentTimeMillis());

                    Intent intent = new Intent(this, ChannelActivity.class);
                    intent.putExtra(
                            ChannelActivity.INTENT_EXTRA_OPEN_CHANNEL_PLAY_URL,
                            mPlayingPath);
                    PendingIntent contentIntent = PendingIntent.getActivity(this,
                            0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
                    n.setLatestEventInfo(this, mNotificationTitle,
                            mNotificationContent, contentIntent);

                    nm.notify(C.NOTIFICATION_ID, n);
                    break;
                }
                case MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED:
                case MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED:
                case MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START: {
                    // 再生中ではないのでNotificationを消す
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.cancel(C.NOTIFICATION_ID);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * メディア再生サービスのインターフェース
     */
    private MediaPlayServiceInterface.Stub mInterfaceImpl = new MediaPlayServiceInterface.Stub() {
        @Override
        public void play(String path, String notificationTitle,
                String notificationContent) throws RemoteException {
            MediaPlayService.this.play(path, notificationTitle,
                    notificationContent);
        }

        @Override
        public void stop() throws RemoteException {
            MediaPlayService.this.stop();
        }

        @Override
        public String getPlayingPath() throws RemoteException {
            return MediaPlayService.this.getPlayingPath();
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return MediaPlayService.this.isPlaying();
        }

        @Override
        public void registerPlayStateChangedCallback(
                PlayStateChangedCallbackInterface callback)
                throws RemoteException {
            playStateChangedCallbackList.register(callback);
        }

        @Override
        public void unregisterPlayStateChangedCallback(
                PlayStateChangedCallbackInterface callback)
                throws RemoteException {
            playStateChangedCallbackList.unregister(callback);
        }
    };
}
