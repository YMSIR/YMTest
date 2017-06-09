package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/6/9.
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.util.Log;


public class YMService extends Service {

    public static YMService instance = null;
    public final static String BROADCAST_ACTION = "com.ym.service";
    private YMNetWorker _netWorker;
    private YMTimer _handler;
    private String _ip;
    private int _port;

    public String getServerIP()
    {
        return _ip;
    }
    public int getServerPort()
    {
        return _port;
    }

    public boolean isConnected()
    {
        return _netWorker.isConnected();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null)
        {
            _ip = intent.getStringExtra("ip");
            _port = intent.getIntExtra("port", 0);
            start();
        }
        return START_STICKY;
    }

    private void start()
    {
        initNetWorker();
        initTimerHandler();
        startForeground();
    }

    private void startForeground()
    {
        //Intent intent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent , 0);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("YMClient正在运行中");// 设置通知的标题
        builder.setContentText("(＾－＾)点击打开应用");// 设置通知的内容
        builder.setOngoing(true);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL; //设置为点击后自动取消
        startForeground(10086, notification);//该方法已创建通知管理器，设置为前台优先级后，点击通知不再自
    }


    // 初始化定时器
    private void initTimerHandler()
    {
        _handler = new YMTimer();
        _handler.setOnUpdate(new YMTimer.OnUpdate() {
            @Override
            public void update() {
                YMService.this.update();
            }
        });
        _handler.start(0, 50);
    }

    // 启动网络连接
    private void initNetWorker()
    {
        _netWorker = new YMNetWorker(this);
        _netWorker.start(_ip, _port);
    }


    private void update()
    {
        if (_netWorker != null)
        {
           _netWorker.update();
        }
    }

    // 发送消息
    public void sendMessage(YMMessage msg)
    {
        if (_netWorker != null)
        {
            _netWorker.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_netWorker != null)
        {
            _netWorker.quit();
            _netWorker = null;
        }
        if (_handler != null)
        {
            _handler.stop();
            _handler = null;
        }
        instance = null;
        stopForeground(true);
    }

}
