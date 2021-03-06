package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

import java.util.Queue;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.ProviderInfo;



public class YMNetWorker {
	
	private Queue<YMMessage> _sendMessageQueue;
	private Queue<YMMessage> _recvMessageQueue;
	private YMNetThread _netThread;
	private YMDispatcher _dispatcher;
	private Context _context;
	private String _ip;
	private int _port;
	
	
	public YMNetWorker(Context context) {
		_context = context;
		_sendMessageQueue = new LinkedList<YMMessage>();
		_recvMessageQueue = new LinkedList<YMMessage>();
		bindListener();
	}
	
	public YMDispatcher getDispatcher() 
	{
		return _dispatcher;
	}
	
	public YMNetThread getNetThread() 
	{
		return _netThread;
	}
	
	public YMMessage popSendMessageQueue()
	{
		synchronized (_sendMessageQueue) {
			
			return _sendMessageQueue.poll();
		}
	}
	
	public void putSendMessageQueue(YMMessage message)
	{
		synchronized (_sendMessageQueue) {
			
			_sendMessageQueue.offer(message);
		}
	}
	
	public YMMessage popRecvMessageQueue() 
	{
		synchronized (_recvMessageQueue) {
			
			return _recvMessageQueue.poll();
			
		}
	}
	
	public void putRecvMessageQueue(YMMessage message) 
	{
		synchronized (_recvMessageQueue) {
	
			_recvMessageQueue.offer(message);
		}
	}
	
	
	public void start(String ip, int port)
	{
		_ip = ip;
		_port = port;
		YMThreadArgs args = new YMThreadArgs();
		args.ip = _ip;
		args.port = _port;
		args.worker = this;
		_netThread = new YMNetThread(args);
		_netThread.start();
	}

	public void quit()
	{
		_netThread.disconnect();
		_netThread.quit();
		_netThread = null;
	}


	public String getServerIP()
	{
		return _ip;
	}

	public int getServerPort()
	{
		return _port;
	}

	public boolean isConnected()
	{
		if (_netThread != null)
		{
			return _netThread.isConnected();
		}

		return  false;
	}


	public void update()
	{
		_dispatcher.update();
		this.handMessage();
	}
	
	public void sendMessage(YMMessage message)
	{
		putSendMessageQueue(message);
	}

	// 绑定监听
	private void bindListener()
	{
		_dispatcher = new YMDispatcher();
		_dispatcher.addListener(YMEvent.ID_ConnSuccess, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_ConnSuccessEvent(event);
			}
		});
		_dispatcher.addListener(YMEvent.ID_DisConnect, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_DisConnectEvent(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_CheckAlive, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_CheckAlive(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_DeviceInfo, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_DeviceInfo(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_DownLoad, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_DownLoad(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_InstallApp, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_InstallApp(event);
			}
		});
		_dispatcher.addListener(YMMessage.S_UninstallApp, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_UninstallApp(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_StartApp, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_StartApp(event);
			}
		});

		_dispatcher.addListener(YMMessage.S_ReStartApp, new YMEvent.OnListener() {
			@Override
			public void onEvent(YMEvent event) {

				on_S_ReStartApp(event);
			}
		});

	}

	private void handMessage() 
	{	
		YMMessage message = popRecvMessageQueue();
		while (message != null)
		{
			YMEvent event = new YMEvent(message.getMessageId());
			event.addAttr("message", message);
			_dispatcher.dispatch(event);
			message = popRecvMessageQueue();
		}
	}
	
	
	private void on_ConnSuccessEvent(YMEvent event)
	{
		YMUtil.log("on_ConnSuccessEvent");
		String deviceId = YMUtil.getDeviceId(_context);
		String phoneBrand = YMUtil.getPhoneBrand();
		String phoneModel = YMUtil.getPhoneModel();
		String version = YMUtil.getBuildVersion();
		String msg = YMMessage.Make_C_DeviceInfo(deviceId, phoneBrand, phoneModel, version);
		sendMessage(new YMMessage(msg));
	}
	
	private void on_DisConnectEvent(YMEvent event)
	{
		YMUtil.log("on_DisConnectEvent");
	}
	
	private void on_S_CheckAlive(YMEvent event) 
	{
		String jsonString = YMMessage.Make_C_CheckAlive();
		sendMessage(new YMMessage(jsonString));
		//YMUtil.log("❤❤❤❤❤");
	}
	
	private void on_S_DeviceInfo(YMEvent event) 
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null) 
		{
			JSONObject obj = message.getJsonObj();
			try {
				int res = obj.getInt("result");
				if (res == 1) {
					YMUtil.log("on_S_DeviceInfo success");
				}
				else {
					YMUtil.log("on_S_DeviceInfo failed");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void on_S_DownLoad(YMEvent event)
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null)
		{
			JSONObject obj = message.getJsonObj();
			try {
				String url = obj.getString("url");
				MainActivity.instance.sendTask(YMTaskType.DownLoadFile, url);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	private void on_S_InstallApp(YMEvent event)
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null)
		{
			JSONObject obj = message.getJsonObj();
			try {
				String filename = obj.getString("filename");
				MainActivity.instance.sendTask(YMTaskType.IntallAPP,filename);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void on_S_UninstallApp(YMEvent event)
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null)
		{
			JSONObject obj = message.getJsonObj();
			try {
				String packagename = obj.getString("packagename");
				MainActivity.instance.sendTask(YMTaskType.UninstallAPP,packagename);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void on_S_StartApp(YMEvent event)
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null)
		{
			JSONObject obj = message.getJsonObj();
			try {
				String packagename = obj.getString("packagename");
				MainActivity.instance.sendTask(YMTaskType.OpenAPP,packagename);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void on_S_ReStartApp(YMEvent event)
	{
		YMMessage message = (YMMessage)event.getAttr("message");
		if (message != null)
		{
			MainActivity.instance.sendTask(YMTaskType.RestartAPP, "");
		}
	}
}
