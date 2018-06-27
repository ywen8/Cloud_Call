package com.tencent.ilivesdk.adapter.tlssdk_impl;

import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.AccountEngine;
import com.tencent.ilivesdk.core.ILiveLog;

import tencent.tls.platform.TLSAccountHelper;
import tencent.tls.platform.TLSErrInfo;
import tencent.tls.platform.TLSLoginHelper;
import tencent.tls.platform.TLSPwdLoginListener;
import tencent.tls.platform.TLSStrAccRegListener;
import tencent.tls.platform.TLSUserInfo;

/**
 * Created by xkazerzhang on 2017/7/26.
 */
public class TLSSDKAccount implements AccountEngine {
    private final static String TAG = "ILVB-TLSSDKAccount";
    public static String COUNTRY_CODE = "86";
    public static int LANGUAGE_CODE = 2052;
    public static int TIMEOUT = 8000;

    private TLSLoginHelper mLoginHelper;
    private TLSAccountHelper mAccountHelper;

    @Override
    public void init() {
        mLoginHelper = TLSLoginHelper.getInstance().init(ILiveSDK.getInstance().getAppContext(),
                ILiveSDK.getInstance().getAppId(), ILiveSDK.getInstance().getAccountType(), ILiveSDK.getInstance().getVersion());
        mLoginHelper.setTimeOut(TIMEOUT);
        mLoginHelper.setLocalId(LANGUAGE_CODE);

        mAccountHelper = TLSAccountHelper.getInstance().init(ILiveSDK.getInstance().getAppContext(),
                ILiveSDK.getInstance().getAppId(), ILiveSDK.getInstance().getAccountType(), ILiveSDK.getInstance().getVersion());
        mAccountHelper.setCountry(Integer.parseInt(COUNTRY_CODE)); // 存储注册时所在国家，只须在初始化时调用一次
        mAccountHelper.setTimeOut(TIMEOUT);
        mAccountHelper.setLocalId(LANGUAGE_CODE);
        ILiveLog.di(TAG, "init->enter");
    }

    @Override
    public void regiest(String id, String pwd, final ILiveCallBack callBack) {
        int ret = mAccountHelper.TLSStrAccReg(id, pwd, new TLSStrAccRegListener() {
            @Override
            public void OnStrAccRegSuccess(TLSUserInfo tlsUserInfo) {
                ILiveFunc.notifySuccess(callBack, tlsUserInfo.identifier);
            }

            @Override
            public void OnStrAccRegFail(TLSErrInfo tlsErrInfo) {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }

            @Override
            public void OnStrAccRegTimeout(TLSErrInfo tlsErrInfo) {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }
        });
        if (TLSErrInfo.PENDING != ret && TLSErrInfo.LOGIN_OK != ret) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_TLSSDK, ret, "input not valid");
        }
    }

    @Override
    public void login(String id, String pwd, final ILiveCallBack<String> callBack) {
        int ret = mLoginHelper.TLSPwdLogin(id, pwd.getBytes(), new TLSPwdLoginListener() {
            @Override
            public void OnPwdLoginSuccess(TLSUserInfo tlsUserInfo) {
                ILiveLog.di(TAG, "TLSPwdLogin->success", new ILiveLog.LogExts().put("id", tlsUserInfo.identifier));
                ILiveFunc.notifySuccess(callBack, mLoginHelper.getUserSig(tlsUserInfo.identifier));
            }

            @Override
            public void OnPwdLoginReaskImgcodeSuccess(byte[] bytes) {

            }

            @Override
            public void OnPwdLoginNeedImgcode(byte[] bytes, TLSErrInfo tlsErrInfo) {

            }

            @Override
            public void OnPwdLoginFail(TLSErrInfo tlsErrInfo) {
                ILiveLog.de(TAG, "TLSPwdLogin->fail", ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }

            @Override
            public void OnPwdLoginTimeout(TLSErrInfo tlsErrInfo) {
                ILiveLog.de(TAG, "TLSPwdLogin->timeout", ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_TLSSDK, tlsErrInfo.ErrCode, tlsErrInfo.Msg);
            }
        });
        if (ILiveConstants.NO_ERR != ret)
            ILiveLog.dw(TAG, "login", new ILiveLog.LogExts().put("id", id)
                    .put("ret", ret));
    }

    @Override
    public String getUserSig(String id) {
        return mLoginHelper.getUserSig(id);
    }
}
