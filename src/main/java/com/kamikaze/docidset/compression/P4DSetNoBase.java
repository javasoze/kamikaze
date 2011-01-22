package com.kamikaze.docidset.compression;

import java.io.Serializable;
import java.util.BitSet;

import org.apache.lucene.util.OpenBitSet;

import com.kamikaze.docidset.bitset.MyOpenBitSet;

/**
 * Implementation of the p4delta algorithm for sorted integer arrays based on
 * 
 * 1. Original Algorithm from
 * http://homepages.cwi.nl/~heman/downloads/msthesis.pdf 2. Optimization and
 * variation from http://www2008.org/papers/pdf/p387-zhangA.pdf
 * 
 * This class is a wrapper around a CompressedSegment based on Lucene OpenBitSet
 */
public class P4DSetNoBase implements CompressedSortedIntegerSegment,
    Serializable {

  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;

  // Maximum bits that can be used = 32

  // Byte Mask
  private static final int BYTE_MASK = 8;

  // 32 bits for retaining base value
  private static final int BASE_MASK = 32;

  // Header size
  private static final int HEADER_MASK = BYTE_MASK;

  // Parameters for the compressed set
  private int _b = INVALID;

  private int _base = INVALID;

  private int _batchSize = INVALID;

  private int _exceptionCount = INVALID;

  private int _exceptionOffset = INVALID;

  //private int[] op = null;

  
  interface Processor extends Serializable
  {
    public int process(int retval, int exceptionOffset, long[] compressedSet);
  };
  
  
  private static final Processor valueproc[] = 
    
  {
    new Processor() {
      public final int process(int retVal, int exceptionOffset, long[] compressedSet){
        return retVal;
      }
      
    },
    
    new Processor(){
    
      public final int process(int retVal, int exceptionOffset, long[] compressedSet){
        // Get the actual value
        return getBitSlice(compressedSet, exceptionOffset + retVal * BASE_MASK, BASE_MASK);
      }
      
    }
    
  };
  
  
  public void setParam(int base, int b, int batchSize, int exceptionCount) {
    this._base = base;
    this._b = b;
    this._batchSize = batchSize;
   
    this._exceptionCount = exceptionCount;
    this._exceptionOffset = HEADER_MASK + _b * _batchSize;
    

  }

  public void updateParams(MyOpenBitSet set) {
    _b = getBitSlice(set, 0, BYTE_MASK);
   
    _exceptionOffset = HEADER_MASK + _b * _batchSize;
  }
  
  public void updateParams(long[] set) {
 
    _b = getBitSlice(set, 0, BYTE_MASK);
  
    _exceptionOffset = HEADER_MASK + _b * _batchSize;
  }

 
  /**
   * Alternate implementation for compress
   * 
   * @param input
   * @return compressed bitset
   * @throws IllegalArgumentException
   */
  public OpenBitSet compress(int[] input) throws IllegalArgumentException {

    if (_base == INVALID || _b == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");


    
    int BATCH_MAX = 1 << (_b - 1);
    // int validCount = (_batchSize - _exceptionCount)*_b +SIZE_MASK+BASE_MASK;

    // Compression mumbo jumbo

    // Set Size -b+base+compressedSet+exception*BASE_MASK
    
    MyOpenBitSet compressedSet = new MyOpenBitSet((_batchSize) * _b
        + HEADER_MASK + _exceptionCount * (BASE_MASK));
    
    // System.out.println("Compressed Set Size : " + compressedSet.capacity());

    
    // Load the b
    copyBits(compressedSet, _b, 0, BYTE_MASK);

    // copy the base value to BASE_MASK offset
    // copyBits(compressedSet, _base, BYTE_MASK, BASE_MASK);

    // Offset is the offset of the next location to place the value
    int offset = HEADER_MASK;
    int exceptionOffset = _exceptionOffset;
    int exceptionIndex = 0;

    // 1. Walk the list
    // TODO : Optimize this process.
    for (int i = 0; i < _batchSize; i++) {
      // else copy in the end
      if (input[i] < BATCH_MAX) {
        copyBits(compressedSet, input[i] << 1, offset, _b);

      } else {
        // Copy the value to the exception location
        // Add a bit marker to place
        copyBits(compressedSet, ((exceptionIndex << 1) | 0x1), offset, _b);
        // System.out.println("Adding Exception
        // Marker:"+(BATCH_MAX|(exceptionIndex-1)) + " at offset:"+offset);

        // Copy the patch value to patch offset location
        copyBits(compressedSet, input[i], exceptionOffset, BASE_MASK);

        // reset exceptionDelta
        exceptionOffset += BASE_MASK;
        exceptionIndex++;
      }

      offset += _b;
    }

    return compressedSet;
  }
  
  /**
   * Alternate implementation for compress
   * 
   * @param input
   * @return comprssed set in long array form
   * @throws IllegalArgumentException
   */
  public long[] compressAlt(int[] input) throws IllegalArgumentException {

    if (_base == INVALID || _b == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");

    
    /*for(int i=0;i<_batchSize;i++)
      System.out.print(input[i]+":");
    System.out.println("\nB:"+_b)*/
    
       
    int BATCH_MAX = 1 << (_b - 1);
    // int validCount = (_batchSize - _exceptionCount)*_b +SIZE_MASK+BASE_MASK;

    // Compression mumbo jumbo

    // Set Size _b+base+compressedSet+exception*BASE_MASK bits
       long[] compressedSet = new long[((((_batchSize) * _b  + HEADER_MASK + _exceptionCount * (BASE_MASK)))>>>6)+1];
      
      
      //new long[((_batchSize) * _b  + HEADER_MASK + _exceptionCount * (BASE_MASK))>>>6 + 1];
    // System.out.println("Compressed Set Size : " + compressedSet.capacity());

    
    // Load the b
    copyBits(compressedSet, _b, 0, BYTE_MASK);

    // copy the base value to BASE_MASK offset
    // copyBits(compressedSet, _base, BYTE_MASK, BASE_MASK);

    // Offset is the offset of the next location to place the value
    int offset = HEADER_MASK;
    int exceptionOffset = _exceptionOffset;
    int exceptionIndex = 0;

    // 1. Walk the list
    // TODO : Optimize this process.
    for (int i = 0; i < _batchSize; i++) {
      // else copy in the end
      if (input[i] < BATCH_MAX) {
        copyBits(compressedSet, input[i] << 1, offset, _b);

      } else {
        // Copy the value to the exception location
        // Add a bit marker to place
        copyBits(compressedSet, ((exceptionIndex << 1) | 0x1), offset, _b);
        // System.out.println("Adding Exception
        // Marker:"+(BATCH_MAX|(exceptionIndex-1)) + " at offset:"+offset);

        // Copy the patch value to patch offset location
        copyBits(compressedSet, input[i], exceptionOffset, BASE_MASK);

        // reset exceptionDelta
        exceptionOffset += BASE_MASK;
        exceptionIndex++;
      }

      offset += _b;
    }

    return compressedSet;
  }

  static private void copyBits(MyOpenBitSet compressedSet, int val, int offset, int length) {
    final long[] bits = compressedSet.getBits();
    final int index = offset >>> 6;
    final int skip = offset & 0x3f;
    val &= (0xffffffff >>> (32 - length));
    bits[index] |= (((long)val) << skip);
    if (64 - skip < length) {
      bits[index + 1] |= ((long)val >>> (64 - skip));
    }
    
  }
  
  static private void copyBits(long[] bits, int val, int offset, int length) {
    
    final int index = offset >>> 6;
    final int skip = offset & 0x3f;
    val &= (0xffffffff >>> (32 - length));
    bits[index] |= (((long)val) << skip);
    if (64 - skip < length) {
      bits[index + 1] |= ((long)val >>> (64 - skip));
    }
    
  }
  
  static private int getBitSlice(OpenBitSet compressedSet, final int offset, final int length) {
    final long[] bits = compressedSet.getBits();
    final int index = offset >>> 6;
    final int skip = offset & 0x3f;
    int val = (int)(bits[index] >>> skip);
    if (64 - skip < length) {
      val |= (int)bits[index + 1] << (64 - skip);
    }
    return val & (0xffffffff >>> (32 - length));
  }
  
  static private int getBitSlice(long[] bits, final int offset, final int length) {
    
    final int index = offset >>> 6;
    final int skip = offset & 0x3f;
    int val = (int)(bits[index] >>> skip);
    if (64 - skip < length) {
      val |= (int)bits[index + 1] << (64 - skip);
    }
    return val & (0xffffffff >>> (32 - length));
  }
  
  
  // Method to allow iteration in decompressed form
  public final int get(long[] compressedSet, int index) {
    final int retVal = getBitSlice(compressedSet, (index * _b + HEADER_MASK), _b);
       
    // fake the function pointer logic
    return valueproc[retVal & 0x1].process(retVal >>> 1, _exceptionOffset, compressedSet);

  }
  
  /* Method to allow iteration in decompressed form
  public int get(OpenBitSet compressedSet, int index) {
    final int retVal = getBitSlice(compressedSet, (index * _b + HEADER_MASK), _b);
       
    // fake the function pointer logic
    return valueproc[retVal & 0x1].process(retVal >>> 1, _exceptionOffset, compressedSet);
    
    
   /*This is an exception
   if (compressedSet.getBit((index + 1) * _b + HEADER_MASK - 1) == 1) {

      int exOffset = _exceptionOffset + retVal * BASE_MASK;
      retVal = 0;
      // Get the actual value
      for (int j = 0; j < BASE_MASK; j++)
        retVal |= (compressedSet.getBit(exOffset + j) << j);
      return retVal;
    } 
    else
      return retVal;
  }*/

  public int[] decompress(OpenBitSet compressedSet) {
   
    int[] op = new int[_batchSize];
    // reuse o/p
    op[0] = _base;

    // Offset of the exception list
    int exceptionOffset = HEADER_MASK + _b * _batchSize;

    // explode and patch
    for (int i = 1; i < _batchSize; i++) {
      int val = getBitSlice(compressedSet, i * _b + HEADER_MASK, _b);
      
      if ((val & 0x1) != 0) {
        // This is an exception
        op[i] = getBitSlice(compressedSet, exceptionOffset, BASE_MASK);
        exceptionOffset += BASE_MASK;
      } else {
        op[i] = val >>> 1;
      }
      op[i] += op[i - 1];
    }
    return op;
  }
  
  public int[] decompress(long[] compressedSet) {
    int[] op  = new int[_batchSize];
    // reuse o/p
    op[0] = _base;

    // Offset of the exception list
    int exceptionOffset = HEADER_MASK + _b * _batchSize;

    // explode and patch
    for (int i = 1; i < _batchSize; i++) {
      int val = getBitSlice(compressedSet, i * _b + HEADER_MASK, _b);
      
      if ((val & 0x1) != 0) {
        // This is an exception
        op[i] = getBitSlice(compressedSet, exceptionOffset, BASE_MASK);
        exceptionOffset += BASE_MASK;
      } else {
        op[i] = val >>> 1;
      }
      op[i] += op[i - 1];
    }
    return op;
  }

  /**
   * Method not supported
   * 
   */
  public int[] decompress(BitSet compressedSet) throws IllegalArgumentException {
    return null;
  }

  public String printParams() {
    return "b val:" + _b + " exceptionOffset:" + _exceptionOffset;
  }





}
