package com.tencent.ilivesdk.adapter.imsdk_impl;

import com.tencent.TIMConversation;
import com.tencent.TIMConversationType;
import com.tencent.TIMManager;
import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.TIMValueCallBack;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.adapter.ConversationEngine;
import com.tencent.ilivesdk.core.ILiveLog;

/**
 * IMSDK消息模块
 */
public class IMSDKConversation implements ConversationEngine{
    private final static String TAG = "ILVB-IMSDKConversation";
    @Override
    public void sendGroupMessage(final String grpId, TIMMessage msg, final ILiveCallBack callBack) {
        TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.Group, grpId);
        conversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {
            @Override
            public void onError(int errCode, String errMsg) {
                ILiveLog.ke(TAG, "sendGroupMessage", ILiveConstants.Module_IMSDK, errCode, errMsg, new ILiveLog.LogExts().put("grpId", grpId));
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }

            @Override
            public void onSuccess(TIMMessage message) {
                ILiveFunc.notifySuccess(callBack, message);
            }
        });
    }

    @Override
    public void sendOnlineGroupMessage(final String grpId, TIMMessage msg, final ILiveCallBack callBack) {
        TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.Group, grpId);
        conversation.sendOnlineMessage(msg, new TIMValueCallBack<TIMMessage>() {
            @Override
            public void onError(int errCode, String errMsg) {
                ILiveLog.ke(TAG, "sendOnlineGroupMessage", ILiveConstants.Module_IMSDK, errCode, errMsg, new ILiveLog.LogExts().put("grpId", grpId));
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }

            @Override
            public void onSuccess(TIMMessage message) {
                ILiveFunc.notifySuccess(callBack, message);
            }
        });
    }

    @Override
    public void sendC2CMessage(final String dstId, TIMMessage msg, final ILiveCallBack callBack) {
        TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.C2C, dstId);
        conversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {
            @Override
            public void onError(int errCode, String errMsg) {
                ILiveLog.ke(TAG, "sendC2CMessage", ILiveConstants.Module_IMSDK, errCode, errMsg, new ILiveLog.LogExts().put("dstId", dstId));
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }

            @Override
            public void onSuccess(TIMMessage message) {
                ILiveFunc.notifySuccess(callBack, message);
            }
        });
    }

    @Override
    public void sendOnlineC2CMessage(final String dstId, TIMMessage msg, final ILiveCallBack callBack) {
        TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.C2C, dstId);
        conversation.sendOnlineMessage(msg, new TIMValueCallBack<TIMMessage>() {
            @Override
            public void onError(int errCode, String errMsg) {
                ILiveLog.ke(TAG, "sendC2CMessage", ILiveConstants.Module_IMSDK, errCode, errMsg, new ILiveLog.LogExts().put("dstId", dstId));
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }

            @Override
            public void onSuccess(TIMMessage message) {
                ILiveFunc.notifySuccess(callBack, message);
            }
        });
    }

    @Override
    public void addMessageListener(TIMMessageListener listener) {
        TIMManager.getInstance().addMessageListener(listener);
    }

    @Override
    public void removeMessageListener(TIMMessageListener listener) {
        TIMManager.getInstance().removeMessageListener(listener);
    }
}
