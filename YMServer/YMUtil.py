#encoding=utf8
#工具类


#获取Dict中value（安全判断）
def safe_GeStringValueFromDict(dict, key, default = ""):
    if dict != None and dict.has_key(key):
        return dict[key]
    else:
        return default