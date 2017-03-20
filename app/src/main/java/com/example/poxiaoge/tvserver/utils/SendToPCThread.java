package com.example.poxiaoge.tvserver.utils;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * Created by poxiaoge on 2016/12/23.
 */

public class SendToPCThread extends Thread{
    private String cmd;
    private String ip;
    private int port;
    private final String tag = "SendThread";

    public SendToPCThread(String cmd, String ip, int port){
        this.cmd = cmd;
        this.ip = ip;
        this.port =port;
    }

    @Override
    public void run() {
//        Log.e(tag, "Begin run");
        Socket client=new Socket();
        DataOutputStream out;
        String data;
        try {
            Log.e(tag, "begin socket");
            client.connect(new InetSocketAddress(ip, port), 5000);
            Log.e(tag, "before write");
            IOUtils.write(cmd, client.getOutputStream(), "UTF-8");
            Log.e(tag, "Out success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            IOUtils.closeQuietly(client);
        }
    }
}
