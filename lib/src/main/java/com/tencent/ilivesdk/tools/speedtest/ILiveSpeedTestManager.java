package com.tencent.ilivesdk.tools.speedtest;

import com.tencent.ilivesdk.core.ILiveLoginManager;

/**
 * 测速接口封装
 */

public class ILiveSpeedTestManager {

    private ILiveSpeedTestManager() {}
    private static ILiveSpeedTestManager instance = new ILiveSpeedTestManager();
    public static ILiveSpeedTestManager getInstance() {
        return instance;
    }

    /**
     * 开始测速
     *
     * @param param 测速参数
     * @param callback 回调
     */
    public void requestSpeedTest(ILiveSpeedTestRequestParam param, ILiveSpeedTestCallback callback) {
        SpeedTest.getInstance().start(ILiveLoginManager.getInstance().getMyUserId(), param.roomId, param.callType, 1, callback);
    }

    /**
     * 停止测速
     */
    public void stopSpeedTest() {
        SpeedTest.getInstance().stop();
    }
}
