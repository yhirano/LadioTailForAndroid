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

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * サービスでのメディア再生
 */
public class MediaPlayAtService implements MediaPlayInterface {

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
	 * @see MediaPlayManager#MSG_PLAY_STARTED
	 * @see MediaPlayManager#MSG_PLAY_COMPLATED
	 * @see MediaPlayManager#MSG_PLAY_STOPPED
	 * @see MediaPlayManager#MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START
	 */
	private ArrayList<Handler> mPlayStateChangedHandlerist = new ArrayList<Handler>();

	/**
	 * mPlayStateChangedHandleristのロックオブジェクト
	 */
	private final Object mPlayStateChangedHandleristLock = new Object();

	@Override
	public void init(Context context) {
		this.mContext = context;

		Intent intent = new Intent(MediaPlayServiceInterface.class.getName());
		context.startService(intent); // MeidaPlayサービス開始
		context.bindService(intent, mMediaPlayServiceConn,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void play(String path, String notificationTitle,
			String notificationContent) {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Trying to play " + path + ".");
		}

		try {
			mMediaPlayServiceInterface.play(path, notificationTitle,
					notificationContent);
		} catch (RemoteException e) {
			Log.w(C.TAG, "Occurd RemoteException(" + e.toString() + ").");
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
		}
	}

	@Override
	public void stop() {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Trying to stop playing.");
		}

		try {
			mMediaPlayServiceInterface.stop();
		} catch (RemoteException e) {
			Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred.");
			notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
		}
	}

	@Override
	public void release() {
		if (C.LOCAL_LOG) {
			Log.v(C.TAG, "Release MediaPlay resouce.");
		}

		boolean isPlayed = isPlaying();

		mContext.unbindService(mMediaPlayServiceConn);
		// 再生中で無い場合はサービスを止める
		if (isPlayed == false) {
			mContext.stopService(new Intent(MediaPlayServiceInterface.class
					.getName()));
		}
	}

	@Override
	public String getPlayingPath() {
		try {
			return mMediaPlayServiceInterface.getPlayingPath();
		} catch (RemoteException e) {
			Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred.");
			// どうしようもないのでとりあえずnullを返す
			return null;
		}
	}

	@Override
	public boolean isPlaying() {
		try {
			return mMediaPlayServiceInterface.isPlaying();
		} catch (RemoteException e) {
			Log.w(C.TAG, "RemoteException(" + e.toString() + ") occurred.");
			// どうしようもないのでとりあえずfalseを返す
			return false;
		}
	}

	@Override
	public void addPlayStateChangedHandler(Handler handler) {
		synchronized (mPlayStateChangedHandleristLock) {
			if (handler != null) {
				mPlayStateChangedHandlerist.add(handler);
			}
		}
	}

	@Override
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
	 * 再生状態が変わった際のハンドラーリストのクローンしたリストを取得する。
	 * 
	 * 浅いクローンなので注意。
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
			case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_STARTED:
				notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STARTED);
				break;
			case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_COMPLATED:
				notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_COMPLATED);
				break;
			case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_PLAY_STOPPED:
				notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_PLAY_STOPPED);
				break;
			case MediaPlayService.MSG_MEDIA_PLAY_SERVICE_FAILD_PLAY_START:
				notifyPlayStateChanged(MediaPlayManager.MSG_MEDIA_PLAY_MANAGER_FAILD_PLAY_START);
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
