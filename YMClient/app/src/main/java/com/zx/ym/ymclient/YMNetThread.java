package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.R.bool;
import android.R.integer;
import android.preference.PreferenceActivity.Header;
import android.provider.ContactsContract.Contacts.Data;
import android.text.InputFilter.LengthFilter;


 class YMThreadArgs{
	String 		ip;
	int 		port;
	YMNetWorker worker;
}
 


public class YMNetThread extends Thread {
	
	public final static int CONNNECT_TIMEOUT = 100;
	public final static int SOCKET_TIMEOUT = 100;
	public final static int RECONNNECT_TIMEDELATA = 5000;
	public final static int SENDBUFFER_SIZE = 100*1024;
	public final static int RECVBUFFER_SIZE = 100*1024;
	private YMThreadArgs _args;
	private boolean _isConnected;
	private Socket _socket;
	private InetSocketAddress _address;
	private byte[] _recvBuffer;
	private byte[] _sendBuffer;
	private int _recvOffset;
	private boolean _isRun;
	private long _lastConnTime;

	
	public YMNetThread(YMThreadArgs args)
	{
		_args = args;
		_isConnected = false;
		_isRun = false;
		_recvOffset = 0;
		_lastConnTime = 0;
		_address = new InetSocketAddress(_args.ip, _args.port);
		_recvBuffer = new byte[RECVBUFFER_SIZE];
		_sendBuffer = new byte[SENDBUFFER_SIZE];
	}

	// 启动
	public void start()
	{
		_isRun = true;
		super.start();
	}
	
	public void quit() 
	{
		_isRun = false;
	}

	// 线程循环
	public void run() 
	{
		do
		{
			if (!_isConnected)
			{
				Connect();
			}
			else
			{
				recv();
				send();		
			}
			
			try 
			{
				sleep(100);
				
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		
		} while (_isRun);		
	}

	// 连接服务器
	private void Connect()
	{
		long curTime = System.currentTimeMillis();
		if (curTime - _lastConnTime < RECONNNECT_TIMEDELATA) 
		{
			return;
		}
		try 
		{
			if (_socket != null)
			{
				_socket.close();
				_socket = null;
			}
			_socket = new Socket();
			_socket.setSoTimeout(SOCKET_TIMEOUT);
			_socket.setReceiveBufferSize(RECVBUFFER_SIZE);
			_socket.setSendBufferSize(SENDBUFFER_SIZE);
			_socket.setKeepAlive(true);
			_socket.setTcpNoDelay(true);
			_socket.connect(_address, CONNNECT_TIMEOUT);
			setIsConnected(true);
		} 
		catch (Exception e) 
		{
			YMUtil.log(e.getMessage());
		}
	}
	
	// 接收消息
	private void recv() 
	{
		try 
		{
			InputStream inputStream = _socket.getInputStream();		
			int readSize = inputStream.read(_recvBuffer, _recvOffset, RECVBUFFER_SIZE - _recvOffset);
			if (readSize > 0) 
			{
				_recvOffset += readSize;
				while (_recvOffset > YMPacketHeader.size) 
				{
					YMPacketHeader header = new YMPacketHeader(_recvBuffer);
					if(header.checkSign())
					{
						if (_recvOffset >= header.len) 
						{
							int contentSize = header.len - header.size;
							byte[] content = new byte[contentSize];
							System.arraycopy(_recvBuffer, header.size, content, 0, contentSize);
							String jsonContent = new String(content);
							YMMessage message = new YMMessage(jsonContent);
							message.decode();
							_args.worker.putRecvMessageQueue(message);
							System.arraycopy(_recvBuffer, header.len, _recvBuffer, 0, _recvOffset - header.len);
							_recvOffset -= header.len;
						}
						else 
						{
							break;
						}
					}
					else
					{
						_recvOffset = 0;
						break;
					}
				}
			}
			else
			{	
				YMUtil.log( "read " + readSize);
			}

		} 
		catch (Exception e) 
		{
			//YMUtil.log(e.getMessage());
		}
	}
	
	// 发送消息
	private void send() 
	{
		try 
		{
			YMMessage message = _args.worker.popSendMessageQueue();
			while (message != null) 
			{
				
				OutputStream outputStream = _socket.getOutputStream();
				outputStream.write(message.getBytes());
				outputStream.flush();
				message = _args.worker.popSendMessageQueue();
			}
				
		} 
		catch (Exception e) 
		{
			disconnect();
		}
	}
	
	// 断开连接
	public void disconnect() 
	{
		if (_isConnected) 
		{
			try 
			{
				_socket.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			_socket = null;
			setIsConnected(false);
		}
	}
	
	// 设置是否连接
	private void setIsConnected(boolean is) {
		if (_isConnected == is) {
			return;
		}
		_isConnected = is;
		if (_isConnected) {
			YMEvent event = new YMEvent(YMEvent.ID_ConnSuccess);
			_args.worker.getDispatcher().dispatchInMainThread(event);
		}
		else {
			YMEvent event = new YMEvent(YMEvent.ID_DisConnect);
			_args.worker.getDispatcher().dispatchInMainThread(event);
		}
	}

	public boolean isConnected()
	{
		return _isConnected;
	}

}
