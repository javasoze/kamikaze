package com.kamikaze.docidset.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.PForDeltaIntSegmentArray;
import com.kamikaze.docidset.utils.CompResult;

public abstract class PForDeltaAbstractDocSet extends DocSet implements Serializable {

  private static final long serialVersionUID = 1L;
 
  public static final int DEFAULT_BATCH_SIZE = 256;
    
  private static final int[] POSSIBLE_B = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,16,20};
 
  /**
   * Default batch size for compression blobs
   * 
   */
  public int batchSize = DEFAULT_BATCH_SIZE;

  /**
   * Last added value
   * 
   */
  protected int lastAdded = 0;
  
  /**
   * List of Data blocks
   * 
   */
  protected PForDeltaIntSegmentArray sequenceOfCompBlocks = null;

  /**
   * Pointer to the current data block. 
   * 
   */
  protected int[] currentNoCompBlock = null;

  /**
   * Size of the currentNoCompBlock array
   * 
   */
  protected int sizeOfCurrentNoCompBlock = 0; 
  
  protected int totalDocIdNum=0; // hao: transient ?

  /**
   * Current Bit Size
   * 
   */
  protected int currentB = 1;

   
  /** 
   * compressed bit size
   */ 
   protected long compressedBitSize=0; // hao: transient ?

   /** 
    * base list for skipping
    */ 
   IntArray baseListForOnlyCompBlocks = null;  // hao: transient ?
   
  /**
   * Internal compression Method 
   * @return compressed object
   */
  protected abstract CompResult PForDeltaCompressOneBlock(int[] srcData, int b, boolean flag);
  protected abstract CompResult PForDeltaEstimateCompSize(int[] srcData, int b);

  protected PForDeltaAbstractDocSet() {
    this.sequenceOfCompBlocks = new PForDeltaIntSegmentArray();
    baseListForOnlyCompBlocks = new IntArray();
  }

  private void initSet() {
    this.currentNoCompBlock = new int[batchSize];
    Arrays.fill(this.currentNoCompBlock, 0);
    sizeOfCurrentNoCompBlock = 0;
    compressedBitSize = 0;
    currentB = 31;    
  }
  
  /**
   * Get compressed size in bits
   * 
   * @return compressed size in bits
   */
  public long getCompressedBitSize()
  {
    return compressedBitSize;
  }

  /**
   * Add document to this set
   * 
   */
  public void addDoc(int docId)
  {
    if(totalDocIdNum==0)
    {
      initSet();
      currentNoCompBlock[sizeOfCurrentNoCompBlock++] = docId;    
      lastAdded = docId;      
    }
    else if (sizeOfCurrentNoCompBlock == batchSize)
    { 
      //the last docId of the block      
      baseListForOnlyCompBlocks.add(lastAdded);
      
      // compress currentNoCompBlock[] (excluding the input docId), return the compressed block with its compressed bitSize
      CompResult compRes = PForDeltaCompressCurrentBlock();
      
      if(compRes.getCompressedBlock() == null)
      {
        System.err.println("ERROR in compressing ");
      }
      
      compressedBitSize += compRes.getCompressedSize();      
      sequenceOfCompBlocks.add(compRes.getCompressedBlock());

      // next block
      sizeOfCurrentNoCompBlock = 1;
      lastAdded = docId;
      currentNoCompBlock[0] = docId;
    }
    else 
    {
      try 
      {   
        lastAdded = docId;
        currentNoCompBlock[sizeOfCurrentNoCompBlock++] = docId;
      } 
      catch (ArrayIndexOutOfBoundsException w) 
      {
        System.err.println("Error inserting DOC:" + docId);
      }
    } // end append to end of array    
    totalDocIdNum++;
  }
  
  /**
   *  Flush the data left in the currentNoCompBlock into the compressed data
   * 
   */
  public void flush(int docId)
  {
    CompResult compRes = PForDeltaCompressCurrentBlock();
    compressedBitSize += compRes.getCompressedSize();      
    sequenceOfCompBlocks.add(compRes.getCompressedBlock());
  }
  /**
   * Number of compressed units (for example, docIds) plus the last block
   * @return docset size
   */
  public int size() {
//    if(totalDocIdNum != sequenceOfCompBlocks.size() * batchSize + sizeOfCurrentNoCompBlock)
//    {
//      System.err.println("ERROR: totalDocIdNum is wrong");
//    }
    return totalDocIdNum;
  }
  
  /**
   * Prefix Sum
   * 
   */
  protected void preProcessBlock(int[] block, int size)
  {
    for(int i=size-1; i>0; --i)
    {
      block[i] = block[i] - block[i-1] - 1; 
    }
  }
  
  /**
   * Reverse Prefix Sum
   * 
   */
  protected void postProcessBlock(int[] block, int size)
  {
    for(int i=1; i<size; ++i)
    {
      block[i] = block[i] + block[i-1] + 1;     
    }
  }
 
  /**
   * Compress the current block with the size of batchSize
   * 
   */
  public CompResult PForDeltaCompressCurrentBlock()
  { 
    // find the best b that can lead to the smallest overall compressed size
    currentB = POSSIBLE_B[0];   
    int tmpB = currentB;
    
    preProcessBlock(currentNoCompBlock, sizeOfCurrentNoCompBlock);
    CompResult optRes = PForDeltaEstimateCompSize(currentNoCompBlock, tmpB);
   
    //int optSize = PForDeltaEstimateCompBlockSize(currentNoCompBlock, tmpB);
    int curSize;
    for (int i = 1; i < POSSIBLE_B.length; ++i)
    {
      tmpB = POSSIBLE_B[i];
      CompResult curRes = PForDeltaEstimateCompSize(currentNoCompBlock, tmpB);
      if(curRes.getCompressedSize() < optRes.getCompressedSize())
      {
        currentB = tmpB;
        optRes = curRes;
      }
    }
    
    // return the compressed data achieved from the best b
    optRes = PForDeltaCompressOneBlock(currentNoCompBlock, currentB, false);
    return optRes;  
  }
 
  
// pre-compute the number of bits for the number within 0-255, NUMBITS[0] =1; NUMBITS[1] = 1, NUMBITS[2]=2; NUMBITS[3]=2;
private static final int[] NUMBITS = new int[256];
static {
  NUMBITS[0] = 1;
  int i, bits, x;
  for(i = 1; i < 256; i++)
  { 
    for(x =i, bits=0; x>0; ++bits, x>>>=1);  
    NUMBITS[i] = bits;
  }
}
  
  private static int getNumBits(int v)
  {
    int n;
    if((n = v >>> 24) > 0) return(NUMBITS[n] + 24);
    if((n = v >>> 16) > 0) return(NUMBITS[n] + 16);
    if((n = v >>> 8) > 0) return(NUMBITS[n] + 8);
    return NUMBITS[v];
  }
  
  protected void printBlock(int[] block, int size)
  {
    System.out.println(" ");
    System.out.println("to compress a block of size " + size);
    System.out.print("[");
    for(int i=0; i<size; i++)
    { 
        System.out.print(block[i]);
        System.out.print(" ");
    }
    System.out.println("]");
  }
  
}
