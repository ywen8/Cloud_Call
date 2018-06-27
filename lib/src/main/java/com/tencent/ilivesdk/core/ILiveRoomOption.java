package com.tencent.ilivesdk.core;

import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveMemStatusLisenter;
import com.tencent.ilivesdk.adapter.CommonConstants;

/**
 * 进入房间参数配置
 */
public class ILiveRoomOption<Self extends ILiveRoomOption<Self>> {
    /**
     * 异常回调
     */
    public interface onExceptionListener {
        // 房间中出现的可忽略的异常
        void onException(int exceptionId, int errCode, String errMsg);
    }

    /**
     * 房间异常退出回调(一般为断网)
     */
    public interface onRoomDisconnectListener {
        // 房间异常退出
        void onRoomDisconnect(int errCode, String errMsg);
    }

    /**
     * 请求画面回调
     */
    public interface onRequestViewListener {
        // 房间异常退出
        void onComplete(String identifierList[], AVView viewList[], int count, int result, String errMsg);
    }

    /**
     * 音频初始化完成回调
     */
    public interface onAudioInitCompletedListener {
        // 音频初始化完成回调
        void onAudioInitCompleted();
    }

    private String mStrHostId;              // 房间创建者id

    private boolean bIMSupport = true;      // 自动创建IM群组
    private boolean bAVSupport = true;      // 支持互动直播功能
    private String mIMGroupId = "";                 // IM群组id
    private String mGroupType = "AVChatRoom";       // IM群组类型

    private boolean bAutoCamera = true;    // 进入房间后自动打开摄像头
    private boolean bAutoMic    = true;    // 进入房间后自动打开Mic
    private boolean bAutoSpeaker = true;    // 进入房间后自动打开扬声器
    private int mCameraId = ILiveConstants.FRONT_CAMERA;    // 默认摄像头id

    private boolean bEnableHwEnc = true;           // 是否开启硬件编码
    private boolean bEnableHwDec = true;           // 是否开启硬件解码

    private ILiveCameraListener mCameraListener = null;     // 摄像头事件回调

    private int iVideoMode = ILiveConstants.VIDEOMODE_BSUPPORT;

    private long mAuthBits = CommonConstants.Const_Auth_Host;                  // 通话能力权限位，默认拥有所有权限
    private String mAvControlRole = "";                                 // 角色
    private int mAudioCategory = CommonConstants.Const_AudioCategory_Host;       // 场景策略，默认为VoIP模式(适合实时音频通信)
    private byte[] mAuthBuffer = null;                                  // 通话能力权限位的加密串
    private int mVideoRecvMode = CommonConstants.Const_AutoRecv_Camera;    // 视频接收模式，默认为半自动接收模式
    private int mScreenRecvMode = CommonConstants.Const_AutoRecv_Screen;  // 屏幕分享接收模式，默认为半自动接收模式

    private boolean bAutoRender = true;     // 自动渲染用户视频数据
    private boolean bHighAudioQuality = false;              // 启动高清音质
    private boolean bAutoFocus = false;
    private boolean bDegreeFix = false;       // 是否在发送视频数据之前纠正视频画面
    private boolean bHostMirror = true;        // 是否开启主播镜像

    private ILiveMemStatusLisenter memberStatusLisenter = null;     // 房间状态回调
    private onExceptionListener mExceptionListener = null;              // 异常回调
    private onRoomDisconnectListener mRoomDisconnectListener = null;   // 房间异常退出回调
    private onAudioInitCompletedListener mAudioInitCompletedListener = null;    // 音频初始化完成回调
    private onRequestViewListener requestViewListener = null;

    public ILiveRoomOption(String hostId){
        mStrHostId = hostId;
    }

    /**
     * 设置进入房间后是否自动开启摄像头
     */
    public Self autoCamera(boolean enable){
        this.bAutoCamera = enable;
        return (Self)this;
    }

    /**
     * 设置默认摄像头id
     * @param cameraId 摄像头id
     * @see ILiveConstants#FRONT_CAMERA
     * @see ILiveConstants#BACK_CAMERA
     * @see ILiveConstants#NONE_CAMERA
     */
    public Self cameraId(int cameraId){
        this.mCameraId = cameraId;
        return (Self)this;
    }

    /**
     * 设置摄像头事件回调
     */
    public Self cameraListener(ILiveCameraListener listener){
        this.mCameraListener = listener;
        return (Self)this;
    }

    /**
     * 设置进入房间后是否自动打开Mic
     */
    public Self autoMic(boolean enable){
        this.bAutoMic = enable;
        return (Self)this;
    }

    /**
     * 设置进入房间后是否自动打开扬声器
     */
    public Self autoSpeaker(boolean enable){
        this.bAutoSpeaker = enable;
        return (Self)this;
    }

    /**
     * 设置是否开启硬件编码(默认开启)
     */
    public Self enableHwEnc(boolean enable){
        this.bEnableHwEnc = enable;
        return (Self)this;
    }

    /**
     * 设置是否开启硬件解码(默认开启)
     */
    public Self enableHwDec(boolean enable){
        this.bEnableHwDec = enable;
        return (Self)this;
    }

    /**
     * 设置视频模式(支持后台，普通、后台静默)
     * @param mode 模式
     * @see ILiveConstants#VIDEOMODE_NORMAL
     * @see ILiveConstants#VIDEOMODE_BSUPPORT
     * @see ILiveConstants#VIDEOMODE_BMUTE
     */
    public Self videoMode(int mode){
        this.iVideoMode = mode;
        return (Self)this;
    }

    /**
     * 设置音频初始化回调
     */
    public Self audioInitCompletedListener(onAudioInitCompletedListener listener){
        this.mAudioInitCompletedListener = listener;
        return (Self)this;
    }

    /**
     * 设置是否自动创建Im群组
     */
    public Self imsupport(boolean enable){
        this.bIMSupport = enable;
        return (Self)this;
    }

    /**
     * 设置是否进入AV房间(支持互动直播能力)
     */
    public Self avsupport(boolean enable){
        this.bAVSupport = enable;
        return (Self)this;
    }

    /**
     * 自定义IM群组id
     */
    public Self imGroupId(String strGroupId){
        this.mIMGroupId = strGroupId;
        return (Self)this;
    }

    /**
     * 设置IM群组类型
     * @param groupType
     * @return
     */
    public Self groupType(String groupType) {
        this.mGroupType = groupType;
        return (Self)this;
    }

    /**
     * 设置通话能力权限位
     */
    public Self authBits(long authBits){
        mAuthBits = authBits;
        return (Self)this;
    }

    /**
     * 设置角色
     */
    public Self controlRole(String role){
        mAvControlRole = role;
        return (Self)this;
    }

    /**
     * 场景策略
     */
    public Self audioCategory(int audioCategory){
        mAudioCategory = audioCategory;
        return (Self)this;
    }

    /**
     * 通话能力权限位的加密串
     */
    public Self authBuffer(byte[] authBuffer){
        mAuthBuffer = authBuffer;
        return (Self)this;
    }

    /**
     * 设置视频接收模式
     */
    public Self videoRecvMode(int videoRecvMode){
        mVideoRecvMode = videoRecvMode;
        return (Self)this;
    }

    /**
     * 设置屏幕分享接收模式
     */
    public Self screenRecvMode(int screenRecvMode){
        mScreenRecvMode = screenRecvMode;
        return (Self)this;
    }

    /**
     * 设置是否自动渲染用户视频
     */
    public Self autoRender(boolean enable) {
        this.bAutoRender = enable;
        return (Self)this;
    }

    /**
     * 设置是否自动对焦用户视频
     */
    public Self autoFocus(boolean enable) {
        this.bAutoFocus = enable;
        return (Self)this;
    }

    /**
     * 设置高清音质
     */
    public Self highAudioQuality(boolean enable) {
        this.bHighAudioQuality = enable;
        return (Self)this;
    }

    /**
     * 是否在发送视频数据之前纠正视频画面
     */
    @Deprecated
    public Self degreeFix(boolean enable){
        this.bDegreeFix = enable;
        return (Self)this;
    }

    public boolean isHostMirror() {
        return bHostMirror;
    }

    /**
     *  是否开启主播镜像
     */
    public Self setHostMirror(boolean bHostMirror) {
        this.bHostMirror = bHostMirror;
        return (Self)this;
    }

    /**
     * 设置房间状态回调(老的EndPoint事件)
     * @param listener 事件回调
     */
    public Self setRoomMemberStatusLisenter(ILiveMemStatusLisenter listener) {
        this.memberStatusLisenter = listener;
        return (Self)this;
    }

    /**
     *设置请求画面回调()
     *
     * @param listener 事件回调
     */
    public Self setRequestViewLisenter(onRequestViewListener listener) {
        this.requestViewListener = listener;
        return (Self) this;
    }


    /**
     * 设置异常回调
     * @param listener 异常回调
     * @return
     */
    public Self exceptionListener(onExceptionListener listener){
        this.mExceptionListener = listener;
        return (Self)this;
    }

    /**
     * 设置房间异常退出回调
     * @param listener 异常退出回调
     * @return
     */
    public Self roomDisconnectListener(onRoomDisconnectListener listener){
        this.mRoomDisconnectListener = listener;
        return (Self)this;
    }

    // 获取函数
    public boolean isAutoCamera() {
        return bAutoCamera;
    }

    public boolean isAutoMic() {
        return bAutoMic;
    }

    public boolean isAutoSpeaker() {
        return bAutoSpeaker;
    }

    public boolean isEnableHwEnc() {
        return bEnableHwEnc;
    }

    public boolean isEnableHwDec() {
        return bEnableHwDec;
    }

    public int getVideoMode() {
        return iVideoMode;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public String getStrHostId() {
        return mStrHostId;
    }

    public long getAuthBits() {
        return mAuthBits;
    }

    public String getAvControlRole() {
        return mAvControlRole;
    }

    public int getAudioCategory() {
        return mAudioCategory;
    }

    public byte[] getAuthBuffer() {
        return mAuthBuffer;
    }

    public int getVideoRecvMode() {
        return mVideoRecvMode;
    }

    public int getScreenRecvMode() {
        return mScreenRecvMode;
    }

    public boolean isIMSupport(){
        return bIMSupport;
    }

    public boolean isAVSupport() {
        return bAVSupport;
    }

    public String getIMGroupId() {
        return mIMGroupId;
    }

    public String getGroupType() {
        return mGroupType;
    }

    public boolean isAutoRender() {
        return bAutoRender;
    }

    public boolean isAutoFocus() {
        return bAutoFocus;
    }

    @Deprecated
    public boolean isDegreeFix(){
        return bDegreeFix;
    }

    public ILiveMemStatusLisenter getMemberStatusLisenter() {
        return memberStatusLisenter;
    }

    public boolean isHighAudioQuality() {
        return bHighAudioQuality;
    }

    public onExceptionListener getExceptionListener() {
        return mExceptionListener;
    }

    public onRoomDisconnectListener getRoomDisconnectListener(){
        return mRoomDisconnectListener;
    }

    public ILiveCameraListener getCameraListener() {
        return mCameraListener;
    }
	
    public onRequestViewListener getRequestViewListener() {
        return requestViewListener;
    }

    public onAudioInitCompletedListener getAudioInitCompletedListener() {
        return mAudioInitCompletedListener;
    }
}
