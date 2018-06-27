package com.tencent.ilivesdk.data.msg;

import com.tencent.TIMElem;
import com.tencent.TIMTextElem;
import com.tencent.ilivesdk.data.ILiveMessage;

/**
 * Created by xkazerzhang on 2017/9/12.
 */
public class ILiveTextMessage extends ILiveMessage {
    private TIMTextElem textElem = new TIMTextElem();

    public ILiveTextMessage(String text){
        setMsgType(ILIVE_MSG_TYPE_TEXT);
        textElem.setText(text);
    }

    public ILiveTextMessage(TIMElem elem){
        setMsgType(ILIVE_MSG_TYPE_TEXT);
        textElem = (TIMTextElem)elem;
    }

    @Override
    public TIMElem getTIMElem() {
        return textElem;
    }

    /** 获取文本消息 */
    public String getText() {
        return textElem.getText();
    }

    /** 设置消息文本 */
    public void setText(String text) {
        textElem.setText(text);
    }
}
