package org.app.enjoy.musicplayer;

import org.app.enjoy.music.adapter.ImageAdapter;
import org.app.enjoy.music.tool.Setting;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.umeng.analytics.MobclickAgent;

public class SkinSettingActivity extends SettingActivity {
	private GridView gv_skin;
	private ImageAdapter adapter;
	private Setting mSetting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skinsetting);

		resultCode=2;
		setBackButton();
		setTopTitle(getResources().getString(R.string.skin_settings));

		mSetting=new Setting(this, true);

		adapter=new ImageAdapter(this, mSetting.getCurrentSkinId());
		gv_skin=(GridView)findViewById(R.id.gv_skin);
		gv_skin.setAdapter(adapter);
		gv_skin.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
				//更新GridView
				adapter.setCurrentId(position);
				//更新背景图片
				SkinSettingActivity.this.getWindow().setBackgroundDrawableResource(Setting.SKIN_RESOURCES[position]);
				//保存数据
				mSetting.setCurrentSkinResId(position);

			}
		});
	}

	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

}
