package org.app.enjoy.music.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.db.DBHelper;
import org.app.enjoy.music.frag.MusicPlayFragment;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaFormat;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkMediaFormat;
import tv.danmaku.ijk.media.player.pragma.DebugLog;
/**
 * 所有播放操作都交给服务,要服务就是为了实现后台播放,如果不用服务，那么一个界面关闭后，音乐也随着消失了。 但是用了服务这种情况不存在了。我们要做的永久
 * 在后台播放。直到用户把服务秒杀了。
 */
public class MusicService extends Service implements MediaPlayer.OnCompletionListener,Observer {
	private static final String TAG = MusicService.class.getName();
	/** 发送给服务一些Action */
	private IMediaPlayer mp = null;
	private Uri uri = null;
	private int id = 10000;
	private Handler handler = null;
	private long currentTime;// 播放时间
	private long duration;// 总时间
	private DBHelper dbHelper = null;// 数据库对象
	private int flag;// 标识
	private int position;// 位置
	public static Notification notification;// 通知栏显示当前播放音乐
	public static NotificationManager nm;
	private List<MusicData> musicDatas;
	private String mPath = "";
	private long mSeekPosition = 0L;
	private boolean isSetDataSource = false;//用来区分cue音乐 切换是否需要跳转
	private int prePosition = 0;
	private String mSampleRate;
	private String mBitRate;
	private Context mContext;
	private String mMusicName, mMusicFormat;
	@Override
	public void onCreate() {
		LogTool.i("onCreate");
		super.onCreate();
		mContext = this;
		release(false);
		try {
			IjkMediaPlayer ijkMediaPlayer = null;
			ijkMediaPlayer = new IjkMediaPlayer();
			mp = ijkMediaPlayer;
			mp.setOnCompletionListener(mCompletionListener);
			mp.setOnInfoListener(mInfoListener);
		} catch (Exception ex) {
			Log.e(TAG, "Unable to open content: " + uri, ex);
			return;
		}

		ShowNotifcation();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.ANSWER");
		filter.addAction("android.intent.action.ACTION_SHUTDOWN");
		filter.addAction(Contsant.PlayAction.MUSIC_STOP_SERVICE);
		registerReceiver(PhoneListener, filter);

		IntentFilter filter1 = new IntentFilter();
		filter1.addAction("com.app.playmusic");
		filter1.addAction("com.app.nextone");
		filter1.addAction("com.app.lastone");
		filter1.addAction("com.app.startapp");
		registerReceiver(appWidgetReceiver, filter1);
		registerHeadsetPlugReceiver();
		DataObservable.getInstance().addObserver(this);
	}

	private void release(boolean cleartargetstate) {
		if (mp != null) {
			mp.reset();
			mp.release();
			mp = null;
		}
	}

	IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
		public void onPrepared(IMediaPlayer mp) {
			LogTool.i("onPrepared");
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_PREPARED);
			if(mSeekPosition != 0){
				LogTool.i("mSeekPosition" + mSeekPosition);
				mp.seekTo(mSeekPosition);
			}else if(musicDatas.get(position).seekPostion != 0 && isSetDataSource){
				LogTool.i("musicDatas mSeekPosition" + musicDatas.get(position).seekPostion);
				mp.seekTo(musicDatas.get(position).seekPostion);
			}
			initAudioInfo();
			saveLastPlayInfo();
		}
	};

	IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {

		@Override
		public boolean onInfo(IMediaPlayer mp, int what, int extra) {
			LogTool.d("getCurrentPosition:" + mp.getCurrentPosition());
			handler.sendEmptyMessage(Contsant.PlayStatus.STATE_INFO);
			return false;
		}
	};
	/** 初始化1*/
	private void initAudioInfo() {
		showMediaInfo();
		if(musicDatas != null && musicDatas.size() > position){
			mMusicName = musicDatas.get(position).title;
			mMusicFormat = musicDatas.get(position).getPath().substring(musicDatas.get(position).getPath().length() - 3).toUpperCase();
		}
		duration = mp.getDuration();
		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_DURATION);
		broadCastMusicInfo(intent);
	}

	private void broadCastMusicInfo(Intent intent){
		if(intent == null){
			intent = new Intent();
		}
		Bundle bundle = new Bundle();
		bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
		intent.putExtras(bundle);
		intent.putExtra(Contsant.MUSIC_INFO_POSTION, position);
		intent.putExtra(Contsant.MUSIC_INFO_NAME, mMusicName);
		intent.putExtra(Contsant.MUSIC_INFO_FORMAT, mMusicFormat);
		intent.putExtra(Contsant.MUSIC_INFO_SAMPLERATE, mSampleRate);
		intent.putExtra(Contsant.MUSIC_INFO_BITRATE, mBitRate);
		intent.putExtra(Contsant.MUSIC_INFO_DURATION, duration);
		LogTool.d(position + mMusicName + mMusicFormat + mSampleRate);
		sendBroadcast(intent);
	}
	private IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
		public void onCompletion(IMediaPlayer mp) {
			DebugLog.d(TAG, "onCompletion");
			nextOne();
		}
	};

	/**
	 * 当开始播放时，通知栏显示当前播放信息
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void ShowNotifcation() {
		/*nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);//获取通知栏系统服务对象
		notification = new Notification();// 实例化通知栏
		notification.icon = R.drawable.music;// 为通知栏增加图标
		notification.defaults = Notification.DEFAULT_LIGHTS;// 默认灯
		notification.flags |= Notification.FLAG_AUTO_CANCEL;// 永远驻留
		notification.when = System.currentTimeMillis();// 获得系统时间
		notification.tickerText = musicDatas.get(position).title;//在通知栏显示有关的信息
		notification.tickerText = musicDatas.get(position).artist;
		Intent intent2 = new Intent(getApplicationContext(),PlayMusicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
		bundle.putInt(Contsant.POSITION_KEY, position);
		intent2.putExtras(bundle);
		intent2.putExtra("position", position);

		String artist = musicDatas.get(position).artist;
		if (artist.equals("<unknown>")) {
			artist = "未知艺术家";
		}
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2,PendingIntent.FLAG_UPDATE_CURRENT);
//		notification.setLatestEventInfo(getApplicationContext(), _title, _artist,contentIntent);
		notification = new Notification.Builder(MusicService.this)
				.setAutoCancel(true)
				.setContentTitle(musicDatas.get(position).title)
				.setContentText(musicDatas.get(position).artist)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.music)
				.setWhen(System.currentTimeMillis())
				.build();
		nm.notify(0, notification);*/

	}

	@Override
	public void onDestroy() {
		DataObservable.getInstance().deleteObserver(this);
		super.onDestroy();
		Log.e(TAG, "MusicService is onDestroy().....................");
		if(nm != null){
			nm.cancelAll();// 清除掉通知栏的信息
		}
		if (mp != null) {
			mp.stop();// 停止播放
			mp = null;
		}
		if (dbHelper != null) {
			dbHelper.close();// 关闭数据库
			dbHelper = null;
		}
		if (handler != null) {
			handler.removeMessages(1);//移除消息
			handler = null;
		}
		if(PhoneListener != null){
			this.unregisterReceiver(PhoneListener);
		}
		if(appWidgetReceiver != null){
			this.unregisterReceiver(appWidgetReceiver);
		}
		if(headsetPlugReceiver != null){
			unregisterReceiver(headsetPlugReceiver);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogTool.i("onStartCommand");
		int startServiceFisrt = intent.getIntExtra(Contsant.START_SERVICE_FIRST, 0);
		LogTool.d("startServiceFisrt:"+startServiceFisrt + "position:"+ position);
		if(startServiceFisrt == 1){//首次启动拿上次播放记录
			int ma_data = SharePreferencesUtil.getInt(mContext, Contsant.CURRENT_FRAG);
			position = SharePreferencesUtil.getInt(mContext, Contsant.MUSIC_INFO_POSTION);
			mMusicName = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_NAME);
			mMusicFormat = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_FORMAT);
			mBitRate = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_BITRATE);
			mSampleRate = SharePreferencesUtil.getString(mContext, Contsant.MUSIC_INFO_SAMPLERATE);
			duration = SharePreferencesUtil.getLong(mContext, Contsant.MUSIC_INFO_DURATION);
			Intent infoIntent = new Intent();
			infoIntent.setAction(Contsant.PlayAction.MUSIC_DURATION);
			broadCastMusicInfo(infoIntent);
			switch (ma_data) {
				case Contsant.Frag.MUSIC_LIST_FRAG:
					break;
				case Contsant.Frag.ARTIST_FRAG:
					break;
				case Contsant.Frag.ALBUM_FRAG:
					break;
				case Contsant.Frag.DIY_FRAG:
					break;
				default:
					break;
			}
			return 0;
		}
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
			position = bundle.getInt(Contsant.POSITION_KEY);
			mSeekPosition = bundle.getLong(Contsant.SEEK_POSITION);
			LogTool.i("position"+position +"mSeekPosition"+ mSeekPosition);
//			mPath = bundle.getString(Contsant.POSITION_PATH);

			// 发送的长度
			int length = intent.getIntExtra("length", -1);
			if (position >= musicDatas.size()) {
				return 0;
			}
			if (musicDatas.get(position).path != null) {
				LogTool.i("mPath" + mPath);
				LogTool.i(musicDatas.get(position).path);
				if (!mPath.equalsIgnoreCase(musicDatas.get(position).path) || (musicDatas.get(position).seekPostion != 0 && position != prePosition)) {
					mPath = musicDatas.get(position).path;
					try {
						mp.reset();
						mp.setDataSource(mPath);
						isSetDataSource = true;
						prePosition = position;
						LogTool.i("setDataSource" + mPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else if (length == 1) {
					try {
						mPath = musicDatas.get(position).path;
						mp.reset();
						mp.setDataSource(mPath);
						isSetDataSource = true;
						prePosition = position;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			setup();
			init();
			if (position != -1) {
				Intent intent1 = new Intent();
				intent1.setAction(Contsant.PlayAction.MUSIC_LIST);
				intent1.putExtra("position", position);
				sendBroadcast(intent1);
				Intent intent2 = new Intent("com.app.musictitle");
				intent2.putExtra("title", musicDatas.get(position).title);
				sendBroadcast(intent2);
				Intent playIntent = new Intent(Contsant.PlayAction.MUSIC_PLAY);
				sendBroadcast(playIntent);
			}
			/**
			 * 初始化数据
			 */
			int op = intent.getIntExtra("op", -1);
			LogTool.i("op" + op);
			if (op != -1) {
				switch (op) {
					case Contsant.PlayStatus.PLAY:// 播放
						if (!mp.isPlaying()) {
							play();
						}
						break;
					case Contsant.PlayStatus.PAUSE:// 暂停
						isSetDataSource = false;
						if (mp.isPlaying()) {
							pause();
						}
						break;
					case Contsant.PlayStatus.STOP:// 停止
						isSetDataSource = false;
						stop();
						break;
					case Contsant.PlayStatus.PROGRESS_CHANGE:// 进度条改变
						isSetDataSource = false;
						currentTime = intent.getExtras().getLong("progress");
						mp.seekTo(currentTime);
						break;
				}
			}
			ShowNotifcation();
		} else {
			LogTool.e("------------------------bundle == null----------------------------------");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/***播放*/
	private void play() {
		if (mp != null) {
			mp.start();
			if(handler != null){
				Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 1, 0);
				msg.sendToTarget();
			}
		}
		flag = 1;

	}

	/**暂停*/
	private void pause() {
		if (mp != null) {
			mp.pause();
			if(handler != null){
				Message msg = handler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC, 0, 0);
				msg.sendToTarget();
			}
		}
		flag = 1;
	}

	/** 停止*/
	private void stop() {
		if (mp != null) {
			LogTool.i("mp.stop();");
			mp.stop();

			if(handler != null){
				handler.removeMessages(1);
			}
			//add by victor 通知前台关闭播放动画
			Intent intent = new Intent(Contsant.PlayAction.MUSIC_STOP);
			sendBroadcast(intent);
		}
	}

	/** 初始化来着*/
	private void init() {
		final Intent intent = new Intent();
		if (handler == null) {
			handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);

					switch (msg.what) {
						case Contsant.PlayStatus.STATE_PREPARED:
							intent.setAction(Contsant.PlayAction.MUSIC_PREPARED);
							currentTime =  mp.getCurrentPosition();
							intent.putExtra("currentTime", currentTime);
							sendBroadcast(intent);
							handler.sendEmptyMessage(Contsant.PlayStatus.STATE_INFO);
							break;
						case Contsant.PlayStatus.STATE_INFO:
								if(mp != null){
									intent.setAction(Contsant.PlayAction.MUSIC_CURRENT);
									currentTime =  mp.getCurrentPosition();
									intent.putExtra("currentTime", currentTime);
									broadCastMusicInfo(intent);
								}
							if(handler != null){
								handler.sendEmptyMessageDelayed(Contsant.PlayStatus.STATE_INFO, 500);
							}
							break;
						case Contsant.Action.PLAY_PAUSE_MUSIC:
							LogTool.i("PLAY_PAUSE_MUSIC"+msg.arg1);
							intent.setAction(Contsant.PlayAction.PLAY_PAUSE_NEXT);
							intent.putExtra("isPlaying", msg.arg1);
							sendBroadcast(intent);
					}
				}
			};
		}
	}

	/** 初始化1*/
	private void setup() {
		try {
			if (!mp.isPlaying()) {
				mp.prepareAsync();
			}
			mp.setOnPreparedListener(mPreparedListener);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		duration = mp.getDuration();
		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_DURATION);
		broadCastMusicInfo(intent);
	}

	/** 获得随机位置*/
	private int getRandomPostion(boolean loopAll) {
		int ret = -1;

		if (MusicPlayFragment.randomNum < musicDatas.size() - 1) {
			MusicPlayFragment.randomIDs[MusicPlayFragment.randomNum] = position;
			ret = MusicPlayFragment.findRandomSound(musicDatas.size());
			MusicPlayFragment.randomNum++;

		} else if (loopAll == true) {
			MusicPlayFragment.randomNum = 0;
			for (int i = 0; i < musicDatas.size(); i++) {
				MusicPlayFragment.randomIDs[i] = -1;
			}
			MusicPlayFragment.randomIDs[MusicPlayFragment.randomNum] = position;
			ret = MusicPlayFragment.findRandomSound(musicDatas.size());
			MusicPlayFragment.randomNum++;
		}

		return ret;
	}

	/**
	 * 下一首
	 */
	private void nextOne() {
		switch (MusicActivity.loop_flag) {
			case Contsant.LoopMode.LOOP_ORDER://顺序播放
				if (position == musicDatas.size() - 1) {
					stop();
					return;
				} else if (position < musicDatas.size() - 1) {
					position++;
				}
				break;
			case Contsant.LoopMode.LOOP_ONE://单曲循环播放
				//不做操作，继续播放当前歌曲
				break;
			case Contsant.LoopMode.LOOP_ALL://全部循环播放
				position++;
				if (position == musicDatas.size()) {
					position = 0;
				}
				break;
			case Contsant.LoopMode.LOOP_RANDOM://随机播放
				int i = getRandomPostion(false);
				if (i == -1) {
					stop();
					return;
				} else {
					position = i;
				}
				break;
		}

		try {
			mp.reset();
			mPath = musicDatas.get(position).path;
			mp.setDataSource(mPath);
			isSetDataSource = true;
			prePosition = position;
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.removeMessages(1);

		setup();
		init();
		play();

		Bundle bundle = new Bundle();
		bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
		bundle.putInt(Contsant.POSITION_KEY, position);
		DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变

		//这里不能再发播放下一曲的广播，因为当前已经播放的是下一曲
		/*Intent intent0 = new Intent();
		intent0.setAction(MUSIC_NEXT);
		intent0.putExtra("position", position);
		sendBroadcast(intent0);*/

		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_LIST);
		intent.putExtra("position", position);
		sendBroadcast(intent);

		Intent intent1 = new Intent();
		intent1.setAction(Contsant.PlayAction.MUSIC_UPDATE);
		intent1.putExtra("position", position);
		sendBroadcast(intent1);

		Intent intent2 = new Intent("com.app.musictitle");
		intent2.putExtra("title", musicDatas.get(position).title);
		sendBroadcast(intent2);
		ShowNotifcation();
	}

	/** 上一首*/
	private void lastOne() {
		ShowNotifcation();
		if (musicDatas.size() == 1) {
			position = position;

		} else if (position == 0) {
			position = musicDatas.size() - 1;
		} else if (position > 0) {
			position--;
		}
		try {
			LogTool.i(musicDatas.get(position).path);
			mPath = musicDatas.get(position).path;
			mp.reset();
			mp.setDataSource(mPath);
			isSetDataSource = true;
			prePosition = position;
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.removeMessages(1);
		setup();
		init();
		play();

		Intent intent = new Intent();
		intent.setAction(Contsant.PlayAction.MUSIC_LIST);
		intent.putExtra("position", position);
		sendBroadcast(intent);

		Intent intent1 = new Intent();
		intent1.setAction(Contsant.PlayAction.MUSIC_UPDATE);
		intent1.putExtra("position", position);
		sendBroadcast(intent1);

		Intent intent2 = new Intent("com.app.musictitle");
		intent2.putExtra("title", musicDatas.get(position).title);
		sendBroadcast(intent2);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		nextOne();
	}

	/** 操作数据库*/
	private void DBOperate(int pos) {
		dbHelper = new DBHelper(this, "music.db", null, 2);
		Cursor c = dbHelper.query(pos);
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		try {
			if (c==null||c.getCount()==0){
				ContentValues values = new ContentValues();
				values.put("music_id", pos);
				values.put("clicks", 1);
				values.put("latest", dateString);
				dbHelper.insert(values);
			} else {
				c.moveToNext();
				int clicks = c.getInt(2);
				clicks++;
				ContentValues values = new ContentValues();
				values.put("clicks", clicks);
				values.put("latest", dateString);
				dbHelper.update(values, pos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (c != null) {
			c.close();
			c = null;
		}
		if (dbHelper!=null){
			dbHelper.close();
			dbHelper = null;
		}
	}

	/*** 来电时监听播放状态*/
	protected BroadcastReceiver PhoneListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_ANSWER)) {
				TelephonyManager telephonymanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				switch (telephonymanager.getCallState()) {
					case TelephonyManager.CALL_STATE_RINGING:// 当有来电时候，暂停音乐，可我试了试，只是把声音降低而已
						pause();
						break;
					case TelephonyManager.CALL_STATE_OFFHOOK:
						play();
						break;
					default:
						break;
				}
			}else if(intent.getAction().equals(Intent.ACTION_SHUTDOWN)){
				saveLastPlayInfo();
			}else if(intent.getAction().equals(Contsant.PlayAction.MUSIC_STOP_SERVICE)){
				saveLastPlayInfo();
				if(mp != null){
					mp.stop();
					mp.reset();
				}
				stopSelf();//在service中停止service
			}
		}
	};
	/**
	 * 记录最后一次播放信息
	 * */
	private void saveLastPlayInfo(){
		LogTool.d(position + mMusicName + mSampleRate);
		SharePreferencesUtil.putInt(mContext, Contsant.MUSIC_INFO_POSTION, position);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_NAME, mMusicName);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_FORMAT, mMusicFormat);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_BITRATE, mBitRate);
		SharePreferencesUtil.putString(mContext, Contsant.MUSIC_INFO_SAMPLERATE, mSampleRate);
		SharePreferencesUtil.putLong(mContext, Contsant.MUSIC_INFO_DURATION, duration);
	}
	/** 桌面小插件*/
	protected BroadcastReceiver appWidgetReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("com.app.playmusic")) {
				if (mp.isPlaying()) {
					pause();
					Intent pauseIntent = new Intent("com.app.pause");
					sendBroadcast(pauseIntent);
				} else {
					play();
					Intent playIntent = new Intent("com.app.play");
					sendBroadcast(playIntent);
				}
			} else if (intent.getAction().equals("com.app.nextone")) {
				nextOne();
				Intent playIntent = new Intent("com.app.play");
				sendBroadcast(playIntent);
			} else if (intent.getAction().equals("com.app.lastone")) {
				lastOne();
				Intent playIntent = new Intent("com.app.play");
				sendBroadcast(playIntent);
			} else if (intent.getAction().equals("com.app.startapp")) {
				Intent intent1 = new Intent("com.app.musictitle");
				intent1.putExtra("title", musicDatas.get(position).title);
				sendBroadcast(intent1);
			}
		}
	};

	@Override
	public void update(Observable observable, Object data) {
		if (data instanceof Bundle) {
			Bundle bundle = (Bundle) data;
			int action = bundle.getInt(Contsant.ACTION_KEY);
			int position = bundle.getInt(Contsant.POSITION_KEY);
			if (action == Contsant.Action.REMOVE_MUSIC) {
				if (musicDatas != null && musicDatas.size() > 0) {
					if (position < musicDatas.size()) {
						musicDatas.remove(position);
					}
				}
			}
		}
	}

	private void registerHeadsetPlugReceiver(){
		IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		intentFilter.addAction("android.intent.action.HEADSET_PLUG");
		registerReceiver(headsetPlugReceiver, intentFilter);
	}

	/**
	 * 注册耳机插拔广播
	 */
	protected BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			LogTool.i(action);
			if ("android.media.AUDIO_BECOMING_NOISY".equals(action)) {
				if(intent.hasExtra("state")){
					if(intent.getIntExtra("state", 0)==0){
						pause();
						Toast.makeText(context, context.getResources().getString(R.string.headset_out), Toast.LENGTH_LONG).show();
					}
				}else{
					pause();
					Toast.makeText(context, context.getResources().getString(R.string.headset_out), Toast.LENGTH_LONG).show();
					Log.e(TAG, "!!!intent.hasExtra(state)");
				}
			}else if ("android.intent.action.HEADSET_PLUG".equals(action)) {
				if(intent.getIntExtra("state", 0)==1){
					play();
					Toast.makeText(context,context.getResources().getString(R.string.headset_in), Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	public void showMediaInfo() {
		if (mp == null)
			return;
		mp.getDuration();

		ITrackInfo trackInfos[] = mp.getTrackInfo();
		if (trackInfos != null) {
			int index = -1;
			for (ITrackInfo trackInfo : trackInfos) {
				index++;
				int trackType = trackInfo.getTrackType();
				IMediaFormat mediaFormat = trackInfo.getFormat();
				if (mediaFormat == null) {
				} else if (mediaFormat instanceof IjkMediaFormat) {
					switch (trackType) {
						case ITrackInfo.MEDIA_TRACK_TYPE_VIDEO:
							LogTool.i(getString(R.string.mi_codec) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI));
							LogTool.i(getString(R.string.mi_profile_level) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_PROFILE_LEVEL_UI));
							LogTool.i(getString(R.string.mi_pixel_format) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_PIXEL_FORMAT_UI));
							LogTool.i(getString(R.string.mi_resolution) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_RESOLUTION_UI));
							LogTool.i(getString(R.string.mi_frame_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_FRAME_RATE_UI));
							LogTool.i(getString(R.string.mi_bit_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
							break;
						case ITrackInfo.MEDIA_TRACK_TYPE_AUDIO:
							/*LogTool.i(getString(R.string.mi_codec) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI));
							LogTool.i(getString(R.string.mi_profile_level) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CODEC_PROFILE_LEVEL_UI));
							LogTool.i(getString(R.string.mi_sample_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI));
							LogTool.i(getString(R.string.mi_channels) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_CHANNEL_UI));
							LogTool.i(getString(R.string.mi_bit_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
							LogTool.i(getString(R.string.mi_frame_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_FRAME_RATE_UI));
							LogTool.i(getString(R.string.mi_resolution) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_RESOLUTION_UI));

							LogTool.i(getString(R.string.mi_sample_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_DIGIT_UI));
							LogTool.i(getString(R.string.mi_sample_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_BIT_UI));
							LogTool.i(getString(R.string.mi_sample_rate) + mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_NUMBER_UI));*/
							mSampleRate = mediaFormat.getString(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI);
							mBitRate = mediaFormat.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI);
							musicDatas.get(position).setSampleRate(mSampleRate);
							musicDatas.get(position).setBitRate(mBitRate);
							break;
						default:
							break;
					}
				}
			}
		}
	}

}
