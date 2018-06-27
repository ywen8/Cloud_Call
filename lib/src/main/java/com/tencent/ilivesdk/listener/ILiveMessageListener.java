package com.tencent.ilivesdk.listener;

import com.tencent.ilivesdk.data.ILiveMessage;

/**
 * Created by xkazerzhang on 2017/9/12.
 */
public interface ILiveMessageListener {
    /** 收到新消息 */
    void onNewMessage(ILiveMessage message);
}
