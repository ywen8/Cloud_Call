package com.tencent.ilivesdk.adapter;

import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVView;

/**
 * Created by xkazerzhang on 2017/7/25.
 */
public class CommonConstants {
    // 音频场景
    /** 开播场景(用于主播) */
    public static int Const_AudioCategory_Host = AVRoomMulti.AUDIO_CATEGORY_MEDIA_PLAY_AND_RECORD;
    /** 观看场景(用于观众) */
    public static int Const_AudioCategory_Guest = AVRoomMulti.AUDIO_CATEGORY_MEDIA_PLAYBACK;

    // 自动接收
    /** 进房间自动请求Camera视频画面 */
    public static int Const_AutoRecv_Camera = AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO;
    /** 进房间自动请求Screen视频画面 */
    public static int Const_AutoRecv_Screen = AVRoomMulti.SCREEN_RECV_MODE_SEMI_AUTO_RECV_SCREEN_VIDEO;

    // 权限
    /** 创建房间权限 */
    public static long Const_Auth_Create = AVRoomMulti.AUTH_BITS_CREATE_ROOM;
    /** 默认权限(全开) */
    public static long Const_Auth_Host = AVRoomMulti.AUTH_BITS_DEFAULT;
    /** 主播权限(仅下行权限) */
    public static long Const_Auth_Member = AVRoomMulti.AUTH_BITS_JOIN_ROOM | AVRoomMulti.AUTH_BITS_RECV_AUDIO | AVRoomMulti.AUTH_BITS_RECV_CAMERA_VIDEO | AVRoomMulti.AUTH_BITS_RECV_SCREEN_VIDEO;


    // 视频数据类型
    /** Camera视频数据(摄像头采集数据) */
    final public static int Const_VideoType_Camera = AVView.VIDEO_SRC_TYPE_CAMERA;
    /** Screen视频数据(屏幕分享数据) */
    final public static int Const_VideoType_Screen = AVView.VIDEO_SRC_TYPE_SCREEN;
    /** File视频数据(播片数据) */
    final public static int Const_VideoType_File = AVView.VIDEO_SRC_TYPE_MEDIA;

    static public class ILiveUserInfo{
        /** sdk appid */
        public int sdkAppId;
        /** account type */
        public int accountType;
        /** id */
        public String identifier;
    }
}
