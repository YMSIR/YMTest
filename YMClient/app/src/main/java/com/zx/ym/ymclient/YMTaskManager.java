package com.zx.ym.ymclient;

import android.app.ActivityManager;
import android.content.Context;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

public class YMTaskManager extends Thread {

    private Queue<YMTask> _taskQueue;
    private Context _context;
    private boolean _isRun;
    private YMTask _curTask;


    public YMTaskManager(Context context) {
        _context = context;
        _taskQueue = new LinkedList<YMTask>();
        _isRun = false;
        _curTask = null;
    }

    public void start()
    {
        _isRun = true;
        super.start();
    }

    public YMTask getCurTask()
    {
        return _curTask;
    }

    public void addTask(YMTask task)
    {
        synchronized (_taskQueue) {

             _taskQueue.offer(task);
        }
    }

    public YMTask popTask()
    {
        synchronized (_taskQueue) {

            return  _taskQueue.poll();
        }
    }

    public void run()
    {
        do {
            _curTask = popTask();
            if (_curTask == null)
            {

            }
            else
            {
                switch (_curTask.taskType)
                {
                    case DownLoadString:
                    {
                        YMDownLoader downLoader = new YMDownLoader(_curTask.mainName);
                        _curTask.result = downLoader.downloadAsString();
                        _curTask.finish();
                        break;
                    }
                    case DownLoadFile:
                    {
                        YMUtil.checkFileRootDir();
                        YMDownLoader downLoader = new YMDownLoader(_curTask.mainName);
                        String fileName = YMUtil.geneFileNameFromUrl(_curTask.mainName);
                        downLoader.down2sd(fileName, _curTask);
                        _curTask.finish();
                    }
                    break;
                    case OpenAPP:
                    {
                        YMUtil.startAPP(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case CloseAPP:
                    {
                        ActivityManager manager = (ActivityManager) MainActivity.instance.getSystemService(Context.ACTIVITY_SERVICE);
                        manager.killBackgroundProcesses(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case IntallAPP:
                    {
                        YMUtil.silentInstall(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case UninstallAPP:
                    {
                        YMUtil.uninstallAPK(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case RestartAPP:
                    {
                        YMUtil.restartAPP();
                        _curTask.finish();
                    }
                    break;
                }
                _curTask = null;
            }

        }while (_isRun);
    }


}
