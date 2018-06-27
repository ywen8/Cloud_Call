package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;

/**
 * 视频模块适配器
 */
public interface VideoEngine {
    interface CameraPreviewCallBack{
        void onCameraPreview(int cameraId);
    }

    /** 初始化音频模块 */
    void init(ContextEngine context, ILiveCallBack callBack);
    /** 设置超时时间 */
    void setTimeOut(int timeOut);

    /** 获取摄像头数量 */
    int getCameraNum();
    /** 获取摄像头对象 */
    Object getCamera();
    /** 获取摄像头参数对象 */
    Object getCameraPara();

    /** 打开摄像头 */
    void enableCamera(int cameraId, ILiveCallBack<Integer> callBack);
    /** 关闭当前摄像头 */
    void disableCamera(ILiveCallBack<Integer> callBack);
    /** 切换摄像头 */
    void switchCamera(int cameraId, ILiveCallBack<Integer> callBack);
    /** 获取当前打开的摄像头 */
    int getActiveCameraId();
    /** 设置摄像头预览回调 */
    void setCameraPreViewCallBack(CameraPreviewCallBack callBack);

    /** 是否支持美颜 */
    boolean isEnableBeauty();
    /** 设置美颜 */
    void enableBeauty(float value);
    /** 设置美白 */
    void enableWhite(float value);

    /** 设置上行数据角度 */
    void setUpRotation(int rotation);

    /** 获取底层视频管理类 */
    Object getVideoObj();
}
