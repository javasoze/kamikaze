package com.kamikaze.docidset.utils;

/** The class is to represent the compression result derived from PForDeltaWithBase. 
 * 
 *   @author hao yan
*/

public class CompResult {

  private int _compressedSize; // the size in bits of the compressed block
  private int[] _compressedBlock = null;  // the compressed block
  public CompResult() {
    _compressedSize = 0;
    _compressedBlock = null;
  }
  
  public CompResult(int compSize, int[] compBlock)
  {
    _compressedSize = compSize;
    _compressedBlock = compBlock;
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
