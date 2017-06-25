#coding=utf-8
#程序入口

import wx
import sys
import MySQLdb

from YMUIWindow import YMUIWindow as YMUIWindow

class YMServer:
    def __init__(self):
        reload(sys)
        sys.setdefaultencoding('utf8')
    #启动
    def start(self):
        self.app = wx.App()
        self.mainUI = YMUIWindow(None,title=u"YMServer")
        self.app.MainLoop()

if __name__ == '__main__':
    YMServer().start()


