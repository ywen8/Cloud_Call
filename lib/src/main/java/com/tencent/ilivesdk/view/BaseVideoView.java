package com.tencent.ilivesdk.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.GestureDetector;

import com.tencent.av.opengl.GraphicRendererMgr;
import com.tencent.av.opengl.glrenderer.GLCanvas;
import com.tencent.av.opengl.texture.YUVTexture;
import com.tencent.av.opengl.utils.Utils;
import com.tencent.av.utils.QLog;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;

/**
 * 视频渲染基础控件
 */
public class BaseVideoView extends GLVideoView {
    private static String TAG = "ILVB-BaseVideoView";

    /**
     * 渲染模式
     */
    public enum BaseRenderMode {
        /**
         * 全屏模式
         */
        SCALE_TO_FIT,
        /**
         * 黑边模式
         */
        BLACK_TO_FILL,
    }

    private boolean rotate = false;
    private BaseRenderMode sameDirectionRenderMode = BaseRenderMode.SCALE_TO_FIT;
    private BaseRenderMode diffDirectionRenderMode = BaseRenderMode.BLACK_TO_FILL;

    private boolean bRendering = false;     // 是否正在渲染
    private boolean bInitLandscape; //横屏
    private boolean isTablet = false;
    private int lastRotation = -1;

    private GestureDetector.SimpleOnGestureListener gestureListener;
    private VideoListener videoListener;
    private int localRotationFix = 0;
    private int remoteRotationFix = 0;

    private int lastDeviceAngle = -1;       // 缓存上次设备角度
    private int lastVideoAngle = -1;        // 缓存上次视频角度
    private float lastRatio = -1;           // 缓存上次视频宽高比
    private boolean bNeedUpdate = false;    // 是否需要直接更新角度
    private boolean bFrontCamera = true;    // 是否前置摄像头
    private int iWaitFrame = 0;             // 等待帧数
    private long timeLimit = 0;
    private int iDropFrame = 0;

    public BaseVideoView() {
    }

    public BaseVideoView(Context context, GraphicRendererMgr graphicRenderMgr) {

        super(context, graphicRenderMgr);
        isTablet = ILiveSDK.getInstance().isTabletDevice(context);
        bInitLandscape = ILiveFunc.isLandScape(context);
        // 补丁
        mImageWidth = 480;
        mImageHeight = 640;
        // 覆盖到达回调
        mYuvTexture.setGLRenderListener(new YUVTexture.GLRenderListener() {
            @Override
            public void onRenderFrame() {
                invalidate();
            }

            @Override
            public void onRenderReset() {
                flush();
                invalidate();
            }

            @Override
            public void onRenderFlush() {
                flush();
                invalidate();
            }

            @Override
            public void onRenderInfoNotify(int width, int height, int angle) {
                if (isFristFrame == false) {
                    ILiveLog.di(TAG, "onRenderInfoNotify", new ILiveLog.LogExts().put("width", width)
                            .put("height", height)
                            .put("angle", angle)
                            .put("id", getIdentifier())
                            .put("type", mVideoSrcType));
                    if (null != videoListener) {
                        videoListener.onFirstFrameRecved(width, height, angle, getIdentifier());
                    }
                    isFristFrame = true;
                }
                mImageWidth = width;
                mImageHeight = height;
                mImageAngle = angle;
                mYuvTexture.setTextureSize(width, height);
                // refresh();
                invalidate();
                if (mIdentifier != null) {
                    ILiveQualityData.addLive(mIdentifier, width, height);
                }
            }
        });
    }

    /**
     * 设置Gesture事件
     */
    public void setGestureListener(GestureDetector.SimpleOnGestureListener gestureListener) {
        this.gestureListener = gestureListener;
    }

    /**
     * 设置是否旋转画面(与view长宽比一致)
     */
    public void setRotate(boolean rotate) {
        this.rotate = rotate;
        lastRotation = -1;
    }

    /**
     * 设置方向一致(画面与view)的渲染模式
     *
     * @param sameDirectionRenderMode 渲染模式
     * @see BaseRenderMode
     */
    public void setSameDirectionRenderMode(BaseRenderMode sameDirectionRenderMode) {
        this.sameDirectionRenderMode = sameDirectionRenderMode;
        lastRotation = -1;
    }

    /**
     * 设置方向不一致(画面与view)的渲染模式
     *
     * @param diffDirectionRenderMode 渲染模式
     * @see BaseRenderMode
     */
    public void setDiffDirectionRenderMode(BaseRenderMode diffDirectionRenderMode) {
        this.diffDirectionRenderMode = diffDirectionRenderMode;
        lastRotation = -1;
    }

    public GestureDetector.SimpleOnGestureListener getGestureListener() {
        return gestureListener;
    }

    public boolean isRendering() {
        return bRendering;
    }

    public void setRendering(boolean bRendering) {
        this.bRendering = bRendering;
    }


    //  视频相关数据获取

    /**
     * 获取视频宽度
     */
    public int getImageWidth() {
        return mImageWidth;
    }

    /**
     * 获取视频高度
     */
    public int getImageHeight() {
        return mImageHeight;
    }

    /**
     * 获取视频旋转角度
     */
    public int getImageAngle() {
        return mImageAngle;
    }

    /**
     * 获取视频首帧是否到达
     */
    public boolean isFirstFrameRecved() {
        return isFristFrame;
    }

    /**
     * 设置丢帧(避免闪屏)
     */
    public void setDropFrame(int count){
        iDropFrame = count;
    }

    public void setNeedUpdateAngle(boolean bNeed){
        bNeedUpdate = bNeed;
    }

    public void setFrontCamera(boolean front){
        bFrontCamera = front;
    }

    public void setLocalRotationFix(int localRotationFix) {
        this.localRotationFix = localRotationFix;
        if (isLocal()) {
            lastRotation = -1;
        }
    }

    public void setRemoteRotationFix(int remoteRotationFix) {
        this.remoteRotationFix = remoteRotationFix;
    }

    @Override
    protected void render(GLCanvas canvas) {
        Rect p = getPaddings();
        renderBackground(canvas);
        if (null != mIdentifier && mYuvTexture != null && mYuvTexture.canRender() && mNeedRenderVideo) {
            boolean isLandscape = ILiveFunc.isLandScape(ILiveSDK.getInstance().getAppContext());
            enableLoading(false);
            int uiWidth = getWidth();
            int uiHeight = getHeight();
            int width = uiWidth - p.left - p.right;
            int height = uiHeight - p.top - p.bottom;

            float imgW = mYuvTexture.getImgWidth();
            float imgH = mYuvTexture.getImgHeight();
            float sRatio = imgW / imgH;
            int angle = mYuvTexture.getImgAngle();      // 视频角度
            int deviceAngle = mRotation;        // 设备角度
            if (isLocal()) {
                // 消除远程纠正对本地的影响
                int offAngle = ILiveFunc.getRotationAngle(remoteRotationFix);
                if (1 == (offAngle & 0x1))
                    angle = (mYuvTexture.getImgAngle() + 4 - ILiveFunc.getRotationAngle(remoteRotationFix)) % 4;
                else
                    angle = (mYuvTexture.getImgAngle() + ILiveFunc.getRotationAngle(remoteRotationFix)) % 4;

                if (-1 == lastVideoAngle || -1 == lastDeviceAngle || lastRatio < 0){ // 缓存角度
                    lastVideoAngle = angle;
                    lastDeviceAngle = deviceAngle;
                    lastRatio = sRatio;
                    ILiveLog.dd(TAG, "render->update", new ILiveLog.LogExts().put("videoAngle", angle).put("deviceAngle", deviceAngle));
                }else{
                    if (lastVideoAngle != angle || lastRatio != sRatio || bNeedUpdate){   // 视频角度或宽高比改变时，才更新设备角度
                        lastDeviceAngle = deviceAngle;
                        lastVideoAngle = angle;
                        bNeedUpdate = false;
                    }else{
                        iWaitFrame ++;
                        if (iWaitFrame > 3){
                            iWaitFrame = 0;
                            bNeedUpdate = true;
                        }
                    }
                    deviceAngle = lastDeviceAngle;
                }
            }

            int rotation = (angle + deviceAngle + 4) % 4;

            if (isLandscape && !bFrontCamera) {
                // 横屏后置摄像头纠正
                rotation = (rotation + 1) % 4;
            }

            if (lastRotation != rotation) {
                ILiveLog.dd(TAG, "render", new ILiveLog.LogExts().put("rotation", rotation)
                        .put("imageAngle", angle)
                        .put("deviceAngle", deviceAngle));
            }

            float tmpRatio = (float) width / (float) height;

            float x = p.left;
            float y = p.top;
            float w = width;
            float h = height;
            boolean switched = false;   // 是否旋转

            if (switchWH(w / h, sRatio, isLocal(), rotation, isLandscape)) {
                float tmp = x;
                x = y;
                y = tmp;
                tmp = w;
                w = h;
                h = tmp;
                tmp = width;
                width = height;
                height = (int) tmp;
                switched = true;
            }


            float targetW = w;
            float targetH = h;
            float tRatio = targetW / targetH;

            boolean hasBorder = false;      // 是否黑边显示

            if (hasBlackBorder(tRatio, sRatio)) {
                hasBorder = true;
                if (tRatio < sRatio) {
                    w = targetW;
                    h = w / sRatio;
                    if (h > targetH) {
                        h = targetH;
                        w = h * sRatio;
                        x += (targetW - w) / 2;
                        // y = 0;
                    } else {
                        // x = 0;
                        y += (targetH - h) / 2;
                    }
                } else {
                    h = targetH;
                    w = h * sRatio;
                    if (w > targetW) {
                        w = targetW;
                        h = w / sRatio;
                        // x = 0;
                        y += (targetH - h) / 2;
                    } else {
                        x += (targetW - w) / 2;
                        // y = 0;
                    }
                }
                targetW = w;
                targetH = h;
                tRatio = targetW / targetH;
            } else {
                int tempW = (int) imgW;
                if (tempW % 8 != 0) {
                    //opengl补了黑边，此处要裁剪
                    imgW = (float) (tempW * tempW) / (float) ((tempW / 8 + 1) * 8);
                    imgH = imgW / sRatio;
                }
            }

            x = x * mScale + mPivotX * (1 - mScale);
            y = y * mScale + mPivotY * (1 - mScale);
            w = w * mScale;
            h = h * mScale;

            if (!mDragging && mPosition != NONE) {
                if ((mPosition & (LEFT | RIGHT)) == (LEFT | RIGHT)) {
                    mOffsetX = width / 2 - (x + w / 2);
                } else if ((mPosition & LEFT) == LEFT) {
                    mOffsetX = -x;
                } else if ((mPosition & RIGHT) == RIGHT) {
                    mOffsetX = width - w - x;
                }
                if ((mPosition & (TOP | BOTTOM)) == (TOP | BOTTOM)) {
                    mOffsetY = height / 2 - (y + h / 2);
                } else if ((mPosition & TOP) == TOP) {
                    mOffsetY = -y;
                } else if ((mPosition & BOTTOM) == BOTTOM) {
                    mOffsetY = height - h - y;
                }
                mPosition = NONE;
            }
            x += mOffsetX;
            y += mOffsetY;

            mX = (int) x;
            mY = (int) y;
            mWidth = (int) w;
            mHeight = (int) h;

            if (sRatio > tRatio) {
                float newSourceH = imgH;
                float newSourceW = newSourceH * tRatio;
                if (Utils.getGLVersion(mContext) == 1) {
                    newSourceW = imgW * newSourceW / Utils.nextPowerOf2((int) imgW);
                }
                float offset = (imgW - newSourceW) / 2;
                mYuvTexture.setSourceSize((int) newSourceW, (int) newSourceH);
                mYuvTexture.setSourceLeft((int) offset);
                mYuvTexture.setSourceTop(0);
            } else {
                float newSourceW = imgW;
                float newSourceH = newSourceW / tRatio;
                if (Utils.getGLVersion(mContext) == 1) {
                    newSourceH = imgH * newSourceH / Utils.nextPowerOf2((int) imgH);
                }
                float offset = (imgH - newSourceH) / 2;
                mYuvTexture.setSourceSize((int) newSourceW, (int) newSourceH);
                mYuvTexture.setSourceLeft(0);
                mYuvTexture.setSourceTop((int) offset);
            }

            if (Utils.getGLVersion(mContext) == 1) {
                float newSourceW = imgW;
                float newSourceH = imgH;
                float offset = 0;
                mYuvTexture.setSourceSize((int) newSourceW, (int) newSourceH);
                mYuvTexture.setSourceLeft(0);
                mYuvTexture.setSourceTop((int) offset);
            }

            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            canvas.translate(centerX, centerY);
            if (mMirror) {
                if ((deviceAngle % 2) == (isLandscape ? 1 : 0)) {
                    canvas.scale(-1, 1, 1);
                } else {
                    canvas.scale(1, -1, 1);
                }
            }

            int finalRotate = 0;
            if (isLocal()) {
                int localRotateAngle = 0;
                if (isLandscape) {
                    localRotateAngle = ILiveFunc.offsetRotation(rotation * 90, 270);
                    if (isLandscape != bInitLandscape && 3 == deviceAngle
                            && bFrontCamera){    // 自动旋转，前置右旋补丁
                        localRotateAngle = ILiveFunc.offsetRotation(localRotateAngle, 180);
                    }
                } else {
                    localRotateAngle = rotation * 90;
                }
                finalRotate = ILiveFunc.offsetRotation(localRotateAngle, localRotationFix);
            } else {
                if (!rotate) {
                    if (isLandscape) {
                        finalRotate = ILiveFunc.offsetRotation(rotation * 90, 270);
                        if (isLandscape != bInitLandscape && 3 == deviceAngle){    // 自动旋转，右旋补丁
                            finalRotate = ILiveFunc.offsetRotation(finalRotate, 180);
                        }
                    } else {
                        finalRotate = rotation * 90;
                    }
                } else {
                    if (!isSameDegree(tmpRatio, sRatio)) {
                        if (deviceAngle == 0 || deviceAngle == 3) {
                            finalRotate = 270;
                        } else {
                            finalRotate = isLandscape ? 270 : 90;
                        }
                    }
                }
            }
            canvas.rotate(finalRotate, 0, 0, 1);
            if (finalRotate == 90 || finalRotate == 270) {  // 有旋转
                canvas.translate(-centerY, -centerX);
            } else {
                canvas.translate(-centerX, -centerY);
            }

            if (lastRotation != rotation) {
                ILiveLog.di(TAG, "render", new ILiveLog.LogExts().put("same", sameDirectionRenderMode)
                        .put("diff", diffDirectionRenderMode)
                        .put("landscape", isLandscape)
                        .put("tablet", isTablet));
                ILiveLog.ki(TAG, "render", new ILiveLog.LogExts().put("id", getIdentifier())
                        .put("switched", switched)
                        .put("hasBorder", hasBorder)
                        .put("visibility", getVisibility()));
                if (isLocal()) {
                    ILiveLog.di(TAG, "render", new ILiveLog.LogExts().put("finalRotation", finalRotate).put("localFix", localRotationFix));
                } else {
                    ILiveLog.di(TAG, "render", new ILiveLog.LogExts().put("finalRotation", finalRotate));
                }
                ILiveLog.di(TAG, "render", new ILiveLog.LogExts().put("x", mX)
                        .put("y", mY)
                        .put("width", mWidth)
                        .put("height", mHeight));
                lastRotation = rotation;
            }

            if (isLocal() && iDropFrame > 0){
                mWidth = 1;
                mHeight = 1;
                iDropFrame --;
            }
            mYuvTexture.draw(canvas, mX, mY, mWidth, mHeight);
            canvas.restore();
        }else{
            long curTime = ILiveFunc.getCurrentSec();
            if (curTime > (timeLimit + 3)){
                ILiveLog.LogExts logExts = new ILiveLog.LogExts().put("identifier", mIdentifier)
                        .put("mNeedRenderVideo", mNeedRenderVideo);
                if (null != mYuvTexture){
                    logExts.put("canRender", mYuvTexture.canRender());
                }
                ILiveLog.ki(TAG, "render", logExts);
                timeLimit = curTime;
            }

        }
        // render loading
        if (mLoading && mLoadingTexture != null) {
            mLoadingAngle = mLoadingAngle % 360;
            int uiWidth = getWidth();
            int uiHeight = getHeight();
            int width = mLoadingTexture.getSourceWidth();
            int height = mLoadingTexture.getSourceHeight();
            if (width > uiWidth) {
                width = uiWidth;
            }
            if (height > uiHeight) {
                height = uiHeight;
            }
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.translate(uiWidth / 2, uiHeight / 2);
            canvas.rotate(mLoadingAngle, 0, 0, 1);
            canvas.translate(-width / 2, -height / 2);
            mLoadingTexture.draw(canvas, 0, 0, width, height);
            canvas.restore();
            long now = System.currentTimeMillis();
            if (now - mLastLoadingTime >= LOADING_ELAPSE) {
                mLastLoadingTime = now;
                mLoadingAngle += 8;
            }
        }
        // render text
        if (mStringTexture != null) {
            int uiWidth = getWidth();
            int uiHeight = getHeight();
            int width = mStringTexture.getSourceWidth();
            int height = mStringTexture.getSourceHeight();
            if (width > uiWidth) {
                width = uiWidth;
            }
            if (height > uiHeight) {
                height = uiHeight;
            }
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.translate(uiWidth / 2 - width / 2, uiHeight / 2 - height / 2);
            mStringTexture.draw(canvas, 0, 0, width, height);
            canvas.restore();
        }
    }


    private boolean isSameDegree(double r1, double r2) {
        return (r1 > 1 && r2 > 1) || (r1 < 1 && r2 < 1);
    }

    boolean switchWH(double r1, double r2, boolean isLocal, int rotation, boolean isLandscape) {
        if (isLocal) {
            return !isSameDegree(r1, r2);
        }
//        //本地高宽是否需调换（从观众角度看）
        if (rotate) {    // 开启方向适应
            return !isSameDegree(r1, r2);
        } else {
            if (isLandscape) {   // 横屏且方向为偶数
                return 0 == rotation % 2;
            } else {      // 竖屏且方向为奇数
                return 1 == rotation % 2;
            }
        }
    }

    public boolean isLocal() {
        return null != mIdentifier && mIdentifier.equals(ILiveLoginManager.getInstance().getMyUserId());
    }


    /**
     * 清除缓存数据
     */
    public void resetCache() {
        isFristFrame = false;
        mImageWidth = 0;
        mImageHeight = 0;
        mImageAngle = 0;
    }


    boolean hasBlackBorder(double r1, double r2) {
        if (null != mIdentifier && mIdentifier.equals("")) {
            return false;
        }
        if (isLocal()) {
            return false;
        }
        if (isSameDegree(r1, r2)) {
            return sameDirectionRenderMode == BaseRenderMode.BLACK_TO_FILL;
        } else {
            return diffDirectionRenderMode == BaseRenderMode.BLACK_TO_FILL;
        }
    }

    /**
     * 设置视频数据回调
     */
    public void setVideoListener(VideoListener listener) {
        this.videoListener = listener;
    }

    public VideoListener getVideoListener() {
        return videoListener;
    }

    @Override
    public void setRender(String identifier, int videoSrcType) {
        super.setRender(identifier, videoSrcType);
        ILiveLog.ki(TAG, "setRender", new ILiveLog.LogExts().put("identifier", identifier)
            .put("videoSrcType", videoSrcType));
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (!TextUtils.isEmpty(getIdentifier())) {
            ILiveLog.ki(TAG, "setVisibility", new ILiveLog.LogExts().put("identifier", getIdentifier())
                    .put("visibility", visibility));
        }
    }

    @Override
    public void setBackground(int resId) {
        super.setBackground(resId);
        ILiveLog.ki(TAG, "setBackground", new ILiveLog.LogExts().put("res", resId));
    }

    @Override
    public void setBackground(Bitmap bitmap) {
        super.setBackground(bitmap);
        ILiveLog.ki(TAG, "setBackground", new ILiveLog.LogExts().put("bitmap", bitmap));
    }
}
