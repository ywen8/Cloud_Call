package com.tencent.ilivesdk;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;

import com.tencent.TIMManager;
import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVContext;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivesdk.adapter.AccountEngine;
import com.tencent.ilivesdk.adapter.AudioEngine;
import com.tencent.ilivesdk.adapter.CommunicationEngine;
import com.tencent.ilivesdk.adapter.ContextEngine;
import com.tencent.ilivesdk.adapter.ConversationEngine;
import com.tencent.ilivesdk.adapter.GroupEngine;
import com.tencent.ilivesdk.adapter.LoginEngine;
import com.tencent.ilivesdk.adapter.VideoEngine;
import com.tencent.ilivesdk.adapter.avsdk_impl.AVSDKContext;
import com.tencent.ilivesdk.adapter.imsdk_impl.IMSDKCommunication;
import com.tencent.ilivesdk.adapter.imsdk_impl.IMSDKConversation;
import com.tencent.ilivesdk.adapter.imsdk_impl.IMSDKGroup;
import com.tencent.ilivesdk.adapter.imsdk_impl.IMSDKLogin;
import com.tencent.ilivesdk.adapter.tlssdk_impl.TLSSDKAccount;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.impl.ILVBLog;
import com.tencent.ilivesdk.tools.log.LogUploader;
import com.tencent.imsdk.util.QualityReportHelper;
import com.tencent.liteav.basicDR.datareport.TXDRApi;


/**
 * SDK
 */
public class ILiveSDK {
    private static String TAG = "ILiveSDK";
    private Context mApplicationContext;

    private ContextEngine mContextEngine = new AVSDKContext();
    private GroupEngine mGroupEngine = new IMSDKGroup();
    private ConversationEngine mConversationEngine = new IMSDKConversation();
    private AccountEngine mAccountEngine = new TLSSDKAccount();
    private LoginEngine mLoginEngine = new IMSDKLogin();
    private CommunicationEngine mComunicationEngine = new IMSDKCommunication();

    private int mAppId = 0;
    private int mAccountType = 0;
    private String mVersion = "1.6.3.r7";

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    public boolean runOnMainThread(Runnable r, long delay) {
        return mMainHandler.postDelayed(r, delay);
    }

    public int getAppId() {
        return mAppId;
    }

    public int getAccountType() {
        return mAccountType;
    }


    private ILiveSDK() {
    }

    /* 内部私有静态实例，目的是实现延迟加载 */
    private static class TIMSdkHolder {
        private static ILiveSDK instance = new ILiveSDK();
    }

    /**
     * 获取iLiveSDK单例
     *
     * @return
     */
    public static ILiveSDK getInstance() {
        return TIMSdkHolder.instance;
    }


    /**
     * 初始化 SDK
     *
     * @param context     APPContext
     * @param appId       app ID
     * @param accountType 类型
     */
    public void initSdk(Context context, int appId, int accountType) {
        this.mApplicationContext = context.getApplicationContext();
        this.mAppId = appId;
        this.mAccountType = accountType;

        // 初始化日志
        ILVBLog.init(mApplicationContext);
        mLoginEngine.init();
        mContextEngine.init();
        mAccountEngine.init();  // 只能在Login模块后初始化

        ILiveLog.kd(TAG, "initSdk->init", new ILiveLog.LogExts()
                .put("appid", appId)
                .put("accountType", accountType)
                .put("version", getVersion())
                .put("abi", ILiveFunc.getABI()));

        TXDRApi.init(String.valueOf(mAppId));
        TXDRApi.txReportDAU(mApplicationContext, ILiveConstants.EVENT_ILIVE_INIT_NEW, 0, "success", 10, mVersion);
        QualityReportHelper helper = new QualityReportHelper();
        helper.init(ILiveConstants.EVENT_ILIVE_INIT, getAppId(), "");
        helper.report();
    }

    /**
     * 获取版本号
     *
     * @return 颁布号
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * 封装接口 获取音视频控制类
     *
     * @return AVContext对象
     */
    // AVSDK 相关(保留接口)
    public AVContext getAVContext() {
        return (AVContext) mContextEngine.getContextObj();
    }

    /**
     * 封装接口 获取音频控制类
     *
     * @return AVAudioCtrl对象
     */
    @Deprecated
    public AVAudioCtrl getAvAudioCtrl() {
        if (null != getAVContext()) {
            return getAVContext().getAudioCtrl();
        }
        return null;
    }

    /**
     * 封装接口 获取视频控制类
     *
     * @return AVVideoCtrl对象
     */
    @Deprecated
    public AVVideoCtrl getAvVideoCtrl() {
        if (null != getAVContext()) {
            return getAVContext().getVideoCtrl();
        }
        return null;
    }

    /**
     * 上报日志
     *
     * @param desc      描述
     * @param dayOffset 日期，0为当天，1为昨天，以此类推
     * @param callBack  回调
     */
    public void uploadLog(String desc, int dayOffset, ILiveCallBack<String> callBack) {
        LogUploader.getInstance().start(desc, getAppContext(), dayOffset, getAppId(), ILiveLoginManager.getInstance().getMyUserId(), callBack);
    }


    /**
     * 获取IMSDK 句柄
     *
     * @return
     */
    @Deprecated
    public TIMManager getTIMManger() {
        return TIMManager.getInstance();
    }

    public ContextEngine getContextEngine() {
        return mContextEngine;
    }

    public AudioEngine getAudioEngine() {
        return mContextEngine.getAudioAdapter();
    }

    public VideoEngine getVideoEngine() {
        return mContextEngine.getVideoAdapter();
    }

    public GroupEngine getGroupEngine() {
        return mGroupEngine;
    }

    public ConversationEngine getConversationEngine() {
        return mConversationEngine;
    }

    public AccountEngine getAccountEngine() {
        return mAccountEngine;
    }

    public LoginEngine getLoginEngine() {
        return mLoginEngine;
    }

    public CommunicationEngine getComunicationEngine(){
        return mComunicationEngine;
    }


    /**
     * 获取应用上下文Context
     *
     * @return ApplicationContext
     */
    public Context getAppContext() {
        return mApplicationContext;
    }


    public boolean isTabletDevice(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
    // 新定义接口
}
