package com.tencent.ilivesdk;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Base64;

import com.tencent.ilivesdk.core.ILiveLog;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 通用方法类
 */
public class ILiveFunc {
    private final static String TAG = "ILVB-Func";

    static public long getCurrentSec(){
        return System.currentTimeMillis() / 1000;
    }
    static public int generateAVCallRoomID(){
        Random random = new Random();
        long uTime = getCurrentSec();
//        int roomid = (int)((uTime - 1465264500)*100 + random.nextInt(100));
        int roomid = (int)(uTime*100%(Integer.MAX_VALUE - 100) + random.nextInt(100));
        return roomid;
    }

    public static void notifySuccess(ILiveCallBack callBack, Object data){
        if (null != callBack){
            callBack.onSuccess(data);
        }
    }

    public static void notifyError(ILiveCallBack callBack, String module, int errCode, String errMsg){
        if (null != callBack){
            callBack.onError(module, errCode, errMsg);
        }
    }

    public static void notifyMainSuccess(final ILiveCallBack callBack, final Object data){
        if (null != callBack){
            ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    notifySuccess(callBack, data);
                }
            }, 0);
        }
    }

    public static void notifyMainError(final ILiveCallBack callBack, final String module, final int errCode, final String errMsg){
        if (null != callBack){
            ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    notifyError(callBack, module, errCode, errMsg);
                }
            }, 0);
        }
    }

    public static String getArrStr(String[] array) {
        String strRet = "";
        if (null != array) {
            for (String data : array) {
                strRet += (data + ",");
            }
        }
        return strRet;
    }

    public static String getListStr(List<String> list) {
        String strRet = "";
        if (null != list) {
            for (String data : list) {
                strRet += (data + ",");
            }
        }
        return strRet;
    }

    public static String getExceptionInfo(Exception e){
        String info = e.toString();

        StackTraceElement[] eles = e.getStackTrace();
        if (null != eles){
            for (int i=0; i<eles.length; i++){
                info = info + "\n\tat "
                        + eles[i].getClassName() + "." + eles[i].getMethodName() + "("
                        + eles[i].getFileName() + ":" + eles[i].getLineNumber() + ")";
            }
        }
        return info;
    }

    /**
     * 计算SHA值
     * @param strSource 源字符串
     * @return
     * @throws Exception
     */
    public static String getHmacSHA1(String secretKey, String strSource) throws Exception{
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secret = new SecretKeySpec(secretKey.getBytes("UTF-8"), mac.getAlgorithm());
        mac.init(secret);

        byte[] sh1bytes = mac.doFinal(strSource.getBytes("UTF-8"));
        return Base64.encodeToString(sh1bytes, Base64.NO_WRAP);     // 避免自动添加换行符
    }

    public static String getMD5(String sourceStr, boolean bTotal){
        try{
            String result = "";
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset=0; offset<b.length; offset++){
                i = b[offset];
                if (i<0)
                    i+=256;
                if (i<16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
            if (bTotal)
                return result;
            else
                return result.substring(8, 24);
        }catch (Exception e){
            return sourceStr;
        }
    }

    /**
     * 计算云图sign
     * @param secretKey
     * @param path
     * @return
     */
    public static String getSignature(String secretKey, String path){
        String srcStr = "GET";
        try {
            int start = path.indexOf("//") + 2;
            int pos = path.indexOf("?") + 1;
            if (start < 2)
                start = 0;
            if (pos < 1)
                return "";
            // 添加路径
            srcStr += path.substring(start, pos);
            // 分割参数
            List<String> params = Arrays.asList(path.substring(pos).split("&"));
            Comparator<String> comparator = new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            };
            Collections.sort(params, comparator);
            for (String param : params) {
                srcStr += (param + '&');
            }
            srcStr = srcStr.substring(0, srcStr.length() - 1);
            ILiveLog.dd(TAG, "getSignature", new ILiveLog.LogExts().put("src", srcStr));
            return getHmacSHA1(secretKey, srcStr);
        }catch (Exception e){
            ILiveLog.dw(TAG, "getSignature", new ILiveLog.LogExts().put("exception", e.toString()));
            return "";
        }
    }

    public static String byte2HexStr(byte[] bs){
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        for (int i=0; i<bs.length; i++){
            sb.append(chars[(bs[i]&0xF0)>>4]);
            sb.append(chars[bs[i]&0x0F]);
        }
        return sb.toString();
    }

    public static String getABI(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            return Build.CPU_ABI;
        }else{
            return Build.SUPPORTED_ABIS[0];
        }
    }

    // 判断是否平板
    public static boolean isTableDevice(Context context){
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // 判断是否横屏
    public static boolean isLandScape(Context context){
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    // 添加角度偏移
    public static int offsetRotation(int angle, int offset){
        for (;offset < 0;){
            offset = 360 + offset;
        }
        return (angle + offset) % 360;
    }

    // 获取角度
    public static int getRotationAngle(int rotation){
        rotation = rotation % 360;
        switch (rotation) {
            case 270: // 270 degree
                return 1;
            case 180: // 180 degree
                return 2;
            case 90: // 90 degree
                return  3;
            default:// 0 degree
                return  0;
        }
    }
}
