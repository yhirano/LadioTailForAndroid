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
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
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
     * 再生準備開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PREPARE_STARTED = 0;
    
    /**
     * 再生開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED = 1;

    /**
     * 再生完了メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED = 2;

    /**
     * 再生停止メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED = 3;

    /**
     * 再生開始失敗メッセージ
     */
    public static final int MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START = 4;

    /**
     * 再生状態・停止中
     */
    public static final int PLAY_STATE_IDLE = 0;
    
    /**
     * 再生状態・準備中
     */
    public static final int PLAY_STATE_PREPARE = 1;

    /**
     * 再生状態・再生中
     */
    public static final int PLAY_STATE_PLAYING = 2;

    /**
     * Media player
     */
    private MediaPlayer mMediaPlayer;

    /**
     * 準備中・再生中のパス。停止中の場合はnull。
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
     * 再生状態
     */
    private PlayState mPlayState = new IdleState();

    /**
     * ロックオブジェクト
     */
    private final Object mLock = new Object();

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
        synchronized (mLock) {
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

        synchronized (mLock) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer
                        .setOnCompletionListener(new OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                synchronized (mLock) {
                                    mp.stop();
                                    mp.setOnPreparedListener(null);
                                    mPlayingPath = null;
                                    mNotificationTitle = null;
                                    mNotificationContent = null;
                                    mp.reset();
                                }
                                notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED);
                            }
                        });
            }
        }
        
        mPlayState.play(path, notificationTitle, notificationContent);
    }

    /**
     * 再生を停止する
     */
    public void stop() {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "trying to stop playing.");
        }

        mPlayState.stop();
    }

    /**
     * 準備中・再生中のパスを取得する
     * 
     * @return 準備中・再生中のパス。停止中の場合はnull。
     */
    public String getPlayingPath() {
        synchronized (mLock) {
            return mPlayingPath;
        }
    }

    /**
     * 再生状態を取得する
     * 
     * @return 再生状態
     * @see MediaPlayService#PLAY_STATE_IDLE
     * @see MediaPlayService#PLAY_STATE_PREPARE
     * @see MediaPlayService#PLAY_STATE_PLAYING
     */
    public int getPlayState() {
        return mPlayState.getPlayState();
    }

    /**
     * 再生状態が変化したので、登録済みのコールバックを実行する。
     * 
     * @param changedState 変化後の状態
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PREPARE_STARTED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED
     * @see MediaPlayService#MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START
     */
    private void notifyPlayStateChanged(int changedState) {
        // コールバックを実行する
        execCallback(changedState);

        // Notificationを更新する
        updateNotification(changedState);
    }

    /**
     * コールバックを実行する
     * 
     * @param changedState 変化後の状態
     */
    private void execCallback(int changedState) {
        synchronized (playStateChangedCallbackList) {
            final int n = playStateChangedCallbackList.beginBroadcast();

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
    }

    /**
     * Notificationを更新する
     * 
     * @param changedState 変化後の状態
     */
    private void updateNotification(int changedState) {
        switch (changedState) {
            case MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED: {
                // Android 2.2以下と2.3以上でステータスバーに表示するアイコンを分ける
                final int iconId = ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                        ? R.drawable.ic_stat_2_3 : R.drawable.ic_stat_2_2);

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification n = new Notification(iconId, mNotificationTitle,
                        System.currentTimeMillis());

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
            case MSG_MEDIA_PLAY_SERVICE_PREPARE_STARTED:
            case MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED:
            case MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED:
            case MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START:
                // 再生中ではないのでNotificationを消す
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.cancel(C.NOTIFICATION_ID);
                break;
            default:
                break;
        }
    }

    /**
     * 再生状態を変更する
     * 
     * @param nextState 次の状態
     */
    private void changeState(PlayState nextState) {
        mPlayState = nextState;
        mPlayState.init();
    }
    
    /**
     * 再生状態を表すインターフェース
     */
    private interface PlayState {
        /**
         * 初期化
         */
        public void init();
        
        /**
         * 再生開始
         * 
         * @param path 再生する音声のパス
         * @param notificationTitle Notificationに表示するタイトル。局名や番組名などを入れる。
         * @param notificationContent Notificationに表示するタイトル。アーティスト名などを入れる。
         */
        public void play(String path, String notificationTitle,
                String notificationContent);
        
        /**
         * 停止
         */
        public void stop();

        /**
         * 再生状態を取得する
         * 
         * @return 再生状態
         * @see MediaPlayService#PLAY_STATE_IDLE
         * @see MediaPlayService#PLAY_STATE_PREPARE
         * @see MediaPlayService#PLAY_STATE_PLAYING
         */
        public int getPlayState();
    }
    
    /**
     * 停止中状態
     */
    private class IdleState implements PlayState {

        @Override
        public void init() {
        }

        @Override
        public void play(String path, String notificationTitle,
                String notificationContent) {
            changeState(new PrepareState(path, notificationTitle, notificationContent));
        }

        @Override
        public void stop() {
        }

        @Override
        public int getPlayState() {
            return PLAY_STATE_IDLE;
        }
    }
    
    /**
     * 準備中状態
     */
    private class PrepareState implements PlayState {
        
        private String mmPath;
        private String mmNotificationTitle;
        private String mmNotificationContent;
        
        public PrepareState(String path, String notificationTitle,
                String notificationContent) {
            mmPath = path;
            mmNotificationTitle = notificationTitle;
            mmNotificationContent = notificationContent;
        }
        
        @Override
        public void init() {
            synchronized (mLock) {
                try {
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PREPARE_STARTED);
                    mMediaPlayer.setDataSource(mmPath);
                    mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {

                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            synchronized (mLock) {
                                mMediaPlayer.start();
                                mNotificationTitle = mmNotificationTitle;
                                mNotificationContent = mmNotificationContent;
                            }
                            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED);
                            changeState(new PlayingState());
                        }
                    });
                    mMediaPlayer.prepareAsync();
                    mPlayingPath = mmPath;
                } catch (IOException e) {
                    Log.i(C.TAG, "MediaPlayer occurred IOException(" + e.toString()
                            + ").");
                    mPlayingPath = null;
                    mNotificationTitle = null;
                    mNotificationContent = null;
                    mMediaPlayer.reset();
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START);
                    changeState(new IdleState());
                }
            }
        }

        @Override
        public void play(String path, String notificationTitle,
                String notificationContent) {
            // 同じ番組が再生中の場合は何もしない
            if (mPlayingPath != null && mPlayingPath.equals(path) == true) {
                return;
            }
            synchronized (mLock) {
                try {
                    mMediaPlayer.setDataSource(new String());
                } catch (Exception e) {
                    mMediaPlayer.reset();
                }
                mMediaPlayer.setOnPreparedListener(null);
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
            }
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);

            changeState(new PrepareState(path, notificationTitle, notificationContent));
        }

        @Override
        public void stop() {
            synchronized (mLock) {
                mMediaPlayer.stop();
                mMediaPlayer.setOnPreparedListener(null);
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
                mMediaPlayer.reset();
            }
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);

            changeState(new IdleState());
        }

        @Override
        public int getPlayState() {
            return PLAY_STATE_PREPARE;
        }
    }

    /**
     * 再生中状態
     */
    private class PlayingState implements PlayState {
        @Override
        public void init() {
        }

        @Override
        public void play(String path, String notificationTitle,
                String notificationContent) {
            // 同じ番組が再生中の場合は何もしない
            if (mPlayingPath != null && mPlayingPath.equals(path) == true) {
                return;
            }
            
            synchronized (mLock) {
                mMediaPlayer.stop();
                mMediaPlayer.setOnPreparedListener(null);
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
                mMediaPlayer.reset();
            }
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);

            changeState(new PrepareState(path, notificationTitle, notificationContent));
        }

        @Override
        public void stop() {
            synchronized (mLock) {
                mMediaPlayer.stop();
                mMediaPlayer.setOnPreparedListener(null);
                mPlayingPath = null;
                mNotificationTitle = null;
                mNotificationContent = null;
                mMediaPlayer.reset();
            }
            notifyPlayStateChanged(MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED);

            changeState(new IdleState());
        }

        @Override
        public int getPlayState() {
            return PLAY_STATE_PLAYING;
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
        public int getPlayState() throws RemoteException {
            return MediaPlayService.this.getPlayState();
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
