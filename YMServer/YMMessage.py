#coding=utf-8
#消息类型，定义组装

import json
import struct


class YMPacketHeader:
    SIZE            = 4
    PACKET_SIGN     = 0x0FFB

    def __init__(self):
        self.sign = 0
        self.len = 0

    def getBytes(self):
        return struct.pack("hh",self.sign,self.len)

    def checkSign(self):
        return self.sign == YMPacketHeader.PACKET_SIGN

    def setData(self, data):
        d = struct.unpack_from("hh", str(data))
        self.sign = d[0]
        self.len = d[1]


class YMMessage:
    C_MIN                               = 1000
    C_CheckAlive                        = C_MIN + 1
    C_DeviceInfo                        = C_MIN + 2
    C_OperatorResult 	                = C_MIN + 3

    S_MIN                               = 2000
    S_CheckAlive                        = S_MIN + 1
    S_DeviceInfo                        = S_MIN + 2
    S_DownLoad                          = S_MIN + 3
    S_InstallApp                        = S_MIN + 4
    S_UninstallApp                      = S_MIN + 5
    S_StartApp                          = S_MIN + 6
    S_ReStartApp                        = S_MIN + 7

    def __init__(self, jsonMsg):
        self.jsonMsg = jsonMsg
        self.content = None
        self.id = -1
        self.data = None

    def decode(self):
        try:
            self.content = json.loads(self.jsonMsg)
            self.id = self.content["id"]
        except Exception as ex:
            print(ex)
            return None
        return self.content

    def getBytes(self):
        if self.data != None:
            return self.data
        datalen = len(self.jsonMsg)
        totallen = datalen + YMPacketHeader.SIZE
        header = YMPacketHeader()
        header.sign = YMPacketHeader.PACKET_SIGN
        header.len = totallen
        buf = bytearray(' '*totallen)
        buf[0:YMPacketHeader.SIZE] = bytearray(header.getBytes())
        buf[YMPacketHeader.SIZE:] = bytearray(self.jsonMsg)
        self.data = str(buf)
        return self.data

    @staticmethod
    def Make_S_DeviceInfo(result):
        p = {'id':YMMessage.S_DeviceInfo, 'result':result}
        s = json.dumps(p,ensure_ascii=False)
        return  s

    @staticmethod
    def Make_C_DeviceInfo(deviceId, phoneBrand, phoneModel, version):
        p = {'id':YMMessage.C_DeviceInfo, 'deviceId':deviceId ,'phoneBrand':phoneBrand, 'phoneModel':phoneModel, 'version':version}
        s = json.dumps(p,ensure_ascii=False)
        return  s

    @staticmethod
    def Make_S_CheckAlive():
        p = {'id':YMMessage.S_CheckAlive}
        s = json.dumps(p,ensure_ascii=False)
        return  s

    @staticmethod
    def Make_S_DownLoade(url):
        p = {'id':YMMessage.S_DownLoad, "url":url}
        s = json.dumps(p,ensure_ascii=False)
        return  s
    @staticmethod
    def Make_S_InstallApp(filename):
        p = {'id':YMMessage.S_InstallApp, "filename":filename}
        s = json.dumps(p,ensure_ascii=False)
        return  s
    @staticmethod
    def Make_S_UnInstallApp(packagename):
        p = {'id':YMMessage.S_UninstallApp, "packagename":packagename}
        s = json.dumps(p,ensure_ascii=False)
        return  s
    @staticmethod
    def Make_S_ReStartApp():
        p = {'id':YMMessage.S_ReStartApp}
        s = json.dumps(p,ensure_ascii=False)
        return  s
    @staticmethod
    def Make_S_StartApp(packagename):
        p = {'id':YMMessage.S_StartApp, "packagename":packagename}
        s = json.dumps(p,ensure_ascii=False)
        return  s