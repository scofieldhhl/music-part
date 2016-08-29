package org.app.enjoy.music.util;import android.content.ContentResolver;import android.content.Context;import android.database.Cursor;import android.net.Uri;import android.os.Environment;import android.provider.MediaStore;import android.provider.MediaStore.Audio;import android.provider.MediaStore.Audio.AudioColumns;import android.text.TextUtils;import android.util.Log;import org.app.enjoy.music.data.MusicData;import org.app.enjoy.music.tool.Contsant;import org.app.enjoy.music.tool.CueUtil;import org.app.enjoy.music.tool.LogTool;import java.io.File;import java.util.ArrayList;import java.util.HashMap;import java.util.List;import tv.danmaku.ijk.media.player.IjkMediaPlayer;public class MusicUtil {	private static String TAG = "MusicUtil";	public static final int TYPE_ALL = 0;	public static final int TYPE_ARTIST = 1;	public static final int TYPE_ALBUM = 2;	public static List<MusicData> getSongList(int type, Context context, String id){		List<MusicData> musicList = new ArrayList<MusicData>();		if (type == TYPE_ALL) {			musicList = getAllSongs(context);		} else if (type == TYPE_ARTIST) {			musicList = getSongsByArtist(context, id);		} else if (type == TYPE_ALBUM) {			musicList = getSongByAlbum(context, id);		}		return musicList;	}	public static List<MusicData> getAllSongs(Context context){		List<MusicData> musicList = new ArrayList<MusicData>();		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,					null, null, null, Audio.Media.ARTIST);// 大小,8			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					} else {						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		return musicList;	}	public static List<MusicData> getSongsByArtist(Context context, String id){		Log.e(TAG,"getSongsByArtist()......");		List<MusicData> musicList = new ArrayList<MusicData>();		String selection = Audio.Media.ARTIST_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";		String sortOrder = AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK;		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(uri, null, selection, null, sortOrder);			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					}else{						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		Log.e(TAG,"musicList.size()=" + musicList.size());		return musicList;	}	public static List<MusicData> getSongByAlbum(Context context, String id){		List<MusicData> musicList = new ArrayList<MusicData>();		String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC + "=1";        String sortOrder = AudioColumns.TRACK;        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;		Cursor cursor = null;		try {			cursor = context.getContentResolver().query(uri, null, selection, null, sortOrder);			if (cursor != null && cursor.getCount() > 0) {				for (int i = 0; i < cursor.getCount(); i++) {					cursor.moveToPosition(i);					MusicData md = new MusicData();					md.title = cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE));					md.duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION));					md.artist = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST));					md.artistId = cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST_ID));					md.id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID));					md.displayName = cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME));					md.data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA));					if(md.data != null && !"".equals(md.data) && md.data.startsWith("rage")){						md.path = md.data.replace("rage","/storage");					}else{						md.path = md.data;					}					md.albumId = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM_ID));					md.album = cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM));					md.size = cursor.getString(cursor.getColumnIndex(Audio.Media.SIZE));					musicList.add(md);					if(md.path != null && md.path.endsWith(Contsant.DSD_APE)){						List<MusicData> cueMusicList = CueUtil.getMusicListFromApe(md);						if(cueMusicList != null && cueMusicList.size() > 0){							musicList.addAll(cueMusicList);						}					}				}			}		} catch (Exception e) {			// TODO: handle exception		} finally {			if (cursor != null) {				cursor.close();			}		}		return musicList;	}	public static HashMap<String, MusicData> getAllArtist (Context context) {		HashMap<String, MusicData> map = new HashMap();		List<MusicData> musicDatas = getAllSongs(context);		for (MusicData info:musicDatas) {			map.put(info.artistId, info);		}		return map;	}	public static HashMap<String, MusicData> getAllAlbum (Context context) {		HashMap<String, MusicData> map = new HashMap();		List<MusicData> musicDatas = getAllSongs(context);		for (MusicData info:musicDatas) {			map.put(info.albumId, info);		}		return map;	}	public static void  deleteFile (Context context,String filePath,int id) {		Log.e(TAG,"deleteFile()...... ");		deleteMusic(context,id);		File file = new File(filePath);		if (file.isFile() && file.exists()) {			file.delete();			Log.e(TAG,"filePath = " + filePath);		}	}	public static void deleteMusic (Context context,int id) {		ContentResolver resolver = context.getContentResolver();		resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media._ID +"="+ id, null);	}	private static String[] arrMusicExtension = new String[]{"dsf","dff","dst","dsd", "wma", "aif", "aac", "iso"};	public static void GetMusicFiles(String Path, boolean IsIterative, List<MusicData> list)  //搜索目录，扩展名，是否进入子文件夹	{		//Path = "/mnt/external_sd/";//sdcard		if(Path == null || TextUtils.isEmpty(Path)){			return;		}		File[] files = new File(Path).listFiles();		if(files != null && files.length > 0){			for (int i = 0; i < files.length; i++){				File f = files[i];				if (f.isFile()){					String[] arrFile = f.getPath().split("\\.");					if(arrFile != null && arrFile.length >0){						int length = arrFile.length;						if(arrFile[length -1] != null){							for(String str : arrMusicExtension){								if(arrFile[length -1].equalsIgnoreCase(str)){									if(Contsant.DSD_ISO.equalsIgnoreCase(str)){										while (!IjkMediaPlayer.mIsLibLoaded){											try {												LogTool.e("!IjkMediaPlayer.mIsLibLoaded");												Thread.sleep(500);											} catch (InterruptedException e) {												e.printStackTrace();											}										}										String cueIso = IjkMediaPlayer.native_getsacdiso_cue(f.getPath());										LogTool.d("cueIso:"+cueIso);										List<MusicData> cueMusicList = CueUtil.getMusicsFromIso(cueIso, f.getPath());										if(cueMusicList != null && cueMusicList.size() > 0){											list.addAll(cueMusicList);										}									}else{										MusicData md = new MusicData();										String[] arrFileName = f.getPath().split("/");										md.title = arrFile[length -1];										if(arrFileName != null && arrFileName.length > 0){											md.title = arrFileName[arrFileName.length - 1].substring(0,arrFileName[arrFileName.length - 1].indexOf("."));										}										md.duration = 0;										md.artist = "";										md.displayName = md.title;										md.data = f.getPath();										md.path = f.getPath();										LogTool.i(f.getPath());										md.size = String.valueOf(f.length());										list.add(md);										LogTool.d("GetMusicFiles:"+f.getPath());									}									break;								}							}						}					}					if (!IsIterative)						break;				}				else if (f.isDirectory() && f.getPath().indexOf("/.") == -1)  //忽略点文件（隐藏文件/文件夹）					GetMusicFiles(f.getPath(), IsIterative, list);			}		}	}	public static String getSDPath(){		File sdDir = null;		boolean sdCardExist = Environment.getExternalStorageState()				.equals(Environment.MEDIA_MOUNTED);   //判断sd卡是否存在		if(sdCardExist){			sdDir = Environment.getExternalStorageDirectory();//获取跟目录		}		if(sdDir != null){			return sdDir.toString();		}else{			return null;		}	}	public static int getPositionByMusicName (List<MusicData> list, String name) {		int position = -1;		if (list == null || list.size() == 0) {			return -1;		}		if (name != null && !TextUtils.isEmpty(name)) {			for (int i=0;i<list.size();i++) {				if (name.equals(list.get(i).title)) {					position = i;					break;				}			}		}		Log.e(TAG, "getPositionByMusicName-position = " + position);		return position;	}}