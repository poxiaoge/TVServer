package com.example.poxiaoge.tvserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    private final String tag = "MyReceiver";
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction().equals(Action.START_DMR_SERVICE)) {
            Log.e("MyReceiver", "restart dmr service……");
            Intent i = new Intent(context, DMRService.class);
            context.startService(i);
//        }
//        if (intent.getAction().equals(Action.START_COMMAND_SERVICE)) {
//            Log.e(tag, "restart command service");
//            Intent i = new Intent(context, CommandService.class);
//            context.startService(i);
//        }
    }
}
