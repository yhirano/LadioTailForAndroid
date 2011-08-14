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

import java.io.IOException;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.util.Log;

/**
 * ローカルでのメディア再生
 */
public class MediaPlayAtLocal implements MediaPlayInterface {

	/**
	 * コンテキスト
	 */
	private Context mContext;

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
	 * 再生状態が変わった際のハンドラーリスト
	 * 
	 * @see MediaPlayManager#MSG_PLAY_STARTED
	 * @see MediaPlayManager#MSG_PLAY_COMPLATED
	 * @see MediaPlayManager#MSG_PLAY_STOPPED
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
	 */
	private ArrayList<Handler> mPlayStateChangedHandlerist = new ArrayList<Handler>();

	/**
	 * ロックオブジェクト
	 */
	private final Object mlock = new Object();

	@Override
	public void init(Context context) {
		this.mContext = context;
	}

	@Override
	public void play(String path, String notificationTitle,
			String notificationContent) {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Trying to play " + path + ".");
		}

		if (path == null) {
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
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
								notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED);
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
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED);
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
				Log.i(C.TAG, "MediaPlayer occerd IOException(" + e.toString()
						+ ").");
				mPlayingPath = null;
				mNotificationTitle = null;
				mNotificationContent = null;
				mMediaPlayer.reset();
				isStarted = false;
			}
		}

		if (isStarted == true) {
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED);
		} else {
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
		}
	}

	@Override
	public void stop() {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Trying to stop playing.");
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
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED);
		}
	}

	@Override
	public void release() {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Release MediaPlay resouce.");
		}

		boolean isPlayed;
		synchronized (mlock) {
			if (mMediaPlayer == null) {
				return;
			}

			isPlayed = mMediaPlayer.isPlaying();
			if (isPlayed == true) {
				mMediaPlayer.stop();
				mPlayingPath = null;
				mNotificationTitle = null;
				mNotificationContent = null;
			}

			mMediaPlayer.release();
			mMediaPlayer = null;
		}

		if (isPlayed == true) {
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED);
		}
	}

	@Override
	public String getPlayingPath() {
		synchronized (mlock) {
			return mPlayingPath;
		}
	}

	@Override
	public boolean isPlaying() {
		synchronized (mlock) {
			if (mMediaPlayer == null) {
				return false;
			}

			return mMediaPlayer.isPlaying();
		}
	}

	@Override
	public void addPlayStateChangedHandler(Handler handler) {
		synchronized (mlock) {
			if (handler != null) {
				mPlayStateChangedHandlerist.add(handler);
			}
		}
	}

	@Override
	public void removePlayStateChangedHandler(Handler handler) {
		synchronized (mlock) {
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

		// Notificationを更新する
		{
			switch (what) {
			case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED: {
				NotificationManager nm = (NotificationManager) mContext
						.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification n = new Notification(R.drawable.icon,
						mNotificationTitle, System.currentTimeMillis());

				Intent intent = new Intent(mContext, ChannelActivity.class);
				intent.putExtra(
						ChannelActivity.INTENT_EXTRA_OPEN_CHANNEL_PLAY_URL,
						mPlayingPath);
				PendingIntent contentIntent = PendingIntent.getActivity(
						mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
				n.setLatestEventInfo(mContext, mNotificationTitle,
						mNotificationContent, contentIntent);

				nm.notify(C.NOTIFICATION_ID, n);
				break;
			}
			case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED:
			case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED:
			case MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START: {
				// 再生中ではないのでNotificationを消す
				NotificationManager nm = (NotificationManager) mContext
						.getSystemService(Context.NOTIFICATION_SERVICE);
				nm.cancel(C.NOTIFICATION_ID);
				break;
			}
			default:
				break;
			}
		}
	}
	
	/**
	 * 再生状態が変わった際のハンドラーリストのクローンしたリストを取得する。
	 * 
	 * 浅いクローンなので注意。
	 * 
	 * @return 再生状態が変わった際のハンドラーリストのクローンしたリスト
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Handler> getPlayStateChangedHandleristClone() {
		synchronized (mlock) {
			return (ArrayList<Handler>) mPlayStateChangedHandlerist.clone();
		}
	}
}
