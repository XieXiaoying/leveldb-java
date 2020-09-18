package com.xiexy.base.include;

enum Code{
    kOk(0),kNotFound(1),kCorruption(2),kNotSupported(3),kInvalidArgument(4),kIOError(5);
    private int code;
    Code(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}