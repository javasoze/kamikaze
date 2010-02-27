package com.kamikaze.docidset.compression;

import java.util.BitSet;

import org.apache.lucene.util.OpenBitSet;

public interface CompressedSortedIntegerSegment {

  public OpenBitSet compress(int[] inputSet) throws IllegalArgumentException;

  public long[] compressAlt(int[] inputSet) throws IllegalArgumentException;
  
  public int[] decompress(BitSet packedSet) throws IllegalArgumentException;

  public int[] decompress(OpenBitSet packedSet);

}
