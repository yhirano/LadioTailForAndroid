/* 
 * Copyright (c) 2011-2014 Yuichi Hirano
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

import com.uraroji.garage.android.ladiotail.PlayStateChangedCallbackInterface;

/**
 * メディア再生サービスのインターフェース
 */
interface MediaPlayServiceInterface {

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
    void play(in String path, in String notificationTitle,
        in String notificationContent);

    /**
     * 再生を停止する
     */
    void stop();

    /**
     * 準備中・再生中のパスを取得する
     * 
     * @return 準備中・再生中のパス。停止中の場合はnull。
     */
    String getPlayingPath();

    /**
     * 再生状態を取得する
     * 
     * @return 再生状態
     */
    int getPlayState();

    /**
     * 再生状態が変わったことを通知するコールバックを登録
     *
     * @param callback 登録するコールバック
     */
    void registerPlayStateChangedCallback(PlayStateChangedCallbackInterface callback);

    /**
     * 登録済みの再生状態が変わったことを通知するコールバックを削除
     *
     * @param callback 削除するコールバック
     */
    void unregisterPlayStateChangedCallback(PlayStateChangedCallbackInterface callback);
}
