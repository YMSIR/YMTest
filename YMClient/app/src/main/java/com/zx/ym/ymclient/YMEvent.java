package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */
import java.util.HashMap;
import java.util.Map;


public class YMEvent {
	
	public final static int ID_ConnSuccess 				= 1;
	public final static int ID_DisConnect 					= 2;
	public final static int ID_Log 							= 3;
	public final static int ID_GetServerInfoSuccess 		= 4;
	public final static int ID_UpdateUI 					= 5;

	interface OnListener{
		public void onEvent(YMEvent event);
	}
	
	private int _ymEventType;
	private Map<String, Object>_argMap;
	
	public YMEvent(int ymEventType) {
		
		_ymEventType = ymEventType;
		_argMap = new HashMap<String, Object>();
	}
	
	public int getEventType() {
		return _ymEventType;
	}
	
	public void addAttr(String key, Object value) {
		_argMap.put(key, value);
	}
	
	public Object getAttr(String key) {
		if (_argMap.containsKey(key)) {
			return _argMap.get(key);
		}
		else {
			return null;
		}
	}
	
}
