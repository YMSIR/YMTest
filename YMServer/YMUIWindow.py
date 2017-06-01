#encoding=utf8
#程序窗口

import random
import sys,os
import time
import socket
import wx
import wx.aui
import  wx.lib.mixins.listctrl  as  listmix
import YMUtil
from YMNetWorker import YMNetWorker as YMNetWorker
from YMEvent import  YMEvent as YMEvent
#---------------------------------------------------------------------------
#日志窗口

class YMLog(wx.PyLog):
    def __init__(self, textCtrl, logTime=0):
        wx.PyLog.__init__(self)
        self.tc = textCtrl
        self.logTime = logTime

    def DoLogText(self, message):
        if self.tc:
            self.tc.AppendText(message + '\n')

#---------------------------------------------------------------------------
#设备列表

class YMDevList(wx.ListCtrl, listmix.CheckListCtrlMixin, listmix.ListCtrlAutoWidthMixin):
    def __init__(self, parent, data):
        wx.ListCtrl.__init__(
            self, parent, -1,
            style=wx.LC_REPORT
                  #|wx.LC_VIRTUAL
                  |wx.LC_HRULES
                  |wx.LC_VRULES
            )
        self.data = data
        listmix.CheckListCtrlMixin.__init__(self)
        listmix.ListCtrlAutoWidthMixin.__init__(self)
        self.InsertColumn(0, u"编号")
        self.InsertColumn(1, u"IP地址")
        self.InsertColumn(2, u"设备ID")
        self.InsertColumn(3, u"手机品牌")
        self.InsertColumn(4, u"手机型号")
        self.InsertColumn(5, u"系统版本")

        self.SetColumnWidth(0, 100)
        self.SetColumnWidth(1, 100)
        self.SetColumnWidth(2, 120)
        self.SetColumnWidth(3, 100)
        self.SetColumnWidth(4, 100)

        self.Bind(wx.EVT_COMMAND_RIGHT_CLICK, self.OnRightClick)
        self.Bind(wx.EVT_RIGHT_UP, self.OnRightClick)

    #数据刷新列表
    def refreshList(self, data):
        self.DeleteAllItems()
        count = len(data.items())
        for i in range(0,count):
            self.addItem(data.items()[i])

    #添加Item
    def addItem(self,data):
        index = self.InsertStringItem(sys.maxint, "")
        res = self.SetItemData(index, data[0])
        self.refreshItem(data)
    #删除Item
    def removeItem(self, id):
        index = self.FindItemData(-1, id)
        self.DeleteItem(index)

    #刷新Item
    def refreshItem(self, data):
        id =  data[0]
        attr = data[1]
        index = self.FindItemData(-1, id)
        self.SetStringItem(index, 0, str(id))
        self.SetStringItem(index, 1, YMUtil.safe_GeStringValueFromDict(attr, "ip"))
        self.SetStringItem(index, 2, YMUtil.safe_GeStringValueFromDict(attr, "deviceId"))
        self.SetStringItem(index, 3, YMUtil.safe_GeStringValueFromDict(attr, "phoneBrand"))
        self.SetStringItem(index, 4, YMUtil.safe_GeStringValueFromDict(attr, "phoneModel"))
        self.SetStringItem(index, 5, YMUtil.safe_GeStringValueFromDict(attr, "version"))

    #右键菜单
    def OnRightClick(self, event):
        if not hasattr(self, "popupID1"):
            self.popupID1 = wx.NewId()
            self.popupID2 = wx.NewId()
            self.popupID3 = wx.NewId()
            self.Bind(wx.EVT_MENU, self.OnPopupOne, id=self.popupID1)
            self.Bind(wx.EVT_MENU, self.OnPopupTwo, id=self.popupID2)
            self.Bind(wx.EVT_MENU, self.OnPopupThree, id=self.popupID3)
        # make a menu
        menu = wx.Menu()
        # add some items
        menu.Append(self.popupID1, u"全部选中")
        menu.Append(self.popupID2, u"取消全选")
        menu.Append(self.popupID3, u"刷新列表")
        # Popup the menu.  If an item is selected then its handler
        # will be called before PopupMenu returns.
        self.PopupMenu(menu)
        menu.Destroy()

    #全部选中
    def OnPopupOne(self, event):
        count =  self.GetItemCount()
        for i in range(0, count):
            self.CheckItem(i, True)

    #取消选中
    def OnPopupTwo(self, event):
        count =  self.GetItemCount()
        for i in range(0, count):
            self.CheckItem(i, False)

    #刷新列表
    def OnPopupThree(self, event):
        self.refreshList(self.data)


#---------------------------------------------------------------------------
#主窗口

class YMUIWindow(wx.Frame):
    def __init__(self, parent, title):

        #网络
        hostName = socket.gethostname()

        localIP = socket.gethostbyname(socket.gethostname())
        print(localIP)
        self.ymNetWorker = YMNetWorker(localIP, 8001, wx.LogMessage)
        self.ymNetWorker.start()

        #UI
        wx.Frame.__init__(self, parent, -1, title, size = (970, 720),
                          style=wx.DEFAULT_FRAME_STYLE | wx.NO_FULL_REPAINT_ON_RESIZE)
        self.SetMinSize((640,480))
        self.rootPanel = wx.Panel(self)
        self.mgr = wx.aui.AuiManager()
        self.allowAuiFloating = False
        self.mgr.SetManagedWindow(self.rootPanel)
        self.leftPanel = wx.Panel(self.rootPanel, style=wx.TAB_TRAVERSAL|wx.CLIP_CHILDREN)
        self.rightPanel = wx.Panel(self.rootPanel, style=wx.TAB_TRAVERSAL|wx.CLIP_CHILDREN)
        self.initCmdList(self.leftPanel)
        self.initDevList(self.rightPanel)
        self.initTimer()

        #日志
        self.log = wx.TextCtrl(self.rootPanel, -1,
                              style = wx.TE_MULTILINE|wx.TE_READONLY|wx.HSCROLL)
        if wx.Platform == "__WXMAC__":
            self.log.MacCheckSpelling(False)
        wx.Log_SetActiveTarget(YMLog(self.log))

        #设置布局
        self.mgr.AddPane(self.rightPanel, wx.aui.AuiPaneInfo().CenterPane().Name("DevList"))
        self.mgr.AddPane(self.leftPanel,
                         wx.aui.AuiPaneInfo().
                         Left().Layer(2).BestSize((240, -1)).
                         MinSize((240, -1)).
                         Floatable(self.allowAuiFloating).FloatingSize((240, 700)).
                         Caption(u"命令列表").
                         CloseButton(False).
                         Name("CMDWindow"))
        self.mgr.AddPane(self.log,
                         wx.aui.AuiPaneInfo().
                         Bottom().BestSize((-1, 150)).
                         MinSize((-1, 140)).
                         Floatable(self.allowAuiFloating).FloatingSize((500, 160)).
                         Caption(u"输出日志").
                         CloseButton(False).
                         Name("LogWindow"))
        self.mgr.Update()
        self.mgr.SetFlags(self.mgr.GetFlags() ^ wx.aui.AUI_MGR_TRANSPARENT_DRAG)
        self.Show()

        self.refreshDevList()
        self.initBindEvent()
        wx.LogMessage(u"启动成功" )

    #初始化定时器
    def initTimer(self):
        self.update = None
        self.timer = wx.Timer(self)
        self.Bind(wx.EVT_TIMER, self.OnUpdate, self.timer)
        self.timer.Start(10)

    #命令列表
    def initCmdList(self, parent):
        btn_openApp = wx.Button(parent, -1, u"启动APP")
        btn_closeApp = wx.Button(parent, -1, u"关闭APP")
        self.Bind(wx.EVT_BUTTON, self.OnBtn_OpenApp, btn_openApp)
        self.Bind(wx.EVT_BUTTON, self.OnBtn_CloseApp, btn_closeApp)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(btn_openApp, 0, wx.Left|wx.Right|wx.EXPAND, 10)
        sizer.Add(btn_closeApp, 0,wx.Left|wx.Right|wx.EXPAND, 10)
        parent.SetSizer(sizer)
        parent.Layout()

    #初始化设备列表
    def initDevList(self, parent):
        self.list = YMDevList(parent, self.ymNetWorker.clientDict)
        titlePanel = wx.Panel(parent)
        titlePanel.SetBackgroundColour('#BEBEBE')
        titleText = wx.StaticText(titlePanel, - 1, u"设备列表",(5, 4))
        font = wx.Font(8,wx.DEFAULT, wx.NORMAL, wx.NORMAL)
        titleText.SetFont(font)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(titlePanel,0, wx.Left|wx.Right|wx.EXPAND, 10)
        sizer.Add(self.list, 1, wx.EXPAND)
        parent.SetSizer(sizer)

    #初始化事件绑定
    def initBindEvent(self):
        self.ymNetWorker.dispatcher.addListener(YMEvent.ID_AddClientInfo, self.on_ClientConnectEvent)
        self.ymNetWorker.dispatcher.addListener(YMEvent.ID_RemoveClientInfo, self.on_ClientDisConnEvent)
        self.ymNetWorker.dispatcher.addListener(YMEvent.ID_RefreshClientInfo, self.on_ClientRefreshInfoEvent)

    #刷新列表
    def refreshDevList(self):
        self.list.refreshList(self.ymNetWorker.clientDict)
        #self.list.SetItemState(5, wx.LIST_STATE_SELECTED, wx.LIST_STATE_SELECTED)

    #刷新
    def OnUpdate(self, evt):
        self.ymNetWorker.update()
    #启动App
    def OnBtn_OpenApp(self, evt):
        wx.LogMessage(u"启动App" )

    #关闭App
    def OnBtn_CloseApp(self, evt):
         wx.LogMessage(u"关闭App" )

    #客户端连接
    def on_ClientConnectEvent(self, event):
        clientId = event.getArg("clientId")
        data = self.ymNetWorker.getClientData(clientId)
        self.list.addItem([clientId, data])

    #客户端断开
    def on_ClientDisConnEvent(self, event):
        clientId = event.getArg("clientId")
        self.list.removeItem(clientId)

    #客户端刷新
    def on_ClientRefreshInfoEvent(self, event):
        clientId = event.getArg("clientId")
        data = self.ymNetWorker.getClientData(clientId)
        self.list.refreshItem([clientId,data])
