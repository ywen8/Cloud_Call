package com.tencent.ilivesdk.tools.speedtest;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.tencent.TIMManager;
import com.tencent.TIMNetworkStatus;
import com.tencent.TIMValueCallBack;
import com.tencent.av.NetworkUtil;
import com.tencent.ilivesdk.protos.gv_comm_operate;
import com.tencent.imsdk.IMMsfCoreProxy;
import com.tencent.imsdk.IMMsfUserInfo;
import com.tencent.imsdk.QLog;
import com.tencent.imsdk.av.MultiVideoTinyId;
import com.tencent.mobileqq.pb.ByteStringMicro;
import com.tencent.mobileqq.pb.InvalidProtocolBufferMicroException;


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 *  音视频接口机测速
 */

public class SpeedTest {

    private static final String TAG = SpeedTest.class.getSimpleName();
    private static final int SUB_CMD_REQ = 0x8;
    private static final int SUB_CMD_REPORT = 0x9;
    private static final int SUB_CMD_REQ_HEAD = 0x144;
    private static final int SUB_CMD_REPORT_HEAD = 0x146;


//    private boolean isRunning = false;
//    private boolean finishing = false;
    Status status = Status.IDLE;
    private Handler handler = new Handler(Looper.getMainLooper());

    //测速用户相关信息
    private long uin;
    private String identifier;
    private int roomId;
    private int callType;
    private int testPurpose;// 0：默认，结束通话上报测速数据或其它一项专项白名单测速; 1：用户主动点击测速，目的是测试当前网络状况，原则上不拒绝,但不把测速数据列入调优
    private ILiveSpeedTestCallback callback;

    //本次测速相关信息
    private int clientIp;
    private long testId;
    private int testType;
    private final List<SpeedTestTask> tasks =
            Collections.synchronizedList(new ArrayList<SpeedTestTask>());
    private int netChange = 0;




    private SpeedTest() {}

    private final static SpeedTest instance = new SpeedTest();

    /**
     *  单例，每次仅允许发起一次测速请求
     */
    public static SpeedTest getInstance() {
        return instance;
    }



   public void start(String identifier,final int roomId,final int callType,final int testPurpose, ILiveSpeedTestCallback callback) {
        if (status != Status.IDLE) {
            callback.onError(SpeedTestConstants.CODE_RUNNING, SpeedTestConstants.MSG_RUNNING);
            return;
        }
        status = Status.RUNNING;
        this.identifier = identifier;
        if (getMsfUserInfo() == null) return;
        this.uin = getMsfUserInfo().getTinyid();
        this.roomId = roomId;
        this.callType = callType;
        this.testPurpose = testPurpose;
        this.callback = callback;
        QLog.i(TAG, QLog.USR, "start speed test by user " + uin + ",testPurpose " + testPurpose
               + ",roomId " + roomId + ",callType " + callType);
        speedTestReq();

    }

    public boolean stop() {
        if (status != Status.RUNNING) {
            return false;
        }
        QLog.i(TAG, QLog.USR, "speed test is stopped by user");
        status = Status.STOPPING;
        testId = 0;
        netChange = 0;
        callback = null;
        return true;
    }

    boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     *  发起请求测速
     */
    private void speedTestReq() {
        //创建请求包头
        //TODO 修改authkey
        gv_comm_operate.GVCommOprHead head = new gv_comm_operate.GVCommOprHead();
        head.uint32_sub_cmd.set(SUB_CMD_REQ);
        head.uint32_buss_type.set(SpeedTestConstants.BUSS_TYPE);
        head.uint32_auth_type.set(SpeedTestConstants.AUTH_TYPE);
        head.uint32_auth_key.set(808161124);
        head.uint64_uin.set(uin);
        head.uint32_sdk_appid.set(IMMsfCoreProxy.get().getSdkAppId());
        //创建请求包体
        gv_comm_operate.ReqBody reqBody = new gv_comm_operate.ReqBody();
        reqBody.req_0x8.setHasFlag(true);
        reqBody.req_0x8.roomid.set(roomId);
        reqBody.req_0x8.call_type.set(callType);
        reqBody.req_0x8.net_type.set(getNetworkClass());
        //client_type 0：unknown 1： pc 2： android 3： iphone 4： ipad
        reqBody.req_0x8.client_type.set(2);
        reqBody.req_0x8.support_type.set(0x1);
        reqBody.req_0x8.test_purpose.set(testPurpose);
        reqBody.req_0x8.os_type.set(3);
        reqBody.req_0x8.os_version.set(ByteStringMicro.copyFrom(android.os.Build.VERSION.RELEASE.getBytes()));


        byte[] busibuf = NetworkUtil.formReq(identifier, SUB_CMD_REQ_HEAD, roomId, "",
                head.toByteArray(), reqBody.toByteArray());

        //do the request
        MultiVideoTinyId.get().requestMultiVideoInfo(busibuf, new TIMValueCallBack<byte[]>() {
            @Override
            public void onError(int code, String desc) {
                SpeedTest.this.onError(code, desc);
            }

            @Override
            public void onSuccess(byte[] rspbody) {
                gv_comm_operate.RspBody rsp = new gv_comm_operate.RspBody();
                byte[] buff = NetworkUtil.parseRsp(rspbody);
                if(buff == null){
                    SpeedTest.this.onError(SpeedTestConstants.ERR_PARSE_RESPONSE_FAILED, "parse streamer rsp failed");
                    return;
                }
                try {
                    rsp.mergeFrom(buff);
                    if (rsp.rsp_0x8.result.get() != 0 || rsp.rsp_0x8.access_list.size() == 0) {
                        SpeedTest.this.onError(SpeedTestConstants.CODE_TEST_REQ_RET_ERROR, "speed test req's response error code " + rsp.rsp_0x8.result.get() + " or ip list size 0");
                        return;
                    }
                    clientIp = rsp.rsp_0x8.client_ip.get();
                    testId = rsp.rsp_0x8.test_id.get();
                    testType = rsp.rsp_0x8.test_type.get();
                    final List<SpeedTestTask> tmpTasks = new ArrayList<SpeedTestTask>();
                    for (gv_comm_operate.SpeedAccessInf info : rsp.rsp_0x8.access_list.get()) {
                        tmpTasks.add(new SpeedTestTask(info, testId, uin, clientIp));
                    }
                    QLog.i(TAG, QLog.USR, "speed test request succeed.test id " + testId + " speed test list:");
                    for (SpeedTestTask task : tmpTasks) {
                        QLog.i(TAG, QLog.USR, task.toString());
                    }
                    if (callback != null) {
                        callback.onStart(new LinkedList<ILiveServerInfo>(tmpTasks));
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                tasks.clear();
                                tasks.addAll(tmpTasks);
                                synchronized(tasks) {
                                    for (final SpeedTestTask task : tasks) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (callback != null) {
                                                    callback.onProgress(task, task.testCnt, task.testGap);
                                                }
                                            }
                                        });
                                        if (status == Status.STOPPING) {
                                            status = Status.IDLE;
                                            break;
                                        }
                                        task.start();
                                    }
                                }
                                if (status == Status.RUNNING) {
                                    speedTestReportReq();
                                }else {
                                    status = Status.IDLE;
                                }
                            }catch (InterruptedException e) {
                                SpeedTest.this.onError(SpeedTestConstants.CODE_TESTING_ERROR, "can not start test thread");
                            }catch (IOException e) {
                                SpeedTest.this.onError(SpeedTestConstants.CODE_TESTING_ERROR, "network io exception");
                            }
                        }
                    }).start();
                } catch (InvalidProtocolBufferMicroException e) {
                    SpeedTest.this.onError(SpeedTestConstants.ERR_PARSE_RESPONSE_FAILED, "parse streamer rsp failed");
                } catch (UnknownHostException e) {
                    SpeedTest.this.onError(SpeedTestConstants.CODE_TEST_IP_UNKNOWN, SpeedTestConstants.MSG_TEST_IP_UNKNOWN);
                }
            }
        });
    }



    /**
     *  获取当前网络类型
     *
     *  @return networkType 0:无网络；1:wifi；2:2g；3:3g；4:4g；10:wap；255:unknow
     *
     */
    private int getNetworkClass() {
        Context context = IMMsfCoreProxy.get().getContext();
        if (IMMsfCoreProxy.get().getNetworkStatus() == TIMNetworkStatus.TIM_NETWORK_STATUS_DISCONNECTED) {
            return 0;
        }
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            return 1;
        }
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return 2;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return 3;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return 4;
            default:
                return 255;
        }
    }


    /**
     *  获取当前网络运营商
     *
     *  @return networkType 0:无网络；1:wifi；2:2g；3:3g；4:4g；10:wap；255:unknow
     *
     */
    private byte[] getNetName() {
        Context context = IMMsfCoreProxy.get().getContext();
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        String name = mTelephonyManager.getSimOperatorName();
        QLog.i(TAG, QLog.DEV, "report info,net name:" + name);
        return name.getBytes();
    }


    private byte[] getWifiName() {
        Context context = IMMsfCoreProxy.get().getContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        QLog.i(TAG, QLog.DEV, "report info,wifi name:" + wifiInfo.getSSID());
        return wifiInfo.getSSID().getBytes();
    }

    private Location getLocation() {
        Context context = IMMsfCoreProxy.get().getContext();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return null;
    }




    /**
     *  发起测速结果上报
     */
    private void speedTestReportReq() {
        QLog.i(TAG, QLog.USR, "start speed test report");
        gv_comm_operate.GVCommOprHead head = new gv_comm_operate.GVCommOprHead();
        head.uint32_sub_cmd.set(SUB_CMD_REPORT);
        head.uint32_buss_type.set(SpeedTestConstants.BUSS_TYPE);        //opensdk
        head.uint32_auth_type.set(SpeedTestConstants.AUTH_TYPE);        //opensdk
        head.uint32_auth_key.set(808161124);
        head.uint64_uin.set(uin);
        head.uint32_sdk_appid.set(IMMsfCoreProxy.get().getSdkAppId());

        gv_comm_operate.ReqBody reqBody = new gv_comm_operate.ReqBody();
        reqBody.req_0x9.setHasFlag(true);
        reqBody.req_0x9.test_id.set(testId);
        reqBody.req_0x9.test_time.set(Calendar.getInstance().getTimeInMillis());
        reqBody.req_0x9.roomid.set(roomId);
        reqBody.req_0x9.client_type.set(2);
        reqBody.req_0x9.net_type.set(getNetworkClass());
        reqBody.req_0x9.net_name.set(ByteStringMicro.copyFrom(getNetName()));
        reqBody.req_0x9.net_name.set(ByteStringMicro.copyFrom(getWifiName()));
        //TODO 添加地理位置的上报
        reqBody.req_0x9.client_ip.set(clientIp);
        reqBody.req_0x9.call_type.set(callType);
        reqBody.req_0x9.sdkappid.set(IMMsfCoreProxy.get().getSdkAppId());
        reqBody.req_0x9.test_type.set(0x1);
        List<gv_comm_operate.SpeedTestResult> rets = new ArrayList<gv_comm_operate.SpeedTestResult>();
        synchronized(tasks) {
            for (SpeedTestTask task : tasks) {
                rets.add(task.getResult());
            }
        }
        reqBody.req_0x9.results.set(rets);
        reqBody.req_0x9.net_changecnt.set(netChange);
        reqBody.req_0x9.access_ip.set(0);
        reqBody.req_0x9.access_port.set(0);

        byte[] busibuf = NetworkUtil.formReq(identifier, SUB_CMD_REPORT_HEAD, roomId, "",
                head.toByteArray(), reqBody.toByteArray());
        //do the request
        MultiVideoTinyId.get().requestMultiVideoInfo(busibuf, new TIMValueCallBack<byte[]>() {

            /**
             * 出错时回调
             *
             * @param code 错误码
             * @param desc 错误描述
             */
            @Override
            public void onError(int code, String desc) {
                SpeedTest.this.onError(code, desc);
            }

            /**
             * 成功时回调
             *
             * @param bytes
             */
            @Override
            public void onSuccess(byte[] bytes) {
                gv_comm_operate.RspBody rsp = new gv_comm_operate.RspBody();
                byte[] buff = NetworkUtil.parseRsp(bytes);
                if(buff == null){
                    SpeedTest.this.onError(SpeedTestConstants.ERR_PARSE_RESPONSE_FAILED, "parse streamer rsp failed");
                    return;
                }
                try {
                    rsp.mergeFrom(buff);
                    if (rsp.rsp_0x9.test_id.get() != testId) {
                        SpeedTest.this.onError(SpeedTestConstants.CODE_TEST_REPORT_ERROR, SpeedTestConstants.MSG_TEST_REPORT_ERROR);
                    }else {
                        QLog.i(TAG, QLog.USR, "speed test report succeed");
                        status = Status.IDLE;
                        testId = 0;
                        netChange = 0;
                        if (callback != null) {
                            callback.onFinish(new LinkedList<ILiveSpeedTestResult>(tasks));
                        }
                    }
                }catch (InvalidProtocolBufferMicroException e) {
                    SpeedTest.this.onError(SpeedTestConstants.ERR_PARSE_RESPONSE_FAILED, "parse streamer rsp failed");
                }

            }
        });
    }




    private String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private IMMsfUserInfo getMsfUserInfo(){
        if (TextUtils.isEmpty(identifier)){
            return IMMsfCoreProxy.get().getMsfUserInfo(TIMManager.getInstance().getIdentification());
        }
        return IMMsfCoreProxy.get().getMsfUserInfo(identifier);
    }

    private void onError(final int code, final String msg) {
        testId = 0;
        netChange = 0;
        QLog.e(TAG, QLog.USR, "onError.code:" + code + ",msg:" + msg);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(code, msg);
                }
            }
        });
        status = Status.IDLE;
    }

//    private boolean changeStatus(Status status) {
//        switch (this.status) {
//            case Status.IDLE:
//                if ()
//                break;
//        }
//
//
//    }

    enum Status {
        IDLE,
        RUNNING,
        STOPPING,
    }


}
