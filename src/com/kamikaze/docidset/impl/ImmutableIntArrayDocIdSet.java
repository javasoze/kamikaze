package com.kamikaze.docidset.impl;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.utils.IntArray;

public class ImmutableIntArrayDocIdSet extends DocIdSet {
  private final int[] _array;
  
  public ImmutableIntArrayDocIdSet(int[] array){
    _array = array;
  }
  
  @Override
  public DocIdSetIterator iterator() {
    return new ImmutableIntArrayDocIdSetIterator(_array);
  }

  @Override
  public final boolean isCacheable() {
    return true;
  }

  public static class ImmutableIntArrayDocIdSetIterator extends DocIdSetIterator{
    private int _doc;
    private int cursor;
    private final int[] _array;
    
    public ImmutableIntArrayDocIdSetIterator(int[] array){
      _array=array;
      _doc = -1;
      cursor=-1;
    }
    
    @Override
    final public int docID(){
      return _doc;
    }
    
    @Override
    public int nextDoc() throws java.io.IOException{
      if (++cursor < _array.length) {
        _doc = _array[cursor];
      }
      else{
        _doc = DocIdSetIterator.NO_MORE_DOCS;
      }
      return _doc;
    }
    
    @Override
    public int advance(int target) throws java.io.IOException{
      if (cursor >= _array.length || _array.length == -1) return DocIdSetIterator.NO_MORE_DOCS;
      if (target <= _doc) target = _doc + 1;      
      int index = IntArray.binarySearch(_array, cursor, _array.length, target);
      if (index > 0){
        cursor = index;
        _doc = _array[cursor];
        return _doc;
      }
      else{
        cursor = -(index+1);
        if (cursor>_array.length) {
          _doc = DocIdSetIterator.NO_MORE_DOCS;
        }
        else {
          _doc = _array[cursor];
        }
        return _doc;     
      }
    }
  }
}
