package org.app.enjoy.musicplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

public class SettingActivity extends BaseActivity {
	private String TAG = "SettingActivity";
	public int resultCode=-1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void setBackButton() {
		((ImageButton) (findViewById(R.id.bar_setting_top)).findViewById(R.id.ibtn_player_back_return)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.ibtn_player_back_return) {
					if(resultCode!=-1){
						setResult(resultCode);
					}
					finish();
				}
			}
		});
	}

	public void setTopTitle(String title) {
		((TextView) (this.findViewById(R.id.bar_setting_top)).findViewById(R.id.tv_setting_top_title)).setText(title);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			if(resultCode!=-1){
				setResult(resultCode);
//				System.out.println("返回键时候resultCode的值:" + resultCode);
				Log.e(TAG, "BACK KEY keyCode = " + resultCode);
			}
			finish();
		}
		return super.onKeyDown(keyCode, event);
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
