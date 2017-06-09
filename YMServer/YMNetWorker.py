#coding=utf-8
#网路模块处理消息

import Queue
import threading
import time

from YMNetThread import YMThreadArgs as YMThreadArgs
from YMNetThread import YMNetThread as YMNetThread
from YMMessage import YMMessage as YMMessage
from YMDispatcher import YMDispatcher as YMDispatcher
from YMEvent import  YMEvent as YMEvent
from YMDoubleQueue import YMDoubleQueue as YMDoubleQueue

class YMNetWorker:
    #初始化
    def __init__(self, ip, port, log):
        #网络处理相关
        self.sendMessageQueue = Queue.Queue()
        self.recvMessageQueue = Queue.Queue()
        self.lockSendQueue = threading.Lock()
        self.lockRecvQueue = threading.Lock()
        self.dispatcher = YMDispatcher()
        self.log = log

        #线程参数
        self.threadArgs = YMThreadArgs()
        self.threadArgs.ip = ip
        self.threadArgs.port = port
        self.threadArgs.popSendMessage = self.popSendMessageQueue
        self.threadArgs.putRecvMessage = self.putRecvMessageQueue
        self.threadArgs.dispatcher = self.dispatcher

        #客户端数据
        self.clientDict = {}
        self.dispatcher.addListener(YMEvent.ID_ClientConnect, self.on_ClientConnectEvent)
        self.dispatcher.addListener(YMEvent.ID_ClientDisConn, self.on_ClientDisConnEvent)
        self.dispatcher.addListener(YMEvent.ID_Log, self.on_LogEvent)
        self.dispatcher.addListener(YMMessage.C_DeviceInfo, self.on_C_DevInfo)
        self.dispatcher.addListener(YMMessage.C_CheckAlive, self.on_C_CheckAlive)
        self.dispatcher.addListener(YMMessage.C_OperatorResult, self.on_C_OperatorResult)
    #启动
    def start(self):
        self.workThread = YMNetThread(self.threadArgs)
        self.workThread.start()
    #停止
    def stop(self):
        self.workThread.stop()

    #入队发送消息
    def putSendMessageQueue(self, message):
        self.lockSendQueue.acquire()
        self.sendMessageQueue.put(message)
        self.lockSendQueue.release()

    #出队发送消息
    def popSendMessageQueue(self):
        if self.sendMessageQueue.empty() == True:
            return None
        self.lockSendQueue.acquire()
        message = self.sendMessageQueue.get()
        self.lockSendQueue.release()
        return message

    #入队接受消息
    def putRecvMessageQueue(self, message):
        self.lockRecvQueue.acquire()
        self.recvMessageQueue.put(message)
        self.lockRecvQueue.release()

    #出队接受消息
    def popRecvMessageQueue(self):
        if self.recvMessageQueue.empty() == True:
            return None
        self.lockRecvQueue.acquire()
        message = self.recvMessageQueue.get()
        self.lockRecvQueue.release()
        return message

    #获取客户端数据
    def getClientData(self, id):
        if self.clientDict.has_key(id):
            return self.clientDict[id]
        else:
            return None

    #加入客户端数据
    def addClientData(self, id, data):
        if self.clientDict.has_key(id) == False:
            self.clientDict[id] = data

    #移除客户端数据
    def removeClentData(self, id):
        del self.clientDict[id]

    #发送消息
    def sendMessage(self, clientId, message):
        self.putSendMessageQueue([clientId, message])

    #刷新处理
    def update(self):
        self.dispatcher.update()
        self.handleMessage()

    #处理消息
    def handleMessage(self):
        data = self.popRecvMessageQueue()
        while data != None:
            try:
                clientId = data[0]
                message = data[1]
                self.hand_Message(clientId, message)
            except Exception as ex:
                self.log(u"消息JSON格式错误")
            data = self.popRecvMessageQueue()

    #派发消息
    def hand_Message(self, clientId, message):
        netEvent = YMEvent(message.id)
        netEvent.addArg("clientId",clientId)
        netEvent.addArg("message",message)
        self.dispatcher.dispatchEvent(netEvent)

    #客户端连接
    def on_ClientConnectEvent(self, event):
        clientId = event.getArg("clientId")
        ip = event.getArg("ip")
        data = self.getClientData(clientId)
        if data == None:
            data = {"ip":ip}
            self.addClientData(clientId,data)
            event = YMEvent(YMEvent.ID_AddClientInfo)
            event.addArg("clientId", clientId)
            self.dispatcher.dispatchEvent(event)
        else:
            data["ip"] = ip
            event = YMEvent(YMEvent.ID_RefreshClientInfo)
            event.addArg("clientId", clientId)
            self.dispatcher.dispatchEvent(event)
        self.log(u'on_ClientConnectEvent client Id: %d ' % clientId)

    #客户端断开
    def on_ClientDisConnEvent(self, event):
        clientId = event.getArg("clientId")
        self.removeClentData(clientId)
        event = YMEvent(YMEvent.ID_RemoveClientInfo)
        event.addArg("clientId", clientId)
        self.dispatcher.dispatchEvent(event)
        self.log(u'on_ClientDisConnEvent client Id: %d ' % clientId)

    #输出日志
    def on_LogEvent(self, event):
        message = event.getArg("log")
        self.log(message)

    #心跳
    def on_C_CheckAlive(self, event):
        clientId = event.getArg("clientId")
        message = event.getArg("message")
        jsonMsg = YMMessage.Make_S_CheckAlive()
        self.sendMessage(clientId, YMMessage(jsonMsg))
        #self.log(u'on_C_DevInfo client Id: %d Message:%s ' % (clientId ,message.jsonMsg))

    #APP设备信息
    def on_C_DevInfo(self, event):
        clientId = event.getArg("clientId")
        message = event.getArg("message")
        content = message.content
        #self.log(u'on_C_DevInfo client Id: %d Message:%s ' % (clientId ,message.jsonMsg))
        deviceId = content["deviceId"]
        phoneBrand = content["phoneBrand"]
        phoneModel = content["phoneModel"]
        version = content["version"]
        data = self.getClientData(clientId)
        if data == None:
            data = {'deviceId':deviceId ,'phoneBrand':phoneBrand, 'phoneModel':phoneModel, 'version':version}
            self.addClientData(clientId,data)
            event = YMEvent(YMEvent.ID_AddClientInfo)
            event.addArg("clientId", clientId)
            self.dispatcher.dispatchEvent(event)
        else:
            data["deviceId"] = deviceId
            data["phoneBrand"] = phoneBrand
            data["phoneModel"] = phoneModel
            data["version"] = version
            event = YMEvent(YMEvent.ID_RefreshClientInfo)
            event.addArg("clientId", clientId)
            self.dispatcher.dispatchEvent(event)

        jsonMsg = YMMessage.Make_S_DeviceInfo(1)
        self.sendMessage(clientId, YMMessage(jsonMsg))

    #操作结果
    def on_C_OperatorResult(self, event):
        clientId = event.getArg("clientId")
        message = event.getArg("message")
        content = message.content
        result = content["result"]
        data = self.getClientData(clientId)
        if data != None:
            data["message"] = result
            event = YMEvent(YMEvent.ID_RefreshClientInfo)
            event.addArg("clientId", clientId)
            self.dispatcher.dispatchEvent(event)

    def downLoadFile(self,clientId, url):
        msg = YMMessage.Make_S_DownLoade(url)
        self.sendMessage(clientId, YMMessage(msg))

    def installApp(self,clientId, filename):
        msg = YMMessage.Make_S_InstallApp(filename)
        self.sendMessage(clientId, YMMessage(msg))

    def uninstallApp(self,clientId, packagename):
        msg = YMMessage.Make_S_UnInstallApp(packagename)
        self.sendMessage(clientId, YMMessage(msg))

    def restartApp(self,clientId):
        msg = YMMessage.Make_S_ReStartApp()
        self.sendMessage(clientId, YMMessage(msg))

    def startApp(self,clientId, packagename):
        msg = YMMessage.Make_S_StartApp(packagename)
        self.sendMessage(clientId, YMMessage(msg))

