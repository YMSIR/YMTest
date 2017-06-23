package com.zx.ym.ymclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by zhangxinwei02 on 2017/6/23.
 */

public class YMMsgReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PackageManager manager = context.getPackageManager();
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            String result = "安装成功"+packageName;
            MainActivity.instance.sendOperatorResult(result);
        }
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            String result = "卸载成功"+packageName;
            MainActivity.instance.sendOperatorResult(result);
        }
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            String result = "替换成功"+packageName;
            MainActivity.instance.sendOperatorResult(result);
        }
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
            Log.i("recv", "onReceive: " + intent.getDataString() + " " + intent.getScheme());
        }
    }

}