package com.tencent.ilivesdk.data.msg;

import com.tencent.TIMElem;
import com.tencent.ilivesdk.data.ILiveMessage;

/**
 * Created by xkazerzhang on 2017/9/12.
 */
public class ILiveOtherMessage extends ILiveMessage {
    private TIMElem elem;

    public ILiveOtherMessage(TIMElem timElem){
        elem = timElem;
    }

    @Override
    public TIMElem getTIMElem() {
        return elem;
    }
}
