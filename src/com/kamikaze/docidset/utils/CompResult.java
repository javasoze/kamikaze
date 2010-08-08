package com.kamikaze.docidset.utils;

public class CompResult {

  private int _compressedSize;
  private int[] _compressedBlock = null;
  private boolean _usingFixedBitCoding = false;
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
