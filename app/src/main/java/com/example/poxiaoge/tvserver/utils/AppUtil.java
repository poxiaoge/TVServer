package com.example.poxiaoge.tvserver.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.poxiaoge.tvserver.application.BaseApplication;
import com.example.poxiaoge.tvserver.model.AppItem;
import com.example.poxiaoge.tvserver.model.MediaItem;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by poxiaoge on 2016/12/21.
 */

public class AppUtil {

    public static final String tag = "AppUtil";



    public static Boolean checkSDCardExists(String path) {
        File f = new File(path);
        return f.exists();
    }


    public static boolean createSingleThumbnail(String path, String type) {
        String rootPath = BaseApplication.appRootPath + "/thumbnail";
        File f = new File(path);
        Bitmap bitmap = null;
        if (type.equals("video")) {
            bitmap = ImageUtil.getThumbnailForVideo(path);
            if (bitmap != null) {
                bitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
            }
        }
        if (type.equals("image")) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            bitmap = ImageUtil.getBitmapByIo(fis, 80);
        }

        if (bitmap != null) {
            try {
                ImageUtil.saveBitmapWithFilePathSuffix(bitmap, rootPath + "/" +
                        f.length() + f.getName().substring(0, f.getName().lastIndexOf(".")) + ".png");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        return true;
    }




    public static Map<String, List<AppItem>> scanInstallApp(PackageManager packageManager) {
        List<AppItem> myappList = new ArrayList<>();
        List<AppItem> systemappList = new ArrayList<>();
        try {
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

            for (PackageInfo packageInfo : packageInfos) {
//                PackageInfo packageInfo = packageInfos.get(i);
                if (packageInfo.versionName == null) {
                    continue;
                }
                ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                AppItem appItem = new AppItem();
                appItem.setAppName(applicationInfo.loadLabel(packageManager).toString());
                appItem.setPackageName(packageInfo.packageName);
                appItem.setLocation("tv");
                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                    systemappList.add(appItem);
                } else {
                    myappList.add(appItem);
                }
                if (appItem.getPackageName().equals("com.microsoft.rdc.android")) {
                    BaseApplication.setRdpPackageName("com.microsoft.rdc.android");
                    BaseApplication.FLAG_RDP=true;
                }
                if (appItem.getPackageName().equals("com.bsplayer.bspandroid.full")) {
                    BaseApplication.FLAG_BS=true;
                }
                if (appItem.getPackageName().equals("com.xiaomi.mitv.smartshare")) {
                    BaseApplication.setMiracastPackageName("com.xiaomi.mitv.smartshare");
                }

            }

        } catch (Exception e) {
            Log.e("AppUtil", "sacnInstallApp failure!");
            e.printStackTrace();
        }
        Map<String,List<AppItem>> map = new HashMap<String, List<AppItem>>();
        map.put("MyApp", myappList);
        map.put("SystemApp", systemappList);
        Log.e("scanapp", "scancomplete");
        return map;
    }

    public static int executeCmd(String cmd){
        try{
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(process.getOutputStream());
            out.writeBytes(cmd + "\n");
            out.flush();
            out.writeBytes("exit\n");
            out.flush();
            out.close();
            process.waitFor();
            int result = process.exitValue();
            return  result;
        }
        catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }


    public static Boolean doStartApplicationWithPackageName(String packagename) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        Context mContext = BaseApplication.getContext();
        PackageInfo packageinfo = null;
        try {
            packageinfo = mContext.getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return false;
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);

        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = mContext.getPackageManager()
                .queryIntentActivities(resolveIntent, 0);

        ResolveInfo resolveinfo;
        if (resolveinfoList.iterator().hasNext()) {
            resolveinfo = resolveinfoList.iterator().next();
        } else {
            return false;
        }
        // packagename = 参数packname
        String packageName = resolveinfo.activityInfo.packageName;
        // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
        String className = resolveinfo.activityInfo.name;
        // LAUNCHER Intent
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 设置ComponentName参数1:packagename参数2:MainActivity路径
        ComponentName cn = new ComponentName(packageName, className);

        intent.setComponent(cn);
        mContext.startActivity(intent);
        return true;
    }

    //以下方法接收一个路径名和媒体类型，返回该路径下的List<MediaItem>实例。如果不是文件夹，则返回null.
    public static List<MediaItem> getChildrenMedias(String pathname,String mime) {
        Log.e("In getChildrenMedias", "path is :" + pathname);
        Log.e("In getChildrenMedias", "mime is :" + mime);
        List<MediaItem> childrenMedias = new ArrayList<>();
        try {
            File file = new File(pathname);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                Log.e("In getChildrenMedias", "file length is:" + files.length);
                for (File fileItem : files) {
                    MediaItem item = null;
                    if (fileItem.isDirectory()) {
                        item = new MediaItem();
                        item.setName(fileItem.getName());
                        item.setLocation("tv");
                        item.setPathName(fileItem.getCanonicalPath());
                        item.setType(mime);
                        item.setIsFolder(true);
                    } else {
                        String fileSuffix = FileUtil.getFileSuffix(fileItem.getName());
                        if (fileSuffix != null) {
                            String verifyType = MediaUtil.verifyMediaTypes(fileSuffix);
                            //TODO:逻辑是否正确？
                            if ( fileItem.isFile() && !mime.equals("*") && !verifyType.equals(mime)) {
                                continue;
                            }

                            item = new MediaItem();
                            item.setName(fileItem.getName());
                            item.setLocation("tv");
                            item.setPathName(fileItem.getCanonicalPath());
                            item.setType(mime);
                            item.setIsFolder(false);

                            if (verifyType.equals("video") || verifyType.equals("image")) {
                                //TODO:是否要加上http头？
                                item.setThumbnailurl("http://" + BaseApplication.LOCAL_IP+":"+BaseApplication.thumbnailPort+"/" +
                                        fileItem.length() + fileItem.getName().substring(0, fileItem.getName().lastIndexOf(".")) + ".png");
                            }

                        }
                    }
                    if (item != null) {
                        childrenMedias.add(item);
                    }
                }

            }else {
                Log.e(tag, "Not a Folder !!!");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return childrenMedias;
    }

//    //以下方法接收一个文件路径名，然后输出一个缩略图
//    public static Bitmap getVideoThumbnail(String path, int width, int height) {
//        Bitmap bitmap = null;
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        if (TextUtils.isEmpty(path)) {
//            return null;
//        }
//
//        File file = new File(path);
//        if (!file.exists()) {
//            return null;
//        }
//
//        try {
//            retriever.setDataSource(path);
//            bitmap = retriever.getFrameAtTime(-1); //取得指定时间的Bitmap，即可以实现抓图（缩略图）功能
//        } catch (IllegalArgumentException ex) {
//            // Assume this is a corrupt video file
//            ex.printStackTrace();
//        } finally {
//            try {
//                retriever.release();
//            } catch (RuntimeException ex) {
//                // Ignore failures while cleaning up.
//            }
//        }
//
//        if (bitmap == null) {
//            return null;
//        }
//
//        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
//        return bitmap;
//    }
//
//    public static Bitmap getImageThumbnail(String path, int width, int height) {
//
//    }
//





}
