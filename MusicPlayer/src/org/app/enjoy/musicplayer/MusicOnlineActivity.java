package org.app.enjoy.musicplayer;


import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.tool.Setting;
import org.app.enjoy.netmusic.NetMusicAdapter;
import org.app.enjoy.netmusic.XmlParse;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

public class MusicOnlineActivity extends BaseActivity {
	private ListView listview;
	private Toast toast;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.net_music);
		listview = (ListView) findViewById(R.id.net_list);

		if (!Contsant.getNetIsAvailable(MusicOnlineActivity.this)) {
			toast=Contsant.showMessage(toast, MusicOnlineActivity.this, getResources().getString(R.string.network_error));
		}
		listview.setAdapter(new NetMusicAdapter(this, XmlParse.parseWebSongList(this)));


	}
	@Override
	public void onResume() {
		super.onResume();
		// 设置皮肤背景
		Setting setting = new Setting(this, false);
		listview.setBackgroundResource(setting.getCurrentSkinResId());//这里只设置listview的皮肤而已。
		MobclickAgent.onResume(this);
	}
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
}
