#encoding=utf-8
#事件定义

class YMEvent:
    ID_ClientConnect    = 1     #客户端连接
    ID_ClientDisConn    = 2     #客户端断开
    ID_Log              = 3     #输出日志
    ID_RefreshClientInfo = 4    #刷新客户端信息
    ID_AddClientInfo    = 5    #刷新客户端信息
    ID_RemoveClientInfo = 6    #刷新客户端信息

    def __init__(self, id):
        self.id = id
        self.args = {}

    #加入参数
    def addArg(self, k, v):
        self.args[k] = v

    #获取参数
    def getArg(self, k):
        return self.args[k]