package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */


import android.app.Service;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.Date;
import java.text.SimpleDateFormat;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class MainActivity extends Activity {

    public static MainActivity instance;
    private YMTimer _handler;
    private YMDispatcher _dispatcher;
    private YMTaskManager _taskManager;
    private View _view_main;
    private View _view_loading;
	private TextView _textView_log;
    private TextView _textView_serverIP;
    private TextView _textView_clientIP;
    private TextView _textView_serverPort;
    private TextView _textView_curTask;
    private TextView _textView_connState;
    private ProgressBar _progressBar_loading;
    private ProgressBar _progressBar_task;
    private TextView _textView_loadtip;
    private final static String _serverInfoURL = "http://code.taobao.org/svn/YMFile/serverinfo.txt";
    private final static String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
    private YMMsgReceiver _msgReceiver;
    private String _ip;
    private int _port;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        initUIWidgets();
        showLoadingUI("");
        boolean has = checkPermissions();
        if (has)
        {
            start();
        }
    }

    private void start()
    {
        initTimerHandler();
        initBindListener();
        initNetWorker();
        initMsgReceiver();
    }


    private boolean checkPermissions()
    {
        boolean isHasPermission = true;
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            for (int i = 0; i < permissions.length; ++i)
            {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                if (result != PackageManager.PERMISSION_GRANTED)
                {
                    isHasPermission = false;
                    break;
                }
            }
        }
        if (!isHasPermission)
        {
            ActivityCompat.requestPermissions(this, permissions, 10086);
        }
        return isHasPermission;
    }

    // 用户权限 申请 的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10086)
        {
            boolean isHasPermissions = checkPermissions();
            if (!isHasPermissions)
            {
                _textView_loadtip.setText("获取权限失败,请手动设置");
            }
            else
            {
                start();
            }
        }
    }

    // 初始化广播接受
    private void initMsgReceiver()
    {
        _msgReceiver = new YMMsgReceiver();
        IntentFilter intent = new IntentFilter();
        intent.addDataScheme("package");
        intent.addAction(Intent.ACTION_PACKAGE_ADDED);
        intent.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intent.addAction(Intent.ACTION_PACKAGE_REPLACED);
        registerReceiver(_msgReceiver, intent);
    }

    // 初始化网络
    private void initNetWorker()
    {
        _textView_loadtip.setText("正在获取服务器地址...");
        _taskManager = new YMTaskManager(this);
        _taskManager.start();
        sendDownLoadServerInfoTask();
    }

    // 监听事件
    private void initBindListener()
    {
        _dispatcher = new YMDispatcher();
        _dispatcher.addListener(YMEvent.ID_GetServerInfoSuccess, new YMEvent.OnListener() {
            @Override
            public void onEvent(YMEvent event) {
                on_GetServerInfoSuccessEvent(event);
            }
        });
        _dispatcher.addListener(YMEvent.ID_UpdateUI, new YMEvent.OnListener() {
            @Override
            public void onEvent(YMEvent event) {

                on_UpdateUIEvent(event);
            }
        });
        _dispatcher.addListener(YMEvent.ID_Log, new YMEvent.OnListener() {
            @Override
            public void onEvent(YMEvent event) {

                on_LogEvent(event);
            }
        });
    }

    // 初始化定时器
    private void initTimerHandler()
    {
        _handler = new YMTimer();
        _handler.setOnUpdate(new YMTimer.OnUpdate() {
            @Override
            public void update() {
                instance.update();
            }
        });
        _handler.start(0, 50);
    }

    // 服务器地址
    private void onServerInfo(YMTask task)
    {
        try
        {
            if (task.result != "")
            {
                JSONObject obj = new JSONObject(task.result);
                _ip = obj.getString("ip");
                _port = obj.getInt("port");
                sendGetServerInfoSuccessEvent();
                YMUtil.log("IP地址:" + _ip + " 端口号:" + _port);
            }
            else {
                YMUtil.log("download serverinfo failed");
                sendDownLoadServerInfoTask();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // 初始化UI
    private void initUIWidgets()
    {
        _view_main = findViewById(R.id.view_main);
        _view_loading = findViewById(R.id.view_loading);
        _textView_log =(TextView) findViewById(R.id.textView_log);
        _textView_log.setMovementMethod(ScrollingMovementMethod.getInstance());
        _textView_log.setText("");
        _textView_serverIP =(TextView) findViewById(R.id.textView_serverip);
        _textView_serverPort =(TextView) findViewById(R.id.textView_serverport);
        _textView_clientIP =(TextView) findViewById(R.id.textView_clientip);
        _textView_connState =(TextView) findViewById(R.id.textView_connstate);
        _textView_curTask =(TextView) findViewById(R.id.textView_curtask);
        _progressBar_loading =(ProgressBar) findViewById(R.id.progressBar_loading);
        _progressBar_task =(ProgressBar) findViewById(R.id.progressBar_task);
        _textView_loadtip = (TextView) findViewById(R.id.textView_tip);
        testButtons();
    }

    @Override
     protected void onDestroy() {
        super.onDestroy();

        quit();
    }

    public void quit()
    {
        unregisterReceiver(_msgReceiver);
        stopService();
    }


    // 启动Service
    private void startService()
    {
        Intent serviceIntent = new Intent(MainActivity.this, YMService.class);
        serviceIntent.putExtra("ip",_ip);
        serviceIntent.putExtra("port", _port);
        startService(serviceIntent);
    }

    // 关闭Service
    private void stopService()
    {
        Intent serviceIntent = new Intent(MainActivity.this, YMService.class);
        stopService(serviceIntent);
    }

    public void sendDownLoadServerInfoTask()
    {
        YMTask task = new YMTask(YMTaskType.DownLoadString);
        task.mainName = _serverInfoURL;
        task.geneDescString();
        task.finishListener = new YMTask.OnFinishListener()
        {
            @Override
            public void onFinish(YMTask task)
            {
                onServerInfo(task);
            }
        };
        _taskManager.addTask(task);
    }

    // 发送任务
    public void sendTask(YMTaskType taskType, String mainName)
    {
        YMTask task = new YMTask(taskType);
        task.mainName = mainName;
        task.geneDescString();
        _taskManager.addTask(task);
    }

    // 测试
    private void testButtons()
    {
        View view = findViewById(R.id.Layout_test);
        view.setVisibility(View.GONE);
        final String testApp = "com.test.ymclient";
        final String nameApp = "YMClient.apk";
        final String urlAPK = "http://dldir1.qq.com/android/weizhuan/qq.hlwg_v1.10.apk";
        Button install = (Button)findViewById(R.id.button_install);
        install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setData(Uri.parse("package:com.ym.client"));
                intent.putExtra("id", "qqyumidi");
                sendBroadcast(intent);
            }
        });
        Button uninstall = (Button)findViewById(R.id.button_uninstall);
        uninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.UninstallAPP);
                task.mainName = testApp;
                task.geneDescString();
                _taskManager.addTask(task);
            }
        });
        Button down = (Button)findViewById(R.id.button_down);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.DownLoadFile);
                task.mainName = urlAPK;
                task.geneDescString();
                _taskManager.addTask(task);
            }
        });
        Button start = (Button)findViewById(R.id.button_start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.CloseAPP);
                task.mainName = testApp;
                task.geneDescString();
                _taskManager.addTask(task);
            }
        });
    }

    // 显示Loading
    private void showLoadingUI(String tip)
    {
        _textView_loadtip.setText(tip);
        _view_loading.setVisibility(View.VISIBLE);
        _view_main.setVisibility(View.INVISIBLE);
    }

    // 显示主信息
    private void showMainUI()
    {
        _view_loading.setVisibility(View.INVISIBLE);
        _view_main.setVisibility(View.VISIBLE);
        this.updateUIServerInfo();
        this.updateUIState();
    }

    // 日志输出
    public void log(String str)
    {
        if(_textView_log != null)
        {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            String date = df.format(new Date());
            str = date + ":" + str + "\n";
            _textView_log.append(str);
            int offset=_textView_log.getLineCount()*_textView_log.getLineHeight();
            if(offset>_textView_log.getHeight()){
                _textView_log.scrollTo(0,offset-_textView_log.getHeight());
            }
        }
    }

    // 发送LOG事件
    public void sendLogEvent(String msg)
    {
        YMEvent ymEvent = new YMEvent(YMEvent.ID_Log);
        ymEvent.addAttr("log",msg);
        _dispatcher.dispatchInMainThread(ymEvent);
    }

    // 发送UpdateUI事件
    public void sendUpdateUIEvent()
    {
        YMEvent ymEvent = new YMEvent(YMEvent.ID_UpdateUI);
        _dispatcher.dispatchInMainThread(ymEvent);
    }

    // 发送GetServerInfoSuccess事件
    public void sendGetServerInfoSuccessEvent()
    {
        YMEvent ymEvent = new YMEvent(YMEvent.ID_GetServerInfoSuccess);
        _dispatcher.dispatchInMainThread(ymEvent);
    }

    // 发送操作结果
    public void sendOperatorResult(String result)
    {
        String msg = YMMessage.Make_C_OperatorResult(result);
        YMService.instance.sendMessage(new YMMessage(msg));
        YMUtil.log(result);
    }

    // 刷新
    public void update()
    {
        if(_dispatcher != null)
        {
            _dispatcher.update();
        }
        this.updateUIState();
    }

    // 刷新服务器信息
    private void updateUIServerInfo()
    {
        _textView_serverIP.setText(_ip);
        _textView_serverPort.setText(String.valueOf(_port));
        _textView_clientIP.setText(YMUtil.getIPAddress(this));
    }

    // 刷新UI
    private void updateUIState()
    {
        boolean isConnected = false;
        if(YMService.instance != null)
        {
            isConnected = YMService.instance.isConnected();
        }
        if (isConnected)
        {
            _textView_connState.setText("已连接");
            _progressBar_loading.setVisibility(View.INVISIBLE);
        }
        else
        {
            _textView_connState.setText("连接中");
            _progressBar_loading.setVisibility(View.VISIBLE);
        }

        YMTask task = _taskManager.getCurTask();
        if (task != null)
        {
            _textView_curTask.setText(task.desc);
            _progressBar_task.setVisibility(View.VISIBLE);
            _progressBar_task.setProgress(task.progress);
        }
        else
        {
            _textView_curTask.setText("空");
            _progressBar_task.setVisibility(View.VISIBLE);
            _progressBar_task.setProgress(0);
        }
    }

    private void on_LogEvent(YMEvent event)
    {
        log((String) event.getAttr("log"));
    }

    private void on_GetServerInfoSuccessEvent(YMEvent event)
    {
        startService();
        showMainUI();
        updateUIServerInfo();
    }

    private void on_UpdateUIEvent(YMEvent event)
    {
        updateUIServerInfo();
        updateUIState();
    }


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
        }

    }

}
