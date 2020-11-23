package com.xiexy.base.table;

import com.xiexy.base.db.Slices;
import com.xiexy.base.include.Slice;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class BlockTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBuffer()
            throws Exception
    {
        new Block(Slices.EMPTY_SLICE, new BytewiseComparator());
    }

    @Test
    public void testEmptyBlock()
            throws Exception
    {
        blockTest(Integer.MAX_VALUE);
    }

    @Test
    public void testSingleEntry()
            throws Exception
    {
        blockTest(Integer.MAX_VALUE,
                BlockHelper.createBlockEntry("name", "dain sundstrom"));
    }

    @Test
    public void testMultipleEntriesWithNonSharedKey()
            throws Exception
    {
        blockTest(Integer.MAX_VALUE,
                BlockHelper.createBlockEntry("beer", "Lagunitas IPA"),
                BlockHelper.createBlockEntry("scotch", "Highland Park"));
    }

    @Test
    public void testMultipleEntriesWithSharedKey()
            throws Exception
    {
        blockTest(Integer.MAX_VALUE,
                BlockHelper.createBlockEntry("beer/ale", "Lagunitas  Little Sumpin’ Sumpin’"),
                BlockHelper.createBlockEntry("beer/ipa", "Lagunitas IPA"),
                BlockHelper.createBlockEntry("scotch", "Highland Park"));
    }

    @Test
    public void testMultipleEntriesWithNonSharedKeyAndRestartPositions()
            throws Exception
    {
        List<BlockEntry> entries = asList(
                BlockHelper.createBlockEntry("ale", "Lagunitas  Little Sumpin’ Sumpin’"),
                BlockHelper.createBlockEntry("ipa", "Lagunitas IPA"),
                BlockHelper.createBlockEntry("stout", "Lagunitas Imperial Stout"),
                BlockHelper.createBlockEntry("strong", "Lagavulin"));

        for (int i = 1; i < entries.size(); i++) {
            blockTest(i, entries);
        }
    }

    @Test
    public void testMultipleEntriesWithSharedKeyAndRestartPositions()
            throws Exception
    {
        List<BlockEntry> entries = asList(
                BlockHelper.createBlockEntry("beer/ale", "Lagunitas  Little Sumpin’ Sumpin’"),
                BlockHelper.createBlockEntry("beer/ipa", "Lagunitas IPA"),
                BlockHelper.createBlockEntry("beer/stout", "Lagunitas Imperial Stout"),
                BlockHelper.createBlockEntry("scotch/light", "Oban 14"),
                BlockHelper.createBlockEntry("scotch/medium", "Highland Park"),
                BlockHelper.createBlockEntry("scotch/strong", "Lagavulin"));

        for (int i = 1; i < entries.size(); i++) {
            blockTest(i, entries);
        }
    }

    private static void blockTest(int blockRestartInterval, BlockEntry... entries)
    {
        blockTest(blockRestartInterval, asList(entries));
    }

    private static void blockTest(int blockRestartInterval, List<BlockEntry> entries)
    {
        BlockBuilder builder = new BlockBuilder(256, blockRestartInterval, new BytewiseComparator());

        for (BlockEntry entry : entries) {
            builder.add(entry);
        }

        assertEquals(builder.currentSizeEstimate(), BlockHelper.estimateBlockSize(blockRestartInterval, entries));
        Slice blockSlice = builder.finish();
        assertEquals(builder.currentSizeEstimate(), BlockHelper.estimateBlockSize(blockRestartInterval, entries));

        Block block = new Block(blockSlice, new BytewiseComparator());
        assertEquals(block.size(), BlockHelper.estimateBlockSize(blockRestartInterval, entries));

        BlockIterator blockIterator = block.iterator();
        BlockHelper.assertSequence(blockIterator, entries);

        blockIterator.seekToFirst();
        BlockHelper.assertSequence(blockIterator, entries);

        for (BlockEntry entry : entries) {
            List<BlockEntry> nextEntries = entries.subList(entries.indexOf(entry), entries.size());
            blockIterator.seek(entry.getKey());
            BlockHelper.assertSequence(blockIterator, nextEntries);

            blockIterator.seek(BlockHelper.before(entry));
            BlockHelper.assertSequence(blockIterator, nextEntries);

            blockIterator.seek(BlockHelper.after(entry));
            BlockHelper.assertSequence(blockIterator, nextEntries.subList(1, nextEntries.size()));
        }

        blockIterator.seek(Slices.wrappedBuffer(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
        BlockHelper.assertSequence(blockIterator, Collections.<BlockEntry>emptyList());
    }
}
