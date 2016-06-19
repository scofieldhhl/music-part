package org.app.enjoy.musicplayer;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.MenuAdapter;
import org.app.enjoy.music.adapter.MusicListAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.CueUtil;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.Menu;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.view.SwipeBackLayout;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * @Description 进入程序时，直接进入音乐列表，这里主要功能包括了
 * 1.自动扫描音乐并显示列表里
 *  2.支持MP3,M4A格式
 *  3.支持换肤
 *  4.定时关闭
 *  5.夜间+日间模式
 *  6.查看音乐详细信息
 *  7.设定一音乐为来电铃声/通知/警告铃声
 *  最最主要的是依靠MediaStore这个大类。负责搜集所有音乐的信息,读音乐信息也是用它读取的。
 *  音乐信息正确写法是MediaStore.Audio.Media.XXXX.但是你要漂亮首先要自定义适配器(Adapter)。
 *  在相应的界面定义一个方法，还有就是定义三个变量(id,title,artits)分别代表歌的id，歌的标题，艺术家，先实例化后循环获取它们各自的索引列。
 *  这次我取消了扫描功能
 * @Author scofield.hhl@gmail.com
 */
public class MusicListActivity extends BaseActivity implements OnClickListener,OnItemClickListener,AdapterView.OnItemLongClickListener,Observer{
	private final String TAG = "MusicListActivity";
	private ImageButton mIbRight;
	/*** 音乐列表 **/
	private ListView mLvSongs;
	private Menu xmenu;//自定义菜单
	private int num;//num确定一个标识
	private int c;//同上
	private LayoutInflater inflater;//装载布局
	private LayoutParams params;
	private Toast toast;//提示
	/**铃声标识常量**/
	public static final int Ringtone = 0;
	public static final int Alarm = 1;
	public static final int Notification = 2;
	private TextView timers;//显示倒计时的文字
	private Timers timer;//倒计时内部对象
//	private ImageButton plays_btn;
	private int currentPosition = 0;
	private int lastLongClickPosition = -1;
	private MusicListAdapter musicListAdapter;
	private List<MusicData> musicDatas = new ArrayList<MusicData>();
	private SwipeBackLayout swipeBackLayout;
	private String[] mArrSongs;
	private PopupWindow popupWindow;
	private long mSeekPosition = 0L;

	private int currentMusicId = -1;//当前播放音乐id
	private boolean isMusicRemove;//是否有音乐被移除

	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case Contsant.Msg.UPDATE_PLAY_LIST:
					if (musicListAdapter != null) {
						musicListAdapter.setCurrentPosition(currentPosition);
					}
					break;
				case Contsant.Msg.UPDATE_PLAY_LIST_EXTENSION:
					if (musicListAdapter != null)
						musicListAdapter.notifyDataSetChanged();
					break;
				case Contsant.Msg.PLAY_CUE:
//					if(mCueSongBeanList != null && mCueSongBeanList.size() > 0){
////						initPopWindow(mCueSongBeanList);
//					}
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_list);

		initialize();
		initData();
		//异步检索其他音频文件
		new Thread(){
			@Override
			public void run() {
				GetFiles(getSDPath(),arrExtension, true);
			}
		}.start();
	}

	private void initialize () {
		DataObservable.getInstance().addObserver(this);
		swipeBackLayout = (SwipeBackLayout) LayoutInflater.from(this).inflate(R.layout.base, null);
		swipeBackLayout.attachToActivity(this);
		swipeBackLayout.setScrollMode(SwipeBackLayout.LEFT_MODE);
		mIbRight = (ImageButton) findViewById(R.id.ib_right);
		mLvSongs = (ListView) findViewById(R.id.local_music_list);
		mLvSongs.setOnItemClickListener(this);
		mLvSongs.setOnItemLongClickListener(this);
		timers=(TextView) findViewById(R.id.timer_clock);
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		params = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
//		plays_btn=(ImageButton) findViewById(R.id.plays_btn);
//		plays_btn.setOnClickListener(listeners);
		mIbRight.setOnClickListener(this);

		musicListAdapter = new MusicListAdapter(this);
		musicListAdapter.setDatas(musicDatas);
		mLvSongs.setAdapter(musicListAdapter);

		LoadMenu();
	}

	/**
	 * 显示MP3信息,其中_ids保存了所有音乐文件的_ID，用来确定到底要播放哪一首歌曲，_titles存放音乐名，用来显示在播放界面，
	 * 而_path存放音乐文件的路径（删除文件时会用到）。
	 */
	private void initData () {
		// 用游标查找媒体详细信息
		Cursor cursor = this.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Media.TITLE,// 标题，游标从0读取
						MediaStore.Audio.Media.DURATION,// 持续时间,1
						MediaStore.Audio.Media.ARTIST,// 艺术家,2
						MediaStore.Audio.Media._ID,// id,3
						MediaStore.Audio.Media.DISPLAY_NAME,// 显示名称,4
						MediaStore.Audio.Media.DATA,// 数据，5
						MediaStore.Audio.Media.ALBUM_ID,// 专辑名称ID,6
						MediaStore.Audio.Media.ALBUM,// 专辑,7
						MediaStore.Audio.Media.SIZE }, null, null, MediaStore.Audio.Media.ARTIST);// 大小,8
		/** 判断游标是否为空，有些地方即使没有音乐也会报异常。而且游标不稳定。稍有不慎就出错了,其次，如果用户没有音乐的话，不妨可以告知用户没有音乐让用户添加进去 */
		if (cursor!= null && cursor.getCount() == 0) {
			final XfDialog xfdialog = new XfDialog.Builder(MusicListActivity.this).setTitle(getResources().getString(R.string.tip)).
					setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
					setPositiveButton(getResources().getString(R.string.confrim), null).create();
			xfdialog.show();
			return;

		}
		/**
		 * 这里获取路径的格式是_path[i]=c.geString,为什么这么写？是因为MediaStore.Audio.Media.DATA的关系
		 * 得到的内容格式为/mnt/sdcard/[子文件夹名/]音乐文件名，而我们想要得到的是/sdcard/[子文件夹名]音乐文件名
		 */
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			MusicData md = new MusicData();
			md.title = cursor.getString(0);
			md.duration = cursor.getLong(1);
			md.artist = cursor.getString(2);
			md.id = cursor.getInt(3);
			md.displayName = cursor.getString(4);
			md.data = cursor.getString(5);
			if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){
				md.path = md.data.replace("rage","/storage");
			}else{
				md.path = md.data;
			}
			md.albumId = cursor.getString(6);
			md.album = cursor.getString(7);
			md.size = cursor.getString(8);
			musicDatas.add(md);
			if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){
				List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);
				if(cueMusicList != null && cueMusicList.size() > 0){
					musicDatas.addAll(cueMusicList);
				}
			}
		}
		musicListAdapter.notifyDataSetChanged();
	}

	@Override
	public void onResume() {
		super.onResume();
		// 设置皮肤背景
		Setting setting = new Setting(this, false);
		mLvSongs.setBackgroundResource(setting.getCurrentSkinResId());//这里我只设置listview的皮肤而已。
		MobclickAgent.onResume(this);
		Intent intentServer = new Intent(MusicListActivity.this, MusicService.class);
		startService(intentServer);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.ib_right:
				//这里处理如果删除了播放列表的歌曲重新找到当前播放歌曲的position
				if (isMusicRemove) {
					if (musicDatas != null && musicDatas.size() > 0) {
						currentPosition = 0;
						for (int i=0;i<musicDatas.size();i++) {
							if (musicDatas.get(i).id == currentMusicId) {
								currentPosition = i;
								break;
							}
						}
					}
				}
				playMusic(currentPosition, 0);
				break;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
		lastLongClickPosition = position;
		if (musicListAdapter != null) {
			musicListAdapter.cancelLongClick(false);
			musicListAdapter.setCurrentLongPosition(position);
		}

		return true;
	}

	@Override
	public void update(Observable observable, Object data) {
		if (data instanceof Bundle) {
			Bundle bundle = (Bundle) data;
			int action = bundle.getInt(Contsant.ACTION_KEY);
			int position = bundle.getInt(Contsant.POSITION_KEY);

			//如果有音乐被移除
			if (action == Contsant.Action.REMOVE_MUSIC) {
				isMusicRemove = true;
			} else if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变
				if(position < musicDatas.size()) {
					currentPosition = position;
					mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);
				}
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		LogTool.i("start play position = " + position);
		playMusic(position, 0);
		currentPosition = position;
		currentMusicId = musicDatas.get(position).id;
		if (musicListAdapter != null) {
			musicListAdapter.cancelLongClick(true);
		}
		if (musicListAdapter != null) {
			musicListAdapter.setCurrentPosition(position);
		}
	}

	/**
	 * 初始化菜单
	 */
	private void LoadMenu() {
		xmenu = new Menu(this);
		List<int[]> data1 = new ArrayList<int[]>();
		data1.add(new int[]{R.drawable.btn_menu_skin, R.string.skin_settings});
		data1.add(new int[]{R.drawable.btn_menu_exit, R.string.menu_exit_txt});

		xmenu.addItem(getResources().getString(R.string.common), data1, new MenuAdapter.ItemListener() {

			@Override
			public void onClickListener(int position, View view) {
				xmenu.cancel();
				if (position ==0) {
					Intent it = new Intent(MusicListActivity.this,SkinSettingActivity.class);
					startActivityForResult(it, 2);

				} else if (position ==1) {
					exit();

				}
			}
		});
		List<int[]> data2 = new ArrayList<int[]>();
		data2.add(new int[] { R.drawable.btn_menu_setting,R.string.menu_settings });
		data2.add(new int[]{R.drawable.btn_menu_sleep, R.string.menu_time_txt});
		Setting setting = new Setting(this, false);
		String brightness=setting.getValue(Setting.KEY_BRIGHTNESS);
		if(brightness != null && brightness.equals("0")) {//夜间模式
			data2.add(new int[]{R.drawable.btn_menu_brightness, R.string.brightness_title});
		} else {
			data2.add(new int[]{R.drawable.btn_menu_darkness, R.string.darkness_title});
		}
		xmenu.addItem(getResources().getString(R.string.tool), data2, new MenuAdapter.ItemListener() {

			@Override
			public void onClickListener(int position, View view) {
				xmenu.cancel();
				if (position == 0) {

				} else if (position == 1) {
					Sleep();
				} else if (position == 2) {
					setBrightness(view);
				}
			}
		});
		List<int[]> data3 = new ArrayList<int[]>();
		data3.add(new int[]{R.drawable.btn_menu_about, R.string.about_title});
		xmenu.addItem(getResources().getString(R.string.help), data3, new MenuAdapter.ItemListener() {
			@Override
			public void onClickListener(int position, View view) {
				xmenu.cancel();
				Intent intent = new Intent(MusicListActivity.this,AboutActivity.class);
				startActivity(intent);

			}
		});
		xmenu.create();
	}
	/**
	 * 根据Position播放音乐
	 */
	public void playMusic(int position, long seekPosition) {
		if (musicDatas.size() > 0) {
			play(position);

			Intent intent = new Intent(MusicListActivity.this,PlayMusicActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
			bundle.putInt(Contsant.POSITION_KEY, position);
			bundle.putLong(Contsant.SEEK_POSITION, seekPosition);
			intent.putExtras(bundle);
			startActivity(intent);
		} else {
			final XfDialog xfdialog = new XfDialog.Builder(MusicListActivity.this).setTitle(getResources().getString(R.string.tip)).
					setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
					setPositiveButton(getResources().getString(R.string.confrim), null).create();
			xfdialog.show();
		}
	}
	public void play(int position) {
		LogTool.i("play---startService");
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
		bundle.putInt(Contsant.POSITION_KEY, position);
		intent.putExtras(bundle);
		intent.setAction("com.app.media.MUSIC_SERVICE");
		intent.putExtra("op", 1);// 向服务传递数据
		intent.setPackage(getPackageName());
		startService(intent);

	}
	/**
	 * 复写菜单方法
	 */
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		menu.add("menu");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuOpened(int featureId, android.view.Menu menu) {
		/** 菜单在哪里显示。参数1是该布局总的ID，第二个位置，第三，四个是XY坐标 **/
		xmenu.showAtLocation(findViewById(R.id.rl_parent_cotent), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
		/** 如果返回true的话就会显示系统自带的菜单，反之返回false的话就显示自己写的。 **/
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==1&&resultCode==1) {
			Setting setting = new Setting(this, false);
//			this.getWindow().setBackgroundDrawableResource(setting.getCurrentSkinResId());
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if(popupWindow != null && popupWindow.isShowing()){
				popupWindow.dismiss();
			}else{
				new XfDialog.Builder(MusicListActivity.this).setTitle(R.string.info).setMessage(R.string.dialog_messenge).setPositiveButton(R.string.confrim, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						exit();

					}
				}).setNeutralButton(R.string.cancel, null).show();
			}

			return false;
		}else if(keyCode == KeyEvent.KEYCODE_BACK && popupWindow.isShowing()){
			popupWindow.dismiss();
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (popupWindow != null && popupWindow.isShowing()) {
			popupWindow.dismiss();
			popupWindow = null;
		}
		return super.onTouchEvent(event);
	}


	/**
	 * 休眠方法
	 */
	private void Sleep(){
		final EditText edtext = new EditText(this);
		edtext.setText("5");//设置初始值
		edtext.setKeyListener(new DigitsKeyListener(false, true));
		edtext.setGravity(Gravity.CENTER_HORIZONTAL);//设置摆设位置
		edtext.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));//字体类型
		edtext.setTextColor(Color.BLUE);//字体颜色
		edtext.setSelection(edtext.length());//设置选择位置
		edtext.selectAll();//全部选择
		new XfDialog.Builder(MusicListActivity.this).setTitle(getResources().getString(R.string.please_enter_time)).
				setView(edtext).setPositiveButton(getResources().getString(R.string.confrim), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				dialog.cancel();
				/**如果输入小于2或者等于0会告知用户**/
				if (edtext.length() <= 2 && edtext.length() != 0) {
					if (".".equals(edtext.getText().toString())) {
						toast = Contsant.showMessage(toast,MusicListActivity.this, getResources().getString(R.string.enter_error));
					} else {
						final String time = edtext.getText().toString();
						long Money = Integer.parseInt(time);
						long cX = Money * 60000;
						timer= new Timers(cX, 1000);
						timer.start();//倒计时开始
						toast = Contsant.showMessage(toast,MusicListActivity.this, getResources().getString(R.string.sleep_mode_start)
								+ String.valueOf(time)+ getResources().getString(R.string.close_app));
						timers.setVisibility(View.INVISIBLE);
						timers.setVisibility(View.VISIBLE);
						timers.setText(String.valueOf(time));
					}

				} else {
					Toast.makeText(MusicListActivity.this, getResources().getString(R.string.please_enter_time_delay),Toast.LENGTH_SHORT).show();
				}

			}
		}).setNegativeButton(R.string.cancel, null).show();

	}
	/**
	 * 产生一个倒计时
	 */
	private class Timers extends CountDownTimer{

		public Timers(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			if (c==0) {
				exit();
				finish();
				onDestroy();
			}else {
				finish();
				onDestroy();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}

		@Override
		public void onTick(long millisUntilFinished) {
			timers.setText("" + millisUntilFinished / 1000 / 60 + ":"+ millisUntilFinished / 1000 % 60);
			// 假如这个数大于9 说明就是2位数了,可以直接输入。假如小于等于9 那就是1位数。所以前面加一个0
			String abc = (millisUntilFinished / 1000 / 60) > 9 ? (millisUntilFinished / 1000 / 60)+ "": "0" + (millisUntilFinished / 1000 / 60);
			String b = (millisUntilFinished / 1000 % 60) > 9 ? (millisUntilFinished / 1000 % 60)+ "": "0" + (millisUntilFinished / 1000 % 60);
			timers.setText(abc + ":" + b);
			timers.setVisibility(View.GONE);
		}

	}
	/**
	 * 退出程序方法
	 */
	private void exit(){
		Intent mediaServer = new Intent(MusicListActivity.this, MusicService.class);
		stopService(mediaServer);
		finish();
	}

	@Override
	protected void onDestroy() {
		DataObservable.getInstance().deleteObserver(this);
		super.onDestroy();
	}

	public int getCurrentPosition () {
		return currentPosition;
	}

	/**
	 * 遍历文件夹，搜索指定扩展名的文件
	 * */
	private String[] arrExtension = new String[]{"dsf","dff","dst","dsd", "wma", "aif", "aac","lrc"};
	public void GetFiles(String Path, String[] arrExtension, boolean IsIterative)  //搜索目录，扩展名，是否进入子文件夹
	{
		if(Path == null || TextUtils.isEmpty(Path)){
			return;
		}
		File[] files = new File(Path).listFiles();

		for (int i = 0; i < files.length; i++)
		{
			File f = files[i];
			if (f.isFile())
			{
				String[] arrFile = f.getPath().split("\\.");
				if(arrFile != null && arrFile.length >0){
					int length = arrFile.length;
					if(arrFile[length -1] != null){
						for(String str : arrExtension){
							if(arrFile[length -1].equalsIgnoreCase(str)){
								MusicData md = new MusicData();
								String[] arrFileName = f.getPath().split("/");
								md.title = arrFile[length -1];
								if(arrFileName != null && arrFileName.length > 0){
									md.title = arrFileName[arrFileName.length - 1].substring(0,arrFileName[arrFileName.length - 1].indexOf("."));
								}
								md.duration = 0;
								md.artist = "";
								md.displayName = md.title;
								md.data = f.getPath();
								md.path = f.getPath();
								LogTool.i( f.getPath());
								md.size = String.valueOf(f.length());
								musicDatas.add(md);
								mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST_EXTENSION);
								break;
							}
						}
					}
				}

				if (!IsIterative)
					break;
			}
			else if (f.isDirectory() && f.getPath().indexOf("/.") == -1)  //忽略点文件（隐藏文件/文件夹）
				GetFiles(f.getPath(), arrExtension, IsIterative);
		}
	}

	public String getSDPath(){
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState()
				.equals(Environment.MEDIA_MOUNTED);   //判断sd卡是否存在
		if(sdCardExist){
			sdDir = Environment.getExternalStorageDirectory();//获取跟目录
		}
		if(sdDir != null){
			return sdDir.toString();
		}else{
			return null;
		}
	}
}
