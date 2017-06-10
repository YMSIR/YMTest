#encoding=utf8
#工具类
import socket
import wx

#获取Dict中value（安全判断）
def safe_GeStringValueFromDict(dict, key, default = ""):
    if dict != None and dict.has_key(key):
        return dict[key]
    else:
        return default

#获取指定前缀IP
def getLocalIPByPrefix(prefix):
    if wx.Platform == "__WXMAC__":
        return getLocalIp()
    else:
        localIP = ''
        for ip in socket.gethostbyname_ex(socket.gethostname())[2]:
            if ip.startswith(prefix):
                localIP = ip
        return localIP

#MAC获取IP地址
def getLocalIp():
    try:
        sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sk.connect(("www.baidu.com", 80))
        return sk.getsockname()[0]
    except:
        return "127.0.0.1"