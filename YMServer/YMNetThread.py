#coding=utf-8
#处理客户端的连接，收发消息

import threading
import socket
import select
import time
from YMEvent import YMEvent as YMEvent
from YMMessage import YMMessage as YMMessage
from YMMessage import YMPacketHeader as YMPacketHeader
#线程初始化参数
class YMThreadArgs:
    ip = "127.0.0.1"
    port = 8001
    popSendMessage = None
    putRecvMessage = None
    dispatcher = None
    HEART_TIMEOUT = 60

#网络线程
class YMNetThread(threading.Thread):
    SELECT_TIMEOUT = 0.05
    SEND_BUF_SIZE = 16*1024
    RECV_BUF_SIZE = 16*1024

    #初始化
    def __init__(self, args):
        threading.Thread.__init__(self)
        self.isRun = False
        self.args = args
        self.socketList = []
        self.connDict = {}
        self.recvBuffer = {}

    #启动线程
    def start(self):
        threading.Thread.start(self)

    #退出线程
    def stop(self):
        self.isRun = False
        self.join()

    #初始化监听Socket
    def listen(self):
        try:
            self.listenSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            #self.listenSocket.settimeout(0.01)
            self.listenSocket.bind((self.args.ip, self.args.port))
            self.listenSocket.listen(50)
            self.socketList.append(self.listenSocket)
            self.isRun = True
        except Exception as ex:
             self.log(str(ex))

    #线程循环
    def run(self):
        self.listen()
        while self.isRun:
            r_list, w_list, e_list = select.select(self.socketList,[],self.socketList, YMNetThread.SELECT_TIMEOUT)
            #接受连接，数据
            for sk in r_list:
                if sk == self.listenSocket:
                    s, ip = sk.accept()
                    self.addConnInfo(s, ip)
                else:
                    self.recvData(sk)
            #发送数据
            self.sendData()
            #断开连接
            for sk in e_list:
                self.removeConnInfo(sk)
            self.checkClientState()
            time.sleep(0.1)

    #是否存在ID
    def isExistClientId(self, id):
        isExist = False
        for k,v in self.connDict.items():
            if v[0] == id:
                isExist = True
                break
        return isExist

    #生成唯一ID
    def geneUniqueId(self):
        id = 1
        while self.isExistClientId(id):
            id = id + 1
        return id

    #加入客户端连接信息
    def addConnInfo(self, sk, ip):
        if not self.connDict.has_key(sk):
            sk.setsockopt(socket.SOL_TCP, socket.TCP_NODELAY, 1)
            sk.setsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF, YMNetThread.SEND_BUF_SIZE)
            sk.setsockopt(socket.SOL_SOCKET,socket.SO_RCVBUF, YMNetThread.RECV_BUF_SIZE)
            id = self.geneUniqueId()
            self.connDict[sk] = [id, time.time()]
            self.socketList.append(sk)
            self.recvBuffer[sk] = [bytearray(" "*YMNetThread.RECV_BUF_SIZE), 0]
            event = YMEvent(YMEvent.ID_ClientConnect)
            event.addArg("clientId", id)
            event.addArg("ip",ip[0])
            self.args.dispatcher.dispatchToMainThread(event)

    #删除客户端连接信息
    def removeConnInfo(self, s):
        if self.connDict.has_key(s):
            event = YMEvent(YMEvent.ID_ClientDisConn)
            event.addArg("clientId",self.connDict[s][0])
            self.args.dispatcher.dispatchToMainThread(event)
            del self.connDict[s]
            del self.recvBuffer[s]
            self.socketList.remove(s)

    #通过Id获取连接信息
    def getSocketById(self, id):
        for k, v in self.connDict.items():
            if v[0] == id:
                return k
        return None

    #通过Socket获取连接信息
    def getConnInfoBySocket(self, s):
        if  self.connDict.has_key(s):
            return self.connDict[s]
        else:
            return None

    #刷新接受消息时间
    def updateConnInfoRecvTime(self,sk):
        conninfo = self.getConnInfoBySocket(sk)
        if conninfo != None:
            conninfo[1] = time.time()

    #检测客户端是否断线
    def checkClientState(self):
        curTime = time.time()
        for k, v in self.connDict.items():
            if curTime -  v[1] > 2 * YMThreadArgs.HEART_TIMEOUT:
                self.removeConnInfo(k)


    #主线程输出日志
    def log(self, message):
        print(message)
        event = YMEvent(YMEvent.ID_Log)
        event.addArg("log", message)
        self.args.dispatcher.dispatchToMainThread(event)

    #接受数据
    def recvData(self, sk):
        try:
            data = sk.recv(YMNetThread.RECV_BUF_SIZE / 2)
            recvSize = len(data)
            if recvSize > 0:
                buffer = self.recvBuffer[sk][0]
                offset = self.recvBuffer[sk][1]
                buffer[offset:offset + recvSize] = bytearray(data)
                offset = offset + recvSize
                offsetMinusHeader = offset - YMPacketHeader.SIZE
                while offsetMinusHeader >= 0:
                    header = YMPacketHeader()
                    header.setData(buffer)
                    if header.checkSign() == True:
                        offsetMinusMessageLen = offset - header.len
                        if offsetMinusMessageLen >= 0:
                            jsonMsg = str(buffer[header.SIZE:header.len])
                            buffer[0:offsetMinusMessageLen] = buffer[header.len:offset]
                            offset = offsetMinusMessageLen
                            self.recvBuffer[sk][1] = offset
                            message = YMMessage(jsonMsg)
                            message.decode()
                            self.putRecvMessage(sk, message)
                        else:
                            break
                    else:
                        buffer[0:offsetMinusHeader] =  buffer[YMPacketHeader.SIZE:offset]
                        self.recvBuffer[sk][1] = offsetMinusHeader
                        break
                self.updateConnInfoRecvTime(sk)
            else:
                self.removeConnInfo(sk)
                print("recv null disconnect")

        except Exception as ex:
            self.removeConnInfo(sk)
            self.log("recv:" + str(ex))

    #向客户端发送数据
    def sendData(self):
        data = self.args.popSendMessage()
        while data != None:
            clientId = data[0]
            message = data[1]
            sk = self.getSocketById(clientId)
            if sk != None:
                try:
                    sk.sendall(message.getBytes())
                    print("send:" + message.jsonMsg)
                except Exception as ex:
                    print(ex)
            data = self.args.popSendMessage()

    #加入接受消息
    def putRecvMessage(self,sk, message):
        clientinfo = self.getConnInfoBySocket(sk)
        if clientinfo != None:
             self.args.putRecvMessage([clientinfo[0],message])


