package org.app.enjoy.music.frag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LRCbean;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.util.CubeLeftOutAnimation;
import org.app.enjoy.music.util.CubeLeftOutBackAnimation;
import org.app.enjoy.music.util.CubeRightInAnimation;
import org.app.enjoy.music.util.CubeRightInBackAnimation;
import org.app.enjoy.music.view.CircularSeekBar;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.music.view.SwipeBackLayout;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.Serializable;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import tv.danmaku.ijk.media.player.misc.IjkMediaFormat;

/**
 * Created by Administrator on 2016/6/2.
 */
public class MusicPlayFragment extends Fragment implements View.OnClickListener,CircularSeekBar.OnCircularSeekBarChangeListener,Observer {

    private String TAG = "MusicPlayFragment";
    private LinearLayout mLayoutActPlay;
    private MovingTextView mMTvMusicName;// 音乐名
    private String mMusicName = "";
    private TextView mTvArtisting;// 艺术家
    private ImageButton mIbFore;
    private ImageButton mIbPlay;// 播放按钮
    private ImageButton mIbPre;// 上一首
    private ImageButton mIbNext,mIbShare;// 下一首
    private ImageButton mIbLoopMode;//播放模式切换按钮
    private TextView mTvDurationTime;// 歌曲时间
    private CircularSeekBar mCsbProgress;// 进度条
    private int position;// 定义一个位置，用于定位用户点击列表曲首
    private long currentTime;// 当前播放位置
    private long duration;// 总时间
    private ImageButton mIbBack;
    private ImageView mIvMusicCd;


    public static int loop_flag = Contsant.LoopMode.LOOP_ORDER;
    public static boolean random_flag = false;
    public static int[] randomIDs = null;
    public static int randomNum = 0;
    private int flag;// 标记
    private Cursor cursor;// 游标
    private TreeMap<Integer, LRCbean> lrc_map = new TreeMap<Integer, LRCbean>();// Treemap对象
    private AudioManager audioManager;
    private int maxVolume;// 最大音量
    private int currentVolume;// 当前音量
    private ImageButton mIbVoice;//右上角的音量图标
    private Toast toast;//提示消息
    private Context mContext;//上下文。这个重要！
    private Animation rotateAnim;//音乐光盘旋转动画
    private boolean isSilence = false;//是否静音

    private List<MusicData> musicDatas;

    private CubeLeftOutAnimation cubeLeftOutAnimation;
    private CubeRightInAnimation cubeRightInAnimation;
    private CubeLeftOutBackAnimation cubeLeftOutBackAnimation;
    private CubeRightInBackAnimation cubeRightInBackAnimation;
    private long mSeekPosition = 0L;
    private long mPositionSeek = 0;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Contsant.Action.CURRENT_TIME_MUSIC:
                    mCsbProgress.setProgress((int)currentTime);// 设置进度条
                    mCsbProgress.setMax((int)duration);
                    mTvDurationTime.setText(toTime((int)currentTime)+"|"+toTime((int)duration));
                    break;
                case Contsant.Action.DURATION_MUSIC:
                    mCsbProgress.setMax((int)duration);
                    mTvDurationTime.setText(toTime((int) currentTime) + "|" + toTime((int) duration));
                    showAudioInfo();
                    break;
                case Contsant.Action.NEXTONE_MUSIC:
                    nextOne();
                    break;
                case Contsant.Action.UPDATE_MUSIC:
                    DataObservable.getInstance().setData(position);//通知播放列表播放位置变化
                    setup();
                    break;
                case Contsant.Action.PLAY_PAUSE_MUSIC:
                    if (msg.arg1 == 1) {
                        openAnim();
                        mIvMusicCd.startAnimation(rotateAnim);
                    } else if (msg.arg1 == 0) {
                        closeAnim();
                        mIvMusicCd.clearAnimation();
                    }
                    break;
                case Contsant.Action.MUSIC_STOP:
                    flag =  Contsant.PlayStatus.PAUSE;
                    closeAnim();
                    mIvMusicCd.clearAnimation();
                    break;
                case Contsant.Msg.PLAY_MUSIC:
                    setup();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_play,container, false);

        flag = Contsant.PlayStatus.PLAY;
        initialize(view);
        return view;
    }

    private void initialize (View view) {
        DataObservable.getInstance().addObserver(this);
        mLayoutActPlay=(LinearLayout) view.findViewById(R.id.l_activity_play);
        mIbBack = (ImageButton)view.findViewById(R.id.ib_back);
        mMTvMusicName = (MovingTextView) view.findViewById(R.id.mtv_music_name);
        mTvArtisting = (TextView) view.findViewById(R.id.tv_format);
        mIbFore = (ImageButton) view.findViewById(R.id.ib_fore);
        mIbPlay = (ImageButton) view.findViewById(R.id.ib_play);
        mIbPre = (ImageButton) view.findViewById(R.id.ib_pre);
        mIbNext = (ImageButton) view.findViewById(R.id.ib_next);
        mIbLoopMode=(ImageButton)view.findViewById(R.id.ib_loop_mode);
        mIbShare = (ImageButton) view.findViewById(R.id.ib_share);
        mIvMusicCd = (ImageView)view.findViewById(R.id.iv_music_cd);
        mIbVoice = (ImageButton)view.findViewById(R.id.ib_voice);
        mTvDurationTime = (TextView) view.findViewById(R.id.tv_current_time);
        mCsbProgress = (CircularSeekBar) view.findViewById(R.id.csb_progress);
        mIbBack.setOnClickListener(this);
        mIbVoice.setOnClickListener(this);
        mIbPre.setOnClickListener(this);
        mIbFore.setOnClickListener(this);
        mIbPlay.setOnClickListener(this);
        mIbNext.setOnClickListener(this);
        mIbLoopMode.setOnClickListener(this);
        mIbShare.setOnClickListener(this);
        mCsbProgress.setOnSeekBarChangeListener(this);
        mContext = getContext();
        // 获取系统音乐音量
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取系统音乐当前音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //初始化光盘旋转动画
        rotateAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        rotateAnim.setInterpolator(new LinearInterpolator());//重复播放不停顿
        rotateAnim.setFillAfter(true);//停在最后

        cubeLeftOutAnimation = new CubeLeftOutAnimation();
        cubeLeftOutAnimation.setDuration(500);
        cubeLeftOutAnimation.setFillAfter(true);

        cubeRightInAnimation = new CubeRightInAnimation();
        cubeRightInAnimation.setDuration(500);
        cubeRightInAnimation.setFillAfter(true);

        //////////////////////////////////////////////////////////
        cubeLeftOutBackAnimation = new CubeLeftOutBackAnimation();
        cubeLeftOutBackAnimation.setDuration(500);
        cubeLeftOutBackAnimation.setFillAfter(true);

        cubeRightInBackAnimation = new CubeRightInBackAnimation();
        cubeRightInBackAnimation.setDuration(500);
        cubeRightInBackAnimation.setFillAfter(true);
        openAnim();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerMusicReceiver();
    }

    private void initData (Bundle bundle) {
//        Intent intent = this.getIntent();//接收来自列表的数据
//        Bundle bundle = intent.getExtras();
        musicDatas = (List<MusicData>) bundle.getSerializable(Contsant.MUSIC_LIST_KEY);
        position = bundle.getInt(Contsant.POSITION_KEY);
        if (musicDatas != null) {
            randomIDs = new int[musicDatas.size()];
        }
    }

    private void openAnim () {
        mIbFore.setImageResource(R.drawable.icon_pause);
        mIbFore.startAnimation(cubeLeftOutAnimation);
        mIbPlay.startAnimation(cubeRightInAnimation);
    }

    private void closeAnim () {
        mIbFore.setImageResource(R.drawable.icon_play);
        mIbFore.startAnimation(cubeLeftOutBackAnimation);
        mIbPlay.startAnimation(cubeRightInBackAnimation);
    }

    /**
     * 在播放布局里先把播放按钮先删除并用background设置为透明。然后在代码添加按钮
     */
    public void play() {
        LogTool.i("play---startService");
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        mIvMusicCd.startAnimation(rotateAnim);
        flag =  Contsant.PlayStatus.PLAY;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op",  Contsant.PlayStatus.PLAY);// 向服务传递数据
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }

    /**
     * 暂停
     */
    public void pause() {
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        mIvMusicCd.clearAnimation();
        flag =  Contsant.PlayStatus.PAUSE;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", Contsant.PlayStatus.PAUSE);
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }
    /**
     * 上一首
     */
    private void lastOne() {
        Log.i(TAG, "postion" + position);
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        stop();
//		if (musicDatas.size() == 1 || loop_flag == LOOP_ONE) {
        if (musicDatas.size() == 1) {//单曲循环允许手动切换到上一曲和下一曲
            position = position;
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            intent.putExtras(bundle);
            intent.setAction("com.app.media.MUSIC_SERVICE");
            intent.putExtra("length", 1);
            intent.setPackage(getActivity().getPackageName());
            getActivity().startService(intent);
            play();
            return;
        }
        if (random_flag == true) {
            if (randomNum < musicDatas.size() - 1) {
                randomIDs[randomNum] = position;
                position = findRandomSound(musicDatas.size());
                randomNum++;

            } else {
                randomNum = 0;
                for (int i = 0; i < musicDatas.size(); i++) {
                    randomIDs[i] = -1;
                }
                randomIDs[randomNum] = position;
                position = findRandomSound(musicDatas.size());
                randomNum++;
            }
        } else {
            if (position == 0) {
                position = musicDatas.size() - 1;
            } else if (position > 0) {
                position--;
            }
        }
        Log.i(TAG, "postion" + position);
        Bundle bundle = new Bundle();
        bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
        bundle.putInt(Contsant.POSITION_KEY, position);
        DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变
        setup();
        play();

    }

    /**
     * 进度条改变事件
     */
    private void seekbar_change(long progress) {
        Intent intent = new Intent();
        intent.setAction("com.app.media.MUSIC_SERVICE");
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        bundle.putLong(Contsant.SEEK_POSITION, mSeekPosition);
        intent.putExtras(bundle);
        intent.putExtra("op",  Contsant.PlayStatus.PROGRESS_CHANGE);
        intent.putExtra("progress", progress);
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }

    /**
     * 下一首
     */
    public  void nextOne() {
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        switch (loop_flag) {
            case Contsant.LoopMode.LOOP_ORDER://顺序播放
                //顺序播放运行手动切换下一曲，如果后台播放的最后一曲则停止播放
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_ONE://单曲循环
                //单曲循环播放模式如果手动点击下一曲则同全部循环处理，后台播放完毕当前歌曲自动播放当前歌曲
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_ALL://全部循环
                position++;
                if (position == musicDatas.size()) {
                    position = 0;
                }
                break;
            case Contsant.LoopMode.LOOP_RANDOM://随机播放
                if (randomNum < musicDatas.size() - 1) {
                    randomIDs[randomNum] = position;
                    position = findRandomSound(musicDatas.size());
                    randomNum++;

                } else {
                    randomNum = 0;
                    for (int i = 0; i < musicDatas.size(); i++) {
                        randomIDs[i] = -1;
                    }
                    randomIDs[randomNum] = position;
                    position = findRandomSound(musicDatas.size());
                    randomNum++;
                }
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.POSITION_CHANGED);
        bundle.putInt(Contsant.POSITION_KEY, position);
        DataObservable.getInstance().setData(bundle);//通知播放列表播放位置改变

        setup();
        play();

    }
    /**找到随机位置**/
    public static int findRandomSound(int end) {
        int ret = -1;
        ret = (int) (Math.random() * end);
        while (havePlayed(ret, end)) {
            ret = (int) (Math.random() * end);
        }
        return ret;
    }
    /**是否在播放**/
    public static boolean havePlayed(int position, int num) {
        boolean ret = false;

        for (int i = 0; i < num; i++) {
            if (position == randomIDs[i]) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    /**
     * 停止播放音乐
     */
    private void stop() {
        Intent isplay = new Intent("notifi.update");
        getActivity().sendBroadcast(isplay);// 发起后台支持
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op",  Contsant.PlayStatus.STOP);
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(getActivity());
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }


    @Override
    public void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
        getActivity().unregisterReceiver(musicReceiver);
    }

    /**
     * 初始化
     */
    private void setup() {
        loadclip();
//        registerMusicReceiver();
    }
    /**
     * 在顶部显示歌手，歌名。这两个是通过服务那边接收过来的
     */
    private void loadclip() {
        mCsbProgress.setProgress(0);
        /**设置歌曲名**/
        if (musicDatas.get(position).title.length() > 15)
            mMTvMusicName.setText(musicDatas.get(position).title.substring(0, 12) + "...");// 设置歌曲名
        else
            mMTvMusicName.setText(musicDatas.get(position).title);
        mMusicName = String.valueOf(mMTvMusicName.getText());
        /**设置艺术家名**/
        String artist = musicDatas.get(position).artist;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);
        intent.setAction("com.app.media.MUSIC_SERVICE");// 给将这个action发送服务
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);
    }

    public void showAudioInfo (){
//		String format = musicDatas.get(position).getPath().substring(musicDatas.get(position).getPath().length() - 3).toUpperCase();
        String artist = musicDatas.get(position).artist + "\n";
//		String audioInfo = musicDatas.get(position).getSampleRate() + "/"
//				+ musicDatas.get(position).getBit() + "/"
//				+ musicDatas.get(position).getBitRate();
//		mTvArtisting.setText(format +"/"+ audioInfo);
        mTvArtisting.setText(artist);
    }
    /**
     * 初始化注册广播
     */
    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contsant.PlayAction.MUSIC_CURRENT);
        filter.addAction(Contsant.PlayAction.MUSIC_DURATION);
        filter.addAction(Contsant.PlayAction.MUSIC_NEXT);
        filter.addAction(Contsant.PlayAction.MUSIC_UPDATE);
        filter.addAction(Contsant.PlayAction.MUSIC_STOP);//add by victor
        filter.addAction(Contsant.PlayAction.MUSIC_PAUSE);
        filter.addAction("notifi.update");
        getActivity().registerReceiver(musicReceiver, filter);
    }
    /**在后台MusicService里使用handler消息机制，不停的向前台发送广播，广播里面的数据是当前mp播放的时间点，
     * 前台接收到广播后获得播放时间点来更新进度条,暂且先这样。但是一些人说虽然这样能实现。但是还是觉得开个子线程不错**/
    protected BroadcastReceiver musicReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Contsant.PlayAction.MUSIC_CURRENT)) {
                duration = intent.getExtras().getLong("duration");// 获得当前播放位置
                currentTime = intent.getExtras().getLong("currentTime");// 获得当前播放位置
                mHandler.sendEmptyMessage(Contsant.Action.CURRENT_TIME_MUSIC);
            } else if (action.equals(Contsant.PlayAction.MUSIC_DURATION)) {
                duration = intent.getExtras().getLong("duration");
                musicDatas.get(position).setSampleRate(intent.getStringExtra(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI));
                musicDatas.get(position).setBitRate(intent.getStringExtra(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
                mHandler.sendEmptyMessage(Contsant.Action.DURATION_MUSIC);
            } else if (action.equals(Contsant.PlayAction.MUSIC_NEXT)) {
                mHandler.sendEmptyMessage(Contsant.Action.NEXTONE_MUSIC);
            } else if (action.equals(Contsant.PlayAction.MUSIC_UPDATE)) {
                position = intent.getExtras().getInt("position");
                mHandler.sendEmptyMessage(Contsant.Action.UPDATE_MUSIC);
            }else if(action.equals(Contsant.PlayAction.MUSIC_PAUSE)){
                int isPlaying = intent.getExtras().getInt("isPlaying");
                Message msg = mHandler.obtainMessage(Contsant.Action.PLAY_PAUSE_MUSIC,isPlaying, 0);
                msg.sendToTarget();
            } else if (action.equals(Contsant.PlayAction.MUSIC_STOP)) {//add by victor
                mHandler.sendEmptyMessage(Contsant.Action.MUSIC_STOP);
            }
        }
    };



    /**
     * 播放时间转换
     */
    public String toTime(int time) {

        time /= 1000;
        int minute = time / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ib_back:
                DataObservable.getInstance().setData(Contsant.Action.GOTO_MUSIC_LIST_FRAG);
                break;
            case R.id.ib_voice:
                if (isSilence) {
                    currentVolume = ((MusicActivity)getActivity()).getCurrentVolume();
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,currentVolume, 0);
                    mIbVoice.setImageResource(R.drawable.btn_voice_normal);
                    isSilence = false;
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0, 0);
                    mIbVoice.setImageResource(R.drawable.icon_silence);
                    isSilence = true;
                }
                break;
            case R.id.ib_pre:
                lastOne();
                break;
            case R.id.ib_play:
                if (flag ==  Contsant.PlayStatus.PLAY) {
//					closeAnim();
//					mIvMusicCd.clearAnimation();
                    pause();
                } else if (flag ==  Contsant.PlayStatus.PAUSE) {
//					openAnim();
//					mIvMusicCd.startAnimation(rotateAnim);
                    play();
                }
                break;
            case R.id.ib_next:
                nextOne();
                break;
            case R.id.ib_loop_mode:
                loop_flag++;
                if (loop_flag > 4) {
                    loop_flag = 1;
                }
                setPlayModeBg();
                break;
            case R.id.ib_share:
                Intent intentshare = new Intent(Intent.ACTION_SEND);
                intentshare
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.play_share))
                        .putExtra(Intent.EXTRA_TEXT,
                                getResources().getString(R.string.play_share_content) + mMusicName);
                Intent.createChooser(intentshare, getResources().getString(R.string.play_share));
                startActivity(intentshare);
                break;
        }
    }


    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, long progress, boolean fromUser) {
        if (fromUser) {
            mPositionSeek = progress;
//			seekbar_change(progress);
        }else if (circularSeekBar.getId() == R.id.sb_player_voice) {
            // 设置音量
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)progress, 0);
        }
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {
        seekbar_change(mPositionSeek);
        play();
    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {
        mPositionSeek = 0;
        pause();
    }

    /**
     * 设置播放模式按钮背景
     * add by victor
     */
    private void setPlayModeBg() {
        if (musicDatas == null || musicDatas.size() == 0) {
            return;
        }
        switch (loop_flag) {
            case Contsant.LoopMode.LOOP_ORDER:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_sequence);
                toast = Contsant.showMessage(toast, getActivity(), getResources().getString(R.string.loop_none_tip));
                break;
            case Contsant.LoopMode.LOOP_ONE:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_circleone);
                toast = Contsant.showMessage(toast, getActivity(), getResources().getString(R.string.loop_one_tip));
                break;
            case Contsant.LoopMode.LOOP_ALL:
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_circlelist);
                toast = Contsant.showMessage(toast, getActivity(), getResources().getString(R.string.loop_all_tip));
                break;
            case Contsant.LoopMode.LOOP_RANDOM:
                for (int i = 0; i < musicDatas.size(); i++) {
                    randomIDs[i] = -1;
                }
                mIbLoopMode.setImageResource(R.drawable.player_btn_player_mode_random);
                toast = Contsant.showMessage(toast,getActivity(), getResources().getString(R.string.loop_random_tip));
                break;
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof Bundle) {
            Bundle bundle = (Bundle) data;
            int action = bundle.getInt(Contsant.ACTION_KEY);
            if (action == Contsant.Action.PLAY_MUSIC) {
                initData(bundle);
                mHandler.sendEmptyMessage(Contsant.Msg.PLAY_MUSIC);
            }
        }
    }

}
