package com.kamikaze.docidset.impl;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.compression.PForDeltaWithBase;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.PForDeltaIntSegmentArray;
import com.kamikaze.docidset.utils.CompResult;

public class PForDeltaDocId extends DocSet implements Serializable {

  private static final long serialVersionUID = 1L;
 
  private PForDeltaIntSegmentArray sequenceOfCompBlocks = null;
  private IntArray baseListForOnlyCompBlocks = null; 
  private int[] currentNoCompBlock = null;
  private int sizeOfCurrentNoCompBlock = 0; 
  private int[] curDecompBlock = null;
  
  public static final int DEFAULT_BATCH_SIZE = 256;
  private static final int[] POSSIBLE_B = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,16,20};
 
  private int _blockSize = DEFAULT_BATCH_SIZE;
  
  private int lastAdded = 0;
  private int totalDocIdNum=0; // cannot be transient (iterator will call size())
  private int currentB = 1;
  private long compressedBitSize=0; // hao: transient ?
   
  private PForDeltaWithBase compBlockWithBase = new PForDeltaWithBase();

  public PForDeltaDocId() {
    sequenceOfCompBlocks = new PForDeltaIntSegmentArray();
    baseListForOnlyCompBlocks = new IntArray();
    currentNoCompBlock = new int[_blockSize];
    sizeOfCurrentNoCompBlock = 0;
    curDecompBlock = new int[_blockSize];
    
    compressedBitSize = 0;
    currentB = 31;    
  }
  
  public PForDeltaDocId(int batchSize) {
    this();
    if(_blockSize < batchSize)
    {
      currentNoCompBlock = new int[batchSize];
      curDecompBlock = new int[batchSize];
    }
    sizeOfCurrentNoCompBlock = 0;
    _blockSize = batchSize;      
  }
 
  @Override
  public final boolean isCacheable() {
    return true;
  }
  
  public PForDeltaDocIdIterator iterator() {
    return new PForDeltaDocIdIterator();
  }

  @Override
  public int findWithIndex(int target) {
    // hy: find the target and advance to there, return the advanced cursor position, otherwise return -1
    PForDeltaDocIdIterator dcit = new PForDeltaDocIdIterator();
    
    int docid = dcit.advance(target);
    if (docid == target)
      return dcit.getCursor();
    return -1;
  }
  
  @Override
  public boolean find(int target)
  { 
    // hy: this func is in PForDeltaDocIdSet instead of in PForDeltaDocIdSetIterator, therefore it cannot use iterBlockIndex, cursor, etc.
    if(size()==0 || sizeOfCurrentNoCompBlock==0)
      return false;
    
    lastAdded = currentNoCompBlock[sizeOfCurrentNoCompBlock-1];
    if(target > lastAdded)
    {
      return false;
    }
    
    // hy: first search noComp block
    if(baseListForOnlyCompBlocks.size()==0 || target>baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))
    {
      if(binarySearchForTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target) >= 0)
        return true;
      else
        return false; 
    }

    // baseListForOnlyCompBlocks.size() must then >0
    // hy: search for the compressed space (we do not keep track of decompressed comp blocks, instead, we always decompress the comp block for each find())
   int posBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(posBlock<0)
     return false;
   
   compBlockWithBase.decompressOneBlock(curDecompBlock, sequenceOfCompBlocks.get(posBlock), _blockSize);
   postProcessBlock(curDecompBlock, _blockSize);
        
   int pos = binarySearchForTarget(curDecompBlock, 0, _blockSize-1, target);
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
    return (long) (headInBytes + 64 +sequenceOfCompBlocks.length()*_blockSize*1.1 + _blockSize*4 + 24 + 110);
    
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
    else if (sizeOfCurrentNoCompBlock == _blockSize)
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
  
  private CompResult PForDeltaCompressOneBlock(int[] srcData, int b, boolean flag)
  {    
    CompResult compRes = compBlockWithBase.compressOneBlock(srcData, b, _blockSize, flag);
    return compRes;
  }
   
  private CompResult PForDeltaEstimateCompSize(int[] srcData, int b)
  {    
    return compBlockWithBase.estimateCompSize(srcData, b, _blockSize);
  }
  
  
  private void initSet() {
    Arrays.fill(this.currentNoCompBlock, 0);    
  }
  
//hy: the baseListForOnlyCompBlocks (in) contains all last elements of the compressed blocks. 
  private int binarySearchInBaseListForBlockThatMayContainTarget(IntArray in, int start, int end, int target)
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
  private int binarySearchForFirstElementEqualOrLargerThanTarget(int in[], int start, int end, int target)
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
    //     start == end;
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
  private int binarySearchForFirstElementEqualOrLargerThanTarget(IntArray in, int start, int end, int target)
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
   * @param vals must be sorted
   * @param start
   * @param end
   * @param target
   * @return the index of the target in the input array. -1 if the target is out of range.
   */  
  private int binarySearchForTarget(int[] vals, int start, int end, int target)
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
  
  
  /**
   * Prefix Sum
   * 
   */
  private void preProcessBlock(int[] block, int size)
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
  private void postProcessBlock(int[] block, int size)
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
  private CompResult PForDeltaCompressCurrentBlock()
  { 
    // find the best b that can lead to the smallest overall compressed size
    currentB = POSSIBLE_B[0];   
    int tmpB = currentB;
    
    preProcessBlock(currentNoCompBlock, sizeOfCurrentNoCompBlock);
    CompResult optRes = PForDeltaEstimateCompSize(currentNoCompBlock, tmpB);
   
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
    optRes = PForDeltaCompressOneBlock(currentNoCompBlock, currentB, optRes.getUsingFixedBitCoding());
    return optRes;  
  }
  
  private void printBlock(int[] block, int size)
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
  
  
  class PForDeltaDocIdIterator extends StatefulDSIterator implements Serializable {

    private static final long serialVersionUID = 1L;
  
    int BLOCK_INDEX_SHIFT_BITS;
  
    // hy: The following three pointer variables must be synced always!!!, so my current way is 
    //  to only update cursor after moving the pointers; and each time before we use offset and iterBlockIndex 
    // (in particular, each time when nextDoc() and advance() is called), we use cursor to get them
    // this is just for safety, later i will change this to be more efficient
    int cursor = -1; // hy: the current pointer 
    int lastAccessedDocId = -1; // hy: the docId that was accessed of the last time called nextDoc() or advance(), therefore, it is kind of synced with the above three too

    int compBlockNum=0; // hy: the number of compressed blocks
    int[] iterDecompBlock = new int[_blockSize];
    PForDeltaWithBase iterPForDeltaSetWithBase = new PForDeltaWithBase();

    PForDeltaDocIdIterator() {
      super();
      compBlockNum = sequenceOfCompBlocks.size();
      cursor = -1;
      // hy: BLOCK_INDEX_SHIFT_BITS = log2(batchSize); assuming batchSize = 2^k, e.g., batchSize=32 =2^5 ; => BLOCK_INDEX_SHIFT_BITS = 5 bits = lg(32) = bitShiftLoopNum-1
      // that is why i is initialized as -1, then we can get blockIndex = (docIdIndex >> BLOCK_INDEX_SHIFT_BITS); for example, in the above example,
      // if docIdIndex = 32, it belongs to the No ((32>>5)=1) block, that is, the 2nd block (the first block is No 0). 
      int i=-1;
      for(int x=_blockSize; x>0; ++i, x>>>=1);  
      BLOCK_INDEX_SHIFT_BITS = i;
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
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      { // hy: if ptr already ptrs to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      {// hy: if ptr ptrs to its next position ptrs to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      int iterBlockIndex = getBlockIndex(cursor);
      int offset = cursor % _blockSize; // hy: sync offset with cursor
      
      if(iterBlockIndex == compBlockNum) // hy: case 1: in the currentNoCompBlock[] array which has never been compressed yet and therefore not added into sequenceOfCompBlocks yet.
      { 
        lastAccessedDocId = currentNoCompBlock[offset];
      }
      //hy: must be in one of the comp blocks: case 2: the comp block is never decompressed; case 3: it was decompressed lately
      else if(offset == 0) // hy: since skipping case 1, that is, iterBlockIndex<compBlockNum && offset == 0; hy: start of the next comp block (need to decompress the block first)
      {
        iterPForDeltaSetWithBase.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);
        postProcessBlock(iterDecompBlock, _blockSize);        
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
    private int getBlockIndex(int docIdIndex) {
      return docIdIndex >> BLOCK_INDEX_SHIFT_BITS;
    }

    // hy:  to call this func, make sure baseListForOnlyCompBlock is not empty and  we must be able to find the target in this function, otherwise something must be wrong
    // hy: I did not put the logic of this inside the func is because i want to make this func simple logic
    private int advanceToTargetInTheFollowingCompBlocks(int target, int startBlockIndex)
    {
      // searching from the current block
      int iterBlockIndex = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, startBlockIndex, baseListForOnlyCompBlocks.size()-1, target);
      
      if(iterBlockIndex < 0)
      {
        System.err.println("ERROR: advanceToTargetInTheFollowingCompBlocks(): Impossible, we must be able to find the block");
      }
      
      iterPForDeltaSetWithBase.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);        
      postProcessBlock(iterDecompBlock, _blockSize);
      
      int offset = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, 0, _blockSize-1, target);
      if(offset < 0)
      {
        System.err.println("ERROR: case 2: advanceToTargetInTheFollowingCompBlocks(), Impossible, we must be able to find the target" + target + " in the block " + iterBlockIndex);
      }
      cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + offset;
      return iterDecompBlock[offset];
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
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      {
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      {// hy: if ptr already ptrs to the end; or its next position ptrs to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      // hy: the expected behavior is to find the first element AFTER the current cursor, who is equal or larger than target
      if(target <= lastAccessedDocId)
      {
        target = lastAccessedDocId + 1;
      }
      
      int iterBlockIndex = getBlockIndex(cursor);
      int offset = cursor % _blockSize;
      
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
            lastAccessedDocId = currentNoCompBlock[offset];            
            cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + offset;
            return lastAccessedDocId;
          }                   
          else
          {
            cursor = totalDocIdNum; // hy: to avoid the repeated lookup next time once it reaches the end of the sequence
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
          lastAccessedDocId = advanceToTargetInTheFollowingCompBlocks(target, iterBlockIndex);
          return lastAccessedDocId;
        }
        else // case 3: offset > 0, hy: the current block has been decompressed, so, first test the first block; and then do sth like case 2 
        {
          if(target <= baseListForOnlyCompBlocks.get(iterBlockIndex))
          {
            //System.out.println("target: " + target + " is <= baseListForOnlyCompBlocks.get(iterBlockIndex)" + ", iterBlockIndex:" + iterBlockIndex + ": " + baseListForOnlyCompBlocks.get(iterBlockIndex));
            offset = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, offset, _blockSize-1, target);
            
            if(offset < 0)
            {
              System.err.println("case 3: Impossible, we must be able to find the target " + target + " in the block" + iterDecompBlock + ", offset: " + offset);
              //printArray(iterDecompBlock, 0, _blockSize-1);
            }
            cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS)  + offset;
            lastAccessedDocId = iterDecompBlock[offset];
            return lastAccessedDocId;
          }
          else // hy: there must exist other comp blocks between the current block and noComp block since target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)
          { 
            lastAccessedDocId = advanceToTargetInTheFollowingCompBlocks(target, iterBlockIndex);
            return lastAccessedDocId;
          }
        }        
      }
    
      lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
      return lastAccessedDocId; 
 }
 
  
    private void printSet() 
    {
       for (int i = 0; i < _blockSize; i++) 
       {
          iterPForDeltaSetWithBase.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(i), _blockSize);
          postProcessBlock(iterDecompBlock, _blockSize);
          System.out.print(iterDecompBlock + ",");
        }
     }

    public int getCursor() {
      return cursor;
    }

  } // hy: end of Iterator
}
