package com.tencent.ilivesdk.data;

/**
 * Created by xkazerzhang on 2017/9/6.
 */
public class ILiveHttpConfig {
    private int _thread_numbers = 1;
    private int _connect_timeout = 3000;
    private int _read_timeout = 3000;

    public int get_thread_numbers() {
        return _thread_numbers;
    }

    public ILiveHttpConfig set_thread_numbers(int thread_numbers) {
        this._thread_numbers = thread_numbers;
        return this;
    }

    public int get_connect_timeout() {
        return _connect_timeout;
    }

    public ILiveHttpConfig set_connect_timeout(int connect_timeout) {
        this._connect_timeout = connect_timeout;
        return this;
    }

    public int get_read_timeout() {
        return _read_timeout;
    }

    public ILiveHttpConfig set_read_timeout(int read_timeout) {
        this._read_timeout = read_timeout;
        return this;
    }
}
