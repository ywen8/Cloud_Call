package com.tencent.ilivesdk.adapter.avsdk_impl;

import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.adapter.VideoEngine;
import com.tencent.ilivesdk.core.ILiveLog;

/**
 * AVSDK音频模块
 */
public class AVSDKVideoCtrl implements VideoEngine {
    private final String TAG = "ILVB-AVSDKVideoCtrl";
    /**
     * 当前打开的摄像头
     */
    private int curCameraId = ILiveConstants.NONE_CAMERA;
    /**
     * 操作时间
     */
    private long uOperateTime = 0;
    /**
     * 超时时间(默认为2秒)
     */
    private int uTimeOut = 2;

    private AVVideoCtrl avVideoCtrl;

    class AVEnableCameraCallBack extends AVVideoCtrl.EnableCameraCompleteCallback {
        private ILiveCallBack<Integer> callBack;
        private int cameraId;

        public AVEnableCameraCallBack(int cameraId, ILiveCallBack<Integer> callBack) {
            this.cameraId = cameraId;
            this.callBack = callBack;
        }

        @Override
        protected void onComplete(boolean bEnable, int iError) {
            super.onComplete(bEnable, iError);
            uOperateTime = 0;
            ILiveLog.dd(TAG, "enableCamera->onComplete", new ILiveLog.LogExts().put("bEnable", bEnable).put("errCode", iError));
            if (AVError.AV_OK == iError) {
                updateCameraId(bEnable ? cameraId : ILiveConstants.NONE_CAMERA);
                ILiveFunc.notifySuccess(callBack, curCameraId);
            } else {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, iError, "Operate Camera failed");
            }
        }
    }

    class AVSwitchCameraCallBack extends AVVideoCtrl.SwitchCameraCompleteCallback {
        private int cameraId;
        private ILiveCallBack<Integer> callBack;

        public AVSwitchCameraCallBack(int cameraId, ILiveCallBack<Integer> callBack) {
            this.cameraId = cameraId;
            this.callBack = callBack;
        }

        @Override
        protected void onComplete(int cameraId, int result) {
            super.onComplete(cameraId, result);
            uOperateTime = 0;
            ILiveLog.dd(TAG, "switchCamera->onComplete", new ILiveLog.LogExts().put("cameraId", cameraId).put("errCode", result));
            if (AVError.AV_OK == result) {
                updateCameraId(this.cameraId);
                ILiveFunc.notifySuccess(callBack, curCameraId);
            } else {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, result, "Switch Camera failed");
            }
        }
    }

    class AVCameraPreviewCallBack extends AVVideoCtrl.CameraPreviewChangeCallback {
        private CameraPreviewCallBack callBack;

        @Override
        public void onCameraPreviewChangeCallback(int cameraId) {
            if (null != callBack) {
                callBack.onCameraPreview(cameraId);
            }
            super.onCameraPreviewChangeCallback(cameraId);
        }

        public void setCallBack(CameraPreviewCallBack callBack) {
            this.callBack = callBack;
        }
    }

    private AVCameraPreviewCallBack mCameraPreviewCallBack = new AVCameraPreviewCallBack();

    @Override
    public void init(ContextEngine context, ILiveCallBack callBack) {
        avVideoCtrl = (AVVideoCtrl) context.getModuleVar("Video");
        avVideoCtrl.setCameraPreviewChangeCallback(mCameraPreviewCallBack);
        curCameraId = ILiveConstants.NONE_CAMERA;       // 重置摄像头状态
        if (null != avVideoCtrl) {
            ILiveFunc.notifySuccess(callBack, 0);
        } else {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "get AudioCtrl failed");
        }
    }

    @Override
    public void setTimeOut(int timeOut) {
        this.uTimeOut = timeOut;
    }

    @Override
    public int getCameraNum() {
        if (null == avVideoCtrl) {
            return 0;
        }
        return avVideoCtrl.getCameraNum();
    }

    @Override
    public Object getCamera() {
        if (null == avVideoCtrl) {
            return null;
        }
        return avVideoCtrl.getCamera();
    }

    @Override
    public Object getCameraPara() {
        if (null == avVideoCtrl) {
            return null;
        }
        return avVideoCtrl.getCameraPara();
    }

    @Override
    public void enableCamera(int cameraId, ILiveCallBack<Integer> callBack) {
        if (null == avVideoCtrl) {
            ILiveLog.ke(TAG, "enableCamera->failed", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            return;
        }
        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveLog.kw(TAG, "enableCamera", new ILiveLog.LogExts().put("operateTime", uOperateTime).put("curTime", uCurTime).put("timeout", uTimeOut));
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        } else {
            avVideoCtrl.enableCamera(cameraId, true, new AVEnableCameraCallBack(cameraId, callBack));
            uOperateTime = uCurTime;
        }
    }

    @Override
    public void disableCamera(ILiveCallBack<Integer> callBack) {
        if (null == avVideoCtrl) {
            ILiveLog.ke(TAG, "disableCamera->failed", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            return;
        }
        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveLog.kw(TAG, "disableCamera", new ILiveLog.LogExts().put("operateTime", uOperateTime).put("curTime", uCurTime).put("timeout", uTimeOut));
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        } else {
            avVideoCtrl.enableCamera(curCameraId, false, new AVEnableCameraCallBack(curCameraId, callBack));
            uOperateTime = uCurTime;
        }
    }

    @Override
    public void switchCamera(int cameraId, ILiveCallBack<Integer> callBack) {
        if (null == avVideoCtrl) {
            ILiveLog.ke(TAG, "switchCamera->failed", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no VideoCtrl found");
            return;
        }
        long uCurTime = ILiveFunc.getCurrentSec();
        if (uCurTime > uOperateTime && uCurTime < (uOperateTime + uTimeOut)) { // 操作进行中
            ILiveLog.kw(TAG, "switchCamera", new ILiveLog.LogExts().put("operateTime", uOperateTime).put("curTime", uCurTime).put("timeout", uTimeOut));
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_BUSY_HERE, "last operate not completed");
        } else {
            avVideoCtrl.switchCamera(cameraId, new AVSwitchCameraCallBack(cameraId, callBack));
            uOperateTime = uCurTime;
        }
    }

    @Override
    public int getActiveCameraId() {
        return curCameraId;
    }

    @Override
    public void setCameraPreViewCallBack(CameraPreviewCallBack callBack) {
        mCameraPreviewCallBack.setCallBack(callBack);
    }

    @Override
    public boolean isEnableBeauty() {
        if (null == avVideoCtrl) {
            return false;
        }
        return AVVideoCtrl.isEnableBeauty();
    }

    @Override
    public void enableBeauty(float value) {
        if (null == avVideoCtrl) {
            return;
        }
        avVideoCtrl.inputBeautyParam(value);
    }

    @Override
    public void enableWhite(float value) {
        if (null == avVideoCtrl) {
            return;
        }
        avVideoCtrl.inputWhiteningParam(value);
    }

    @Override
    public void setUpRotation(int rotation) {
        if (null == avVideoCtrl) {
            return;
        }
        avVideoCtrl.setRotation(rotation);
    }

    @Override
    public Object getVideoObj() {
        return avVideoCtrl;
    }

    private void updateCameraId(int cameraId){
        ILiveLog.ki(TAG, "updateCameraId", new ILiveLog.LogExts().put("cameraId", cameraId));
        curCameraId = cameraId;
    }
}
