package org.app.enjoy.musicplayer;

import android.os.Bundle;

import com.umeng.analytics.MobclickAgent;

public class AboutActivity extends SettingActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		setTopTitle(getResources().getString(R.string.about_title));
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
