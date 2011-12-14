package com.kamikaze.docidset.utils;

import java.io.Serializable;

public class LongSegmentArray extends PrimitiveArray<long[]>  implements Serializable{

  private static final long serialVersionUID = -5791570113959834064L;

  public LongSegmentArray(int len) {
    super(len);
  }

  public LongSegmentArray() {
    super();
  }
  
  protected Object buildArray(int len) {
    return new long[len][];
  }
  
  public void add(long[] val) {
    ensureCapacity(_count + 1);
    long[][] array = (long[][]) _array;
    array[_count] = val;
    _count++;
  }

  
  public void set(int index, long[] ref) {
    ensureCapacity(index);
    ((long[][])_array)[index] = ref;
    _count = Math.max(_count, index + 1);
  }

  public long[] get(int index) {
    return ((long[][])_array)[index];
  }
}
