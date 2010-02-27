package com.kamikaze.docidset.impl;

import java.io.Serializable;
import java.util.BitSet;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.utils.LongSegmentArray;

public abstract class AbstractDocSet extends DocSet implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final double logBase2 = Math.log(2);

  public static final int DEFAULT_BATCH_SIZE = 256;

 

  /**
   * Default batch size for compression blobs
   * 
   */
  public int BATCH_SIZE = DEFAULT_BATCH_SIZE;

  /**
   * Default batch size for compression blobs
   * 
   */
  protected int BATCH_OVER = 12;

  /**
   * Current base size
   * 
   */
  protected int current_base;

  /**
   * Last added value
   * 
   */
  protected int lastAdded = 0;

  /**
   * List of Data blobs
   * 
   
  protected MyOpenBitSetArray blob = null;*/
  
  
  /**
   * List of Data blobs
   * 
   */
  protected LongSegmentArray blob = null;

  /**
   * Pointer to the current data block.
   * 
   */
  protected int[] current = null;

  /**
   * Size of the current array
   * 
   */
  protected int current_size = 0;

  /**
   * Current Max bit count
   * 
   */
  protected int current_ex_count = 0;

  /**
   * Current Bit Size
   * 
   */
  protected int current_b = 1;

  /**
   * B Value accumulator
   * 
   */
  protected int[] bVal = null;
  
  /** 
   * compressed bit size
   */
  /** 
   * Compressed Bits
   */
   protected long compressedBits;


   
  /**
   * Internal compression Method 
   * @return compressed object
   */
  protected abstract Object compress();
 // protected abstract Object compressAlt();

  protected AbstractDocSet() {
    this.blob = new LongSegmentArray();
    
  }

  /**
   * Internal Decompression Method
   * 
   * @return
   */
  private int[] decompress(MyOpenBitSet packedSet) {
    System.err.println("Method not implemented");
    return null;
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
    this.current = new int[BATCH_SIZE];
    current_size = 0;
    current_b = 32;
    // blob = new ArrayList<MyOpenBitSet>();
    bVal = new int[33];
  }

  /**
   * Number of compressed units plus the last block
   * @return docset size
   */
  public int size() {
    return blob.size() * BATCH_SIZE + current_size;
  }

  
  
  /**
   * Add document to this set
   * 
   */
  public void addDoc(int docid) {
    if (size() == 0) {
      initSet();
      current[current_size++] = docid;
      current_base = docid;
      lastAdded = current_base;
    }

    else if (current_size == BATCH_SIZE) {
      current_b = 32;
      current_ex_count = 0;
      
      int totalBitSize = current_b * BATCH_SIZE;
      int exceptionCount = 0;

      // formulate b value. Minimum bits used is minB.
      for (int b = 32; b > 0; b--)
      {
        exceptionCount += bVal[b];
        
        // break if exception count is too large for this b
        if((getNumBits(exceptionCount) + 1) >= b) break;
        
        if ((exceptionCount * 32 + b * BATCH_SIZE) < totalBitSize)
        {
          // this is the best parameter so far
          current_b = b;
          current_ex_count = exceptionCount;
        }
      }

      long[] myop = (long[]) compress();
      compressedBits+=myop.length<<6;
      blob.add(myop);

      // roll the batch
      current_size = 1;
      current_base = docid;
      lastAdded = current_base;
      current[0] = current_base;
      current_ex_count = 0;

      bVal = new int[33];

    }// end batch boundary

    else {
      try {
        int delta = docid - lastAdded;
        current[current_size] = delta;
        lastAdded = docid;
        if (delta != 0)
          bVal[getNumBits(delta)]++;

        current_size++;
      } catch (ArrayIndexOutOfBoundsException w) {
        System.err.println("Error inserting DOC:" + docid);

      }

    } // end append to end of array
    
  }
  
  /**
   * Add document to this set
   * 
   
  public void addDoc(int docid) {
    if (size() == 0) {
      initSet();
      current[current_size++] = docid;
      current_base = docid;
      lastAdded = current_base;
    }

    else if (current_size == BATCH_SIZE) {

      int exceptionCount = 0;

      // formulate b value. Minimum bits used is 5.
      for (int k = 31; k > 3; k--) {
        // System.out.print(bVal[k]+":");
        exceptionCount += bVal[k];
        if (exceptionCount >= BATCH_OVER) {
          current_b = k;
          exceptionCount -= bVal[k];
          break;
        }
      }

      // Compensate for extra bit
      current_b += 1;

      // set current_exception_count
      current_ex_count = exceptionCount;

      MyOpenBitSet myop = (MyOpenBitSet) compress();
      compressedBits+=myop.capacity();
      blob.add(myop);

      // roll the batch
      current_size = 1;
      current_base = docid;
      lastAdded = current_base;
      current[0] = current_base;
      current_ex_count = 0;

      bVal = new int[33];

    }// end batch boundary

    else {
      try {

        current[current_size] = docid - lastAdded;
        lastAdded = docid;
        if (current[current_size] != 0)
          bVal[(int) (Math.log(current[current_size]) / logBase2) + 1]++;

        current_size++;
      } catch (ArrayIndexOutOfBoundsException w) {
        System.err.println("Error inserting DOC:" + docid);

      }

    } // end append to end of array

  }*/

  private static final int[] NUMBITS = new int[256];
  static {
    NUMBITS[0] = 1;
    for(int i = 1; i < 256; i++)
    {
      int j = 7;
      while(j > 0)
      {
        if((i & (1 << j)) != 0) break;
        j--;
      }
      NUMBITS[i] = j + 1;
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
}
