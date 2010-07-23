package com.kamikaze.docidset.impl;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.compression.PForDeltaSetWithBase;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.CompResult;

/**
 * Doc id set wrapper around PForDeltaSetWithBase 
 * 
 * 
 * @author abhasin
 * 
 */
public class PForDeltaDocIdSet extends PForDeltaAbstractDocSet implements Serializable {
  //hy: use adaptor (the object of the class PForDeltaSetWithBase) 
  private static final long serialVersionUID = 1L;

  /**
   * Utility Object compression.
   */
  private PForDeltaSetWithBase compressedBlocksNoBase = new PForDeltaSetWithBase();

  public PForDeltaDocIdSet() {  
    super();
    compressedBitSize = 0;    
  }
  
  public PForDeltaDocIdSet(int batchSize) {
    this();
    this.BATCH_SIZE = batchSize;      
  }
  
  private int[] curDecompBlock = new int[BATCH_SIZE];
  @Override
  public final boolean isCacheable() {
    return true;
  }
  
  @Override
  protected CompResult PForDeltaCompressOneBlock(int[] srcData, int b)
  {    
    compressedBlocksNoBase.setParam(b, BATCH_SIZE);
    CompResult compRes = compressedBlocksNoBase.compressOneBlock(srcData);
    return compRes;
  }
   
  /**
   * Method to decompress the entire block
   * 
   * @param compressedBlock the compressed block as an array of ints
   * @return the decompressed block stored as ints 
   */
  protected int[] decompress(int[] compressedBlock) {
    return new PForDeltaSetWithBase().decompress(compressedBlock);
  }

  // hy: the baseListForOnlyCompBlocks (in) contains all last elements of the compressed block. 
  protected int binarySearchInBaseListForBlockThatMayContainTarget(IntArray in, int start, int end, int target)
  {   
    return binarySearchForFirstElementEqualOrLargerThanTarget(in, start, end, target);
  }
  
 
  /**
   * Binary search for the first element that is equal to or larger than the target 
   * 
   * @param in must be sorted and contains no duplicates
   * @param start
   * @param end
   * @param target
   * @return the index of the first element in the array that is equal or larger than the target. -1 if the target is out of range.
   */  
  protected int binarySearchForFirstElementEqualOrLargerThanTarget(int in[], int start, int end, int target)
  {
    int mid;
    while(start < end)
    {
      mid = (start + end)/2;
      if(in[mid] < target)
        start = mid+1;
      else if(in[mid] == target)
        return mid;
      else
        end = mid;
    }
    // hy: start == end;
    if(in[start] >= target)
      return start;
    else
      return -1;
  }
  
  /**
   * Binary search for the first element that is equal or larger than the target 
   * 
   * @param in must be sorted
   * @param start
   * @param end
   * @param target
   * @return the index of the first element in the array that is equal or larger than the target. -1 if the target is out of range.
   */  
  protected int binarySearchForFirstElementEqualOrLargerThanTarget(IntArray in, int start, int end, int target)
  {   
    int mid;
    while(start < end)
    {
      mid = (start + end)/2;
      if(in.get(mid) < target)
        start = mid+1;
      else if(in.get(mid) == target)
        return mid;
      else
        end = mid;
    }
    // hy: start == end;
    if(in.get(start) >= target)
      return start;
    else
      return -1;
  }
  
  /**
   * Regular Binary search for the the target 
   * 
   * @param in must be sorted
   * @param start
   * @param end
   * @param target
   * @return the index of the target in the input array. -1 if the target is out of range.
   */  
  protected int binarySearchForTarget(int[] vals, int start, int end, int target)
  {
    int mid;
    while(start <= end)
    {
      mid = (start+end)/2;
      if(vals[mid]<target)
        start = mid+1;
      else if(vals[mid]==target)
        return mid;
      else
        end = mid-1;
    }
    return -1;
  }
  
  public PForDeltaDocIdSetIterator iterator() {
    return new PForDeltaDocIdSetIterator();
  }

  @Override
  public int findWithIndex(int target) {
    // hy: find the target and advance to there, return the advanced cursor position, otherwise return -1
    PForDeltaDocIdSetIterator dcit = new PForDeltaDocIdSetIterator();
    
    int docid = dcit.advance(target);
    if (docid == target)
      return dcit.getCursor();
    return -1;
  }
  
  @Override
  public boolean find(int target)
  { 
    // hy: this func is in PForDeltaDocIdSet instead of in PForDeltaDocIdSetIterator, therefore it cannot use iterBlockIndex, cursor, etc.
    if(size()==0)
      return false;
   
    // hy: first search noComp block
    if(sizeOfCurrentNoCompBlock > 0)
    {
      if(target > currentNoCompBlock[sizeOfCurrentNoCompBlock-1])
        return false;     
      
      // hy: if it is possible to be in the noComp block, then we check 
      if(target>baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))
      {
        if(binarySearchForTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target) >= 0)
          return true;
        else
          return false; 
      }
    }

    // hy: search for the compressed space (we do not keep track of decompressed comp blocks, instead, we alwasy decompress the comp block for each find())
   int posBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(posBlock<0)
     return false;
   
   compressedBlocksNoBase.setParam(0, BATCH_SIZE);
   //int[] decompBlock = compressedBlocksNoBase.decompressOneBlock(sequenceOfCompBlocks.get(posBlock));
   compressedBlocksNoBase.decompressOneBlockFast(curDecompBlock, sequenceOfCompBlocks.get(posBlock));
   postProcessBlock(curDecompBlock, BATCH_SIZE);
        
   int pos = binarySearchForTarget(curDecompBlock, 0, BATCH_SIZE-1, target);
   if(pos>=0) 
      return true;
   
   return false;
  }

  
  @Override
  public void optimize()
  {
    //Trim the baselist to size
    this.baseListForOnlyCompBlocks.seal();
    this.sequenceOfCompBlocks.seal();
  }
  

  @Override
  public long sizeInBytes()
  {
    // 64 is the overhead for an int array
    // blobsize * numberofelements * 1.1 (Object Overhead)
    // batch_size * 4 + int array overhead
    // P4dDocIdSet Overhead 110
    optimize();
    int headInBytes = baseListForOnlyCompBlocks.length()*4*2; // 1 int for storing b and expNum; the other int is for storing base
    return (long) (headInBytes + 64 +sequenceOfCompBlocks.length()*BATCH_SIZE*1.1 + BATCH_SIZE*4 + 24 + 110);
    
  }
  
  public int totalBlobSize()
  {
    return totalSequenceSize();
  }
  
  public int totalSequenceSize()
  {
    int total = 0;
    for(int i = sequenceOfCompBlocks.length() - 1; i >= 0; i--)
    {
      int[] segment = sequenceOfCompBlocks.get(i);
      total += segment.length;
    }
    return total;
  }
  
  
  class PForDeltaDocIdSetIterator extends StatefulDSIterator implements Serializable {

    private static final long serialVersionUID = 1L;
  
    int BATCH_INDEX_SHIFT_BITS;
  
    // hy: The following three pointer variables must be synced always!!!, so my current way is 
    //  to only update cursor after moving the pointers; and each time before we use offset and iterBlockIndex 
    // (in particular, each time when nextDoc() and advance() is called), we use cursor to get them
    // this is just for safety, later i will change this to be more efficient
    int cursor = -1; // hy: the current pointer 
    int iterBlockIndex = -1; // hy: the current block index
   // int offset = 0; // hy: the distance of the cursor to the beginning of the current block  
    int lastAccessedDocId = -1; // hy: the docId that was accessed of the last time called nextDoc() or advance(), therefore, it is kind of synced with the above three too

    int sequenceSize = size(); // hy: the number of docIds in the sequence 

    int compBlockNum=0; // hy: the number of compressed blocks
    
    int[] iterDecompBlock = new int[BATCH_SIZE];
   
    PForDeltaSetWithBase iterPForDeltaSetNoBase = new PForDeltaSetWithBase();

    PForDeltaDocIdSetIterator() {
      super();
      compBlockNum = sequenceOfCompBlocks.size();
      iterBlockIndex = 0;
     // offset = 0;
      cursor = -1;
      // hy: BATCH_INDEX_SHIFT_BITS = log2(BATCH_SIZE); assuming BATCH_SIZE = 2^k, e.g., BATCH_SIZE=32 =2^5 ; => BATCH_INDEX_SHIFT_BITS = 5 bits = lg(32) = bitShiftLoopNum-1
      // that is why i is initialized as -1, then we can get blockIndex = (docIdIndex >> BATCH_INDEX_SHIFT_BITS); for example, in the above example,
      // if docIdIndex = 32, it belongs to the No ((32>>5)=1) block, that is, the 2nd block (the first block is No 0). 
      int i=-1;
      for(int x=BATCH_SIZE; x>0; ++i, x>>>=1);  
      BATCH_INDEX_SHIFT_BITS = i;
      //System.out.println("BATCH_INDEX_SHIFT_BITS" + BATCH_INDEX_SHIFT_BITS);
      // hy: assume that b=31 results in 0 exps, in this Iterator, we do not need to set b and expCount since
      // we do not need to compress one block. In contrast, we only need to decompress one block.
      // However, we still need to set BATCH_SIZE.
      iterPForDeltaSetNoBase.setParam(31, BATCH_SIZE);  
    }

    @Override
    public int docID() {
      return lastAccessedDocId;
    }

    /**
     * Method to allow iteration in decompressed form
     */
    @Override
    public int nextDoc() 
    {
      // hy: return docId instead of d-gap
      // increment the cursor and check if it falls in the range for the
      // number of batches, if not return false else, its within range
      if(cursor == sequenceSize || ++cursor == sequenceSize)
      {// hy: if ptr already ptrs to the end; or its next position ptrs to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      iterBlockIndex = batchIndex(cursor);
      int offset = cursor % BATCH_SIZE; // hy: sync offset with cursor
      //System.out.println("iterBlockIndex:" + iterBlockIndex + ", compBlockNum" + compBlockNum + ",cursor:" + cursor + ",sequenceSize:" + sequenceSize + ",offset" + offset);
      if(iterBlockIndex == compBlockNum) // hy: case 1: in the currentNoCompBlock[] array which has never been compressed yet and therefore not added into sequenceOfCompBlocks yet.
      { 
        lastAccessedDocId = currentNoCompBlock[offset];
      }
      //hy: must be in one of the comp blocks: case 2: the comp block is never decompressed; case 3: it was decompressed lately
      else if(offset == 0) // hy: since skipping case 1, that is, iterBlockIndex<compBlockNum && offset == 0; hy: start of the next comp block (need to decompress the block first)
      {
        //System.out.println("again, iterBlockIndex:" + iterBlockIndex + ", compBlockNum" + compBlockNum + ",cursor:" + cursor + ",sequenceSize:" + sequenceSize + ",offset" + offset);
       
        //iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(iterBlockIndex));
        iterPForDeltaSetNoBase.decompressOneBlockFast(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex));
        
        postProcessBlock(iterDecompBlock, BATCH_SIZE);        
        lastAccessedDocId = iterDecompBlock[offset];
      }
      else // hy: case 3: a specialin the lately decompressed block (different from blockIndex == compBlockNum where the currentNoCompBlock[] has never been compressed)
      {
        lastAccessedDocId = iterDecompBlock[offset];
      }        
      return lastAccessedDocId;
    }

    /**
     * Get the index of the batch this cursor position falls into
     * 
     * @param index
     * @return
     */
    private int batchIndex(int docIdIndex) {
      return docIdIndex >> BATCH_INDEX_SHIFT_BITS;
    }

    // hy:  to call this func, make sure baseListForOnlyCompBlock is not empty and  we must be able to find the target in this function, otherwise something must be wrong
    // hy: I did not put the logic of this inside the func is because i want to make this func simple logic
    private int advanceToTargetInTheFollowingCompBlocks(int target)
    {
      int posBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, iterBlockIndex, baseListForOnlyCompBlocks.size()-1, target);
      
      //System.out.println("baseListForOnlyCompBlocks:" + baseListForOnlyCompBlocks + ",posBlock: " + posBlock + ",target: " + target);
      if(posBlock < 0)
      {
        System.err.println("ERROR: advanceToTargetInTheFollowingCompBlocks(): Impossible, we must be able to find the block");
      }
      
      //iterBlockIndex = posBlock;
      
      //System.out.println("sequenceOfCompBlocks.size():" + sequenceOfCompBlocks.size() + ",posBlock:" + posBlock);
      
      iterPForDeltaSetNoBase.decompressOneBlockFast(iterDecompBlock, sequenceOfCompBlocks.get(posBlock));        
      postProcessBlock(iterDecompBlock, BATCH_SIZE);
      
      int pos = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, 0, BATCH_SIZE-1, target);
      if(pos < 0)
      {
        System.err.println("ERROR: case 2: advanceToTargetInTheFollowingCompBlocks(), Impossible, we must be able to find the target" + target + " in the block " + posBlock);
      }
      cursor = posBlock * BATCH_SIZE + pos; 
      //System.out.println("case 2: cursor: " + cursor);
      return iterDecompBlock[pos];
    }
    
    private void printArray(int[] list, int start, int end)
    {
      System.out.print("(" + (end-start+1) + ")[");
      for(int i=start; i<=end; ++i)
      {
        System.out.print(list[i]);
        System.out.print(", ");
      }
      System.out.println("]");
    }
    
    /**
     * Next need be called after skipping.
     * 
     */
    @Override
    public int advance(int target) {
      // hy: the expected behavior is to find the first element AFTER the current cursor, who is equal or larger than target
      if(target <= lastAccessedDocId)
      {
        target = lastAccessedDocId + 1;
      }
      
      if(sequenceSize <= 0)
      {
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(cursor == sequenceSize || ++cursor == sequenceSize)
      {// hy: if ptr already ptrs to the end; or its next position ptrs to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      int offset = cursor % BATCH_SIZE;
      iterBlockIndex = batchIndex(cursor);
      
      //System.out.println("cursor: " + cursor + ", offset: " + offset);
      
      // hy: if there is noComp block, check noComp block 
      // the next element is in currently in the last block , or currently not in the last block, but the target is larger than the last element of the last compressed block
      if(sizeOfCurrentNoCompBlock>0) // if there exists the last decomp block (which does not mean that there is definitely decomp block)
      {
        if(iterBlockIndex == compBlockNum || (baseListForOnlyCompBlocks.size()>0 && target > baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)))
        {   
          offset = binarySearchForFirstElementEqualOrLargerThanTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target);
          if(offset>=0)
          {         
            iterBlockIndex = compBlockNum;
            //System.out.println("found at pos" + offset + ", which is " + currentNoCompBlock[offset] + " against the target of " + target);
            lastAccessedDocId = currentNoCompBlock[offset];            
            cursor = iterBlockIndex * BATCH_SIZE + offset; 
            return lastAccessedDocId;
          }                   
          else
          {
            cursor = sequenceSize; // hy: to avoid the next time repeated lookup once it reaches the end of the sequence
            lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
            return lastAccessedDocId;
          }
        }     
      }
      
      // hy: if did not find it in the noComp block, check the comp blocks
      if(baseListForOnlyCompBlocks.size()>0 && target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))  
      {
        // hy: for the following cases, it must exist in one of the comp block since target<= the last base in the comp blocks
        if(offset == 0)
        {
          // hy: searching the right block from the current block to the last block
          lastAccessedDocId = advanceToTargetInTheFollowingCompBlocks(target);
          return lastAccessedDocId;
        }
        else // case 3: offset > 0, hy: the current block has been decompressed, so, first test the first block; and then do sth like case 2 
        {
          if(target <= baseListForOnlyCompBlocks.get(iterBlockIndex))
          {
            //System.out.println("target: " + target + " is <= baseListForOnlyCompBlocks.get(iterBlockIndex)" + ", iterBlockIndex:" + iterBlockIndex + ": " + baseListForOnlyCompBlocks.get(iterBlockIndex));
            offset = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, offset, BATCH_SIZE-1, target);
            
            if(offset < 0)
            {
              System.err.println("case 3: Impossible, we must be able to find the target " + target + " in the block" + iterDecompBlock + ", offset: " + offset);
              printArray(iterDecompBlock, 0, BATCH_SIZE-1);
            }
            cursor = iterBlockIndex * BATCH_SIZE + offset; 
            lastAccessedDocId = iterDecompBlock[offset];
            return lastAccessedDocId;
          }
          else // hy: there must exist other comp blocks between the current block and noComp block since target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)
          { 
            lastAccessedDocId = advanceToTargetInTheFollowingCompBlocks(target);
            return lastAccessedDocId;
          }
        }        
      }
    
      lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
      return lastAccessedDocId; 
 }
 
  
    private void printSet() 
    {
       for (int i = 0; i < BATCH_SIZE; i++) 
       {
          iterPForDeltaSetNoBase.decompressOneBlockFast(iterDecompBlock, sequenceOfCompBlocks.get(i));
          postProcessBlock(iterDecompBlock, BATCH_SIZE);
          System.out.print(iterDecompBlock + ",");
        }
     }

    public int getCursor() {
      return cursor;
    }

  } // hy: end of Iterator
    
  
  
}
