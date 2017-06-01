#encoding=utf-8
#双队列处理线程同步

import Queue

class YMDoubleQueue:
    def __init__(self):
        self.queueA = Queue.Queue()
        self.queueB = Queue.Queue()
        self.cur = self.queueA

    def getCurQueue(self):
        return self.cur

    def swapQueue(self):
        q = self.cur
        if self.cur == self.queueA:
            self.cur = self.queueB
        else:
            self.cur = self.queueA
        return q