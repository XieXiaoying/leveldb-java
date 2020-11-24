package com.xiexy.base.table;

import com.xiexy.base.db.Slices;
import com.xiexy.base.include.Slice;
import com.xiexy.base.utils.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;

import static com.xiexy.base.CompressionType.SNAPPY;

public class FileChannelTable
        extends Table
{
    public FileChannelTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean verifyChecksums)
            throws IOException
    {
        super(name, fileChannel, comparator, verifyChecksums);
    }

    @Override
    protected Footer init()
            throws IOException
    {
        long size = fileChannel.size();
        ByteBuffer footerData = read(size - Footer.ENCODED_LENGTH, Footer.ENCODED_LENGTH);
        return Footer.readFooter(Slices.copiedBuffer(footerData));
    }

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "NonPrivateFieldAccessedInSynchronizedContext"})
    @Override
    protected Block readBlock(BlockHandle blockHandle)
            throws IOException
    {
        // read block trailer
        ByteBuffer trailerData = read(blockHandle.getOffset() + blockHandle.getDataSize(), BlockTrailer.ENCODED_LENGTH);
        BlockTrailer blockTrailer = BlockTrailer.readBlockTrailer(Slices.copiedBuffer(trailerData));

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

        ByteBuffer uncompressedBuffer = read(blockHandle.getOffset(), blockHandle.getDataSize());
        Slice uncompressedData;
        if (blockTrailer.getCompressionType() == SNAPPY) {
            synchronized (FileChannelTable.class) {
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

    private ByteBuffer read(long offset, int length)
            throws IOException
    {
        ByteBuffer uncompressedBuffer = ByteBuffer.allocate(length);
        fileChannel.read(uncompressedBuffer, offset);
        if (uncompressedBuffer.hasRemaining()) {
            throw new IOException("Could not read all the data");
        }
        uncompressedBuffer.clear();
        return uncompressedBuffer;
    }
}