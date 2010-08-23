package com.kamikaze.docidset.utils;

/** The class is to represent the compression result derived from PForDeltaWithBase. 
 * 
 *   @author hao yan
*/

public class CompResult {

  private int _compressedSize; // the size in bits of the compressed block
  private int[] _compressedBlock = null;  // the compressed block
  private boolean _usingFixedBitCoding = false; // indicating if we use fixed bits to encode expPos (positions of exceptions)
  public CompResult() {
    _compressedSize = 0;
    _compressedBlock = null;
    _usingFixedBitCoding = false;
  }
  
  public CompResult(int compSize, int[] compBlock)
  {
    _compressedSize = compSize;
    _compressedBlock = compBlock;
  }
  
  public void setUsingFixedBitCoding(boolean usingFixedBitCoding)
  {
    _usingFixedBitCoding = usingFixedBitCoding;
  }
  
  public boolean getUsingFixedBitCoding()
  {
    return _usingFixedBitCoding ;
  }
  
  public void setCompressedSize(int compressedSize)
  {
     _compressedSize = compressedSize;
  }
  public void setCompressedBlock(int[] compressedBlock)
  {
     _compressedBlock = compressedBlock;
  }
  public int getCompressedSize()
  {
    return _compressedSize;
  }
  public int[] getCompressedBlock()
  {
    return _compressedBlock;
  }
}
