package com.tencent.ilivesdk.adapter.imsdk_impl;

import com.tencent.TIMCallBack;
import com.tencent.TIMGroupManager;
import com.tencent.TIMValueCallBack;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.adapter.GroupEngine;
import com.tencent.ilivesdk.core.ILiveLog;

import java.util.ArrayList;

/**
 * IMSDK群组模块
 */
public class IMSDKGroup implements GroupEngine {
    private final static  String TAG = "ILVB-IMSDKGroup";
    @Override
    public void createGroup(String groupId, String groupName, String GroupType, final ILiveCallBack callBack) {
        TIMGroupManager.getInstance().createGroup(GroupType, new ArrayList<String>(), groupName, groupId, new TIMValueCallBack<String>() {
            @Override
            public void onError(int errorCode, String errInfo) {
                ILiveLog.ke(TAG, "createGroup", ILiveConstants.Module_IMSDK, errorCode, errInfo);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errorCode, errInfo);
            }

            @Override
            public void onSuccess(String groupId) {
                ILiveFunc.notifySuccess(callBack, groupId);
            }
        });
    }

    @Override
    public void joinGroup(String groupId, String GroupType, final ILiveCallBack callBack) {
        if (GroupType.equals("Private")){   // 私有群组不用创建
            ILiveFunc.notifySuccess(callBack, 0);
            return;
        }
        TIMGroupManager.getInstance().applyJoinGroup(groupId, "request to join " + groupId, new TIMCallBack() {
            @Override
            public void onError(int errorCode, String errInfo) {
                ILiveLog.ke(TAG, "applyJoinGroup", ILiveConstants.Module_IMSDK, errorCode, errInfo);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errorCode, errInfo);
            }

            @Override
            public void onSuccess() {
                ILiveFunc.notifySuccess(callBack, 0);
            }
        });
    }

    @Override
    public void quitGroup(String groupId, final ILiveCallBack callBack) {
        TIMGroupManager.getInstance().quitGroup(groupId, new TIMCallBack() {
            @Override
            public void onError(int errorCode, String errInfo) {
                ILiveLog.ke(TAG, "quitGroup", ILiveConstants.Module_IMSDK, errorCode, errInfo);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errorCode, errInfo);
            }

            @Override
            public void onSuccess() {
                ILiveFunc.notifySuccess(callBack, 0);
            }
        });
    }

    @Override
    public void deleteGroup(String groupId, final ILiveCallBack callBack) {
        TIMGroupManager.getInstance().deleteGroup(groupId, new TIMCallBack() {
            @Override
            public void onError(int errorCode, String errInfo) {
                ILiveLog.ke(TAG, "deleteGroup", ILiveConstants.Module_IMSDK, errorCode, errInfo);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errorCode, errInfo);
            }

            @Override
            public void onSuccess() {
                ILiveFunc.notifySuccess(callBack, 0);
            }
        });
    }
}
