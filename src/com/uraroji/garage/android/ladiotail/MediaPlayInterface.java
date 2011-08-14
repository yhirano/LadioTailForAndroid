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
 * メディア再生のインターフェース
 */
public interface MediaPlayInterface {

	/**
	 * 初期化
	 * 
	 * 一番はじめに再生をする前に初期化すること
	 * 
	 * @param context
	 *            コンテキスト。アプリケーションのコンテキストを渡すこと。
	 */
	public void init(Context context);

	/**
	 * 再生を開始する
	 * 
	 * @param path
	 *            再生する音声のパス
	 * @param notificationTitle
	 *            Notificationに表示するタイトル。局名や番組名などを入れる。
	 * @param notificationContent
	 *            Notificationに表示するタイトル。アーティスト名などを入れる。
	 */
	public void play(String path, String notificationTitle,
			String notificationContent);

	/**
	 * 再生を停止する
	 */
	public void stop();

	/**
	 * 再生に使用したリソースを解放する。 アプリケーションの終了時などにリソースを解放すること。
	 * 
	 * 解放後に再生したい場合、改めてplayを呼べば再生は可能。 その場合は改めてリソース解放をすること。
	 */
	public void release();

	/**
	 * 再生中のパスを取得する
	 * 
	 * @return 再生中のパス。再生していない場合はnull。
	 */
	public String getPlayingPath();

	/**
	 * 再生中かを取得する
	 * 
	 * @return 再生中の場合はtrue、そうでない場合はfalse
	 */
	public boolean isPlaying();

	/**
	 * 再生状態が変わった際にメッセージが受け取るハンドラーを登録する
	 * 
	 * 再生状態が変わった際には、Handlerのwhatに変更後の状態が格納される。
	 * 
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
	 * 
	 * @param handler
	 *            登録するハンドラ
	 */
	public void addPlayStateChangedHandler(Handler handler);

	/**
	 * 登録済みの再生状態変更ハンドラを削除する
	 * 
	 * @param handler
	 *            削除するハンドラ
	 * 
	 * @see MediaPlayManager#addPlayStateChangedHandler
	 */
	public void removePlayStateChangedHandler(Handler handler);
}
