package org.app.enjoy.music.frag;import android.content.Intent;import android.os.Bundle;import android.os.Handler;import android.os.Message;import android.support.v4.app.Fragment;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.AdapterView;import android.widget.ListView;import com.umeng.analytics.MobclickAgent;import org.app.enjoy.music.adapter.ArtistAdapter;import org.app.enjoy.music.data.ArtistData;import org.app.enjoy.music.data.MusicData;import org.app.enjoy.music.mode.DataObservable;import org.app.enjoy.music.service.MusicService;import org.app.enjoy.music.tool.Contsant;import org.app.enjoy.music.tool.Setting;import org.app.enjoy.music.util.MusicUtil;import org.app.enjoy.music.util.SharePreferencesUtil;import org.app.enjoy.musicplayer.R;import java.io.Serializable;import java.util.ArrayList;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;/** * Created by victor on 2016/6/12. */public class ArtistFragment extends Fragment implements AdapterView.OnItemClickListener{    private String TAG = "ArtistFragment";    private View view;    private ListView mLvArtist;    private ArtistAdapter artistAdapter;    private List<ArtistData> artistList = new ArrayList<>();    Handler mHandler = new Handler(){        @Override        public void handleMessage(Message msg) {            switch (msg.what) {                case Contsant.Msg.UPDATE_ARTIST_LIST:                    if (artistAdapter != null) {                        artistAdapter.setDatas(artistList);                        artistAdapter.notifyDataSetChanged();                    }                    break;            }        }    };    @Override    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        view = inflater.inflate(R.layout.frag_artist,container, false);        return view;    }    @Override    public void onResume() {        super.onResume();        initialize(view);        initData();        MobclickAgent.onResume(getActivity());    }    public void onPause() {        super.onPause();        MobclickAgent.onPause(getActivity());    }    private void initialize (View view) {        mLvArtist = (ListView) view.findViewById(R.id.lv_artist);        mLvArtist.setOnItemClickListener(this);        artistAdapter = new ArtistAdapter(getActivity());        artistAdapter.setDatas(artistList);        mLvArtist.setAdapter(artistAdapter);    }    private void initData () {        if (artistList!= null) {            artistList.clear();        }        new Thread(){            @Override            public void run() {                HashMap<String,MusicData> map = MusicUtil.getAllArtist(getContext());                Iterator it = map.entrySet().iterator();                while (it.hasNext()) {                    Map.Entry entry = (Map.Entry) it.next();                    String key = (String) entry.getKey();                    MusicData value = (MusicData) entry.getValue();                    ArtistData info = new ArtistData();                    info.artistId = key;                    info.artist = value.artist;                    info.music = value;                    artistList.add(info);                }                mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_ARTIST_LIST);            }        }.start();    }    public void play(List<MusicData> musicDatas) {        Intent intent = new Intent();        Bundle bundle = new Bundle();        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);        bundle.putInt(Contsant.POSITION_KEY, 0);        intent.putExtras(bundle);        intent.setAction("com.app.media.MUSIC_SERVICE");        intent.putExtra("op", 1);// 向服务传递数据        intent.setPackage(getActivity().getPackageName());        getActivity().startService(intent);    }    @Override    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {        SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.ARTIST_FRAG);        List<MusicData> musicDatas = MusicUtil.getSongsByArtist(getContext(), artistList.get(position).artistId);        if (musicDatas != null && musicDatas.size() > 0) {            if (artistAdapter != null) {                artistAdapter.setCurrentPosition(position);            }            play(musicDatas);//            Bundle bundle = new Bundle();//            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);//            bundle.putInt(Contsant.POSITION_KEY, 0);////            Intent intent = new Intent();//            intent.setAction(Contsant.PlayAction.MUSIC_LIST);//            intent.putExtras(bundle);//            getActivity().sendBroadcast(intent);            Bundle bundle = new Bundle();            bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);            bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);            bundle.putInt(Contsant.POSITION_KEY, 0);            DataObservable.getInstance().setData(bundle);        }    }}