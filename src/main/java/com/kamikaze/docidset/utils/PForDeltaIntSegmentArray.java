package com.kamikaze.docidset.utils;

import java.io.IOException;
import java.io.Serializable;

import com.kamikaze.docidset.impl.PForDeltaDocIdSet;

/** 
 * Utility class to provide integer arrays whose sizes can be changed dynamically
 * 
 * @author hao yan
 */

public class PForDeltaIntSegmentArray extends PrimitiveArray<int[]>  implements Serializable{

  private static final long serialVersionUID = -8496856509758658879L;

  public PForDeltaIntSegmentArray(int len) {
    super(len);
  }

  public PForDeltaIntSegmentArray() {
    super();
  }
  
  protected Object buildArray(int len) {
    return new int[len][];
  }
  
  public void add(int[] val) {
    ensureCapacity(_count + 1);
    int[][] array = (int[][]) _array;
    array[_count] = val;
    _count++;
  }

  
  public void set(int index, int[] ref) {
    ensureCapacity(index);
    ((int[][])_array)[index] = ref;
    _count = Math.max(_count, index + 1);
  }

  public int[] get(int index) {
    return ((int[][])_array)[index];
  }
  
  public static int getSerialIntNum(PForDeltaIntSegmentArray instance)
  {
    int num = 3; // _len, _count, _growth
    for(int i=0; i<instance.size(); i++)
    {
      num += 1 + instance.get(i).length; // 1 is the int to record the length of the array
    }
    return num;
  }
  
  public static void convertToBytes(PForDeltaIntSegmentArray instance, byte[] out, int offset)
  {
    int numInt=0;
    Conversion.intToByteArray(instance._len, out, offset);
    offset += Conversion.BYTES_PER_INT;
    
    Conversion.intToByteArray(instance._count, out, offset);
    offset += Conversion.BYTES_PER_INT;
    
    Conversion.intToByteArray(instance._growth, out, offset);
    offset += Conversion.BYTES_PER_INT;
    
    for(int i=0; i<instance.size(); i++)
    {
      int[] data = instance.get(i);
      Conversion.intToByteArray(data.length, out, offset);
      offset += Conversion.BYTES_PER_INT;
      for(int j=0; j<data.length; j++)
      {
        Conversion.intToByteArray(data[j], out, offset);
        offset += Conversion.BYTES_PER_INT;
      }
    }
  }
  
  public static PForDeltaIntSegmentArray newInstanceFromBytes(byte[] inData, int offset) throws IOException
  {
    
    int len = Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    PForDeltaIntSegmentArray instance = new PForDeltaIntSegmentArray(len);
    
    int count = Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    int growth =  Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;
    
    for(int i=0; i<count; i++)
    {
      int dataSize = Conversion.byteArrayToInt(inData, offset);
      offset += Conversion.BYTES_PER_INT;    
      int[] data = new int[dataSize];
      for(int j=0; j<dataSize; j++)
      {
        data[j] = Conversion.byteArrayToInt(inData, offset);
        offset += Conversion.BYTES_PER_INT;
      }
      instance.add(data);
    }
    
    instance._growth = growth;
    
    if(instance._count != count)
      throw new IOException("cannot build PForDeltaIntSegmentArray from byte[]");
    
    return instance;
  }
}
