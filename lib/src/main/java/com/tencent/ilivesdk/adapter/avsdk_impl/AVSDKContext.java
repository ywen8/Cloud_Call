package com.tencent.ilivesdk.adapter.avsdk_impl;


import com.tencent.av.sdk.AVCallback;
import com.tencent.av.sdk.AVContext;
import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVSDKLogSetting;
import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.AudioEngine;
import com.tencent.ilivesdk.adapter.CommonConstants;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.adapter.VideoEngine;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveRoomOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * AVSDK Context模块
 */
public class AVSDKContext implements ContextEngine, AVRoomMulti.EventListener {
    private final static String TAG = "ILVB-AVSDKContext";
    private AVContext mAVContext = null;
    private AVRoomMulti mRoomMulti = null;

    private CommonConstants.ILiveUserInfo mUserInfo = null;

    // 回调存储
    private ILiveCallBack mEnterCallBack, mExitCallBack, mSwitchRoomCallBack;
    private AVEndPointEvent mEndPointListener = null;
    private AVRoomDisconnect mRoomDisconnectListener = null;
    private AVDataChangeEvent mDataChangeListener = null;
    private ILiveRoomOption.onRequestViewListener mRequestViewListener = null;

    /**
     * 请求画面
     */
    private boolean bRequsting = false;     // 正在请求画面
    private boolean bChanged = false;       // 请求已经变更
    private List<String> mReqUserCameraListBak = new ArrayList<>();  // 保存要请求视频的用户(恢复时使用)
    private List<String> mReqUserScreenListBak = new ArrayList<>(); // 保存要请求屏幕的用户(恢复时使用)
    private List<String> mReqUserFileListBak = new ArrayList<>(); // 保存要请求屏幕的用户(恢复时使用)

    /**
     * 操作时间
     */
    private long uOperateTime = 0;
    /**
     * 超时时间(默认为2秒)
     */
    private int uTimeOut = 2;

    private AudioEngine mAVAudioCtrl = new AVSDKAudioCtrl();
    private VideoEngine mAVVideoCtrl = new AVSDKVideoCtrl();
    private HashMap<String, Object> mVars = new HashMap<>();

    private AVRoomMulti.RequestViewListCompleteCallback mRequestViewListCompleteCallback = new AVRoomMulti.RequestViewListCompleteCallback() {
        public void OnComplete(String identifierList[], AVView viewList[], int count, int result, String errMsg) {
            bRequsting = false;

            if (null != mRequestViewListener) {
                mRequestViewListener.onComplete(identifierList, viewList, count, result, errMsg);
            }
            if (bChanged) {
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        requestRemoteVideo();
                    }
                }, 0);
            }
            ILiveLog.kd(TAG, "requestViewList->OnComplete", new ILiveLog.LogExts().put("result", result));
        }
    };

    class AVCreateContextCallBack implements AVCallback {
        ILiveCallBack callBack;

        public AVCreateContextCallBack(ILiveCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        public void onComplete(int result, String message) {
            if (result == AVError.AV_OK || result == AVError.AV_ERR_HAS_IN_THE_STATE) {
                if (null != mAVContext) {
                    mVars.put("Audio", mAVContext.getAudioCtrl());
                    mVars.put("Video", mAVContext.getVideoCtrl());

                    mAVAudioCtrl.init(AVSDKContext.this, null);
                    mAVVideoCtrl.init(AVSDKContext.this, null);
                    uOperateTime = 0;
                    ILiveFunc.notifySuccess(callBack, 0);
                }else{
                    ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, ILiveConstants.ERR_AV_NOT_READY, "AVContext is empty");
                }
            } else {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, result, message);
            }
        }
    }

    @Override
    public void onEnterRoomComplete(int result, String info) {
        uOperateTime = 0;
        ILiveCallBack tmpCb = mEnterCallBack;
        mEnterCallBack = null;

        releaseUserVideoData();
        releaseUserAudioData();
        mRoomMulti = mAVContext.getRoom();

        if (AVError.AV_OK == result) {
            ILiveFunc.notifySuccess(tmpCb, 0);
        } else {
            ILiveFunc.notifyError(tmpCb, ILiveConstants.Module_AVSDK, result, info);
        }
    }

    @Override
    public void onExitRoomComplete() {
        ILiveCallBack tmpCb = mExitCallBack;
        mExitCallBack = null;
        ILiveFunc.notifySuccess(tmpCb, 0);

        clearRoomInfo();
    }

    @Override
    public void onRoomDisconnect(int result, String info) {
        clearRoomInfo();

        if (null != mRoomDisconnectListener) {
            mRoomDisconnectListener.onRoomDisconnect(result, info);
        }
    }

    @Override
    public void onSwitchRoomComplete(int result, String info) {
        uOperateTime = 0;

        ILiveCallBack tmpCb = mSwitchRoomCallBack;
        mSwitchRoomCallBack = null;

        releaseUserVideoData();
        releaseUserAudioData();
        mRoomMulti = mAVContext.getRoom();

        if (AVError.AV_OK == result) {
            ILiveFunc.notifySuccess(tmpCb, 0);
        } else {
            ILiveFunc.notifyError(tmpCb, ILiveConstants.Module_AVSDK, result, info);
        }
    }

    @Override
    public void onEndpointsUpdateInfo(int event, String[] users) {
        if (null != mEndPointListener) {
            mEndPointListener.onEndPointEvent(event, users);
        }
    }

    @Override
    public void onPrivilegeDiffNotify(int i) {

    }

    @Override
    public void onSemiAutoRecvCameraVideo(String[] users) {
        if (null != mDataChangeListener) {
            for (String user : users) {
                mDataChangeListener.onVideoChangeEvent(true, user, CommonConstants.Const_VideoType_Camera);
            }
        }
    }

    @Override
    public void onSemiAutoRecvScreenVideo(String[] users) {
        if (null != mDataChangeListener) {
            for (String user : users) {
                mDataChangeListener.onVideoChangeEvent(true, user, CommonConstants.Const_VideoType_Screen);
            }
        }
    }

    @Override
    public void onSemiAutoRecvMediaFileVideo(String[] users) {
        if (null != mDataChangeListener) {
            for (String user : users) {
                mDataChangeListener.onVideoChangeEvent(true, user, CommonConstants.Const_VideoType_File);
            }
        }
    }

    @Override
    public void onCameraSettingNotify(int i, int i1, int i2) {

    }

    @Override
    public void onRoomEvent(int i, int i1, Object o) {

    }

    @Override
    public void onDisableAudioIssue() {

    }

    @Override
    public void onHwStateChangeNotify(boolean b, boolean b1, boolean b2, String s) {

    }

    @Override
    public void init() {
        ILiveLog.ki(TAG, "init", new ILiveLog.LogExts().put("version", AVContext.sdkVersion));
    }

    @Override
    public boolean setUserInfo(CommonConstants.ILiveUserInfo info) {
        AVContext tmpContext = null;
        tmpContext = AVContext.createInstance(ILiveSDK.getInstance().getAppContext(), false);
        if (null != tmpContext) {    // 用新的替换
            if (null != mAVContext && tmpContext != mAVContext) {
                ILiveLog.kw(TAG, "setUserInfo->replace", new ILiveLog.LogExts().put("AVContext", mAVContext)
                        .put("tmpContext", tmpContext));
                stop(null);
            }
            mAVContext = tmpContext;
        } else {
            ILiveLog.kw(TAG, "setUserInfo->fail", new ILiveLog.LogExts().put("AVContext", mAVContext)
                    .put("tmpContext", tmpContext));
            return false;
        }
        mUserInfo = info;
        return true;
    }

    @Override
    public void start(ILiveLog.TILVBLogLevel level, ILiveCallBack callBack) {
        if (null == mAVContext) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AVContext instance found");
            return;
        }
        AVContext.StartParam param = new AVContext.StartParam();
        param.sdkAppId = mUserInfo.sdkAppId;
        param.accountType = "" + mUserInfo.accountType;
        param.appIdAt3rd = "" + mUserInfo.sdkAppId;
        param.identifier = mUserInfo.identifier;

        AVSDKLogSetting.Builder builder = new AVSDKLogSetting.Builder();

        boolean bPrint = level.ordinal() >= ILiveLog.TILVBLogLevel.DEBUG.ordinal();
        boolean bWirte = ILiveLog.TILVBLogLevel.OFF != level;
        builder.isEnablePrintLog(bPrint);
        builder.isEnableWriteLog(bWirte);
        ILiveLog.di(TAG, "start", new ILiveLog.LogExts().put("level", level).put("print", bPrint).put("write", bWirte));

        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        }else {
            uOperateTime = uCurTime;
            mAVContext.start(param, builder.build(), new AVCreateContextCallBack(callBack));
        }
    }

    @Override
    public void stop(ILiveCallBack callBack) {
        if (null == mAVContext) {
            ILiveFunc.notifySuccess(callBack, 0);
            return;
        }
        mAVContext.stop();
        mAVContext.destroy();
        mAVContext = null;
        uOperateTime = 0;
        ILiveFunc.notifySuccess(callBack, 0);
    }

    @Override
    public void enterRoom(int roomId, ILiveRoomOption option, ILiveCallBack callBack) {
        if (null == mAVContext) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AVContext found");
            return;
        }
        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        } else {
            AVRoomMulti.EnterParam.Builder builder = new AVRoomMulti.EnterParam.Builder(roomId)
                    .auth(option.getAuthBits(), option.getAuthBuffer())
                    .avControlRole(option.getAvControlRole())
                    .audioCategory(option.getAudioCategory())
                    .autoCreateRoom(true)
                    .isEnableHwEnc(option.isEnableHwEnc())
                    .isEnableHwDec(option.isEnableHwDec())
                    .isEnableHdAudio(option.isHighAudioQuality())
                    .isEnableMic(option.isAutoMic())
                    .isEnableSpeaker(option.isAutoSpeaker())
                    .videoRecvMode(option.getVideoRecvMode())
                    .screenRecvMode(option.getScreenRecvMode());
            mAVContext.enterRoom(this, builder.build());
            uOperateTime = uCurTime;
            mEnterCallBack = callBack;
        }
    }

    @Override
    public void exitRoom(ILiveCallBack callBack) {
        if (null == mAVContext) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AVContext found");
            return;
        }
        // 重置音视频请求
        releaseUserAudioData();
        releaseUserVideoData();
        int iRet = mAVContext.exitRoom();
        if (AVError.AV_ERR_HAS_IN_THE_STATE == iRet) {
            ILiveFunc.notifySuccess(callBack, 0);
        } else if (AVError.AV_OK != iRet) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, iRet, "Quit AV Room Failed");
        } else {     // 等待回调
            mExitCallBack = callBack;
        }
    }

    @Override
    public void switchRoom(int roomId, ILiveCallBack callBack) {
        if (null == mAVContext) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AVContext found");
            return;
        }

        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        } else {
            mAVContext.switchRoom(roomId);
            uOperateTime = uCurTime;
            mSwitchRoomCallBack = callBack;
        }
    }

    @Override
    public boolean isEnterRoom() {
        return null != mRoomMulti;
    }

    @Override
    public void linkRoom(final int roomId, final String accountId, final String sign, final ILiveCallBack callBack) {
        mRoomMulti.linkRoom(roomId, accountId, sign, new AVCallback() {
            @Override
            public void onComplete(int code, String errMsg) {
                if (AVError.AV_OK != code) {
                    ILiveLog.ke(TAG, "linkRoom->failed", ILiveConstants.Module_AVSDK, code, errMsg, new ILiveLog.LogExts().put("roomId", roomId)
                            .put("accountId", accountId)
                            .put("sign", sign));
                    ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, code, errMsg);
                } else {
                    ILiveLog.dd(TAG, "linkRoom->success", new ILiveLog.LogExts().put("roomId", roomId)
                            .put("accountId", accountId));
                    ILiveFunc.notifySuccess(callBack, 0);
                }
            }
        });
    }

    @Override
    public void unlinkRoom(final ILiveCallBack callBack) {
        mRoomMulti.unlinkRoom(new AVCallback() {
            @Override
            public void onComplete(int code, String errMsg) {
                if (AVError.AV_OK != code) {
                    ILiveLog.ke(TAG, "unlinkRoom", ILiveConstants.Module_AVSDK, code, errMsg);
                    ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, code, errMsg);
                } else {
                    ILiveFunc.notifySuccess(callBack, 0);
                }
            }
        });
    }

    @Override
    public void changeRole(String role, final ILiveCallBack callBack) {
        mRoomMulti.changeAVControlRole(role, new AVCallback() {
            @Override
            public void onComplete(int errCode, String errMsg) {
                if (AVError.AV_OK != errCode) {
                    ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, errCode, "change role failed");
                } else {
                    ILiveFunc.notifySuccess(callBack, 0);
                }
            }
        });
    }

    @Override
    public void setEndPointEventListener(AVEndPointEvent listener) {
        this.mEndPointListener = listener;
    }

    @Override
    public void setRoomDisconnectListener(AVRoomDisconnect listener) {
        this.mRoomDisconnectListener = listener;
    }

    @Override
    public void setDataChangeListener(AVDataChangeEvent listener) {
        this.mDataChangeListener = listener;
    }

    @Override
    public void setRequestCompleteListener(ILiveRoomOption.onRequestViewListener listener) {
        this.mRequestViewListener = listener;
    }

    @Override
    public void requestUserAudioData(String identifer) {

    }

    @Override
    public void releaseUserAudioData() {

    }

    @Override
    public void requestUserVideoData(String identifer, int videoType) {
        ILiveLog.dd(TAG, "requestUserVideoData", new ILiveLog.LogExts().put("id", identifer).put("type", videoType));
        switch (videoType) {
            case CommonConstants.Const_VideoType_Camera:
                if (!mReqUserCameraListBak.contains(identifer))
                    mReqUserCameraListBak.add(identifer);
                else
                    return;
                break;
            case CommonConstants.Const_VideoType_Screen:
                if (!mReqUserScreenListBak.contains(identifer))
                    mReqUserScreenListBak.add(identifer);
                else
                    return;
                break;
            case CommonConstants.Const_VideoType_File:
                if (!mReqUserFileListBak.contains(identifer))
                    mReqUserFileListBak.add(identifer);
                else
                    return;
                break;
        }
        requestRemoteVideo();
    }

    @Override
    public void releaseUserVideoData() {
        mReqUserCameraListBak.clear();
        mReqUserScreenListBak.clear();
        mReqUserFileListBak.clear();
    }

    @Override
    public void removeUserVideoData(String identifer, int videoType) {
        switch (videoType) {
            case CommonConstants.Const_VideoType_Camera:
                mReqUserCameraListBak.remove(identifer);
                break;
            case CommonConstants.Const_VideoType_Screen:
                mReqUserScreenListBak.remove(identifer);
                break;
            case CommonConstants.Const_VideoType_File:
                mReqUserFileListBak.remove(identifer);
                break;
        }
    }

    @Override
    public List<String> getVideoUserList(int videoType) {
        switch (videoType) {
            case CommonConstants.Const_VideoType_Camera:
                return mReqUserCameraListBak;
            case CommonConstants.Const_VideoType_Screen:
                return mReqUserScreenListBak;
            case CommonConstants.Const_VideoType_File:
                return mReqUserFileListBak;
        }
        return new ArrayList<>();
    }

    @Override
    public void pauseUserData() {
        mRoomMulti.cancelAllView(new AVCallback() {
            @Override
            public void onComplete(int errCode, String errInfo) {
                ILiveLog.kd(TAG, "cancelAllView->onComplete", new ILiveLog.LogExts().put("errCode", errCode)
                        .put("errInfo", errInfo));
            }
        });
    }

    @Override
    public void resumeUserData() {
        requestRemoteVideo();
    }

    @Override
    public Object getModuleVar(String key) {
        return mVars.get(key);
    }

    @Override
    public void setModuleVar(String key, Object value) {
        mVars.put(key, value);
    }

    @Override
    public Object getContextObj() {
        return mAVContext;
    }

    @Override
    public Object getRoomObj() {
        return mRoomMulti;
    }

    @Override
    public AudioEngine getAudioAdapter() {
        return mAVAudioCtrl;
    }

    @Override
    public VideoEngine getVideoAdapter() {
        return mAVVideoCtrl;
    }

    /**
     * 设置超时时间
     */
    public void setTimeOut(int timeOut) {
        this.uTimeOut = timeOut;
    }

    /**
     * 请求远程画面
     */
    private void requestRemoteVideo() {
        if (bRequsting) {
            bChanged = true;
            ILiveLog.kw(TAG, "requestRemoteVideo->busy", new ILiveLog.LogExts().put("bRequsting", bRequsting));
            return;
        } else {
            bChanged = false;
        }

        int viewindex = 0;
        int len = mReqUserCameraListBak.size() + mReqUserScreenListBak.size() + mReqUserFileListBak.size();
        ILiveLog.kd(TAG, "requestRemoteVideo->enter", new ILiveLog.LogExts().put("length", len));
        if (len > ILiveConstants.MAX_AV_VIDEO_NUM) {
            len = ILiveConstants.MAX_AV_VIDEO_NUM;
        }
        AVView mRequestViewList[] = new AVView[len];
        String mRequestIdentifierList[] = new String[len];
        for (String id : mReqUserCameraListBak) {
            if (viewindex >= len)
                break;
            AVView view = new AVView();
            view.videoSrcType = AVView.VIDEO_SRC_TYPE_CAMERA;
            view.viewSizeType = AVView.VIEW_SIZE_TYPE_BIG;
            mRequestViewList[viewindex] = view;
            mRequestIdentifierList[viewindex] = id;
            viewindex++;
        }

        for (String id : mReqUserScreenListBak) {
            if (viewindex >= len)
                break;
            AVView view = new AVView();
            view.videoSrcType = AVView.VIDEO_SRC_TYPE_SCREEN;
            view.viewSizeType = AVView.VIEW_SIZE_TYPE_BIG;
            mRequestViewList[viewindex] = view;
            mRequestIdentifierList[viewindex] = id;
            viewindex++;
        }

        for (String id : mReqUserFileListBak) {
            if (viewindex >= len)
                break;
            AVView view = new AVView();
            view.videoSrcType = AVView.VIDEO_SRC_TYPE_MEDIA;
            view.viewSizeType = AVView.VIEW_SIZE_TYPE_BIG;
            mRequestViewList[viewindex] = view;
            mRequestIdentifierList[viewindex] = id;
            viewindex++;
        }

        bRequsting = true;
        mRoomMulti.requestViewList(mRequestIdentifierList, mRequestViewList, viewindex, mRequestViewListCompleteCallback);
    }

    private void clearRoomInfo(){
        mRoomMulti = null;
        bRequsting = false;
    }
}
