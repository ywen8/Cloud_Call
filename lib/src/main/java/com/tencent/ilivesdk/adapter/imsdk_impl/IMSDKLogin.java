package com.tencent.ilivesdk.adapter.imsdk_impl;

import android.text.TextUtils;

import com.tencent.TIMCallBack;
import com.tencent.TIMLogLevel;
import com.tencent.TIMManager;
import com.tencent.TIMUser;
import com.tencent.TIMUserStatusListener;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.LoginEngine;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.timint.TIMIntManager;

/**
 * IMSDK登录模块
 */
public class IMSDKLogin implements LoginEngine, TIMUserStatusListener {
    private final static String TAG = "ILVB-IMSDKLogin";
    private ILiveLoginManager.TILVBStatusListener mStatusListener = null;

    @Override
    public void onForceOffline() {
        ILiveLog.kw(TAG, "onForceOffline->kickOut");
        if (null != mStatusListener) {
            mStatusListener.onForceOffline(ILiveConstants.ERR_KICK_OUT, "kick out");
        }
    }

    @Override
    public void onUserSigExpired() {
        ILiveLog.kw(TAG, "onForceOffline->sigExpire");
        if (null != mStatusListener) {
            mStatusListener.onForceOffline(ILiveConstants.ERR_EXPIRE, "sig expire");
        }
    }

    @Override
    public void init() {
        switch (ILiveLog.getLogLevel()){
            case OFF:
                TIMManager.getInstance().setLogLevel(TIMLogLevel.OFF);
                break;
            case ERROR:
                TIMManager.getInstance().setLogLevel(TIMLogLevel.ERROR);
                break;
            case WARN:
                TIMManager.getInstance().setLogLevel(TIMLogLevel.WARN);
                break;
            case INFO:
                TIMManager.getInstance().setLogLevel(TIMLogLevel.INFO);
                break;
            case DEBUG:
                TIMManager.getInstance().setLogLevel(TIMLogLevel.DEBUG);
                break;
        }
        ILiveLog.di(TAG, "init", new ILiveLog.LogExts()
                .put("level", ILiveLog.getLogLevel())
                .put("version", TIMManager.getInstance().getVersion()));
        TIMManager.getInstance().init(ILiveSDK.getInstance().getAppContext());

        // 添加异常日志上报
        TIMIntManager.getInstance().setAvSDKVersionToBugly("e217b9cd0c", ILiveSDK.getInstance().getVersion());
    }

    @Override
    public void login(String identifier, String userSig, final ILiveCallBack callBack) {
        TIMUser user = new TIMUser();
        user.setAccountType("" + ILiveSDK.getInstance().getAccountType());
        user.setAppIdAt3rd("" + ILiveSDK.getInstance().getAppId());
        user.setIdentifier(identifier);

        // 监控用户状态事件
        TIMManager.getInstance().setUserStatusListener(this);
        //发起登录请求
        TIMManager.getInstance().login(
                ILiveSDK.getInstance().getAppId(),
                user,
                userSig, //用户帐号签名，由私钥加密获得，具体请参考文档
                new TIMCallBack() {
                    @Override
                    public void onError(int errCode, String errMsg) {
                        ILiveLog.ke(TAG, "login", ILiveConstants.Module_IMSDK, errCode, errMsg);
                        ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
                    }

                    @Override
                    public void onSuccess() {
                        ILiveFunc.notifySuccess(callBack, 0);
                    }
                });
    }

    @Override
    public void logout(final ILiveCallBack callBack) {
        TIMManager.getInstance().logout(new TIMCallBack() {
            @Override
            public void onError(int code, String message) {
                ILiveLog.ke(TAG, "logout", ILiveConstants.Module_IMSDK, code, message);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, code, message);
            }

            @Override
            public void onSuccess() {
                ILiveFunc.notifySuccess(callBack, 0);
            }
        });
    }

    @Override
    public void setLoginStatusListner(ILiveLoginManager.TILVBStatusListener listner) {
        this.mStatusListener = listner;
    }

    @Override
    public boolean isLogin() {
        return !TextUtils.isEmpty(TIMManager.getInstance().getLoginUser());
    }
}
