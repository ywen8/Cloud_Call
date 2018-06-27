package com.tencent.ilivesdk.adapter.avsdk_impl;

import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVError;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.adapter.AudioEngine;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.core.ILiveLog;

/**
 * AVSDK音频模块
 */
public class AVSDKAudioCtrl implements AudioEngine {
    private final String TAG = "ILVB-AVSDKAudioCtrl";
    AVAudioCtrl avAudioCtrl = null;

    /**
     * 麦克风操作回调
     */
    class AVEnableMicCallBack extends AVAudioCtrl.EnableMicCompleteCallback {
        ILiveCallBack callBack;

        public AVEnableMicCallBack(ILiveCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        protected void onComplete(boolean bEnable, int iError) {
            super.onComplete(bEnable, iError);
            if (AVError.AV_OK == iError) {
                ILiveFunc.notifySuccess(callBack, 0);
            } else {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, iError, "Operate Mic failed");
            }
        }
    }

    /**
     * 扬声器操作回调
     */
    class AVEnableSpeakerCallBack extends AVAudioCtrl.EnableSpeakerCompleteCallback {
        ILiveCallBack callBack;

        public AVEnableSpeakerCallBack(ILiveCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        protected void onComplete(boolean bEnable, int iError) {
            super.onComplete(bEnable, iError);
            if (AVError.AV_OK == iError) {
                ILiveFunc.notifySuccess(callBack, 0);
            } else {
                ILiveFunc.notifyError(callBack, ILiveConstants.Module_AVSDK, iError, "Operate Speaker failed");
            }
        }
    }


    @Override
    public void init(ContextEngine context, ILiveCallBack callBack) {
        avAudioCtrl = (AVAudioCtrl) context.getModuleVar("Audio");
        if (null != avAudioCtrl) {
            ILiveFunc.notifySuccess(callBack, 0);
        } else {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "get AudioCtrl failed");
        }
    }

    @Override
    public void start(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        avAudioCtrl.startTRAEService();
        ILiveFunc.notifySuccess(callBack, 0);
    }

    @Override
    public void stop(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        avAudioCtrl.stopTRAEService();
        ILiveFunc.notifySuccess(callBack, 0);
    }

    @Override
    public void enableMic(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveLog.ke(TAG, "enableMic", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_SDK_FAILED, "no AudioCtrl found");
        }
        avAudioCtrl.enableMic(true, new AVEnableMicCallBack(callBack));
    }

    @Override
    public void disableMic(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveLog.ke(TAG, "disableMic", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_SDK_FAILED, "no AudioCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        avAudioCtrl.enableMic(false, new AVEnableMicCallBack(callBack));
    }

    @Override
    public AudioStatus getMicStatus() {
        if (null == avAudioCtrl) {
            return AudioStatus.AudioClose;
        }
        switch (avAudioCtrl.getMicState()) {
            case AVAudioCtrl.AUDIO_DEVICE_OPEN:
                return AudioStatus.AudioOpen;
            case AVAudioCtrl.AUDIO_DEVICE_OPERATING:
                return AudioStatus.AudioOperating;
            case AVAudioCtrl.AUDIO_DEVICE_NOT_EXIST:
                return AudioStatus.AudioNotExist;
            case AVAudioCtrl.AUDIO_DEVICE_CLOSE:
            default:
                return AudioStatus.AudioClose;
        }
    }

    @Override
    public void enableSpeaker(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveLog.ke(TAG, "enableSpeaker", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_SDK_FAILED, "no AudioCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        avAudioCtrl.enableSpeaker(true, new AVEnableSpeakerCallBack(callBack));
    }

    @Override
    public void disableSpeaker(ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveLog.ke(TAG, "disableSpeaker", ILiveConstants.Module_AVSDK, ILiveConstants.ERR_SDK_FAILED, "no AudioCtrl found");
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        avAudioCtrl.enableSpeaker(false, new AVEnableSpeakerCallBack(callBack));
    }

    @Override
    public AudioStatus getSpeakerStatus() {
        if (null == avAudioCtrl) {
            return AudioStatus.AudioUnknown;
        }
        switch (avAudioCtrl.getSpeakerState()) {
            case AVAudioCtrl.AUDIO_DEVICE_CLOSE:
                return AudioStatus.AudioClose;
            case AVAudioCtrl.AUDIO_DEVICE_OPEN:
                return AudioStatus.AudioOpen;
            case AVAudioCtrl.AUDIO_DEVICE_OPERATING:
                return AudioStatus.AudioOperating;
            case AVAudioCtrl.AUDIO_DEVICE_NOT_EXIST:
                return AudioStatus.AudioNotExist;
            default:
                return AudioStatus.AudioUnknown;
        }
    }

    @Override
    public AudioOutputMode getOutputMode() {
        if (null == avAudioCtrl) {
            return AudioOutputMode.AudioUnknown;
        }

        switch (avAudioCtrl.getAudioOutputMode()) {
            case AVAudioCtrl.OUTPUT_MODE_HEADSET:
                return AudioOutputMode.AudioHeadset;
            case AVAudioCtrl.OUTPUT_MODE_SPEAKER:
                return AudioOutputMode.AudioSpeader;
            default:
                return AudioOutputMode.AudioUnknown;
        }
    }

    @Override
    public void setOutputMode(AudioOutputMode mode, ILiveCallBack callBack) {
        if (null == avAudioCtrl) {
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_AV_NOT_READY, "no AudioCtrl found");
            return;
        }
        boolean bRet = false;
        switch (mode){
            case AudioHeadset:
                bRet = avAudioCtrl.setAudioOutputMode(AVAudioCtrl.OUTPUT_MODE_HEADSET);
                break;
            case AudioSpeader:
                bRet = avAudioCtrl.setAudioOutputMode(AVAudioCtrl.OUTPUT_MODE_SPEAKER);
                break;
        }
        if (bRet)
            ILiveFunc.notifySuccess(callBack, 0);
        else
            ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_SDK_FAILED, "setOutputMode failed");
    }

    @Override
    public int getVolumn() {
        return 0;
    }

    @Override
    public void setVolume(int volume, ILiveCallBack callBack) {
        ILiveFunc.notifyError(callBack, ILiveConstants.Module_ILIVESDK, ILiveConstants.ERR_NOT_SUPPORT, "not support yet");
    }

    @Override
    public Object getAudioObj() {
        return avAudioCtrl;
    }
}
