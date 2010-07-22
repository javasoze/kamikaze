package com.kamikaze.docidset.bitset;

import java.io.Serializable;

import org.apache.lucene.util.OpenBitSet;

public class MyOpenBitSet extends OpenBitSet implements Serializable
{
	private static final long serialVersionUID = 1L;

	public MyOpenBitSet()
	  {
		  super();
	  }	
	  
	  public MyOpenBitSet(long numBits)
	  {
		  super(numBits);
	  }
	 
	 
	
	  /** Set 0/1 at the specified index.
	   * Note: The value for the bitVal is not checked for 0/1, hence incorrect values passed 
	   * lead to unexpected results
	   * 
	   * @param index
	   * @param bitVal
	   */
	  public void fastSetAs(long index, int bitVal)
	  {		  
		int wordNum = (int)(index >> 6);
		int bit = (int)index & 0x3f;
		long bitmask = ((long)bitVal) << bit;
		bits[wordNum] |= bitmask;		  
	  }
}
