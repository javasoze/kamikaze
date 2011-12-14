package com.kamikaze.docidset.utils;

import java.io.Serializable;

import com.kamikaze.docidset.bitset.MyOpenBitSet;

/** Dynamic Array to hold MyOpenBitSets
 * 
 * author@abhasin
 */
public class MyOpenBitSetArray extends PrimitiveArray<MyOpenBitSet>  implements Serializable{
  private static final long serialVersionUID = 2493283898882704031L;
  
  public MyOpenBitSetArray(int len) {
    super(len);
  }

  public MyOpenBitSetArray() {
    super();
  }
  
  protected Object buildArray(int len) {
    return new MyOpenBitSet[len];
  }
  
  public void add(MyOpenBitSet val) {
    ensureCapacity(_count + 1);
    MyOpenBitSet[] array = (MyOpenBitSet[]) _array;
    array[_count] = val;
    _count++;
  }

  
  public void set(int index, MyOpenBitSet ref) {
    ensureCapacity(index);
    ((MyOpenBitSet[])_array)[index] = ref;
    _count = Math.max(_count, index + 1);
  }

  public MyOpenBitSet get(int index) {
    return ((MyOpenBitSet[])_array)[index];
  }
}
