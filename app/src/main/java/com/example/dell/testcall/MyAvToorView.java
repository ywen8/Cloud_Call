package com.example.dell.testcall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;

import com.tencent.av.opengl.GraphicRendererMgr;
import com.tencent.av.opengl.ui.GLRootView;
import com.tencent.av.opengl.ui.GLView;
import com.tencent.av.opengl.utils.Utils;
import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.view.AVVideoGroup;
import com.tencent.ilivesdk.view.AVVideoView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyAvToorView  extends GLRootView {
    private static String TAG = "ILVB-MyAvToorView";

    /**
     * 左侧对齐
     */
    public final static int LAYOUT_GRAVITY_LEFT = 0;
    /**
     * 上侧对齐
     */
    public final static int LAYOUT_GRAVITY_TOP = 1;
    /**
     * 右侧对齐
     */
    public final static int LAYOUT_GRAVITY_RIGHT = 2;
    /**
     * 下侧对齐
     */
    public final static int LAYOUT_GRAVITY_BOTTOM = 3;

    private class BindInfo {
        String id = null;                               // 绑定的id
        int type = AVView.VIDEO_SRC_TYPE_CAMERA;        // 视频类型
        boolean bFlush = true;                          // 无画面时清屏(不保留最后一帧)
        boolean bWeak = true;                           // 是否强绑定(用户绑定为强绑定，自动分配为弱绑定)
    }

    private GraphicRendererMgr mGraphicRenderMgr = null;
    private AVVideoView[] mVideoArr = new AVVideoView[ILiveConstants.MAX_AV_VIDEO_NUM];
    //private String[] mIdMap = new String[ILiveConstants.MAX_AV_VIDEO_NUM];
    private MyAvToorView.BindInfo[] mBindMap = new MyAvToorView.BindInfo[ILiveConstants.MAX_AV_VIDEO_NUM];
    private AVVideoGroup mVideoGroup;
    private boolean mAutoOrientation = true;       // 自动旋转功能
    private MyAvToorView.VideoOrientationEventListener mOrientationEventListener;
    private boolean mInitLandScape = false; // 初始化是否为横屏
    private boolean mRenderMySelf = true;   // 是否渲染自己

    // 敏感度
    private int iRoleDt = 50;
    private boolean isFrontCamera = true;   // 是否前置摄像头
    private int mRotationAngle = 0;

    // 小屏布局
    private int mGravity = LAYOUT_GRAVITY_LEFT;
    private int mMarginX = 50;
    private int mMarginY = 50;
    private int mPadding = 30;
    private int mWidth = 0;         // 小屏宽度(0为屏幕的1/4)
    private int mHeight = 0;        // 小屏高度(0为屏幕的1/4)

    // 旋转纠正
    private int localRotationFix = 0;   // 本地视频角度纠正
    private int remoteRotationFix = 0;  // 远程视频角度纠正

    public interface onSurfaceCreatedListener {
        void onSurfaceCreated();
    }

    /**
     * 小屏初始化回调
     */
    public interface onSubViewCreatedListener {
        void onSubViewCreated();
    }

    private MyAvToorView.onSurfaceCreatedListener mSCUserListner = null;     // SurfaceView创建回调
    private MyAvToorView.onSubViewCreatedListener mSubCreateListner;

    class VideoOrientationEventListener extends OrientationEventListener {
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
            for (int i = 0; i < mVideoArr.length; i++) {
                if (null != mVideoArr[i]) {
                    mVideoArr[i].setRotation(orientation);
                    mVideoArr[i].setNeedUpdateAngle(bNeedUpdate);
                }
            }
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

    private void init() {
        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            mBindMap[i] = new MyAvToorView.BindInfo();
        }

        mInitLandScape = ILiveFunc.isLandScape(getContext());

        ILiveLog.di(TAG, "init", new ILiveLog.LogExts().put("table", ILiveFunc.isTableDevice(getContext()))
                .put("landscape", mInitLandScape));
    }

    public MyAvToorView(Context context) {
        super(context);
        init();
    }

    public MyAvToorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        ILiveLog.ki(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        ILiveLog.ki(TAG, "onResume");
    }

    /**
     * 设置SurfaceView创建成功回调(用户在该回调触发后手动打开摄像头)
     *
     * @param listener
     */
    public void setSurfaceCreateListener(MyAvToorView.onSurfaceCreatedListener listener) {
        mSCUserListner = listener;
    }

    public MyAvToorView.onSurfaceCreatedListener getmSCUserListner() {
        return mSCUserListner;
    }

    /**
     * 查找用户对应的view的索引
     *
     * @param id   用户id
     * @param type 视频类型
     * @return
     * @see AVView#VIDEO_SRC_TYPE_CAMERA
     * @see AVView#VIDEO_SRC_TYPE_SCREEN
     */
    public int findUserViewIndex(String id, int type) {
        if (null == id) {
            return ILiveConstants.INVALID_INTETER_VALUE;
        }

        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null != mBindMap[i]
                    && null != mBindMap[i].id
                    && mBindMap[i].type == type
                    && mBindMap[i].id.equals(id)) {
                return i;
            }
        }

        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null != mVideoArr[i]
                    && mVideoArr[i].getVideoSrcType() == type
                    && id.equals(mVideoArr[i].getIdentifier())) {
                return i;
            }
        }

        return ILiveConstants.INVALID_INTETER_VALUE;
    }

    /**
     * 查找空闲索引
     *
     * @return
     */
    public int findValidViewIndex() {
        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null == mBindMap[i].id) {
                return i;
            }
        }
        return ILiveConstants.INVALID_INTETER_VALUE;
    }

    // 获取当前旋转敏感度
    public int getRoleDt() {
        return iRoleDt;
    }

    // 设置当前旋转敏感度(默认为20)
    public void setRoleDt(int iRoleDt) {
        this.iRoleDt = iRoleDt;
    }

    // 获取AVVideoGroup
    public AVVideoGroup getVideoGroup() {
        if (null == mVideoGroup) {
            mVideoGroup = new AVVideoGroup();
        }
        return mVideoGroup;
    }

    // 初始化View
    public void initView() {
        mGraphicRenderMgr = GraphicRendererMgr.getInstance();

        mOrientationEventListener = new MyAvToorView.VideoOrientationEventListener(getContext().getApplicationContext(), SensorManager.SENSOR_DELAY_UI);
        if (mAutoOrientation) {
            ILiveLog.ki(TAG, "initView", new ILiveLog.LogExts().put("landscape", ILiveFunc.isLandScape(getContext())));
            mOrientationEventListener.enable();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);
        ILiveLog.ki(TAG, "onSurfaceCreated->enter");
        if (mAutoOrientation && null != mOrientationEventListener)
            mOrientationEventListener.enable();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
        ILiveLog.ki(TAG, "surfaceDestroyed->enter");
        if (null != mOrientationEventListener)
            mOrientationEventListener.disable();
    }

    // 设置本地旋转角度(可用于模拟本地旋转)
    public void setDeviceRotation(int rotation){
        ILiveLog.ki(TAG, "setDeviceRotation", new ILiveLog.LogExts().put("rotation", rotation));
        if (null != mOrientationEventListener) {
            mOrientationEventListener.processOrientation(rotation, true);
        }
    }

    // 通知小屏初始化完毕
    public void notifySubViewCreated() {
        if (null != mSubCreateListner) {
            mSubCreateListner.onSubViewCreated();
        }
    }

    // 设置小窗口位置
    private void resetVideoViewPos(){
        int topPos = 0, topLeft = 0, subWidth = 0, subHeight = 0;
        subWidth = (0==mWidth) ? getWidth()/4 : mWidth;         // 小屏默认宽度为主屏的1/4
        subHeight = (0==mHeight) ? getHeight()/4 : mHeight;     // 小屏默认高度为主屏的1/4

        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (0 == i) {    // 主屏占满控件
                mVideoArr[i].setPosLeft(0);
                mVideoArr[i].setPosTop(0);
                mVideoArr[i].setPosWidth(getWidth());
                mVideoArr[i].setPosHeight(getHeight());
            } else {
                switch (mGravity) {
                    case LAYOUT_GRAVITY_TOP:    // 小屏在上侧
                        mVideoArr[i].setPosLeft(mMarginX + topLeft);
                        mVideoArr[i].setPosTop(mMarginY);
                        topLeft += (subWidth + mPadding);
                        break;
                    case LAYOUT_GRAVITY_RIGHT:  // 小屏在右侧
                        mVideoArr[i].setPosLeft(getWidth() - subWidth - mMarginX);
                        mVideoArr[i].setPosTop(mMarginY + topPos);
                        topPos += (subHeight + mPadding);
                        break;
                    case LAYOUT_GRAVITY_BOTTOM: // 小屏在底侧
                        mVideoArr[i].setPosLeft(mMarginX + topLeft);
                        mVideoArr[i].setPosTop(getHeight() - subHeight - mMarginY);
                        topLeft += (subWidth + mPadding);
                        break;
                    case LAYOUT_GRAVITY_LEFT:   // 小屏在左侧
                    default:
                        mVideoArr[i].setPosLeft(mMarginX);
                        mVideoArr[i].setPosTop(mMarginY + topPos);
                        topPos += (subHeight + mPadding);
                        break;
                }
                mVideoArr[i].setPosWidth(subWidth);
                mVideoArr[i].setPosHeight(subHeight);
            }
            if (i < 3) {    // 只打印前三个view
                ILiveLog.di(TAG, "resetVideoViewPos", new ILiveLog.LogExts().put("index", i)
                        .put("left", mVideoArr[i].getPosLeft())
                        .put("top", mVideoArr[i].getPosTop())
                        .put("width", mVideoArr[i].getPosWidth())
                        .put("height", mVideoArr[i].getPosHeight()));
            }
        }
    }

    public void layoutVideo(boolean virtical) {//
        int width = getWidth();
        int height = getHeight();

        ILiveLog.di(TAG, "layoutVideo", new ILiveLog.LogExts().put("width", width)
                .put("height", height));

        // 重置小视频位置
        resetVideoViewPos();
        notifySubViewCreated();

        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null != mVideoArr[i]) {
                mVideoArr[i].autoLayout();
                mVideoArr[i].setBackgroundColor(Color.BLACK);
            }
        }


        ILiveSDK.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                mOrientationEventListener.resetLastRation();
                mOrientationEventListener.onOrientationChanged(mRotationAngle);
                invalidate();
            }
        }, 0);
    }

    // 初始化用户视频
    public AVVideoView[] initVideoGroup() {
        ILiveLog.di(TAG, "initVideoGroup", new ILiveLog.LogExts().put("width", getWidth())
                .put("height", getHeight()));

        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            mVideoArr[i] = new AVVideoView(getContext(), mGraphicRenderMgr);
            mVideoArr[i].setLocalRotationFix(localRotationFix);
            mVideoArr[i].setRemoteRotationFix(remoteRotationFix);
            mVideoArr[i].setVisibility(GLView.INVISIBLE);
        }

        resetVideoViewPos();

        return mVideoArr;
    }

    /**
     * 渲染视频
     *
     * @param bHasVideo    是否已有视频
     * @param id           要渲染的用户id
     * @param videoSrcType 渲染的视频类型
     * @param bAutoRender  是否自动渲染(自动查找空闲view)
     * @return
     * @see AVView#VIDEO_SRC_TYPE_CAMERA
     */
    public boolean renderVideoView(boolean bHasVideo, String id, int videoSrcType, boolean bAutoRender) {
        if (Utils.getGLVersion(getContext()) == 1) {
            return false;
        }
        int index = findUserViewIndex(id, videoSrcType);
        if (id.equals(ILiveLoginManager.getInstance().getMyUserId())) { //直播设置ID
            mGraphicRenderMgr.setSelfId(ILiveLoginManager.getInstance().getMyUserId() + "_" + AVView.VIDEO_SRC_TYPE_CAMERA);
            if (!mRenderMySelf && ILiveConstants.INVALID_INTETER_VALUE == index){
                ILiveLog.ki(TAG, "renderVideoView->do not auto render myself");
                return false;
            }
        }

//        ILiveLog.v(TAG, ILiveConstants.LOG_KEY_PR+"|ILVB-MyAvToorView|renderVideoView->enter id:" + id + "/" + ILiveLoginManager.getInstance().getMyUserId() + "|index:" + index);
        if (ILiveConstants.INVALID_INTETER_VALUE == index) {
            if (bAutoRender) {
                index = findValidViewIndex();   // 获取空闲view
            }
            if (ILiveConstants.INVALID_INTETER_VALUE == index) {
                ILiveLog.kw(TAG, "renderVideoView->fail", new ILiveLog.LogExts().put("id", id)
                        .put("index", index));
                return false;
            }
        }
        AVVideoView videoView = mVideoArr[index];
        if (null == videoView) {
            ILiveLog.kw(TAG, "renderVideoView->noSub");
            return false;
        }
        if (!id.equals(mBindMap[index].id)) //避免重复上抛首帧回调
            videoView.resetCache();
        ILiveLog.ki(TAG, "renderVideoView", new ILiveLog.LogExts().put("index", index)
                .put("id", id)
                .put("type", videoSrcType)
                .put("left", videoView.getPosLeft())
                .put("top", videoView.getPosTop())
                .put("width", videoView.getPosWidth())
                .put("height", videoView.getPosHeight()));
        mBindMap[index].id = id; // 绑定用户
        mBindMap[index].type = videoSrcType;    // 配置视频类型
        if (bHasVideo) {// 打开摄像头
            videoView.setRender(id, videoSrcType);
            videoView.setIsPC(false);
            videoView.enableLoading(false);
            videoView.setVisibility(GLView.VISIBLE);
            videoView.setRendering(true);
        } else {// 关闭摄像头
            videoView.setVisibility(GLView.INVISIBLE);
            videoView.setNeedRenderVideo(true);
            videoView.enableLoading(false);
            videoView.setIsPC(false);
            videoView.clearRender();

            layoutVideo(false);
        }

        return true;
    }

    /**
     * 交换两路视频
     *
     * @param src 源view索引
     * @param dst 目标view索引
     * @return
     */
    public int swapVideoView(int src, int dst) {
        if (src >= ILiveConstants.MAX_AV_VIDEO_NUM || dst >= ILiveConstants.MAX_AV_VIDEO_NUM) {
            return ILiveConstants.ERR_NOT_FOUND;
        }
        AVVideoView srcView = mVideoArr[src];
        AVVideoView dstView = mVideoArr[dst];
        if (null == srcView || null == dstView) {
            ILiveLog.kw(TAG, "swapVideoView->not-exist", new ILiveLog.LogExts().put("src", src).put("dst", dst));
            return ILiveConstants.ERR_NOT_FOUND;
        }

        String tmpId = srcView.getIdentifier();
        int tmpSrcType = srcView.getVideoSrcType();
        boolean tmpIsPC = srcView.isPC();
        boolean tmpIsMirror = srcView.isMirror();
        boolean tmpIsLoading = srcView.isLoading();
        boolean tmpIsRendering = srcView.isRendering();
        int tmpVisible = srcView.getVisibility();

        srcView.setIsPC(dstView.isPC());
        srcView.setMirror(dstView.isMirror());
        srcView.enableLoading(dstView.isLoading());
        srcView.setRendering(dstView.isRendering());
        srcView.setRender(dstView.getIdentifier(), dstView.getVideoSrcType());
        srcView.setVisibility(dstView.getVisibility());

        dstView.setIsPC(tmpIsPC);
        dstView.setMirror(tmpIsMirror);
        dstView.enableLoading(tmpIsLoading);
        dstView.setRendering(tmpIsRendering);
        dstView.setRender(tmpId, tmpSrcType);
        dstView.setVisibility(tmpVisible);

        // 交换绑定关系
        MyAvToorView.BindInfo tmpBindInfo = mBindMap[src];
        mBindMap[src] = mBindMap[dst];
        mBindMap[dst] = tmpBindInfo;
        ILiveLog.di(TAG, "swapVideoView", new ILiveLog.LogExts().put("srcId", srcView.getIdentifier())
                .put("srcType", srcView.getVideoSrcType())
                .put("dstId", srcView.getIdentifier())
                .put("dstType", srcView.getVideoSrcType()));

        return ILiveConstants.NO_ERR;
    }

    /**
     * 配置是否渲染自己(在渲染前调用)
     * @param enable
     */
    public void renderMySelf(boolean enable){
        mRenderMySelf = enable;
    }

    /**
     * 通过索引获取AVVideoView
     *
     * @param index 索引
     * @return view
     */
    public AVVideoView getViewByIndex(int index) {
        if (index >= ILiveConstants.MAX_AV_VIDEO_NUM) {
            return null;
        }

        return mVideoArr[index];
    }

    /**
     * 关闭用户视频
     *
     * @param id      用户id
     * @param type    视频类型
     * @param bRemove 是否移除该路视频(索引在后面的视频会向前顺移)
     */
    public void closeUserView(String id, int type, boolean bRemove) {
        int index = findUserViewIndex(id, type);
        if (ILiveConstants.INVALID_INTETER_VALUE == index) {
            ILiveLog.kw(TAG, "closeUserView->not-exist", new ILiveLog.LogExts().put("id", id).put("type", type));
            return;
        }

        AVVideoView avVideoView = mVideoArr[index];
        if (mBindMap[index].bFlush) {
            avVideoView.flush();
            avVideoView.clearRender();
            avVideoView.setRendering(false);
        }else{  // 保留最后一帧
            avVideoView.setRendering(false);
        }

        ILiveLog.ki(TAG, "closeUserView", new ILiveLog.LogExts().put("id", id)
                .put("type", type)
                .put("flush", mBindMap[index].bFlush)
                .put("remove", bRemove));
        if (bRemove && mBindMap[index].bWeak) {
            int curIdx = index;
            avVideoView.setVisibility(GLView.INVISIBLE);        // 设置为不可见
            mBindMap[index].id = null;  //解除当前绑定
            mBindMap[index].type = AVView.VIDEO_SRC_TYPE_CAMERA;
            // 前移弱绑定的view
            for (int i = index + 1; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
                if (!mVideoArr[i].isRendering() || !mBindMap[i].bWeak) {    // 跳过正在渲染的及强绑定的
                    continue;
                }
                swapVideoView(i, curIdx);
                curIdx = i;
            }
        }
        ILiveQualityData.removeLive(id);
    }

    /**
     * 清空用户View并重置绑定关系
     */
    public void clearUserView() {
        ILiveLog.ki(TAG, "clearUserView->enter");
        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null == mVideoArr[i])
                continue;
            mVideoArr[i].flush();
            mVideoArr[i].clearRender();
            mVideoArr[i].setRendering(false);
            mVideoArr[i].setVisibility(View.INVISIBLE);
            mBindMap[i].id = null;   // 重置绑定关系
            mBindMap[i].type = AVView.VIDEO_SRC_TYPE_CAMERA;
            mBindMap[i].bWeak = true;
        }
    }


    public void onDestory() {
        if (null != mOrientationEventListener && mAutoOrientation) {
            mOrientationEventListener.disable();
        }
        if (null != mVideoGroup) {
            mVideoGroup.onDestroy();
        }
        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (null != mVideoArr[i]) {
                mVideoArr[i].flush();
                mVideoArr[i].clearRender();
            }
        }
    }

    /**
     * 绑定view索引与用户id(id为null时解绑)
     *
     * @param index view索引
     * @param type  视频类型
     * @param id    用户id
     * @return 成功返回
     * @see AVView#VIDEO_SRC_TYPE_CAMERA
     * @see AVView#VIDEO_SRC_TYPE_SCREEN
     */
    public int bindIdAndView(int index, int type, String id) {
        return bindIdAndView(index, type, id, true);
    }

    /**
     * 绑定view索引与用户id(id为null时解绑)
     *
     * @param index view索引
     * @param type  视频类型
     * @param id    用户id
     * @param bFlush 是否无画面时清屏(不保留最后一帧)
     * @return 成功返回
     * @see AVView#VIDEO_SRC_TYPE_CAMERA
     * @see AVView#VIDEO_SRC_TYPE_SCREEN
     */
    public int bindIdAndView(int index, int type, String id, boolean bFlush) {
        ILiveLog.ki(TAG, "bindIdAndView", new ILiveLog.LogExts().put("index", index)
                .put("type", type)
                .put("flush", bFlush)
                .put("id", id));
        if (index >= ILiveConstants.MAX_AV_VIDEO_NUM) {
            return ILiveConstants.ERR_NOT_FOUND;
        }

        mBindMap[index].id = id;
        mBindMap[index].bFlush = bFlush;
        mBindMap[index].type = type;
        mBindMap[index].bWeak = (null == id);
        return ILiveConstants.NO_ERR;
    }

    /**
     * 通过用户id查找view
     *
     * @param id   用户id
     * @param type 视频类型
     * @return
     * @see AVView#VIDEO_SRC_TYPE_CAMERA
     * @see AVView#VIDEO_SRC_TYPE_SCREEN
     */
    public AVVideoView getUserAvVideoView(String id, int type) {
        int index = findUserViewIndex(id, type);
        if (ILiveConstants.INVALID_INTETER_VALUE == index) {
            return null;
        }
        return mVideoArr[index];
    }

    /**
     * 设置小屏初始位置
     *
     * @param gravity 小屏位置
     * @see #LAYOUT_GRAVITY_LEFT
     * @see #LAYOUT_GRAVITY_RIGHT
     * @see #LAYOUT_GRAVITY_TOP
     * @see #LAYOUT_GRAVITY_BOTTOM
     */
    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    /**
     * 设置小屏初始x轴边距
     *
     * @param margin 边距
     */
    public void setSubMarginX(int margin) {
        this.mMarginX = margin;
    }

    /**
     * 设置小屏初始y轴边距
     *
     * @param margin 边距
     */
    public void setSubMarginY(int margin) {
        this.mMarginY = margin;
    }

    /**
     * 设置小屏初始间距
     *
     * @param padding 间距
     */
    public void setSubPadding(int padding) {
        this.mPadding = padding;
    }

    /**
     * 设置小屏初始宽度(默认为0表示为主屏的1/4)
     *
     * @param width 宽度
     */
    public void setSubWidth(int width) {
        this.mWidth = width;
    }

    /**
     * 设置小屏初始高度(默认为0表示为主屏的1/4)
     *
     * @param height 高度
     */
    public void setSubHeight(int height) {
        this.mHeight = height;
    }

    /**
     * 设置小屏初始化回调(可用于设置小屏初始位置等)
     *
     * @param listner 回调
     */
    public void setSubCreatedListener(MyAvToorView.onSubViewCreatedListener listner) {
        this.mSubCreateListner = listner;
    }

    /**
     * 设置是否开启自动旋转(关闭后SDK内部旋转机制将失效，用户需要自行适配旋转方案，慎用!!!)
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
     * 设置本地角度纠正
     */
    public void setLocalRotationFix(int rotation) {
        ILiveLog.ki(TAG, "setLocalRotationFix", new ILiveLog.LogExts().put("rotation", rotation));
        localRotationFix = rotation;
        if (null != mVideoArr[0]) {
            for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
                mVideoArr[i].setLocalRotationFix(localRotationFix);
            }
        }
    }

    /**
     * 设置远程角度纠正
     */
    public void setRemoteRotationFix(int rotation) {
        ILiveLog.ki(TAG, "setRemoteRotationFix", new ILiveLog.LogExts().put("rotation", rotation)
                .put("RotationAngle", mRotationAngle));
        remoteRotationFix = rotation;
        if (null != ILiveSDK.getInstance().getAvVideoCtrl()) {
            ILiveSDK.getInstance().getAvVideoCtrl().setRotation(ILiveFunc.offsetRotation(mRotationAngle, remoteRotationFix));
        }
        if (null != mVideoArr[0]) {
            for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
                mVideoArr[i].setRemoteRotationFix(remoteRotationFix);
            }
        }
    }

    public int getRenderFuncPtr() {
        return mGraphicRenderMgr.getRecvDecoderFrameFunctionptr();
    }

    // 设置是否前置摄像头
    public void setFrontCamera(boolean frontCamera) {
        if (isFrontCamera != frontCamera) {
            isFrontCamera = frontCamera;
            if (null != mVideoArr[0]) {
                for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
                    mVideoArr[i].setFrontCamera(isFrontCamera);
                }
            }
        }
        if (null != mOrientationEventListener) {
            ILiveLog.ki(TAG, "setFrontCamera", new ILiveLog.LogExts()
                    .put("isFrontCamera", isFrontCamera)
                    .put("lastAngle", mOrientationEventListener.getLastAngle()));
            mOrientationEventListener.processOrientation(mOrientationEventListener.getLastAngle(), true);
        }
    }

    /**
     * 设置背景图片(请在设置MyAvToorView后设置)
     */
    public void setBackground(Bitmap bitmap) {
        mVideoGroup.setBackground(bitmap);
    }

    /**
     * 设置背景图片(请在设置MyAvToorView后设置)
     */
    public void setBackground(int res) {
        mVideoGroup.setBackground(res);
    }

    /**
     * 设置背景颜色(请在设置MyAvToorView后设置)
     */
    @Override
    public void setBackgroundColor(int color) {
        mVideoGroup.setBackgroundColor(color);
    }

    public void debugViews() {
        for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++) {
            if (mVideoArr[i].isRendering()) {
                ILiveLog.dd(TAG, "debugViews", new ILiveLog.LogExts().put("index", i)
                        .put("id", mVideoArr[i].getIdentifier())
                        .put("type", mVideoArr[i].getVideoSrcType())
                        .put("weak", mBindMap[i].bWeak));
            }
        }
    }
}
