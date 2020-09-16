package com.xiexy.base.include;

public class Status {
    /**
     *  OK status has a null state_.  Otherwise, state_ is a new[] array of the following form:
     *  state_[0..3] == length of message
     *  state_[4]    == code
     *  state_[5..]  == message
     * */
    private byte[] state_;
    private enum Code{
        kOk(0),kNotFound(1),kCorruption(2),kNotSupported(3),kInvalidArgument(4),kIOError(5);
        private int code;
        Code(int code){
            this.code = code;
        }
    }
    Code code(){
        if(this.state_ == null) return Code.kOk;
        for(Code code : Code.values()){
            if(code.code == this.state_[4]) return code;
        }
        return Code.kNotFound;
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


