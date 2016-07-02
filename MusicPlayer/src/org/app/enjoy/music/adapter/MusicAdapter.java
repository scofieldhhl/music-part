package org.app.enjoy.music.adapter;import android.content.Context;import android.graphics.Bitmap;import android.os.Bundle;import android.support.v4.graphics.drawable.RoundedBitmapDrawable;import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;import android.text.TextUtils;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.BaseAdapter;import android.widget.ImageButton;import android.widget.ImageView;import android.widget.TextView;import com.bumptech.glide.Glide;import com.bumptech.glide.request.target.BitmapImageViewTarget;import org.app.enjoy.music.data.MusicData;import org.app.enjoy.music.mode.DataObservable;import org.app.enjoy.music.tool.Contsant;import org.app.enjoy.music.util.AlbumImgUtil;import org.app.enjoy.music.view.CircleImageView;import org.app.enjoy.music.view.MovingTextView;import org.app.enjoy.musicplayer.R;import java.util.List;public class MusicAdapter extends BaseAdapter {	private String TAG = "MusicAdapter";	private Context mContext;// 上下文	private List<MusicData> musicDatas;	private int currentPosition = -1;	private int currentMusicId  = -1;//当前播放音乐id	public MusicAdapter(Context context) {		mContext = context;	}	public void setDatas (List<MusicData> musicDatas) {		this.musicDatas = musicDatas;	}	public void setCurrentPosition (int position) {		currentPosition = position;		currentMusicId = musicDatas.get(currentPosition).id;		notifyDataSetChanged();	}	@Override	public int getCount() {		return musicDatas.size();	}	@Override	public Object getItem(int position) {		return musicDatas.get(position);	}	@Override	public long getItemId(int position) {		return position;	}	@Override	public View getView(final int position, View convertView, ViewGroup parent) {		final ViewHolder viewholder;		if (convertView == null) {			viewholder = new ViewHolder();			convertView = LayoutInflater.from(mContext).inflate(R.layout.lv_diy_frag_music_item, null);			viewholder.mCivAlbum = (CircleImageView) convertView.findViewById(R.id.civ_album);			viewholder.mMtvTitle = (MovingTextView) convertView.findViewById(R.id.mtv_title);			viewholder.mTvArtist = (TextView) convertView.findViewById(R.id.tv_artist);			if (position % 2 == 0) {				convertView.setBackgroundColor(mContext.getResources().getColor(R.color.light_blue));			} else {				convertView.setBackgroundColor(mContext.getResources().getColor(R.color.dark_blue));			}			convertView.setTag(viewholder);		} else {			viewholder = (ViewHolder) convertView.getTag();		}		MusicData data = musicDatas.get(position);		viewholder.mMtvTitle.setText(data.title);		String albumId = data.getAlbumId();		Glide.with(mContext)//加载显示圆形图片				.load(AlbumImgUtil.getAlbumartPath(data.getId(), Long.parseLong(albumId), mContext))				.asBitmap()				.placeholder(R.drawable.default_album)				.centerCrop()				.into(new BitmapImageViewTarget(viewholder.mCivAlbum) {					@Override					protected void setResource(Bitmap resource) {						RoundedBitmapDrawable circularBitmapDrawable =								RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);						circularBitmapDrawable.setCircular(true);						viewholder.mCivAlbum.setImageDrawable(circularBitmapDrawable);					}				});		viewholder.mTvArtist.setText(data.artist);		if (currentPosition == position) {			convertView.setBackgroundColor(mContext.getResources().getColor(R.color.light_yellow));		} else {			if (position % 2 == 0) {				convertView.setBackgroundColor(mContext.getResources().getColor(R.color.light_blue));			} else {				convertView.setBackgroundColor(mContext.getResources().getColor(R.color.dark_blue));			}		}		return convertView;	}	public class ViewHolder {		public CircleImageView mCivAlbum;		public MovingTextView mMtvTitle;		public TextView mTvArtist;	}}