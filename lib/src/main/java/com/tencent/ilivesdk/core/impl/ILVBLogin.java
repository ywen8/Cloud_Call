package com.tencent.ilivesdk.core.impl;
import com.tencent.av.sdk.AVContext;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.CommonConstants;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.imsdk.util.QualityReportHelper;


/**
 * 登录模块的实现类
 */
public class ILVBLogin extends ILiveLoginManager implements ILiveLoginManager.TILVBStatusListener {
    private TILVBStatusListener statusListener;

    private String mMyUserId = "";
    private boolean bLogin = false;
    private static final String TAG = ILVBLogin.class.getSimpleName();
    private QualityReportHelper helper = new QualityReportHelper();

    @Override
    public void onForceOffline(int error, String message) {
        ILiveLog.ke(TAG, "onForceOffline", ILiveConstants.Module_IMSDK, error, message);
        if (null != statusListener) {
            statusListener.onForceOffline(error, message);
        }
        // 回收资源
        iLiveLogout(null);
    }

    @Override
    public void setUserStatusListener(TILVBStatusListener listener) {
        statusListener = listener;
    }

    @Override
    public void tlsRegister(String id, String pwd, final ILiveCallBack listener) {
        ILiveSDK.getInstance().getAccountEngine().regiest(id, pwd, listener);
    }

    @Override
    public void tlsLogin(final String id, String pwd, final ILiveCallBack<String> listener) {
        ILiveLog.kd(TAG, "tlsLogin", new ILiveLog.LogExts().put("id", id));
        ILiveSDK.getInstance().getAccountEngine().login(id, pwd, listener);
    }


    @Override
    public void tlsLoginAll(final String id, String pwd, final ILiveCallBack callBack) {
        ILiveLog.kd(TAG, "tlsLoginAll", new ILiveLog.LogExts().put("id", id));
        tlsLogin(id, pwd, new ILiveCallBack<String>() {
            @Override
            public void onSuccess(String userSig) {
                iLiveLogin(id, userSig, callBack);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveFunc.notifyError(callBack, module, errCode, errMsg);
            }
        });
    }


    @Override
    public void iLiveLogin(final String id, String sig, final ILiveCallBack tilvbLoginListener) {
        ILiveLog.ki(TAG, "iLiveLogin", new ILiveLog.LogExts().put("id", id));
        //iLiveLogout(null);
        ILiveSDK.getInstance().getLoginEngine().setLoginStatusListner(this);
        ILiveSDK.getInstance().getLoginEngine().login(id, sig, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveLog.ki(TAG, "iLiveLogin->success");
                startContext(id, tilvbLoginListener);
                mMyUserId = id;
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveLog.ke(TAG, "iLiveLogin", module, errCode, errMsg);
                ILiveFunc.notifyError(tilvbLoginListener, ILiveConstants.Module_IMSDK, errCode, errMsg);
                helper.init(ILiveConstants.EVENT_ILIVE_LOGIN, errCode, errMsg);
                helper.report();
            }
        });
    }

    private void logoutIM(final ILiveCallBack callBack) {
        ILiveSDK.getInstance().getLoginEngine().logout(callBack);
    }

    private void logoutSDK(final ILiveCallBack logoutListener) {
        if (!ILiveSDK.getInstance().getLoginEngine().isLogin()) {
            ILiveFunc.notifyError(logoutListener, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_NOT_LOGIN, "im logout already");
            stopContext(null);      // IM注销失败也要停止AVSDK
        } else {
            logoutIM(new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    ILiveLog.ki(TAG, "logoutSDK->onSuccess");
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    ILiveLog.ke(TAG, "logoutSDK", module, errCode, errMsg);
                    ILiveFunc.notifyError(logoutListener, ILiveConstants.Module_IMSDK, errCode, errMsg);
                }
            });
            stopContext(logoutListener);    // 并行停止AVSDK
        }
    }

    @Override
    public void iLiveLogout(final ILiveCallBack logoutListener) {
        statusListener = null;
        bLogin = false;
        ILiveLog.ki(TAG, "iLiveLogout->enter");
        if (ILiveRoomManager.getInstance().isEnterRoom())
            ILiveRoomManager.getInstance().quitRoom(new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    logoutSDK(logoutListener);
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    logoutSDK(logoutListener);
                }
            });  // 可能不会回调，如被踢*/
        else
            logoutSDK(logoutListener);
    }

    @Override
    public String getSig(String id) {
        if (id != null)
            return ILiveSDK.getInstance().getAccountEngine().getUserSig(id);
        return null;
    }

    /**
     * 获取用户id
     *
     * @return id
     */
    @Override
    public String getMyUserId() {
        return mMyUserId;
    }

    @Override
    public AVContext getAVConext() {
        return (AVContext) ILiveSDK.getInstance().getContextEngine().getContextObj();
    }

    @Override
    public boolean isLogin() {
        return bLogin;
    }

    /**
     * 实际初始化AVSDK
     */
    private void startContext(String identifier, final ILiveCallBack callBack) {
        ContextEngine contextAdapter = ILiveSDK.getInstance().getContextEngine();
        ILiveLog.di(TAG, "startContext");
        CommonConstants.ILiveUserInfo info = new CommonConstants.ILiveUserInfo();
        info.sdkAppId = ILiveSDK.getInstance().getAppId();
        info.accountType = ILiveSDK.getInstance().getAccountType();
        info.identifier = identifier;
        contextAdapter.setUserInfo(info);
        contextAdapter.start(ILiveLog.getLogLevel(), new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                bLogin = true;
                ILiveFunc.notifySuccess(callBack, 0);
                helper.init(ILiveConstants.EVENT_ILIVE_LOGIN, 0, "");
                helper.report();
                ((ILVBRoom) ILiveRoomManager.getInstance()).afterLogin();
                ILiveLog.di(TAG, "startContext->onSuccess");
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                logoutIM(null);
                ILiveFunc.notifyError(callBack, module, errCode, errMsg);

                helper.init(ILiveConstants.EVENT_ILIVE_LOGIN, errCode, errMsg);
                helper.report();
                ILiveLog.de(TAG, "startContext", module, errCode, errMsg);
                stopContext(null);
            }
        });
    }

    void stopContext(final ILiveCallBack logoutListener) {
        ILiveLog.di(TAG, "stopContext");
        ILiveSDK.getInstance().getContextEngine().stop(new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveFunc.notifySuccess(logoutListener, 0);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveLog.ke(TAG, "stopContext", module, errCode, errMsg);
                ILiveFunc.notifyError(logoutListener, module, errCode, errMsg);
            }
        });
    }
}
