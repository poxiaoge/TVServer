package com.example.poxiaoge.tvserver.application;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.poxiaoge.tvserver.model.AppItem;
import com.example.poxiaoge.tvserver.model.MediaItem;
import com.example.poxiaoge.tvserver.utils.PreferenceUtil;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.example.poxiaoge.tvserver.dmp.ContentItem;
import com.example.poxiaoge.tvserver.dmp.DeviceItem;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.support.model.DIDLContent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseApplication extends Application {

//	public static DeviceItem deviceItem;
//
//	public DIDLContent didl;

	public static DeviceItem dmrDeviceItem;

	public static boolean isLocalDmr = true;

//	public ArrayList<ContentItem> listMusic;
//
//	public ArrayList<ContentItem> listPhoto;
//
//	public ArrayList<ContentItem> listPlayMusic = new ArrayList();
//
//	public ArrayList<ContentItem> listVideo;
//
//	public ArrayList<ContentItem> listcontent;

	public HashMap<String, ArrayList<ContentItem>> map;

	// public MediaUtils mediaUtils;

//	public int position;

	public static AndroidUpnpService upnpService;

//	public static int MOBILE_In_PORT = 12548;
//
//	public static int PC_OUT_PORT = 9752;
//	public static String PC_IP = null;
//先线程1监听mobile_broad_port。监听到之后就给mobile_ip和mobile_port本地存储一下，然后开启线程2监听local_port。
	//线程2里可能收到的有来自mobile和来自pc的信息。来自mobile的有（报告pc ip 端口）和（命令）两种。
	//TODO:测试完后改回来
	public static String PC_IP = "192.168.1.126";
	public static int PC_PORT = 8888;
	public static String MOBILE_IP = "192.168.1.116";
	public static int MOBILE_PORT = 0;
	public static String LOCAL_IP = null;
	public static int LOCAL_PORT = 12548;
	public static int MOBILE_BROAD_PORT = 9321;
//	public static String PC_IP = null;
//	public static int PC_PORT = 0;
//	public static String MOBILE_IP = null;
//	public static int MOBILE_PORT = 0;
//	public static String LOCAL_IP = null;
//	public static int LOCAL_PORT = 12548;
//	public static int MOBILE_BROAD_PORT = 9321;



	private static String dmrName;
	private static int image_slide_time;




	public static Context mContext;

	private static InetAddress inetAddress;

	private static String hostAddress;

	private static String hostName;

	private static String deviceID;


    private static List<AppItem> mAppList = new ArrayList<>();
    private static List<AppItem> mMyAppList = new ArrayList<>();
	private static List<AppItem> mSystemAppList = new ArrayList<>();
	private static List<MediaItem> mVideoList = new ArrayList<>();
	private static List<MediaItem> mAudioList = new ArrayList<>();
	private static List<MediaItem> mImageList = new ArrayList<>();
	private static Map<String, List<MediaItem>> mMediaMap = new HashMap<>();

	private static Boolean appOK;
	public static Boolean videoOk;
	public static Boolean audioOk;
	public static Boolean imageOk;
	private static Boolean mediaOk;


	private static String miracastPackageName;
	private static String rdpPackageName;
	private static Boolean miracastOK;
	private static Boolean rdpOK;
//
//    public static final int GET_TV_APPS_OK = 1;
//    public static final int OPEN_TV_APPS_OK = 2;
//    public static final int GET_TV_VIDEOS_OK = 3;
//    public static final int GET_TV_AUDIOS_OK = 4;
//    public static final int GET_TV_IMAGES_OK = 5;
//    public static final int OPEN_TV_MEDIAS_OK = 6;

	public static Boolean FLAG_RDP = false;
	public static Boolean FLAG_BS = false;

	public static List<MediaItem> rootVideoFolders;
	public static List<MediaItem> rootAudioFolders;
	public static List<MediaItem> rootImageFolders;

    public static  String appRootPath;

    public static int thumbnailPort = 9876;

	private static final String tag = "BaseApplication";


    @Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
		deviceID = new PreferenceUtil(mContext).read("UUID");
		if (deviceID == null || deviceID.equals("")) {
			deviceID =	UUID.randomUUID().toString();
			new PreferenceUtil(mContext).write("UUID",deviceID );
		}
//		Log.e(tag, "DeviceID: " + deviceID);

		rootVideoFolders = new ArrayList<>();
		rootAudioFolders = new ArrayList<>();
		rootImageFolders = new ArrayList<>();


        try {
            appRootPath = getExternalCacheDir().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initImageLoader(getApplicationContext());
	}

	public static Context getContext() {
		return mContext;
	}

	public static String getDeviceID() {
		return deviceID;
	}

	public static void setSystemAppList(List<AppItem> list) {
		BaseApplication.mSystemAppList=list;
	}

	public static List<AppItem> getSystemAppList(){
		return BaseApplication.mSystemAppList;
	}

	public static void setMyAppList(List<AppItem> list) {
		BaseApplication.mMyAppList=list;
	}

	public static List<AppItem> getMyAppList(){
		return BaseApplication.mMyAppList;
	}

	public static void setAppList(List<AppItem> list1, List<AppItem> list2) {
		for (AppItem item : list1) {
		BaseApplication.mAppList.add(item);
		}
		for (AppItem item : list2) {
			BaseApplication.mAppList.add(item);
		}
		BaseApplication.appOK=true;
	}

	public static List<AppItem> getAppList(){
		return BaseApplication.mAppList;
	}

	public static void setVideoList(List<MediaItem> list) {
		BaseApplication.mVideoList = list;
		BaseApplication.videoOk=true;
	}

	public static List<MediaItem> getVideoList(){
		return BaseApplication.mVideoList;
	}

	public static void setAudioList(List<MediaItem> list) {
		BaseApplication.mAudioList = list;
		BaseApplication.audioOk=true;
	}

	public static List<MediaItem> getAudioList(){
		return BaseApplication.mAudioList;
	}

	public static void setImageList(List<MediaItem> list) {
		BaseApplication.mImageList = list;
		BaseApplication.imageOk=true;
	}

	public static List<MediaItem> getImageList(){
		return BaseApplication.mImageList;
	}

    public static void setMediaMap(List<MediaItem> videos, List<MediaItem> audios, List<MediaItem> images) {
        mMediaMap.put("video", videos);
        mMediaMap.put("audio", audios);
        mMediaMap.put("image", images);
        BaseApplication.mediaOk=true;
    }

    public static Map<String,List<MediaItem>> getMediaMap(){
        if (BaseApplication.mediaOk) {
            return mMediaMap;
        }
        else{
            return null;
        }
    }





	public static Boolean getAppOK(){
		return BaseApplication.appOK;
	}

	public static Boolean getMediaOk(){
		if (BaseApplication.videoOk && BaseApplication.audioOk && BaseApplication.imageOk) {
			return true;
		}
		else return false;
	}


	public static void setMiracastPackageName(String packageName) {
		BaseApplication.miracastPackageName=packageName;
		BaseApplication.miracastOK=true;
	}

	public static String getMiracastPackageName(){
		return BaseApplication.miracastPackageName;
	}

	public static Boolean getMiracastOK(){
		return BaseApplication.miracastOK;
	}

	public static void setRdpPackageName(String packageName) {
		BaseApplication.rdpPackageName=packageName;
		BaseApplication.rdpOK=true;
	}

	public static String getRdpPackageName(){
		return BaseApplication.rdpPackageName;
	}

	public static Boolean getRdpOK(){
		return BaseApplication.rdpOK;
	}




	public static void setDMRName(String dmrName) {
		BaseApplication.dmrName = dmrName;
	}
	public static String getDMRName() {
		return BaseApplication.dmrName;
	}

	public static void setImage_slide_time(int time) {
		BaseApplication.image_slide_time = time;
	}

	public static int getImage_slide_time() {
		return BaseApplication.image_slide_time;
	}







	public static void setLocalIpAddress(InetAddress inetAddr) {
		inetAddress = inetAddr;
        String ip = inetAddress.toString();
        LOCAL_IP = ip.substring(ip.indexOf("/") + 1);
	}

	public static InetAddress getLocalIpAddress() {
		return inetAddress;
	}

	public static String getHostAddress() {
		return hostAddress;
	}

	public static void setHostAddress(String hostAddress) {
		BaseApplication.hostAddress = hostAddress;
	}

	public static String getHostName() {
		return hostName;
	}

	public static void setHostName(String hostName) {
		BaseApplication.hostName = hostName;
	}


	//以下用来将某个path设置为链接目录。传过来的mime要确保是video,audio,image三者之一。
	public static boolean linkUserFolder(String path,String mime) {
		File f = new File(path);
		if (f.isDirectory()) {
			MediaItem newFolder = new MediaItem();
			newFolder.setName(f.getName());
			newFolder.setLocation("tv");
			newFolder.setType(mime);
			newFolder.setPathName(path);
			newFolder.setIsFolder(true);

			PreferenceUtil pu = new PreferenceUtil(mContext);
			String folderString = pu.read(mime);
			List<MediaItem> mediaFolders;

			if (!folderString.equals("")) {
				mediaFolders = JSON.parseObject(folderString, new TypeReference<List<MediaItem>>() {
				});
			}else {
				mediaFolders = new ArrayList<>();
			}
			mediaFolders.add(newFolder);

			String newMediaFoldersString = JSON.toJSONString(mediaFolders);

			pu.write(mime, newMediaFoldersString);

			switch (mime) {
				case ("video"):
					BaseApplication.rootVideoFolders.add(newFolder);
					break;
				case ("audio"):
					BaseApplication.rootAudioFolders.add(newFolder);
					break;
				case ("image"):
					BaseApplication.rootImageFolders.add(newFolder);
					break;
			}

			return true;
		}
		else {
			return false;
		}


	}



	public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them, 
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .enableLogging() // Not necessary in common
                .build();
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);
    }
}
