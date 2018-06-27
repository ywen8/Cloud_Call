package com.tencent.ilivesdk.tools.log;

import android.content.Context;
import android.os.Environment;

import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.ILiveHttpEngine;
import com.tencent.ilivesdk.adapter.normal_impl.NormalHttpCtrl;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.data.ILiveHttpConfig;
import com.tencent.ilivesdk.data.ILiveHttpReq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */

public class LogUploader {

    private static final String TAG = LogUploader.class.getSimpleName();

    private static final String sigUrl = "http://avc.qcloud.com/log/appsign.php";
    private static final String reportUrl = "http://avc.qcloud.com/log/report.php";
    public static final int ERR_ERR_PARAM = 8101;
    public static final int ERR_FILE_NOT_EXIS = 8102;
    public static final int ERR_ZIP_FAILED = 8103;
    public static final int ERR_SIGN_FAILED = 8104;
    public static final int ERR_PARSE_FAILED = 8105;
    public static final int ERR_UPLOAD_FAILED = 8106;
    public static final int ERR_FINISH_FAILED = 8107;

    public static class LogParams {
        private Context context;    // 上下文
        private int appid;          // appid
        private int dayOffset;      // 日期偏移
        private String userId;      // 用户id
        private String desc;        // 描述

        public LogParams() {
            context = ILiveSDK.getInstance().getAppContext();
            appid = ILiveSDK.getInstance().getAppId();
            dayOffset = 0;
            userId = ILiveLoginManager.getInstance().getMyUserId();
            desc = "ILiveSDK";
        }

        public LogParams setContext(Context ctx) {
            context = ctx;
            return this;
        }

        public LogParams setAppId(int sdkAppId) {
            appid = sdkAppId;
            return this;
        }

        public LogParams setDayOffset(int iDayOffset) {
            dayOffset = iDayOffset;
            return this;
        }

        public LogParams setUserId(String id) {
            userId = id;
            return this;
        }

        public LogParams setDesc(String strDesc) {
            desc = strDesc;
            return this;
        }

        public Context getContext() {
            return context;
        }

        public int getDayOffset() {
            return dayOffset;
        }

        public int getAppid() {
            return appid;
        }

        public String getUserId() {
            return userId;
        }

        public String getDesc() {
            return desc;
        }
    }

    private static LogUploader instance = new LogUploader();
    private String logKey = "";
    private ILiveHttpEngine httpEngine;


    private LogUploader() {
        httpEngine = new NormalHttpCtrl();
        httpEngine.init(new ILiveHttpConfig().set_thread_numbers(10));
    }

    public static LogUploader getInstance() {
        return instance;
    }


    private void parseUploadRsp(LogParams params, String rspData, final ILiveCallBack<String> callBack) {
        try {
            JSONObject resObj = new JSONObject(rspData);
            CosRes res = new CosRes();
            res.code = resObj.getInt("code");
            JSONObject data = resObj.getJSONObject("data");
            res.source_url = data.getString("source_url");
            JSONObject report = new JSONObject();
            report.put("appid", ILiveSDK.getInstance().getAppId());
            report.put("sdkappid", ILiveSDK.getInstance().getAppId());
            JSONObject reportData = new JSONObject();
            reportData.put("fileurl", res.source_url);
            reportData.put("desc", params.getDesc());
            reportData.put("userid", params.getUserId());
            reportData.put("logkey", logKey);
            reportData.put("file_list", getFileNames(params.getContext(), params.getDayOffset()));
            report.put("data", reportData);
            ILiveLog.dd(TAG, "start", new ILiveLog.LogExts().put("cosUrl", res.source_url));

            ILiveHttpReq req = new ILiveHttpReq().set_url(reportUrl)
                    .set_data(report.toString().getBytes("utf-8"));
            httpEngine.asyncPost(req, new ILiveCallBack<String>(){
                @Override
                public void onSuccess(String data) {
                    try {
                        JSONObject resObj = new JSONObject(data);
                        int retcode = resObj.getInt("retcode");
                        if (ILiveConstants.NO_ERR == retcode)
                            ILiveFunc.notifySuccess(callBack, logKey);
                        else
                            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, retcode, "report fail");
                    } catch (JSONException e) {
                        ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_SDK_FAILED, e.toString());
                    }
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    ILiveFunc.notifyError(callBack, module, errCode, errMsg);
                }
            });
        } catch (Exception e) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_SDK_FAILED, e.toString());
        }
    }

    private void upload(final LogParams params, String data, final ILiveCallBack<String> callBack) {
        try {
            JSONObject resObj = new JSONObject(data);
            SigRes res = new SigRes();
            res.retcode = resObj.getInt("retcode");
            res.errmsg = resObj.getString("errmsg");
            res.logkey = resObj.getString("logkey");
            res.sign = resObj.getString("sign");
            res.bucket = resObj.getString("bucket");
            res.path = resObj.getString("path");
            res.region = resObj.getString("region");
            res.cosAppid = resObj.getString("cosAppid");
            logKey = res.logkey;

            String uploadRoot = res.region + ".file.myqcloud.com/files/v2/" + res.cosAppid + "/" + res.bucket + "/" + res.path + "/";
            String upload_url = "http://" + uploadRoot + res.logkey + ".zip";

            ILiveLog.di(TAG, "start", new ILiveLog.LogExts().put("url", upload_url));

            // 填充请求头部
            String boundary = UUID.randomUUID().toString();
            Map<String, String> proMaps = new HashMap<String, String>();
            proMaps.put("Authorization", res.sign);
            proMaps.put("Content-Type", "multipart/form-data; boundary=" + boundary);

            ILiveHttpReq req = new ILiveHttpReq().set_url(upload_url)
                    .set_headers(proMaps)
                    .set_data(formUploadData(res, boundary, params.getContext(), params.getDayOffset()));
            httpEngine.asyncPost(req, new ILiveCallBack<String>() {
                    @Override
                    public void onSuccess(String rspData) {
                        parseUploadRsp(params, rspData, callBack);
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        ILiveFunc.notifyError(callBack, module, errCode, errMsg);
                    }
                    });
        } catch (Exception e) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_SDK_FAILED, e.toString());
        }
    }

    public void start(final LogParams params, final ILiveCallBack<String> callBack) {
        byte[] data = getStartReq(params.getAppid(), params.getAppid(), callBack);
        if (null != data) {
            ILiveLog.di(TAG, "start", new ILiveLog.LogExts().put("id", params.getUserId()).put("day", params.getDayOffset()));
            ILiveHttpReq req = new ILiveHttpReq().set_url(sigUrl)
                    .set_data(data);
            httpEngine.asyncPost(req, new ILiveCallBack<String>() {
                @Override
                public void onSuccess(String data) {
                    upload(params, data, callBack);
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    ILiveFunc.notifyError(callBack, module, errCode, errMsg);
                }
            });
        }
    }

    public void start(final String desc, final Context context, final int dayOffset, int appid, final String id, final ILiveCallBack<String> callBack) {
        start(new LogParams().setContext(context)
                        .setDesc(desc)
                        .setUserId(id)
                        .setDayOffset(dayOffset)
                        .setAppId(appid),
                callBack);
    }

    private byte[] getStartReq(long appid, long sdkappid, ILiveCallBack callBack) {
        if (appid == 0 || sdkappid == 0) {
            callBack.onError(ILiveConstants.Module_ILIVESDK, ERR_ERR_PARAM, "ERR_ERR_PARAM");
        }
        try {
            JSONObject obj = new JSONObject();
            obj.put("appid", appid);
            obj.put("sdkappid", sdkappid);
            return obj.toString().getBytes("utf-8");
        } catch (Exception e) {
            callBack.onError(ILiveConstants.Module_ILIVESDK, ERR_ERR_PARAM, "ERR_ERR_PARAM");
            return null;
        }
    }

    private void zip(List<String> files, String zipFileName) {

        File zipFile = new File(zipFileName);
        InputStream is = null;
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            zos.setComment("hello");
            for (String path : files) {
                File file = new File(path);
                try {
                    is = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buffer = new byte[8 * 1024];
                    int length = 0;
                    while ((length = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            ILiveLog.kw(TAG, "zip", new ILiveLog.LogExts().put("exception", e.toString()));
        } finally {
            try {
                zos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] formUploadData(SigRes sigRes, String boundary, Context context, int dayOffset) throws IOException {

        String zipPath = zipLogs(context, sigRes.logkey, dayOffset);
        ILiveLog.dd(TAG, "formUploadData", new ILiveLog.LogExts().put("zipPath", zipPath));
        RandomAccessFile zipFile = new RandomAccessFile(zipPath, "r");
        byte[] b = new byte[(int) zipFile.length()];
        zipFile.readFully(b);

        // 填充请求内容
        byte[] op = getParamBytes(boundary, "op", "upload");
        byte[] file = getParamBytes(boundary, "filecontent", sigRes.logkey + ".zip", b);

        byte[] end = ("--" + boundary + "--\r\n").getBytes();

        byte[] postData = new byte[file.length + op.length + end.length];
        System.arraycopy(op, 0, postData, 0, op.length);
        System.arraycopy(file, 0, postData, op.length, file.length);
        System.arraycopy(end, 0, postData, file.length + op.length, end.length);
        return postData;
    }

    private String zipLogs(Context context, String newName, int dayOffset) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory().toString())
                .append("/tencent/imsdklogs/")
                .append(context.getPackageName().replace(".", "/"))
                .append("/");
        String filePathBase = sb.toString();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -dayOffset);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String formatted = format.format(c.getTime());
        String zipPath = filePathBase + newName + ".zip";
        List<String> logs = new ArrayList<>();
        logs.add(filePathBase + "ilivesdk_" + formatted + ".log");
        logs.add(filePathBase + "imsdk_" + formatted + ".log");
        logs.add(filePathBase + "QAVSDK_" + formatted + ".log");
        logs.add(filePathBase + "QAVSDK2S_ALL_" + formatted + ".log");
        Iterator<String> it = logs.iterator();
        while (it.hasNext()) {
            String path = it.next();
            File file = new File(path);
            if (!file.exists()) {
                it.remove();
            }
        }
        if (logs.size() == 0) throw new FileNotFoundException();
        zip(logs, zipPath);
        return zipPath;

    }

    private String getFileNames(Context context, int dayOffset) {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory().toString())
                .append("/tencent/imsdklogs/")
                .append(context.getPackageName().replace(".", "/"))
                .append("/");
        String filePathBase = sb.toString();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -dayOffset);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String formatted = format.format(c.getTime());

        List<String> logs = new ArrayList<>();
        File file1 = new File(filePathBase + "ilivesdk_" + formatted + ".log");
        if (file1.exists()) {
            logs.add("ilivesdk_" + formatted + ".log");
        }
        File file2 = new File(filePathBase + "imsdk_" + formatted + ".log");
        if (file2.exists()) {
            logs.add("imsdk_" + formatted + ".log");
        }
        File file3 = new File(filePathBase + "QAVSDK_" + formatted + ".log");
        if (file3.exists()) {
            logs.add("QAVSDK_" + formatted + ".log");
            logs.add("QAVSDK2S_ALL_" + formatted + ".log");

        }
        StringBuilder sb1 = new StringBuilder();
        for (int i = 0; i < logs.size(); ++i) {
            if (i != logs.size() - 1) {
                sb1.append(logs.get(i)).append(",");
            } else {
                sb1.append(logs.get(i));
            }
        }
        return sb1.toString();
    }

    /**
     * 生成参数字符串
     */
    private byte[] getParamBytes(String boundary, String name, String value) {
        String param = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "\r\n"
                + value + "\r\n";
        return param.getBytes();
    }

    private byte[] getParamBytes(String boundary, String name, String filename, byte[] binaryData) {
        String param = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream" + "\"\r\n"
                + "\r\n";
        byte[] paramByte = param.getBytes();
        byte[] bts = new byte[paramByte.length + binaryData.length + 2];
        System.arraycopy(paramByte, 0, bts, 0, paramByte.length);
        System.arraycopy(binaryData, 0, bts, paramByte.length, binaryData.length);
        System.arraycopy("\r\n".getBytes(), 0, bts, paramByte.length + binaryData.length, 2);
        return bts;
    }


    public static byte[] gzCompress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        return out.toByteArray();
    }


}

class SigRes {
    int retcode;
    String errmsg;
    String logkey;
    String sign;
    String path;
    String bucket;
    String cosAppid;
    String region;
}

class CosRes {
    int code;
    String access_url;
    String preview_url;
    String resource_path;
    String source_url;
    String url;

}

class UploadRes {
    int retcode;
    String errmsg;
}
