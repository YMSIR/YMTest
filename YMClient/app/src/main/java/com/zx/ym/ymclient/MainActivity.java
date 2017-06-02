package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogRecord;

import android.app.Notification;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import org.w3c.dom.ProcessingInstruction;

import android.os.Message;
import android.os.Handler;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        initUIWidgets();
        initNetWorker();
        initTimerHandler();
        initBindListener();
        showLoadingUI();
    }

    // 初始化网络
    private void initNetWorker()
    {
        _taskManager = new YMTaskManager(this);
        _taskManager.start();
        YMTask task = new YMTask(YMTaskType.DownLoadString);
        task.mainName = _serverInfoURL;
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
        _textView_serverIP =(TextView) findViewById(R.id.textView_serverip);
        _textView_serverPort =(TextView) findViewById(R.id.textView_serverport);
        _textView_clientIP =(TextView) findViewById(R.id.textView_clientip);
        _textView_connState =(TextView) findViewById(R.id.textView_connstate);
        _textView_curTask =(TextView) findViewById(R.id.textView_curtask);
        _progressBar_loading =(ProgressBar) findViewById(R.id.progressBar_loading);
        _progressBar_task =(ProgressBar) findViewById(R.id.progressBar_task);

        testButtons();
    }

    // 测试
    private void testButtons()
    {
        final String testApp = "com.test.ymclient";
        final String nameApp = "YMClient.apk";
        final String urlAPK = "http://10.246.52.71/YMClient.apk";
        Button install = (Button)findViewById(R.id.button_install);
        install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.IntallAPP);
                task.mainName = nameApp;
                _taskManager.addTask(task);
            }
        });
        Button uninstall = (Button)findViewById(R.id.button_uninstall);
        uninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.UninstallAPP);
                task.mainName = testApp;
                _taskManager.addTask(task);
            }
        });
        Button down = (Button)findViewById(R.id.button_down);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.DownLoadFile);
                task.mainName = urlAPK;
                _taskManager.addTask(task);
            }
        });
        Button start = (Button)findViewById(R.id.button_start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.OpenAPP);
                task.mainName = testApp;
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
            _textView_log.append(str + "\n");
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
            _textView_curTask.setText("正在进行");
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
