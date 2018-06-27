package com.tencent.ilivesdk.core;

import android.util.Log;

import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.core.impl.ILVBLog;

import java.util.LinkedList;

/**
 * 日志输出
 */
public class ILiveLog {
    public enum TILVBLogLevel {
        OFF,
        ERROR,
        WARN,
        INFO,
        DEBUG
    }

    static public class LogExts {
        private LinkedList<String> listExts = new LinkedList<>();

        public LogExts put(String key, String value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, int value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, long value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, float value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, double value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, boolean value) {
            listExts.add(key + ":" + value);
            return this;
        }

        public LogExts put(String key, Object value) {
            listExts.add(key + ":" + (null==value ? "null" : value.toString()));
            return this;
        }

        public String getExtsInfo() {
            String exts = null;
            for (String ext : listExts) {
                exts = (null == exts ? "[" + ext : exts + "|" + ext);
            }
            return exts + "]";
        }
    }

    static private TILVBLogLevel level = TILVBLogLevel.DEBUG;

    static public String[] getStringValues() {
        TILVBLogLevel[] levels = TILVBLogLevel.values();
        String[] stringValuse = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            stringValuse[i] = levels[i].toString();
        }
        return stringValuse;
    }

    /**
     * 设置写文件的日志级别
     *
     * @param newLevel
     */
    static public void setLogLevel(TILVBLogLevel newLevel) {
        level = newLevel;
        w("Log", "change log level: " + newLevel);
    }

    /**
     * 获取日志等级
     */
    static public TILVBLogLevel getLogLevel() {
        return level;
    }

    /**
     * 打印INFO级别日志
     * @param strTag TAG
     * @param strInfo  消息
     */
    public static void v(String strTag, String strInfo) {
        if (level.ordinal() >= TILVBLogLevel.DEBUG.ordinal()) {
            Log.v(strTag, strInfo);
            ILVBLog.writeLog(strInfo, null);
        }
    }

    /**
     * 打印INFO级别日志
     * @param strTag TAG
     * @param strInfo  消息
     */
    public static void i(String strTag, String strInfo) {
        if (level.ordinal() >= TILVBLogLevel.INFO.ordinal()) {
            Log.i(strTag, strInfo);
            ILVBLog.writeLog(strInfo, null);
        }
    }

    /**
     * 打印DEBUG级别日志
     *
     * @param strTag  TAG
     * @param strInfo 消息
     */
    public static void d(String strTag, String strInfo) {
        if (level.ordinal() >= TILVBLogLevel.DEBUG.ordinal()) {
            Log.d(strTag, strInfo);
            ILVBLog.writeLog(strInfo, null);
        }
    }

    /**
     * 打印WARN级别日志
     *
     * @param strTag  TAG
     * @param strInfo 消息
     */
    public static void w(String strTag, String strInfo) {
        if (level.ordinal() >= TILVBLogLevel.WARN.ordinal()) {
            Log.w(strTag, strInfo);
            ILVBLog.writeLog(strInfo, null);
        }
    }

    /**
     * 打印ERROR级别日志
     *
     * @param strTag  TAG
     * @param strInfo 消息
     */
    public static void e(String strTag, String strInfo) {
        if (level.ordinal() >= TILVBLogLevel.ERROR.ordinal()) {
            Log.e(strTag, strInfo);
            ILVBLog.writeLog(strInfo, null);
        }
    }

    /**
     * 打印异常堆栈信息
     *
     * @param strTag
     * @param strInfo
     * @param tr
     */
    public static void writeException(String strTag, String strInfo, Exception tr) {
        ILVBLog.writeLog(strInfo, tr);
    }

    /**
     * 关键日志打印
     */
    // debug
    public static void kd(String strTag, String strMsg) {
        d(strTag, "[" + ILiveConstants.LOG_KEY + "][D][" + strTag + "][" + strMsg + "]");
    }

    public static void kd(String strTag, String strMsg, LogExts exts) {
        d(strTag, "[" + ILiveConstants.LOG_KEY + "][D][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // info
    public static void ki(String strTag, String strMsg) {
        i(strTag, "[" + ILiveConstants.LOG_KEY + "][I][" + strTag + "][" + strMsg + "]");
    }

    public static void ki(String strTag, String strMsg, LogExts exts) {
        i(strTag, "[" + ILiveConstants.LOG_KEY + "][I][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // warn
    public static void kw(String strTag, String strMsg) {
        w(strTag, "[" + ILiveConstants.LOG_KEY + "][W][" + strTag + "][" + strMsg + "]");
    }

    public static void kw(String strTag, String strMsg, LogExts exts) {
        w(strTag, "[" + ILiveConstants.LOG_KEY + "][W][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // error
    public static void ke(String strTag, String strMsg, String strModule, int errCode, String errMsg) {
        e(strTag, "[" + ILiveConstants.LOG_KEY + "][E][" + strTag + "][" + strMsg + "]" + new LogExts()
                .put("module", strModule)
                .put("errCode", errCode)
                .put("errMsg", errMsg)
                .getExtsInfo());
    }

    public static void ke(String strTag, String strMsg, String strModule, int errCode, String errMsg, LogExts exts) {
        w(strTag, "[" + ILiveConstants.LOG_KEY + "][E][" + strTag + "][" + strMsg + "]" + exts
                .put("module", strModule)
                .put("errCode", errCode)
                .put("errMsg", errMsg)
                .getExtsInfo());
    }

    /**
     * 开发日志打印
     */
    // debug
    public static void dd(String strTag, String strMsg) {
        d(strTag, "[" + ILiveConstants.LOG_DEV + "][D][" + strTag + "][" + strMsg + "]");
    }

    public static void dd(String strTag, String strMsg, LogExts exts) {
        d(strTag, "[" + ILiveConstants.LOG_DEV + "][D][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // info
    public static void di(String strTag, String strMsg) {
        i(strTag, "[" + ILiveConstants.LOG_DEV + "][I][" + strTag + "][" + strMsg + "]");
    }

    public static void di(String strTag, String strMsg, LogExts exts) {
        i(strTag, "[" + ILiveConstants.LOG_DEV + "][I][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // warn
    public static void dw(String strTag, String strMsg) {
        w(strTag, "[" + ILiveConstants.LOG_DEV + "][W][" + strTag + "][" + strMsg + "]");
    }

    public static void dw(String strTag, String strMsg, LogExts exts) {
        w(strTag, "[" + ILiveConstants.LOG_DEV + "][W][" + strTag + "][" + strMsg + "]" + exts.getExtsInfo());
    }

    // error
    public static void de(String strTag, String strMsg, String strModule, int errCode, String errMsg) {
        w(strTag, "[" + ILiveConstants.LOG_DEV + "][E][" + strTag + "][" + strMsg + "]" + new LogExts()
                .put("module", strModule)
                .put("errCode", errCode)
                .put("errMsg", errMsg)
                .getExtsInfo());
    }

    public static void de(String strTag, String strMsg, String strModule, int errCode, String errMsg, LogExts exts) {
        e(strTag, "[" + ILiveConstants.LOG_DEV + "][E][" + strTag + "][" + strMsg + "]" + exts
                .put("module", strModule)
                .put("errCode", errCode)
                .put("errMsg", errMsg)
                .getExtsInfo());
    }
}
