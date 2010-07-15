package com.kamikaze.docidset.compression;

import java.io.Serializable;
//import java.util.BitSet;
//import org.apache.lucene.util.OpenBitSet;
//import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.utils.CompResult;

/**
 * Implementation of the p4delta algorithm for sorted integer arrays based on
 * 
 * 1. Original Algorithm from
 * http://homepages.cwi.nl/~heman/downloads/msthesis.pdf 2. Optimization and
 * variation from http://www2008.org/papers/pdf/p387-zhangA.pdf 3. Further optimization
 * http://www2009.org/proceedings/pdf/p401.pdf 
 * 
 * This class is a wrapper around a CompressedSegment based on Lucene OpenBitSet
 */
public class PForDeltaSetNoBase implements PForDeltaCompressedSortedIntegerSegment,
    Serializable {

  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;  

  // Maximum bits that can be used = 32

  // 32 bits for retaining base value
  private static final int BASE_MASK = 32;

  // Header size
  private static final int HEADER_MASK = BASE_MASK;
 
  // hy: Header number
  private static final int HEADER_NUM = 2;

  // Parameters for the compressed set
  private int _b = INVALID;

  private int _batchSize = INVALID;

  private int _expNum = INVALID;

  private int _compressedBitSize = 0;
  
  
  //hy: set param for one block to be prepared for compression
  public void setParam(int b, int batchSize) {
    // hy: attn: _b must be > 0
    this._b = b;
    this._batchSize = batchSize;    
}  

  /**
   * Alternate implementation for compress
   * 
   * @param input
   * @return comprssed set in long array form
   * @throws IllegalArgumentException
   */
  public CompResult compressOneBlock(int[] inputBlock) throws IllegalArgumentException {
    // hy: compress a sequence of gaps except the first element (which is the original docId) 
    if (_b == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");        
   
    int BATCH_MAX = 1<<_b;
   
    // hy: the following allocated memory should be enough for storing the compressed data, the last 32 bits is for the aligned word
    // header (_b|_expNum and base for skipping) + _batchSize b-bit slots + expPos + expHighBits + alignment  
    int MAX_POSSIBLE_COMPRESSED_BITSIZE =  HEADER_MASK *HEADER_NUM + _batchSize * _b  + _batchSize * BASE_MASK + _batchSize * BASE_MASK + 32;
    
    int[] compressedBlock = new int[(MAX_POSSIBLE_COMPRESSED_BITSIZE>>>5)];
    int[] expPos = new int[_batchSize];
    int[] expHighBits = new int[_batchSize];
    
    // hy: the first int of the header records the value of _b and the number of exps, the second one records the base
    // hy: record compressedBlock[0] after compression
    //compressedBlock[0] = _b << 10 | _expNum;
    compressedBlock[1] = inputBlock[0];
    
    // Offset is the offset of the next location to place the value
    int offset = HEADER_MASK *  HEADER_NUM;
  
    int expIndex = 0;      
    
    for (int i = 0; i < _batchSize; i++) 
    {      
      if (inputBlock[i] < BATCH_MAX) // not exp
      {
        writeBits(compressedBlock, inputBlock[i], offset, _b);
      } 
      else // exp
      {
        //System.out.println("exp input[" + i + "]: " + input[i]);
        writeBits(compressedBlock, inputBlock[i] & MASK[_b], offset, _b); // hy: copy lower b-bits 
        expPos[expIndex] = i; // hy: write the position array        
        expHighBits[expIndex] = (inputBlock[i] >>> _b) & MASK[32-_b];   // hy: write the higher 32-b bits     
        expIndex++;
      }
      offset += _b;
    }    
    
    _expNum = expIndex;
    // hy: assuming expNum < 2^10
    compressedBlock[0] = (_b << 10) | (_expNum & 0x3ff);
    
    int compressedExpPosBitSize = compressExpPos(compressedBlock, offset, expPos, expIndex);
    offset += compressedExpPosBitSize;
    
    int compressedExpHighBits = compressExpHighBits(compressedBlock, offset, expHighBits, expIndex, 32-_b);
    offset += compressedExpHighBits;
    
    if(offset > MAX_POSSIBLE_COMPRESSED_BITSIZE)
    {
       System.err.println("ERROR: compressed buffer overflow"); 
    }
    
    _compressedBitSize = offset;
    
    CompResult compRes = new CompResult();
    compRes.setCompressedBlock(compressedBlock);
    compRes.setCompressedSize(offset);
    
    return compRes;
  }

 
  public int getCompressedBitSize()
  {
     return _compressedBitSize;
  }  
    
  static private void writeBits(int[] bits, int val, int offset, int length) {
    // hy: length must > 0
    final int index = offset >>> 5;
    final int skip = offset & 0x1f;
    val &= (0xffffffff >>> (32 - length));   
    bits[index] |= (val << skip);
    if (32 - skip < length) {
      bits[index + 1] |= (val >>> (32 - skip));
    }
  }
  
static private int readBits(int[] bits, final int offset, final int length) {
    final int index = offset >>> 5;
    final int skip = offset & 0x1f;
    int val = (int)(bits[index] >>> skip);
    if (32 - skip < length) {      
      val |= (int)(bits[index + 1] << (32 - skip));
    }
    return val & (0xffffffff >>> (32 - length));
  }
  
//void writeBits(int[] buf,  int[] offset, int val, int bits)
//{                                     
//  int bPtr;
//  int w;
//
//  bPtr = offset[0]&31;
//  if (bPtr == 0)  buf[offset[0]>>>5] = 0;
//  w = (32 - bPtr > bits)? bits : (32 - bPtr);
//  buf[offset[0]>>>5] |= ((val&MASK[w])<<bPtr);
//  offset[0] += w;
//
//  if (bits > w)
//  {
//    buf[offset[0]>>>5] = (val>>>w)&MASK[bits-w];
//    offset[0] += (bits-w);
//  }
//}
//
//int readBits(int[] buf, int[] offset, int bits)
//{
//  int bPtr;
//  int w;
//  int v;
//
//  bPtr = offset[0]&31;
//  w = (32 - bPtr > bits)? bits : (32 - bPtr);
//  v = ((buf[offset[0]>>>5]>>>bPtr) & MASK[w]);
//  offset[0] += w;
//
//  if (bits == w)  return(v);
//
//  v = v | ((buf[offset[0]>>>5] & MASK[bits-w])<<w);
//  offset[0] += (bits-w);
//  return(v);
//}

  
  public int[] decompress(int[] compBlock)
  {
    return decompressOneBlock(compBlock);
  }  
  
  // hy: call this function after setParam() is called
  public int[] decompressOneBlock(int[] compBlock)
  {
    if (_b == INVALID)
      throw new IllegalArgumentException(" Codec not initialized correctly ");
    
    // hy: first decompress the _b and _expNum
    if(compBlock == null)
    {
      System.out.println("compBlock is null");
    }
    _expNum = compBlock[0] & 0x3ff; 
    _b = (compBlock[0]>>>10) & (0x1f);    
    
    int[] decompBlock = new int[_batchSize]; 
    int[] expPos = new int[_expNum];
    int[] expHighBits =  new int[_expNum];
   
    int offset = HEADER_MASK*HEADER_NUM;
    int compressedBits = 0;
    //System.out.println("decompressOneBlock(): _batchSize:" + _batchSize + "_b:" + _b + "expNum" + _expNum);
    
    compressedBits = decompressBBitSlots(decompBlock, compBlock, _batchSize, offset, _b);
    offset += compressedBits;
    
    compressedBits = decompressExpPos(expPos, compBlock, _expNum, offset);
    offset += compressedBits;
    
    compressedBits = decompressExpHighBits(expHighBits, compBlock, _expNum, offset, 32-_b);
    offset += compressedBits;
    
    int i=0;
    for (i = 0; i < _expNum; i++) 
    { 
      //System.out.println("i: " + i + ", expPos" + expPos[i] + ", expHighBits:" + expHighBits[i]);
      decompBlock[expPos[i]] = (decompBlock[expPos[i]] & MASK[_b]) | (expHighBits[i] << _b);
    }
    
    return decompBlock;
  }
  
  // hy: TODO: add compression method for compressing exps' positions 
  private int compressExpPos(int[] compBlock, int offset, int[] expPos, int expSize)
  {
    // hy: currently no compression for pos
    if(offset%32 !=0)
    {
      System.out.println("comp: offset should have been the multiple of 32");
      return -1;
    }
    
    int startPos  = offset>>5;
    //System.out.println("compExpPos: startPos" + startPos + ", expSize:" + expSize);
    for(int i=0; i<expSize; i++)
    {
      compBlock[startPos+i] = expPos[i];  
      //System.out.print(expPos[i] + " ");
    }
    
    int compressedBitSize = expSize * 32;
    return compressedBitSize;    
  }
  
  
  public int decompressExpPos(int[] expPos, int []compBlock, int expSize, int offset)
  {
    // hy: currently no compression for pos
    if(offset%32 !=0)
    {
      System.out.println("decompExpPos: offset should have been the multiple of 32");
      return -1;
    }
    
    int startPos = offset>>5;
    //System.out.println("decompExpPos: startPos" + startPos + ", expSize:" + expSize);
    for(int i=0; i<expSize; i++)
    {      
      expPos[i] = compBlock[startPos+i];  
      //System.out.print(expPos[i] + " ");
    }
    
    int compressedBitSize = expSize * 32;
    return compressedBitSize;    
  }
  
  private int compressExpHighBits(int[] compBlock, int offset, int[] expHighBits, int expSize, int bits)
  {   
    int compressedBitSize = 0;    
    for(int i =0; i<expSize; i++)
    {
      //System.out.println("comp: i:" + i + "b:" + b + "expHighBits" + expHighBits[i]);      
     writeBits(compBlock, expHighBits[i] & MASK[bits], offset, bits);
     offset += bits;
    }
    compressedBitSize = bits * expSize;
    
    return compressedBitSize;    
  }
  
  public int decompressExpHighBits(int[] expHighBits, int[] compBlock, int expSize, int offset, int bits)
  {    
    if(offset%32 !=0)
    {
      System.out.println("decompExpHighBits: offset should have been the multiple of 32");
      return -1;
    }
    int compressedBitSize = 0;   
    for(int i =0; i<expSize; i++)
    {
      //System.out.println("b:" + b + "decomp: i:" + i + "expHighBits" + expHighBits[i]);
      expHighBits[i] = readBits(compBlock, offset, bits);
      offset +=  bits;
      //expHighBits[i] = readBits(compBlock, offset +i*32, 32) & BATCH_HIGHER_MASK;
    }
    compressedBitSize = bits * expSize;
    
    return compressedBitSize;    
  }
  
  public int decompressBBitSlots(int[] decompressedSlots, int[] compBlock, int blockSize, int offset, int bits)
  {
    int compressedBitSize = 0;
    
    for(int i =0; i<blockSize; i++)
    {
      decompressedSlots[i] = readBits(compBlock, offset, bits);
      offset += bits;
    }
    compressedBitSize = bits * blockSize;
    
    return compressedBitSize;    
  } 

  public String printParams() {
    return "b val:" + _b ;
  }
  
  private static final int[] MASK = {0x00000000,
    0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f,
    0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff,
    0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff, 0x0001ffff, 0x0003ffff,
    0x0007ffff, 0x000fffff, 0x001fffff, 0x003fffff, 0x007fffff, 0x00ffffff,
    0x01ffffff, 0x03ffffff, 0x07ffffff, 0x0fffffff, 0x1fffffff, 0x3fffffff,
    0x7fffffff, 0xffffffff};

}
