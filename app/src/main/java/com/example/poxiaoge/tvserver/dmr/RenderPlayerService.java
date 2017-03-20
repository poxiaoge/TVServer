/*
 * RenderPlayerService.java
 * Description:
 * Author: zxt
 */

package com.example.poxiaoge.tvserver.dmr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.example.poxiaoge.tvserver.dmp.GPlayer;
import com.example.poxiaoge.tvserver.dmp.ImageDisplay;
import com.example.poxiaoge.tvserver.utils.Action;

public class RenderPlayerService extends Service {

	private final String tag = "RenderPlayerService";

	public IBinder onBind(Intent intent) {
		return null;
	}

	public int onStartCommand(Intent intent, int flags,int startId) {
		//xgf fix bug null point
		if (null != intent) {
			super.onStartCommand(intent,flags, startId);
			String type = intent.getStringExtra("type");
			Intent intent2;
			String packageName;
			String className;

			switch (type) {
				case ("video"):
					Log.e(tag, "play video……");
//					break;
				case ("audio"):
					Log.e(tag, "play audio……");
					intent2 = new Intent();
					intent2.setAction("android.intent.action.VIEW");
					intent2.addCategory("android.intent.category.DEFAULT");
					intent2.addCategory("android.intent.category.BROWSABLE");
					intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					Uri uri = Uri.parse(intent.getStringExtra("playURI"));
					intent2.setDataAndType(uri, type);
					packageName = "com.bsplayer.bspandroid.full";
					className = "com.bsplayer.bsplayeran.CmdParse";
					ComponentName cn = new ComponentName(packageName,className);
					intent2.setComponent(cn);
					startActivity(intent2);
					break;
				case ("image"):
					intent2 = new Intent(this, ImageDisplay.class);
					intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent2.putExtra("name", intent.getStringExtra("name"));
					intent2.putExtra("playURI", intent.getStringExtra("playURI"));
					intent2.putExtra("isRender", true);

					packageName=intent2.getPackage();
					className = intent2.getClass().toString();
					Log.e(tag, "package name is:" + packageName + "\n" + "class name is:" + className);
					startActivity(intent2);
					break;
				default:
					intent2 = new Intent(Action.DMR);
					intent2.putExtra("playpath", intent.getStringExtra("playURI"));
					sendBroadcast(intent2);
			}







//
//			if (type.equals("audio")) {
////				intent2 = new Intent(this, GPlayer.class);
////				intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////				intent2.putExtra("name", intent.getStringExtra("name"));
////				intent2.putExtra("playURI", intent.getStringExtra("playURI"));
////				startActivity(intent2);
//
//
//
//
//			} else if (type.equals("video")) {
////				intent2 = new Intent(this, GPlayer.class);
////				intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////				intent2.putExtra("name", intent.getStringExtra("name"));
////				intent2.putExtra("playURI", intent.getStringExtra("playURI"));
////				startActivity(intent2);
//
//				//TODO:当用户没有安装BSplayer的时候，自动打开Gplayer
//				intent2 = new Intent();
//				intent2.setAction("android.intent.action.VIEW");
//				intent2.addCategory("android.intent.category.DEFAULT");
//				intent2.addCategory("android.intent.category.BROWSABLE");
//				intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//				Uri uri = Uri.parse(intent.getStringExtra("playURI"));
//				intent2.setDataAndType(uri, type);
//				String packageName = "com.bsplayer.bspandroid.full";
//				String className = "com.bsplayer.bsplayeran.CmdParse";
//				ComponentName cn = new ComponentName(packageName,className);
//				intent2.setComponent(cn);
//				startActivity(intent2);
//
//			} else if (type.equals("image")) {
//				String packageName;
//				String className;
//				intent2 = new Intent(this, ImageDisplay.class);
//				intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				intent2.putExtra("name", intent.getStringExtra("name"));
//				intent2.putExtra("playURI", intent.getStringExtra("playURI"));
//				intent2.putExtra("isRender", true);
//
//				packageName=intent2.getPackage();
//				className = intent2.getClass().toString();
//				Log.e(tag, "package name is:" + packageName + "\n" + "class name is:" + className);
//				startActivity(intent2);
//			} else {
//				intent2 = new Intent(Action.DMR);
//				intent2.putExtra("playpath", intent.getStringExtra("playURI"));
//				sendBroadcast(intent2);
//			}
		}

		return START_STICKY;
	}
}
