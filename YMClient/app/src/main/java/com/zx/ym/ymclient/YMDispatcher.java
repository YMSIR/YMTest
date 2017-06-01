package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;




interface YMListener{

	public void onEvent(YMEvent event); 
}


public class YMDispatcher {
	
	// 监听列表
	private Map<Integer, List<YMListener>> _listenMap;
	// 缓存列表
	private	Queue<YMEvent> _cacheEventQueue;
	
	public YMDispatcher() {
	
		_listenMap = new HashMap<Integer, List<YMListener>>();
		_cacheEventQueue = new LinkedList<YMEvent>();
	}
	
	// 监听事件
	public void addListener(int id, YMListener listener) {
		
		List<YMListener> list = _listenMap.get(id);
		if (list == null) {
			list = new ArrayList<YMListener>();
			_listenMap.put(id, list);
		}
		
		if(!list.contains(listener))
		{
			list.add(listener);
		}
	}
	
	// 派发事件
	public void dispatch(YMEvent event) {
		
		List<YMListener> list = _listenMap.get(event.getEventType());
		if (list != null) {
			
			for (int i = 0; i < list.size(); i++) {
				list.get(i).onEvent(event);
			}
			
		}
	}
	
	// 在主线程派发事件
	public void dispatchInMainThread(YMEvent event)
	{
		synchronized (_cacheEventQueue) {
			_cacheEventQueue.offer(event);
		}
		
	}
	
	
	// 主线程派发缓存事件
	public void update() {
		
		while (_cacheEventQueue.isEmpty() == false) {
			
			YMEvent event = null;
			synchronized (_cacheEventQueue) {
				event =  _cacheEventQueue.poll();
			}
			dispatch(event);
		}
	}
	

}
