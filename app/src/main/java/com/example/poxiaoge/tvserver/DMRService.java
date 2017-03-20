package com.example.poxiaoge.tvserver;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.example.poxiaoge.tvserver.application.BaseApplication;
import com.example.poxiaoge.tvserver.dmr.RenderPlayerService;
import com.example.poxiaoge.tvserver.dmr.ZxtMediaRenderer;
import com.example.poxiaoge.tvserver.model.CommandItem;
import com.example.poxiaoge.tvserver.model.ConnectItem;
import com.example.poxiaoge.tvserver.model.DeviceItem;
import com.example.poxiaoge.tvserver.utils.Action;
import com.example.poxiaoge.tvserver.utils.AppUtil;
import com.example.poxiaoge.tvserver.utils.FileUtil;
import com.example.poxiaoge.tvserver.utils.MediaUtil;
import com.example.poxiaoge.tvserver.utils.SendToPCThread;

import org.apache.commons.io.IOUtils;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.LocalDevice;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMRService extends Service {
    public DMRService() {
    }

    private String lastMethod;

    private final String tag = "DMRService";
    private final static String LOGTAG = "DMRService";

    public static final int DMR_GET_NO = 0;

    public static final int DMR_GET_SUC = 1;
    private LocalDevice localDevice;
    private long exitTime = 0;

    private AndroidUpnpService upnpService;

    WifiManager.MulticastLock multicastLock;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            String dmrName = "TVServerMediaRenderer" + " (" + android.os.Build.MODEL + ")";
            Log.e(tag, "dmrName is : " + dmrName);
            BaseApplication.setDMRName(dmrName);
            BaseApplication.setImage_slide_time(5);
            upnpService = (AndroidUpnpService) service;
            BaseApplication.upnpService = upnpService;
            Log.e(LOGTAG, "Connected to UPnP Service");

            ZxtMediaRenderer mediaRenderer = new ZxtMediaRenderer(1,
                    DMRService.this);
            upnpService.getRegistry().addDevice(mediaRenderer.getDevice());


            //TODO:pushDeviceMsgToPC应该放到哪里？
            pushDeviceMsgToPC();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Notification.Builder localBuilder = new Notification.Builder(this);
        localBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        localBuilder.setAutoCancel(false);
        localBuilder.setSmallIcon(R.drawable.ic_launcher);
        localBuilder.setTicker("Foreground Service Start");
        localBuilder.setContentTitle("TVServer服务端");
        localBuilder.setContentText("正在运行...");
        startForeground(1, localBuilder.getNotification());

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast.test");
        if (multicastLock != null){
            multicastLock.acquire();
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
//        new ServerThread(BaseApplication.MOBILE_BROAD_PORT).start();


//TODO:测试完后要把下面这一行的注释消掉！！！
//        new BroadThread(BaseApplication.MOBILE_BROAD_PORT).start();
        new ServerThread(BaseApplication.LOCAL_PORT).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(tag, "onDestroy");
        getApplicationContext().unbindService(serviceConnection);

        if (multicastLock != null){
            multicastLock.release();
            multicastLock = null;
        }

        stopForeground(true);
        //TODO:试试完全退出会不会解决dlna的问题
        Intent intent = new Intent(Action.START_DMR_SERVICE);
        sendBroadcast(intent);
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public class BroadThread extends Thread{
        private int port;

        public BroadThread(int port){
            this.port = port;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
        /*在这里同样使用约定好的端口*/
            DatagramSocket server = null;


            try {
                server = new DatagramSocket (port);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                Log.e(tag, "Begin UDP Broad Server on port " + port);
                while(true){
                    try {
//                        Log.e(tag, "Before receive");
                        server.receive(packet);
                        Log.e(tag,"connected by Mobile ip: "+packet.getAddress().getHostAddress());
                        String s = new String(packet.getData(), 0, packet.getLength(), "UTF-8");


                        Log.e(tag, "receive from mobile broad: " + s);
                        ConnectItem connectItem = JSON.parseObject(s, ConnectItem.class);
                        for (Map<String, String> map : connectItem.getParam()) {
                            BaseApplication.MOBILE_IP = map.get("ip");
                            BaseApplication.MOBILE_PORT = Integer.parseInt(map.get("port"));
                        }

                        Log.e(tag, "received mobile ip: " + BaseApplication.MOBILE_IP);

                        ConnectItem responseItem = new ConnectItem();
                        responseItem.setSource("TV");
                        responseItem.setType("default");
                        HashMap<String, String> map = new HashMap<>();
                        map.put("ip", BaseApplication.LOCAL_IP);
                        map.put("port", String.valueOf(BaseApplication.LOCAL_PORT));
                        map.put("function", "pctomobile");
                        List<Map<String, String>> listParam = new ArrayList<>();
                        listParam.add(map);
                        responseItem.setParam(listParam);
                        String responseString = JSON.toJSONString(responseItem);
                        new SendToPCThread(responseString, BaseApplication.MOBILE_IP, BaseApplication.MOBILE_PORT).start();
                        Log.e("responseToMobileBroad", responseString);





//                        System.out.println("address : " + packet.getAddress() + ", port : " + packet.getPort() + ", content : " + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }finally{
                multicastLock.release();
                if(server != null)
                    server.close();
            }
        }
    }

    public class ServerThread extends Thread {
        private int port;

        public ServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            Log.e(tag, "Begin normal Server Thread on "+port);
            ServerSocket server = null;
//            DataOutputStream out;
//            DataInputStream in;
//            String data;
//            String outString="null";
//            ByteArrayOutputStream bytebuffer = new ByteArrayOutputStream();
            try {
                server = new ServerSocket(port);
                Socket client;
                while (true) {
//                    Log.e(tag, "before accept");
                    client = server.accept();
                    Log.e(tag, "connected by " + client.getRemoteSocketAddress().toString());
                    new SocketHandleThread(client).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (server != null) {
                    IOUtils.closeQuietly(server);
                }
            }
        }
    }


    public class SocketHandleThread extends Thread {
        Socket client;
        DataInputStream in = null;
        String data;
        String outString = "null";

        public SocketHandleThread(Socket socket) {
            this.client = socket;
        }

        @Override
        public void run() {
            try {
//                Log.e(tag, "abcde");
                in = new DataInputStream(client.getInputStream());
                data = in.readUTF();
//                    IOUtils.copy(client.getInputStream(), bytebuffer, 8192);
//                    data = new String(bytebuffer.toByteArray(), "UTF-8");
                Log.e(tag, "Get data from mobile: " + data);
//                if (type.equals("cmd")) {
                    if (!data.contains("source")) {
                        Log.e(tag, "Execute cmd from mobile");
                        CommandItem commandItem = JSON.parseObject(data, CommandItem.class);
                        switch (commandItem.getFirstcommand()) {
                            case ("GET_TV_APPS"):
                                if (BaseApplication.getAppOK()) {
                                    outString = JSON.toJSONString(BaseApplication.getAppList());
                                    Log.e(tag, outString);
                                    IOUtils.write(outString, client.getOutputStream(), "UTF-8");
                                }
                                break;
                            case ("OPEN_TV_APPS"):
                                if (BaseApplication.getAppOK()) {
                                    String packageName = commandItem.getSecondcommand();
                                    outString = "Begin open this app";
                                    AppUtil.doStartApplicationWithPackageName(packageName);
                                }
                                break;
//                            case ("GET_TV_VIDEOS"):
//                                if (BaseApplication.videoOk) {
//                                    outString = JSON.toJSONString(BaseApplication.getVideoList());
//                                }
//                                break;
//                            case ("GET_TV_AUDIOS"):
//                                if (BaseApplication.audioOk) {
//                                    outString = JSON.toJSONString(BaseApplication.getAudioList());
//                                }
//                                break;
//                            case ("GET_TV_IMAGES"):
//                                if (BaseApplication.imageOk) {
//                                    outString = JSON.toJSONString(BaseApplication.getImageList());
//                                }
//                                break;
                            case ("GET_TV_MEDIAS"):
                                if (BaseApplication.getMediaOk()) {
                                    outString = JSON.toJSONString(BaseApplication.getMediaMap());
                                    Log.e(tag, outString);
                                    IOUtils.write(outString, client.getOutputStream(), "UTF-8");
                                }
                                break;
                            case ("OPEN_TV_MEDIAS"):
                                if (BaseApplication.getMediaOk()) {
                                    String MediaPath = commandItem.getSecondcommand();
                                    File f = new File(MediaPath);
                                    Intent i = new Intent(DMRService.this, RenderPlayerService.class);
                                    i.putExtra("type", MediaUtil.verifyMediaTypes(FileUtil.getFileSuffix(MediaPath)));
                                    i.putExtra("name", f.getName());
                                    i.putExtra("playURI", URLDecoder.decode(Uri.fromFile(f).toString(), "UTF-8"));
                                    Log.e("playURI", URLDecoder.decode(Uri.fromFile(f).toString(), "UTF-8"));
                                    Log.e("name", f.getName());
                                    Log.e("type", MediaUtil.verifyMediaTypes(FileUtil.getFileSuffix(MediaPath)));
                                    startService(i);
                                    outString = "OPEN TV MEDIAS SUCCESS";
                                }
                                break;

                            case ("OPEN_RDP"):
        //                        BaseApplication.FLAG_RDP = true;
        //                        Intent i = new Intent(DMRService.this, MainActivity.class);
                                AppUtil.doStartApplicationWithPackageName(BaseApplication.getRdpPackageName());
        //                        AppUtil.executeCmd("input tap 500 500");
        //                        Toast.makeText(getApplicationContext(),"测试后台命令!!!!!!!!!",Toast.LENGTH_LONG).show();
                                Log.e(tag, "open rdp success!");
                                break;

                            case ("OPEN_MIRACAST"):
                                AppUtil.doStartApplicationWithPackageName(BaseApplication.getMiracastPackageName());
                                Log.e(tag, "open miracast success");
                                break;

                            case ("video"):
                                if (commandItem.getThirdcommand()) {
                                    Log.e("case video", "return rootVideo");
                                    outString = JSON.toJSONString(BaseApplication.rootVideoFolders);
                                }else{
                                    Log.e("case video", "return normal");
                                    outString = JSON.toJSONString(AppUtil.getChildrenMedias(commandItem.getSecondcommand(), "video"));
                                }
                                Log.e(tag, "data to mobile is :"+ outString);
                                IOUtils.write(outString, client.getOutputStream(), "UTF-8");
                                break;
                            case ("audio"):
                                if (commandItem.getThirdcommand()) {
                                    outString = JSON.toJSONString(BaseApplication.rootAudioFolders);
                                }else{
                                    outString = JSON.toJSONString(AppUtil.getChildrenMedias(commandItem.getSecondcommand(), "audio"));
                                }
                                Log.e(tag, outString);
                                IOUtils.write(outString, client.getOutputStream(), "UTF-8");
                                break;
                            case ("image"):
                                if (commandItem.getThirdcommand()) {
                                    outString = JSON.toJSONString(BaseApplication.rootImageFolders);
                                }else{
                                    outString = JSON.toJSONString(AppUtil.getChildrenMedias(commandItem.getSecondcommand(), "image"));
                                }
                                Log.e(tag, outString);
                                IOUtils.write(outString, client.getOutputStream(), "UTF-8");
                                break;

                            default:
                                Log.e(tag, "Use default！");
                                outString = "null";
                        }
                        Log.e(tag, "Data to mobile successfully!before close socket");
                    } else {
                        Log.e(tag, "Get PC Msg from Mobile");
                        ConnectItem pcMsgItem =  JSON.parseObject(data, ConnectItem.class);
                        for (Map<String, String> map : pcMsgItem.getParam()) {
                            BaseApplication.PC_IP = map.get("ip");
                            BaseApplication.PC_PORT = Integer.parseInt(map.get("port"));
                        }
                        //TODO:放的位置是否正确？
                        if (BaseApplication.getMediaOk()) {
                            pushDeviceMsgToPC();
                        }
                    }
//                } else {
//                    ConnectItem connectItem = JSON.parseObject(data, ConnectItem.class);
//                    for (Map<String, String> map : connectItem.getParam()) {
//                        BaseApplication.LOCAL_IP = map.get("ip");
//                        BaseApplication.LOCAL_PORT = Integer.parseInt(map.get("port"));
//                    }
//                    ConnectItem responseItem = new ConnectItem();
//                    responseItem.setSource("TV");
//                    responseItem.setType("default");
//                    HashMap<String, String> map = new HashMap<>();
//                    map.put("ip", BaseApplication.LOCAL_IP);
//                    map.put("port", String.valueOf(BaseApplication.LOCAL_PORT));
//                    map.put("function", "pctomobile");
//                    List<Map<String, String>> listParam = new ArrayList<>();
//                    listParam.add(map);
//                    responseItem.setParam(listParam);
//                    String responseString = JSON.toJSONString(responseItem);
//                    new SendToPCThread(responseString, BaseApplication.MOBILE_IP, BaseApplication.MOBILE_PORT);
//                    Log.e("responseString", responseString);
//                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    IOUtils.closeQuietly(in);
                }
                if (!client.isClosed()) {
                    IOUtils.closeQuietly(client);
                }
            }
        }
    }


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






}
