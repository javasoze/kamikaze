package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * Implementation of the union set of multiple DocIdSets (which essentially is a merged set of thes DocIdSets). 
 *  
 * @author hao yan
 */

public class PForDeltaOrDocIdSet extends ImmutableDocSet implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final int INVALID = -1;


  public class AescDocIdSetComparator implements Comparator<DocIdSetIterator>,
      Serializable {

    private static final long serialVersionUID = 1L;

    public int compare(DocIdSetIterator o1, DocIdSetIterator o2) {
      return o1.docID() - o2.docID();
    }

  }

  List<DocIdSet> sets = null;
  
  private int _size = INVALID;

  public PForDeltaOrDocIdSet(List<DocIdSet> docSets) {
    this.sets = docSets;
    int size = 0;
    if (sets != null) {
      for(DocIdSet set : sets) {
        if(set != null) size++;
      }
    }
  }
  
  @Override
  public DocIdSetIterator iterator() throws IOException{
    return new PForDeltaOrDocIdSetIterator(sets);
  }
  
  
  /**
   * Find existence in the set with index
   * 
   * NOTE :  Expensive call. Avoid.
   * @param val value to find the index for
   * @return index where the value is
   */
  @Override
  public int findWithIndex(int val) throws IOException
  {
    DocIdSetIterator finder = new PForDeltaOrDocIdSetIterator(sets);
    int cursor = -1;
    try {
      int docid;
      while((docid = finder.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
      {
        if(docid > val)
          return -1;
        else if(docid== val )
          return ++cursor;
        else 
          ++cursor;
      }
    } catch (IOException e) {
      return -1;
    }
    return -1;
  }
  
  @Override
  public int size() throws IOException
  {
  
    if(_size==INVALID)
    {
      _size=0;
      DocIdSetIterator it = this.iterator();
      
      try {
        while(it.nextDoc()!=DocIdSetIterator.NO_MORE_DOCS)
          _size++;
      } catch (IOException e) {
        e.printStackTrace();
        _size = INVALID;
      }
      
    }
    return _size;
  }
 
  class PForDeltaOrDocIdSetIterator extends DocIdSetIterator {

    private final class Item
    {
      public final DocIdSetIterator iter;
      public int doc;
      public Item(DocIdSetIterator iter)
      {
        this.iter = iter;
        this.doc = -1;
      }
    }
    private int _curDoc;
    private final Item[] _heap;
    private int _size;
   
    PForDeltaOrDocIdSetIterator(List<DocIdSet> sets) throws IOException
    {
      _curDoc = -1;
      _heap = new Item[sets.size()];
      _size = 0;
      for(DocIdSet set : sets)
      {
        _heap[_size++] = new Item(set.iterator());
      }
      if(_size == 0) _curDoc = DocIdSetIterator.NO_MORE_DOCS;
    }
    
    @Override
    public final int docID() {
      return _curDoc;
    }

    @Override
    public final int nextDoc() throws IOException
    {
      if(_curDoc == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;

      Item top = _heap[0];
      while(true)
      {
        DocIdSetIterator topIter = top.iter;
        int docid;
        if((docid = topIter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
        {
          top.doc = docid;
          heapAdjust();
        }
        else
        {
          heapRemoveRoot();
          if(_size == 0) return (_curDoc = DocIdSetIterator.NO_MORE_DOCS);
        }
        top = _heap[0];
        int topDoc = top.doc;
        if(topDoc > _curDoc)
        {
          return (_curDoc = topDoc);
        }
      }
    }

    @Override
    public final int advance(int target) throws IOException
    {
      if(_curDoc == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;

      if(target <= _curDoc) target = _curDoc + 1;
       
      Item top = _heap[0];
      while(true)
      {
        DocIdSetIterator topIter = top.iter;
        int docid;
        if((docid = topIter.advance(target))!=DocIdSetIterator.NO_MORE_DOCS)
        {
          top.doc = docid;
          heapAdjust();
        }
        else
        {
          heapRemoveRoot();
          if (_size == 0) return (_curDoc = DocIdSetIterator.NO_MORE_DOCS);
        }
        top = _heap[0];
        int topDoc = top.doc;
        if(topDoc >= target)
        {
          return (_curDoc = topDoc);
        }
      }
    }
    
  // Organize subScorers into a min heap with scorers generating the earlest document on top.
    /*
    private final void heapify() {
        int size = _size;
        for (int i=(size>>1)-1; i>=0; i--)
            heapAdjust(i);
    }
    */
    /* The subtree of subScorers at root is a min heap except possibly for its root element.
     * Bubble the root down as required to make the subtree a heap.
     */
    private final void heapAdjust()
    {
      final Item[] heap = _heap;
      final Item top = heap[0];
      final int doc = top.doc;
      final int size = _size;
      int i = 0;
      
      while(true)
      {
        int lchild = (i<<1)+1;
        if(lchild >= size) break;
        
        Item left = heap[lchild];
        int ldoc = left.doc;
        
        int rchild = lchild+1;
        if(rchild < size){
          Item right = heap[rchild];
          int rdoc = right.doc;
          
          if(rdoc <= ldoc)
          {
            if(doc <= rdoc) break;
            
            heap[i] = right;
            i = rchild;
            continue;
          }
        }
        
        if(doc <= ldoc) break;
        
        heap[i] = left;
        i = lchild;
      }
      heap[i] = top;
    }

    // Remove the root Scorer from subScorers and re-establish it as a heap
    private final void heapRemoveRoot()
    {
      _size--;
      if (_size > 0)
      {
        Item tmp = _heap[0];
        _heap[0] = _heap[_size];
        _heap[_size] = tmp; // keep the finished iterator at the end for debugging
        heapAdjust();
      }
    }

  }
}