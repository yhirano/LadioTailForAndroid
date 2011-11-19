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

import android.content.Context;
import android.os.Handler;

/**
 * メディアの再生を管理 複数の画面から共通して再生・停止の管理ができる。
 */
public class MediaPlayManager {

    /**
     * 再生開始メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED = 0;

    /**
     * 再生完了メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED = 1;

    /**
     * 再生停止メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED = 2;

    /**
     * 再生開始失敗メッセージ
     */
    public static final int MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START = 3;

    /**
     * 再生・停止の実行モジュール
     */
    private static MediaPlayInterface mMediaPlay = ((C.MEDIA_PLAY_AT_SERVICE) ? (new MediaPlayAtService())
            : (new MediaPlayAtLocal()));

    /**
     * コンストラクタ シングルトンなのでprivateとする
     */
    private MediaPlayManager() {
    }

    /**
     * 初期化 一番はじめに再生をする前に初期化すること
     * 
     * @param context コンテキスト。アプリケーションのコンテキストを渡すこと。
     */
    public static void init(Context context) {
        mMediaPlay.init(context);
    }

    /**
     * 再生を開始する
     * 
     * @param path 再生する音声のパス
     * @param notificationTitle Notificationに表示するタイトル。局名や番組名などを入れる。
     * @param notificationContent Notificationに表示するタイトル。アーティスト名などを入れる。
     */
    public static void play(String path, String notificationTitle,
            String notificationContent) {
        mMediaPlay.play(path, ((notificationTitle != null) ? notificationTitle
                : ""), ((notificationContent != null) ? notificationContent
                : ""));
    }

    /**
     * 再生を停止する
     */
    public static void stop() {
        mMediaPlay.stop();
    }

    /**
     * 再生に使用したリソースを解放する。 アプリケーションの終了時などにリソースを解放すること。
     */
    public static void release() {
        mMediaPlay.release();
    }

    /**
     * 再生中のパスを取得する
     * 
     * @return 再生中のパス。再生していない場合はnull。
     */
    public static String getPlayingPath() {
        return mMediaPlay.getPlayingPath();
    }

    /**
     * 再生中かを取得する
     * 
     * @return 再生中の場合はtrue、そうでない場合はfalse
     */
    public static boolean isPlaying() {
        return mMediaPlay.isPlaying();
    }

    /**
     * 再生状態が変わった際にメッセージが受け取るハンドラーを登録する 再生状態が変わった際には、Handlerのwhatに変更後の状態が格納される。
     * 
     * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED
     * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED
     * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED
     * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
     * @param handler 登録するハンドラ
     */
    public static void addPlayStateChangedHandler(Handler handler) {
        mMediaPlay.addPlayStateChangedHandler(handler);
    }

    /**
     * 登録済みの再生状態変更ハンドラを削除する
     * 
     * @param handler 削除するハンドラ
     * @see MediaPlayManager#addPlayStateChangedHandler
     */
    public static void removePlayStateChangedHandler(Handler handler) {
        mMediaPlay.removePlayStateChangedHandler(handler);
    }
}
