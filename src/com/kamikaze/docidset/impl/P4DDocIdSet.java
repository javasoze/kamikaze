package com.kamikaze.docidset.impl;

import java.io.Serializable;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.compression.P4DSetNoBase;
import com.kamikaze.docidset.utils.IntArray;

/**
 * Doc id set wrapper around P4DSet
 * 
 * 
 * @author abhasin
 * 
 */
public class P4DDocIdSet extends AbstractDocSet implements Serializable {
 
  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_B = 5;
  /**
   * Utitlity Object compression.
   */
  private P4DSetNoBase compressedSet = new P4DSetNoBase();

  /**
   * List for the base integer values of the compressed batches.
   */
  private IntArray baseList = null;

  
  
  public P4DDocIdSet() {
  
    baseList = new IntArray();
    compressedBits = 0;
    
  }
  
  public P4DDocIdSet(int batchSize) {
    this();
    this.BATCH_SIZE = batchSize;
    this.BATCH_OVER = batchSize / 20;

  }
  
  @Override
  public final boolean isCacheable() {
    return true;
  }

  @Override
  protected Object compress() {
    current[0] = 0;
    compressedSet.setParam(current_base, current_b, BATCH_SIZE,
        current_ex_count);
    baseList.add(current_base);
    return compressedSet.compressAlt(current);
  }

  /**
   * Method to decompress the entire batch
   * 
   * @param blob MyOpenBitSet
   * @return int array with decompressed segment of numbers
   */
  protected int[] decompress(MyOpenBitSet blob) {
    return new P4DSetNoBase().decompress(blob);
  }

  /**
   * Binary search
   * 
   * @param val
   * @param begin
   * @param end
   * @return index greater than or equal to the target. -1 if the target is out
   *         of range.
   */
  protected int binarySearchForNearest(int val, int begin, int end) {
    int mid = (begin + end) / 2;
    if (mid == end || (baseList.get(mid) <= val && baseList.get(mid + 1) > val))
      return mid;
    else if (baseList.get(mid) < val)
      return binarySearchForNearest(val, mid + 1, end);
    else
      return binarySearchForNearest(val, begin, mid);
  }

  protected int binarySearchForNearestAlt(int val, int begin, int end)
  {
    while(true)
    {

      int mid = (begin+end)/2;
      
      if(mid==end || (baseList.get(mid) <= val && baseList.get(mid + 1) > val))
        return mid;
    
      else if(baseList.get(mid) < val)
      {
        begin = mid+1;
        
      }
      else
      {
        end=mid;
      }
    }
    
  }
  
  class P4DDocIdSetIterator extends StatefulDSIterator implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Address bits
     * 
     */
    int ADDRESS_BITS = (int) (Math.log(BATCH_SIZE) / Math.log(2));

    /**
     * retaining Offset from the list of blobs from the iterator pov
     * 
     */
    int cursor = -1;

    /**
     * Current iterating batch index.
     * 
     */
    int bi = -1;

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
    int size = size();

    /**
     * Reference to the blob iterating
     * 
     */
    long[] ref = null;

    /**
     * Reference to the blob iterating
     * 
     */
    int blobSize = blob.size();
    
    
    P4DSetNoBase localCompressedSet = new P4DSetNoBase();
    


    P4DDocIdSetIterator() {
      super();
      localCompressedSet.setParam(0, DEFAULT_B, BATCH_SIZE, BATCH_OVER);
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
    public int get(long[] set, int index) {
      return localCompressedSet.get(set, index);
    }

    @Override
    public int nextDoc() {
      // increment the cursor and check if it falls in the range for the
      // number of batches, if not return false else, its within range
      if (++cursor < size) {

        // We are already in the array
        if (bi == blobSize) {
          if (offset == -1) {
            lastReturn = DocIdSetIterator.NO_MORE_DOCS;
            return DocIdSetIterator.NO_MORE_DOCS;
          } else
            lastReturn += current[offset++];
        }
        // if we are not in the array but on the boundary of a batch
        // update local blob and set params
        else if (offset == 0) {

          bi = batchIndex(cursor);

          if (bi < blobSize) {
            lastReturn = baseList.get(bi);
            ref = blob.get(bi);
            localCompressedSet.updateParams(ref);
            offset++;// cursor - (bi << ADDRESS_BITS);+1
          } else {
            // offset = 0;//cursor - (bi << ADDRESS_BITS);
            lastReturn = current[offset++];
          }
        } else {

          lastReturn += localCompressedSet.get(ref, offset);
          offset = (++offset) % BATCH_SIZE;
        }
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
    private int batchIndex(int index) {
      return index >> ADDRESS_BITS;
    }

    /**
     * Next need be called after skipping.
     * 
     */
    @Override
    public int advance(int target) {
      
      if (target <= lastReturn) target = lastReturn + 1;

      // NOTE : Update lastReturn.

      if (bi == blobSize || (bi + 1 < blobSize && target < baseList.get(bi + 1))) {
        while (nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
          if (lastReturn >= target) return lastReturn;
        }
        lastReturn = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }

      // If the target is outside the compressed space
      if (blobSize == 0 || target >= current[0]) {

        bi = blobSize;
        ref = null;

        offset = findAndUpdate(current, target);

        if (offset > 0) {
          cursor = blobSize * BATCH_SIZE + offset - 1;
          return lastReturn;
        }
        // We have gone over the batch boundary
        else if (offset == 0) {
          cursor = (blobSize + 1) * BATCH_SIZE;
          return lastReturn;
        }

        lastReturn = DocIdSetIterator.NO_MORE_DOCS;
        return DocIdSetIterator.NO_MORE_DOCS;
      }


      // This returns the blob where base value is less than the value looked
      // for.
      int index = binarySearchForNearest(target, bi, blobSize - 1);
      // Move both these further, as we are in this block, so that
      // doc() call works.
      bi = index;
      lastReturn = baseList.get(index);
      ref = blob.get(index);
      localCompressedSet.updateParams(ref);
        
      // find the nearest integer in the compressed space.
      offset = findAndUpdate(ref, target, lastReturn);

      if (offset < 0) {
        // oops we fell into the gap. This case happens when we land
        // in the gap between two batches. We can optimize this
        // step.
        if (++index < blobSize) {
          lastReturn = baseList.get(index);
          ref = blob.get(index);
          localCompressedSet.updateParams(ref);
        }
        else {
          lastReturn = current[0];
          ref = null;
        }
        bi = index;
        offset = 1;
      }

      cursor = bi * BATCH_SIZE + offset - 1;

      return lastReturn;
    }

   /* private void printSet(MyOpenBitSet test, int base) {
      try {
        int localBase = base;
        for (int i = 1; i < BATCH_SIZE; i++) {
          localBase += compressedSet.get(test, i);
          System.out.print(localBase + ",");
        }
      } catch (Exception e) {
        e.printStackTrace();
        int localBase = base;
        int testint[] = compressedSet.decompress(test);
        for (int i = 1; i < BATCH_SIZE; i++) {
          localBase += testint[i];
          System.out.print(localBase + ",");
        }
      }

    }*/
    
    private void printSet(long[] test, int base) {
      try {
        int localBase = base;
        for (int i = 1; i < BATCH_SIZE; i++) {
          localBase += localCompressedSet.get(test, i);
          System.out.print(localBase + ",");
        }
      } catch (Exception e) {
        e.printStackTrace();
        int localBase = base;
        int testint[] = localCompressedSet.decompress(test);
        for (int i = 1; i < BATCH_SIZE; i++) {
          localBase += testint[i];
          System.out.print(localBase + ",");
        }
      }

    }

    /**
     * Find the element in the compressed set
     * 
     * @param next
     * @param target
     * @param base
     * @return
     */
    private int findAndUpdate(long[] next, int target, int base) {
      lastReturn = base;
      if (lastReturn >= target)
        return 1;

      for (int i = 1; i < BATCH_SIZE; i++) {
        // System.out.println("Getting "+i);
        // System.out.flush();

        lastReturn += localCompressedSet.get(next, i);
        if (lastReturn >= target) {
          // if(i==127)
          return (i + 1) % BATCH_SIZE;
        }
      }
      return -1;
    }
    
    /**
     * Find the element in the compressed set
     * 
     * @param next
     * @param target
     * @param base
     * @return
     
    private int findAndUpdate(MyOpenBitSet next, int target, int base) {
      lastReturn = base;
      if (lastReturn >= target)
        return 1;

      for (int i = 1; i < BATCH_SIZE; i++) {
        // System.out.println("Getting "+i);
        // System.out.flush();

        lastReturn += compressedSet.get(next, i);
        if (lastReturn >= target) {
          // if(i==127)
          return (i + 1) % BATCH_SIZE;
        }
      }
      return -1;
    }*/

    /**
     * Find the element in the set and update parameters.
     * 
     */
    private int findAndUpdate(int[] array, int target) {

      if(array==null)
          return -1;
      
      lastReturn = array[0];
      if (lastReturn >= target)
        return 1;

      for (int i = 1; i < current_size; i++) {
        lastReturn += array[i];

        if (lastReturn >= target)
          return (i + 1) % BATCH_SIZE;
      }
      return -1;

    }

    public int getCursor() {
      return cursor;
    }

  }

  public P4DDocIdSetIterator iterator() {
    return new P4DDocIdSetIterator();
  }

  @Override
  public int findWithIndex(int val) {
    
    P4DDocIdSetIterator dcit = new P4DDocIdSetIterator();
    
    int docid = dcit.advance(val);
    if (docid == val)
      return dcit.getCursor();
    return -1;
  }
  
  @Override
  public boolean find(int val)
  {
    
    long time = System.nanoTime();
    int local = 0; 
    
    if(size()==0)
      return false;
    //Short Circuit case where its not in the set at all
    if(val>lastAdded||val<baseList.get(0))
    {   
      //System.out.println("Time to perform BinarySearch for:"+val+":"+(System.nanoTime() - time));
      return false;
    }
  
    // We are in the set
    else if(val>=current_base)
    {
      
        int i=0;
        for( i=0;i<current_size;i++)
        {
          local+=current[i];
        
          if(local>val)
            break;
        }
       
        if(i==current_size)
          return local == val;
        else
            return (local-current[i] == val);
    }

    // We are in the compressed space
    else
    {
      
      if(baseList.size() == 0)
        return false;
      
      int blobIndex = binarySearchForNearest(val, 0,  blob.size() - 1 );
      
      local = baseList.get(blobIndex);
      long[] ref = blob.get(blobIndex);
      P4DSetNoBase localCompressedSet = new P4DSetNoBase();
      localCompressedSet.setParam(0, DEFAULT_B, BATCH_SIZE, BATCH_OVER);
      localCompressedSet.updateParams(ref);
      
      int i = 0;
        
        for(i=0;i<BATCH_SIZE;i++)
        {
          local+=localCompressedSet.get(ref, i);
         
          if(local>val)
          {
            break;
          }
          
        }
        if(i==BATCH_SIZE)
          return local == val;
        else
        return (local-localCompressedSet.get(ref,i))==val;
      }

    
  }

  private int findIn(MyOpenBitSet myOpenBitSet, int baseVal, int val) {
    return -1;
  }

  private int findIn(int[] current, Integer baseVal, int val) {
    int local = baseVal;
    for (int i = 1; i < BATCH_SIZE; i++) {
      local += current[i];

      if (val > local) {
        if (local == val)
          return i;
      } else
        return -1;

    }
    return -1;
  }

  @Override
  public void optimize()
  {
    //Trim the baselist to size
    this.baseList.seal();
    this.blob.seal();
  }
  

  @Override
  public long sizeInBytes()
  {
    // 64 is the overhead for an int array
    // blobsize * numberofelements * 1.1 (Object Overhead)
    // batch_size * 4 + int array overhead
    // P4dDocIdSet Overhead 110
    optimize();
    return (long) (baseList.length()*4 + 64 +blob.length()*BATCH_SIZE*1.1 + BATCH_SIZE*4 + 24 + 110);
    
  }
  
  public int totalBlobSize()
  {
    int total = 0;
    for(int i = blob.length() - 1; i >= 0; i--)
    {
      long[] segment = blob.get(i);
      total += segment.length;
    }
    return total;
  }
  
}
