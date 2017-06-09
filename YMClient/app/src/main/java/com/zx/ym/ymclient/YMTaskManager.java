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
        YMUtil.checkRootPermission();
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
            _taskQueue.notify();
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
                try
                {
                    synchronized (_taskQueue)
                    {
                        _taskQueue.wait();
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            else
            {
                YMUtil.log("执行任务:" + _curTask.desc);
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
                        handleDownLoadFile(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case OpenAPP:
                    {
                        handleStartApp(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case CloseAPP:
                    {
                        handleCloseApp(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case IntallAPP:
                    {
                        handleInstallApp(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case UninstallAPP:
                    {
                        handleUninstallApp(_curTask.mainName);
                        _curTask.finish();
                    }
                    break;
                    case RestartAPP:
                    {
                        handleRestartApp();
                        _curTask.finish();
                    }
                    break;
                }
                _curTask = null;
            }

        }while (_isRun);
    }

    private boolean handleInstallApp(String fileName)
    {
        int result =  YMUtil.installAPK(fileName);
        if (result == 0)
        {
            MainActivity.instance.sendOperatorResult("安装" + fileName + "成功");
            return true;
        }
        else if(result == 1)
        {
            MainActivity.instance.sendOperatorResult("安装" + fileName + "等待手动操作");
            return false;
        }
        else
        {
            MainActivity.instance.sendOperatorResult("安装" + fileName + "失败");
            return false;
        }
    }

    private boolean handleUninstallApp(String packageName)
    {
        int result =  YMUtil.uninstallAPK(packageName);
        if (result == 0)
        {
            MainActivity.instance.sendOperatorResult("卸载" + packageName + "成功");
            return true;
        }
        else if(result == 1)
        {
            MainActivity.instance.sendOperatorResult("卸载" + packageName + "等待手动操作");
            return false;
        }
        else
        {
            MainActivity.instance.sendOperatorResult("卸载" + packageName + "失败");
            return false;
        }
    }

    private boolean handleStartApp(String packageName)
    {
        boolean result =  YMUtil.startAPP(packageName);
        if (result)
        {
            MainActivity.instance.sendOperatorResult("启动" + packageName + "成功");

        }
        else
        {
            MainActivity.instance.sendOperatorResult("启动" + packageName + "失败");
        }
        return result;
    }

    private boolean handleRestartApp()
    {
        MainActivity.instance.quit();
        boolean result =  YMUtil.restartAPP();
        if (result)
        {
            MainActivity.instance.sendOperatorResult("重启"  + "成功");
        }
        else
        {
            MainActivity.instance.sendOperatorResult("重启"  + "失败");
        }
        return result;
    }

    private boolean handleCloseApp(String packageName)
    {
        boolean result =  YMUtil.closeAPP(packageName);
        if (result)
        {
            MainActivity.instance.sendOperatorResult("关闭" + packageName + "成功");

        }
        else
        {
            MainActivity.instance.sendOperatorResult("关闭" + packageName + "失败");
        }
        return result;
    }

    private boolean handleDownLoadFile(String url)
    {
        YMUtil.checkFileRootDir();
        YMDownLoader downLoader = new YMDownLoader(url);
        String fileName = YMUtil.geneFileNameFromUrl(url);
        boolean result = downLoader.down2sd(fileName, _curTask);
        if (result == true)
        {
            MainActivity.instance.sendOperatorResult("下载" + fileName + "成功");
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (suffix.equals("apk"))
            {
                handleInstallApp(fileName);
            }
        }
        else
        {
            MainActivity.instance.sendOperatorResult("下载" + fileName + "失败");
        }
        return result;
    }
}
