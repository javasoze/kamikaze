package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.utils.CompResult;
import com.kamikaze.docidset.utils.Conversion;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.PForDeltaIntSegmentArray;
import com.kamikaze.pfordelta.PForDelta;

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
   
  transient private IntArray baseListForOnlyCompBlocks; // the base lists for skipping
  transient private int[] currentNoCompBlock;  // the memory used to store the uncompressed elements. Once the block is full, all its elements are compressed into sequencOfCompBlock and the block is cleared.
  transient private int sizeOfCurrentNoCompBlock = 0; // the number of uncompressed elements that is hold in the currentNoCompBlock  
  
  private int version = 1;
  
  public PForDeltaDocIdSet() {
    sequenceOfCompBlocks = new PForDeltaIntSegmentArray();
    baseListForOnlyCompBlocks = new IntArray();
    currentNoCompBlock = new int[_blockSize];
    sizeOfCurrentNoCompBlock = 0;
    compressedBitSize = 0;
  }
  
  public PForDeltaDocIdSet(int batchSize) {
    this();
    if(_blockSize != batchSize)
    {
      currentNoCompBlock = new int[batchSize];
    }
    sizeOfCurrentNoCompBlock = 0;
    _blockSize = batchSize;      
  }
  
  public static PForDeltaDocIdSet deserialize(byte[] bytesData, int offset) throws IOException
  {
    PForDeltaDocIdSet res = new PForDeltaDocIdSet();
    
    // 1. version
    res.version = Conversion.byteArrayToInt(bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 2. blockSize
    int blkSize = Conversion.byteArrayToInt(bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    if(res._blockSize != blkSize)
    {
      res._blockSize = blkSize;
      res.currentNoCompBlock = new int[res._blockSize];
    }
    
    // 3. lastAdded
    res.lastAdded = Conversion.byteArrayToInt(bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 4. totalDocIdNum
    res.totalDocIdNum = Conversion.byteArrayToInt(bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 5. compressedBitSize
    res.compressedBitSize = Conversion.byteArrayToLong(bytesData, offset);
    offset += Conversion.BYTES_PER_LONG;
    
    // 6. base (skipping info)
    res.baseListForOnlyCompBlocks = IntArray.newInstanceFromBytes(bytesData, offset);
    offset += (IntArray.getSerialIntNum(res.baseListForOnlyCompBlocks) * Conversion.BYTES_PER_INT);
    
    // 7. the last block (uncompressed) 
    int noCompBlockSize = Conversion.byteArrayToInt(bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    for(int i=0; i<noCompBlockSize; i++)
    {
      res.currentNoCompBlock[i] = Conversion.byteArrayToInt(bytesData, offset);
      offset += Conversion.BYTES_PER_INT;
    }
    
    // 8. compressed blocks
    res.sequenceOfCompBlocks = PForDeltaIntSegmentArray.newInstanceFromBytes(bytesData, offset);
    offset += (PForDeltaIntSegmentArray.getSerialIntNum(res.sequenceOfCompBlocks) * Conversion.BYTES_PER_INT);
    
    // 9. checksum
    Checksum digest = new CRC32();
    digest.update(bytesData, 0, offset);
    long checksum = digest.getValue();
    
    long receivedChecksum = Conversion.byteArrayToLong(bytesData, offset);
    
    if(receivedChecksum != checksum)
    {
      throw new IOException("serialization error: check sum does not match: ");
    }
    
    return res;
  }
  
  public static byte[] serialize(PForDeltaDocIdSet pForDeltaDocIdSet)
  {
    int versionNumInt = 1;
    int blockSizeNumInt = 1;
    int checksumInt = 2; // checksum is long = 2 ints
    int lastAddedNumInt = 1;
    int totalDocIdNumInt = 1;
    int compressedBitsNumInt = 2; // long = 2 ints
    
    int baseListForOnlyComnpBlocksNumInt= IntArray.getSerialIntNum(pForDeltaDocIdSet.baseListForOnlyCompBlocks);
    int currentNoCompBlockBlockNumInt = 1 + pForDeltaDocIdSet.sizeOfCurrentNoCompBlock;
    
    int seqCompBlockIntNum = PForDeltaIntSegmentArray.getSerialIntNum(pForDeltaDocIdSet.sequenceOfCompBlocks);
    
    // plus the hashCode for all data
    int totalNumInt = versionNumInt + blockSizeNumInt + lastAddedNumInt + totalDocIdNumInt + compressedBitsNumInt +  
                      baseListForOnlyComnpBlocksNumInt + currentNoCompBlockBlockNumInt + seqCompBlockIntNum + checksumInt;
    
    byte[] bytesData = new byte[(totalNumInt+1)*Conversion.BYTES_PER_INT];  // +1 because of totalNumInt itself
    
    int offset = 0;
    
    // 0. totalNumInt
    Conversion.intToByteArray(totalNumInt, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    int startOffset = offset;
    // 1. version
    Conversion.intToByteArray(pForDeltaDocIdSet.version, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 2. blockSize
    Conversion.intToByteArray(pForDeltaDocIdSet._blockSize, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 3. lastAdded
    Conversion.intToByteArray(pForDeltaDocIdSet.lastAdded, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 4. totalDocIdNum
    Conversion.intToByteArray(pForDeltaDocIdSet.totalDocIdNum, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    // 5. compressedBitSize
    Conversion.longToByteArray(pForDeltaDocIdSet.compressedBitSize, bytesData, offset);
    offset += Conversion.BYTES_PER_LONG;
    
    // 6. base (skipping info)
    int baseIntNum = IntArray.convertToBytes(pForDeltaDocIdSet.baseListForOnlyCompBlocks, bytesData, offset);
    offset += (baseIntNum * Conversion.BYTES_PER_INT);

    // 7. the last block (uncompressed) 
    Conversion.intToByteArray(pForDeltaDocIdSet.sizeOfCurrentNoCompBlock, bytesData, offset);
    offset += Conversion.BYTES_PER_INT;
    for(int i=0; i<pForDeltaDocIdSet.sizeOfCurrentNoCompBlock; i++)
    {
      Conversion.intToByteArray(pForDeltaDocIdSet.currentNoCompBlock[i], bytesData, offset);
      offset += Conversion.BYTES_PER_INT;
    }
    
    // 8. compressed blocks
    PForDeltaIntSegmentArray.convertToBytes(pForDeltaDocIdSet.sequenceOfCompBlocks, bytesData, offset);
    offset += (seqCompBlockIntNum*Conversion.BYTES_PER_INT); 
    
    // 9. checksum
    Checksum digest = new CRC32();
    digest.update(bytesData, startOffset, offset-startOffset);
    long checksum = digest.getValue();
    
    Conversion.longToByteArray(checksum, bytesData, offset);
    
    return bytesData;
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
    
    int[] baseArray = (int[])inStrm.readObject();
    baseListForOnlyCompBlocks = new IntArray();
    for(int i=0; i<baseArray.length; ++i)
    {
      baseListForOnlyCompBlocks.add(baseArray[i]);
    }
    
    currentNoCompBlock = (int[])inStrm.readObject();
    sizeOfCurrentNoCompBlock = currentNoCompBlock.length;
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
    // this func is in PForDeltaDocIdSet instead of in PForDeltaDocIdSetIterator, therefore it cannot use iterBlockIndex, cursor, etc.
    int[] myDecompBlock = new int[_blockSize]; 
    if(size()==0 || sizeOfCurrentNoCompBlock==0)
      return false;
    
    int lastId = currentNoCompBlock[sizeOfCurrentNoCompBlock-1];
    if(target > lastId)
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

    // baseListForOnlyCompBlocks.size() must then >0
   int iterDecompBlock = binarySearchInBaseListForBlockThatMayContainTarget(baseListForOnlyCompBlocks, 0, baseListForOnlyCompBlocks.size()-1, target);
   if(iterDecompBlock<0)
     return false;
   
  // compBlockWithBase.decompressOneBlock(curDecompBlock, sequenceOfCompBlocks.get(iterDecompBlock), _blockSize);
   //compBlockWithBase.decompressOneBlock(myDecompBlock, sequenceOfCompBlocks.get(iterDecompBlock), _blockSize);
   PForDelta.decompressOneBlock(myDecompBlock, sequenceOfCompBlocks.get(iterDecompBlock), _blockSize);
   
   int idx ;
   lastId = myDecompBlock[0];
   if (lastId == target) return true;
   
   // searching while doing prefix sum (to get docIds instead of d-gaps)
   for(idx = 1; idx<_blockSize; ++idx)
   {
     lastId += (myDecompBlock[idx]+1);
     if (lastId >= target)
       break;
   }
   if(idx == _blockSize)
     return false;
   return (lastId == target);
   
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
    // estimated size of the serialized object in Bytes
    // blobsize * numberofelements * EstimatedBitsPerInteger ( this factor is achieved from experiments)
    optimize();
    long estimatedBitsPerInteger;
    if(totalDocIdNum < 100)
    {
      estimatedBitsPerInteger = 320;
    }
    else if(totalDocIdNum < 200)
    {
      estimatedBitsPerInteger = 114;
    }
    else if(totalDocIdNum < 400)
    {
      estimatedBitsPerInteger = 66;
    }
    else if(totalDocIdNum < 800)
    {
      estimatedBitsPerInteger = 43;
    }
    else if(totalDocIdNum < 1600)
    {
      estimatedBitsPerInteger = 31;
    }
    else if(totalDocIdNum < 3200)
    {
      estimatedBitsPerInteger = 24;
    }
    else if(totalDocIdNum < 6400)
    {
      estimatedBitsPerInteger = 20;
    }
    else if(totalDocIdNum < 12800)
    {
      estimatedBitsPerInteger = 17;
    }
    else if(totalDocIdNum < 25600)
    {
      estimatedBitsPerInteger = 15;
    }
    else if(totalDocIdNum < 51200)
    {
      estimatedBitsPerInteger = 14;
    }
    else
    {
      estimatedBitsPerInteger = 11;
    }
    
    long sizeBytes = (sequenceOfCompBlocks.length() * _blockSize *estimatedBitsPerInteger)>>>3;
    return sizeBytes;
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
  private int totalSequenceSize()
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
   * Add an array of document to this set, starting from the index start and ending at the index end
   * 
   */
  @Override
  public void addDocs(int[] docids, int start, int len) throws IOException
  {
    if(totalDocIdNum==0)
    {
      initSet();
    }
 
    if((len + sizeOfCurrentNoCompBlock)<=_blockSize)
    {
      System.arraycopy(docids, start, currentNoCompBlock, sizeOfCurrentNoCompBlock, len);
      sizeOfCurrentNoCompBlock += len;
    }
    else
    {
      // the first block
      int copyLen = _blockSize - sizeOfCurrentNoCompBlock;
      System.arraycopy(docids, start, currentNoCompBlock, sizeOfCurrentNoCompBlock, copyLen);
      sizeOfCurrentNoCompBlock = _blockSize;
      baseListForOnlyCompBlocks.add(currentNoCompBlock[_blockSize-1]);
      CompResult compRes = PForDeltaCompressCurrentBlock();
      //CompResult compRes = PForDeltaCompressCurrentBlock(currentNoCompBlock, 0, _blockSize);
      if(compRes.getCompressedBlock() == null)
      {
        throw new IOException("ERROR in compressing the first block");
      }
      compressedBitSize += compRes.getCompressedSize();      
      sequenceOfCompBlocks.add(compRes.getCompressedBlock());
      

      // the middle blocks
      int leftLen = len - copyLen;
      int newStart = start + copyLen;
      while(leftLen > _blockSize)
      {
        baseListForOnlyCompBlocks.add(docids[newStart+_blockSize-1]);
        System.arraycopy(docids, newStart, currentNoCompBlock, 0, _blockSize);
        compRes = PForDeltaCompressCurrentBlock();
        //compRes = PForDeltaCompressCurrentBlock(currentNoCompBlock, 0, _blockSize);
        if(compRes.getCompressedBlock() == null)
        {
          throw new IOException("ERROR in compressing middle blocks");
        }
        compressedBitSize += compRes.getCompressedSize();      
        sequenceOfCompBlocks.add(compRes.getCompressedBlock());
        
        leftLen -= _blockSize;
        newStart += _blockSize;
      }
      
      // the last block
      if(leftLen > 0)
      {
        System.arraycopy(docids, newStart, currentNoCompBlock, 0, leftLen);
      }
      sizeOfCurrentNoCompBlock = leftLen;
    }
    
    lastAdded = docids[start+len-1];
    totalDocIdNum += len;
  }

  
  /**
   * Add document to this set
   * 
   */
  public void addDoc(int docId) throws IOException
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
      
      if(compRes == null)
      {
        throw new IOException("ERROR in compressing ");
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
        throw new IOException("Error inserting DOC:" + docId);
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
  private CompResult PForDeltaCompressOneBlock(int[] srcData)
  {    
    int[] compBlock = PForDelta.compressOneBlockOpt(srcData, _blockSize);
    CompResult res = new CompResult();
    res.setCompressedSize(compBlock.length<<5);
    res.setCompressedBlock(compBlock);
    return res;
  }
   
  /**
   *  Estimated the compressed size of one block of integers using PForDelta
   * 
   */
  private int PForDeltaEstimateCompSize(int[] srcData, int b)
  {    
    return PForDelta.estimateCompressedSize(srcData, b, _blockSize);
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
   * Prefix Sum
   * 
   */
  private void preProcessBlockOpt(int[] block, int start, int len)
  {
    for(int i=start+len-1; i>start; --i)
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
    
    // return the compressed data achieved from the best b
    CompResult finalRes = PForDeltaCompressOneBlock(currentNoCompBlock);
    return finalRes;  
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
        PForDelta.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);
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
      
      PForDelta.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);
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
      
      PForDelta.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(iterBlockIndex), _blockSize);
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
    
    private void printSet() 
    {
       for (int i = 0; i < _blockSize; i++) 
       {
          PForDelta.decompressOneBlock(iterDecompBlock, sequenceOfCompBlocks.get(i), _blockSize);
          postProcessBlock(iterDecompBlock, _blockSize);
          System.out.print(iterDecompBlock + ",");
        }
     }

  } // end of Iterator
}
