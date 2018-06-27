package com.tencent.ilivesdk.adapter.normal_impl;


import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.adapter.ILiveHttpEngine;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.data.ILiveHttpConfig;
import com.tencent.ilivesdk.data.ILiveHttpReq;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xkazerzhang on 2017/9/6.
 */
public class NormalHttpCtrl implements ILiveHttpEngine {
    private final static String TAG = "NormalHttpCtrl";

    private ILiveHttpConfig mConfig = null;
    private ExecutorService mThreadPool = null;

    @Override
    public void init(ILiveHttpConfig config) {
        if (null == config){
            config = new ILiveHttpConfig();
        }
        mConfig = config;
        if (config.get_thread_numbers() > 1){
            ILiveLog.ki(TAG, "init->multi-thread", new ILiveLog.LogExts().put("threads", config.get_thread_numbers()));
            mThreadPool = Executors.newFixedThreadPool(config.get_thread_numbers());
        }else{  // 单线程
            ILiveLog.ki(TAG, "init->single");
            mThreadPool = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void asyncGet(final ILiveHttpReq req, final ILiveCallBack<String> callBack) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doGetRequest(req, callBack);
            }
        });
    }

    @Override
    public void asyncPost(final ILiveHttpReq req, final ILiveCallBack<String> callBack) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                doPostRequest(req, callBack);
            }
        });
    }

    private void doGetRequest(ILiveHttpReq req, ILiveCallBack<String> callBack){
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(req.get_url());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(mConfig.get_connect_timeout());
            urlConnection.setReadTimeout(mConfig.get_read_timeout());
            if (req.get_headers() != null) {
                for (String key : req.get_headers().keySet()) {
                    urlConnection.setRequestProperty(key, req.get_headers().get(key));
                }
            }
            int status = urlConnection.getResponseCode();
            ILiveLog.dd(TAG, "post", new ILiveLog.LogExts().put("status", status));
            if (status == 200) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                ILiveFunc.notifyMainSuccess(callBack, new String(readStream(in)));
            } else {
                ILiveFunc.notifyMainError(callBack, ILiveConstants.Module_HTTP, status, "http error");
            }
        } catch (Exception e){
            ILiveLog.dw(TAG, "doGetRequest", new ILiveLog.LogExts().put("exception", e.toString()));
            ILiveFunc.notifyMainError(callBack, ILiveConstants.Module_HTTP, ILiveConstants.ERR_SDK_FAILED, e.toString());
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }

    private void doPostRequest(ILiveHttpReq req, ILiveCallBack<String> callBack){
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(req.get_url());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(mConfig.get_connect_timeout());
            urlConnection.setReadTimeout(mConfig.get_read_timeout());
            if (req.get_headers() != null) {
                for (String key : req.get_headers().keySet()) {
                    urlConnection.setRequestProperty(key, req.get_headers().get(key));
                }
            }
            urlConnection.setRequestProperty("Content-Length", String.valueOf(req.get_data().length));
            urlConnection.getOutputStream().write(req.get_data());
            int status = urlConnection.getResponseCode();
            ILiveLog.dd(TAG, "post", new ILiveLog.LogExts().put("status", status));
            if (status == 200) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                ILiveFunc.notifyMainSuccess(callBack, new String(readStream(in)));
            } else {
                ILiveFunc.notifyMainError(callBack, ILiveConstants.Module_HTTP, status, "http error");
            }
        } catch (Exception e){
            ILiveLog.dw(TAG, "doPostRequest", new ILiveLog.LogExts().put("exception", e.toString()));
            ILiveFunc.notifyMainError(callBack, ILiveConstants.Module_HTTP, ILiveConstants.ERR_SDK_FAILED, e.toString());
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }

    byte[] readStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }
        outSteam.close();
        inStream.close();
        return outSteam.toByteArray();
    }
}
