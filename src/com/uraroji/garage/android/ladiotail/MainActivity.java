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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.uraroji.garage.android.ladiotail.bugreport.AppUncaughtExceptionHandler;
import com.uraroji.garage.android.netladiolib.Channel;
import com.uraroji.garage.android.netladiolib.Headline;
import com.uraroji.garage.android.netladiolib.HeadlineManager;

import java.io.IOException;
import java.util.List;

/**
 * 番組一覧Activity
 */
public class MainActivity extends TabActivity {

    private final static int MENU_ID_RELOAD = Menu.FIRST + 1;

    private final static int MENU_ID_STOP = Menu.FIRST + 2;

    private final static int REQUEST_VOICE_SEARCH = 0;

    private EditText mSearchEditText;

    private ChannelAdapter mNewlyListAdapter;

    private ChannelAdapter mListenersListAdapter;

    private ChannelAdapter mTitleListAdapter;

    private ChannelAdapter mDjListAdapter;

    /**
     * 起動時にヘッドラインを取得しに行くかを管理するフラグ。 諸事情によりonCreateでヘッドラインを取得できなくなったので、
     * onStartでヘッドラインを取得することになった。 しかし、onStartだとアプリに戻ってきた場合も処理されるため、
     * それを防ぐためにこのフラグを用意した。
     */
    private boolean isFetchAndUpdateHeadlineStartup = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 補足されない例外をキャッチするハンドラを登録（バグレポート用）
        Thread.setDefaultUncaughtExceptionHandler(new AppUncaughtExceptionHandler(
                this));

        // 再生管理の初期化
        MediaPlayManager.init(getApplicationContext());

        // TabHostを取得
        final TabHost tabHost = getTabHost();
        final Resources resources = getResources();

        // Newlyタブの作成
        TabSpec newlyTab = tabHost.newTabSpec("NewlyTab");
        newlyTab.setIndicator(getString(R.string.newly),
                resources.getDrawable(R.drawable.ic_tab_newly));
        newlyTab.setContent(R.id.NewlyListView);
        tabHost.addTab(newlyTab);

        // Listenersタブの作成
        TabSpec listenersTab = tabHost.newTabSpec("ListenersTab");
        listenersTab.setIndicator(getString(R.string.listeners),
                resources.getDrawable(R.drawable.ic_tab_listeners));
        listenersTab.setContent(R.id.ListenersListView);
        tabHost.addTab(listenersTab);

        // Titleタブの作成
        TabSpec titleTab = tabHost.newTabSpec("TitleTab");
        titleTab.setIndicator(getString(R.string.title),
                resources.getDrawable(R.drawable.ic_tab_title));
        titleTab.setContent(R.id.TitleListView);
        tabHost.addTab(titleTab);

        // DJタブの作成
        TabSpec djTab = tabHost.newTabSpec("DjTab");
        djTab.setIndicator(getString(R.string.dj),
                resources.getDrawable(R.drawable.ic_tab_dj));
        djTab.setContent(R.id.DjListView);
        tabHost.addTab(djTab);

        // 検索ボックス
        mSearchEditText = (EditText) findViewById(R.id.SearchEditText);
        // 検索ボックス入力時に番組をフィルタリングする
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                ;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateHeadline();
            }
        });
        /*
         * TabHost/TabWedgetがEditTextからフォーカスを盗むAndroidのバグに対する対処
         * http://code.google.com/p/android/issues/detail?id=2516
         */
        if (C.ABOID_TAB_STEALS_FOCUS_FROM_EDITTEXT) {
            mSearchEditText.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mSearchEditText.requestFocusFromTouch();
                    return true;
                }
            });
        }
        // 検索ボックスからフォーカスが外れた際にはキーボードを隠す
        mSearchEditText
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean flag) {
                        if (flag == false) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    }
                });

        // Voiceボタン
        // 音声による検索単語の入力が有効の場合にのみ機能する
        if (C.ENABLE_VOICE_SEARCH_INPUT) {
            final Button voiceButton = (Button) findViewById(R.id.VoiceSearchButton);
            voiceButton.setVisibility(View.VISIBLE);
            voiceButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // 音声認識を起動させる
                        Intent intent = new Intent(
                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        if (C.VOICE_SEARCH_MAX_RESULT_NUM > 0) {
                            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,
                                    C.VOICE_SEARCH_MAX_RESULT_NUM);
                        }
                        startActivityForResult(intent, REQUEST_VOICE_SEARCH);
                    } catch (ActivityNotFoundException e) {
                        // 音声認識に対応していないので警告を出す
                        AlertDialog dialog = new AlertDialog.Builder(
                                MainActivity.this)
                                .setMessage(
                                        R.string.not_supported_voice_recognize)
                                .setPositiveButton(R.string.ok, null).create();
                        dialog.show();
                        // 音声認識が使用できないためにVoiceボタンを無効にする
                        voiceButton.setEnabled(false);
                    }
                }
            });
        }

        // 番組を選択した際の処理
        OnItemClickListener channelClickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                Object selectedItem = arg0.getItemAtPosition(arg2);

                if (selectedItem == null) {
                    if (C.LOCAL_LOG) {
                        Log.v(C.TAG, "Clicked null item. Maybe selected ad.");
                    }
                } else if (selectedItem instanceof Channel) {
                    if (C.LOCAL_LOG) {
                        Log.v(C.TAG,
                                String.format("Clicked %s item.",
                                        selectedItem.toString()));
                    }
                    // 再生
                    play((Channel) selectedItem);
                } else {
                    Log.w(C.TAG, String.format("Clicked unknown %s item.",
                            selectedItem.toString()));
                }
            }
        };
        // 番組を長押しした際の処理
        OnItemLongClickListener channelLongClickListener = new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                Object selectedItem = arg0.getItemAtPosition(arg2);

                if (selectedItem == null) {
                    if (C.LOCAL_LOG) {
                        Log.v(C.TAG,
                                "Long clicked null item. Maybe selected ad.");
                    }
                    return true;
                } else if (selectedItem instanceof Channel) {
                    if (C.LOCAL_LOG) {
                        Log.v(C.TAG, String.format("Long clicked %s item.",
                                selectedItem.toString()));
                    }

                    Intent intent = new Intent(MainActivity.this,
                            ChannelActivity.class);
                    intent.putExtra(ChannelActivity.INTENT_EXTRA_OPEN_CHANNEL,
                            ((Channel) selectedItem));
                    startActivity(intent);

                    return false;
                } else {
                    Log.w(C.TAG, String.format("Long clicked unknown %s item.",
                            selectedItem.toString()));

                    return true;
                }
            }
        };

        // Newly list
        ListView newlyListView = (ListView) findViewById(R.id.NewlyListView);
        newlyListView.setOnItemClickListener(channelClickListener);
        newlyListView.setOnItemLongClickListener(channelLongClickListener);
        mNewlyListAdapter = new ChannelAdapter(this);
        newlyListView.setAdapter(mNewlyListAdapter);

        // Listeners list
        ListView listenersListView = (ListView) findViewById(R.id.ListenersListView);
        listenersListView.setOnItemClickListener(channelClickListener);
        listenersListView.setOnItemLongClickListener(channelLongClickListener);
        mListenersListAdapter = new ChannelAdapter(this);
        listenersListView.setAdapter(mListenersListAdapter);

        // Title list
        ListView titleListView = (ListView) findViewById(R.id.TitleListView);
        titleListView.setOnItemClickListener(channelClickListener);
        titleListView.setOnItemLongClickListener(channelLongClickListener);
        mTitleListAdapter = new ChannelAdapter(this);
        titleListView.setAdapter(mTitleListAdapter);

        // DJ list
        ListView djListView = (ListView) findViewById(R.id.DjListView);
        djListView.setOnItemClickListener(channelClickListener);
        djListView.setOnItemLongClickListener(channelLongClickListener);
        mDjListAdapter = new ChannelAdapter(this);
        djListView.setAdapter(mDjListAdapter);

        MediaPlayManager.addPlayStateChangedHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 再生状態が変わったらリストを更新
                updateHeadline();
            }
        });

        isFetchAndUpdateHeadlineStartup = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 前回バグで強制終了した場合はダイアログ表示
        AppUncaughtExceptionHandler.showBugReportDialogIfExist();

        /*
         * ヘッドラインが未取得の場合にのみヘッドラインをネットから取得する。
         * onCreateでヘットラインを取得しないのは、再生をサービスで行う場合にonCreateでサービスを
         * 初期化しており、onCreateを抜けるまでサービスにアクセスできないためである。
         * http://d.hatena.ne.jp/Kazzz/20100630/p1
         */
        if (isFetchAndUpdateHeadlineStartup == true) {
            fecthAndUpdateHeadline();
        }

        // ヘッドラインの自動取得は起動時のみのため、あとはonStartにきてもヘッドラインを取得しない
        isFetchAndUpdateHeadlineStartup = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 一応起動時にヘッドライン自動取得ができるようにしておく
        isFetchAndUpdateHeadlineStartup = true;

        MediaPlayManager.release();
    }

    // オプションメニュー作成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューアイテムを追加
        final MenuItem stopMenuItem = menu.add(Menu.NONE, MENU_ID_STOP,
                Menu.NONE, R.string.stop);
        stopMenuItem.setIcon(R.drawable.ic_menu_stop);

        final MenuItem reloadMenuItem = menu.add(Menu.NONE, MENU_ID_RELOAD,
                Menu.NONE, R.string.reload);
        reloadMenuItem.setIcon(R.drawable.ic_menu_reload);

        MediaPlayManager.addPlayStateChangedHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED:
                    case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED:
                        // 再生が終了したら停止ボタンを無効にする
                        stopMenuItem.setEnabled(false);
                        break;
                    default:
                        break;
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    // オプションメニュー表示
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 再生中のみに停止ボタンを有効にする
        menu.findItem(MENU_ID_STOP).setEnabled(MediaPlayManager.isPlaying());

        return super.onPrepareOptionsMenu(menu);
    }

    // オプションメニューアイテムの選択
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_STOP:
                stop();
                return false;
            case MENU_ID_RELOAD:
                fecthAndUpdateHeadline();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // REQUEST_VOICE_SEARCH
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_VOICE_SEARCH: {
                if (resultCode == RESULT_OK) {
                    // 結果文字列リスト
                    final List<String> results = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if (results.size() == 0) {
                        ;
                    } else if (results.size() == 1) {
                        mSearchEditText.setText(results.get(0));
                    } else {
                        // 複数の選択肢からユーザーに選択させる
                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setItems(
                                        results.toArray(new String[results.size()]),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                mSearchEditText.setText(results
                                                        .get(which));
                                            }
                                        }).setNegativeButton(R.string.cancel, null)
                                .create();
                        dialog.show();
                    }
                }
                break;
            }
            default: {
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 通信中のダイアログを表示させつつ、番組を取得し、取得後にヘッドラインリストの内容を更新する
     */
    private void fecthAndUpdateHeadline() {
        // 通信中ダイアログを表示する
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage(getString(R.string.now_loading));
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // 番組の取得は別スレッドで行う
        new Thread() {

            /**
             * 番組取得終了のメッセージ
             */
            private final static int MSG_FETCHED_HEADLINE = 0;

            /**
             * 番組取得失敗のメッセージ
             */
            private final static int MSG_FAILED_FETCH_HEADLINE = 1;

            @Override
            public void run() {
                try {
                    HeadlineManager.getHeadline().fecthHeadline();

                    // 読み込み終了の通知
                    mmHandler.sendEmptyMessage(MSG_FETCHED_HEADLINE);
                } catch (IOException e) {
                    // 読み込み失敗の通知
                    mmHandler.sendEmptyMessage(MSG_FAILED_FETCH_HEADLINE);
                }
            }

            private Handler mmHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_FETCHED_HEADLINE:
                            // ヘッドラインリストの内容を更新する
                            updateHeadline();
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            break;
                        case MSG_FAILED_FETCH_HEADLINE:
                            // ヘッドラインリストの内容を更新する
                            updateHeadline();
                            // ダイアログを閉じる
                            loadingDialog.dismiss();
                            // 失敗した旨のメッセージを出す
                            Toast.makeText(MainActivity.this,
                                    R.string.failed_fetch_headline,
                                    Toast.LENGTH_LONG).show();
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
     * ヘッドラインリストの内容を更新する
     */
    private void updateHeadline() {
        // 検索ボックスの文字列を空白文字で分割する
        final String searchWord = mSearchEditText.getText().toString();

        final String playingPath = MediaPlayManager.getPlayingPath();

        // リストの更新
        mNewlyListAdapter.update(
                HeadlineManager.getHeadline().getChannels(
                        Headline.SORT_TYPE_NEWLY, searchWord), playingPath);
        mListenersListAdapter.update(
                HeadlineManager.getHeadline().getChannels(
                        Headline.SORT_TYPE_LISTENERS, searchWord), playingPath);
        mTitleListAdapter.update(
                HeadlineManager.getHeadline().getChannels(
                        Headline.SORT_TYPE_TITLE, searchWord), playingPath);
        mDjListAdapter.update(
                HeadlineManager.getHeadline().getChannels(
                        Headline.SORT_TYPE_DJ, searchWord), playingPath);
    }

    /**
     * 番組を再生する 再生前にはプログレス画面が表示され、再生が開始するとプログレス画面が消える。
     * 
     * @param channel 再生する番組
     */
    private void play(final Channel channel) {
        // 放送URLが存在しない場合などはエラーメッセージを表示して終了
        if (channel == null || channel.getPlayUrl() == null) {
            // 失敗した旨のメッセージを出す
            Toast.makeText(this, R.string.failed_play_message,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // 現在再生中の番組の場合は何もしない
        if (channel.getPlayUrl().toString()
                .equals(MediaPlayManager.getPlayingPath()) == true) {
            return;
        }

        // 再生準備中ダイアログを表示する
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage(getString(R.string.preparing));
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        // 再生開始は別スレッドで行う
        new Thread() {

            @Override
            public void run() {
                // 再生開始のメッセージを捕捉するためにハンドラーを登録
                MediaPlayManager.addPlayStateChangedHandler(mmHandler);
                // 再生開始
                MediaPlayManager.play(channel.getPlayUrl().toString(),
                        channel.getNam(), channel.getDj());
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
                            Toast.makeText(MainActivity.this,
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
     * ListView用の番組一覧のAdapter。 広告 + 番組一覧を表示できる。
     */
    private class ChannelAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        /**
         * 再生中のパス
         */
        private String mPlayingPath;

        /**
         * 番組リスト
         */
        private Channel[] mChannelList = new Channel[0];

        /**
         * コンストラクタ
         * 
         * @param context コンテキスト
         */
        public ChannelAdapter(Context context) {
            super();

            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // 広告 + 番組
            return mChannelList.length + 1;
        }

        @Override
        public Object getItem(int position) {
            // リスト上端は広告なのでとりあえずnullを返しておく
            if (position == 0) {
                return null;
            } else {
                return mChannelList[position - 1];
            }
        }

        @Override
        public long getItemId(int position) {
            // 使用しないのでとりあえずpositionでも返しておく
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (position == 0) {
                /*
                 * convertView.getTagで何かが入っている場合、番組の表示で使用されたconvertViewだと思われるので、
                 * AdMob表示用に改めてconvertViewを作成する。
                 */
                if (view == null || view.getTag() != null) {
                    view = mInflater.inflate(R.layout.ad_item_row, null);
                    view.setTag(null);
                }
            } else {
                ViewHolder holder = null;

                /*
                 * convertView.getTagで何も入っていない場合、AdMobの表示で使用されたconvertViewだと思われるので
                 * 、 番組表示用に改めてconvertViewを作成する。
                 */
                if (view == null || view.getTag() == null) {
                    view = mInflater
                            .inflate(R.layout.channel_item_row, null);
                }

                holder = (ViewHolder) view.getTag();

                if (holder == null) {
                    holder = new ViewHolder();
                    holder.channelTitleTextView = (TextView) view
                            .findViewById(R.id.ChannelTitleTextView);
                    holder.channelDjTextView = (TextView) view
                            .findViewById(R.id.ChannelDjTextView);
                    holder.channelListenersTextView = (TextView) view
                            .findViewById(R.id.ChannelListenersTextView);
                    holder.channelDateTextView = (TextView) view
                            .findViewById(R.id.ChannelDateTextView);
                    holder.playingImageView = (ImageView) view
                            .findViewById(R.id.PlayingImageView);
                    view.setTag(holder);
                }

                final Channel channel = (Channel) getItem(position);

                String title = (channel).getNam();
                if (title != null) {
                    holder.channelTitleTextView.setText(title);
                } else {
                    holder.channelTitleTextView.setText("");
                }

                String dj = (channel).getDj();
                if (dj != null) {
                    holder.channelDjTextView.setText(dj);
                } else {
                    holder.channelDjTextView.setText("");
                }

                int listenersNum = (channel).getCln();
                if (listenersNum != Channel.UNKNOWN_LISTENER_NUM) {
                    holder.channelListenersTextView.setText(String
                            .valueOf(listenersNum) + " listeners");
                } else {
                    holder.channelListenersTextView.setText("");
                }

                String dateString = (channel).getTimsString();
                if (dateString != null) {
                    holder.channelDateTextView.setText("at " + dateString);
                } else {
                    holder.channelDateTextView.setText("");
                }

                // 再生中のURLと番組のURLが同じ場合に再生中であることをリスト内に表示する
                if (channel.getPlayUrl() != null
                        && channel.getPlayUrl().toString().equals(mPlayingPath)) {
                    holder.playingImageView.setImageResource(R.drawable.play_in_list);
                } else {
                    holder.playingImageView.setImageBitmap(null);
                }
            }

            return view;
        }

        /**
         * ChannelAdapter#getViewにおけるViewの保持クラス
         */
        private class ViewHolder {
            /* package */TextView channelTitleTextView;
            /* package */TextView channelDjTextView;
            /* package */TextView channelListenersTextView;
            /* package */TextView channelDateTextView;
            /* package */ImageView playingImageView;
        }

        /**
         * 番組リストを更新する
         * 
         * @param channels 番組リスト
         */
        /* package */void update(Channel[] channels, String playingPath) {
            if (channels == null) {
                throw new IllegalArgumentException(
                        "channels is specified null.");
            }

            this.mPlayingPath = playingPath;

            mChannelList = channels;

            notifyDataSetChanged();
        }
    }
}
