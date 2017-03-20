package com.example.poxiaoge.tvserver;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.util.IOUtils;
import com.example.poxiaoge.tvserver.application.BaseApplication;
import com.example.poxiaoge.tvserver.dmr.RenderPlayerService;
import com.example.poxiaoge.tvserver.model.CommandItem;
import com.example.poxiaoge.tvserver.utils.Action;
import com.example.poxiaoge.tvserver.utils.AppUtil;
import com.example.poxiaoge.tvserver.utils.FileUtil;
import com.example.poxiaoge.tvserver.utils.MediaUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;

import org.apache.commons.io.IOUtils;

public class CommandService extends Service {
    private static final String tag = "CommandService";




    public CommandService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        new ServerThread(BaseApplication.MOBILE_In_PORT).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(Action.START_DMR_SERVICE);
        sendBroadcast(intent);
        super.onDestroy();
    }

    public class ServerThread extends Thread {
        private int port;
        public ServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            Log.e(tag, "Begin run");
            DataOutputStream out;
            DataInputStream in;
            String data;
            String outString="null";
            ByteArrayOutputStream bytebuffer = new ByteArrayOutputStream();
            try {
                ServerSocket server = new ServerSocket(port);
                Socket client;
                Log.e("@run", "before while(true)");
                while (true) {

                    client = server.accept();
                    Log.e(tag, "connected by " + client.getRemoteSocketAddress().toString());




//                    out = new DataOutputStream(client.getOutputStream());

                    in = new DataInputStream(client.getInputStream());
                    data = in.readUTF();
//                    IOUtils.copy(client.getInputStream(), bytebuffer, 8192);
//                    data = new String(bytebuffer.toByteArray(), "UTF-8");





                    Log.e(tag, "in data:" + data);
                    CommandItem commandItem = JSON.parseObject(data, CommandItem.class);
                    switch (commandItem.getFirstcommand()) {
                        case ("GET_TV_APPS"):
                            if (BaseApplication.getAppOK()) {
                                outString = JSON.toJSONString(BaseApplication.getAppList());
                            }
                            break;
                        case ("OPEN_TV_APPS"):
                            if (BaseApplication.getAppOK()) {
                                String packageName = commandItem.getSecondcommand();
                                outString = "Begin open this app";
                                AppUtil.doStartApplicationWithPackageName(packageName);
                            }
                            break;
                        case ("GET_TV_VIDEOS"):
                            if (BaseApplication.videoOk) {
                                outString = JSON.toJSONString(BaseApplication.getVideoList());
                            }
                            break;
                        case ("GET_TV_AUDIOS"):
                            if (BaseApplication.audioOk) {
                                outString = JSON.toJSONString(BaseApplication.getAudioList());
                            }
                            break;
                        case ("GET_TV_IMAGES"):
                            if (BaseApplication.imageOk) {
                                outString = JSON.toJSONString(BaseApplication.getImageList());
                            }
                            break;
                        case ("GET_TV_MEDIAS"):
                            if (BaseApplication.getMediaOk()) {
                                outString = JSON.toJSONString(BaseApplication.getMediaMap());
                            }
                            break;
                        case ("OPEN_TV_MEDIAS"):
                            if (BaseApplication.getMediaOk()) {
                                String MediaPath=commandItem.getSecondcommand();
                                File f = new File(MediaPath);
                                Intent i = new Intent(CommandService.this, RenderPlayerService.class);
                                i.putExtra("type", MediaUtil.verifyMediaTypes(FileUtil.getFileSuffix(MediaPath)));
                                i.putExtra("name", f.getName());
                                i.putExtra("playURI",URLDecoder.decode(Uri.fromFile(f).toString(), "UTF-8"));
                                Log.e("playURI", URLDecoder.decode(Uri.fromFile(f).toString(), "UTF-8"));
                                Log.e("name", f.getName());
                                Log.e("type", MediaUtil.verifyMediaTypes(FileUtil.getFileSuffix(MediaPath)));
                                startService(i);
                                outString = "OPEN TV MEDIAS SUCCESS";
                            }
                            break;
//                        case ("GET_PC_APPS"):
//                            break;
//                        case ("OPEN_PC_APPS"):
//                            break;
//                        case ("GET_PC_VIDEOS"):
//                            break;
//                        case ("GET_PC_AUDIOS"):
//                            break;
//                        case ("GET_PC_IMAGES"):
//                            break;
//                        case ("OPEN_PC_MEDIAS"):
//
                        default:
                            Log.e(tag, "Use default！");
                            outString = "null";
                    }
                    Log.e("CommandService",outString);
//                    out.writeUTF(outString);
                    IOUtils.write(outString, client.getOutputStream(), "UTF-8");
//                    Log.e(tag, "1");
//                    out.flush();
//                    Log.e(tag, "2");
//                    out.close();
//                    Log.e(tag, "3");
//                    in.close();
//                    Log.e(tag, "4");
//                    client.close();
                    Log.e(tag, "5");
                    IOUtils.closeQuietly(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }







}
