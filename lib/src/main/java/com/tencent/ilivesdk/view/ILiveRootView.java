package com.tencent.ilivesdk.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.OrientationEventListener;
import android.view.View;

import com.tencent.av.opengl.GraphicRendererMgr;
import com.tencent.av.opengl.ui.GLRootView;
import com.tencent.av.opengl.ui.GLView;
import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;

/**
 * 视频显示控件[分开显示]
 */
public class ILiveRootView extends GLRootView {
    private final static String TAG = "ILVB-ILiveRootView";

    protected INGroupView groupView;
    protected INVideoView videoView;
    private boolean isRendering = false;
    private boolean bInited = false;
    private boolean bAttached = false;

    private boolean mAutoOrientation = true;       // 自动旋转功能
    protected VideoOrientationEventListener mOrientationEventListener;
    private boolean mInitLandScape = false; // 初始化是否为横屏

    // 敏感度
    private int iRoleDt = 20;
    private int iLastRotate = 0;    // 缓存上次角度，用于判断角度是否发生变更
    private boolean isFrontCamera = true;   // 是否前置摄像头
    private int mRotationAngle = 0;

    // 旋转纠正
    private int localRotationFix = 0;   // 本地视频角度纠正
    private int remoteRotationFix = 0;  // 远程视频角度纠正

    public class VideoOrientationEventListener extends OrientationEventListener {
        boolean mIsTablet = false;
        int mLastOrientation = -25;
        int mLastAngle = 0;

        public VideoOrientationEventListener(Context context, int rate) {
            super(context, rate);
            mIsTablet = ILiveFunc.isTableDevice(context);
        }

        public void resetLastRation(){
            mLastOrientation = -25;
        }

        public int getLastAngle(){
            return mLastAngle;
        }

        public void processOrientation(int orientation, boolean bNeedUpdate){
            // 横屏时后置摄像头角度纠正
            if (ILiveFunc.isLandScape(getContext()) && !isFrontCamera) {
                ILiveLog.ki(TAG, "processOrientation->landscape&backCamera", new ILiveLog.LogExts()
                        .put("initLandscape", mInitLandScape).put("orientation", orientation));
                if (mInitLandScape != ILiveFunc.isLandScape(getContext()) && 90 == orientation){     // 后置自动旋转纠正
                    orientation = ILiveFunc.offsetRotation(orientation, -90);
                }else {
                    orientation = ILiveFunc.offsetRotation(orientation, 90);
                }
            }

            if (null != ILiveSDK.getInstance().getAvVideoCtrl()) {
                int finalUpAngle = ILiveFunc.offsetRotation(orientation, remoteRotationFix);
                ILiveLog.ki(TAG, "processOrientation->setRotation", new ILiveLog.LogExts()
                        .put("isFrontCamera", isFrontCamera)
                        .put("remoteRotationFix", remoteRotationFix)
                        .put("Landscape", ILiveFunc.isLandScape(getContext()))
                        .put("orientation", orientation)
                        .put("finalUpAngle", finalUpAngle));
                ILiveSDK.getInstance().getAvVideoCtrl().setRotation(finalUpAngle);
            }

            videoView.setRotation(orientation);
            videoView.setNeedUpdateAngle(bNeedUpdate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                mLastOrientation = orientation;
                return;
            }

            if (mLastOrientation < 0) {
                mLastOrientation = 0;
            }

            if (((orientation - mLastOrientation) < iRoleDt)
                    && ((orientation - mLastOrientation) > -iRoleDt)) {
                return;
            }

            mLastOrientation = orientation;

            if (orientation > 314 || orientation < 45) {
                mRotationAngle = 0;
            } else if (orientation > 44 && orientation < 135) {
                mRotationAngle = 90;
            } else if (orientation > 134 && orientation < 225) {
                mRotationAngle = 180;
            } else {
                mRotationAngle = 270;
            }

            mLastAngle = mRotationAngle;
            processOrientation(mRotationAngle, false);
        }

    }

    public ILiveRootView(Context context) {
        super(context);
        init();
    }

    public ILiveRootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        groupView = new INGroupView();
        videoView = new INVideoView(getContext(), GraphicRendererMgr.getInstance());
        mInitLandScape = ILiveFunc.isLandScape(getContext());
        ILiveLog.di(TAG, "init", new ILiveLog.LogExts().put("table", ILiveFunc.isTableDevice(getContext()))
                .put("landscape", ILiveFunc.isLandScape(getContext())));

        if (mAutoOrientation) {
            mOrientationEventListener = new VideoOrientationEventListener(getContext().getApplicationContext(), SensorManager.SENSOR_DELAY_UI);
            mOrientationEventListener.enable();
        }
    }


    // 设置本地旋转角度(可用于模拟本地旋转)
    public void setDeviceRotation(int rotation){
        ILiveLog.ki(TAG, "setDeviceRotation", new ILiveLog.LogExts().put("rotation", rotation));
        if (null != mOrientationEventListener) {
            mOrientationEventListener.processOrientation(rotation, true);
        }
    }

    private void initSubView() {
        videoView.layout(0, 0, getWidth(), getHeight());
        if (!bAttached) {
            groupView.addView(videoView);
            bAttached = true;
        }
    }

    public void setOnTouchListenerEvent(GLVideoView.OnTouchListener listener) {
        groupView.setOnTouchListenerEvent(listener);
    }

    /**
     * 设置是否开启自动旋转
     *
     * @param bEnable
     */
    @Deprecated
    public void setAutoOrientation(boolean bEnable) {
        ILiveLog.ki(TAG, "setAutoOrientation", new ILiveLog.LogExts().put("enable", bEnable));
        // 判断是否需要更新
        if (null != mOrientationEventListener && mAutoOrientation != bEnable) {
            if (bEnable)
                mOrientationEventListener.enable();
            else
                mOrientationEventListener.disable();
        }
        mAutoOrientation = bEnable;
    }

    /**
     * 初始化View(设置到房间时由房间内部调用)
     */
    public void initViews() {
        ILiveLog.di(TAG, "initViews->enter");
        if (!bInited) {
            if (0 == getWidth()) {
                addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        initSubView();
                    }
                });
            } else {
                initSubView();
            }

            setContentPane(groupView);
            bInited = true;
        }
    }

    /**
     * 当前是否在渲染
     *
     * @return
     */
    public boolean isRendering() {
        return isRendering;
    }

    public void render(String id, int srcType) {
        ILiveLog.ki(TAG, "render", new ILiveLog.LogExts().put("id", id)
                .put("type", srcType));
        if (id.equals(ILiveLoginManager.getInstance().getMyUserId())) { //直播设置ID
            GraphicRendererMgr.getInstance().setSelfId(ILiveLoginManager.getInstance().getMyUserId() + "_" + AVView.VIDEO_SRC_TYPE_CAMERA);
        }
        isRendering = true;
        videoView.setRender(id, srcType);
        videoView.setIsPC(false);
        videoView.enableLoading(false);
        videoView.setVisibility(GLView.VISIBLE);
    }

    /**
     * 关闭当前渲染
     */
    public void closeVideo() {
        ILiveLog.ki(TAG, "closeVideo", new ILiveLog.LogExts().put("id", videoView.getIdentifier()));
        isRendering = false;

        videoView.setVisibility(GLView.INVISIBLE);
        videoView.setNeedRenderVideo(true);
        videoView.enableLoading(false);
        videoView.setIsPC(false);
        videoView.clearRender();
    }

    public void onDestory() {
        if (null != mOrientationEventListener && mAutoOrientation) {
            mOrientationEventListener.disable();
        }
    }

    /**
     * 设置Gesture事件
     *
     * @param gestureListener
     */
    public void setGestureListener(GestureDetector.SimpleOnGestureListener gestureListener) {
        groupView.setGestureListener(getContext(), gestureListener);
    }

    /**
     * 设置视频回调
     *
     * @param listener
     */
    public void setVideoListener(VideoListener listener) {
        videoView.setVideoListener(listener);
    }

    /**
     * 获取当前渲染的用户id
     */
    public String getIdentifier() {
        return videoView.getIdentifier();
    }

    /**
     * 获取渲染数据类型
     */
    public int getVideoSrcType() {
        return videoView.getVideoSrcType();
    }

    /**
     * 获取视频回调
     */
    public VideoListener getVideoListener() {
        return videoView.getVideoListener();
    }

    /**
     * 设置是否旋转画面(与view长宽比一致)
     *
     * @param rotate
     */
    public void setRotate(boolean rotate) {
        videoView.setRotate(rotate);
    }

    /**
     * 设置方向一致(画面与view)的渲染模式
     *
     * @param mode 渲染模式
     */
    public void setSameDirectionRenderMode(BaseVideoView.BaseRenderMode mode) {
        videoView.setSameDirectionRenderMode(mode);
    }

    /**
     * 设置方向一致(画面与view)的渲染模式
     *
     * @param mode 渲染模式
     */
    public void setDiffDirectionRenderMode(BaseVideoView.BaseRenderMode mode) {
        videoView.setDiffDirectionRenderMode(mode);
    }

    /** 设置丢帧数量(避免闪屏) */
    public void setDropFrame(int iDropFrame) {
        videoView.setDropFrame(iDropFrame);
    }

    /**
     * 设置背景图片(请在设置AVRootView后设置)
     */
    public void setBackground(Bitmap bitmap) {
        groupView.setBackground(bitmap);
    }

    /**
     * 设置背景图片(请在设置AVRootView后设置)
     */
    public void setBackground(int res) {
        groupView.setBackground(res);
    }

    // 获取当前旋转敏感度
    public int getRoleDt() {
        return iRoleDt;
    }

    // 设置当前旋转敏感度(默认为20)
    public void setRoleDt(int iRoleDt) {
        this.iRoleDt = iRoleDt;
    }

    /**
     * 设置本地角度纠正
     */
    public void setLocalRotationFix(int rotation) {
        ILiveLog.ki(TAG, "setLocalRotationFix", new ILiveLog.LogExts().put("rotation", rotation));
        localRotationFix = rotation;
        videoView.setLocalRotationFix(localRotationFix);
    }

    /**
     * 设置远程角度纠正
     */
    public void setRemoteRotationFix(int rotation) {
        ILiveLog.ki(TAG, "setRemoteRotationFix", new ILiveLog.LogExts().put("rotation", rotation)
                .put("RotationAngle", mRotationAngle));
        remoteRotationFix = rotation;
        if (null != ILiveSDK.getInstance().getAvVideoCtrl() && videoView.isLocal()) {
            ILiveSDK.getInstance().getAvVideoCtrl().setRotation(ILiveFunc.offsetRotation(mRotationAngle, remoteRotationFix));
        }

        videoView.setRemoteRotationFix(remoteRotationFix);
    }

    // 设置是否前置摄像头
    public void setFrontCamera(boolean frontCamera) {
        if (isFrontCamera != frontCamera) {
            isFrontCamera = frontCamera;
            videoView.setFrontCamera(isFrontCamera);
        }
        if (null != mOrientationEventListener) {
            ILiveLog.ki(TAG, "setFrontCamera", new ILiveLog.LogExts()
                    .put("isFrontCamera", isFrontCamera)
                    .put("lastAngle", mOrientationEventListener.getLastAngle()));
            mOrientationEventListener.processOrientation(mOrientationEventListener.getLastAngle(), true);
        }
    }

    public BaseVideoView getVideoView() {
        return videoView;
    }
}
