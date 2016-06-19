package org.app.enjoy.musicplayer;

import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.umeng.analytics.MobclickAgent;

public class FavroiteActivity extends BaseActivity {
	WebView webview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite);
		LoadData();
	}

	private void LoadData() {
		webview = (WebView) findViewById(R.id.music_webview);
		webview.loadUrl("http://shop116644118.taobao.com/");
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url){
				view.loadUrl(url);
				return false;

			}
		});

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (webview.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {
			webview.goBack(); // goBack()表示返回webView的上一页面

			return true;
		}
		return false;
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
