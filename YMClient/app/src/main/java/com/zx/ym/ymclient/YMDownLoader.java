package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class YMDownLoader {
    /** 连接url */
    private String _urlstr;
    /** http连接管理类 */
    private HttpURLConnection _urlcon;

    public YMDownLoader(String url)
    {
        _urlstr = url;
        _urlcon = getConnection();
    }

    /*
     * 读取网络文本
     */
    public String downloadAsString()
    {
        StringBuilder sb = new StringBuilder();
        String temp = null;
        try {
            InputStream is = _urlcon.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((temp = br.readLine()) != null) {
                sb.append(temp);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /*
     * 获取http连接处理类HttpURLConnection
     */
    private HttpURLConnection getConnection()
    {
        URL url;
        HttpURLConnection urlcon = null;
        try {
            url = new URL(_urlstr);
            urlcon = (HttpURLConnection) url.openConnection();
            int resCode =  urlcon.getResponseCode();
            if(resCode == 200)
                return urlcon;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * 获取连接文件长度。
     */
    public int getLength()
    {
        return _urlcon.getContentLength();
    }

    /*
     * 写文件到sd卡 demo
     * 前提需要设置模拟器sd卡容量，否则会引发EACCES异常
     * 先创建文件夹，在创建文件
     */
    public int down2sd(String filename, YMTask task)
    {
        File file =  YMUtil.createSDFile(filename);
        FileOutputStream fos = null;
        try {
            InputStream is = _urlcon.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            while ((is.read(buf)) != -1) {
                fos.write(buf);
                task.progress = buf.length / getLength() * 100;
            }
            is.close();
        } catch (Exception e) {
            return 0;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }
}
