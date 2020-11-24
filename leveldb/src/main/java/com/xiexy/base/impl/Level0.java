package com.xiexy.base.impl;

import com.xiexy.base.include.Slice;
import com.xiexy.base.table.UserComparator;
import com.xiexy.base.utils.InternalTableIterator;
import com.xiexy.base.utils.Level0Iterator;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static com.xiexy.base.impl.SequenceNumber.MAX_SEQUENCE_NUMBER;
import static com.xiexy.base.impl.ValueType.VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class Level0
        implements SeekingIterable<InternalKey, Slice>
{
    private final TableCache tableCache;
    private final InternalKeyComparator internalKeyComparator;
    private final List<FileMetaData> files;

    public static final Comparator<FileMetaData> NEWEST_FIRST = new Comparator<FileMetaData>()
    {
        @Override
        public int compare(FileMetaData fileMetaData, FileMetaData fileMetaData1)
        {
            return (int) (fileMetaData1.getNumber() - fileMetaData.getNumber());
        }
    };

    public Level0(List<FileMetaData> files, TableCache tableCache, InternalKeyComparator internalKeyComparator)
    {
        requireNonNull(files, "files is null");
        requireNonNull(tableCache, "tableCache is null");
        requireNonNull(internalKeyComparator, "internalKeyComparator is null");

        this.files = new ArrayList<>(files);
        this.tableCache = tableCache;
        this.internalKeyComparator = internalKeyComparator;
    }

    public int getLevelNumber()
    {
        return 0;
    }

    public List<FileMetaData> getFiles()
    {
        return files;
    }

    @Override
    public Level0Iterator iterator()
    {
        return new Level0Iterator(tableCache, files, internalKeyComparator);
    }

    public LookupResult get(LookupKey key, ReadStats readStats)
    {
        if (files.isEmpty()) {
            return null;
        }

        List<FileMetaData> fileMetaDataList = new ArrayList<>(files.size());
        for (FileMetaData fileMetaData : files) {
            if (internalKeyComparator.getUserComparator().compare(key.getUserKey(), fileMetaData.getSmallest().getUserKey()) >= 0 &&
                    internalKeyComparator.getUserComparator().compare(key.getUserKey(), fileMetaData.getLargest().getUserKey()) <= 0) {
                fileMetaDataList.add(fileMetaData);
            }
        }

        Collections.sort(fileMetaDataList, NEWEST_FIRST);

        readStats.clear();
        for (FileMetaData fileMetaData : fileMetaDataList) {
            // open the iterator
            InternalTableIterator iterator = tableCache.newIterator(fileMetaData);

            // seek to the key
            iterator.seek(key.getInternalKey());

            if (iterator.hasNext()) {
                // parse the key in the block
                Map.Entry<InternalKey, Slice> entry = iterator.next();
                InternalKey internalKey = entry.getKey();
                checkState(internalKey != null, "Corrupt key for %s", key.getUserKey().toString(UTF_8));

                // if this is a value key (not a delete) and the keys match, return the value
                if (key.getUserKey().equals(internalKey.getUserKey())) {
                    if (internalKey.getValueType() == ValueType.DELETION) {
                        return LookupResult.deleted(key);
                    }
                    else if (internalKey.getValueType() == VALUE) {
                        return LookupResult.ok(key, entry.getValue());
                    }
                }
            }

            if (readStats.getSeekFile() == null) {
                // We have had more than one seek for this read.  Charge the first file.
                readStats.setSeekFile(fileMetaData);
                readStats.setSeekFileLevel(0);
            }
        }

        return null;
    }

    public boolean someFileOverlapsRange(Slice smallestUserKey, Slice largestUserKey)
    {
        InternalKey smallestInternalKey = new InternalKey(smallestUserKey, MAX_SEQUENCE_NUMBER, VALUE);
        int index = findFile(smallestInternalKey);

        UserComparator userComparator = internalKeyComparator.getUserComparator();
        return ((index < files.size()) &&
                userComparator.compare(largestUserKey, files.get(index).getSmallest().getUserKey()) >= 0);
    }

    private int findFile(InternalKey targetKey)
    {
        if (files.isEmpty()) {
            return files.size();
        }

        // todo replace with Collections.binarySearch
        int left = 0;
        int right = files.size() - 1;

        // binary search restart positions to find the restart position immediately before the targetKey
        while (left < right) {
            int mid = (left + right) / 2;

            if (internalKeyComparator.compare(files.get(mid).getLargest(), targetKey) < 0) {
                // Key at "mid.largest" is < "target".  Therefore all
                // files at or before "mid" are uninteresting.
                left = mid + 1;
            }
            else {
                // Key at "mid.largest" is >= "target".  Therefore all files
                // after "mid" are uninteresting.
                right = mid;
            }
        }
        return right;
    }

    public void addFile(FileMetaData fileMetaData)
    {
        // todo remove mutation
        files.add(fileMetaData);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Level0");
        sb.append("{files=").append(files);
        sb.append('}');
        return sb.toString();
    }
}