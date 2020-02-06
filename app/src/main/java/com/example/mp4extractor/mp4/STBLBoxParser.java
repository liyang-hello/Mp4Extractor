package com.example.mp4extractor.mp4;
import com.coremedia.iso.boxes.ChunkOffset64BitBox;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.example.mp4extractor.LogU;

import java.util.List;

/**
 * Description:
 * Created by liyang on 2019/11/28.
 */
public class STBLBoxParser {

    private SampleSizeBox stsz;
    private SampleToChunkBox stsc;
    private ChunkOffsetBox stco;
    private ChunkOffset64BitBox stco2;
    private SyncSampleBox stss;
    private TimeToSampleBox stts;
    private CompositionTimeToSample ccts;

    private boolean mAllSampleSameSize = false;


    public STBLBoxParser(SampleTableBox stbl) {
        getMainChildBox(stbl);
    }

    private void getMainChildBox(SampleTableBox stbl) {
        stsc = stbl.getSampleToChunkBox();
        stsz = stbl.getSampleSizeBox();
        stco = stbl.getChunkOffsetBox();
        List<ChunkOffset64BitBox> coBoxes = stbl.getBoxes(ChunkOffset64BitBox.class);
        if(coBoxes != null && coBoxes.size()>0) {
            stco2 = coBoxes.get(0);
        }
        stss = stbl.getSyncSampleBox();
        stts = stbl.getTimeToSampleBox();
    }

    public int getChunkCount() {
        int count = 0;
        if(stco != null) {
            count = stco.getChunkOffsets().length;
        } else if(stco2 != null) {
            count = stco2.getChunkOffsets().length;
        }
//        LogU.d("chunk count= "+ count);
        return count;
    }

    /**
     *
     * @param index start with 1
     * @return
     */
    public long getChunkOffset(int index) {
        long offset = 0;
        long chunkOffset[] = null;
        if(stco != null) {
            chunkOffset = stco.getChunkOffsets();
        } else if(stco2 != null){
            chunkOffset = stco2.getChunkOffsets();
        }

        if(chunkOffset != null) {
            if(index-1 < chunkOffset.length) {
                offset = chunkOffset[index-1];
            }
        }

        return offset;
    }

    /**
     * --- ISO/IEC 14496-12 ----
     *
     * 8.7.4 Sample To Chunk Box
     * 8.7.4.1 Definition
     * Box Type: ‘stsc’
     * Container: Sample Table Box (‘stbl’)
     * Mandatory: Yes
     * Quantity: Exactly one
     * Samples within the media data are grouped into chunks. Chunks can be of different sizes, and the
     * samples within a chunk can have different sizes. This table can be used to find the chunk that contains a
     * sample, its position, and the associated sample description.
     * The table is compactly coded. Each entry gives the index of the first chunk of a run of chunks with the
     * same characteristics. By subtracting one entry here from the previous one, you can compute how many
     * chunks are in this run. You can convert this to a sample count by multiplying by the appropriate
     * samples‐per‐chunk.
     * 8.7.4.2 Syntax
     * aligned(8) class SampleToChunkBox
     * extends FullBox(‘stsc’, version = 0, 0) {
     * unsigned int(32) entry_count;
     * for (i=1; i <= entry_count; i++) {
     * unsigned int(32) first_chunk;
     * unsigned int(32) samples_per_chunk;
     * unsigned int(32) sample_description_index;
     * }
     * }
     * 8.7.4.3 Semantics
     * version is an integer that specifies the version of this box
     * entry_count is an integer that gives the number of entries in the following table
     * first_chunk is an integer that gives the index of the first chunk in this run of chunks that share
     * the same samples‐per‐chunk and sample‐description‐index; the index of the first chunk in a
     * track has the value 1 (the first_chunk field in the first record of this box has the value 1,
     * identifying that the first sample maps to the first chunk).
     * samples_per_chunk is an integer that gives the number of samples in each of these chunks
     * sample_description_index is an integer that gives the index of the sample entry that
     * describes the samples in this chunk. The index ranges from 1 to the number of sample entries in
     * the Sample Description Box
     *
     * @param index chunk index start with 1;
     * @return
     */
    public long getChunkSize(int index) {
        long size = 0;
        if(stsc != null) {
            List<SampleToChunkBox.Entry> entrys = stsc.getEntries();
//            LogU.d(" stsc entry size "+ entrys.size() + " index "+ index);
            int entryIndex = 0;
            for (; entryIndex<entrys.size(); entryIndex++) {
                if(entrys.get(entryIndex).getFirstChunk() > index) {
                    LogU.d("entryIndex "+entryIndex + " stsc entry "+ entrys.get(entryIndex));
                    break;
                }
            }

            if(entryIndex-1 < entrys.size() && entrys.size()>0) {
                SampleToChunkBox.Entry entry = entrys.get(entryIndex-1);
                getSampleSize(0);
//                LogU.d(" mAllSampleSameSize "+ mAllSampleSameSize + " entryIndex "+ entryIndex);
                if(mAllSampleSameSize) {
                    size = entry.getSamplesPerChunk()*getSampleSize(0);
                } else if(entrys.size() ==1 && entry.getSamplesPerChunk() ==1) {
                    size = getSampleSize(index-1);
                } else {
                    // todo sum all sample`size in this chunk
                    if(entryIndex-1 == 0) {
                        for(int i=0; i<entry.getSamplesPerChunk(); i++){
                            if(i <= index) {
                                size += getSampleSize(i);
                            } else {
                                break;
                            }
                        }
                    } else {
                        int sampleIndex = 0;
                        for(int i=0; i<entryIndex-2; i++) {
                            sampleIndex += entrys.get(i).getSamplesPerChunk();
                        }

                        for(int i=0; i<entrys.get(entryIndex-1).getSamplesPerChunk(); i++) {
                            size += getSampleSize(sampleIndex+i);
                        }
                    }

                }
            } else {
                LogU.e("stsc box is broken ");
            }

        }
//        LogU.d("chunk size "+ size + " index "+ index);
        return size;
    }


    private long getSampleSize(int index) {
        long size = 0;
        if(stsz != null) {
            size = stsz.getSampleSize();
            if(size == 0) {
                long sampleSizes[] = stsz.getSampleSizes();
                if(index < sampleSizes.length) {
                    size = sampleSizes[index];
                }
            } else {
                mAllSampleSameSize = true;
            }
        }
        return size;
    }


    /** --- ISO/IEC 14496-12 ----
     *
     * 8.6.2 Sync Sample Box
     * 8.6.2.1 Definition
     * Box Type: ‘stss’
     * Container: Sample Table Box (‘stbl’)
     * Mandatory: No
     * Quantity: Zero or one
     * This box provides a compact marking of the sync samples within the stream. The table is arranged in
     * strictly increasing order of sample number.
     * If the sync sample box is not present, every sample is a sync sample.
     * 8.6.2.2 Syntax
     * aligned(8) class SyncSampleBox
     * extends FullBox(‘stss’, version = 0, 0) {
     * unsigned int(32) entry_count;
     * int i;
     * for (i=0; i < entry_count; i++) {
     * unsigned int(32) sample_number;
     * }
     * }
     * 8.6.2.3 Semantics
     * version ‐ is an integer that specifies the version of this box.
     * entry_count is an integer that gives the number of entries in the following table. If entry_count
     * is zero, there are no sync samples within the stream and the following table is empty.
     * sample_number gives the numbers of the samples that are sync samples in the stream.
     * @param index
     * @return
     */
    public boolean isKeyFrame(int index) {
        if(stss != null) {
            long sampleNumbers[] = stss.getSampleNumber();
            int ret = binarySearch(sampleNumbers, index);
            if(ret<0) {
                return false;
            }
        }
        return true;
    }

    public int getPreKeyFrameIndex(int frameIndex) {
        int index = 0;
        if(stss != null) {
            int syncFrameIndex = searchPreTargetIndex(stss.getSampleNumber(), frameIndex);
            if(syncFrameIndex >=0) {
                index = (int) stss.getSampleNumber()[syncFrameIndex];
            }
        }
        return index;
    }

    public int getSampleIndexFromChunk(int chunkIndex) {
        int sampleIndex = 0;
        if(stsc != null) {
            List<SampleToChunkBox.Entry> entrys = stsc.getEntries();
            SampleToChunkBox.Entry preEntry = null;
            if(entrys.size() <= 1) {
                sampleIndex = chunkIndex;
            } else {
                preEntry = entrys.get(0);
                long sampleCount = 0;
                for (int i=1; i<entrys.size(); i++) {
                    SampleToChunkBox.Entry entry = entrys.get(i);
                    if(entry.getFirstChunk()<chunkIndex) {
                        sampleCount += (entry.getFirstChunk()-preEntry.getFirstChunk())*preEntry.getSamplesPerChunk();
                    } else {
                        sampleCount += (chunkIndex-preEntry.getFirstChunk())*preEntry.getSamplesPerChunk();
                        break;
                    }
                    preEntry = entry;

                }
                sampleIndex = (int) sampleCount;
            }

        }
        return sampleIndex;
    }

    public long getSampleCount() {
        long sampleCount = 0;
        if(stsz != null) {
            sampleCount = stsz.getSampleCount();
        }
        return sampleCount;
    }

    public long getFrameDelta() {
        long deltaTime = 0;
        if(stts != null) {
            deltaTime = stts.getEntries().get(0).getDelta();
        }
        return deltaTime;
    }

    /**
     *
     * @param array
     * @param valueToFind
     * @return -1  ---  valueToFind not in array
     *         >=0 ---  the index of valueToFind in array;
     */
    private static int binarySearch(long[] array, long valueToFind) {
        if(array.length <1) {
            return -1;
        }

        int low = 0;
        int high = array.length - 1;
        int middle = 0;

        while (low <= high) {
            middle = (low + high) / 2;
            if (array[middle] > valueToFind) {
                high = middle - 1;
            } else if (array[middle] < valueToFind) {
                low = middle + 1;
            } else {
                return middle;
            }
        }

        return -1;
    }


    /**
     *
     * @param array
     * @param valueToFind
     * @return -1  ---  valueToFind not in array
     *         >=0 ---  the index of valueToFind in array;
     */
    private static int searchPreTargetIndex(long[] array, long valueToFind) {
        if(array.length <1) {
            return -1;
        }

        int low = 0;
        int high = array.length - 1;
        int middle = 0;

        while (low <= high) {
            middle = (low + high) / 2;
            if (array[middle] > valueToFind) {
                high = middle - 1;
            } else if (array[middle] < valueToFind) {
                low = middle + 1;
            } else {
                return middle;
            }
        }

        return low;
    }

}
