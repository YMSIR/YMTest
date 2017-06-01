#encoding=utf-8
#事件派发处理器

import Queue
import threading


class YMDispatcher:
    #初始化
    def __init__(self):
        self.listenDict = {}
        self.cacheEvents = Queue.Queue()
        self.lock = threading.Lock()

    #加入监听
    def addListener(self, id, handler):
        if self.listenDict.has_key(id) == False:
            self.listenDict[id] = []
        isHas = False
        for h in self.listenDict[id]:
            if h == handler:
                isHas = True
                break
        if isHas == False:
            self.listenDict[id].append(handler)

    #删除监听
    def removeListener(self, hander):
        for k, v in self.listenDict.items():
            for h in v:
                if h == hander:
                    v.remove(h)
                    break

    #派发事件
    def dispatchEvent(self, event):
        if event.id == None:
            return
        handlers = self.listenDict[event.id]
        if handlers != None:
            for handler in handlers:
                handler(event)

    #发送到主线程执行
    def dispatchToMainThread(self,event):
        self.lock.acquire()
        self.cacheEvents.put(event)
        self.lock.release()

    #刷新执行缓存事件
    def update(self):
        while self.cacheEvents.empty() == False:
            self.lock.acquire()
            event = self.cacheEvents.get()
            self.lock.release()
            self.dispatchEvent(event)
    #清空
    def clear(self):
        del self.listenDict[:]
        while self.cacheEvents.empty() == False:
            self.cacheEvents.get()

