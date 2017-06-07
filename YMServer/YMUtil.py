#encoding=utf8
#工具类
import socket


#获取Dict中value（安全判断）
def safe_GeStringValueFromDict(dict, key, default = ""):
    if dict != None and dict.has_key(key):
        return dict[key]
    else:
        return default

#获取指定前缀IP
def getLocalIPByPrefix(prefix):
    localIP = ''
    for ip in socket.gethostbyname_ex(socket.gethostname())[2]:
        if ip.startswith(prefix):
            localIP = ip
    return localIP