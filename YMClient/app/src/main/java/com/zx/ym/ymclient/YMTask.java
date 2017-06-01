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
    public boolean isCompleted;
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
        isCompleted = false;
        finishListener = null;
    }

    public void finish()
    {
        progress = 100;
        isCompleted = true;
        if (finishListener != null)
        {
            finishListener.onFinish(this);
        }
    }

}
