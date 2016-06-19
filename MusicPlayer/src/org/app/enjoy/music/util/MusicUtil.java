package org.app.enjoy.music.util;import java.util.ArrayList;import java.util.HashMap;import java.util.List;import android.content.Context;import android.database.Cursor;import android.net.Uri;import android.provider.BaseColumns;import android.provider.MediaStore;import android.provider.MediaStore.Audio;import android.provider.MediaStore.Audio.AudioColumns;import android.util.Log;import org.app.enjoy.music.data.MusicData;import org.app.enjoy.music.tool.Contsant;import org.app.enjoy.music.tool.CueUtil;public class MusicUtil {	private static String TAG = "MusicUtil";	public static final int TYPE_ALL = 0;	public static final int TYPE_ARTIST = 1;	public static final int TYPE_ALBUM = 2;	public static List<MusicData> getSongList(int type, Context context, String id){		List<MusicData> musicList = new ArrayList<MusicData>();		if (type == TYPE_ALL) {			musicList = getAllSongs(context);		} else if (type == TYPE_ARTIST) {			musicList = getSongsByArtist(context, id);		} else if (type == TYPE_ALBUM) {			musicList = getSongByAlbum(context, id);		}		return musicList;	}	public static List<MusicData> getAllSongs(Context context){		List<MusicData> musicList = new ArrayList<MusicData>();		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,					null, null, null, Audio.Media.ARTIST);// 大小,8			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					} else {						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		return musicList;	}	public static List<MusicData> getSongsByArtist(Context context, String id){		Log.e(TAG,"getSongsByArtist()......");		List<MusicData> musicList = new ArrayList<MusicData>();		String selection = Audio.Media.ARTIST_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";		String sortOrder = AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK;		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(uri, null, selection, null, sortOrder);			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					}else{						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		Log.e(TAG,"musicList.size()=" + musicList.size());		return musicList;	}	public static List<MusicData> getSongByAlbum(Context context, String id){		List<MusicData> musicList = new ArrayList<MusicData>();		String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";        String sortOrder = AudioColumns.TRACK;        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(uri, null, selection, null, sortOrder);			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					}else{						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		return musicList;	}	public static HashMap<String, String> getAllArtist (Context context) {		HashMap<String, String> map = new HashMap<String, String>();		List<MusicData> musicDatas = getAllSongs(context);		for (MusicData info:musicDatas) {			map.put(info.artistId, info.artist);		}		return map;	}	public static HashMap<String, String> getAllAlbum (Context context) {		HashMap<String, String> map = new HashMap<String, String>();		List<MusicData> musicDatas = getAllSongs(context);		for (MusicData info:musicDatas) {			map.put(info.albumId, info.album);		}		return map;	}}