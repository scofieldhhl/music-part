package org.app.enjoy.music.adapter;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.app.enjoy.music.data.MusicData;
import org.app.enjoy.music.mode.DataObservable;
import org.app.enjoy.music.tool.Contsant;
import org.app.enjoy.music.util.AlbumImgUtil;
import org.app.enjoy.music.view.CircleImageView;
import org.app.enjoy.music.view.MovingTextView;
import org.app.enjoy.musicplayer.MusicListActivity;
import org.app.enjoy.musicplayer.R;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MusicListAdapter extends BaseAdapter {
	private String TAG = "MusicListAdapter";
	private Context mcontext;// 上下文
	private List<MusicData> musicDatas;
	private int currentPosition = -1;
	private int currentLongPosition = -1;
	private int currentMusicId  = -1;//当前播放音乐id
	private boolean cancelLong;
//	private Typeface typeFace;
	public MusicListAdapter(Context context) {
		mcontext = context;
		//经典细圆字体
//		typeFace = Typeface.createFromAsset(mcontext.getAssets(), "fonts/DroidSansFallback.ttf");
	}

	public void setDatas (List<MusicData> musicDatas) {
		this.musicDatas = musicDatas;
	}

	public void setCurrentPosition (int position) {
		currentPosition = position;
		currentMusicId = musicDatas.get(currentPosition).id;
		notifyDataSetChanged();
	}
	public void setCurrentLongPosition (int position) {
		currentLongPosition = position;
		notifyDataSetChanged();
	}
	public void cancelLongClick (boolean cancel) {
		cancelLong = cancel;
	}

	@Override
	public int getCount() {
		return musicDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return musicDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewholder = null;
		if (convertView == null) {
			viewholder = new ViewHolder();
			convertView = LayoutInflater.from(mcontext).inflate(R.layout.lv_music_item, null);
			viewholder.mCivAlbum = (CircleImageView) convertView.findViewById(R.id.civ_album);
			viewholder.mMtvTitle = (MovingTextView) convertView.findViewById(R.id.mtv_title);
			viewholder.singers = (TextView) convertView.findViewById(R.id.singer);
			viewholder.times = (TextView) convertView.findViewById(R.id.time);
			viewholder.mIconRemove = (ImageView) convertView.findViewById(R.id.iv_remove);
			viewholder.song_list_item_menu = (ImageButton) convertView.findViewById(R.id.ibtn_song_list_item_menu);
			if (position % 2 == 0) {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_focus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_blue));
			} else {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_unfocus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.dark_blue));
			}
			convertView.setTag(viewholder);
		} else {
			viewholder = (ViewHolder) convertView.getTag();
		}
//		viewholder.mTvPosition.setTypeface(typeFace);

		MusicData data = musicDatas.get(position);
		viewholder.mMtvTitle.setText(data.title);

		String albumId = data.getAlbumId();
		Bitmap bitmap;
		if (!TextUtils.isEmpty(albumId)) {
			bitmap = AlbumImgUtil.getArtwork(mcontext,data.getId(),Long.parseLong(albumId),false);
			if (bitmap != null) {
				viewholder.mCivAlbum.setImageBitmap(bitmap);
			} else {
				viewholder.mCivAlbum.setImageResource(R.drawable.default_album);
			}
		} else {
			viewholder.mCivAlbum.setImageResource(R.drawable.default_album);
		}

		viewholder.singers.setText(data.artist);
		if(data.duration > 0){
			viewholder.times.setText(toTime(data.duration));
		}else{
			viewholder.times.setText("");
		}

		if (cancelLong) {
			viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.white));
//			viewholder.mIvLeft.setVisibility(View.VISIBLE);
			viewholder.mIconRemove.setVisibility(View.GONE);
			if (position % 2 == 0) {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_focus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_blue));
			} else {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_unfocus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.dark_blue));
			}
		} else {
			if (currentLongPosition == position) {
				convertView.setBackgroundResource(R.drawable.bg_item_long_click);
				viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.red));
				viewholder.mIconRemove.setVisibility(View.VISIBLE);
			} else {
				if (position % 2 == 0) {
//					viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_focus);
					convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_blue));
				} else {
//					viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_unfocus);
					convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.dark_blue));
				}
				viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.times.setTextColor(mcontext.getResources().getColor(R.color.white));
				viewholder.times.setVisibility(View.VISIBLE);
				viewholder.mIconRemove.setVisibility(View.GONE);
			}
		}
		if (currentPosition == position) {
			viewholder.mMtvTitle.setTextColor(mcontext.getResources().getColor(R.color.white));
			viewholder.singers.setTextColor(mcontext.getResources().getColor(R.color.white));
//			viewholder.mIvLeft.setVisibility(View.VISIBLE);
			viewholder.times.setVisibility(View.VISIBLE);
			viewholder.mIconRemove.setVisibility(View.GONE);

			convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_yellow));
		} else {
//			viewholder.mIvLeft.setVisibility(View.GONE);
			if (position % 2 == 0) {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_focus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.light_blue));
			} else {
//				viewholder.mLayoutContent.setBackgroundResource(R.drawable.bg_lv_music_item_unfocus);
				convertView.setBackgroundColor(mcontext.getResources().getColor(R.color.dark_blue));
			}
		}

		viewholder.mIconRemove.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Bundle bundle = new Bundle();
				bundle.putInt(Contsant.ACTION_KEY,Contsant.Action.REMOVE_MUSIC);
				bundle.putInt(Contsant.POSITION_KEY, currentLongPosition);
				DataObservable.getInstance().setData(bundle);
				musicDatas.remove(currentLongPosition);

				currentLongPosition = -1;
				//删除之后重新找到当前播放音乐的position
				for (int i=0;i<musicDatas.size();i++) {
					if (currentMusicId == musicDatas.get(i).id) {
						currentPosition = i;
						break;
					}
				}
				notifyDataSetChanged();
			}
		});
		return convertView;

	}
	public class ViewHolder {
		public CircleImageView mCivAlbum;
		public MovingTextView mMtvTitle;
		public TextView singers;
		public TextView times;
		public ImageButton song_list_item_menu;
		public ImageView mIconRemove;
	}

	/**
	 * 时间的转换
	 */
	public String toTime(long time) {

		time /= 1000;
		long minute = time / 60;
		long second = time % 60;
		minute %= 60;
		/** 返回结果用string的format方法把时间转换成字符类型 **/
		return String.format("%02d:%02d", minute, second);
	}

}
