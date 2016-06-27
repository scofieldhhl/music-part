package org.app.enjoy.music.frag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.MusicListAdapter;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.tool.Menu;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.tool.XfDialog;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.musicplayer.MusicActivity;
import org.app.enjoy.musicplayer.PlayMusicActivity;
import org.app.enjoy.musicplayer.R;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Administrator on 2016/6/2.
 */
public class MusicListFragment extends Fragment implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener,Observer {

    private String TAG = "MusicListFragment";
    private View view;
    private ImageButton mIbRight;
    /*** 音乐列表 **/
    private ListView mLvSongs;
    private Menu xmenu;//自定义菜单
    private LayoutInflater inflater;//装载布局
    private ViewGroup.LayoutParams params;
    private Toast toast;//提示
    /**铃声标识常量**/
    public static final int Ringtone = 0;
    public static final int Alarm = 1;
    public static final int Notification = 2;
    private int currentPosition = -1;
    private int lastLongClickPosition = -1;
    private MusicListAdapter musicListAdapter;
    private List<MusicData> musicDatas = new ArrayList<MusicData>();
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
                    if (musicDatas != null && musicDatas.size() > 0) {
                        if (musicListAdapter != null) {
                            if (currentPosition != -1) {
                                musicListAdapter.setDatas(musicDatas);
                                musicListAdapter.setCurrentPosition(currentPosition);
                            }

                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
                            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.UPDATE_MUSIC);
                            bundle.putInt(Contsant.POSITION_KEY, -1);
                            DataObservable.getInstance().setData(bundle);
                        }
                    } else {
                        final XfDialog xfdialog = new XfDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.tip)).
                                setMessage(getResources().getString(R.string.dlg_not_found_music_tip)).
                                setPositiveButton(getResources().getString(R.string.confrim), null).create();
                        xfdialog.show();
                    }

                    break;
//                case Contsant.Msg.UPDATE_PLAY_LIST_EXTENSION:
//                    if (musicListAdapter != null){
//                        musicListAdapter.setDatas(musicDatas);
//                        musicListAdapter.notifyDataSetChanged();
//                    }
//                    break;
                case Contsant.Msg.PLAY_CUE:
//					if(mCueSongBeanList != null && mCueSongBeanList.size() > 0){
////						initPopWindow(mCueSongBeanList);
//					}
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.music_list,container, false);
        return view;
    }

    private void initialize (View view) {
        DataObservable.getInstance().addObserver(this);
        mIbRight = (ImageButton) view.findViewById(R.id.ib_right);
        mLvSongs = (ListView) view.findViewById(R.id.local_music_list);
        mLvSongs.setOnItemClickListener(this);
        mLvSongs.setOnItemLongClickListener(this);
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        musicListAdapter = new MusicListAdapter(getContext());
        musicListAdapter.setDatas(musicDatas);
        mLvSongs.setAdapter(musicListAdapter);

    }

    /**
     * 显示MP3信息,其中_ids保存了所有音乐文件的_ID，用来确定到底要播放哪一首歌曲，_titles存放音乐名，用来显示在播放界面，
     * 而_path存放音乐文件的路径（删除文件时会用到）。
     */
    private void initData () {
        List<MusicData> musicList = MusicUtil.getAllSongs(getContext());
        musicDatas.addAll(musicList);
        mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (musicDatas != null) {
            musicDatas.clear();
        }

        initialize(view);
        //异步检索其他音频文件
        new Thread(){
            @Override
            public void run() {
                initData();
                GetFiles(getSDPath(),arrExtension, true);
            }
        }.start();

        /*// 设置皮肤背景
        Setting setting = new Setting(getActivity(), false);
        mLvSongs.setBackgroundResource(setting.getCurrentSkinResId());//这里我只设置listview的皮肤而已。
        MobclickAgent.onResume(getActivity());
        Intent intentServer = new Intent(getActivity(), MusicService.class);
        getActivity().startService(intentServer);*/
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    /**
     * 根据Position播放音乐
     */
    public void playMusic(int position, long seekPosition) {
        if (musicDatas.size() > 0) {
            play(position);

            Intent intent = new Intent(getActivity(),PlayMusicActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, position);
            bundle.putLong(Contsant.SEEK_POSITION, seekPosition);
            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.PLAY_MUSIC);
            intent.putExtras(bundle);
            DataObservable.getInstance().setData(bundle);
            DataObservable.getInstance().setData(Contsant.Action.GOTO_MUSIC_PLAY_FRAG);//进入播放fragmengt
        } else {
            final XfDialog xfdialog = new XfDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.tip)).
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
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.MUSIC_LIST_FRAG);
        playMusic(position, 0);
        currentPosition = position;
        currentMusicId = musicDatas.get(position).id;
        if (musicListAdapter != null) {
            musicListAdapter.cancelLongClick(true);
        }
        if (musicListAdapter != null && currentPosition != -1) {
            musicListAdapter.setCurrentPosition(currentPosition);
            sendBroadcast(currentPosition);
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
                    if (((MusicActivity) getActivity()).getCurrentPage() == 0) {
                        if (currentPosition != position) {
                            currentPosition = position;
                            mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        DataObservable.getInstance().deleteObserver(this);
        super.onDestroy();
    }

    /**
     * 遍历文件夹，搜索指定扩展名的文件
     * */
    private String[] arrExtension = new String[]{"dsf","dff","dst","dsd", "wma", "aif", "aac"};
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
                                mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);
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


    private void sendBroadcast(int position){
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, position);

        Intent intent = new Intent();
        intent.setAction(Contsant.PlayAction.MUSIC_LIST);
        intent.putExtras(bundle);
        getActivity().sendBroadcast(intent);
    }

}
