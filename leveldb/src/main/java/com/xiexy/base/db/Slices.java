package com.xiexy.base.db;

import com.xiexy.base.include.Slice;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.IdentityHashMap;
import java.util.Map;

public class Slices {
    public static final Slice EMPTY_SLICE = new Slice(0);

    private static final ThreadLocal<Map<Charset, CharsetDecoder>> decoders =
            new ThreadLocal<Map<Charset, CharsetDecoder>>()
            {
                @Override
                protected Map<Charset, CharsetDecoder> initialValue()
                {
                    return new IdentityHashMap<>();
                }
            };

    private static final ThreadLocal<Map<Charset, CharsetEncoder>> encoders =
            new ThreadLocal<Map<Charset, CharsetEncoder>>()
            {
                @Override
                protected Map<Charset, CharsetEncoder> initialValue()
                {
                    return new IdentityHashMap<>();
                }
            };

    public static String decodeString(ByteBuffer src, Charset charset)
    {
        CharsetDecoder decoder = getDecoder(charset);
        /** maxCharsPerByte()方法是java.nio.charset.CharsetDecoder类的内置方法，该方法返回为每个输入字节生成的最大字符数。
         * 该值可用于计算给定输入序列所需的输出缓冲区的最坏情况大小。
         *
         * 缓存区的实现原理：https://blog.csdn.net/u010659877/article/details/108864125
         * allocate是内部创建数组，wrap是通过传入外部数组创建
         * decode实现原理：https://blog.csdn.net/u010659877/article/details/109192298
         * */
        CharBuffer dst = CharBuffer.allocate(
                (int) ((double) src.remaining() * decoder.maxCharsPerByte()));
        try {
            CoderResult cr = decoder.decode(src, dst, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = decoder.flush(dst);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        }
        catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
        return dst.flip().toString();
    }


    /**
     * 根据指定的<tt>charset</tt>返回一个缓存的threadlocal {@link CharsetDecoder}
     */
    private static CharsetDecoder getDecoder(Charset charset)
    {
        if (charset == null) {
            throw new NullPointerException("charset");
        }

        Map<Charset, CharsetDecoder> map = decoders.get();
        CharsetDecoder d = map.get(charset);
        if (d != null) {
            // 将CharsetDecoder的state设为0
            d.reset();
            // 对错误输入的操作使用CodingErrorAction.REPLACE
            d.onMalformedInput(CodingErrorAction.REPLACE);
            // 对不可映射的字符错误的操作使用CodingErrorAction.REPLACE
            d.onUnmappableCharacter(CodingErrorAction.REPLACE);
            return d;
        }

        d = charset.newDecoder();
        d.onMalformedInput(CodingErrorAction.REPLACE);
        d.onUnmappableCharacter(CodingErrorAction.REPLACE);
        map.put(charset, d);
        return d;
    }

    public static ByteBuffer encodeString(CharBuffer src, Charset charset)
    {
        CharsetEncoder encoder = getEncoder(charset);
        ByteBuffer dst = ByteBuffer.allocate(
                (int) ((double) src.remaining() * encoder.maxBytesPerChar()));
        try {
            CoderResult cr = encoder.encode(src, dst, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = encoder.flush(dst);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        }
        catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
        dst.flip();
        return dst;
    }

    /**
     * 根据指定的<tt>charset</tt>返回一个缓存的threadlocal {@link CharsetEncoder}
     */
    private static CharsetEncoder getEncoder(Charset charset)
    {
        if (charset == null) {
            throw new NullPointerException("charset");
        }

        Map<Charset, CharsetEncoder> map = encoders.get();
        CharsetEncoder e = map.get(charset);
        if (e != null) {
            e.reset();
            e.onMalformedInput(CodingErrorAction.REPLACE);
            e.onUnmappableCharacter(CodingErrorAction.REPLACE);
            return e;
        }

        e = charset.newEncoder();
        e.onMalformedInput(CodingErrorAction.REPLACE);
        e.onUnmappableCharacter(CodingErrorAction.REPLACE);
        map.put(charset, e);
        return e;
    }


}
