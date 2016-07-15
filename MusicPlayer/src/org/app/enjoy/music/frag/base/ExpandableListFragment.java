package org.app.enjoy.music.frag.base;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.umeng.analytics.MobclickAgent;

import org.app.enjoy.music.adapter.BaseAddressExpandableListAdapter;
import org.app.enjoy.music.data.AlbumData;
import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.LogTool;
import org.app.enjoy.music.util.MusicUtil;
import org.app.enjoy.music.view.LoadingDialog;
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
public abstract class ExpandableListFragment extends Fragment{
    private View view;
    public int mMa_data = Contsant.Frag.ALBUM_FRAG;//当前播放列表

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Contsant.Msg.UPDATE_ALBUM_LIST:
                    childList = new ArrayList[albumList.size()];
                    baseELAdapter = new BaseAddressExpandableListAdapter(getActivity(), albumList, childList);
                    expandableListView.setAdapter(baseELAdapter);
                    baseELAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    /*
         * To be overrode in child classes to setup fragment data
         */
    public abstract void setupFragmentData();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_album,container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupFragmentData();
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(getActivity());
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(getActivity());
    }

    private void initialize (View view) {
        expandableListView = (ExpandableListView)view.findViewById(R.id.ag_addressbook_ELV);
        GroupClickListener();
        expandableListView.setDividerHeight(0);
        expandableListView.setGroupIndicator(null);
        albumList = new ArrayList<>();
    }

    private void initData () {
        if (albumList != null) {
            albumList.clear();
        }
        new Thread(){
            @Override
            public void run() {
                HashMap<String,MusicData> map;
                switch (mMa_data){
                    case Contsant.Frag.ALBUM_FRAG:
                        map = MusicUtil.getAllAlbum(getContext());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        map = MusicUtil.getAllArtist(getContext());
                        break;
                    default:
                        map = MusicUtil.getAllAlbum(getContext());
                        break;
                }
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String key = (String) entry.getKey();
                    MusicData value = (MusicData) entry.getValue();

                    AlbumData info = new AlbumData();
                    info.setAlbumId(key);
                    info.setMusic(value);
                    switch (mMa_data){
                        case Contsant.Frag.ALBUM_FRAG:
                            info.setAlbum(value.album);
                            break;
                        case Contsant.Frag.ARTIST_FRAG:
                            info.setAlbum(value.artist);
                    }
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
    public void play(List<MusicData> musicDatas, int position) {
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

    //------------------------------------------------------------------------------------------------------------------
    private int sign= -1;//控制列表的展开
    public void GroupClickListener(){
        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                LogTool.d("setOnGroupClickListener：" + groupPosition);
                if (childList[groupPosition] == null) {
                    AddressBookChildrenTask task = new AddressBookChildrenTask();
                    task.execute(Integer.toString(groupPosition));
                    if (progressDialog == null){
                        progressDialog = LoadingDialog.createDialog(getActivity());
                    }
                    progressDialog.show();
                }else{
                    setELVGroup(parent,groupPosition);
                }
                return true;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                LogTool.d("groupPosition" + groupPosition + "childPosition"+ childPosition);
                List<MusicData> musicDatas = childList[groupPosition];
                if (musicDatas != null && musicDatas.size() > 0 && childPosition < musicDatas.size()) {
                    play(musicDatas, childPosition);

                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);
                    bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);
                    bundle.putInt(Contsant.POSITION_KEY, childPosition);
                    DataObservable.getInstance().setData(bundle);
                }
                baseELAdapter.setmGroupPositionFocus(groupPosition);
                baseELAdapter.setmChildPositionFocus(childPosition);
                baseELAdapter.notifyDataSetChanged();
                return true;
            }
        });

    }

    public void setELVGroup(ExpandableListView parent,int groupPosition){
        if(parent.isGroupExpanded(groupPosition)){
            expandableListView.collapseGroup(groupPosition);
        }else{
            expandableListView.expandGroup(groupPosition);
            expandableListView.setSelectedGroup(groupPosition);
        }
        for (int i = 0; i < baseELAdapter.getGroupCount(); i++) {
            if (parent.isGroupExpanded(i)) {
                count_expand = i+1;
                break;
            }
            count_expand=0;
        }
        count_expand=1;
        indicatorGroupId = groupPosition;
    }


    /**
     * 子节点异步加载
     */
    public class AddressBookChildrenTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            try{
                int groupPosition = Integer.parseInt(params[0]);
                List<MusicData> musicDatas;
                switch (mMa_data){
                    case Contsant.Frag.ALBUM_FRAG:
                        musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                    case Contsant.Frag.ARTIST_FRAG:
                        musicDatas = MusicUtil.getSongsByArtist(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                    default:
                        musicDatas = MusicUtil.getSongByAlbum(getContext(), albumList.get(groupPosition).getAlbumId());
                        break;
                }
                childList[groupPosition] =  musicDatas;
                return params[0];
            }catch(Exception e){
                e.getStackTrace();
                return "-1";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            /*try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            progressDialog.dismiss();
            if(!result.equals("-1")){
                int tmpGroupPosition = Integer.parseInt(result);
                count_expand=1;
                //expandableListView.collapseGroup(sign);
                // 展开被选的group
                expandableListView.expandGroup(tmpGroupPosition);
                // 设置被选中的group置于顶端
                expandableListView.setSelectedGroup(tmpGroupPosition);
                //sign= tmpGroupPosition;
                indicatorGroupId = tmpGroupPosition;
            }
        }

    }
    private List<AlbumData> albumList;
    private List<MusicData>[] childList;
    private ExpandableListView expandableListView;
    private BaseAddressExpandableListAdapter baseELAdapter = null;
    private LoadingDialog progressDialog=null;
    private int count_expand = 0;
    private int indicatorGroupHeight;
    private int indicatorGroupId = -1;
}
