package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;

/**
 * IM群组模块
 */
public interface GroupEngine {
    /** 创建IM群组 */
    void createGroup(String groupId, String groupName, String GroupType, ILiveCallBack callBack);
    /** 加入IM群组 */
    void joinGroup(String groupId, String GroupType, ILiveCallBack callBack);
    /** 退出IM群组 */
    void quitGroup(String groupId, ILiveCallBack callBack);
    /** 解散IM群组 */
    void deleteGroup(String groupId, ILiveCallBack callBack);
}
