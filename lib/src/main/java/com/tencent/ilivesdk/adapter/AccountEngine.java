package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;

/**
 * 帐户管理模块
 */
public interface AccountEngine {
    /** 初始化 */
    void init();
    /** 新用户注册 */
    void regiest(String id, String pwd, ILiveCallBack callBack);
    /** 用户登录 */
    void login(String id, String pwd, ILiveCallBack<String> callBack);
    /** 获取用户UserSig */
    String getUserSig(String id);
}
