package com.kamikaze.docidset.compression;

import java.util.BitSet;

import org.apache.lucene.util.OpenBitSet;
import com.kamikaze.docidset.utils.CompResult;

public interface PForDeltaCompressedSortedIntegerSegment {
  
  public CompResult compressOneBlock(int[] inputBlock, int bits, int blockSize, boolean flag) throws IllegalArgumentException;
}
