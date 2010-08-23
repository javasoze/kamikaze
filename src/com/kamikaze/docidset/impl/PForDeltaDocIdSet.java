package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.compression.PForDeltaWithBase;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.PForDeltaIntSegmentArray;
import com.kamikaze.docidset.utils.CompResult;

/**
 * This class implements the DocId set which is built on top of the optimized PForDelta algorithm (PForDeltaWithBase)
 * supporting various DocId set operations on the PForDelta-compressed data. 
 * 
 * 
 * @author hao yan
 * 
 */

public class PForDeltaDocIdSet extends DocSet implements Serializable {

  private static final long serialVersionUID = 1L;
 
  private PForDeltaIntSegmentArray sequenceOfCompBlocks; // segments of compressed data (each segment contains the compressed array of say, 256 integers)
  
  public static final int DEFAULT_BATCH_SIZE = 256; // default block size
  private static final int[] POSSIBLE_B = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,16,20}; // all possible values of b in PForDelta algorithm
 
  private int _blockSize = DEFAULT_BATCH_SIZE; // block size
  private int lastAdded = 0; // recently inserted/accessed element
  private int totalDocIdNum=0; // the total number of elemnts that have been inserted/accessed so far
  private long compressedBitSize=0; // compressed size in bits
   
  transient private PForDeltaWithBase compBlockWithBase = new PForDeltaWithBase(); // the PForDelta algorithm to compress a block
  transient private IntArray baseListForOnlyCompBlocks; // the base lists for skipping
  transient private int[] currentNoCompBlock;  // the memory used to store the uncompressed elements. Once the block is full, all its elements are compressed into sequencOfCompBlock and the block is cleared.
  transient private int sizeOfCurrentNoCompBlock = 0; // the number of uncompressed elements that is hold in the currentNoCompBlock  
  transient private int[] curDecompBlock; // temporary space to hold the decompressed data
  
  public PForDeltaDocIdSet() {
    sequenceOfCompBlocks = new PForDeltaIntSegmentArray();
    baseListForOnlyCompBlocks = new IntArray();
    currentNoCompBlock = new int[_blockSize];
    sizeOfCurrentNoCompBlock = 0;
    curDecompBlock = new int[_blockSize];
    
    compressedBitSize = 0;
  }
  
  public PForDeltaDocIdSet(int batchSize) {
    this();
    if(_blockSize < batchSize)
    {
      currentNoCompBlock = new int[batchSize];
      curDecompBlock = new int[batchSize];
    }
    sizeOfCurrentNoCompBlock = 0;
    _blockSize = batchSize;      
  }
 
  /**
   * Serialize the object manually
   * 
   */
  private void writeObject(ObjectOutputStream outStrm) throws IOException
  {
    outStrm.defaultWriteObject();
    
    int[] baseArray= new int[baseListForOnlyCompBlocks.size()];
    for(int i=0; i<baseListForOnlyCompBlocks.size(); i++)
    {
      baseArray[i] = baseListForOnlyCompBlocks.get(i);
    }
    outStrm.writeObject(baseArray);
    
    int[] noCompBlock = new int[sizeOfCurrentNoCompBlock];
    System.arraycopy(currentNoCompBlock, 0, noCompBlock, 0, sizeOfCurrentNoCompBlock);
    outStrm.writeObject(noCompBlock);
  }
  
  /**
   * Deserialize the object manually
   * 
   */
  private void readObject(ObjectInputStream inStrm) throws IOException, ClassNotFoundException
  {
    inStrm.defaultReadObject();
    
    curDecompBlock = new int[_blockSize];
    compBlockWithBase = new PForDeltaWithBase();
    
    int[] baseArray = (int[])inStrm.readObject();
    baseListForOnlyCompBlocks = new IntArray();
    for(int i=0; i<baseArray.length; ++i)
    {
      baseListForOnlyCompBlocks.add(baseArray[i]);
    }
    
    int[] noCompBlock = (int[])inStrm.readObject();
    sizeOfCurrentNoCompBlock = noCompBlock.length;
    currentNoCompBlock = new int[sizeOfCurrentNoCompBlock];
    System.arraycopy(noCompBlock, 0, currentNoCompBlock, 0, sizeOfCurrentNoCompBlock);
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
  
  public boolean findSyncItself(int target)
  { 
    int[] localCurDecompBlock = new int[_blockSize];
    PForDeltaWithBase localCompBlockWithBase = new PForDeltaWithBase();
    int localLastAdded;
    if(size()==0 || sizeOfCurrentNoCompBlock==0)
      return false;
    
    localLastAdded = currentNoCompBlock[sizeOfCurrentNoCompBlock-1];
    if(target > localLastAdded)
    {
      return false;
    }
    
    // hy: first search noComp block
    if(baseListForOnlyCompBlocks.size()==0 || target>baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))
    {
      int i;
      for(i=0; i<sizeOfCurrentNoCompBlock; ++i)
      {
        if(currentNoCompBlock[i] >= target)
          break;
      }
      if(i == sizeOfCurrentNoCompBlock) 
        return false;
      return currentNoCompBlock[i] == target; 
    }

   int iterDecompBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(iterDecompBlock<0)
     return false;
   
   localCompBlockWithBase.decompressOneBlock(localCurDecompBlock, sequenceOfCompBlocks.get(iterDecompBlock), _blockSize);
   int idx ;
   localLastAdded = localCurDecompBlock[0];
   if (localLastAdded == target) return true;
   
   for(idx = 1; idx<_blockSize; ++idx)
   {
     localLastAdded += (localCurDecompBlock[idx]+1);
     if (localLastAdded >= target)
       break;
   }
   if(idx == _blockSize)
     return false;
   return (localLastAdded == target);
   
  }
  
  @Override
  public boolean find(int target)
  { 
    // this func is in PForDeltaDocIdSet instead of in PForDeltaDocIdSetIterator, therefore it cannot use iterBlockIndex, cursor, etc.
    
    if(size()==0 || sizeOfCurrentNoCompBlock==0)
      return false;
    
    lastAdded = currentNoCompBlock[sizeOfCurrentNoCompBlock-1];
    if(target > lastAdded)
    {
      return false;
    }
    
    // first search noComp block
    if(baseListForOnlyCompBlocks.size()==0 || target>baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))
    {
      //if(binarySearchForTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target) >= 0)
        //return true;
      int i;
      for(i=0; i<sizeOfCurrentNoCompBlock; ++i)
      {
        if(currentNoCompBlock[i] >= target)
          break;
      }
      if(i == sizeOfCurrentNoCompBlock) 
        return false;
      return currentNoCompBlock[i] == target; 
    }

    // baseListForOnlyCompBlocks.size() must then >0
   int iterDecompBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(iterDecompBlock<0)
     return false;
   
   compBlockWithBase.decompressOneBlock(curDecompBlock, sequenceOfCompBlocks.get(iterDecompBlock), _blockSize);

   int idx ;
   lastAdded = curDecompBlock[0];
   if (lastAdded == target) return true;
   
   // searching while doing prefix sum (to get docIds instead of d-gaps)
   for(idx = 1; idx<_blockSize; ++idx)
   {
     lastAdded += (curDecompBlock[idx]+1);
     if (lastAdded >= target)
       break;
   }
   if(idx == _blockSize)
     return false;
   return (lastAdded == target);
   
  }
  
  /**
   * Implements the same functionality as find() except by decompressing one single element at a time (instead of decompressing the entire block and then search in the resulting block)
   * 
   */
  public boolean findUsingSingleElementDecompresion(int target)
  { 
    // this func is in PForDeltaDocIdSet instead of in PForDeltaDocIdSetIterator, therefore it cannot use iterBlockIndex, cursor, etc.
    if(size()==0 || sizeOfCurrentNoCompBlock==0)
      return false;
    
    lastAdded = currentNoCompBlock[sizeOfCurrentNoCompBlock-1];
    if(target > lastAdded)
    {
      return false;
    }
    
    // first search noComp block
    if(baseListForOnlyCompBlocks.size()==0 || target>baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))
    {
      int i;
      for(i=0; i<sizeOfCurrentNoCompBlock; ++i)
      {
        if(currentNoCompBlock[i] >= target)
          break;
      }
      if(i == sizeOfCurrentNoCompBlock) 
        return false;
      return currentNoCompBlock[i] == target; 
    }

   int iterDecompBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(iterDecompBlock<0)
     return false;
   
   int idx ;
   lastAdded = compBlockWithBase.decompressOneElement(curDecompBlock,sequenceOfCompBlocks.get(iterDecompBlock), 0, _blockSize);
   if (lastAdded == target) return true;
   
   for(idx = 1; idx<_blockSize; ++idx)
   {
     lastAdded += (compBlockWithBase.decompressOneElement(curDecompBlock,sequenceOfCompBlocks.get(iterDecompBlock), idx, _blockSize)+1);
     if (lastAdded >= target)
       break;
   }
   if(idx == _blockSize)
     return false;
   return (lastAdded == target);
   
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
    // the size returned by this function is not precise and not guaranteed to be correct
    // 64 is the overhead for an int array
    // blobsize * numberofelements * 1.1 (Object Overhead, assuming each element is encoded in about 1.1 bytes)
    // batch_size * 4 + int array overhead
    // P4dDocIdSet Overhead 110
    optimize();
    int headInBytes = baseListForOnlyCompBlocks.length()*4*2; // 1 int for storing b and expNum; the other int is for storing base
    return (long) (headInBytes + 64 +sequenceOfCompBlocks.length()*_blockSize*1.1 + _blockSize*4 + 24 + 110);
    
  }
  
  /**
   * The total number of elements in the compressed blocks
   * 
   */
  public int totalBlobSize()
  {
    return totalSequenceSize();
  }
  
  /**
   * The total number of elements in the compressed blocks
   * 
   */
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
   *  Flush the data left in the currentNoCompBlock into the compressed data (never called)
   * 
   */
  public void flush(int docId)
  {
    CompResult compRes = PForDeltaCompressCurrentBlock();
    compressedBitSize += compRes.getCompressedSize();      
    sequenceOfCompBlocks.add(compRes.getCompressedBlock());
  }
  
  /**
   *  Compress one block of integers using PForDelta
   * 
   */
  private CompResult PForDeltaCompressOneBlock(int[] srcData, int b, boolean flag)
  {    
    CompResult compRes = compBlockWithBase.compressOneBlock(srcData, b, _blockSize, flag);
    return compRes;
  }
   
  /**
   *  Estimated the compressed size of one block of integers using PForDelta
   * 
   */
  private CompResult PForDeltaEstimateCompSize(int[] srcData, int b)
  {    
    return compBlockWithBase.estimateCompSize(srcData, b, _blockSize);
  }
  
  
  private void initSet() {
    Arrays.fill(this.currentNoCompBlock, 0);    
  }
  

  /**
   *  Binary search in the base list for the block that may contain docId greater than or equal to the target 
   * 
   */
  private int binarySearchInBaseListForBlockThatMayContainTarget(IntArray in, int start, int end, int target)
  {   
    //the baseListForOnlyCompBlocks (in) contains all last elements of the compressed blocks. 
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
    if(in[start] >= target)
      return start;
    else
      return -1;
  }
  
  /**
   * Linear search for the first element that is equal to or larger than the target 
   */
  private int searchForFirstElementEqualOrLargerThanTarget(int in[], int start, int end, int target)
  {
    while(start <= end)
    {
      if(in[start] >= target)
        return start;
      start++;
    }
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
   * Compress the currentNoCompblock 
   * 
   */
  private CompResult PForDeltaCompressCurrentBlock()
  { 
    // find the best b that can lead to the smallest overall compressed size
    int currentB = POSSIBLE_B[0];   
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
  
    int BLOCK_INDEX_SHIFT_BITS; // floor(log(blockSize))
  
    int cursor = -1; // the current pointer of the input 
    int lastAccessedDocId = -1; //  the docId that was accessed of the last time called nextDoc() or advance(), therefore, it is kind of synced with the above three too

    int compBlockNum=0; // the number of compressed blocks
    transient int[] iterDecompBlock = new int[_blockSize]; // temporary storage for the decompressed data
    PForDeltaWithBase iterPForDeltaSetWithBase = new PForDeltaWithBase(); // PForDelta algorithm

    PForDeltaDocIdIterator() {
      super();
      compBlockNum = sequenceOfCompBlocks.size();
      cursor = -1;
      int i=-1;
      for(int x=_blockSize; x>0; ++i, x>>>=1);  
      BLOCK_INDEX_SHIFT_BITS = i;
    }

    @Override
    public int docID() {
      return lastAccessedDocId;
    }

    @Override
    public int nextDoc() 
    {
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      { //the pointer points past the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      { //: if the pointer points to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      int iterBlockIndex = getBlockIndex(cursor); // get the block No
      int offset = cursor % _blockSize; // sync offset with cursor
      
      if(iterBlockIndex == compBlockNum) // case 1: in the currentNoCompBlock[] array which has never been compressed yet and therefore not added into sequenceOfCompBlocks yet.
      { 
        lastAccessedDocId = currentNoCompBlock[offset];
      }
      // must be in one of the compressed blocks
      else if(offset == 0) // case 2: the comp block has been decompressed; 
      {
        iterPForDeltaSetWithBase.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);
        lastAccessedDocId = iterDecompBlock[offset];
      }
      else // case 3: in the recently decompressed block
      {
        lastAccessedDocId += (iterDecompBlock[offset]+1);
      }        
      return lastAccessedDocId;
    }

    @Override
    public int advance(int target) {
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      {//the pointer points past the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      {//: if the pointer points to the end
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      // the expected behavior is to find the first element AFTER the current cursor, who is equal or larger than target
      if(target <= lastAccessedDocId)
      {
        target = lastAccessedDocId + 1;
      }
      
      int iterBlockIndex = getBlockIndex(cursor);
      int offset = cursor % _blockSize;
      
      // if there is noComp block, check noComp block 
      // the next element is in currently in the last block , or currently not in the last block, but the target is larger than the last element of the last compressed block
      if(sizeOfCurrentNoCompBlock>0) // if there exists the last decomp block 
      {
        if(iterBlockIndex == compBlockNum || (baseListForOnlyCompBlocks.size()>0 && target > baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)))
        {   
          offset = binarySearchForFirstElementEqualOrLargerThanTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target);
          //offset = searchForFirstElementEqualOrLargerThanTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target);
          
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
      
      // if we cannot not find it in the noComp block, we check the comp blocks
      if(baseListForOnlyCompBlocks.size()>0 && target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))  
      {
        // for the following cases, it must exist in one of the comp blocks since target<= the last base in the comp blocks
        if(offset == 0)
        {
          // searching the right block from the current block to the last block
          lastAccessedDocId = advanceToTargetInTheFollowingCompBlocksNoPostProcessing(target, iterBlockIndex);
          //lastAccessedDocId = advanceToTargetInTheFollowingCompBlocksUsingLinearSearchAndSingleElementDecomp(target, iterBlockIndex);
          
          return lastAccessedDocId;
        }
        else // offset > 0, the current block has been decompressed, so, first test the first block; and then do sth like case 2 
        {
          if(target <= baseListForOnlyCompBlocks.get(iterBlockIndex))
          {
            while(offset < _blockSize)
            {
              //lastAccessedDocId += (iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,sequenceOfCompBlocks.get(iterBlockIndex), offset, _blockSize)+1);
              lastAccessedDocId += (iterDecompBlock[offset]+1); 
             
              if (lastAccessedDocId >= target) {
                break; 
              }
              offset++;
            }
            // offset = getNextLargerOrEqualTo(sequenceOfCompBlocks.get(iterBlockIndex), target);
            if (offset == _blockSize)
            {
              System.err.println("case 3: Impossible, we must be able to find the target " + target + " in the block, lastAccessedDocId: " + lastAccessedDocId + ", baseListForOnlyCompBlocks.get(iterBlockIndex): " + baseListForOnlyCompBlocks.get(iterBlockIndex) + "iterBlockIndex: " + iterBlockIndex);
            }
            
            cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS)  + offset;
           // lastAccessedDocId = iterDecompBlock[offset];
            
            return lastAccessedDocId;
          }
          else // hy: there must exist other comp blocks between the current block and noComp block since target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)
          { 
            lastAccessedDocId = advanceToTargetInTheFollowingCompBlocksNoPostProcessing(target, iterBlockIndex);
            //lastAccessedDocId = LS_advanceToTargetInTheFollowingCompBlocks(target, iterBlockIndex);
            return lastAccessedDocId;
          }
        }        
      }
    
      lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
      return lastAccessedDocId; 
 }
    

    public int getCursor() {
      return cursor;
    }
    
    /**
     * Implement the same functionality as nextDoc() except that each element is decompressed individually
     */
    private int nextDocSingle() 
    {
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      { 
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      {
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      int iterBlockIndex = getBlockIndex(cursor);
      int offset = cursor % _blockSize; 
      
      if(iterBlockIndex == compBlockNum) 
      { 
        lastAccessedDocId = currentNoCompBlock[offset];
      }
      else if(offset == 0) 
      {
        lastAccessedDocId = iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,sequenceOfCompBlocks.get(iterBlockIndex), offset, _blockSize);
       
        
      }
      else 
      {
        lastAccessedDocId += (iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,sequenceOfCompBlocks.get(iterBlockIndex), offset, _blockSize) +1);
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

    /**
     * Find the first element that is equal to or larger than the target in the (startBlockIndex)th compressed block 
     * Before this function is called, baseListForOnlyCompBlock must not be empty and the target must be able to be found in this function, otherwise something must be wrong
     * 
     */
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
      //int offset = searchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, 0, _blockSize-1, target);
      
      if(offset < 0)
      {
        System.err.println("ERROR: case 2: advanceToTargetInTheFollowingCompBlocks(), Impossible, we must be able to find the target" + target + " in the block " + iterBlockIndex);
      }
      cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + offset;
      return iterDecompBlock[offset];
    }
    
    /**
     * Implement the same functionality as  advanceToTargetInTheFollowingCompBlocks() except that this function do prefix sum during searching
     * 
     */
    private int advanceToTargetInTheFollowingCompBlocksNoPostProcessing(int target, int startBlockIndex)
    {
      // searching from the current block
      int iterBlockIndex = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, startBlockIndex, baseListForOnlyCompBlocks.size()-1, target);
      
      if(iterBlockIndex < 0)
      {
        System.err.println("ERROR: advanceToTargetInTheFollowingCompBlocks(): Impossible, we must be able to find the block");
      }
      
      iterPForDeltaSetWithBase.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);        
      lastAccessedDocId = iterDecompBlock[0];
      if (lastAccessedDocId >= target)
      {
        cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + 0;
        return lastAccessedDocId;
      }
   

      for (int offset=1; offset < _blockSize; ++offset) 
      {
        lastAccessedDocId += ( iterDecompBlock[offset]+1);
        if (lastAccessedDocId >= target) {
          cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + offset;
          return lastAccessedDocId;
        }
      }
      
      System.err.println("ERROR: case 2: advanceToTargetInTheFollowingCompBlocks(), Impossible, we must be able to find the target" + target + " in the block " + iterBlockIndex);
      return -1;
    }
    
    /**
     * Linear search (each time decompress one single element)
     * 
     */
    private int getNextLargerOrEqualTo(int[] compBlock, int target) {
      int idx = 0;
      lastAccessedDocId = iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,compBlock, idx, _blockSize);
      if (lastAccessedDocId >= target)
        return idx;
      idx++;

      for (idx=1; idx < _blockSize; idx++) 
      {
        lastAccessedDocId += (iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,compBlock, idx, _blockSize)+1);
        if (lastAccessedDocId >= target) {
          return idx;
        }
      }
      return -1;
    }
    
    /**
     * Implements the same functionality as advanceToTargetInTheFollowingCompBlocks() except using linear search and single element decompression each time
     * 
     */
    private int advanceToTargetInTheFollowingCompBlocksUsingLinearSearchAndSingleElementDecomp(int target, int startBlockIndex)
    {
      int iterBlockIndex = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, startBlockIndex, baseListForOnlyCompBlocks.size()-1, target);
      
      if(iterBlockIndex < 0)
      {
        System.err.println("ERROR: advanceToTargetInTheFollowingCompBlocks(): Impossible, we must be able to find the block");
      }
      
      int offset = getNextLargerOrEqualTo(sequenceOfCompBlocks.get(iterBlockIndex), target);
      if(offset < 0)
      {
        System.err.println("ERROR: advanceToTargetInTheFollowingCompBlocks(): Impossible, we must be able to find the element in the block");
        System.out.println("target: " + target + ", lastID: " + baseListForOnlyCompBlocks.get(iterBlockIndex));
      }
      
      cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS) + offset;
      return lastAccessedDocId;
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
     * Implements the same functionality as advance() except using single element decompression
     * 
     */
    public int advanceSingle(int target) {
      if(totalDocIdNum <= 0 || cursor == totalDocIdNum)
      {
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return lastAccessedDocId;
      }
      
      if(++cursor == totalDocIdNum)
      {
        lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }
      
      if(target <= lastAccessedDocId)
      {
        target = lastAccessedDocId + 1;
      }
      
      int iterBlockIndex = getBlockIndex(cursor);
      int offset = cursor % _blockSize;
      
      if(sizeOfCurrentNoCompBlock>0) 
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
            cursor = totalDocIdNum; // to avoid the repeated lookup next time once it reaches the end of the sequence
            lastAccessedDocId = DocIdSetIterator.NO_MORE_DOCS;
            return lastAccessedDocId;
          }
        }     
      }
      
      //  if did not find it in the noComp block, check the comp blocks
      if(baseListForOnlyCompBlocks.size()>0 && target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1))  
      {
        if(offset == 0)
        {
          lastAccessedDocId = advanceToTargetInTheFollowingCompBlocksUsingLinearSearchAndSingleElementDecomp(target, iterBlockIndex);
          return lastAccessedDocId;
        }
        else  
        {
          if(target <= baseListForOnlyCompBlocks.get(iterBlockIndex))
          {
            while(offset < _blockSize)
            {
              lastAccessedDocId += (iterPForDeltaSetWithBase.decompressOneElement(iterDecompBlock,sequenceOfCompBlocks.get(iterBlockIndex), offset, _blockSize)+1);
              if (lastAccessedDocId >= target) {
                break; 
              }
              offset++;
            }
            if (offset == _blockSize)
            {
              System.err.println("case 3: Impossible, we must be able to find the target " + target + " in the block" + iterDecompBlock + ", offset: " + offset);
            }
            cursor = (iterBlockIndex << BLOCK_INDEX_SHIFT_BITS)  + offset;
            return lastAccessedDocId;
          }
          else // hy: there must exist other comp blocks between the current block and noComp block since target <= baseListForOnlyCompBlocks.get(baseListForOnlyCompBlocks.size()-1)
          { 
            lastAccessedDocId = advanceToTargetInTheFollowingCompBlocksUsingLinearSearchAndSingleElementDecomp(target, iterBlockIndex);
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

  } // end of Iterator
}
