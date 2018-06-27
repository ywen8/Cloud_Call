package com.tencent.ilivesdk.core.impl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.tencent.TIMCallBack;
import com.tencent.TIMElem;
import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.TIMValueCallBack;
import com.tencent.av.NetworkUtil;
import com.tencent.av.TIMAvManager;
import com.tencent.av.opengl.GraphicRendererMgr;
import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.CommonConstants;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.adapter.VideoEngine;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILivePushOption;
import com.tencent.ilivesdk.core.ILiveRecordOption;
import com.tencent.ilivesdk.core.ILiveRoomConfig;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.data.ILivePushRes;
import com.tencent.ilivesdk.data.msg.ILiveCustomMessage;
import com.tencent.ilivesdk.data.msg.ILiveOtherMessage;
import com.tencent.ilivesdk.data.msg.ILiveTextMessage;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.ilivesdk.view.AVVideoView;
import com.tencent.ilivesdk.view.ILiveRootView;
import com.tencent.imsdk.BaseConstants;
import com.tencent.imsdk.IMMsfCoreProxy;
import com.tencent.imsdk.IMMsfUserInfo;
import com.tencent.imsdk.av.MultiVideoTinyId;
import com.tencent.imsdk.util.QualityReportHelper;
import com.tencent.liteav.basicDR.datareport.TXDRApi;
import com.tencent.openqq.protocol.imsdk.gv_comm_operate;

import java.util.ArrayList;
import java.util.List;


/**
 * 房间管理类
 */
public class ILVBRoom extends ILiveRoomManager implements TIMMessageListener, VideoEngine.CameraPreviewCallBack,
        ContextEngine.AVEndPointEvent, ContextEngine.AVRoomDisconnect, ILiveRoomOption.onRequestViewListener,
        AVRootView.onSubViewCreatedListener{
    private static String TAG = "ILVBRoom";
    private static final int IMSDK_MASK = 1;
    private static final int AVSDK_MASK = 2;
    private static final int AVSWITCH_MASK = 4;

    private AVRootView mRootView;           // 统一渲染控件
    private ILiveRootView mRootViewArr[];   // 分开渲染数组

    private int mRoomId;                    // AV Room id
    private float curWhite = 0;

    private String chatRoomId;

    private boolean bAudioInited = false;   // 音频是否完成初始化
    private boolean bCameraEnableUserBak = false;   // 保存摄像头用户意向状态(恢复时使用)
    private boolean bMicEnableUserBak = false;      // 保存Mic用户意向状态(恢复时使用)
    private boolean bSpeakerEnableUserBak = false;  // 保存Speaker用户意向状态(恢复时使用)
    private SensorControl sensorControl;
    private QualityReportHelper helper;
    private boolean isHost = false;             // 区分createRoom和joinRoom
    private int iEnterMask = 0;

    private SurfaceView mSurfaceView = null;

    private ILiveCallBack<Integer> mCameraCallback = new ILiveCallBack<Integer>() {
        @Override
        public void onSuccess(Integer cameraId) {
            ILiveLog.kd(TAG, "enableCamera->onSuccess", new ILiveLog.LogExts().put("cameraId", cameraId));
            if (null != mOption && null != mOption.getCameraListener()) {
                if (ILiveConstants.NONE_CAMERA != cameraId) {
                    mOption.getCameraListener().onCameraEnable(cameraId);
                    renderUserVideo(ILiveLoginManager.getInstance().getMyUserId(), CommonConstants.Const_VideoType_Camera);
                } else {
                    mOption.getCameraListener().onCameraDisable(cameraId);
                }
            }

            TXDRApi.txReportDAU(ILiveSDK.getInstance().getAppContext(), ILiveConstants.EVENT_ILIVE_OPEN_CAMERA_NEW, 0, "success", 10, ILiveSDK.getInstance().getVersion());
        }

        @Override
        public void onError(String module, int errCode, String errMsg) {
            ILiveLog.ke(TAG, "enableCamera->onError", module, errCode, errMsg);
            notifyException(ILiveConstants.EXCEPTION_ENABLE_CAMERA_FAILED, errCode, errMsg);
            TXDRApi.txReportDAU(ILiveSDK.getInstance().getAppContext(), ILiveConstants.EVENT_ILIVE_OPEN_CAMERA_NEW, errCode, errMsg, 10, ILiveSDK.getInstance().getVersion());
        }
    };

    @Override
    public void onCameraPreview(int cameraId) {
        if (null != mOption) {
            mOption.cameraId(cameraId);
        }
        if (null == mOption || mOption.isHostMirror()) {
            if (null != mRootView) {
                AVVideoView myView = mRootView.getUserAvVideoView(ILiveLoginManager.getInstance().getMyUserId(), CommonConstants.Const_VideoType_Camera);
                if (null != myView) {    // 若为前置摄像头，需镜像显示
                    myView.setMirror(cameraId == ILiveConstants.FRONT_CAMERA);
                }
            } else if (null != mRootViewArr) {
                for (ILiveRootView rootView : mRootViewArr) {
                    if (rootView.getVideoView().isLocal())
                        rootView.getVideoView().setMirror(ILiveConstants.FRONT_CAMERA == cameraId);
                }
            }
        }
        if (null != mRootView) { // 根据前后置调整上行角度
            mRootView.setFrontCamera(ILiveConstants.FRONT_CAMERA == cameraId);
        } else if (null != mRootViewArr) {
            for (ILiveRootView rootView : mRootViewArr) {
                rootView.setFrontCamera(ILiveConstants.FRONT_CAMERA == cameraId);
            }
        }
        ILiveLog.ki(TAG, "CameraPreview", new ILiveLog.LogExts().put("cameraId", cameraId));
        if (null != mOption && null != mOption.getCameraListener()) {
            mOption.getCameraListener().onCameraPreviewChanged(cameraId);
        }
        if (null != mOption && mOption.isAutoFocus()) {
            if (null == sensorControl) {
                sensorControl = new SensorControl(ILiveSDK.getInstance().getAppContext());
                sensorControl.startListener((Camera) ILiveSDK.getInstance().getAvVideoCtrl().getCamera());
            } else {
                sensorControl.updateCamera((Camera) ILiveSDK.getInstance().getAvVideoCtrl().getCamera());
            }
        }
    }

    @Override
    public void onComplete(String[] identifierList, AVView[] viewList, int count, int result, String errMsg) {
        if (mOption != null && mOption.getRequestViewListener() != null)
            mOption.getRequestViewListener().onComplete(identifierList, viewList, count, result, errMsg);
        if (AVError.AV_OK == result) {
            for (int i = 0; i < identifierList.length; i++) {
                if (!renderUserVideo(identifierList[i], viewList[i].videoSrcType)) {
                    notifyException(ILiveConstants.EXCEPTION_RENDER_USER_FAILED, ILiveConstants.ERR_SDK_FAILED, identifierList.toString());
                }
            }
        } else {
            notifyException(ILiveConstants.EXCEPTION_REQUEST_VIDEO_FAILED, result, errMsg);
        }
    }

    /**
     * surfaceHolder listener
     */
    private SurfaceHolder.Callback mSurfaceHolderListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null != ILiveSDK.getInstance().getAVContext()) {
                // 初始化渲染view
                ILiveLog.ki(TAG, "onSurfaceCreated->setMgrAndHolder");
                ILiveSDK.getInstance().getAVContext().setRenderMgrAndHolder(GraphicRendererMgr.getInstance(), holder);//获取SurfaceView holder;
            }
            if (null != mRootView && null != mRootView.getmSCUserListner()) {
                mRootView.getmSCUserListner().onSurfaceCreated();
            }

            ILiveLog.ki(TAG, "onSurfaceCreated");
            if (null != mOption && mOption.isAutoCamera()) {    // 自动打开摄像头
                int ret = enableCamera(mOption.getCameraId(), true);
                if (ILiveConstants.NO_ERR != ret) {
                    notifyException(ILiveConstants.EXCEPTION_ENABLE_CAMERA_FAILED, ret, "open camera failed!");
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    // 生成IMGroupId
    private void generateIMGroupId() {
        if (null != mOption && !TextUtils.isEmpty(mOption.getIMGroupId())) {  // 优先采用用户传入的群组id
            ILiveLog.dd(TAG, "generateIMGroupId->custom", new ILiveLog.LogExts().put("groupId", mOption.getIMGroupId()));
            chatRoomId = mOption.getIMGroupId();
        } else if (!TextUtils.isEmpty(chatRoomId)) {          // 其次采用之前绑定的群组
            ILiveLog.dd(TAG, "generateIMGroupId->bind", new ILiveLog.LogExts().put("groupId", chatRoomId));
            if (null != mOption) {
                mOption.imGroupId(chatRoomId);
            }
        } else {     // 最后自动生成
            chatRoomId = mConfig.getGenFunc().generateImGroupId(mRoomId);
            ILiveLog.dd(TAG, "generateIMGroupId->generate", new ILiveLog.LogExts().put("groupId", chatRoomId));
            if (null != mOption) {
                mOption.imGroupId(chatRoomId);
            }
        }
    }

    // 通知房间异常事件
    private void notifyException(int exceptionId, int errCode, String errMsg) {
        ILiveLog.kw(TAG, "notifyException", new ILiveLog.LogExts().put("exceptionId", exceptionId)
                .put("errCode", errCode)
                .put("errMsg", errMsg));
        if (null != mOption && null != mOption.getExceptionListener()) {
            mOption.getExceptionListener().onException(exceptionId, errCode, errMsg);
        }
    }

    // 对外接口
    @Override
    public int init(ILiveRoomConfig config) {
        helper = new QualityReportHelper();
        ILiveLog.ki(TAG, "init");
        this.mConfig = config;

        clearRoomRes();

        // 添加IM消息回调
        ILiveSDK.getInstance().getConversationEngine().addMessageListener(this);
        // 设置房间事件回调
        ILiveSDK.getInstance().getContextEngine().setEndPointEventListener(this);
        ILiveSDK.getInstance().getContextEngine().setRoomDisconnectListener(this);
        ILiveSDK.getInstance().getContextEngine().setRequestCompleteListener(this);
        // 设置摄像头预览回调
        ILiveSDK.getInstance().getVideoEngine().setCameraPreViewCallBack(this);

        return ILiveConstants.NO_ERR;
    }

    @Override
    public void shutdown() {
        // 移除IM消息回调
        ILiveSDK.getInstance().getConversationEngine().removeMessageListener(this);
        mConfig = null;
        ILiveLog.ki(TAG, "shutdown");
    }

    @Override
    public int getRoomId() {
        return mRoomId;
    }

    @Override
    public AVRoomMulti getAvRoom() {
        return (AVRoomMulti) ILiveSDK.getInstance().getContextEngine().getRoomObj();
    }

    @Override
    public String getIMGroupId() {
        return chatRoomId;
    }

    @Override
    public String getHostId() {
        return mOption.getStrHostId();
    }

    /**
     * 获取质量数据，仅限在主线程使用
     */
    @Override
    public ILiveQualityData getQualityData() {
        if (null == ILiveSDK.getInstance().getAVContext() || null == ILiveSDK.getInstance().getAVContext().getRoom()) {
            ILiveLog.kw(TAG, "getQualityData->failed", new ILiveLog.LogExts().put("AVContext", ILiveSDK.getInstance().getAVContext()));
            return null;
        }
        String strTips = ILiveSDK.getInstance().getAVContext().getRoom().getQualityParam();
        if (null == strTips) {   // 空字符串检测
            return null;
        }
        String[] tips = strTips.split(",");
        long loss_rate_recv = 0, loss_rate_send = 0, loss_rate_recv_udt = 0, tick_count_end = 0, cpu_rate_app = 0, cpu_rate_sys = 0, loss_rate_send_udt = 0, tick_count_begin = 0,
                kbps_send = 0, kbps_recv = 0, qos_big_fps = 0;
        for (String tip : tips) {
            if (tip.contains("loss_rate_recv")) {
                loss_rate_recv = getQuality(tip);
            }
            if (tip.contains("loss_rate_send")) {
                loss_rate_send = getQuality(tip);
            }
            if (tip.contains("loss_rate_recv_udt")) {
                loss_rate_recv_udt = getQuality(tip);
            }
            if (tip.contains("loss_rate_send_udt")) {
                loss_rate_send_udt = getQuality(tip);
            }
            if (tip.contains("tick_count_begin")) {
                tick_count_begin = getQuality(tip);
            }
            if (tip.contains("tick_count_end")) {
                tick_count_end = getQuality(tip);
            }
            if (tip.contains("cpu_rate_app")) {
                cpu_rate_app = getQuality(tip);
            }
            if (tip.contains("cpu_rate_sys")) {
                cpu_rate_sys = getQuality(tip);
            }
            if (tip.contains("kbps_send")) {
                kbps_send = getQuality(tip);
            }
            if (tip.contains("kbps_recv")) {
                kbps_recv = getQuality(tip);
            }
            if (tip.contains("qos_big_fps")) {
                qos_big_fps = getQuality(tip);
            }
        }
        ILiveQualityData qd = new ILiveQualityData(tick_count_begin, tick_count_end,
                loss_rate_send, loss_rate_recv,
                cpu_rate_app, cpu_rate_sys,
                kbps_send, kbps_recv, qos_big_fps);
        return qd;

    }

    private long getQuality(String str) {
        long res = 0;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                res = res * 10 + (c - '0');
            }
        }
        return res;
    }

    @Override
    public int createRoom(int roomId, ILiveRoomOption option, final ILiveCallBack callBack) {
        if (null == option || 0 == (option.getAuthBits() & CommonConstants.Const_Auth_Create)) {
            ILiveLog.ke(TAG, "createRoom->failed", ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_INVALID_PARAM, "option is null or no create permission");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_INVALID_PARAM, "option is null or no create permission");
            return ILiveConstants.ERR_INVALID_PARAM;
        }

        isHost = true;
        mRoomId = roomId;
        mOption = option;

        bMicEnableUserBak = mOption.isAutoMic();
        bCameraEnableUserBak |= mOption.isAutoCamera();
        bSpeakerEnableUserBak = mOption.isAutoSpeaker();

        ILiveLog.ki(TAG, "createRoom", new ILiveLog.LogExts().put("roomId", roomId)
                .put("imSupport", mOption.isIMSupport())
                .put("imGroupType", mOption.getGroupType())
                .put("imGroupId", mOption.getIMGroupId())
                .put("avSupport", mOption.isAVSupport())
                .put("role", mOption.getAvControlRole())
                .put("autoCamera", mOption.isAutoCamera()));

        iEnterMask = 0;
        generateIMGroupId();
        if (mOption.isAVSupport())
            enterAVRoom(callBack);
        if (mOption.isIMSupport())
            enterIMGroup(true, callBack);
        checkResult(callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int joinRoom(final int roomId, ILiveRoomOption option, final ILiveCallBack callBack) {
        if (null == option) {
            ILiveLog.ke(TAG, "joinRoom->failed", ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_INVALID_PARAM, "option is null");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_INVALID_PARAM, "option is null");
            return ILiveConstants.ERR_INVALID_PARAM;
        }
        isHost = false;
        mRoomId = roomId;
        mOption = option;
        ILiveLog.ki(TAG, "joinRoom", new ILiveLog.LogExts().put("roomId", roomId)
                .put("imSupport", mOption.isIMSupport())
                .put("imGroupType", mOption.getGroupType())
                .put("imGroupId", mOption.getIMGroupId())
                .put("avSupport", mOption.isAVSupport())
                .put("role", mOption.getAvControlRole())
                .put("autoCamera", mOption.isAutoCamera()));

        bMicEnableUserBak = mOption.isAutoMic();
        bCameraEnableUserBak |= mOption.isAutoCamera();
        bSpeakerEnableUserBak = mOption.isAutoSpeaker();

        iEnterMask = 0;
        generateIMGroupId();
        if (mOption.isAVSupport())
            enterAVRoom(callBack);
        if (mOption.isIMSupport())
            enterIMGroup(false, callBack);
        checkResult(callBack);

        return ILiveConstants.NO_ERR;
    }


    @Override
    public int switchRoom(final int roomId, final ILiveRoomOption option, final ILiveCallBack callBack) {
        if (isHost) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_NOT_SUPPORT, "host cannot switch room");
            return ILiveConstants.ERR_NOT_SUPPORT;
        }
        quitIMGroup();
        if (null != mRootView) {
            mRootView.clearUserView();
        }

        iEnterMask = 0;
        clearRoomRes();
        mRoomId = roomId;
        mOption = option;
        generateIMGroupId();
        if (mOption.isAVSupport())
            switchAVRoom(roomId, callBack);
        if (mOption.isIMSupport())
            enterIMGroup(false, callBack);
        checkResult(callBack);
        return 0;
    }

    @Override
    public int quitRoom(final ILiveCallBack callBack) {
        int ret = 0;
        ILiveLog.ki(TAG, "quitRoom");
        if (mOption != null && mOption.isIMSupport()) {
            quitIMGroup();
        }

        // 关闭摄像头
        if (ILiveConstants.NONE_CAMERA != getActiveCameraId()) {
            enableCamera(getActiveCameraId(), false);
        }

        quitAVRoom(callBack);

        if (null != mRootView) {
            mRootView.clearUserView();
        }

        return ret;
    }

    @Override
    public int linkRoom(int roomId, final String accountId, String sign, ILiveCallBack callBack) {
        ILiveLog.ki(TAG, "linkRoom", new ILiveLog.LogExts().put("roomId", roomId)
                .put("accountId", accountId)
                .put("sign", sign));
        ILiveSDK.getInstance().getContextEngine().linkRoom(roomId, accountId, sign, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int unlinkRoom(final ILiveCallBack callBack) {
        ILiveSDK.getInstance().getContextEngine().unlinkRoom(callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public boolean isEnterRoom() {
        return ILiveSDK.getInstance().getContextEngine().isEnterRoom();
    }

    @Override
    public void onSubViewCreated() {
        if (null != mOption && mOption.isAutoRender()) {
            // 渲染所有已有视频画面
            renderAllVideoView();
        }
    }

    @Override
    public int initAvRootView(AVRootView view) {
        if (null == mRootView || view != mRootView) {
            ILiveLog.ki(TAG, "initAvRootView", new ILiveLog.LogExts()
                    .put("mRootView", null==mRootView?"null":Integer.toHexString(mRootView.hashCode()))
                    .put("view", null==view?"null":Integer.toHexString(view.hashCode())));
            if (null != ILiveLoginManager.getInstance().getAVConext() && null == mSurfaceView) { // 房间已进入未创建悬浮窗
                createSurfaceView(view.getContext());
            }
            mRootView = view;
            mRootView.initView();
            mRootView.getVideoGroup().initAvRootView(mRootView.getContext(), view, this);
        }
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int initRootViewArr(ILiveRootView[] views) {
        if (null != views && 0 != views.length) {
            ILiveLog.ki(TAG, "initRootViewArr", new ILiveLog.LogExts().put("size", views.length));
            mRootViewArr = views;
            if (null != ILiveLoginManager.getInstance().getAVConext() && null == mSurfaceView) { // 房间已进入未创建悬浮窗
                createSurfaceView(mRootViewArr[0].getContext());
            }
            for (int i = 0; i < mRootViewArr.length; i++) {
                mRootViewArr[i].initViews();
            }
        }
        return ILiveConstants.NO_ERR;
    }

    @Override
    public AVRootView getRoomView() {
        return mRootView;
    }

    @Override
    public ILiveRootView[] getRoomViewArr() {
        return mRootViewArr;
    }

    @Override
    public void onPause() {
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()) {
            // SDK尚未初始化
            return;
        }

        if (null != mOption) {
            ILiveLog.ki(TAG, "onPause", new ILiveLog.LogExts().put("mode", mOption.getVideoMode())
                    .put("cameraBak", bCameraEnableUserBak)
                    .put("micBak", bMicEnableUserBak)
                    .put("speakerBak", bSpeakerEnableUserBak));
            switch (mOption.getVideoMode()) {
                case ILiveConstants.VIDEOMODE_BSUPPORT:
                    // 允许后台模式下什么也不做
                    break;
                case ILiveConstants.VIDEOMODE_NORMAL:
                    pauseCameraAndMic();
                    break;
                case ILiveConstants.VIDEOMODE_BMUTE:
                    pauseBgStream();
                    break;
                default:
                    break;
            }
        }

        if (null != mRootView) {
            mRootView.onPause();
        }

        if (null != mRootViewArr) {
            for (int i = 0; i < mRootViewArr.length; i++) {
                mRootViewArr[i].onPause();
            }
        }
    }

    @Override
    public void onResume() {
        // 恢复摄像头与Mic状态
        if (null != mOption) {
            ILiveLog.ki(TAG, "onResume", new ILiveLog.LogExts().put("mode", mOption.getVideoMode())
                    .put("cameraBak", bCameraEnableUserBak)
                    .put("micBak", bMicEnableUserBak)
                    .put("speakerBak", bSpeakerEnableUserBak));
        }

        if (null != mRootView) {
            mRootView.onResume();
        }

        if (!isEnterRoom() || null == ILiveSDK.getInstance().getAVContext()) { // 尚未初始化摄像头回调(未进入房间)，则不处理
            return;
        }

        if (null != mOption) {
            switch (mOption.getVideoMode()) {
                case ILiveConstants.VIDEOMODE_BSUPPORT:
                    // 允许后台模式下什么也不做
                    break;
                case ILiveConstants.VIDEOMODE_NORMAL:
                    resumeCameraAndMic();
                    break;
                case ILiveConstants.VIDEOMODE_BMUTE:
                    resumeBgStream();
                    break;
            }
        }

        if (null != mRootViewArr) {
            for (int i = 0; i < mRootViewArr.length; i++) {
                mRootViewArr[i].onResume();
            }
        }
    }

    @Override
    public void onDestory() {
        ILiveLog.ki(TAG, "onDestory", new ILiveLog.LogExts().put("isEnterRoom", isEnterRoom()));
        if (ILiveSDK.getInstance().getContextEngine().isEnterRoom()) {
            quitRoom(null);
        }
        if (null != mRootView) {
            mRootView.onDestory();
            mRootView = null;
        }
        if (null != mRootViewArr) {
            for (ILiveRootView rootView : mRootViewArr) {
                rootView.onDestory();
            }
            mRootViewArr = null;
        }
        // 删除悬浮窗
        removeSurfaceView(ILiveSDK.getInstance().getAppContext());
    }

    @Override
    public void changeRole(final String role, final ILiveCallBack callBack) {
        ILiveLog.ki(TAG, "changeRole", new ILiveLog.LogExts().put("role", role));
        ILiveSDK.getInstance().getContextEngine().changeRole(role, callBack);
    }

    @Override
    public int startPushStream(ILivePushOption option, final ILiveCallBack<ILivePushRes> callBack) {
        PushUseCase usecase = new PushUseCase(ILiveSDK.getInstance().getComunicationEngine());
        usecase.start(ILiveLoginManager.getInstance().getMyUserId(), mRoomId, option, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int stopPushStream(long channelId, ILiveCallBack callBack) {
        List<Long> ids = new ArrayList<>();
        ids.add(channelId);
        return stopPushStreams(ids, callBack);
    }

    @Override
    public int stopPushStreams(List<Long> ids, final ILiveCallBack callBack) {
        PushUseCase usecase = new PushUseCase(ILiveSDK.getInstance().getComunicationEngine());
        usecase.stop(ILiveLoginManager.getInstance().getMyUserId(), mRoomId, ids, callBack);

        return ILiveConstants.NO_ERR;
    }

    @Override
    public int startRecordVideo(ILiveRecordOption option, final ILiveCallBack callBack) {
        if (null == TIMAvManager.getInstance()) {
            ILiveLog.ke(TAG, "startRecordVideo", ILiveConstants.Module_IMSDK, ILiveConstants.ERR_IM_NOT_READY, "TIMAvManager not found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, ILiveConstants.ERR_IM_NOT_READY, "TIMAvManager not found");
            return ILiveConstants.ERR_NOT_FOUND;
        }
        TIMAvManager.RoomInfo roomInfo = TIMAvManager.getInstance().new RoomInfo();
        roomInfo.setRoomId(mRoomId);
        roomInfo.setRelationId(mRoomId);

        requestMultiVideoRecorderStart(roomInfo, option.getParam(), new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, i, s);
            }

            @Override
            public void onSuccess() {
                ILiveFunc.notifySuccess(callBack, 0);
            }
        });
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int stopRecordVideo(final ILiveCallBack<List<String>> callBack) {
        if (null == TIMAvManager.getInstance()) {
            ILiveLog.ke(TAG, "stopRecordVideo", ILiveConstants.Module_IMSDK, ILiveConstants.ERR_IM_NOT_READY, "TIMAvManager not found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, ILiveConstants.ERR_IM_NOT_READY, "TIMAvManager not found");
            return ILiveConstants.ERR_NOT_FOUND;
        }
        TIMAvManager.RoomInfo roomInfo = TIMAvManager.getInstance().new RoomInfo();
        roomInfo.setRoomId(mRoomId);
        roomInfo.setRelationId(mRoomId);
        TIMAvManager.getInstance().requestMultiVideoRecorderStop(roomInfo, new TIMValueCallBack<List<String>>() {
            @Override
            public void onError(int i, String s) {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, i, s);
            }

            @Override
            public void onSuccess(List<String> files) {
                ILiveFunc.notifySuccess(callBack, files);
            }
        });
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int enableMic(final boolean bEnable) {
        bMicEnableUserBak = bEnable;     // 更新备份状态(允许后台修改)
        ILiveCallBack callBack = new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveLog.ki(TAG, (bEnable?"enableMic":"disableMic")+"->success");
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveLog.ke(TAG, bEnable?"enableMic":"disableMic", ILiveConstants.Module_AVSDK, errCode, errMsg);
            }
        };

        if (bEnable)
            ILiveSDK.getInstance().getAudioEngine().enableMic(callBack);
        else
            ILiveSDK.getInstance().getAudioEngine().disableMic(callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int enableSpeaker(final boolean bEnable) {
        bSpeakerEnableUserBak = bEnable;
        ILiveCallBack callBack = new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveLog.ki(TAG, (bEnable?"enableSpeaker":"disableSpeaker")+"->success");
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveLog.ke(TAG, bEnable?"enableSpeaker":"disableSpeaker", module, errCode, errMsg);
            }
        };
        if (bEnable)
            ILiveSDK.getInstance().getAudioEngine().enableSpeaker(callBack);
        else
            ILiveSDK.getInstance().getAudioEngine().disableSpeaker(callBack);

        return ILiveConstants.NO_ERR;
    }

    @Override
    public int enableCamera(int cameraId, boolean bEnable) {
        ILiveLog.ki(TAG, "enableCamera", new ILiveLog.LogExts().put("cameraId", cameraId).put("enable", bEnable));

        if (bEnable) {
            ILiveSDK.getInstance().getVideoEngine().enableCamera(cameraId, mCameraCallback);
            renderUserVideo(ILiveLoginManager.getInstance().getMyUserId(), CommonConstants.Const_VideoType_Camera);
        } else {
            ILiveSDK.getInstance().getVideoEngine().disableCamera(mCameraCallback);
        }

        bCameraEnableUserBak = bEnable;     // 更新备份状态(允许后台修改)
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int getCurCameraId() {
        if (null != mOption) {
            return mOption.getCameraId();
        }
        return ILiveConstants.FRONT_CAMERA;
    }

    @Override
    public int switchCamera(int cameraId, int dropFrame) {
        ILiveSDK.getInstance().getVideoEngine().switchCamera(cameraId, mCameraCallback);
        if (null != mRootView){
            int idx = mRootView.findUserViewIndex(ILiveLoginManager.getInstance().getMyUserId(),
                    CommonConstants.Const_VideoType_Camera);
            if (ILiveConstants.INVALID_INTETER_VALUE != idx){
                mRootView.getViewByIndex(idx).setDropFrame(dropFrame);
            }
        }else if (null != mRootViewArr){
            for (int i = 0; i < mRootViewArr.length; i++) {
                if (ILiveLoginManager.getInstance().getMyUserId().equals(mRootViewArr[i].getIdentifier())
                        && CommonConstants.Const_VideoType_Camera == mRootViewArr[i].getVideoSrcType()) {
                    mRootViewArr[i].setDropFrame(dropFrame);
                    break;
                }
            }
        }
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int switchCamera(int cameraId) {
        return switchCamera(cameraId, 3);
    }

    @Override
    public int getActiveCameraId() {
        return ILiveSDK.getInstance().getVideoEngine().getActiveCameraId();
    }

    @Override
    public int enableBeauty(float value) {
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()) {
            return ILiveConstants.ERR_AV_NOT_READY;
        }
        ILiveSDK.getInstance().getAvVideoCtrl().inputBeautyParam(value);
        // 恢复美白
        enableWhite(curWhite);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int enableWhite(float value) {
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()) {
            return ILiveConstants.ERR_AV_NOT_READY;
        }
        ILiveSDK.getInstance().getAvVideoCtrl().inputWhiteningParam(value);
        curWhite = value;
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int bindIMGroupId(String groupId) {
        ILiveLog.ki(TAG, "bindIMGroupId", new ILiveLog.LogExts().put("groupId", groupId));
        chatRoomId = groupId;
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int unBindIMGroupId() {
        ILiveLog.ki(TAG, "unBindIMGroupId->enter");
        chatRoomId = null;
        return ILiveConstants.NO_ERR;
    }

    private void innerSendC2CMessage(String dstUser, TIMMessage message, final ILiveCallBack<TIMMessage> callBack){
        ILiveSDK.getInstance().getConversationEngine().sendC2CMessage(dstUser, message, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, 0, "");
                helper.report();//数据采集
                ILiveFunc.notifySuccess(callBack, data);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, errCode, errMsg);
                helper.report();//数据采集
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }
        });
    }

    public void innerSendGroupMessage(String grpId, TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        ILiveSDK.getInstance().getConversationEngine().sendGroupMessage(grpId, message, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, 0, "");
                helper.report();//数据采集
                ILiveFunc.notifySuccess(callBack, data);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, errCode, errMsg);
                helper.report();//数据采集
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }
        });
    }

    public void innerSendC2COnlineMessage(String dstUser, TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        ILiveSDK.getInstance().getConversationEngine().sendOnlineC2CMessage(dstUser, message, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, 0, "");
                helper.report();//数据采集
                ILiveFunc.notifySuccess(callBack, data);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, errCode, errMsg);
                helper.report();//数据采集
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }
        });
    }

    public void innerSendGroupOnlineMessage(String grpId, TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        ILiveSDK.getInstance().getConversationEngine().sendOnlineGroupMessage(grpId, message, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, 0, "");
                helper.report();//数据采集
                ILiveFunc.notifySuccess(callBack, data);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                helper.init(ILiveConstants.EVENT_SEND_GROUP_TEXT_MSG, errCode, errMsg);
                helper.report();//数据采集
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
            }
        });
    }

    @Override
    public int sendC2CMessage(String dstUser, TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        innerSendC2CMessage(dstUser, message, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int sendGroupMessage(TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        innerSendGroupMessage(getIMGroupId(), message, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int sendC2COnlineMessage(String dstUser, TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        innerSendC2COnlineMessage(dstUser, message, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public int sendGroupOnlineMessage(TIMMessage message, final ILiveCallBack<TIMMessage> callBack) {
        innerSendGroupOnlineMessage(getIMGroupId(), message, callBack);
        return ILiveConstants.NO_ERR;
    }

    @Override
    public void sendC2CMessage(String dstUser, ILiveMessage message, ILiveCallBack callBack) {
        TIMMessage timMessage = new TIMMessage();
        timMessage.addElement(message.getTIMElem());
        innerSendC2CMessage(dstUser, timMessage, callBack);
    }

    @Override
    public void sendGroupMessage(ILiveMessage message, ILiveCallBack callBack) {
        sendGroupMessage(getIMGroupId(), message, callBack);
    }

    @Override
    public void sendGroupMessage(String grpId, ILiveMessage message, ILiveCallBack callBack) {
        TIMMessage timMessage = new TIMMessage();
        timMessage.addElement(message.getTIMElem());
        innerSendGroupMessage(grpId, timMessage, callBack);
    }

    @Override
    public void sendC2COnlineMessage(String dstUser, ILiveMessage message, ILiveCallBack callBack) {
        TIMMessage timMessage = new TIMMessage();
        timMessage.addElement(message.getTIMElem());
        innerSendC2COnlineMessage(dstUser, timMessage, callBack);
    }

    @Override
    public void sendGroupOnlineMessage(ILiveMessage message, ILiveCallBack callBack) {
        sendGroupOnlineMessage(getIMGroupId(), message, callBack);
    }

    @Override
    public void sendGroupOnlineMessage(String grpId, ILiveMessage message, ILiveCallBack callBack) {
        TIMMessage timMessage = new TIMMessage();
        timMessage.addElement(message.getTIMElem());
        innerSendGroupOnlineMessage(grpId, timMessage, callBack);
    }

    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        ILiveLog.kd(TAG, "onNewMessages", new ILiveLog.LogExts().put("size", list.size())
                .put("listener", mConfig.getRoomMessageListener()));
        if (null != mConfig.getRoomMessageListener()) {
            mConfig.getRoomMessageListener().onNewMessages(list);
        }
        if (null != mConfig.getRoomMsgListener()) {
            for (TIMMessage message : list) {
                for (int i = 0; i < message.getElementCount(); i++) {
                    TIMElem elem = message.getElement(i);
                    ILiveMessage iliveMsg = null;
                    switch (elem.getType()) {
                        case Text:
                            iliveMsg = new ILiveTextMessage(elem);
                            break;
                        case Custom:
                            iliveMsg = new ILiveCustomMessage(elem);
                            break;
                        default:
                            iliveMsg = new ILiveOtherMessage(elem);
                            break;
                    }
                    iliveMsg.setSender(message.getSender());
                    iliveMsg.setTimeStamp(message.timestamp());
                    iliveMsg.setConversation(message.getConversation());
                    mConfig.getRoomMsgListener().onNewMessage(iliveMsg);
                }
            }
        }
        return false;
    }

    @Override
    public void onRoomDisconnect(int errCode, String errMsg) {
        ILiveLog.ke(TAG, "onRoomDisconnect", ILiveConstants.Module_AVSDK, errCode, errMsg);
        // 关闭摄像头
        if (ILiveConstants.NONE_CAMERA != getActiveCameraId()) {
            enableCamera(getActiveCameraId(), false);
        }
        if (null != mOption && mOption.isIMSupport()) { // 退出IM房间
            quitIMGroup();
        }
        if (null != mOption.getRoomDisconnectListener()) {
            mOption.getRoomDisconnectListener().onRoomDisconnect(errCode, "room disconnected");
        }
        removeSurfaceView(ILiveSDK.getInstance().getAppContext());
        clearRoomRes();
    }

    @Override
    public void onEndPointEvent(int eventid, String[] updateList) {
        //设置了外部回调
        if (mOption == null) return;

        if (!bAudioInited) {
            if (null != mOption.getAudioInitCompletedListener()) {
                ILiveLog.di(TAG, "onEndPointEvent->notify");
                mOption.getAudioInitCompletedListener().onAudioInitCompleted();
            }
            bAudioInited = true;
        }

        if (mOption.getMemberStatusLisenter() != null) {
            if (mOption.getMemberStatusLisenter().onEndpointsUpdateInfo(eventid, updateList) == true) {
                ILiveLog.kw(TAG, "onEndPointEvent->custom", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                return;
            }
        }

        switch (eventid) {
            case ILiveConstants.TYPE_MEMBER_CHANGE_IN:
                ILiveLog.ki(TAG, "onEndPointEvent->in", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_CAMERA_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->hasCamera", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                //如果有自己直接渲染
                for (String id : updateList) {
                    if (id.equals(ILiveLoginManager.getInstance().getMyUserId())) {
                        ILiveLog.di(TAG, "onEndPointEvent-", new ILiveLog.LogExts().put("eventId", eventid)
                                .put("users", ILiveFunc.getArrStr(updateList)));
                        renderUserVideo(id, CommonConstants.Const_VideoType_Camera);
                        return;
                    } else if (mOption.isAutoRender()) {
                        ILiveSDK.getInstance().getContextEngine().requestUserVideoData(id, CommonConstants.Const_VideoType_Camera);
                    }
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_Camera, true);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_SCREEN_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->hasScreen", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                //如果有自己直接渲染
                for (String id : updateList) {
                    if (id.equals(ILiveLoginManager.getInstance().getMyUserId())) {
                        renderUserVideo(id, CommonConstants.Const_VideoType_Screen);
                        return;
                    } else if (mOption.isAutoRender()) {
                        ILiveSDK.getInstance().getContextEngine().requestUserVideoData(id, CommonConstants.Const_VideoType_Screen);
                    }
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_Screen, true);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_FILE_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->hasFile", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                //如果有自己直接渲染
                for (String id : updateList) {
                    if (id.equals(ILiveLoginManager.getInstance().getMyUserId())) {
                        renderUserVideo(id, CommonConstants.Const_VideoType_File);
                        return;
                    } else if (mOption.isAutoRender()) {
                        ILiveSDK.getInstance().getContextEngine().requestUserVideoData(id, CommonConstants.Const_VideoType_File);
                    }
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_File, true);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_CAMERA_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->noCamera", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                for (String id : updateList) {
                    closeUserVideo(id, CommonConstants.Const_VideoType_Camera);
                    // 移除备份信息
                    ILiveSDK.getInstance().getContextEngine().removeUserVideoData(id, CommonConstants.Const_VideoType_Camera);
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_Camera, false);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_SCREEN_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->noScreen", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                for (String id : updateList) {
                    closeUserVideo(id, CommonConstants.Const_VideoType_Screen);
                    // 移除备份信息
                    ILiveSDK.getInstance().getContextEngine().removeUserVideoData(id, CommonConstants.Const_VideoType_Screen);
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_Screen, false);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_FILE_VIDEO:
                ILiveLog.ki(TAG, "onEndPointEvent->noFile", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                if (null == updateList) return;
                for (String id : updateList) {
                    closeUserVideo(id, CommonConstants.Const_VideoType_File);
                    // 移除备份信息
                    ILiveSDK.getInstance().getContextEngine().removeUserVideoData(id, CommonConstants.Const_VideoType_File);
                    notifyVideoVideoEvent(id, CommonConstants.Const_VideoType_File, false);
                }
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_AUDIO:
                ILiveLog.ki(TAG, "onEndPointEvent->hasAudio", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_AUDIO:
                ILiveLog.ki(TAG, "onEndPointEvent->noAudio", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_OUT:
                ILiveLog.ki(TAG, "onEndPointEvent->out", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                break;
            default:
                ILiveLog.kw(TAG, "onEndPointEvent->ignore", new ILiveLog.LogExts().put("eventId", eventid)
                        .put("users", ILiveFunc.getArrStr(updateList)));
                break;
        }
    }

    private void clearRoomRes() {
        ILiveLog.ki(TAG, "clearRoomRes->enter");
        bAudioInited = false;
        bCameraEnableUserBak = false;
        bMicEnableUserBak = false;
        bSpeakerEnableUserBak = false;
        mOption = null;
        chatRoomId = null;
        isHost = false;
        if (null != sensorControl) {
            sensorControl.stopListener();
            sensorControl = null;
        }
        // 重置房间质量信息
        ILiveQualityData.clearLive();
        mRoomId = ILiveConstants.INVALID_INTETER_VALUE;
    }

    private void pauseCameraAndMic() {
        if (bCameraEnableUserBak) {
            enableCamera(mOption.getCameraId(), false);
            bCameraEnableUserBak = true;   // 恢复修改
        }
        if (bMicEnableUserBak) {
            enableMic(false);
            bMicEnableUserBak = true;   // 恢复修改
        }
    }

    private void resumeCameraAndMic() {
        if (bCameraEnableUserBak) {
            int ret = enableCamera(mOption.getCameraId(), true);
            if (ILiveConstants.NO_ERR != ret) {
                notifyException(ILiveConstants.EXCEPTION_ENABLE_CAMERA_FAILED, ret, "open camera failed!");
            }
        }
        if (bMicEnableUserBak) {
            int ret = enableMic(true);
            if (ILiveConstants.NO_ERR != ret) {
                notifyException(ILiveConstants.EXCEPTION_ENABLE_MIC_FAILED, ret, "open camera failed!");
            }
        }
    }

    private void pauseBgStream() {
        pauseCameraAndMic();
        ILiveSDK.getInstance().getContextEngine().pauseUserData();
        if (bSpeakerEnableUserBak) {
            enableSpeaker(false);
            bSpeakerEnableUserBak = true;
        }
    }

    private void resumeBgStream() {
        resumeCameraAndMic();
        ILiveSDK.getInstance().getContextEngine().resumeUserData();
        if (bSpeakerEnableUserBak) {
            enableSpeaker(true);
        }
    }

    /**
     * 视频渲染
     */
    private boolean renderUserVideo(String id, int srcType) {
        if (null != mRootView) {
            int otherType = (CommonConstants.Const_VideoType_Camera == srcType ? CommonConstants.Const_VideoType_Screen : CommonConstants.Const_VideoType_Camera);
            int idx = mRootView.findUserViewIndex(id, otherType);
            if (ILiveConstants.INVALID_INTETER_VALUE != idx && mRootView.getViewByIndex(idx) != null && !mRootView.getViewByIndex(idx).isRendering()) {    // 关闭主播其它的无效视频
                mRootView.closeUserView(id, otherType, true);
                ILiveLog.ki(TAG, "renderUserVideo->zambie", new ILiveLog.LogExts().put("index", idx)
                        .put("id", id)
                        .put("type", otherType));
            }
            return mRootView.renderVideoView(true, id, srcType, null == mOption ? true : mOption.isAutoRender());
        } else if (null != mRootViewArr) {
            for (int i = 0; i < mRootViewArr.length; i++) {
                if (id.equals(mRootViewArr[i].getIdentifier()) && srcType == mRootViewArr[i].getVideoSrcType()) {
                    mRootViewArr[i].render(id, srcType);
                    return true;
                }
            }
            if (null != mOption && mOption.isAutoRender()) {    // 开启自动渲染去查找空闲view
                for (int i = 0; i < mRootViewArr.length; i++) {
                    if (!mRootViewArr[i].isRendering()) {
                        mRootViewArr[i].render(id, srcType);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void closeUserVideo(String id, int srcType) {
        if (null != mOption && mOption.isAutoRender()) {
            if (null != mRootView) {
                if (id.equals(mOption.getStrHostId())) {
                    int otherType = (CommonConstants.Const_VideoType_Camera == srcType ? CommonConstants.Const_VideoType_Screen : CommonConstants.Const_VideoType_Camera);
                    if (ILiveConstants.INVALID_INTETER_VALUE == mRootView.findUserViewIndex(id, otherType)) {    // 是主播仅有的一路视频
                        int idx = mRootView.findUserViewIndex(id, srcType);
                        if (ILiveConstants.INVALID_INTETER_VALUE != idx) {
                            mRootView.getViewByIndex(idx).setRendering(false);
                        }
                        ILiveLog.kw(TAG, "closeUserVideo->host", new ILiveLog.LogExts().put("index", idx)
                                .put("type", srcType));
                        return;
                    }
                }
                mRootView.closeUserView(id, srcType, true);
            } else if (null != mRootViewArr) {
                for (int i = 0; i < mRootViewArr.length; i++) {
                    if (id.equals(mRootViewArr[i].getIdentifier())) {
                        mRootViewArr[i].closeVideo();
                        return;
                    }
                }
            }
        }
    }

    // 删除SurfaceView
    private void removeSurfaceView(Context context) {
        if (null != mSurfaceView) {
            ILiveLog.ki(TAG, "removeSurfaceView->enter");
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            try {
                windowManager.removeView(mSurfaceView);
                mSurfaceView = null;
            } catch (Exception e) {
                ILiveLog.dw(TAG, "removeSurfaceView", new ILiveLog.LogExts().put("exception", e.toString()));
            }
        }
    }

    // 创建悬浮窗，用于打开摄像头
    private void createSurfaceView(Context context) {
        removeSurfaceView(context);    // 若之前有创建，先删除

        WindowManager windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = 1;
        layoutParams.height = 1;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        // layoutParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.windowAnimations = 0;// android.R.style.Animation_Toast;
        //layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if(Build.VERSION.SDK_INT > 24){
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }else{
                layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            }
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        //layoutParams.setTitle("Toast");
        try {
            mSurfaceView = new SurfaceView(context);
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.addCallback(mSurfaceHolderListener);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 3.0以下必须在初始化时调用，否则不能启动预览
            mSurfaceView.setZOrderMediaOverlay(true);
            windowManager.addView(mSurfaceView, layoutParams);
        } catch (IllegalStateException e) {
            windowManager.updateViewLayout(mSurfaceView, layoutParams);
            ILiveLog.kw(TAG, "createSurfaceView", new ILiveLog.LogExts().put("illegal", e.toString()));
        } catch (Exception e) {
            ILiveLog.kw(TAG, "createSurfaceView", new ILiveLog.LogExts().put("exception", e.toString()));
        }
        ILiveLog.ki(TAG, "createSurfaceView->enter");
    }

    /**
     * 发起开始录制请求
     *
     * @param roomInfo 录制房间相关信息，详见{@see RoomInfo}
     * @param param    录制参数，详见{@see RecordParam}
     * @param cb       回调
     */
    private void requestMultiVideoRecorderStart(TIMAvManager.RoomInfo roomInfo, TIMAvManager.RecordParam param, final TIMCallBack cb) {
        if (ILiveLoginManager.getInstance().getMyUserId() == null) return;
        IMMsfUserInfo msfUserInfo = IMMsfCoreProxy.get().getMsfUserInfo(ILiveLoginManager.getInstance().getMyUserId());
        if (null == msfUserInfo || !msfUserInfo.isLoggedIn()) {
            cb.onError(BaseConstants.ERR_SDK_NOT_LOGGED_IN, "current user not login. id: " + ILiveLoginManager.getInstance().getMyUserId());
            return;
        }

        StreamerRecorderContext context = new StreamerRecorderContext();
        context.busiType = 7;
        context.authType = 6;
        context.authKey = mRoomId;
        context.roomId = mRoomId;
        context.sdkAppId = IMMsfCoreProxy.get().getSdkAppId();
        context.uin = msfUserInfo.getTinyid();
        context.recordParam = param;
        context.operation = 1;
        context.subcmd = 0x142;

        requestMultiVideoRecorderRelay(context, new TIMValueCallBack<List<String>>() {
            @Override
            public void onError(int code, String desc) {
                cb.onError(code, desc);
            }

            @Override
            public void onSuccess(List<String> t) {
                cb.onSuccess();
            }
        });
    }


    private void requestMultiVideoRecorderRelay(StreamerRecorderContext context, final TIMValueCallBack<List<String>> cb) {

        if (context.sig != null && context.sig.getBytes().length > 256) {
            cb.onError(-1, "Invalid signature, length is limited to 256 bytes");
        }

        gv_comm_operate.GVCommOprHead head = new gv_comm_operate.GVCommOprHead();
        head.uint32_buss_type.set(context.busiType);        //opensdk
        head.uint32_auth_type.set(context.authType);        //opensdk
        head.uint32_auth_key.set(context.authKey);
        head.uint64_uin.set(context.uin);
        head.uint32_sdk_appid.set(context.sdkAppId);

        gv_comm_operate.ReqBody reqbody = new gv_comm_operate.ReqBody();

        reqbody.req_0x5.setHasFlag(true);
        reqbody.req_0x5.uint32_oper.set(context.operation);
        reqbody.req_0x5.uint32_seq.set(IMMsfCoreProxy.get().random.nextInt());
        if (context.recordParam != null) {
            if (context.recordParam.filename() != null) {
                reqbody.req_0x5.string_file_name.set(context.recordParam.filename());
            }

            reqbody.req_0x5.uint32_classid.set(context.recordParam.classId());
            reqbody.req_0x5.uint32_IsTransCode.set(context.recordParam.isTransCode() ? 1 : 0);
            reqbody.req_0x5.uint32_IsScreenShot.set(context.recordParam.isScreenShot() ? 1 : 0);
            reqbody.req_0x5.uint32_IsWaterMark.set(context.recordParam.isWaterMark() ? 1 : 0);
            if (context.recordParam.getRecordType() != TIMAvManager.RecordType.VIDEO) {
                reqbody.req_0x5.uint32_record_type.set(context.recordParam.getRecordType().getValue());
            }
            for (String tag : context.recordParam.tags()) {
                reqbody.req_0x5.string_tags.add(tag);
            }

            reqbody.req_0x5.uint32_sdk_type.set(0x01);

        }

        byte[] busibuf = NetworkUtil.formReq(ILiveLoginManager.getInstance().getMyUserId(), context.subcmd, context.roomId, context.sig,
                head.toByteArray(), reqbody.toByteArray());

        //do the request
        MultiVideoTinyId.get().requestMultiVideoInfo(busibuf, new TIMValueCallBack<byte[]>() {
            @Override
            public void onError(int code, String desc) {
                cb.onError(code, desc);
            }


            @Override
            public void onSuccess(byte[] rspbody) {
                gv_comm_operate.RspBody rsp = new gv_comm_operate.RspBody();

                byte[] buff = NetworkUtil.parseRsp(rspbody);
                if (buff == null) {
                    cb.onError(BaseConstants.ERR_PARSE_RESPONSE_FAILED, "parse recorder rsp failed");
                    return;
                }

                try {
                    rsp.mergeFrom(buff);
                } catch (Throwable e) {
                    cb.onError(BaseConstants.ERR_PARSE_RESPONSE_FAILED, "parse recorder rsp failed");
                    return;
                }

                if (rsp.rsp_0x5.uint32_result.get() != 0) {

                    cb.onError(rsp.rsp_0x5.uint32_result.get(), rsp.rsp_0x5.str_errorinfo.get());
                    return;
                }

                cb.onSuccess(rsp.rsp_0x5.str_fileID.get());
            }
        });
    }

    private void notifyVideoVideoEvent(String id, int srcType, boolean bHasVideo) {
        if (null != mRootView) {
            AVVideoView videoView = mRootView.getUserAvVideoView(id, srcType);
            if (null != videoView && null != videoView.getVideoListener()) {
                if (bHasVideo)
                    videoView.getVideoListener().onHasVideo(srcType);
                else
                    videoView.getVideoListener().onNoVideo(srcType);
            }
        } else if (null != mRootViewArr) {
            for (int i = 0; i < mRootViewArr.length; i++) {
                if (id.equals(mRootViewArr[i]) && srcType == mRootViewArr[i].getVideoSrcType()) {
                    if (null != mRootViewArr[i].getVideoListener()) {
                        if (bHasVideo)
                            mRootViewArr[i].getVideoListener().onHasVideo(srcType);
                        else
                            mRootViewArr[i].getVideoListener().onNoVideo(srcType);
                    }
                    return;
                }
            }
        }
    }

    // 登录成功后初始化悬浮层
    public void afterLogin() {
        if (null == mSurfaceView) {
            createSurfaceView(ILiveSDK.getInstance().getAppContext());
        }
    }

    private void checkResult(ILiveCallBack callBack) {
        if (0 == iEnterMask) {
            ILiveFunc.notifySuccess(callBack, 0);
        }
    }

    // 进入IM群组
    private void enterIMGroup(final boolean bCreated, final ILiveCallBack callBack) {
        // 需要加入IM房间
        iEnterMask |= IMSDK_MASK;
        ILiveCallBack enterCallBack = new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                //数据采集
                helper.init(bCreated ? ILiveConstants.EVENT_CREATE_AVCHATROOM : ILiveConstants.EVENT_JOIN_AVCHATROOM, 0, "");
                helper.report();

                iEnterMask &= (~IMSDK_MASK);
                ILiveLog.ki(TAG, "enterIMGroup->succuess", new ILiveLog.LogExts().put("iEnterMask", Integer.toHexString(iEnterMask)));
                checkResult(callBack);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                if (errCode == ILiveConstants.IS_ALREADY_MEMBER || errCode == ILiveConstants.IS_ALREDY_MASTER) {   //已在房间中,重复进入房间
                    notifyException(ILiveConstants.EXCEPTION_IMROOM_EXIST, errCode, "room exist");
                    iEnterMask &= (~IMSDK_MASK);
                    ILiveLog.dw(TAG, "enterIMGroup->exist", new ILiveLog.LogExts().put("iEnterMask", Integer.toHexString(iEnterMask)));
                    checkResult(callBack);
                } else {
                    helper.init(bCreated ? ILiveConstants.EVENT_CREATE_AVCHATROOM : ILiveConstants.EVENT_JOIN_AVCHATROOM, errCode, errMsg);
                    helper.report();
                    ILiveLog.ke(TAG, "enterIMGroup", ILiveConstants.Module_IMSDK, errCode, errMsg);
                    helper.init(bCreated ? ILiveConstants.EVENT_ILIVE_CREATEROOM : ILiveConstants.EVENT_ILIVE_JOINROOM, errCode, errMsg);
                    helper.report();//数据采集
                    ILiveFunc.notifyError(callBack, ILiveConstants.Module_IMSDK, errCode, errMsg);
                    quitAVRoom(null);
                }
            }
        };
        ILiveLog.ki(TAG, "enterIMGroup", new ILiveLog.LogExts().put("groupId", getIMGroupId())
                .put("groupType", mOption.getGroupType()).put("iEnterMask", Integer.toHexString(iEnterMask)));
        if (bCreated) {
            ILiveSDK.getInstance().getGroupEngine().createGroup(getIMGroupId(), getIMGroupId(), mOption.getGroupType(), enterCallBack);
        } else {
            ILiveSDK.getInstance().getGroupEngine().joinGroup(getIMGroupId(), mOption.getGroupType(), enterCallBack);
        }
    }

    // 退出IM群组
    private void quitIMGroup() {
        if (null != mOption && mOption.isIMSupport()) {
            ILiveLog.ki(TAG, "quitIMGroup", new ILiveLog.LogExts().put("isHost", isHost));
            if (isHost) {
                ILiveSDK.getInstance().getGroupEngine().deleteGroup(getIMGroupId(), null);
            } else {
                //成员退出群
                ILiveSDK.getInstance().getGroupEngine().quitGroup(getIMGroupId(), null);
            }
            chatRoomId = null;
        }
    }

    // 进入AV房间
    private void enterAVRoom(final ILiveCallBack callBack) {
        iEnterMask |= AVSDK_MASK;
        ILiveLog.ki(TAG, "enterAVRoom->enter", new ILiveLog.LogExts().put("iEnterMask", Integer.toHexString(iEnterMask)));
        ILiveSDK.getInstance().getAudioEngine().start(null);
        ILiveSDK.getInstance().getContextEngine().enterRoom(mRoomId, mOption, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                TXDRApi.txReportDAU(ILiveSDK.getInstance().getAppContext(), ILiveConstants.EVENT_ILIVE_ENTER_ROOM_NEW, 0, "success", 10, ILiveSDK.getInstance().getVersion());
                helper.init(isHost ? ILiveConstants.EVENT_ILIVE_CREATEROOM : ILiveConstants.EVENT_ILIVE_JOINROOM, 0, "");
                helper.report();//数据采集
                if (null != mOption) {    // 自动打开摄像头
                    ILiveLog.ki(TAG, "enterAVRoom->enableCamera", new ILiveLog.LogExts().put("autoCamera", mOption.isAutoCamera())
                        .put("curCamera", getActiveCameraId()).put("cameraId", mOption.getCameraId()));
                    if (mOption.isAutoCamera() && ILiveConstants.NONE_CAMERA == getActiveCameraId()) {
                        int ret = enableCamera(mOption.getCameraId(), true);
                        if (ILiveConstants.NO_ERR != ret) {
                            notifyException(ILiveConstants.EXCEPTION_ENABLE_CAMERA_FAILED, ret, "open camera failed!");
                        }
                    }
                }
                iEnterMask &= (~AVSDK_MASK);
                ILiveLog.ki(TAG, "enterAVRoom->succuess", new ILiveLog.LogExts().put("iEnterMask", Integer.toHexString(iEnterMask)));
                checkResult(callBack);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                TXDRApi.txReportDAU(ILiveSDK.getInstance().getAppContext(), ILiveConstants.EVENT_ILIVE_ENTER_ROOM_NEW, errCode, errMsg, 10, ILiveSDK.getInstance().getVersion());
                helper.init(isHost ? ILiveConstants.EVENT_ILIVE_CREATEROOM : ILiveConstants.EVENT_ILIVE_JOINROOM, errCode, errMsg);
                helper.report();//数据采集
                ILiveLog.ke(TAG, "enterAVRoom", module, errCode, errMsg);
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, errCode, errMsg);
                quitIMGroup();
            }
        });
    }

    // 切换AV房间
    private void switchAVRoom(final int roomId, final ILiveCallBack callBack) {
        iEnterMask |= AVSWITCH_MASK;
        ILiveLog.ki(TAG, "switchAVRoom->enter");
        ILiveSDK.getInstance().getContextEngine().switchRoom(roomId, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                mRoomId = roomId;
                iEnterMask &= (~AVSWITCH_MASK);
                checkResult(callBack);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, errCode, errMsg);
            }
        });
    }

    // 退出AV房间
    private void quitAVRoom(final ILiveCallBack callBack) {
        ILiveLog.ki(TAG, "quitAVRoom->enter");
        // 退出房间前关闭服务
        ILiveSDK.getInstance().getAudioEngine().stop(null);
        ILiveSDK.getInstance().getContextEngine().exitRoom(new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveLog.ki(TAG, "quitAVRoom->onSuccess");
                if (null != mRootView) {
                    mRootView.clearUserView();
                }
                clearRoomRes();
                ILiveFunc.notifySuccess(callBack, 0);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                ILiveLog.ke(TAG, "quitAVRoom", module, errCode, errMsg);
                ILiveFunc.notifyError(callBack, module, errCode, errMsg);
            }
        });
    }

    // 渲染所有视频画面
    private void renderAllVideoView(){
        ILiveLog.ki(TAG, "renderAllVideoView->enter");
        ContextEngine contextEngine = ILiveSDK.getInstance().getContextEngine();
        // 显示摄像头画面
        for (String id : contextEngine.getVideoUserList(CommonConstants.Const_VideoType_Camera)){
            renderUserVideo(id, CommonConstants.Const_VideoType_Camera);
        }

        // 显示屏幕分享画面
        for (String id : contextEngine.getVideoUserList(CommonConstants.Const_VideoType_Screen)){
            renderUserVideo(id, CommonConstants.Const_VideoType_Screen);
        }

        // 显示文件分享画面
        for (String id : contextEngine.getVideoUserList(CommonConstants.Const_VideoType_File)){
            renderUserVideo(id, CommonConstants.Const_VideoType_File);
        }

        if (ILiveConstants.NONE_CAMERA != getActiveCameraId()){
            // 渲染本地画面
            renderUserVideo(ILiveLoginManager.getInstance().getMyUserId(), CommonConstants.Const_VideoType_Camera);
        }
    }
}


