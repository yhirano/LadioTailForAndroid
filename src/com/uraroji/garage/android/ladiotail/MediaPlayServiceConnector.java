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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

/**
 * 再生サービスとの通信クラス
 */
public class MediaPlayServiceConnector {

    /**
     * 再生準備開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PREPARE_STARTED = 0;
    
    /**
     * 再生開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED = 1;

    /**
     * 再生完了メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED = 2;

    /**
     * 再生停止メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED = 3;

    /**
     * 再生開始失敗メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START = 4;

    /**
     * MediaPlayServiceへのインターフェース
     */
    private MediaPlayServiceInterface mMediaPlayServiceInterface;

    /**
     * コンテキスト
     */
    private Context mContext;

    /**
     * 再生状態が変わった際のハンドラーリスト
     * 
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PREPARE_STARTED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
     */
    private ArrayList<Handler> mPlayStateChangedHandlerist = new ArrayList<Handler>();

    /**
     * mPlayStateChangedHandleristのロックオブジェクト
     */
    private final Object mPlayStateChangedHandleristLock = new Object();

    /**
     * 初期化
     * 
     * 一番はじめに再生をする前に初期化すること
     * 
     * @param context コンテキスト。アプリケーションのコンテキストを渡すこと。
     */
    public void init(Context context) {
        this.mContext = context;

        Intent intent = new Intent(MediaPlayServiceInterface.class.getName());
        context.startService(intent); // MeidaPlayサービス開始
        context.bindService(intent, mMediaPlayServiceConn,
                Context.BIND_AUTO_CREATE);
    }

    /**
     * 再生を開始する
     * 
     * @param path 再生する音声のパス
     * @param notificationTitle Notificationに表示するタイトル。局名や番組名などを入れる。
     * @param notificationContent Notificationに表示するタイトル。アーティスト名などを入れる。
     */
    public void play(String path, String notificationTitle,
            String notificationContent) {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "Trying to play " + path + ".");
        }

        try {
            if (mMediaPlayServiceInterface != null) {
                mMediaPlayServiceInterface.play(path, notificationTitle,
                        notificationContent);
            } else {
                Log.w(C.TAG, "Service interface is NULL in play.");
                notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
            }
        } catch (RemoteException e) {
            Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred in play.");
            notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
        }
    }

    /**
     * 再生を停止する
     */
    public void stop() {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "Trying to stop playing.");
        }

        try {
            if (mMediaPlayServiceInterface != null) {
                mMediaPlayServiceInterface.stop();
            } else {
                Log.w(C.TAG, "Service interface is NULL in stop.");
                notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
            }
        } catch (RemoteException e) {
            Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred in stop.");
            notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
        }
    }

    /**
     * 再生に使用したリソースを解放する。 アプリケーションの終了時などにリソースを解放すること。
     * 解放後に再生したい場合、改めてplayを呼べば再生は可能。 その場合は改めてリソース解放をすること。
     */
    public void release() {
        if (C.LOCAL_LOG) {
            Log.v(C.TAG, "Release MediaPlay resouce.");
        }

        final boolean isPlayed = (getPlayingPath() != null);

        mContext.unbindService(mMediaPlayServiceConn);
        // 再生中で無い場合はサービスを止める
        if (isPlayed == false) {
            mContext.stopService(new Intent(MediaPlayServiceInterface.class
                    .getName()));
        }
    }

    /**
     * 再生中のパスを取得する
     * 
     * @return 再生中のパス。再生していない場合はnull。
     */
    public String getPlayingPath() {
        try {
            if (mMediaPlayServiceInterface != null) {
                return mMediaPlayServiceInterface.getPlayingPath();
            } else {
                Log.w(C.TAG, "Service interface is NULL in getPlayingPath.");
                // どうしようもないのでとりあえずnullを返す
                return null;
            }
        } catch (RemoteException e) {
            Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred in getPlayingPath.");
            // どうしようもないのでとりあえずnullを返す
            return null;
        }
    }

    /**
     * 再生状態が変わった際にメッセージが受け取るハンドラーを登録する 再生状態が変わった際には、Handlerのwhatに変更後の状態が格納される。
     * 
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PREPARE_STARTED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED
     * @see MediaPlayServiceConnector#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
     * @param handler 登録するハンドラ
     */
    public void addPlayStateChangedHandler(Handler handler) {
        synchronized (mPlayStateChangedHandleristLock) {
            if (handler != null) {
                mPlayStateChangedHandlerist.add(handler);
            }
        }
    }

    /**
     * 登録済みの再生状態変更ハンドラを削除する
     * 
     * @param handler 削除するハンドラ
     * @see addPlayStateChangedHandler
     */
    public void removePlayStateChangedHandler(Handler handler) {
        synchronized (mPlayStateChangedHandleristLock) {
            mPlayStateChangedHandlerist.remove(handler);
        }
    }

    /**
     * 登録されたハンドラにメッセージを送信する
     * 
     * @param what
     */
    private void notifyPlayStateChanged(int what) {
        for (Handler h : getPlayStateChangedHandleristClone()) {
            if (h != null) {
                h.sendEmptyMessage(what);
            }
        }
    }

    /**
     * 再生状態が変わった際のハンドラーリストのクローンしたリストを取得する。 浅いクローンなので注意。
     * 
     * @return 再生状態が変わった際のハンドラーリストのクローンしたリスト
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Handler> getPlayStateChangedHandleristClone() {
        synchronized (mPlayStateChangedHandleristLock) {
            return (ArrayList<Handler>) mPlayStateChangedHandlerist.clone();
        }
    }

    /**
     * 再生サービスからのコールバック
     */
    PlayStateChangedCallbackInterface remoteCallback = new PlayStateChangedCallbackInterface.Stub() {
        @Override
        public void changed(int changedState) throws RemoteException {
            switch (changedState) {
                case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PREPARE_STARTED:
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_PREPARE_STARTED);
                    break;
                case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED:
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED);
                    break;
                case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED:
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED);
                    break;
                case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED:
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED);
                    break;
                case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START:
                    notifyPlayStateChanged(MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
                    break;
                default:
                    Log.w(C.TAG,
                            "Unknown PlayStateChangedCallbackInterface changedState("
                                    + changedState + ")");
                    break;
            }
        }
    };

    /**
     * サービスへのコネクション
     */
    private ServiceConnection mMediaPlayServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // サービスIFを取得する
            mMediaPlayServiceInterface = MediaPlayServiceInterface.Stub
                    .asInterface(service);
            try {
                mMediaPlayServiceInterface
                        .registerPlayStateChangedCallback(remoteCallback);
            } catch (RemoteException e) {
                // 例外はどうしようもないので無視しておく
                Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                mMediaPlayServiceInterface
                        .unregisterPlayStateChangedCallback(remoteCallback);
            } catch (RemoteException e) {
                // 例外はどうしようもないので無視しておく
                Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred.");
            }
            mMediaPlayServiceInterface = null;
        }
    };
}
