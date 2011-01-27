package com.kamikaze.docidset.compression;

import java.io.Serializable;
import com.kamikaze.docidset.utils.CompResult;
import com.kamikaze.pfordelta.PForDelta;

/**
 * Wrapper of PForDelta class. This class is used to compress/decompress data blocks of integers
 *  
 * @author hao yan
 */
public class PForDeltaWithBase implements PForDeltaCompressedSortedIntegerSegment, Serializable {

  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;  

  // Max number of  bits to store an uncompressed value
  private int _compressedBitSize = 0;  // The compressed size in bits of the block 
  
  /**
   * Get the compressed size in bits of the block 
   * @return the compressed size in bits of the block 
   */
  public int getCompressedBitSize()
  {
     return _compressedBitSize;
  }  
  
  /**
   * Estimate the compressed size of a block 
   * 
   * @param inputBlock a block of non-negative integers to be compressed
   * @param bits the value of b in the PForDelta algorithm
   * @param blockSize the block size which is 256 by default
   * @return CompResult 
   * @throws IllegalArgumentException
   */
  public int estimateCompSize(int[] inputBlock, int bits, int blockSize) throws IllegalArgumentException {
    return PForDelta.estimateCompressedSize(inputBlock, bits, blockSize);
  }

  @Override
  public CompResult compressOneBlock(int[] inputBlock, int bits, int blockSize, boolean flag) throws IllegalArgumentException {
    return compressOneBlock(inputBlock, bits, blockSize);
  }
  
  /**
   * Compress an integer array
   * 
   * @param inputBlock the integer input array
   * @param bits the value of b in the PForDelta algorithm
   * @param blockSize the block size which is 256 by default
   * @return CompResult which contains the compressed size in number of bits and the reference to the compressed data
   * @throws IllegalArgumentException
   */
  public CompResult compressOneBlock(int[] inputBlock, int bits, int blockSize) throws IllegalArgumentException {
    int[] compBlock = PForDelta.compressOneBlock(inputBlock, bits, blockSize);
    CompResult res = new CompResult();
    res.setCompressedSize(compBlock.length<<5);
    res.setCompressedBlock(compBlock);
    return res;
  }
  
  /**
   * Decompress a compressed block into an integer array
   * 
   * @param compBlock the compressed input block
   * @param blockSize the block size which is 256 by default 
   * @return the decompressed output block 
   */  
  public int decompressOneBlock(int[] decompBlock, int[] compBlock, int blockSize)
  {
    return PForDelta.decompressOneBlock(decompBlock, compBlock, blockSize);
  }
}
