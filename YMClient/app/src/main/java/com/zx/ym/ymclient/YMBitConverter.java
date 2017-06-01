package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

import java.io.UnsupportedEncodingException;

import android.R.integer;

public class YMBitConverter {

	// 字符串转BYTES
	public static byte[] getBytes(String str)
	{
		return str.getBytes();
	}

	// INT转BYTES
	public static byte[] getBytes(int i)
	{
		byte[] data = new byte[4];
		data[0] = (byte)(i >> 24 & 0xFF );
		data[1] = (byte)(i >> 16 & 0xFF );
		data[2] = (byte)(i >> 8 & 0xFF );
		data[3] = (byte)(i & 0xFF);
		return data;
	}

	// SHORT转BYTES
	public static byte[] getBytes(short s)
	{
		byte[] data = new byte[2];
		data[0] = (byte)(s & 0xFF);
		data[1] = (byte)(s >> 8 & 0xFF);
		return data;
	}
	
	// BYTES转SHORT
	public static short toShort(byte[] data, int index)
	{
		assert(data.length - index >= 2);
		return  (short) (data[index + 0] & 0xFF | (data[index + 1] & 0xFF) << 8);  
	}
	
	// BYTES转INT
	public static int toInt(byte[] data, int index)
	{
		assert(data.length - index >= 4);
		return  data[0 + index] & 0xFF | (data[1 + index] & 0xFF) << 8 | (data[2 + index] & 0xFF) << 16 | (data[3 + index] & 0xFF) << 24;  
	}
	
	// BYTES转STRING
	public static String toString(byte[] data)
	{
		String	s = new String(data);
		return s;
	}

}
