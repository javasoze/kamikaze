package com.kamikaze.docidset.compression;

import java.io.Serializable;
import java.util.Arrays;
import com.kamikaze.docidset.utils.CompResult;
import com.kamikaze.docidset.compression.PForDeltaUnpack;

/**
 * Implementation of the optimized PForDelta algorithm for sorted integer arrays. The basic ideas are based on
 * 
 * 1. Original Algorithm from
 * http://homepages.cwi.nl/~heman/downloads/msthesis.pdf 
 * 
 * 2. Optimization and
 * variation from http://www2008.org/papers/pdf/p387-zhangA.pdf 
 * 
 * 3. Further optimization
 * http://www2009.org/proceedings/pdf/p401.pdf
 *  
 * @author hao yan
 */
public class PForDeltaWithBase implements PForDeltaCompressedSortedIntegerSegment, Serializable {

  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;  

  // Max number of  bits to store an uncompressed value
  private static final int MAX_BITS = 32;

  // Header number. The header of each block contains 2 integers, the first one stores the parameters to decompress the block;
  // the second integer stores the last (uncompressed) docId of the block to speed up query processing by the skipping techniques.
  private static final int HEADER_NUM = 2;
  
  // Header size
  private static final int HEADER_SIZE = MAX_BITS * HEADER_NUM;
 
  private int _compressedBitSize = 0;  // The compressed size in bits of the block 
  
  /**
   * Get the compressed size in bits of the block 
   * 
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
   * @return CompResult 
   * @throws IllegalArgumentException
   */
  public CompResult estimateCompSize(int[] inputBlock, int bits, int blockSize) throws IllegalArgumentException {
    // The header and the bits-bit slots have the deterministic size. However, the compressed size for expPos and expHighBits are estimated.
    if (bits == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");        
   
    int maxNoExp = (1<<bits)-1;
    int outputOffset = HEADER_SIZE + bits * blockSize; //  Size of the header and the bits-bit slots
    int expNum = 0;      
    
    for (int i = 0; i<blockSize; ++i)
    {      
      if (inputBlock[i] > maxNoExp) 
      {
        expNum++;
      }
    }    
    outputOffset += (expNum<<5);
    CompResult compRes = new CompResult();
    compRes.setUsingFixedBitCoding(false);
    compRes.setCompressedSize(outputOffset);
      
    return compRes;
  }

  /**
   * Compress an integer array
   * 
   * @param inputBlock the integer input array
   * @param bits the value of b in the PForDelta algorithm
   * @param blockSize the block size which is 256 by default
   * @param flagFixedBitExpPos true if fixed bit size is used to encode exception positions
   * @return CompResult which contains the compressed size in number of bits and the reference to the compressed data
   * @throws IllegalArgumentException
   */
  public CompResult compressOneBlock(int[] inputBlock, int bits, int blockSize, boolean flagFixedBitExpPos) throws IllegalArgumentException {
    // hy: compress a sequence of gaps except the first element (which is the original docId) 
    if (bits == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");        
   
    int[] expAux = new int[blockSize * 2];
    
    int maxNoExp = 1<<bits;
   
    int maxCompBitSize =  HEADER_SIZE + blockSize * (bits  + MAX_BITS + MAX_BITS) + 32;
    int[] compressedBlock = new int[(maxCompBitSize>>>5)];
    
    // The second HEADER: the last (uncompressed) docId of the block 
    compressedBlock[1] = inputBlock[blockSize-1];
    
    int outputOffset = HEADER_SIZE;
    int expNum = 0;      
    
    for (int i = 0; i<blockSize; ++i)
    {      
      if (inputBlock[i] < maxNoExp) 
      {
        writeBits(compressedBlock, inputBlock[i], outputOffset, bits);
      } 
      else // exp
      {
        writeBits(compressedBlock, inputBlock[i] & MASK[bits], outputOffset, bits); // store the lower bits-bits of the exception
        expAux[expNum] = i; // write the position of exception
        expAux[expNum+blockSize] = (inputBlock[i] >>> bits) & MASK[32-bits];   // write the higher 32-bits bits of the exception
        
        expNum++;
      }
      outputOffset += bits;
    }    
    
    // The first HEADER stores:  flagFixedBitExpPos | bits | expNum, assuming expNum < 2^10 and bits<2^10
    if(flagFixedBitExpPos) // using fixed bits to encode expPos
    {
      compressedBlock[0] = (1<<20) | ((bits & MASK[10]) << 10) | (expNum & 0x3ff);
    }
    else
    {
      compressedBlock[0] = ((bits & MASK[10]) << 10) | (expNum & 0x3ff);
    }
    
    int compressedBitSize;
    if(expNum>0)
    {
      System.arraycopy(expAux, blockSize, expAux, expNum, expNum);
      compressedBitSize = compressBlockByS16(compressedBlock, outputOffset, expAux, expNum * 2);
      outputOffset += compressedBitSize;
    }
    
    _compressedBitSize = outputOffset;
    
    int[] sealedCompBlock = new int[(outputOffset+31)>>>5];
    System.arraycopy(compressedBlock,0, sealedCompBlock, 0, (outputOffset+31)>>>5);
    
    CompResult compRes = new CompResult();
    compRes.setUsingFixedBitCoding(flagFixedBitExpPos);
    compRes.setCompressedBlock(sealedCompBlock);
    compRes.setCompressedSize(outputOffset);
    
    return compRes;
  }
  
  /**
   * Decompress a compressed block into an integer array
   * 
   * @param outDecompBlock the decompressed output block
   * @param compBlock the compressed input block
   * @param blockSize the block size which is 256 by default 
   * @return the processed data size (the number of bits in the compressed form)
   */  
  public int decompressOneBlock(int[] outDecompBlock, int[] compBlock, int blockSize)
  {
    int[] expAux = new int[blockSize * 2];
    
    // first decompress the flagFixedBitExpPos, bits and expNum
    if(compBlock == null)
    {
      System.out.println("compBlock is null");
      return 0;
    }
    int expNum = compBlock[0] & 0x3ff; 
    int bits = (compBlock[0]>>>10) & (0x1f);    
    boolean flagFixedBitExpPos = ((compBlock[0]>>>20) & (0x1)) > 0 ? true : false;
    
    // decompress the b-bit slots
    int offset = HEADER_SIZE;
    int compressedBits = 0;
    if(bits == 0)
    {
      Arrays.fill(outDecompBlock,0);
    }
    else
    {
      //compressedBits = decompressBBitSlots(outDecompBlock, compBlock, blockSize, bits);
      compressedBits = decompressBBitSlotsWithHardCodes(outDecompBlock, compBlock, blockSize, bits);
    }
    offset += compressedBits;
    
    // decompress the expPos and expHighBits and assemble them with the above b-bit slots values into the final results
    if(expNum>0)
    {
      compressedBits = decompressBlockByS16(expAux, compBlock, expNum*2, offset);
      offset += compressedBits;

      int i=0;
      int curExpPos;
      int curHighBits;

      for (i = 0; i < expNum; i++) 
      { 
        curExpPos = expAux[i]  ;
        curHighBits = expAux[i+expNum];
        outDecompBlock[curExpPos] = (outDecompBlock[curExpPos] & MASK[bits]) | ((curHighBits & MASK[32-bits] ) << bits);
      }
    }
    return offset;
  }
  
  /**
   * Decompress the b-bit slots
   * 
   * @param decompressedSlots the decompressed output 
   * @param compBlock the compressed input block
   * @param blockSize the block size which is 256 by default 
   * @param bits the value of b
   * @return the processed data size (the number of bits in the compressed form)
   */ 
  private int decompressBBitSlots(int[] decompressedSlots, int[] compBlock, int blockSize, int bits)
  {
    int compressedBitSize = 0;
    int offset = HEADER_SIZE;
    for(int i =0; i<blockSize; i++)
    {
      decompressedSlots[i] = readBits(compBlock, offset, bits);
      offset += bits;
    }
    compressedBitSize = bits * blockSize;
    
    return compressedBitSize;    
  } 
  
  /**
   * Decompress the b-bit slots using hardcoded unpack methods
   * 
   * @param decompressedSlots the decompressed output 
   * @param compBlock the compressed input block
   * @param blockSize the block size which is 256 by default 
   * @param bits the value of b
   * @return the processed data size (the number of bits in the compressed form)
   */ 
  private int decompressBBitSlotsWithHardCodes(int[] decompressedSlots, int[] compBlock, int blockSize, int bits)
  {
    int compressedBitSize = 0;
    PForDeltaUnpack.unpack(decompressedSlots, compBlock, bits);
    compressedBitSize = bits * blockSize;
    
    return compressedBitSize;    
  } 
  
  /**
   * Compress an integer array using Simple16 (used to compress positions and highBits of exceptions)
   * 
   * @param outCompBlock the compressed output 
   * @param offset the bit offset in the compressed input block
   * @param inBlock the compressed input block 
   * @param expsize the number of exceptions
   * @return the compressed data size (the number of bits in the compressed form)
   */ 
  private int compressBlockByS16(int[] outCompBlock, int offset, int[] inBlock, int expSize)
  {
    int outOffset  = (offset+31)>>>5; 
    int num, inOffset=0, numLeft;
    for(numLeft=expSize; numLeft>0; numLeft -= num)
    {
       num = s16Compress(outCompBlock, outOffset, inBlock, inOffset, numLeft);
       outOffset++;
       inOffset += num;
    }
    
    int compressedBitSize = (outOffset<<5)-offset;
    return compressedBitSize;    
  }
  
  /**
   * Decompress an integer array using Simple16 (used to decompress positions and highBits of exceptions)
   * 
   * @param outDecompBlock the decompressed output 
   * @param inCompBlock the compressed input block 
   * @param expsize the number of exceptions
   * @param offset the bit offset in the compressed input block
   * @return the processed data size (the number of bits in the compressed form)
   */ 
  private int decompressBlockByS16(int[] outDecompBlock, int[] inCompBlock, int expSize, int offset)
  {    
    int inOffset  = (offset+31)>>>5;
    int num, outOffset=0, numLeft;
    for(numLeft=expSize; numLeft>0; numLeft -= num)
    {
       num = s16Decompress(outDecompBlock, outOffset, inCompBlock, inOffset, numLeft);
       outOffset += num;
       inOffset++;
    }
    int compressedBitSize = (inOffset<<5)-offset;
    return compressedBitSize;    
  }
  
  
  /**
   * Write certain number of bits of an integer into an integer array starting from the given offset
   * 
   * @param out the output array 
   * @param val the integer to be written
   * @param outOffset the offset in the number of bits in the output array, where the integer will be written
   * @param bits the number of bits to be written
   */
  static private void writeBits(int[] out, int val, int outOffset, int bits) {
    if(bits == 0)
      return;
    final int index = outOffset >>> 5;
    final int skip = outOffset & 0x1f;
    val &= (0xffffffff >>> (32 - bits));   
    out[index] |= (val << skip);
    if (32 - skip < bits) {
      out[index + 1] |= (val >>> (32 - skip));
    }
  }
  
  /**
   * Read certain number of bits of an integer into an integer array starting from the given offset
   * 
   * @param in the input array 
   * @param val the integer to be written
   * @param inOffset the offset in the number of bits in the input array, where the integer will be read
   * @param bits the number of bits to be read, unlike writeBits(), readBits() does not deal with bits==0 and thus bits must > 0. When bits ==0, the calling functions will just skip the entire bits-bit slots without decoding them
   */
static private int readBits(int[] in, final int inOffset, final int bits) {
    final int index = inOffset >>> 5;
    final int skip = inOffset & 0x1f;
    int val = (int)(in[index] >>> skip);
    if (32 - skip < bits) {      
      val |= (int)(in[index + 1] << (32 - skip));
    }
    return val & (0xffffffff >>> (32 - bits));
  }
  
static private int readBitsForS16(int[] in, final int inIntOffset, final int inWithIntOffset, final int bits) {
  final int val = (int)(in[inIntOffset] >>> inWithIntOffset);
  return val & (0xffffffff >>> (32 - bits));
}

/**
 * Codes for encoding/decoding of the Simple16 compression algorithm
 * 
 */
  private static final int[] MASK = {0x00000000,
    0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f,
    0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff,
    0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff, 0x0001ffff, 0x0003ffff,
    0x0007ffff, 0x000fffff, 0x001fffff, 0x003fffff, 0x007fffff, 0x00ffffff,
    0x01ffffff, 0x03ffffff, 0x07ffffff, 0x0fffffff, 0x1fffffff, 0x3fffffff,
    0x7fffffff, 0xffffffff};

  private static final int S16_NUMSIZE = 16;
  private static final int S16_BITSSIZE = 28;
  private static final int[] S16_NUM = {28, 21, 21, 21, 14, 9, 8, 7, 6, 6, 5, 5, 4, 3, 2, 1};
  private static final int[][] S16_BITS = { {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
      {2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0},
      {1,1,1,1,1,1,1,2,2,2,2,2,2,2,1,1,1,1,1,1,1,0,0,0,0,0,0,0},
      {1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,0,0,0,0,0,0,0},
      {2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,3,3,3,3,3,3,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {3,4,4,4,4,3,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,4,4,4,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {5,5,5,5,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,4,5,5,5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {6,6,6,5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {5,5,6,6,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {7,7,7,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {10,9,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {14,14,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {28,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} };
  
  /**
   * Compress an integer array using Simple16
   * 
   * @param out the compressed output 
   * @param outOffset the offset of the output in the number of integers
   * @param in the integer input array
   * @param inOffset the offset of the input in the number of integers
   * @param n the number of elements to be compressed
   * @return the number of compressed integers
   */ 
  private static final int s16Compress(int[] out, int outOffset, int[] in, int inOffset, int n)
  {
     int numIdx, j, num, bits;
    for (numIdx = 0; numIdx < S16_NUMSIZE; numIdx++) 
    { 
      out[outOffset] = numIdx<<S16_BITSSIZE; 
      num = (S16_NUM[numIdx] < n) ? S16_NUM[numIdx] : n; 
      
      for (j = 0, bits = 0; (j < num) && in[inOffset+j] < (1<<S16_BITS[numIdx][j]); ) 
      { 
        out[outOffset] |= (in[inOffset+j]<<bits); 
        bits += S16_BITS[numIdx][j]; 
        j++;
      } 
      if (j == num) 
      { 
        return num;
      } 
    } 
    return -1;
  }
  
  /**
   * Decompress an integer array using Simple16
   * 
   * @param out the decompressed output 
   * @param outOffset the offset of the output in the number of integers
   * @param in the compressed input array
   * @param inOffset the offset of the input in the number of integers
   * @param n the number of elements to be compressed
   * @return the number of processed integers
   */ 
  private static final int s16Decompress(int[] out, int outOffset, int[] in, int inOffset, int n)
  {
     int numIdx, j=0, bits=0;
     numIdx = in[inOffset]>>>S16_BITSSIZE;
     int num = S16_NUM[numIdx] < n ? S16_NUM[numIdx] : n;
     for(j=0, bits=0; j<num; j++)
     {
       out[outOffset+j]  = readBitsForS16(in, inOffset, bits,  S16_BITS[numIdx][j]);
       bits += S16_BITS[numIdx][j];
     }
     return num;
  }
}
