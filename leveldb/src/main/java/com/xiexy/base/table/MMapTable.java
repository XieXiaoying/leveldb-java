package com.xiexy.base.table;

import com.xiexy.base.db.Slices;
import com.xiexy.base.include.Slice;
import com.xiexy.base.utils.ByteBufferSupport;
import com.xiexy.base.utils.Closeables;
import com.xiexy.base.utils.Snappy;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.xiexy.base.CompressionType.SNAPPY;

public class MMapTable
        extends Table
{
    private MappedByteBuffer data;

    public MMapTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean verifyChecksums)
            throws IOException
    {
        super(name, fileChannel, comparator, verifyChecksums);
        checkArgument(fileChannel.size() <= Integer.MAX_VALUE, "File must be smaller than %s bytes", Integer.MAX_VALUE);
    }

    @Override
    protected Footer init()
            throws IOException
    {
        long size = fileChannel.size();
        data = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        Slice footerSlice = Slices.copiedBuffer(data, (int) size - Footer.ENCODED_LENGTH, Footer.ENCODED_LENGTH);
        return Footer.readFooter(footerSlice);
    }

    @Override
    public Callable<?> closer()
    {
        return new Closer(name, fileChannel, data);
    }

    private static class Closer
            implements Callable<Void>
    {
        private final String name;
        private final Closeable closeable;
        private final MappedByteBuffer data;

        public Closer(String name, Closeable closeable, MappedByteBuffer data)
        {
            this.name = name;
            this.closeable = closeable;
            this.data = data;
        }

        public Void call()
        {
            ByteBufferSupport.unmap(data);
            Closeables.closeQuietly(closeable);
            return null;
        }
    }

    @SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext", "AssignmentToStaticFieldFromInstanceMethod"})
    @Override
    protected Block readBlock(BlockHandle blockHandle)
            throws IOException
    {
        // read block trailer
        BlockTrailer blockTrailer = BlockTrailer.readBlockTrailer(Slices.copiedBuffer(this.data,
                (int) blockHandle.getOffset() + blockHandle.getDataSize(),
                BlockTrailer.ENCODED_LENGTH));

// todo re-enable crc check when ported to support direct buffers
//        // only verify check sums if explicitly asked by the user
//        if (verifyChecksums) {
//            // checksum data and the compression type in the trailer
//            PureJavaCrc32C checksum = new PureJavaCrc32C();
//            checksum.update(data.getRawArray(), data.getRawOffset(), blockHandle.getDataSize() + 1);
//            int actualCrc32c = checksum.getMaskedValue();
//
//            checkState(blockTrailer.getCrc32c() == actualCrc32c, "Block corrupted: checksum mismatch");
//        }

        // decompress data
        Slice uncompressedData;
        ByteBuffer uncompressedBuffer = read(this.data, (int) blockHandle.getOffset(), blockHandle.getDataSize());
        if (blockTrailer.getCompressionType() == SNAPPY) {
            synchronized (MMapTable.class) {
                int uncompressedLength = uncompressedLength(uncompressedBuffer);
                if (uncompressedScratch.capacity() < uncompressedLength) {
                    uncompressedScratch = ByteBuffer.allocateDirect(uncompressedLength);
                }
                uncompressedScratch.clear();

                Snappy.uncompress(uncompressedBuffer, uncompressedScratch);
                uncompressedData = Slices.copiedBuffer(uncompressedScratch);
            }
        }
        else {
            uncompressedData = Slices.copiedBuffer(uncompressedBuffer);
        }

        return new Block(uncompressedData, comparator);
    }

    public static ByteBuffer read(MappedByteBuffer data, int offset, int length)
            throws IOException
    {
        int newPosition = data.position() + offset;
        ByteBuffer block = (ByteBuffer) data.duplicate().order(ByteOrder.LITTLE_ENDIAN).clear().limit(newPosition + length).position(newPosition);
        return block;
    }
}
