package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.bitset.MyOpenBitSet;

public class OBSDocIdSet extends DocSet implements Serializable {

  private static final long serialVersionUID = 1L;

  private MyOpenBitSet bitSet = null;

   int min = -1;

  int max = -1;

  public OBSDocIdSet(int length) {
    bitSet = new MyOpenBitSet(length);
  }

  public void addDoc(int docid) {
    if (min == -1) {
      min = docid;
    }
    max = docid;
    bitSet.set(max - min);

  }
  
  @Override
  public final boolean isCacheable() {
    return true;
  }

  class OBSDocIdSetIterator extends StatefulDSIterator {
    int lastReturn = -1;
    private int cursor = -1;
    private int marker=-1;

    @Override
    public int docID() {
      return lastReturn + min;
    }

    @Override
    public int nextDoc() throws IOException {
     
      if (bitSet.size() - 1 > lastReturn) {
        if (lastReturn == -1) {
          
          if (bitSet.fastGet(0)) {
            lastReturn = 0;cursor++; marker = lastReturn;
            
            return lastReturn + min;
          }
        } else
          lastReturn = bitSet.nextSetBit(lastReturn + 1);
       
        if (lastReturn != -1)
        {
          cursor++; marker = lastReturn;
          return lastReturn + min;
        }
      }
      return DocIdSetIterator.NO_MORE_DOCS;

    }

    @Override
    public int advance(int target) throws IOException {
      if (target > max)
        return DocIdSetIterator.NO_MORE_DOCS;
      
      target -= min; // adjust target to the local offset
      if (target <= lastReturn) target = lastReturn + 1; 
      
      if(target <= 0) {
        if (bitSet.fastGet(0)) {
          lastReturn = 0;
          return min;
        }
      }
      else {
        lastReturn = bitSet.nextSetBit(target);
        if (lastReturn != -1)
          return lastReturn + min;
      }
      return DocIdSetIterator.NO_MORE_DOCS;
    }

    @Override
    public int getCursor() {
   
      while(marker < lastReturn)
      {
        if(bitSet.fastGet(++marker))
        {
            cursor++;
        }
      }
     
    return cursor;
    }
  }

  @Override
  public OBSDocIdSetIterator iterator() {
   return new OBSDocIdSetIterator();
  }

  public int range() {
    return max - min;
  }

  public int size() {
    return (int) this.bitSet.cardinality();
  }

  public int findWithIndex(int val) {
    
    val -= min;
    if (val >=0 && bitSet.get(val)) {
      int index = -1;
      int counter = -1;
      while(true)
      {
        index = this.bitSet.nextSetBit(index+1);
        if(index<=val && index!=-1)
          counter++;
        else 
          break;
      }
      return counter;
        
    } else
      return -1;

  }
  @Override
  public boolean find(int val) {
    
    val -= min;
    if (val >=0 && bitSet.get(val)) {
      return true;
    } else
      return false;

  }

  @Override
  public long sizeInBytes()
  {
    return  bitSet.capacity()/8;
  }
  
  @Override
  public void optimize()
  {
    this.bitSet.trimTrailingZeros();
  }
}
