package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */


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



public class MainActivity extends Activity {

    public static MainActivity instance;
	private YMNetWorker _netWorker;
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
    private final static String _serverInfoURL = "http://code.taobao.org/svn/YMFile/serverinfo.txt";
    private final static String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
    private AlertDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        initUIWidgets();
        showLoadingUI();
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
            startRequestPermission();
        }
        return isHasPermission;
    }

    // 开始提交请求权限
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }

    // 用户权限 申请 的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        showDialogTipUserGoToAppSettting();
                    } else
                        finish();
                } else {
                    start();
                }
            }
        }
    }

    // 提示用户去应用设置界面手动开启权限

    private void showDialogTipUserGoToAppSettting() {

        dialog = new AlertDialog.Builder(this)
                .setTitle("存储权限不可用")
                .setMessage("请在-应用设置-权限-中，允许支付宝使用存储权限来保存用户数据")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);

        startActivityForResult(intent, 123);
    }

    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 检查该权限是否已经获取
                int i = ContextCompat.checkSelfPermission(this, permissions[0]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    showDialogTipUserGoToAppSettting();
                } else {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 初始化网络
    private void initNetWorker()
    {
        _taskManager = new YMTaskManager(this);
        _taskManager.start();
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
                String ip = obj.getString("ip");
                int port = obj.getInt("port");
                _netWorker = new YMNetWorker(this);
                _netWorker.start(ip, port);
                sendGetServerInfoSuccessEvent();
            }
            else {
                log("download serverinfo failed");
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

        testButtons();
    }

    public void sendDownLoadTask(String url)
    {
        YMTask task = new YMTask(YMTaskType.DownLoadFile);
        task.mainName = url;
        task.geneDescString();
        _taskManager.addTask(task);
    }

    public void sendInstallAppTask(String fileName)
    {
        YMTask task = new YMTask(YMTaskType.IntallAPP);
        task.mainName = fileName;
        task.geneDescString();
        _taskManager.addTask(task);
    }

    public void sendUnInstallAppTask(String packageName)
    {
        YMTask task = new YMTask(YMTaskType.UninstallAPP);
        task.mainName = packageName;
        task.geneDescString();
        _taskManager.addTask(task);
    }

    public void sendStartAppTask(String packageName)
    {
        YMTask task = new YMTask(YMTaskType.OpenAPP);
        task.mainName = packageName;
        task.geneDescString();
        _taskManager.addTask(task);
    }

    public void sendRestartAppTask()
    {
        YMTask task = new YMTask(YMTaskType.RestartAPP);
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
                YMTask task = new YMTask(YMTaskType.IntallAPP);
                task.mainName = nameApp;
                task.geneDescString();
                _taskManager.addTask(task);
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
    private void showLoadingUI()
    {
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
            _textView_log.append(str + "\n");
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
        _netWorker.sendMessage(new YMMessage(msg));
        YMUtil.log(result);
    }

    public void quit()
    {
        _netWorker.quit();
    }


    // 刷新
    public void update()
    {
        if (_netWorker != null)
        {
            _netWorker.update();
        }
        if(_dispatcher != null)
        {
            _dispatcher.update();
        }
        this.updateUIState();
    }

    // 刷新服务器信息
    private void updateUIServerInfo()
    {
        _textView_serverIP.setText(_netWorker.getServerIP());
        _textView_serverPort.setText(String.valueOf(_netWorker.getServerPort()));
        _textView_clientIP.setText(YMUtil.getIPAddress(this));
    }

    // 刷新UI
    private void updateUIState()
    {
        boolean isConnected = false;
        if(_netWorker != null)
        {
            isConnected = _netWorker.isConnected();
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
        showMainUI();
        updateUIServerInfo();
    }

    private void on_UpdateUIEvent(YMEvent event)
    {
        updateUIServerInfo();
        updateUIState();
    }

}
