package com.tencent.ilivesdk.adapter;

import android.content.Context;

import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveRoomOption;

import java.util.List;

/**
 * 上下文模块
 */
public interface ContextEngine {
    interface AVEndPointEvent{      // 兼容接口
        void onEndPointEvent(int event, String[] users);
    }
    interface AVRoomDisconnect{
        void onRoomDisconnect(int errCode, String errMsg);
    }
    interface AVDataChangeEvent{
        void onVideoChangeEvent(boolean hasData, String id, int videoType);
        void onAudioChangeEvent(boolean hasData, String id);
    }

    /** 初始化 */
    void init();
    /** 设置用户信息 */
    boolean setUserInfo(CommonConstants.ILiveUserInfo info);
    /** 设置超时时间 */
    void setTimeOut(int timeOut);

    /** 启动模块 */
    void start(ILiveLog.TILVBLogLevel level, ILiveCallBack callBack);
    /** 停止模块 */
    void stop(ILiveCallBack callBack);

    /** 进入房间 */
    void enterRoom(int roomId, ILiveRoomOption option, ILiveCallBack callBack);
    /** 退出房间 */
    void exitRoom(ILiveCallBack callBack);
    /** 切换房间 */
    void switchRoom(int roomId, ILiveCallBack callBack);
    /** 判断是否在房间中 */
    boolean isEnterRoom();

    /** 跨房连麦 */
    void linkRoom(int roomId, String accountId, String sign, ILiveCallBack callBack);
    /** 取消跨房连麦 */
    void unlinkRoom(ILiveCallBack callBack);

    /** 切换角色 */
    void changeRole(String role, ILiveCallBack callBack);

    /** 设置EndPoint事件 */
    void setEndPointEventListener(AVEndPointEvent listener);
    /** 设置RoomDisconnect事件 */
    void setRoomDisconnectListener(AVRoomDisconnect listener);
    /** 设置音视频数据事件 */
    void setDataChangeListener(AVDataChangeEvent listener);
    /** 设置视频请求回调 */
    void setRequestCompleteListener(ILiveRoomOption.onRequestViewListener listener);

    /** 请求远程用户音频数据(增量添加)  */
    void requestUserAudioData(String identifer);
    /** 清空用户音频数据请求 */
    void releaseUserAudioData();
    /** 请求远程用户视频数据(增量添加) */
    void requestUserVideoData(String identifer, int videoType);
    /** 清空用户视频数据请求 */
    void releaseUserVideoData();
    /** 移除用户数据请求 */
    void removeUserVideoData(String identifer, int videoType);
    /** 暂停后台数据请求 */
    void pauseUserData();
    /** 恢复后台数据请求 */
    void resumeUserData();
    /** 获取需要渲染数据用户列表 */
    List<String> getVideoUserList(int videoType);

    /** 获取模块变量 */
    Object getModuleVar(String key);
    /** 添加模块变量 */
    void setModuleVar(String key, Object value);

    /** 获取底层Context类 */
    Object getContextObj();
    /** 获取底层Room类 */
    Object getRoomObj();

    /** 获取音视适配器 */
    AudioEngine getAudioAdapter();
    /** 获取视频适配器  */
    VideoEngine getVideoAdapter();
}
