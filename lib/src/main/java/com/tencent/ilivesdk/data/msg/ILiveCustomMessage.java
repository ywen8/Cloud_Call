package com.tencent.ilivesdk.data.msg;

import com.tencent.TIMCustomElem;
import com.tencent.TIMElem;
import com.tencent.ilivesdk.data.ILiveMessage;

/**
 * Created by xkazerzhang on 2017/9/12.
 */
public class ILiveCustomMessage extends ILiveMessage {
    private TIMCustomElem customElem = new TIMCustomElem();

    @Override
    public TIMElem getTIMElem() {
        return customElem;
    }

    public ILiveCustomMessage(byte[] data, String desc){
        setMsgType(ILIVE_MSG_TYPE_CUSTOM);
        customElem.setData(data);
        customElem.setDesc(desc);
    }

    public ILiveCustomMessage(TIMElem elem){
        setMsgType(ILIVE_MSG_TYPE_CUSTOM);
        customElem = (TIMCustomElem)elem;
    }

    public void setExts(byte[] exts){
        customElem.setExt(exts);
    }

    public void setSound(byte[] sound){
        setMsgType(ILIVE_MSG_TYPE_CUSTOM);
        customElem.setSound(sound);
    }

    public byte[] getData(){
        return customElem.getData();
    }

    public String getDesc(){
        return customElem.getDesc();
    }

    public byte[] getExts(){
        return customElem.getExt();
    }

    public byte[] getSound(){
        return customElem.getSound();
    }
}
