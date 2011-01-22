package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.StatefulDSIterator;

/**
 * Implementation of the intersection set of multiple DocIdSets in a Document-at-a-time (DAAT) manner. 
 *  
 * @author hao yan
 */

public class PForDeltaAndDocIdSet extends ImmutableDocSet implements Serializable {
  private static final long serialVersionUID = 1L;

  public class DescDocIdSetComparator implements Comparator<StatefulDSIterator>, Serializable {
    private static final long serialVersionUID = 1L;

    public int compare(StatefulDSIterator o1, StatefulDSIterator o2) {
      return o2.docID() - o1.docID();
    }
  }

  private List<DocIdSet> sets = null;
  private int nonNullSize; // excludes nulls
  
  private ArrayList<Integer> _interSectionResult = new ArrayList<Integer>();
  
  public PForDeltaAndDocIdSet(List<DocIdSet> docSets) {
    this.sets = docSets;
    int size = 0;
    if (sets != null) {
      for(DocIdSet set : sets) {
        if(set != null) size++;
      }
    }
    nonNullSize = size;
  }

  public ArrayList<Integer> getIntersection()
  {
    return _interSectionResult;
  }
  
  /**
   * Find existence in the set with index
   * 
   * NOTE :  Expensive call. Avoid.
   * @param val value to find the index for
   * @return index where the value is
   */
  @Override
  public final int findWithIndex(int val) throws IOException
  {  
    DocIdSetIterator finder = new PForDeltaAndDocIdSetIterator();
    int cursor = -1;
    try {
      int docId;
      while((docId = finder.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      {
        if(docId > val)
          return -1;
        else if(docId == val )
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
  public final boolean find(int val) throws IOException{
    
    DocIdSetIterator finder = new PForDeltaAndDocIdSetIterator();
  
    try {
      int docid = finder.advance(val);
      if(docid!=DocIdSetIterator.NO_MORE_DOCS && docid == val)
        return true;
      else
        return false;
    }
    catch (IOException e) {
      return false;
    }
  }  
  
  class PForDeltaAndDocIdSetIterator extends DocIdSetIterator {
    int lastReturn = -1;
    private DocIdSetIterator[] iterators = null;

    PForDeltaAndDocIdSetIterator() throws IOException{
      if (nonNullSize < 1)
        throw new IllegalArgumentException("Minimum one iterator required");
      
      iterators = new DocIdSetIterator[nonNullSize];
      int j = 0;
      for (DocIdSet set : sets) {
        if (set != null) {
          DocIdSetIterator dcit = set.iterator();
          iterators[j++] = dcit;
        }
      }
      lastReturn = (iterators.length > 0 ? -1 : DocIdSetIterator.NO_MORE_DOCS);
    }

    @Override
    public final int docID() {
      return lastReturn;
    }
    
    @Override
    public final int nextDoc() throws IOException {
      // DAAT
      if (lastReturn == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;
      
      DocIdSetIterator dcit = iterators[0];
      
      int target = dcit.nextDoc();
      // shortcut: if it reaches the end of the shortest list, do not scan other lists
      if(target == DocIdSetIterator.NO_MORE_DOCS)
      { 
        return (lastReturn = target);
      }

      int size = iterators.length;
      int skip = 0;
      int i = 1;
      
      // i is ith iterator
      while (i < size) {
        if (i != skip) {
          dcit = iterators[i];
          int docId = dcit.advance(target);
          
          // once we reach the end of one of the blocks, we return NO_MORE_DOCS
          if(docId == DocIdSetIterator.NO_MORE_DOCS)
          {
            return (lastReturn = docId);
          }
           
          if (docId > target) { //  cannot find the target in the next list
            target = docId;
            if(i != 0) {
              skip = i;
              i = 0;
              continue;
            }
            else // for the first list, it must succeed as long as the docId is not NO_MORE_DOCS
              skip = 0;
          }
        }
        i++;
      }
      
      //_interSectionResult.add(target);
      return (lastReturn = target);
    }

    
    @Override
    public final int advance(int target) throws IOException {

      if (lastReturn == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;
      
      DocIdSetIterator dcit = iterators[0];
      target = dcit.advance(target);
      if(target == DocIdSetIterator.NO_MORE_DOCS)
      { 
        return (lastReturn = target);
      }
      
      int size = iterators.length;
      int skip = 0;
      int i = 1;
      while (i < size) {
        if (i != skip) {
          dcit = iterators[i];
          int docId = dcit.advance(target);
          if(docId == DocIdSetIterator.NO_MORE_DOCS)
          {
            return (lastReturn = docId);
          }
          if (docId > target) {
            target = docId;
            if(i != 0) {
              skip = i;
              i = 0;
              continue;
            }
            else
              skip = 0;
          }
        }
        i++;
      }
      return (lastReturn = target);
    }
  }

  public final DocIdSetIterator iterator() throws IOException{
    return new PForDeltaAndDocIdSetIterator();
  }
  
  
}
