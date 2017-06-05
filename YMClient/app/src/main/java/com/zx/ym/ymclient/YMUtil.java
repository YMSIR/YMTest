package com.zx.ym.ymclient;

/**
 * Created by zhangxinwei02 on 2017/5/31.
 */

import android.app.ActivityManager;
import android.os.Environment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import android.content.pm.*;

import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import android.net.wifi.*;
import java.io.DataOutputStream;


public class YMUtil {

	// 下载缓存目录
	public static String fileRootPath = Environment.getExternalStorageDirectory() + "/YMClient/" ;
	private final static int kSystemRootStateUnknow = -1;
	private final static int kSystemRootStateDisable = 0;
	private final static int kSystemRootStateEnable = 1;
	private static int systemRootState = kSystemRootStateUnknow;
	private  static boolean isHasRootPermission = false;

	// 获取设备ID
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        if (deviceId == null) {
            return "";
        } else {
            return deviceId;
        }
    }

	// 获取手机品牌
    public static String getPhoneBrand() {
        return android.os.Build.BRAND;
    }

	// 获取手机型号
    public static String getPhoneModel() {
        return android.os.Build.MODEL;
    }

	// 获取SDK
    public static int getBuildLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

	// 获取系统版本
    public static String getBuildVersion() {
        return android.os.Build.VERSION.RELEASE;
    }
	
	// 获取网络类型
    public static String getNetType(Context context) {
    	  String netType = "null";
    	  ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    	  NetworkInfo networkInfo = manager.getActiveNetworkInfo();

    	  if (networkInfo == null) {
    	    return netType;
    	  }
    	  int nType = networkInfo.getType();
    	  if (nType == ConnectivityManager.TYPE_WIFI) {
    	    //WIFI
    	    netType = "wifi";
    	  } else if (nType == ConnectivityManager.TYPE_MOBILE) {
    	    int nSubType = networkInfo.getSubtype();
    	    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    	    //4G
    	    if (nSubType == TelephonyManager.NETWORK_TYPE_LTE
    	        && !telephonyManager.isNetworkRoaming()) {
    	      netType = "4G";
    	    } else if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS || nSubType == TelephonyManager.NETWORK_TYPE_HSDPA || nSubType == TelephonyManager.NETWORK_TYPE_EVDO_0 && !telephonyManager.isNetworkRoaming()) {
    	      netType = "3G";
    	    } else if (nSubType == TelephonyManager.NETWORK_TYPE_GPRS || nSubType == TelephonyManager.NETWORK_TYPE_EDGE || nSubType == TelephonyManager.NETWORK_TYPE_CDMA && !telephonyManager.isNetworkRoaming()) {
    	      netType = "2G";
    	    } else {
    	      netType = "2G";
    	    }
    	  }
    	  return netType;
    	}

	// 日志输出
	public static void log(String message) {
		//Log.i("YMClient", message);
		MainActivity.instance.sendLogEvent(message);
	}

	// 检测目录是否存在
	public static void checkFileRootDir()
	{

		File dir = new File(fileRootPath);
		if (!dir.exists())
		{
			dir.mkdir();
		}
		if (!dir.exists())
		{
			log("xxxxx");
		}

	}

	// 创建文件
	public static File createSDFile(String fileName)  {
		try
		{
			File file = new File(fileRootPath + fileName);
			file.delete();
			file.createNewFile();
			return file;
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	// 判断文件是否存在
	public static boolean isFileExist(String fileName){
		File file = new File(fileRootPath + fileName);
		return file.exists();
	}

	// 获取下载文件名
	public static String geneFileNameFromUrl(String url)
	{
		int index = url.lastIndexOf("/");
		if (index >= 0)
		{
			return url.substring(index + 1);
		}
		else
		{
			return url;
		}
	}

	// 获取IP地址
	public static String getIPAddress(Context context) {
		NetworkInfo info = ((ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
				try {
					//Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
					for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
						NetworkInterface intf = en.nextElement();
						for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
							InetAddress inetAddress = enumIpAddr.nextElement();
							if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
								return inetAddress.getHostAddress();
							}
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
				}

			} else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
				return ipAddress;
			}
		} else {
			//当前无网络连接,请在设置中打开网络
		}
		return null;
	}

	/**
	 * 将得到的int类型的IP转换为String类型
	 */
	public static String intIP2StringIP(int ip) {
		return (ip & 0xFF) + "." +
				((ip >> 8) & 0xFF) + "." +
				((ip >> 16) & 0xFF) + "." +
				(ip >> 24 & 0xFF);
	}

	// 是否ROOT
	public static boolean isRootSystem() {
		if (systemRootState == kSystemRootStateEnable) {
			return true;
		} else if (systemRootState == kSystemRootStateDisable) {
			return false;
		}
		File f = null;
		final String kSuSearchPaths[] = { "/system/bin/", "/system/xbin/",
				"/system/sbin/", "/sbin/", "/vendor/bin/" };
		try {
			for (int i = 0; i < kSuSearchPaths.length; i++) {
				f = new File(kSuSearchPaths[i] + "su");
				if (f != null && f.exists()) {
					systemRootState = kSystemRootStateEnable;
					return true;
				}
			}
		} catch (Exception e) {
		}
		systemRootState = kSystemRootStateDisable;
		return false;
	}

	// 获取ROOT权限
	public static void checkRootPermission()
	{
		if (isRootSystem() == false)
		{
			return;
		}

		int result = -1;
		DataOutputStream dos = null;
		try {
			Process p = Runtime.getRuntime().exec("su");
			dos = new DataOutputStream(p.getOutputStream());
			dos.writeBytes("exit\n");
			dos.flush();
			p.waitFor();
			result = p.exitValue();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (result == 0)
		{
			isHasRootPermission = true;
		}
	}

	// 安装APP -1失败 0成功 1等待
	public static int installAPK(String apkFile) {

		// 安装程序的apk文件路径
		String fileName = fileRootPath + apkFile;
		File file =	new File(fileName);
		if (file.exists() == false)
		{
			return -1;
		}
		if (isHasRootPermission)
		{
			return silentInstall(apkFile);
		}
		else
		{
			// 创建URI
			Uri uri = Uri.fromFile(file);
			// 创建Intent意图
			Intent intent = new Intent(Intent.ACTION_VIEW);
			// 设置Uri和类型
			intent.setDataAndType(uri, "application/vnd.android.package-archive");
			// 执行意图进行安装
			MainActivity.instance.startActivity(intent);
			return 1;
		}
	}

	// 卸载APP -1失败 0成功 1等待
	public static int uninstallAPK(String packageName) {

		if (isHasRootPermission)
		{
			return silentUninstall(packageName);
		}
		else
		{
			// 通过程序的报名创建URI
			Uri packageURI = Uri.parse("package:" + packageName);
			// 创建Intent意图
			Intent intent = new Intent(Intent.ACTION_DELETE);
			intent.setData(packageURI);
			// 执行卸载程序
			MainActivity.instance.startActivity(intent);
			return 1;
		}
	}

	// 包名是否存在
	public static boolean isAvilible(String packageName) {
		PackageManager packageManager = MainActivity.instance.getPackageManager();

		//获取手机系统的所有APP包名，然后进行一一比较
		List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
		for (int i = 0; i < pinfo.size(); i++) {
			if (((PackageInfo) pinfo.get(i)).packageName
					.equalsIgnoreCase(packageName))
				return true;
		}
		return false;
	}

	// 启动APP
	public static boolean startAPP(String packageName)
	{
		if (!isAvilible(packageName))
		{
			return false;
		}
		try {
			PackageManager packageManager = MainActivity.instance.getPackageManager();
			Intent intent = packageManager.getLaunchIntentForPackage(packageName);
			MainActivity.instance.startActivity(intent);
			return true;
		} catch (Exception e) {
			log(e.getMessage());
		}
		return false;
	}

	public static boolean closeAPP(String packageName)
	{
		ActivityManager manager = (ActivityManager) MainActivity.instance.getSystemService(Context.ACTIVITY_SERVICE);
		manager.killBackgroundProcesses(packageName);
		return true;
	}

	// 重启APP
	public static boolean restartAPP()
	{
		Intent intent = MainActivity.instance.getPackageManager()
				.getLaunchIntentForPackage(MainActivity.instance.getPackageName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		MainActivity.instance.startActivity(intent);
		return true;
	}

	// 静默安装
	public static int silentInstall(String apkFile) {
		String apkAbsolutePath = fileRootPath + apkFile;
		String cmd = "pm install -r " + apkAbsolutePath;
		int result = -1;
		DataOutputStream dos = null;

		try {
			Process p = Runtime.getRuntime().exec("su");
			dos = new DataOutputStream(p.getOutputStream());
			dos.writeBytes(cmd + "\n");
			dos.flush();
			dos.writeBytes("exit\n");
			dos.flush();
			p.waitFor();
			result = p.exitValue();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	// 静默卸载
	public static int silentUninstall(String packageName) {
		String cmd = "pm uninstall -k " + packageName;
		int result = -1;
		DataOutputStream dos = null;

		try {
			Process p = Runtime.getRuntime().exec("su");
			dos = new DataOutputStream(p.getOutputStream());
			dos.writeBytes(cmd + "\n");
			dos.flush();
			dos.writeBytes("exit\n");
			dos.flush();
			p.waitFor();
			result = p.exitValue();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

}
