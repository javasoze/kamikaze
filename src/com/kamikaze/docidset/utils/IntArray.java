/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.kamikaze.docidset.utils;

import java.io.Serializable;


/**
 * 
 */
public class IntArray extends PrimitiveArray<Integer> implements Serializable {

  private static final long serialVersionUID = 1L;

  public IntArray(int len) {
    super(len);
  }

  public IntArray() {
    super();
  }

  public void add(int val) {
    ensureCapacity(_count + 1);
    int[] array = (int[]) _array;
    array[_count] = val;
    _count++;
  }

  
  public void set(int index, int val) {
    ensureCapacity(index);
    int[] array = (int[]) _array;
    array[index] = val;
    _count = Math.max(_count, index + 1);
  }

  public int get(int index) {
    int[] array = (int[]) _array;
    return array[index];
  }

  public boolean contains(int elem) {
    int size = this.size();
    for (int i = 0; i < size; ++i) {
      if (get(i) == elem)
        return true;
    }
    return false;
  }

  @Override
  protected Object buildArray(int len) {
    return new int[len];
  }

 

  public static int binarySearch(int[] a, int fromIndex, int toIndex,int key) {
    int low = fromIndex;
    int high = toIndex - 1;
  
    while (low <= high) {
        int mid = (low + high) >>> 1;
        int midVal = a[mid];
  
        if (midVal < key)
        low = mid + 1;
        else if (midVal > key)
        high = mid - 1;
        else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

}
