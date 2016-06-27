package org.app.enjoy.music.frag;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.AlbumAdapter;
import org.app.enjoy.music.data.AlbumData;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.service.MusicService;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.util.SharePreferencesUtil;
import org.app.enjoy.musicplayer.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by victor on 2016/6/12.
 */
public class AlbumFragment extends Fragment implements AdapterView.OnItemClickListener{
    private String TAG = "AlbumFragment";
    private View view;
    private ListView mLvAlbum;

    private AlbumAdapter albumAdapter;
    private List<AlbumData> albumList = new ArrayList<>();

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.UPDATE_ALBUM_LIST:
                    if (albumAdapter != null) {
                        albumAdapter.setDatas(albumList);
                        albumAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_album,container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initialize(view);
        initData();

        // 设置皮肤背景
        /*Setting setting = new Setting(getActivity(), false);
        mLvAlbum.setBackgroundResource(setting.getCurrentSkinResId());//这里我只设置listview的皮肤而已。
        MobclickAgent.onResume(getActivity());
        Intent intentServer = new Intent(getActivity(), MusicService.class);
        getActivity().startService(intentServer);*/
    }

    private void initialize (View view) {
        mLvAlbum = (ListView) view.findViewById(R.id.lv_album);
        mLvAlbum.setOnItemClickListener(this);
        albumAdapter = new AlbumAdapter(getActivity());
        albumAdapter.setDatas(albumList);
        mLvAlbum.setAdapter(albumAdapter);
    }

    private void initData () {
        if (albumList != null) {
            albumList.clear();
        }
        new Thread(){
            @Override
            public void run() {
                HashMap<String,String> map = MusicUtil.getAllAlbum(getContext());
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();

                    AlbumData info = new AlbumData();
                    info.setAlbumId(key);
                    info.setAlbum(value);
                    albumList.add(info);
                }
                mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_ALBUM_LIST);
            }
        }.start();
    }

    /**
     * @param musicDatas
     * 从当前专辑第一首歌曲开始播放
     */
    public void play(List<MusicData> musicDatas) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
        bundle.putInt(Contsant.POSITION_KEY, 0);
        intent.putExtras(bundle);
        intent.setAction("com.app.media.MUSIC_SERVICE");
        intent.putExtra("op", 1);// 向服务传递数据
        intent.setPackage(getActivity().getPackageName());
        getActivity().startService(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.ALBUM_FRAG);
        List<MusicData> musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(position).getAlbumId());
        if (musicDatas != null && musicDatas.size() > 0) {
            if (albumAdapter != null) {
                albumAdapter.setCurrentPosition(position);
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
            bundle.putInt(Contsant.POSITION_KEY, 0);

            Intent intent = new Intent();
            intent.setAction(Contsant.PlayAction.MUSIC_LIST);
            intent.putExtras(bundle);
            getActivity().sendBroadcast(intent);
            play(musicDatas);
        }
    }
}
