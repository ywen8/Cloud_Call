package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.core.ILiveLoginManager;

/**
 * 帐号登录模块
 */
public interface LoginEngine {
    /** 初始化 */
    void init();
    /** 登录 */
    void login(String identifier, String userSig, ILiveCallBack callBack);
    /** 注销 */
    void logout(ILiveCallBack callBack);
    /** 是否登录 */
    boolean isLogin();
    /** 设置状态管理监听 */
    void setLoginStatusListner(ILiveLoginManager.TILVBStatusListener listner);
}
