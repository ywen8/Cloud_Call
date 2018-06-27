package com.tencent.ilivesdk.data;

import com.tencent.TIMConversation;
import com.tencent.TIMElem;

/**
 * IM消息格式
 */
public abstract class ILiveMessage {
    /**
     * 文本消息类型
     */
    public static final int ILIVE_MSG_TYPE_TEXT = 0;
    /**
     * 自定义消息类型
     */
    public static final int ILIVE_MSG_TYPE_CUSTOM = 5;

    /**
     * 其它消息类型
     */
    public static final int ILIVE_MSG_TYPE_OTHER = 9;

    /**
     * 无效消息
     */
    public static final int ILIVE_CONVERSATION_INVALID = 0;
    /**
     * C2C消息
     */
    public static final int ILIVE_CONVERSATION_C2C = 1;
    /**
     * 群组消息
     */
    public static final int ILIVE_CONVERSATION_GROUP = 2;
    /**
     * 系统消息
     */
    public static final int ILIVE_CONVERSATION_SYSTEM = 3;

    /**
     * 发送者
     */
    private String sender;

    /**
     * 会话Peer
     */
    private String peer = null;
    /**
     * 会话类型
     */
    private int conversationType = ILIVE_CONVERSATION_INVALID;

    /**
     * 时间戳
     */
    private long timeStamp;

    /**
     * 消息类型(默认为ILIVE_MSG_TYPE_TEXT)
     */
    private int msgType = ILIVE_MSG_TYPE_OTHER;

    /**
     * 获取消息类型，参考ILIVE_MSG_TYPE_TEXT
     */
    public int getMsgType() {
        return msgType;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getPeer() {
        return peer;
    }

    public int getConversationType() {
        return conversationType;
    }

    public void setConversation(TIMConversation conversation) {
        if (null != conversation) {
            peer = conversation.getPeer();
            switch (conversation.getType()) {
                case C2C:
                    conversationType = ILIVE_CONVERSATION_C2C;
                    break;
                case Group:
                    conversationType = ILIVE_CONVERSATION_GROUP;
                    break;
                case System:
                    conversationType = ILIVE_CONVERSATION_SYSTEM;
                    break;
            }
        }
    }

    /**
     * 设置消息类型，参考ILIVE_MSG_TYPE_TEXT
     */
    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public abstract TIMElem getTIMElem();
}
