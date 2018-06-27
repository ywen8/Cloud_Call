package com.tencent.ilivesdk.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xkazerzhang on 2017/9/5.
 */
public class ILiveHttpReq {
    private String _url;
    private Map<String, String> _headers;
    private byte[] _data;

    public String get_url() {
        return _url;
    }

    public ILiveHttpReq set_url(String url) {
        this._url = url;
        return this;
    }

    public Map<String, String> get_headers() {
        return _headers;
    }

    public ILiveHttpReq set_headers(Map<String, String> headers) {
        this._headers = headers;
        return this;
    }

    public ILiveHttpReq add_header(String param, String value){
        if (null == _headers){
            _headers = new HashMap<>();
        }
        _headers.put(param, value);
        return this;
    }

    public byte[] get_data() {
        return _data;
    }

    public ILiveHttpReq set_data(byte[] data) {
        this._data = data;
        return this;
    }
}
