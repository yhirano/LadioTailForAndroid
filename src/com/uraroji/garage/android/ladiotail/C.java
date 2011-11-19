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

/**
 * アプリケーションの静的な設定情報保持
 */
public class C {

    /**
     * ログのタグ
     */
    public static final String TAG = "LadioTail";

    /**
     * Verboseログを表示するか リリースの場合にはfalse
     */
    public static final boolean LOCAL_LOG = false;

    /**
     * 番組の再生をサービスで行うか サービスで行う場合、LadioTailが終了しても再生を継続する
     */
    public static final boolean MEDIA_PLAY_AT_SERVICE = true;

    /**
     * TabHost/TabWedgetがEditTextからフォーカスを盗むAndroidのバグに対する対処をするか
     * エミュレータでは発生するが、GaraxyTabでは発生していない 参照：
     * http://code.google.com/p/android/issues/detail?id=2516
     */
    public static final boolean ABOID_TAB_STEALS_FOCUS_FROM_EDITTEXT = false;

    /**
     * NotificationのID ユニークなIDを取得するために、R.layout.mainのリソースIDを使う
     */
    public static final int NOTIFICATION_ID = R.layout.main;

    /**
     * 音声による検索単語の入力を有効にするか 微妙な機能なのでfalse推奨
     */
    public static final boolean ENABLE_VOICE_SEARCH_INPUT = false;

    /**
     * 音声による検索単語の入力において、音声認識エンジンが結果を返す最大数。
     * たとえば「おなかすいた」と入力し、この数値が2の場合には「お腹すいた」「おなかすいた」が返る。
     * このように複数の結果が想定される場合に音声認識エンジンが返す結果の最大数を設定する。
     * 0以下を入力する場合は、特に設定しない（認識エンジンのデフォルトとする）。
     * ENABLE_VOICE_SEARCH_INPUTがtrueの場合のみに有効
     */
    public static final int VOICE_SEARCH_MAX_RESULT_NUM = 0;

    /**
     * コンストラクタ シングルトンなのでprivateとする
     */
    private C() {
    }
}
