package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

enum YMTaskType
{
    DownLoadString,
    DownLoadFile,
    OpenAPP,
    CloseAPP,
    IntallAPP,
    UninstallAPP,
    RestartAPP,
}


public class YMTask {

    public YMTaskType taskType;
    public String mainName;
    public int progress;
    public boolean isFinished;
    public String result;
    public String desc;
    public OnFinishListener finishListener;

    interface OnFinishListener
    {
        public void onFinish(YMTask task);
    }

    public YMTask(YMTaskType type)
    {
        taskType = type;
        mainName = "";
        progress = 0;
        result = "";
        desc = "";
        isFinished = false;
        finishListener = null;
    }

    public void finish()
    {
        progress = 100;
        isFinished = true;
        if (finishListener != null)
        {
            finishListener.onFinish(this);
        }
    }

    public void geneDescString()
    {
        switch (taskType)
        {
            case DownLoadString:
            {
                desc = "下载服务器地址";
                break;
            }
            case DownLoadFile:
            {
                desc = "下载文件:" + mainName;
            }
            break;
            case OpenAPP:
            {
                desc = "打开App:" + mainName;
            }
            break;
            case CloseAPP:
            {
                desc = "关闭App:" + mainName;
            }
            break;
            case IntallAPP:
            {
                desc = "安装App:" + mainName;
            }
            break;
            case UninstallAPP:
            {
                desc = "卸载App:" + mainName;
            }
            break;
            case RestartAPP:
            {
                desc = "重启App";
            }
            break;
        }
    }


}
