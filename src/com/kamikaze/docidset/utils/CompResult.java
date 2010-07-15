package com.kamikaze.docidset.utils;

public class CompResult {

  private int _compressedSize;
  private int[] _compressedBlock = null;
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
