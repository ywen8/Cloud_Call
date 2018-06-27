package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;

/**
 * 音频模块适配器
 */
public interface AudioEngine {
    enum AudioStatus{
        /** 音频设备关闭 */
        AudioClose,
        /** 音频设备开启 */
        AudioOpen,
        /** 音频设备操作中 */
        AudioOperating,
        /** 音频设备不存在 */
        AudioNotExist,
        /** 未知 */
        AudioUnknown
    }

    enum AudioOutputMode{
        /** 扬声器 */
        AudioSpeader,
        /** 听筒 */
        AudioHeadset,
        /** 未知 */
        AudioUnknown
    }

    /** 初始化音频模块 */
    void init(ContextEngine context, ILiveCallBack callBack);
    /** 启动音频模块 */
    void start(ILiveCallBack callBack);
    /** 关闭音频模块 */
    void stop(ILiveCallBack callBack);

    /** 打开麦克风 */
    void enableMic(ILiveCallBack callBack);
    /** 关闭麦克风 */
    void disableMic(ILiveCallBack callBack);
    /** 获取麦克风状态 */
    AudioStatus getMicStatus();

    /** 打开扬声器 */
    void enableSpeaker(ILiveCallBack callBack);
    /** 关闭扬声器 */
    void disableSpeaker(ILiveCallBack callBack);
    /** 获取扬声器状态 */
    AudioStatus getSpeakerStatus();


    /** 获取播放通道 */
    AudioOutputMode getOutputMode();
    /** 设置播放通道 */
    void setOutputMode(AudioOutputMode mode, ILiveCallBack callBack);

    /** 获取音量大小 */
    int getVolumn();
    /** 设置音量大小(0-100) */
    void setVolume(int volume, ILiveCallBack callBack);

    /** 获取底层音频管理类 */
    Object getAudioObj();
}
