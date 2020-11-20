/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiexy.base.impl;

import com.xiexy.base.include.Slice;

public class VersionSet{
//        implements SeekingIterable<InternalKey, Slice>
//{
    private static final int L0_COMPACTION_TRIGGER = 4;

    public static final int TARGET_FILE_SIZE = 2 * 1048576;
    private long manifestFileNumber = 1;

}
