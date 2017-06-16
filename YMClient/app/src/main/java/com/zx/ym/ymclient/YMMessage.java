package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */
import org.json.JSONException;
import org.json.JSONObject;




class YMPacketHeader
{
	public short sign;
	public short len;
	
	public final static int size = 4;
	public final static short PACKET_SIGN 	= (short) 0x0FFB;
	
	public YMPacketHeader()
	{
		sign = len = 0;
	}
	
	public YMPacketHeader(byte[] data) 
	{
		sign = YMBitConverter.toShort(data, 0);
		len = YMBitConverter.toShort(data, 2);
	}
	
	public boolean checkSign() {
		
		return sign == PACKET_SIGN;
	}

	
	public byte[] getBytes()
	{
		byte[] data = new byte[size];
		byte[] temp = YMBitConverter.getBytes(sign);
		System.arraycopy(temp, 0, data, 0, 2);
		temp = YMBitConverter.getBytes(len);
		System.arraycopy(temp, 0, data, 2, 2);
		return data;
	}
}



public class YMMessage {
	
	public final static int C_MIN 				= 1000;
	public final static int C_CheckAlive 		= C_MIN + 1;
	public final static int C_DeviceInfo 		= C_MIN + 2;
	public final static int C_OperatorResult 	= C_MIN + 3;
	
	public final static int S_MIN 				= 2000;
	public final static int S_CheckAlive 		= S_MIN + 1;
	public final static int S_DeviceInfo 		= S_MIN + 2;
	public final static int S_DownLoad         = S_MIN + 3;
	public final static int S_InstallApp      	= S_MIN + 4;
	public final static int S_UninstallApp    	= S_MIN + 5;
	public final static int S_StartApp         = S_MIN + 6;
	public final static int S_ReStartApp      	= S_MIN + 7;
	
	
	private int _messageId;
	private String _jsonContent;
	private JSONObject _jsonObject;
	
	public  YMMessage(String msg) 
	{	
		_jsonContent = msg;
		_jsonObject = null;
	}	
	
	public int getMessageId()
	{
		return _messageId;
	}
	public String getJsonContent()
	{
		return _jsonContent;
	}
	
	public JSONObject getJsonObj() 
	{
		return _jsonObject;
	}
	
	public void decode() 
	{
		try 
		{
			_jsonObject = new JSONObject(_jsonContent);
			_messageId = _jsonObject.getInt("id");
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
	}
	
	public byte[] getBytes()
	{
		byte[] contentBytes =  _jsonContent.getBytes();
		short dataLen = (short) (contentBytes.length + YMPacketHeader.size);
		YMPacketHeader header = new YMPacketHeader();
		header.sign = YMPacketHeader.PACKET_SIGN;
		header.len = dataLen;
		byte[] data = new byte[dataLen];
		byte[] headerBytes = header.getBytes();
		System.arraycopy(headerBytes, 0, data, 0, header.size);
		System.arraycopy(contentBytes, 0, data, header.size, contentBytes.length);
		
		return data;
	}
	
	public static String Make_C_CheckAlive() 
	{
		String formateStr = "{\"id\":%d,\"order\":%d}";
		return String.format( formateStr,C_CheckAlive, System.currentTimeMillis());
	}
	
	public static String Make_C_DeviceInfo(String deviceId,String phoneBrand, String phoneModel, String version) 
	{
		String formateStr = "{\"id\":%d,\"deviceId\":\"%s\",\"phoneBrand\":\"%s\",\"phoneModel\":\"%s\",\"version\":\"%s\"}";
		return String.format( formateStr,C_DeviceInfo, deviceId, phoneBrand, phoneModel, version);
	}

	public static String Make_C_OperatorResult(String result)
	{
		String formateStr = "{\"id\":%d,\"result\":\"%s\"}";
		return String.format( formateStr,C_OperatorResult, result);
	}
	
}
