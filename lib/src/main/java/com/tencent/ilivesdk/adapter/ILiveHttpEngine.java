package com.tencent.ilivesdk.adapter;

import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.data.ILiveHttpConfig;
import com.tencent.ilivesdk.data.ILiveHttpReq;

/**
 * Created by xkazerzhang on 2017/9/5.
 */
public interface ILiveHttpEngine {

    /**
     *  初始化HTTP线程
     *  @param config HTTP配置
     */
    void init(ILiveHttpConfig config);
    /**
     * 异步提交Get请求
     */
    void asyncGet(ILiveHttpReq req, ILiveCallBack<String> callBack);

    /**
     * 异步提交Post请求
     */
    void asyncPost(ILiveHttpReq req, ILiveCallBack<String> callBack);
}
