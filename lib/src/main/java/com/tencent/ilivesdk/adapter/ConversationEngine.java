package com.tencent.ilivesdk.adapter;

import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.ilivesdk.ILiveCallBack;

/**
 * IM会话模块
 */
public interface ConversationEngine {
    /** 发送群组消息 */
    void sendGroupMessage(String grpId, TIMMessage msg, ILiveCallBack callBack);
    /** 发送在线群组消息 */
    void sendOnlineGroupMessage(String grpId, TIMMessage msg, ILiveCallBack callBack);

    /** 发送C2C消息 */
    void sendC2CMessage(String dstId, TIMMessage msg, ILiveCallBack callBack);
    /** 发送在线C2C消息 */
    void sendOnlineC2CMessage(String dstId, TIMMessage msg, ILiveCallBack callBack);

    /** 添加消息回调 */
    void addMessageListener(TIMMessageListener listener);
    /** 移除消息回调 */
    void removeMessageListener(TIMMessageListener listener);
}
