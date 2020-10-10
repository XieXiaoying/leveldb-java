package com.xiexy.base.include;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

public abstract class SliceOutput extends OutputStream
        implements DataOutput {
    /**
     * 设置流的位置为起始位置
     */
    public abstract void reset();

    /**
     * 返回当前buffer的index
     */
    public abstract int size();

    /**
     * 返回可写的byte数量，等于buffer的容量-当前index
     */
    public abstract int writableBytes();

    /**
     * 可写的byte数量大于0.返回true，否则返回false
     */
    public abstract boolean isWritable();

    @Override
    public final void writeBoolean(boolean value)
    {
        writeByte(value ? 1 : 0);
    }

    @Override
    public final void write(int value)
    {
        writeByte(value);
    }

    /**
     * 在指定的位置写入int值，int占4字节，共4 * 8 = 32位，但是一个byte只有8位，所以int的高24位会被忽略
     * 写完之后index+1
     * @throws IndexOutOfBoundsException 如果可写的byte数量 < 1
     */
    @Override
    public abstract void writeByte(int value);

    /**
     * 在指定的位置写入int值，int占4字节，共4 * 8 = 32位，但是一个short只有16位，所以int的高16位会被忽略
     * 写完之后index+2
     * @throws IndexOutOfBoundsException 如果可写的byte数量 < 2
     */
    @Override
    public abstract void writeShort(int value);

    /**
     * 在指定的位置写入int值，int占4字节
     * 写完之后index+4
     * @throws IndexOutOfBoundsException 如果可写的byte数量 < 4
     */
    @Override
    public abstract void writeInt(int value);

    /**
     * 在指定的位置写入long值，long占8字节
     * 写完之后index+8
     * @throws IndexOutOfBoundsException 如果可写的byte数量 < 8
     */
    @Override
    public abstract void writeLong(long value);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(Slice, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(Slice, int, int)}
     * does not.
     *
     * @throws IndexOutOfBoundsException if {@code source.readableBytes} is greater than
     * {@code this.writableBytes}
     */
    public abstract void writeBytes(Slice source);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(Slice, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(Slice, int, int)} does not.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.writableBytes} or
     * if {@code length} is greater then {@code source.readableBytes}
     */
    public abstract void writeBytes(SliceInput source, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param sourceIndex the first index of the source
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code sourceIndex} is less than {@code 0},
     * if {@code sourceIndex + length} is greater than
     * {@code source.capacity}, or
     * if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract void writeBytes(Slice source, int sourceIndex, int length);

    @Override
    public final void write(byte[] source)
            throws IOException
    {
        writeBytes(source);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code source.length}).
     *
     * @throws IndexOutOfBoundsException if {@code source.length} is greater than {@code this.writableBytes}
     */
    public abstract void writeBytes(byte[] source);

    @Override
    public final void write(byte[] source, int sourceIndex, int length)
    {
        writeBytes(source, sourceIndex, length);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param sourceIndex the first index of the source
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code sourceIndex} is less than {@code 0},
     * if {@code sourceIndex + length} is greater than
     * {@code source.length}, or
     * if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract void writeBytes(byte[] source, int sourceIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.
     *
     * @throws IndexOutOfBoundsException if {@code source.remaining()} is greater than
     * {@code this.writableBytes}
     */
    public abstract void writeBytes(ByteBuffer source);

    /**
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified stream
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.writableBytes}
     * @throws java.io.IOException if the specified stream threw an exception during I/O
     */
    public abstract int writeBytes(InputStream in, int length)
            throws IOException;

    /**
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     *
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.writableBytes}
     * @throws java.io.IOException if the specified channel threw an exception during I/O
     */
    public abstract int writeBytes(ScatteringByteChannel in, int length)
            throws IOException;

    public abstract int writeBytes(FileChannel in, int position, int length)
            throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract void writeZero(int length);

    /**
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract Slice slice();

    /**
     * Converts this buffer's readable bytes into a NIO buffer.  The returned
     * buffer might or might not share the content with this buffer, while
     * they have separate indexes and marks.  This method is identical to
     * {@code buf.toByteBuffer(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    public abstract ByteBuffer toByteBuffer();

    /**
     * Decodes this buffer's readable bytes into a string with the specified
     * character set name.  This method is identical to
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws java.nio.charset.UnsupportedCharsetException if the specified character set name is not supported by the
     * current VM
     */
    public abstract String toString(Charset charset);

    //
    // Unsupported operations
    //

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeChar(int value)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeFloat(float v)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeDouble(double v)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeChars(String s)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeUTF(String s)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeBytes(String s)
    {
        throw new UnsupportedOperationException();
    }
}
