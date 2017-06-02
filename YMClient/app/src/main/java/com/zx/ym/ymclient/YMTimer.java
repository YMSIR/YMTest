package com.zx.ym.ymclient;

import android.os.Handler;
import android.os.Message;

/**
 * Created by zhangxinwei02 on 2017/6/1.
 */

public class YMTimer extends Handler {

    private OnUpdate _onUpdate;
    private long _period;

     public YMTimer()
     {
         super();
         _onUpdate = null;
         _period = 100;
     }

    public void setOnUpdate(OnUpdate onUpdate)
    {
        _onUpdate = onUpdate;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                // 移除所有的msg.what为0等消息，保证只有一个循环消息队列再跑
                this.removeMessages(0);
                if(_onUpdate != null)
                {
                    _onUpdate.update();
                }
                // 再次发出msg，循环更新
                this.sendEmptyMessageDelayed(0, _period);
                break;
            case 1:
                // 直接移除，定时器停止
                this.removeMessages(0);
                break;

            default:
                break;
        }
    }

    public void start(long delay, long period)
    {
        _period = period;
        this.sendEmptyMessageDelayed(0, delay);
    }

    public void stop()
    {
        this.sendEmptyMessage(1);
    }

    interface OnUpdate
    {
        public void update();
    }

}
