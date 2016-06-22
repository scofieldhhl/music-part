package org.app.enjoy.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.app.enjoy.music.adapter.CategoryAdapter;
import org.app.enjoy.music.adapter.MenuAdapter;
import org.app.enjoy.music.adapter.ViewPagerAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.db.DbDao;
import org.app.enjoy.music.frag.AlbumFragment;
import org.app.enjoy.music.frag.ArtistFragment;
import org.app.enjoy.music.frag.DiyFragment;
import org.app.enjoy.music.frag.MusicListFragment;
import org.app.enjoy.music.frag.MusicPlayFragment;
import org.app.enjoy.music.frag.SearchMusicFragment;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.Menu;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.util.AlbumImgUtil;
import org.app.enjoy.music.view.AttrViewPager;
import org.app.enjoy.music.view.CategoryPopWindow;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.music.view.PagerSlidingTabStrip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import tv.danmaku.ijk.media.player.misc.IjkMediaFormat;


public class MusicActivity extends BaseActivity implements View.OnClickListener,Observer,View.OnTouchListener{
    private String TAG = "MusicActivity";
    private MusicListFragment musicListFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private DiyFragment diyFragment;
    private SearchMusicFragment searchMusicFragment;
    private List<Fragment> frags = new ArrayList<>();
    private ViewPagerAdapter viewPagerAdapter;
    private ImageView mIvSearch,mIvPlay,mIvAdd;
    private LinearLayout mLayoutPlayBottom,mLayoutPlayBottomRight;
    private PagerSlidingTabStrip tabs;
    private AttrViewPager viewPager;

    public static int loop_flag = Contsant.LoopMode.LOOP_ORDER;
    private Menu xmenu;//自定义菜单
    private Toast toast;//提示
    private Timers timer;//倒计时内部对象
    private TextView timers;//显示倒计时的文字
    private CircleImageView mCivAlbum;
    private MovingTextView mMtvTitle;
    private int c;//同上
    private PopupWindow popupWindow;
    private AudioManager audioManager;
    private int maxVolume;// 最大音量
    private int currentVolume;// 当前音量
    private int playStatus;
    private String[] titles = { "All Songs", "Artist","Album", "PlayLists"};
    private List<MusicData> musicDatas = new ArrayList<>();
    private int currentPosition;
    private CategoryPopWindow categoryPopWindow;

    private float xLast,yLast;
    private boolean isNext = false;//是否是下一曲
    private boolean isClick = false;//判断是否是点击事件

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Action.GOTO_MUSIC_LIST_FRAG:
//                    if (viewPager != null) {
//                        viewPager.setCurrentItem(0);
//                    }
                    break;
                case Contsant.Action.GOTO_MUSIC_PLAY_FRAG:
//                    if (viewPager != null) {
//                        viewPager.setCurrentItem(1);
//                    }
                    break;
                case Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED:
                    showPlayInfo();
                    break;
                case Contsant.Msg.SHOW_BOTTOM_PLAY_INFO:
                    showPlayInfo();
                    break;
                case Contsant.Action.MUSIC_STOP:
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                    break;
                case Contsant.PlayStatus.PAUSE:
                    mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        initialize();
        LoadMenu();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private void initialize () {
        registerMusicReceiver();
        DataObservable.getInstance().addObserver(this);
        categoryPopWindow = new CategoryPopWindow(this);

        mIvSearch = (ImageView) findViewById(R.id.iv_search);
        mIvAdd = (ImageView) findViewById(R.id.iv_add);
        mIvPlay = (ImageView) findViewById(R.id.iv_play);
        mLayoutPlayBottom = (LinearLayout) findViewById(R.id.l_play_bottom);
        mLayoutPlayBottomRight = (LinearLayout) findViewById(R.id.l_play_bottom_right);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        viewPager = (AttrViewPager) findViewById(R.id.viewpager);
        viewPager.setCanScroll(false);//屏蔽左右滑动

        musicListFragment = new MusicListFragment();
        artistFragment = new ArtistFragment();
        albumFragment = new AlbumFragment();
        diyFragment = new DiyFragment();
        searchMusicFragment = new SearchMusicFragment();

        frags.add(musicListFragment);
        frags.add(artistFragment);
        frags.add(albumFragment);
        frags.add(diyFragment);
        frags.add(searchMusicFragment);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.setTitles(titles);
        viewPagerAdapter.setFrags(frags);
        viewPager.setAdapter(viewPagerAdapter);
        tabs.setViewPager(viewPager);
        timers=(TextView) findViewById(R.id.timer_clock);
        mCivAlbum = (CircleImageView) findViewById(R.id.civ_album);
        mMtvTitle = (MovingTextView) findViewById(R.id.mtv_title);

        mIvSearch.setOnClickListener(this);
        mIvPlay.setOnClickListener(this);
        mIvAdd.setOnClickListener(this);
        mLayoutPlayBottom.setOnTouchListener(this);
        mLayoutPlayBottom.setOnClickListener(this);

        // 获取系统音乐音量
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取系统音乐当前音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void showPlayInfo () {
        if (musicDatas != null && musicDatas.size() > 0) {
            if (currentPosition < musicDatas.size()) {
                playStatus = Contsant.PlayStatus.PLAY;
                mMtvTitle.setText(musicDatas.get(currentPosition).title);
                mIvPlay.setImageResource(R.drawable.icon_play_bottom_pause);
                String albumId = musicDatas.get(currentPosition).getAlbumId();
                Bitmap bitmap;
                if (!TextUtils.isEmpty(albumId)) {
                    bitmap = AlbumImgUtil.getArtwork(this,musicDatas.get(currentPosition).getId(),Long.parseLong(albumId),false);
                    if (bitmap != null) {
                        mCivAlbum.setImageBitmap(bitmap);
                    } else {
                        mCivAlbum.setImageResource(R.drawable.default_album);
                    }
                } else {
                    mCivAlbum.setImageResource(R.drawable.default_album);
                }
            }
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
                /*if (position == 0) {
                    Intent it = new Intent(MusicActivity.this, SkinSettingActivity.class);
                    startActivityForResult(it, 2);

                } else if (position == 1) {
                    exit();

                }*/
            }
        });
        List<int[]> data2 = new ArrayList<int[]>();
        data2.add(new int[]{R.drawable.btn_menu_setting, R.string.menu_settings});
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
                /*Intent intent = new Intent(MusicActivity.this, AboutActivity.class);
                startActivity(intent);*/

            }
        });
        xmenu.create();
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
        new XfDialog.Builder(MusicActivity.this).setTitle(getResources().getString(R.string.please_enter_time)).
                setView(edtext).setPositiveButton(getResources().getString(R.string.confrim), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialog.cancel();
                /**如果输入小于2或者等于0会告知用户**/
                if (edtext.length() <= 2 && edtext.length() != 0) {
                    if (".".equals(edtext.getText().toString())) {
                        toast = Contsant.showMessage(toast, MusicActivity.this, getResources().getString(R.string.enter_error));
                    } else {
                        final String time = edtext.getText().toString();
                        long Money = Integer.parseInt(time);
                        long cX = Money * 60000;
                        timer= new Timers(cX, 1000);
                        timer.start();//倒计时开始
                        toast = Contsant.showMessage(toast,MusicActivity.this, getResources().getString(R.string.sleep_mode_start)
                                + String.valueOf(time)+ getResources().getString(R.string.close_app));
                        timers.setVisibility(View.INVISIBLE);
                        timers.setVisibility(View.VISIBLE);
                        timers.setText(String.valueOf(time));
                    }

                } else {
                    Toast.makeText(MusicActivity.this, getResources().getString(R.string.please_enter_time_delay), Toast.LENGTH_SHORT).show();
                }

            }
        }).setNegativeButton(R.string.cancel, null).show();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float xDistance,yDistance;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = 0f;
                yDistance = 0f;
                xLast = event.getX();
                yLast = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (musicDatas != null && musicDatas.size() > 0 && !isClick) {
                    if (isNext) {
                        //下一曲
                        currentPosition ++;
                        if (currentPosition == musicDatas.size()) {
                            currentPosition = 0;
                        }
                    } else {
                        //上一曲
                        currentPosition --;
                        if (currentPosition < 0) {
                            currentPosition = musicDatas.size() - 1;
                        }
                    }
                    //如果当前是播放列表MusicListFragment或者搜索SearchMusicFragment则通知播放位置发生改变同步更新UI
                    if (viewPagerAdapter != null) {
                        if (viewPager.getCurrentItem() == 0 || viewPager.getCurrentItem() == 3 || viewPager.getCurrentItem() == 4) {
                            Bundle bundle = new Bundle();
                            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
                            bundle.putInt(Contsant.POSITION_KEY, currentPosition);
                            DataObservable.getInstance().setData(bundle);
                        }
                    }
                    play();
                }else if(isClick){
                    if(currentPosition < musicDatas.size()){
                        playMusic(currentPosition,musicDatas.get(currentPosition).seekPostion);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float curX = event.getX();
                float curY = event.getY();
                xDistance = curX - xLast;
                yDistance = curY - yLast;
                float distance = Math.abs(xDistance) - Math.abs(yDistance);
                LogTool.d("distance:"+distance);
                if(distance == 0 || distance < 10){
                    isClick = true;
                }else {
                    if(xDistance > 50 && distance > 50){//从左向右滑动并且x方向比y方向滑动距离>50
                        Log.e(TAG,">>>>>>>>>>>>>>>>>>>>>>>>>>>>>从左向右滑动");
                        isNext = false;
                    } else {//从右向左滑动
                        Log.e(TAG,"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<从右向左滑动");
                        isNext = true;
                    }
                    isClick = false;
                }
                break;
        }
        return true;
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
//                finish();
//                onDestroy();
            }else {
                finish();
//                onDestroy();
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

    /**
     * 退出程序方法
     */
    private void exit(){
        Intent mediaServer = new Intent(MusicActivity.this, MusicService.class);
        stopService(mediaServer);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_search:
                if (viewPager != null) {
                    viewPager.setCurrentItem(4);
                }
                break;
            case R.id.iv_play:
                if (musicDatas != null && musicDatas.size() > 0) {
                    if (playStatus == Contsant.PlayStatus.PLAY) {
                        pause();
                        playStatus = Contsant.PlayStatus.PAUSE;

                    } else {
                        play();
                        playStatus = Contsant.PlayStatus.PLAY;
                    }
                }
                break;
            case R.id.iv_add:
                if (musicDatas != null && musicDatas.size() > 0) {
                    if (currentPosition < musicDatas.size()) {
                        categoryPopWindow.setData(musicDatas.get(currentPosition));
                        categoryPopWindow.showPopWindow(mLayoutPlayBottomRight);
                    }
                }
                break;
            case R.id.l_play_bottom:
                LogTool.d("l_play_bottom:");
                startActivity(new Intent(MusicActivity.this, MusicPlayActivity.class));
                break;
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Integer) {
            int action = (int) data;
            if (action == Contsant.Action.GOTO_MUSIC_PLAY_FRAG) {
                mHandler.sendEmptyMessage(Contsant.Action.GOTO_MUSIC_PLAY_FRAG);
            } else if (action == Contsant.Action.GOTO_MUSIC_LIST_FRAG) {
                mHandler.sendEmptyMessage(Contsant.Action.GOTO_MUSIC_LIST_FRAG);
            }
        } else if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            int position = bundle.getInt(Contsant.POSITION_KEY);

            if (action == Contsant.Action.UPDATE_MUSIC) {
                musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
                mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
            }  else  if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变
                if(position < musicDatas.size()) {
                    currentPosition = position;
                    mHandler.sendEmptyMessage(Contsant.Msg.CURRENT_PLAY_POSITION_CHANGED);
                }
            } else if (action == Contsant.Action.MUSIC_STOP) {//后台发过来的播放位置改变前台同步改变
                pause();
            } else if (action == Contsant.Action.REMOVE_MUSIC) {
                if (musicDatas != null && musicDatas.size() > 0) {
                    if (position < musicDatas.size()) {
                        musicDatas.remove(position);
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if(popupWindow != null && popupWindow.isShowing()){
                popupWindow.dismiss();
            }else{
                new XfDialog.Builder(MusicActivity.this).setTitle(R.string.info).setMessage(R.string.dialog_messenge).setPositiveButton(R.string.confrim, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exit();

                    }
                }).setNeutralButton(R.string.cancel, null).show();
            }

            return false;
        }else if(keyCode == KeyEvent.KEYCODE_BACK && popupWindow.isShowing()){
            popupWindow.dismiss();
        }*/
        finish();
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
     * 回调音量大小函数
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_UP) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                return false;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
        unregisterReceiver(musicReceiver);
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public void play() {
        Log.e(TAG,"play-currentPosition=" + currentPosition);
        mIvPlay.setImageResource(R.drawable.icon_play_bottom_pause);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, currentPosition);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", 1);// 向服务传递数据
        intent.setPackage(getPackageName());
        startService(intent);
        showPlayInfo();
    }

    /**
     * 暂停
     */
    public void pause() {
        mIvPlay.setImageResource(R.drawable.icon_play_bottom_play);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, currentPosition);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", Contsant.PlayStatus.PAUSE);
        intent.setPackage(getPackageName());
        startService(intent);
    }

    /**
     * 初始化注册广播
     */
    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contsant.PlayAction.MUSIC_LIST);
        filter.addAction(Contsant.PlayAction.MUSIC_LAST);
        filter.addAction(Contsant.PlayAction.MUSIC_NEXT);
        filter.addAction(Contsant.PlayAction.MUSIC_UPDATE);
        filter.addAction(Contsant.PlayAction.MUSIC_STOP);
        registerReceiver(musicReceiver, filter);

    }
    /**在后台MusicService里使用handler消息机制，不停的向前台发送广播，广播里面的数据是当前mp播放的时间点，
     * 前台接收到广播后获得播放时间点来更新进度条,暂且先这样。但是一些人说虽然这样能实现。但是还是觉得开个子线程不错**/
    protected BroadcastReceiver musicReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Contsant.PlayAction.MUSIC_LIST)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    if (bundle.containsKey(Contsant.MUSIC_LIST_KEY)) {
                        currentPosition = bundle.getInt(Contsant.POSITION_KEY);
                        musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
                        mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
                    }
                }
            } else if (action.equals(Contsant.PlayAction.MUSIC_UPDATE)) {
                currentPosition = intent.getExtras().getInt("position");;
                mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
            } else if (action.equals(Contsant.PlayAction.MUSIC_NEXT)) {
                currentPosition = intent.getExtras().getInt("position");;
                mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
            }else if (action.equals(Contsant.PlayAction.MUSIC_LAST)) {
                currentPosition = intent.getExtras().getInt("position");;
                mHandler.sendEmptyMessage(Contsant.Msg.SHOW_BOTTOM_PLAY_INFO);
            } else if(action.equals(Contsant.PlayStatus.PAUSE)){
                mHandler.sendEmptyMessage(Contsant.PlayStatus.PAUSE);
            } else if (action.equals(Contsant.PlayAction.MUSIC_STOP)) {
                mHandler.sendEmptyMessage(Contsant.Action.MUSIC_STOP);
            }
        }
    };

    /**
     * 根据Position播放音乐
     */
    public void playMusic(int position, long seekPosition) {
        if (musicDatas.size() > 0) {

            Intent intent = new Intent(MusicActivity.this,MusicPlayActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            bundle.putLong(Contsant.SEEK_POSITION, seekPosition);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            final XfDialog xfdialog = new XfDialog.Builder(MusicActivity.this).setTitle(getResources().getString(R.string.tip)).
                    setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                    setPositiveButton(getResources().getString(R.string.confrim), null).create();
            xfdialog.show();
        }
    }

    public int getCurrentPage () {
        if (viewPager != null) {
            return viewPager.getCurrentItem();
        }
        return 0;
    }
}
