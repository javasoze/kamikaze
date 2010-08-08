package com.kamikaze.docidset.compression;

import java.io.Serializable;
import java.util.Arrays;
import com.kamikaze.docidset.utils.CompResult;
import com.kamikaze.docidset.compression.PForDeltaUnpack;

/**
 * Implementation of the pForDelta algorithm for sorted integer arrays based on
 * 
 * 1. Original Algorithm from
 * http://homepages.cwi.nl/~heman/downloads/msthesis.pdf 2. Optimization and
 * variation from http://www2008.org/papers/pdf/p387-zhangA.pdf 3. Further optimization
 * http://www2009.org/proceedings/pdf/p401.pdf 
 *  2. The optimized algorithm from 
 *  http://www2009.org/proceedings/pdf/p401.pdf
 * 
 */
public class PForDeltaSetWithBase implements PForDeltaCompressedSortedIntegerSegment, Serializable {

  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;  

  // Max number of  bits to store an uncompressed value
  private static final int MAX_BITS = 32;

  // Header number. The header of each block contains 2 integers, the first one stores the parameters to decompress the block;
  // the second integer stores the last (uncompressed) docId of the block to speed up query processing by the skipping techniques.
  private static final int HEADER_NUM = 2;
  
  // Header size
  private static final int HEADER_SIZE = MAX_BITS * HEADER_NUM;
 
  // Default block size, must be X * 32
  private static final int MAX_EXPECTED_BLOCKSIZE = 1024;
  
  //The number of bits to store exception positions if fixedBitEncoding is chosen
  private static final int POSBITS = 8;
  
  // Auxiliary spaces to store temporary compressed/decompressed results
  private static int[] _expPos = new int[MAX_EXPECTED_BLOCKSIZE];
  private static int[] _expHighBits = new int[MAX_EXPECTED_BLOCKSIZE];
  private static int[] _expAux = new int[MAX_EXPECTED_BLOCKSIZE * 2];
  private static int[]  _EstCompBlock = new int[MAX_EXPECTED_BLOCKSIZE * 2]; 
  
  // The compressed size in bits of the block 
  private int _compressedBitSize = 0;
  
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
    
    // get expPos and expHighBits
    for (int i = 0; i<blockSize; ++i)
    {      
      if (inputBlock[i] > maxNoExp) 
      {
        _expPos[expNum] = i; //  the position of the exception        
        _expHighBits[expNum] = (inputBlock[i] >>> bits) & MASK[32-bits];   //  the higher 32-bits bits of the exception
        expNum++;
      }
    }    

    CompResult compRes = new CompResult();
    if(expNum>0)
    {
      System.arraycopy(_expPos, 0, _expAux, 0, expNum);
      System.arraycopy(_expHighBits, 0, _expAux, expNum, expNum);
      int compressedBitSize = compressBlockByS16(_EstCompBlock, 0, _expAux, expNum*2); // 2 blocks of expPos and expHighBits
      int compressedHighBitSize = compressBlockByS16(_EstCompBlock, 0, _expHighBits, expNum);
      // choose the better one of using Simple16 or using FixedBitSize to compress expPos
      if(compressedBitSize > (POSBITS *expNum + compressedHighBitSize))
      {
        compRes.setUsingFixedBitCoding(true);
        outputOffset += (POSBITS*expNum + compressedHighBitSize);
      }
      else
      {
        outputOffset += compressedBitSize;
      }
    }
    compRes.setCompressedSize(outputOffset);
    return compRes;
  }

  /**
   * Compress a block of non-negative integers
   * 
   * @param inputBlock
   * @param flag true if fixed bit size is used to encode exception positions
   * @return CompResult
   * @throws IllegalArgumentException
   */
  public CompResult compressOneBlock(int[] inputBlock, int bits, int blockSize, boolean flagFixedBitExpPos) throws IllegalArgumentException {
    // hy: compress a sequence of gaps except the first element (which is the original docId) 
    if (bits == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");        
   
    if(blockSize > MAX_EXPECTED_BLOCKSIZE)
    {
      _expPos = new int[blockSize];
      _expHighBits = new int[blockSize];
      _expAux = new int[blockSize * 2];
       _EstCompBlock = new int[blockSize * 2]; 
    }
    
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
        _expPos[expNum] = i; // write the position of exception
        _expHighBits[expNum] = (inputBlock[i] >>> bits) & MASK[32-bits];   // write the higher 32-bits bits of the exception
        
        expNum++;
      }
      outputOffset += bits;
    }    
    
    // The first HEADER stores:  flag | bits | expNum, assuming expNum < 2^10 and bits<2^10
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
        System.arraycopy(_expPos, 0, _expAux, 0, expNum);
        System.arraycopy(_expHighBits, 0, _expAux, expNum, expNum);
        if(flagFixedBitExpPos)
        {
          compressedBitSize = compressExpPosFixedBits(compressedBlock, outputOffset, _expPos, expNum, POSBITS);
          outputOffset += compressedBitSize;
          compressedBitSize = compressBlockByS16(compressedBlock, outputOffset, _expHighBits, expNum);
          outputOffset += compressedBitSize;
        }
        else
        {
          compressedBitSize = compressBlockByS16(compressedBlock, outputOffset, _expAux, expNum * 2);
          outputOffset += compressedBitSize;
        }
        
    }
    
    _compressedBitSize = outputOffset;
    
    CompResult compRes = new CompResult();
    compRes.setUsingFixedBitCoding(flagFixedBitExpPos);
    compRes.setCompressedBlock(compressedBlock);
    compRes.setCompressedSize(outputOffset);
    
    return compRes;
  }
 
 //call this function only after setParam() is called
  public int decompressOneBlock(int[] outDecompBlock, int[] compBlock, int blockSize)
  {
    if(blockSize > MAX_EXPECTED_BLOCKSIZE)
    {
      _expPos = new int[blockSize];
      _expHighBits = new int[blockSize];
      _expAux = new int[blockSize * 2];
       _EstCompBlock = new int[blockSize * 2]; 
    }
    
    // first decompress the bits and expNum
    if(compBlock == null)
    {
      System.out.println("compBlock is null");
      return 0;
    }
    
    int expNum = compBlock[0] & 0x3ff; 
    int bits = (compBlock[0]>>>10) & (0x1f);    
    boolean flagFixedBitExpPos = ((compBlock[0]>>>20) & (0x1)) > 0 ? true : false;
    
    int offset = HEADER_SIZE;
    int compressedBits = 0;
    if(bits == 0)
    {
      Arrays.fill(outDecompBlock,0);
    }
    else
    {
      compressedBits = decompressBBitSlots(outDecompBlock, compBlock, blockSize, bits);
      //compressedBits = decompressBBitSlotsWithHardCodes(decompBlock, compBlock, blockSize, bits);
    }
    offset += compressedBits;
    
    if(expNum>0)
    {
      if(flagFixedBitExpPos)
      {
        compressedBits =  decompressExpPosFixedBits(_expPos, compBlock, expNum, offset, 8);
        offset += compressedBits;
        compressedBits = decompressBlockByS16(_expHighBits, compBlock, expNum, offset);
        offset += compressedBits;
      }
      else
      {
        compressedBits = decompressBlockByS16(_expAux, compBlock, expNum*2, offset);
        offset += compressedBits;
      }
      
      int i=0;
      int curExpPos; // this makes sense since expNum is > 0
      int curHighBits;
      if(flagFixedBitExpPos)
      {
        curExpPos = _expPos[0]; // this makes sense since expNum is > 0
        curHighBits = _expHighBits[0];
      }
      else
      {
        curExpPos = _expAux[0]; // this makes sense since expNum is > 0
        curHighBits = _expAux[expNum];
      }
      outDecompBlock[curExpPos] = (outDecompBlock[curExpPos] & MASK[bits]) | ((curHighBits & MASK[32-bits] ) << bits);
      for (i = 1; i < expNum; i++) 
      { 
        //curExpPos += _expAux[i]  + 1;
        if(flagFixedBitExpPos)
        {
          curExpPos = _expPos[i]; // this makes sense since expNum is > 0
          curHighBits = _expHighBits[i];
        }
        else
        {
          curExpPos = _expAux[i]  ;
          curHighBits = _expAux[i+expNum];
        }
        outDecompBlock[curExpPos] = (outDecompBlock[curExpPos] & MASK[bits]) | ((curHighBits & MASK[32-bits] ) << bits);
      }
    }
    return offset;
  }
  
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
  
  public int decompressBBitSlotsWithHardCodes(int[] decompressedSlots, int[] compBlock, int blockSize, int bits)
  {
    int compressedBitSize = 0;
    PForDeltaUnpack.unpack(decompressedSlots, compBlock, bits);
    compressedBitSize = bits * blockSize;
    
    return compressedBitSize;    
  } 
  
  // compress a block by Simple16
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
  
  // decompress a block by Simple16
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
  
  // compress expPos using fixed number of bits
  private int compressExpPosFixedBits(int[] compBlock, int offset, int[] expPos, int expSize, int bits)
  {
    int outputOffset = offset;
    for(int i=0; i<expSize; i++)
    {
      writeBits(compBlock, expPos[i] & MASK[bits], outputOffset, bits); // copy lower bits-bits 
      outputOffset += bits;
    }
  
    int compressedBitSize = outputOffset - offset;
    return compressedBitSize;    
  }
  
  // decompress expPos using fixed number of bits
  private int decompressExpPosFixedBits(int[] expPos, int []compBlock, int expSize, int offset, int bits)
  {
    int outputOffset = offset;
  
    for(int i=0; i<expSize; i++)
    {      
      expPos[i] = readBits(compBlock, outputOffset, bits);
      outputOffset += bits;
    }
    
    int compressedBitSize = outputOffset - offset;
    return compressedBitSize;    
  }
  
//bits can be = 0
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
  
  // bits must > 0, unlike writeBits, readBits does not deal with bits==0. 
  //When bits ==0, the calling functions will just skip the entire bits-bit slots without decoding them
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
  
  // hy: n is the number to be compressed, after the call, inOffset+=returnValue, outOffset++;
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
  
//hy: no compression (just use int to represent them) 
  private int compressExpPosBasic(int[] compBlock, int offset, int[] expPos, int expSize)
  {
    if(offset%32 !=0)
    {
      System.out.println("comp: offset should have been the multiple of 32");
      return -1;
    }
    
    int startPos  = offset>>>5;
    for(int i=0; i<expSize; i++)
    {
      compBlock[startPos+i] = expPos[i];  
    }
    
    int compressedBitSize = expSize << 5;
    return compressedBitSize;    
  }
  
  private int decompressExpPosBasic(int[] expPos, int []compBlock, int expSize, int offset)
  {
    if(offset%32 !=0)
    {
      System.out.println("decompExpPos: offset should have been the multiple of 32");
      return -1;
    }
    
    int startPos = offset>>>5;
    for(int i=0; i<expSize; i++)
    {      
      expPos[i] = compBlock[startPos+i];  
    }
    
    int compressedBitSize = expSize << 5;
    return compressedBitSize;    
  }
  
  // hy: just use 32-bits to represent the high bits of exps 
  private int compressExpHighBitsBasic(int[] compBlock, int offset, int[] expHighBits, int expSize, int bits)
  {   
    int compressedBitSize = 0;    
    for(int i =0; i<expSize; i++)
    {
     writeBits(compBlock, expHighBits[i] & MASK[bits], offset, bits);
     offset += bits;
    }
    compressedBitSize = bits * expSize;
    
    return compressedBitSize;    
  }
  
  private int decompressExpHighBitsBasic(int[] expHighBits, int[] compBlock, int expSize, int offset, int bits)
  {    
    if(offset%32 !=0)
    {
      System.out.println("decompExpHighBits: offset should have been the multiple of 32");
      return -1;
    }
    int compressedBitSize = 0;   
    for(int i =0; i<expSize; i++)
    {
      expHighBits[i] = readBits(compBlock, offset, bits);
      offset +=  bits;
    }
    compressedBitSize = bits * expSize;
    
    return compressedBitSize;    
  }
  
  // for debug
  private void printList(int[] list, int start, int end)
  {
    System.out.print("(" + (end-start+1) + ")[");
    for(int i=start; i<=end; ++i)
    {
      System.out.print(list[i]);
      System.out.print(", ");
    }
    System.out.println("]");
  }
  
}
