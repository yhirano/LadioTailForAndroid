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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.uraroji.garage.android.netladiolib.Channel;
import com.uraroji.garage.android.netladiolib.HeadlineManager;

import java.util.ArrayList;

/**
 * 番組詳細Activity
 */
public class ChannelActivity extends Activity {

    /**
     * IntentのEXTRAキー 指定されたChannelの詳細を開く
     */
    public static final String INTENT_EXTRA_OPEN_CHANNEL = "OPEN_CHANNEL";

    /**
     * IntentのEXTRAキー 指定された再生URLを持つChannelの詳細を開く
     */
    public static final String INTENT_EXTRA_OPEN_CHANNEL_PLAY_URL = "OPEN_CHANNEL_PLAY_URL";

    /**
     * Play/Stopボタン
     */
    private ImageButton mPlayStopImageButton;

    /**
     * 情報を表示するChannel
     */
    private Channel mChannel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_info);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Channel channel = (Channel) bundle
                        .getSerializable(INTENT_EXTRA_OPEN_CHANNEL);
                String channelPlayUrl = bundle
                        .getString(INTENT_EXTRA_OPEN_CHANNEL_PLAY_URL);

                // INTENT_EXTRA_OPEN_CHANNEL_SERIAL_IDが指定されている場合
                if (channel != null) {
                    mChannel = channel;
                } else if (channelPlayUrl != null) {
                    mChannel = HeadlineManager.getHeadline().getChannel(
                            channelPlayUrl);
                    if (mChannel == null) {
                        Log.w(C.TAG, "Channel isn't exists. Finish");
                        finish();
                    }
                } else {
                    Log.w(C.TAG,
                            "ChannelActivity received unknown extra key. Finish.");
                    finish();
                }
            } else {
                Log.w(C.TAG,
                        "ChannelActivity can't get intent extra data. Finish.");
                finish();
            }
        } else {
            Log.w(C.TAG, "ChannelActivity can't get intent. Finish.");
            finish();
        }

        final String title = mChannel.getNam();
        if (title != null) {
            TextView titleTextView = (TextView) findViewById(R.id.TitleTextView);
            titleTextView.setText(title);
        }

        final String dj = mChannel.getDj();
        if (dj != null) {
            TextView djTextView = (TextView) findViewById(R.id.DjTextView);
            djTextView.setText(dj);
        }

        // Play/Stopボタン
        mPlayStopImageButton = (ImageButton) findViewById(R.id.PlayStopImageButton);
        mPlayStopImageButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final String playingPath = MediaPlayManager.getPlayingPath();

                // 再生URLが存在しない場合は何もしない
                if (mChannel.getPlayUrl() == null) {
                    ;
                }
                // 再生中の番組が無い場合はとりあえず再生する
                else if (playingPath == null) {
                    play();
                }
                // 再生中の番組がこの画面で表示している番組と同じ場合には停止をする
                else if (playingPath.equals(mChannel.getPlayUrl().toString())) {
                    stop();
                }
                // それ以外はとりあえずPlayを表示しておく
                else {
                    play();
                }
            }
        });
        switchPlayStopButtonText();
        // 再生対象が存在しない場合はボタンを無効にする
        if (mChannel.getPlayUrl() == null) {
            mPlayStopImageButton.setEnabled(false);
            mPlayStopImageButton.setVisibility(View.INVISIBLE);
        }

        // Siteボタン
        ImageButton siteImageButton = (ImageButton) findViewById(R.id.SiteImageButton);
        if (mChannel.getUrl() == null) {
            siteImageButton.setEnabled(false);
            siteImageButton.setVisibility(View.INVISIBLE);
        }
        siteImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(mChannel.getUrl().toString());
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(i);
            }
        });

        ListView channelInfoListView = (ListView) findViewById(R.id.ChannelInfoListView);
        ChannelInfoAdapter channelInfoAdapter = new ChannelInfoAdapter(this,
                mChannel);
        channelInfoListView.setAdapter(channelInfoAdapter);

        // Closeボタン
        Button closeButton = (Button) findViewById(R.id.BackButton);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        MediaPlayManager.addPlayStateChangedHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switchPlayStopButtonText();
            }
        });
    }

    /**
     * 番組を再生する 再生前にはプログレス画面が表示され、再生が開始するとプログレス画面が消える。
     */
    private void play() {
        // 再生準備中ダイアログを表示する
        final ProgressDialog loadingDialog = new ProgressDialog(
                ChannelActivity.this);
        loadingDialog.setMessage(getString(R.string.preparing));
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // 番組の取得は別スレッドで行う
        new Thread() {
            @Override
            public void run() {
                // 再生開始のメッセージを捕捉するためにハンドラーを登録
                MediaPlayManager.addPlayStateChangedHandler(mmHandler);
                // 再生開始
                MediaPlayManager.play(mChannel.getPlayUrl().toString(),
                        mChannel.getNam(), mChannel.getDj());
            }

            private Handler mmHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED:
                            // 再生開始したのでハンドラーを削除
                            MediaPlayManager.removePlayStateChangedHandler(this);
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            break;
                        case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START:
                            // 再生失敗したのでハンドラーを削除
                            MediaPlayManager.removePlayStateChangedHandler(this);
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            // 失敗した旨のメッセージを出す
                            Toast.makeText(ChannelActivity.this,
                                    R.string.failed_play_message, Toast.LENGTH_LONG)
                                    .show();
                            break;
                        case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED:
                        case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED:
                            break;
                        default:
                            Log.w(C.TAG, String.format(
                                    "Unknown mesasge(%d) from fetch handler.",
                                    msg.what));
                            break;
                    }
                }
            };
        }.start();
    }

    /**
     * 番組を停止する 停止前にはプログレス画面が表示され、停止するとプログレス画面が消える。
     */
    private void stop() {
        // 停止準備中ダイアログを表示する
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage(getString(R.string.preparing));
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // 停止は別スレッドで行う
        new Thread() {
            /**
             * 停止のメッセージ
             */
            private final static int MSG_STOPPED = 0;

            @Override
            public void run() {
                MediaPlayManager.stop();
                // 停止の通知
                mmHandler.sendEmptyMessage(MSG_STOPPED);
            }

            private Handler mmHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_STOPPED:
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            break;
                        default:
                            Log.w(C.TAG, String.format(
                                    "Unknown mesasge(%d) from fetch handler.",
                                    msg.what));
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            break;
                    }
                }
            };
        }.start();
    }

    /**
     * Play/Stopボタンのテキストを、再生状態によって書き換える
     */
    private void switchPlayStopButtonText() {
        final String playingPath = MediaPlayManager.getPlayingPath();

        // 再生URLが存在しない場合や、再生中の番組が無い場合はとりあえずPlayを表示しておく
        if (mChannel.getPlayUrl() == null || playingPath == null) {
            mPlayStopImageButton.setImageResource(R.drawable.play_button);
        }
        // 再生中の番組がこの画面で表示している番組と同じ場合には、Stopを表示する
        else if (playingPath.equals(mChannel.getPlayUrl().toString())) {
            mPlayStopImageButton.setImageResource(R.drawable.stop_button);
        }
        // それ以外はとりあえずPlayを表示しておく
        else {
            mPlayStopImageButton.setImageResource(R.drawable.play_button);
        }
    }

    private class ChannelInfoAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        /**
         * 表示するキーと内容の組み合わせ ["Title", "今日からラジオ"]といった組み合わせを格納する。
         */
        private class ChannelInfo {

            /**
             * キー
             */
            /* package */String title;

            /**
             * 内容
             */
            /* package */String value;

            /**
             * コンストラクタ
             * 
             * @param title キー
             * @param value 内容
             */
            public ChannelInfo(String title, String value) {
                this.title = title;
                this.value = value;
            }
        }

        /**
         * 表示内容のリスト
         */
        private ArrayList<ChannelInfo> mInfoList = new ArrayList<ChannelInfo>(11);

        /**
         * コンストラクタ
         * 
         * @param context コンテキスト
         * @param channel 表示する番組
         */
        public ChannelInfoAdapter(Context context, Channel channel) {
            super();

            this.mInflater = LayoutInflater.from(context);

            int listensersNum = channel.getCln();
            if (listensersNum != Channel.UNKNOWN_LISTENER_NUM) {
                addInfoList(R.string.listeners, String.valueOf(listensersNum));
            }
            addInfoList(R.string.genre, channel.getGnl());
            addInfoList(R.string.description, channel.getDesc());
            addInfoList(R.string.song, channel.getSong());
            addInfoList(R.string.airtime, channel.getTimsString());
            int totalListensersNum = channel.getClns();
            if (totalListensersNum != Channel.UNKNOWN_LISTENER_NUM) {
                addInfoList(R.string.total_listener,
                        String.valueOf(totalListensersNum));
            }
            int maxListensersNum = channel.getMax();
            if (maxListensersNum != Channel.UNKNOWN_LISTENER_NUM) {
                addInfoList(R.string.max_listeners,
                        String.valueOf(maxListensersNum));
            }
            if (channel.getChs() != Channel.UNKNOWN_CHANNEL_NUM) {
                addInfoList(R.string.channel, getChsString(channel.getChs()));
            }
            addInfoList(R.string.format, channel.getType());
            int bitNum = channel.getBit();
            if (bitNum != Channel.UNKNOWN_BITRATE_NUM) {
                addInfoList(R.string.bitrate, String.valueOf(bitNum) + "kbps");
            }
            if (channel.getSmpl() != Channel.UNKNOWN_SAMPLING_RATE_NUM) {
                addInfoList(R.string.sampling_rate,
                        String.valueOf(channel.getSmpl() / 1000) + "kHz");
            }
        }

        /**
         * 指定したキーと内容を表示内容に追加する
         * 
         * @param titleId キーの文字列ID
         * @param value 内容
         */
        private void addInfoList(int titleId, String value) {
            if (value != null && value.length() != 0) {
                mInfoList.add(new ChannelInfo(getString(titleId), value));
            }
        }

        @Override
        public int getCount() {
            return mInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            // 使用しないのでnullを返す
            return null;
        }

        @Override
        public long getItemId(int position) {
            // 使用しないのでpositionを返す
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder = null;

            if (view == null) {
                view = mInflater.inflate(R.layout.channel_info_item_row,
                        null);

                holder = new ViewHolder();
                holder.infoTitleTextView = (TextView) view
                        .findViewById(R.id.InfoTitleTextView);
                holder.infoValueTextView = (TextView) view
                        .findViewById(R.id.InfoValueTextView);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.infoTitleTextView.setText(mInfoList.get(position).title);
            holder.infoValueTextView.setText(mInfoList.get(position).value);

            return view;
        }

        /**
         * ChannelInfoAdapter#getViewにおけるViewの保持クラス
         */
        private class ViewHolder {
            /* package */TextView infoTitleTextView;
            /* package */TextView infoValueTextView;
        }

        /**
         * チャンネル数を文字列で取得する
         * 
         * @param chs チャンネル数
         * @return チャンネル数の文字列
         */
        private String getChsString(int chs) {
            switch (chs) {
                case Channel.UNKNOWN_CHANNEL_NUM:
                    return ChannelActivity.this.getString(R.string.unknown);
                case 1:
                    return ChannelActivity.this.getString(R.string.mono);
                case 2:
                    return ChannelActivity.this.getString(R.string.stereo);
                default:
                    return String.valueOf(chs);
            }
        }
    }
}
