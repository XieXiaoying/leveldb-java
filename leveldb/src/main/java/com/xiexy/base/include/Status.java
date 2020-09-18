package com.xiexy.base.include;

import java.util.Arrays;
import static com.google.common.base.Equivalence.equals;
public class Status {
    /**
     *  OK status has a null state_.  Otherwise, state_ is a new[] array of the following form:
     *  state_[0..3] == length of message
     *  state_[4]    == code
     *  state_[5..]  == message
     * */
    private byte[] state_;
    private Code code(){
        if(this.state_ == null) return Code.kOk;
        for(Code code : Code.values()){
            if(code.getCode() == this.state_[4]) return code;
        }
        return Code.kNotFound;
    }

    Status(Code code, Slice msg1, Slice msg2) {

        if(!code.equals(Code.kOk)){
            int len1 = msg1.length();
            int len2 = msg2.length();
            int size = len1 + (len2 > 0 ? (2 + len2) : 0);
            /**
             * 这里源码是+5，因为在c++中，一个char占1字节，源码增加了':'和' '两个char,
             * if (len2) {
             *     result[5 + len1] = ':';
             *     result[6 + len1] = ' ';
             *     std::memcpy(result + 7 + len1, msg2.data(), len2);
             *   }
             *   java中一个char占两个字节，因此不能直接将char转换为byte，因为':'字符的二进制表示为00111010,
             *   ' '字符的二进制表示为00100000
             * */
            byte[] result = new byte[size + 5];
            result[0] = (byte)(size >>> 8);
            result[1] = (byte)(size >>> 16);
            result[2] = (byte)(size >>> 24);
            result[3] = (byte)(size >>> 32);
            result[4] = (byte)(code.getCode() & 0xFF);
            if(len2 > 0){
                result[5] = (byte)58;
                result[6] = (byte)32;
            }
        }
    }

    byte[] CopyState(byte[] s) {
        return (byte[]) Arrays.copyOf(s,s.length);
    }

    @Override
    public String toString(){
        equivalent()
    }
    public Status(){
        super();
        this.state_ = null;
    }
    public Status(Code code, Slice msg){
        super(code, msg);
    }
    public Status(Status rhs){
        rhs.state_ = null;
    }
    /**
     * Return a success status.
     * */
    public static Status OK() { return new Status(); }

    /**
     * Return error status of an appropriate type.
     * */
    public static Status NotFound(final Slice msg) {
        return new Status(Code.kNotFound, msg);
    }
    public static Status Corruption(final Slice msg) {
        return new Status(Code.kCorruption, msg);
    }
    public static Status NotSupported(final Slice msg) {
        return new Status(Code.kNotSupported, msg);
    }
    public static Status InvalidArgument(final Slice msg) {
        return new Status(Code.kInvalidArgument, msg);
    }
    public static Status IOError(final Slice msg) {
        return new Status(Code.kIOError, msg);
    }
    /**
     * Returns true iff the status indicates success.
     * */
    boolean ok() { return (code() == Code.kOk); }

    /**
     * Returns true iff the status indicates a NotFound error.
     * */
    boolean IsNotFound() { return code() == Code.kNotFound; }

    /**
     * Returns true iff the status indicates a Corruption error.
     * */
    boolean IsCorruption() { return code() == Code.kCorruption; }

    /**
     * Returns true iff the status indicates an IOError.
     * */
    boolean IsIOError() { return code() == Code.kIOError; }

    /**
     * Returns true iff the status indicates a NotSupportedError.
     * */
    boolean IsNotSupportedError() { return code() == Code.kNotSupported; }

    /**
     * Returns true iff the status indicates an InvalidArgument.
     * */
    boolean IsInvalidArgument() { return code() == Code.kInvalidArgument; }

    // Return a string representation of this status suitable for printing.
    // Returns the string "OK" for success.
}


