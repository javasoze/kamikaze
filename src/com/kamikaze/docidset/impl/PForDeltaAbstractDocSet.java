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
 
  public static final int DEFAULT_BATCH_SIZE = 128;
    
  private static final int[] POSSIBLE_B = {1,2,3,4,5,6,7,8,9,10,11,12,13,16,20};
 
  /**
   * Default batch size for compression blobs
   * 
   */
  public int BATCH_SIZE = DEFAULT_BATCH_SIZE;

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
  
  protected int totalDocIdNum=0;

  /**
   * Current Bit Size
   * 
   */
  protected int currentB = 1;

   
  /** 
   * compressed bit size
   */ 
   protected long compressedBitSize=0;

   // hy: used for binary searching across blocks
   IntArray baseList = null;  
   
  /**
   * Internal compression Method 
   * @return compressed object
   */
  
  protected abstract CompResult PForDeltaCompressOneBlock(int[] srcData, int b);

  protected PForDeltaAbstractDocSet() {
    //hy: initially, blob.size() is _count which is 0, only when add() is called, _count > =0. 
    this.sequenceOfCompBlocks = new PForDeltaIntSegmentArray();
    compressedBitSize = 0;
    
    baseList = new IntArray();
  }

  /**
   * Internal Decompression Method
   * 
   * @return decompressed in the form of integer array
   */
  protected int[] decompress(BitSet packedSet) {
    System.err.println("Method not implemented");
    return null;
  }

  private void initSet() {
    this.currentNoCompBlock = new int[BATCH_SIZE];
    sizeOfCurrentNoCompBlock = 0;
    currentB = 31;    
  }
   

  /**
   * Add document to this set
   * 
   */
  public void addDoc(int docId)
  {
    //if (size() == 0)
    if(totalDocIdNum==0)
    {
      initSet();
      baseList.add(docId);
      currentNoCompBlock[sizeOfCurrentNoCompBlock++] = docId;    
      lastAdded = docId;      
    }
    else if (sizeOfCurrentNoCompBlock == BATCH_SIZE)
    { 
      // hy: the base of the currentNoCompBlock[] is also added into the baseList
      baseList.add(docId);
      // hy: compress currentNoCompBlock[] (excluding the input docId) , return the compressed block with its compressed bitSize
      CompResult compRes = PForDeltaCompressCurrentBlock();
      
      if(compRes.getCompressedBlock() == null)
      {
        System.err.println("ERROR in compressing ");
      }
      
      compressedBitSize += compRes.getCompressedSize();      
      sequenceOfCompBlocks.add(compRes.getCompressedBlock());

      // hy: next block
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
   * Number of compressed units (hy: e.g., docIds) plus the last block
   * @return docset size
   */
  public int size() {
    if(totalDocIdNum != sequenceOfCompBlocks.size() * BATCH_SIZE + sizeOfCurrentNoCompBlock)
    {
      System.err.println("ERROR: totalDocIdNum is wrong");
    }
    return totalDocIdNum;
    //return sequenceOfCompBlocks.size() * BATCH_SIZE + sizeOfCurrentNoCompBlock;
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
   * Compress the current block with the size of BATCH_SIZE
   * 
   */
  public CompResult PForDeltaCompressCurrentBlock()
  { 
    // hy: find the best b (achieving the smallest overall compressed size) and the compressed block
    currentB = POSSIBLE_B[0];   
    int tmpB = currentB;
   
    preProcessBlock(currentNoCompBlock, sizeOfCurrentNoCompBlock);
    CompResult optRes = PForDeltaCompressOneBlock(currentNoCompBlock, tmpB);
    if(optRes == null)
    {     
      return null;
    }
    
    for (int i = 1; i < POSSIBLE_B.length; ++i)
    {
      tmpB = POSSIBLE_B[i];
      CompResult curRes = PForDeltaCompressOneBlock(currentNoCompBlock, tmpB);
      if(curRes.getCompressedSize() < optRes.getCompressedSize())
      {
        currentB = tmpB;
        optRes = curRes;
      }
    }
    return optRes;  
  }
 
  
// hy: precompute the number of bits for the number within 0-255, NUMBITS[0] =1; NUMBITS[1] = 1, NUMBITS[2]=2; NUMBITS[3]=2;
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
