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

}
