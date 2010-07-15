package com.kamikaze.docidset.impl;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.compression.PForDeltaSetNoBase;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.CompResult;

/**
 * Doc id set wrapper around PForDeltaSetNoBase 
 * 
 * 
 * @author abhasin
 * 
 */
public class PForDeltaDocIdSet extends PForDeltaAbstractDocSet implements Serializable {
  //hy: use adaptor (the object of the class PForDeltaSetNoBase) 
  private static final long serialVersionUID = 1L;

  /**
   * Utility Object compression.
   */
  private PForDeltaSetNoBase compressedBlocksNoBase = new PForDeltaSetNoBase();

  public PForDeltaDocIdSet() {  
    super();
    compressedBitSize = 0;    
  }
  
  public PForDeltaDocIdSet(int batchSize) {
    this();
    this.BATCH_SIZE = batchSize;      
  }
  
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
    return new PForDeltaSetNoBase().decompress(compressedBlock);
  }

  protected int binarySearchForBlockThatMayContainTarget(IntArray in, int start, int end, int target)
  {   
    int bn = binarySearchForFirstElementLargerThanTarget(in, start, end, target);
    if(bn > start)
      return bn-1;
    
    return -1;
  }
  

  protected int binarySearchForFirstElementLargerThanTarget(IntArray in, int start, int end, int target)
  {   
    int mid;
    if(in.get(end)<=target)
      return end;
    
    while(start < end)
    {
      mid = (start + end)/2;
      if(in.get(mid) <= target)
        start = mid+1;
      else
        end = mid;
    }
    // hy: start == end;
    if(in.get(start) > target)
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
    if(size()==0)
      return false;
    //Short Circuit case where its not in the set at all
    if(target>lastAdded || target<baseList.get(0))
    {   
      //System.out.println("Time to perform BinarySearch for:"+val+":"+(System.nanoTime() - time));
      return false;
    }  
    else if(target>=baseList.get(sequenceOfCompBlocks.size()))
    {      // hy: in the decompBlock
        int pos = binarySearchForTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target);
       
        if(pos<0) // hy: not found
          return false;
        
        return true;
    }
    else
    {      // We are in the compressed space
      if(baseList.size() == 0)
        return false;
      
      int posBlock = binarySearchForBlockThatMayContainTarget(baseList, 0, baseList.size()-1, target);
      if(posBlock >= 0)
      {
        compressedBlocksNoBase.setParam(0, BATCH_SIZE);
        int[] decompBlock = compressedBlocksNoBase.decompressOneBlock(sequenceOfCompBlocks.get(posBlock));
        postProcessBlock(decompBlock, BATCH_SIZE);
        
        int pos = binarySearchForTarget(decompBlock, 0, BATCH_SIZE-1, target);
        if(pos<0) // hy: not found
          return false;
        
        return true;
      }
    }
    return false;
  }

  
  @Override
  public void optimize()
  {
    //Trim the baselist to size
    this.baseList.seal();
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
    int headInBytes = baseList.length()*4*2; // 1 int for storing b and expNum; the other int is for storing base
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
    /**
     * Address bits
     * 
     */
    int BATCH_INDEX_SHIFT_BITS;
    /**
     * retaining Offset from the list of blobs from the iterator pov
     * 
     */
    int cursor = -1;

    /**
     * Current iterating batch index.
     * 
     */
    int iterBlockIndex = -1;

    /**
     * Current iterating offset.
     * 
     */
    int offset = 0;

    /**
     * doc() returned
     * 
     */
    int lastReturn = -1;

    /**
     * size of the set
     * 
     */
    int sequenceSize = size();

    int compBlockNum=0;
    
    int[] iterDecompBlock = null;
   
    PForDeltaSetNoBase iterPForDeltaSetNoBase = new PForDeltaSetNoBase();

    PForDeltaDocIdSetIterator() {
      super();
      compBlockNum = sequenceOfCompBlocks.size();
      iterBlockIndex = 0;
      offset = 0;
      cursor = -1;
      // hy: BATCH_INDEX_SHIFT_BITS = log2(BATCH_SIZE); assuming BATCH_SIZE = 2^k, e.g., BATCH_SIZE=32 =2^5 ; => BATCH_INDEX_SHIFT_BITS = 5 bits = lg(32) = bitShiftLoopNum-1
      // that is why i is initialized as -1, then we can get blockIndex = (docIdIndex >> BATCH_INDEX_SHIFT_BITS); for example, in the above example,
      // if docIdIndex = 32, it belongs to the No ((32>>5)=1) block, that is, the 2nd block (the first block is No 0). 
      int i=-1;
      for(int x=BATCH_SIZE; x>0; ++i, x>>>=1);  
      BATCH_INDEX_SHIFT_BITS = i;
      System.out.println("BATCH_INDEX_SHIFT_BITS" + BATCH_INDEX_SHIFT_BITS);
      // hy: assume that b=31 results in 0 exps, in this Iterator, we do not need to set b and expCount since
      // we do not need to compress one block. In contrast, we only need to decompress one block.
      // However, we still need to set BATCH_SIZE.
      iterPForDeltaSetNoBase.setParam(31, BATCH_SIZE);  
    }

    @Override
    public int docID() {
      return lastReturn;
    }

    /**
     * Method to allow iteration in decompressed form
     
    public int get(OpenBitSet set, int index) {
      return compressedSet.get(set, index);
    }*/
    
    /**
     * Method to allow iteration in decompressed form
     */
//    public int get(int[] compBlock, int index) {
//      return iterPForDeltaSetNoBase.get(compBlock, index);
//    }

    @Override
    public int nextDoc() 
    {
      // hy: return docId instead of d-gap
      // increment the cursor and check if it falls in the range for the
      // number of batches, if not return false else, its within range
      
      if (++cursor < sequenceSize)
      {   
          iterBlockIndex = batchIndex(cursor);
          //System.out.println("iterBlockIndex:" + iterBlockIndex + ", compBlockNum" + compBlockNum + ",cursor:" + cursor + ",sequenceSize:" + sequenceSize + ",offset" + offset);
          if(iterBlockIndex == compBlockNum) // hy: in the currentNoCompBlock[] array which has never been compressed yet and therefore not added into sequenceOfCompBlocks yet.
          {           
              lastReturn = currentNoCompBlock[offset];
          }
          else if(offset == 0) // hy: that is, iterBlockIndex<compBlockNum && offset == 0; hy: start of the next comp block (need to decompress the block first)
          {
            //iterBlockIndex = batchIndex(cursor);
            //System.out.println("again, iterBlockIndex:" + iterBlockIndex + ", compBlockNum" + compBlockNum + ",cursor:" + cursor + ",sequenceSize:" + sequenceSize + ",offset" + offset);
            iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(iterBlockIndex));
            postProcessBlock(iterDecompBlock, BATCH_SIZE);
            if(iterDecompBlock == null)
            {
              System.err.println("ERROR: cannot decompress one block");
            }            
            lastReturn = iterDecompBlock[offset];
          }
          else // hy: in the lately decompressed block (different from blockIndex == compBlockNum where the currentNoCompBlock[] has never been compressed)
          {
            // hy: all decompressed number are docIds instead of d-gaps
            lastReturn = iterDecompBlock[offset];
          }
          offset = (offset+1) % BATCH_SIZE;         
            
          return lastReturn;
      }     
      lastReturn = DocIdSetIterator.NO_MORE_DOCS;
      return DocIdSetIterator.NO_MORE_DOCS;
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

    /**
     * Next need be called after skipping.
     * 
     */
    @Override
    public int advance(int target) {
      // hy: advance() will go to some docId that is after the lastReturn (that is, the last nextDoc()), return the first element that is >= target && after lastReturn, or return NO_MORE_DOCS if there is no such docIds.
      if (target <= lastReturn) //target = lastReturn + 1;
      {
        System.out.println("call advance, and target<= lastReturn, target:"+target+"lastReturn"+lastReturn);
        target = lastReturn + 1;
        //return nextDoc();
      }

     // hy: iterBlockIndex is achieved from nextDoc() (by default it is -1), it is the index of the block where the cursor is, and it
     // also means that iterDecompBlock[] stores the uncompressed block of the (iterBlockIndex)th block.
     int pos = -1;
     int posBlock = -1;
 
     iterBlockIndex = batchIndex(cursor);
     //if(iterBlockIndex<0)
     //  iterBlockIndex = 0;
     
     System.out.println("iterBlockIndex: " + iterBlockIndex + ", compBlockNum:" + compBlockNum + ", cursor:" + cursor);
     if(cursor<0)
     {
       System.out.println("first decomp");
       posBlock = binarySearchForBlockThatMayContainTarget(baseList, 0, baseList.size()-1, target);
      
       iterBlockIndex = posBlock;
       if(posBlock == compBlockNum)
       {
         System.out.println("first decomp:in the noComp block, posBlock: " + posBlock);
         iterBlockIndex = posBlock;
         
         //System.err.println("the currentNoCompBlock should have been taken care by case 1, cursor:" + cursor + "posBlock:" + posBlock + "compBlockNum" + compBlockNum);
       }
       else
       {
         System.out.println("first decomp: in the " + posBlock + " block");
         iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(iterBlockIndex));
         postProcessBlock(iterDecompBlock, BATCH_SIZE);
         
       } 
       // hy: advance cursor to the first one of the found block
       cursor = BATCH_SIZE * iterBlockIndex;
     }
     
     if(iterBlockIndex == compBlockNum || (target >= baseList.get(baseList.size()-1)))
     { 
       // hy: case 1: check the uncompressed block, i.e., the currrentNoCompBlock[]
       posBlock = iterBlockIndex;
       pos = binarySearchForFirstElementEqualOrLargerThanTarget(currentNoCompBlock, 0, sizeOfCurrentNoCompBlock-1, target);
       if(pos>=0)
       {         
         System.out.println("found at pos" + pos + ", which is " + currentNoCompBlock[pos] + " against the target of " + target);
         lastReturn = currentNoCompBlock[pos];
       }
     }     
     else if(iterBlockIndex>=0 && iterBlockIndex+1<compBlockNum)
     {
       // hy: case 2: in the the (iterBlockIndex)th block, which is already decompressed into iterDecompBlock[]
       //     this case including the case where compBlockNum = 0 (i.e., the very beginning, no decomp at all), where iterBlockIndex is also 0, so, they are also equal 
       posBlock = iterBlockIndex;
       if(baseList.get(iterBlockIndex+1)>target)
       {
         pos = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, 0, BATCH_SIZE-1, target);
         if(pos>=0)
         {          
           lastReturn = iterDecompBlock[pos];
         }
         else
         { // hy: then must be the base of the next compressed block
           iterBlockIndex++;
           iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(iterBlockIndex));
           lastReturn = iterDecompBlock[0];
           pos = 0;
         }
       }       
     }
     else // hy: case 3: check other comp blocks , first find the block using binary search on the baseList, then decomp the block, then do binary search within the block
     { // hy: including the case where cursor<0
        posBlock = binarySearchForBlockThatMayContainTarget(baseList, iterBlockIndex, baseList.size()-1, target);
        
        System.out.println("baseList:" + baseList + ",posBlock: " + posBlock + ",target: " + target);
        if(posBlock == compBlockNum)
        {
          System.out.println("ERROR: should have been handled above when (target >= baseList.get(baseList.size()-1)" +
          		"");
          //System.err.println("the currentNoCompBlock should have been taken care by case 1, cursor:" + cursor + "posBlock:" + posBlock + "compBlockNum" + compBlockNum);
        }
        if(posBlock >= 0)
        {
          iterBlockIndex = posBlock;
          System.out.println("sequenceOfCompBlocks.size():" + sequenceOfCompBlocks.size() + ",iterBlockIndex:" + iterBlockIndex);
          iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(iterBlockIndex));
          postProcessBlock(iterDecompBlock, BATCH_SIZE);
          pos = binarySearchForFirstElementEqualOrLargerThanTarget(iterDecompBlock, 0, BATCH_SIZE-1, target);
          if(pos>=0)
          {
            lastReturn = iterDecompBlock[pos];
          }
          else 
          { // iterBlockIndex must == compBlockNum
            pos = 0;
            lastReturn = currentNoCompBlock[pos];
          }
        }
     }
     
     if(posBlock<0 || pos<0)
     {
       lastReturn = DocIdSetIterator.NO_MORE_DOCS;
       return DocIdSetIterator.NO_MORE_DOCS;
     }
     else
     { 
       cursor = iterBlockIndex * BATCH_SIZE + pos;
       return lastReturn;
     }
    }
 
  
    private void printSet() 
    {
       for (int i = 0; i < BATCH_SIZE; i++) 
       {
          iterDecompBlock = iterPForDeltaSetNoBase.decompressOneBlock(sequenceOfCompBlocks.get(i));
          postProcessBlock(iterDecompBlock, BATCH_SIZE);
          System.out.print(iterDecompBlock + ",");
        }
     }

    public int getCursor() {
      return cursor;
    }

  } // hy: end of Iterator
    
  
  
}
