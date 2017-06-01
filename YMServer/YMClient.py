#coding=utf-8
#客户端测试

import socket
import sys
import json
import time
import threading
from YMMessage import YMMessage as YMMessage
from YMMessage import YMPacketHeader as YMPacketHeader

class YMClient(threading.Thread):
    SELECT_TIMEOUT = 0.01
    #初始化
    def __init__(self, ip, port, name):
        threading.Thread.__init__(self)
        self.ip = ip
        self.port = port
        self.name = name
        jsonMsg =  YMMessage.Make_C_DeviceInfo(name,"huawei","x1","anroid5.0")
        self.message = YMMessage(jsonMsg)
        self.isRun = False

    #启动连接
    def start(self):
        threading.Thread.start(self)

    #停止连接
    def stop(self):
        self.isRun = False
        self.join()

    #线程循环
    def run(self):
        try:
            while 1:
                self.sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                res = self.sk.connect_ex((self.ip, self.port))
                if res == 0:
                    self.isRun = True
                    break
                else:
                    time.sleep(1)
            while self.isRun:
                self.sk.sendall(self.message.getBytes())
                self.sk.sendall(self.message.getBytes())
                data = self.sk.recv(1024)
                header = YMPacketHeader()
                header.setData(data)
                bytes = bytearray(data)
                jsonMsg = (str)(bytes[YMPacketHeader.SIZE:header.len])
                a = jsonMsg.decode("utf-8")
                print(jsonMsg + "\n")
                time.sleep(1)
                #self.isRun = False
        except Exception as ex:
            print(ex)


if __name__ == "__main__":
    reload(sys)
    sys.setdefaultencoding('utf8')
    localIP = socket.gethostbyname(socket.gethostname())
    clients = []
    for i in range(1, 2):
        client = YMClient(localIP, 8001, "iphone" + str(i))
        client.start()
        clients.append(client)
        print("socket " +str(i))
    s = input("input any word exit\n")
    for client in clients:
        client.stop()

