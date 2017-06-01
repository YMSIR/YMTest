package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    public static MainActivity instance;
	private YMNetWorker _netWorker;
	private Timer _timer;
    private YMTaskManager _taskManager;
	private TextView _textView_log;
    private TextView _textView_serverIP;
    private TextView _textView_clientIP;
    private TextView _textView_serverPort;
    private TextView _textView_curTask;
    private TextView _textView_connState;
    private ProgressBar _progressBar_loading;
    private ProgressBar _progressBar_task;
    //private static String _serverInfoURL = "https://pan.baidu.com/s/1dEC1033";
    private static String _serverInfoURL = "http://code.taobao.org/p/3ddemo/src/Resources/ui/worldmap/worldmap0.plist";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        _timer= new java.util.Timer(true); 
		TimerTask task = new TimerTask() {  
			   public void run() {
                   update();
			   }     
			};
		_timer.schedule(task, 0, 100);
        initUIWidgets();
        initNetWorker();
    }

    // 初始化网络
    private void initNetWorker()
    {
        _taskManager = new YMTaskManager(this);
        _taskManager.start();
        YMTask task = new YMTask(YMTaskType.DownLoadString);
        task.mainName = _serverInfoURL;
        _taskManager.addTask(task);
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
                updateUIServerInfo();
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
        _textView_log =(TextView) findViewById(R.id.textView_log);
        _textView_log.setText("");
        _textView_log.setMovementMethod(ScrollingMovementMethod.getInstance());
        _textView_serverIP =(TextView) findViewById(R.id.textView_serverip);
        _textView_serverIP.setText("127.0.0.1");
        _textView_serverPort =(TextView) findViewById(R.id.textView_serverport);
        _textView_serverPort.setText("8001");
        _textView_clientIP =(TextView) findViewById(R.id.textView_clientip);
        _textView_clientIP.setText(YMUtil.getLocalIpAddress());
        _textView_connState =(TextView) findViewById(R.id.textView_connstate);
        _textView_connState.setText("未连接");
        _textView_curTask =(TextView) findViewById(R.id.textView_curtask);
        _textView_curTask.setText("空");
        _progressBar_loading =(ProgressBar) findViewById(R.id.progressBar_loading);
        _progressBar_task =(ProgressBar) findViewById(R.id.progressBar_task);

        final String testApp = "com.test.ymclient";
        final String urlAPK = "http://pan.baidu.com/s/1nuAPn7N";
        Button install = (Button)findViewById(R.id.button_install);
        install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YMTask task = new YMTask(YMTaskType.IntallAPP);
                task.mainName = testApp;
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

    // 日志输出
    public void log(String str)
    {
        if(_textView_log != null)
        {
            _textView_log.append(str + "\n");
        }
    }

    // 刷新
    public void update()
    {
        if (_netWorker != null)
        {
            _netWorker.update();
            this.updateUIState();
        }
    }

    // 刷新服务器信息
    private void updateUIServerInfo()
    {
        _textView_serverIP.setText(_netWorker.getServerIP());
        _textView_serverPort.setText(_netWorker.getServerPort());
        _textView_clientIP.setText(YMUtil.getLocalIpAddress());
    }

    // 刷新UI
    private void updateUIState()
    {
        boolean isConnected = _netWorker.isConnected();
        if (isConnected)
        {
            _textView_connState.setText("已连接");
            YMTask task = _taskManager.getCurTask();
            if (task != null)
            {
                _textView_curTask.setText("正在进行");
                _progressBar_loading.setVisibility(View.GONE);
                _progressBar_task.setVisibility(View.VISIBLE);
                _progressBar_task.setProgress(task.progress);
            }
            else
            {
                _textView_curTask.setText("空");
                _progressBar_loading.setVisibility(View.GONE);
                _progressBar_task.setVisibility(View.GONE);
            }
        }
        else
        {
            _textView_connState.setText("连接中");
            _progressBar_loading.setVisibility(View.VISIBLE);
            _progressBar_task.setVisibility(View.GONE);
            _textView_curTask.setText("连接服务器");
        }

    }

}
