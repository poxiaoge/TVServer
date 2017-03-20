package com.example.poxiaoge.tvserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.poxiaoge.tvserver.application.BaseApplication;
import com.example.poxiaoge.tvserver.model.AppItem;
import com.example.poxiaoge.tvserver.model.DeviceItem;
import com.example.poxiaoge.tvserver.model.MediaItem;
import com.example.poxiaoge.tvserver.utils.AppUtil;
import com.example.poxiaoge.tvserver.utils.FileHelper;
import com.example.poxiaoge.tvserver.utils.FileUtil;
import com.example.poxiaoge.tvserver.utils.HttpServer;
import com.example.poxiaoge.tvserver.utils.ImageUtil;
import com.example.poxiaoge.tvserver.utils.MediaUtil;
import com.example.poxiaoge.tvserver.utils.PreferenceUtil;
import com.example.poxiaoge.tvserver.utils.SendToPCThread;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

//    ServerSocket server;
//    DataInputStream in;
//    int port;
    //    public final String tag = "TVServer";
//    boolean flag = false;
    private long exitTime = 0;

    public static final int GET_IP_FAIL = 0;

//    public static final int GET_IP_SUC = 1;

    public static final int OPEN_APP_FAIL = 2;

    public static final int GET_EXTERNAL_OK = 3;

//    public static final int EXTERNAL_MOUNTED = 4;
//
//    public static final int EXTERNAL_REMOVED = 5;

    public static final int GET_APP_OK = 6;
    public static final int GET_MEDIA_OK = 7;
    public static final int GET_MEDIA_FAIL = 8;
//    public static final int GET_APP_FAIL = 9;
    public static final int CREATE_THUMBNAIL_OK = 10;

//    private String hostName;
//
//    private String hostAddress;

//    private static List<AppItem> mAppList;
    private static List<AppItem> mMyAppList;
    private static List<AppItem> mSystemAppList;
    private static List<MediaItem> mVideoList = new ArrayList<>();
    private static List<MediaItem> mAudioList = new ArrayList<>();
    private static List<MediaItem> mImageList = new ArrayList<>();


//    private SharedPreferences sp;
//    private SharedPreferences.Editor editor;

    private static String tag = "MainActivity";

    private Context mContext;

//    private ListView list;
//    private ListView list_media;
//    private TextView internal_path;
//    private TextView external_path;

    public TextView text_debug_msg;


    //    public String SDCARD_PATH = System.getenv("SECONDARY_STORAGE");
    //小米电视2s外置sd卡路径
    public String SDCARD_PATH = "/mnt/usb";
    //小米电视2s内置sd卡路径
    public String SDCARD_PATH2 = "/storage/sdcard0";
    //以下是红米note2的sd卡路径
//    public String SDCARD_PATH = "/storage";

//    Map<String, List<AppItem>> map;

    Button btn_push2pc;

    public MediaItem defaultVideoFolder;
    public MediaItem defaultAudioFolder;
    public MediaItem defaultImageFolder;

    public List<MediaItem> userVideoFolders;
    public List<MediaItem> userAudioFolders;
    public List<MediaItem> userImageFolders;

    HttpServer httpServer;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //在TV屏幕上显示debug信息
        showDebugMsg();

        //如果cache目录下没有这两个apk，则创建。
        createApks("bsplayer.apk");
        createApks("rdp.apk");

        initData();
        Intent i = new Intent(mContext, DMRService.class);
        startService(i);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BaseApplication.FLAG_RDP || !BaseApplication.FLAG_BS) {
            scanApps();
            checkApps();
        }
    }


    public void showDebugMsg() {
        text_debug_msg = (TextView) findViewById(R.id.text_debug_msg);

        btn_push2pc = (Button) findViewById(R.id.btn_push2pc);
        btn_push2pc.setOnClickListener(clickListener1);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                push2DebugMsg("getExternalStorageDirectory: " + Environment.getExternalStorageDirectory().getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    View.OnClickListener clickListener1 = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
//            pushDeviceMsgToPC();
        }
    };


    public void push2DebugMsg(String newMsg) {
        text_debug_msg.setText(text_debug_msg.getText() + "\n" + newMsg);
    }


    public void initData() {
        getIp();
        scanApps();
        initFolders();
        scanMedias();
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_IP_FAIL: {
                    Toast.makeText(mContext, R.string.ip_get_fail, Toast.LENGTH_SHORT);
                    break;
                }
                case OPEN_APP_FAIL: {
                    Toast.makeText(mContext, "failed to open this app", Toast.LENGTH_LONG).show();
                    break;
                }
                case GET_EXTERNAL_OK: {
                    Log.e("get_external_ok", "begin scan external!!");
                    Toast.makeText(mContext, "Begin scan external files", Toast.LENGTH_SHORT).show();
                }
                case GET_APP_OK: {
                    BaseApplication.setMyAppList(mMyAppList);
                    BaseApplication.setSystemAppList(mSystemAppList);
                    BaseApplication.setAppList(mMyAppList, mSystemAppList);
//                    pushDeviceMsgToPC();
                    Log.e(tag, "GET_APP_OK");
                    break;
                }
                case GET_MEDIA_OK: {
                    BaseApplication.setVideoList(mVideoList);
                    BaseApplication.setAudioList(mAudioList);
                    BaseApplication.setImageList(mImageList);

                    BaseApplication.setMediaMap(BaseApplication.getVideoList()
                            , BaseApplication.getAudioList()
                            , BaseApplication.getImageList());

                    createThumbnail();

                    //TODO:测试完后要把下面这个注释掉
//                    if (BaseApplication.getMediaOk()) {
//                        pushDeviceMsgToPC();
//                    }


//                    pushDeviceMsgToPC();


                    //     Log.e(tag, "Before json map");
//          //          Log.e("json map", JSON.toJSONString(BaseApplication.getMediaMap()));
                    //    Log.e(tag, "After json map");

                    ////     BaseApplication.setMediaMap(new ArrayList<MediaItem>(),new ArrayList<MediaItem>(),new ArrayList<MediaItem>());
//                    Log.e("Video List Length", String.valueOf(BaseApplication.getVideoList().size()));
//                    Log.e("Audio List Length", String.valueOf(BaseApplication.getAudioList().size()));
//                    Log.e("Image List Length", String.valueOf(BaseApplication.getImageList().size()));
                    Log.e(tag, "GET_MEDIA_OK");
                    break;
                }

                case CREATE_THUMBNAIL_OK: {
                    try {
                        httpServer = new HttpServer(BaseApplication.thumbnailPort, BaseApplication.appRootPath+"/thumbnail/");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
            super.handleMessage(msg);
        }

    };


//TODO:测试完后可以把下面这个方法消掉。
    public void pushDeviceMsgToPC() {
        DeviceItem tvDevice = new DeviceItem();
        tvDevice.setName(BaseApplication.getDMRName());
//        String ip = BaseApplication.getLocalIpAddress().toString();
//        tvDevice.setIp(ip.substring(ip.indexOf("/") + 1));
        tvDevice.setIp(BaseApplication.LOCAL_IP);
        tvDevice.setUuid(BaseApplication.getDeviceID());
        tvDevice.setType("Online");
        tvDevice.setDlnaOk(true);
        tvDevice.setMiracastOk(BaseApplication.getMiracastOK());
        tvDevice.setRdpOk(BaseApplication.getRdpOK());
        tvDevice.setDeviceName(BaseApplication.getDMRName());
        tvDevice.setScreen("");
        String deviceString = JSON.toJSONString(tvDevice);
        Log.e("push Device to PC", deviceString);
//        Toast.makeText(mContext,deviceString,Toast.LENGTH_LONG).show();
//        push2DebugMsg(deviceString);
        new SendToPCThread(deviceString, BaseApplication.PC_IP, BaseApplication.PC_PORT).start();
    }


    private void createApks(String apkname) {
        try {
            File f = new File(BaseApplication.appRootPath+"/"+apkname);
//            if (f.exists()) {
//                f.delete();
//                Log.e("delete", filepath);
//            }

            if (!f.exists()) {
                InputStream inputStream;
                if (apkname.equals("rdp.apk")) {
                    inputStream = getResources().openRawResource(R.raw.rdp);
                } else {
                    inputStream = getResources().openRawResource(R.raw.bsplayer);

                }

                FileOutputStream fos = new FileOutputStream(f);
                byte[] buffer = new byte[8192];
                int count = 0;
                while ((count = inputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkApps() {
//        for (AppItem appItem : mMyAppList) {
//            if (appItem.getPackageName().equals("com.microsoft.rdc.android")) {
//                BaseApplication.FLAG_RDP=true;
//            }
//            if (appItem.getPackageName().equals("com.bsplayer.bspandroid.full")) {
//                BaseApplication.FLAG_BS=true;
//            }
//        }

        if (!BaseApplication.FLAG_BS) {
            installApps("bsplayer.apk");
        }
        if (!BaseApplication.FLAG_RDP) {
            installApps("rdp.apk");
        }
    }

    private void installApps(String apkname) {
        String filepath;
        try {
            filepath = getExternalCacheDir().getCanonicalPath() + "/" + apkname;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filepath)), "application/vnd.android.package-archive");
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    public void initDocumentFile(){
//        SDCARD_PATH = System.getenv("SECONDARY_STORAGE");
//        if (!TextUtils.isEmpty(SDCARD_PATH)) {
//            String strUri = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_DEFAULT_URI, null);
//            if (TextUtils.isEmpty(strUri)) {
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//                startActivityForResult(intent, 101);
//            } else {
//                Uri uri = Uri.parse(strUri);
//                rootDocumentFile = DocumentFile.fromTreeUri(this, uri);
//            }
//        }
//    }

//    public Boolean setExternalRoot(){
//        SDCARD_PATH = System.getenv("SECONDARY_STORAGE");
//        if (!TextUtils.isEmpty(SDCARD_PATH)) {
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//            startActivityForResult(intent, 101);
//           return true;
//        }
//        else{
//            return false;
//        }
//    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 101 && resultCode == RESULT_OK) {
//            Uri uri = data.getData();
//            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
//            getContentResolver().takePersistableUriPermission(uri, takeFlags);
//            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PREF_DEFAULT_URI, uri.toString()).apply();
//            rootDocumentFile = DocumentFile.fromTreeUri(this, uri);
//            mHandle.sendEmptyMessage(GET_EXTERNAL_OK);
//        }
//    }

//    public void scanExternalFiles(DocumentFile documentFile){
//        if (!documentFile.isDirectory()) {
////            FileItem fileItem = new FileItem();
////            fileItem.setLocation("External");
////            fileItem.setFile(documentFile);
////            fileItem.setUri(documentFile.getUri());
////            fileItem.setFileName(documentFile.getName());
////
////            fileItem.setType(documentFile.getType());
////            fileItem.setLastModified(documentFile.lastModified());
////            fileItem.setSize(documentFile.length());
//            Log.e("Name!!!:", documentFile.getName());
//            Log.e("Uri!!!:", documentFile.getUri().toString());
//            Log.e("Type!!!:", documentFile.getType());
//        }
//        else{
//            DocumentFile[] files = documentFile.listFiles();
//            for (DocumentFile file : files) {
//                scanExternalFiles(file);
//            }
//        }
//
//    }

    private void scanApps() {
        BaseApplication.FLAG_RDP = false;
        BaseApplication.FLAG_BS = false;
        Map<String, List<AppItem>> map = AppUtil.scanInstallApp(mContext.getPackageManager());
        mMyAppList = map.get("MyApp");
        mSystemAppList = map.get("SystemApp");
        mHandler.sendEmptyMessage(GET_APP_OK);
    }

//    public void showMedia() {
//        try {
//            readFile("/mnt/sdcard2");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        //TODO:外置存储卡的路径在红米上是/mnt/sdcard2 ，在小米电视上是/mnt/usb/sda1 /mnt/usb/sdb1
////        int currentapiVersion=android.os.Build.VERSION.SDK_INT;
//        internal_path = (TextView) findViewById(R.id.internal_path);
//        external_path = (TextView) findViewById(R.id.external_path);
////        internal_path.setText(MediaStore.Video.Media.INTERNAL_CONTENT_URI.toString());
////        external_path.setText(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString());
//
////        Toast.makeText(mContext,MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString(),Toast.LENGTH_LONG).show();
////        ArrayList<Map<String,String>> videoMap = (ArrayList<String>)getVideoFilePaths();
//        ArrayList<String> videoList = (ArrayList<String>) getVideoFilePaths();
//        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(mContext, android.R.layout.simple_expandable_list_item_1, videoList);
//        list_media = (ListView) findViewById(R.id.list_media);
//        list_media.setAdapter(adapter1);
//
////        File f = new File("/mnt/sdcard2");
////        Uri sdURI = Uri.fromFile(f);
//
//
//    }

    private void getIp() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                Log.e(tag, "ip address" + ipAddress);

                InetAddress inetAddress;
                try {
                    inetAddress = InetAddress.getByName(String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff)));
//                    Log.e(tag, "ip 2:" + InetAddress.getByName(null));
                    Log.e(tag, "ip:" + inetAddress.toString().substring(inetAddress.toString().indexOf("/") + 1));
                    BaseApplication.setHostName(inetAddress.getHostName());
                    BaseApplication.setHostAddress(inetAddress.getHostAddress());
                    BaseApplication.setLocalIpAddress(inetAddress);
                } catch (UnknownHostException e) {
                    mHandler.sendEmptyMessage(GET_IP_FAIL);
                }
            }
        }).start();
    }


//    private void createFolder() {
//        FileUtil.createSDCardDir(true);
//    }

    public void readFile(String filepath) throws IOException {
        try {
            File file = new File(filepath);
            if (!file.isDirectory()) {
                String fileSuffix = FileUtil.getFileSuffix(filepath).toLowerCase();
                switch (MediaUtil.verifyMediaTypes(fileSuffix)) {
                    case ("video"):
                        Log.e("from readFile", filepath);
                        MediaItem media1 = new MediaItem();
                        media1.setPathName(filepath);
                        media1.setName(file.getName());
//                        media1.setUri(Uri.fromFile(file));
                        media1.setLocation("tv");

                        mVideoList.add(media1);
                        break;
                    case ("audio"):
                        MediaItem media2 = new MediaItem();
                        media2.setPathName(filepath);
                        media2.setName(file.getName());
//                        media2.setUri(Uri.fromFile(file));
                        media2.setLocation("tv");
                        mAudioList.add(media2);
                        break;
                    case ("image"):
                        MediaItem media3 = new MediaItem();
                        media3.setPathName(filepath);
                        media3.setName(file.getName());
//                        media3.setUri(Uri.fromFile(file));
                        media3.setLocation("tv");
                        mImageList.add(media3);
                        break;
                }
            } else if (file.isDirectory()) {
                String[] filelist = file.list();
                if (filelist != null && filelist.length > 0) {
                    for (String fileItem : filelist) {
                        readFile(filepath + File.separator + fileItem);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //该方法用于检测默认媒体文件夹是否存在，不存在就创建。从SharedPreference里读取用户选择的媒体目录，并给BaseApplication的rootxxxFolders赋值。
    public void initFolders() {
        if (!AppUtil.checkSDCardExists(BaseApplication.appRootPath + "/video")) {
            FileHelper.createDirectory(BaseApplication.appRootPath + "/video");
        }
        if (!AppUtil.checkSDCardExists(BaseApplication.appRootPath + "/audio")) {
            FileHelper.createDirectory(BaseApplication.appRootPath + "/audio");
        }
        if (!AppUtil.checkSDCardExists(BaseApplication.appRootPath + "/image")) {
            FileHelper.createDirectory(BaseApplication.appRootPath + "/image");
        }
            Log.e(tag, "before create thumbnail");
        if (!AppUtil.checkSDCardExists(BaseApplication.appRootPath + "/thumbnail")) {
            FileHelper.createDirectory(BaseApplication.appRootPath + "/thumbnail");
        }


        defaultVideoFolder = new MediaItem();
        defaultVideoFolder.setName("video");
        defaultVideoFolder.setPathName(BaseApplication.appRootPath + "/video");
        defaultVideoFolder.setType("video");
        defaultVideoFolder.setLocation("tv");
        defaultVideoFolder.setIsFolder(true);

        defaultAudioFolder = new MediaItem();
        defaultAudioFolder.setName("audio");
        defaultAudioFolder.setPathName(BaseApplication.appRootPath + "/audio");
        defaultAudioFolder.setType("audio");
        defaultAudioFolder.setLocation("tv");
        defaultAudioFolder.setIsFolder(true);

        defaultImageFolder = new MediaItem();
        defaultImageFolder.setName("image");
        defaultImageFolder.setPathName(BaseApplication.appRootPath + "/image");
        defaultImageFolder.setType("image");
        defaultImageFolder.setLocation("tv");
        defaultImageFolder.setIsFolder(true);

        PreferenceUtil pu = new PreferenceUtil(mContext);
        String userVideoString = pu.read("video");
        String userAudioString = pu.read("audio");
        String userImageString = pu.read("image");

        //TODO：设置该变量时，要先序列化为json字符串，然后再保存到sharedPreference里面
        if (!userVideoString.equals("")) {
            userVideoFolders = JSON.parseObject(userVideoString, new TypeReference<List<MediaItem>>() {
            });
            BaseApplication.rootVideoFolders.addAll(userVideoFolders);
        }
        BaseApplication.rootVideoFolders.add(defaultVideoFolder);

        if (!userAudioString.equals("")) {
            userAudioFolders = JSON.parseObject(userAudioString, new TypeReference<List<MediaItem>>() {
            });
            BaseApplication.rootAudioFolders.addAll(userAudioFolders);
        }
        BaseApplication.rootAudioFolders.add(defaultAudioFolder);

        if (!userImageString.equals("")) {
            userImageFolders = JSON.parseObject(userImageString, new TypeReference<List<MediaItem>>() {
            });
            BaseApplication.rootImageFolders.addAll(userImageFolders);
        }
        BaseApplication.rootImageFolders.add(defaultImageFolder);


    }



    //该方法用于生成缩略图
    private void createThumbnail() {
        new Thread() {
            @Override
            public void run() {
                String rootPath = BaseApplication.appRootPath + "/thumbnail";
                if (!AppUtil.checkSDCardExists(rootPath)) {
                    FileHelper.createDirectory(rootPath);
                }

                //TODO：生成缩略图用ThumbnailUtils或者MediaMetadataRetriever都可以，选择一个合适的。
                for (MediaItem mediaItem : mVideoList) {
                    File f = new File(mediaItem.getPathName());
                    File f2 = new File(rootPath + "/" +
                            f.length() + f.getName().substring(0, f.getName().lastIndexOf(".")) + ".png");
                    if (!f2.exists()) {
                        AppUtil.createSingleThumbnail(mediaItem.getPathName(), "video");
                    }
                }

                for (MediaItem mediaItem : mImageList) {
                    File f = new File(mediaItem.getPathName());
                    File f2 = new File(rootPath + "/" +
                            f.length() + f.getName().substring(0, f.getName().lastIndexOf(".")) + ".png");
                    if (!f2.exists()) {
                        AppUtil.createSingleThumbnail(mediaItem.getPathName(), "image");
                    }
                }

                mHandler.sendEmptyMessage(CREATE_THUMBNAIL_OK);
            }
        }.start();
    }


    private void scanMedias() {
        new Thread() {
            @Override
            public void run() {
                try {
                    for (MediaItem mediaItem : BaseApplication.rootVideoFolders) {
                        String path = mediaItem.getPathName();
                        if (!TextUtils.isEmpty(path) && AppUtil.checkSDCardExists(path)) {
                            readFile(path);
                        }
                    }
                    for (MediaItem mediaItem : BaseApplication.rootAudioFolders) {
                        String path = mediaItem.getPathName();
                        if (!TextUtils.isEmpty(path) && AppUtil.checkSDCardExists(path)) {
                            readFile(path);
                        }
                    }
                    for (MediaItem mediaItem : BaseApplication.rootImageFolders) {
                        String path = mediaItem.getPathName();
                        if (!TextUtils.isEmpty(path) && AppUtil.checkSDCardExists(path)) {
                            readFile(path);
                        }
                    }


                    mHandler.sendEmptyMessage(GET_MEDIA_OK);

                } catch (IOException e) {
                    mHandler.sendEmptyMessage(GET_MEDIA_FAIL);
                    e.printStackTrace();
                }
            }
        }.start();
    }
















//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (BaseApplication.FLAG_RDP) {
//            AppUtil.executeCmd("input tap 250 250");
//        }
//    }


    //    private void splitMediaPaths(){
//        mVideoPaths = new ArrayList<>();
//        mAudioPaths = new ArrayList<>();
//        mImagePaths = new ArrayList<>();
//        if(!mFilePaths.isEmpty()){
//            for (String filepath : mFilePaths) {
//                String fileSuffix = FileUtil.getFileSuffix(filepath).toLowerCase();
//                switch (MediaUtil.verifyMediaTypes(fileSuffix)) {
//                    case ("Video"):
//                        mVideoPaths.add(filepath);
//                        Log.e("Video", filepath);
//                        break;
//                    case ("Audio"):
//                        mAudioPaths.add(filepath);
//                        Log.e("Audio", filepath);
//                        break;
//                    case ("Image"):
//                        mImagePaths.add(filepath);
//                        Log.e("Image", filepath);
//                        break;
//                }
//            }
//        }
//    }


//    private void getVideoFilePaths() {
////        mVideoFilePaths = new ArrayList<String>();
//        Cursor cursor;
////        Cursor cursor2;
//        String[] videoColumns = {
//                MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
//                MediaStore.Video.Media.DATA, MediaStore.Video.Media.ARTIST,
//                MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.SIZE,
//                MediaStore.Video.Media.DURATION, MediaStore.Video.Media.RESOLUTION
//        };
////        String[] videoColumns = {MediaStore.Video.Media.DATA};
//        cursor = mContext.getContentResolver().query(MediaStore.Video.Media.INTERNAL_CONTENT_URI, videoColumns, null, null, null);
////        cursor = mContext.getContentResolver().query(sdURI, videoColumns, null, null, null);
////        Log.e(tag, "before cursor.moveToFirst");
//        if (null != cursor && cursor.moveToFirst()) {
//            do {
//                String id = ContentTree.VIDEO_PREFIX
//                        + cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
//                String filePath = "Internal:::" + cursor.getString(cursor
//                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
//                Log.e("from internal", filePath);
////                Map<String, String> fileInfoMap = new HashMap<String, String>();
////                fileInfoMap.put(id, filePath);
////                mVideoFilePaths.add(filePath);
////                Log.e(tag, "Add video item " + " <" + id + ">" + "from " + filePath);
//            } while (cursor.moveToNext());
//        }
//        if (null != cursor) {
//            Log.e(tag, "cursor.close");
//            cursor.close();
//        }
////        cursor2 = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
////                videoColumns, null, null, null);
////        if (null != cursor2 && cursor2.moveToFirst()) {
////            do {
////                String id = ContentTree.VIDEO_PREFIX
////                        + cursor2.getInt(cursor2.getColumnIndex(MediaStore.Video.Media._ID));
////                String filePath = "External:::" + cursor2.getString(cursor2
////                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
//////                Map<String, String> fileInfoMap = new HashMap<String, String>();
//////                fileInfoMap.put(id, filePath);
//////                mVideoFilePaths.add(filePath);
//////                Log.e(tag, "Add video item " + " <" + id + ">" + "from " + filePath);
////                Log.e("from getVideoPath", filePath);
////            } while (cursor2.moveToNext());
//////            return mVideoFilePaths;
////        }
////        if (null != cursor2) {
////            Log.e(tag, "cursor.close");
////            cursor2.close();
////        }
//    }

//    public void getAudioFilePaths() {
//        mAudioFilePaths = new ArrayList<Map<String, String>>();
//        Cursor cursor;
//        String[] audioColumns = {
//                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE,
//                MediaStore.Audio.Media.DURATION
//        };
//        cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                audioColumns, null, null, null);
//        if (null != cursor && cursor.moveToFirst()) {
//            do {
//                String id = ContentTree.AUDIO_PREFIX
//                        + cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
//                String filePath = cursor.getString(cursor
//                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
//                Map<String, String> fileInfoMap = new HashMap<String, String>();
//                fileInfoMap.put(id, filePath);
//                mAudioFilePaths.add(fileInfoMap);
//                Log.w(tag, "added audio item " + " <" + id + ">" + "from " + filePath);
//
//            } while (cursor.moveToNext());
//        }
//        if (null != cursor) {
//            cursor.close();
//        }
//    }
//
//    private void getImageFilePaths() {
//        mImageFilePaths = new ArrayList<Map<String, String>>();
//        Cursor cursor;
//        String[] imageColumns = {
//                MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE,
//                MediaStore.Images.Media.DATA,
//                MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.SIZE,
//                MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.WIDTH
//        };
//        cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                imageColumns, null, null, null);
//        if (null != cursor && cursor.moveToFirst()) {
//            do {
//                String id = ContentTree.IMAGE_PREFIX
//                        + cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
//                String filePath = cursor.getString(cursor
//                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
//                Map<String, String> fileInfoMap = new HashMap<String, String>();
//                fileInfoMap.put(id, filePath);
//                mImageFilePaths.add(fileInfoMap);
//                Log.w(tag, "added image item " + " <" + id + ">" + "from " + filePath);
//
//            } while (cursor.moveToNext());
//        }
//        if (null != cursor) {
//            cursor.close();
//        }
//    }

    /*****************************
     * 以下是非核心代码及工具代码
     **************************************************************************/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), R.string.exit,
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


//    protected int executeCmd(String cmd){
//        try{
//            Process process = Runtime.getRuntime().exec("su");
//            DataOutputStream out = new DataOutputStream(process.getOutputStream());
//            out.writeBytes(cmd + "\n");
//            out.flush();
//            out.writeBytes("exit\n");
//            out.flush();
//            out.close();
//            process.waitFor();
//            int result = process.exitValue();
//            return  result;
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            return -1;
//        }
//    }

//    private void doStartApplicationWithPackageName(String packagename) {
//
//        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
//        PackageInfo packageinfo = null;
//        try {
//            packageinfo = getPackageManager().getPackageInfo(packagename, 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        if (packageinfo == null) {
//            return;
//        }
//
//        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
//        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
//        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        resolveIntent.setPackage(packageinfo.packageName);
//
//        // 通过getPackageManager()的queryIntentActivities方法遍历
//        List<ResolveInfo> resolveinfoList = getPackageManager().queryIntentActivities(resolveIntent, 0);
//
//
//        ResolveInfo resolveinfo;
//        if (resolveinfoList.iterator().hasNext()) {
//            resolveinfo = resolveinfoList.iterator().next();
//        } else {
//            mHandle.sendEmptyMessage(OPEN_APP_FAIL);
//            return;
//        }
//        if (resolveinfo != null) {
//            // packagename = 参数packname
//            String packageName = resolveinfo.activityInfo.packageName;
//            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
//            String className = resolveinfo.activityInfo.name;
//            // LAUNCHER Intent
//            Intent intent = new Intent(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_LAUNCHER);
//
//            // 设置ComponentName参数1:packagename参数2:MainActivity路径
//            ComponentName cn = new ComponentName(packageName, className);
//
//            intent.setComponent(cn);
//            startActivity(intent);
//        }
//    }

    //    private void createVideoThumb() {
//        if (null != mVideoFilePaths && mVideoFilePaths.size() > 0) {
//            new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    for (int i = 0; i < mVideoFilePaths.size(); i++) {
//                        Set entries = mVideoFilePaths.get(i).entrySet();
//                        if (entries != null) {
//                            Iterator iterator = entries.iterator();
//                            while (iterator.hasNext()) {
//                                Map.Entry entry = (Map.Entry) iterator.next();
//                                Object id = entry.getKey();
//                                Object filePath = entry.getValue();
//
//                                Bitmap videoThumb = ImageUtil.getThumbnailForVideo(filePath
//                                        .toString());
//                                String videoSavePath = ImageUtil.getSaveVideoFilePath(
//                                        filePath.toString(), id.toString());
//                                try {
//                                    ImageUtil.saveBitmapWithFilePathSuffix(videoThumb,
//                                            videoSavePath);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    }
//                }
//
//            }).start();
//        }
// }


}
