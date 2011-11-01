package com.kamikaze.docidset.impl;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

public class AndNotDocIdSetIterator extends DocIdSetIterator{
  private int _nextDelDoc;
  private final DocIdSetIterator _baseIter;
  private final DocIdSetIterator _notIter;
  private int _currID;
  
  public AndNotDocIdSetIterator(DocIdSetIterator baseIter,DocIdSetIterator notIter) throws IOException{
    _nextDelDoc = notIter.nextDoc();
    _baseIter = baseIter;
    _notIter = notIter;
    _currID = -1;
  }

  @Override
  public int advance(int target) throws IOException {
    _currID = _baseIter.advance(target);
    if (_currID==DocIdSetIterator.NO_MORE_DOCS)
      return _currID;

    if (_nextDelDoc!=DocIdSetIterator.NO_MORE_DOCS){
      _currID = _baseIter.docID();
      if (_currID<_nextDelDoc) return _currID;
      _nextDelDoc = _notIter.advance(_currID);
      if (_currID==_nextDelDoc) return nextDoc();
    }
    return _currID;
  }

  @Override
  public int docID() {
    return _currID;
  }

  @Override
  public int nextDoc() throws IOException {
    _currID =_baseIter.nextDoc();
    if (_nextDelDoc!=DocIdSetIterator.NO_MORE_DOCS){
        while(_currID != DocIdSetIterator.NO_MORE_DOCS){
            if (_currID<_nextDelDoc){
                return _currID;
            }
            else{
                if (_currID == _nextDelDoc){
                    _currID =_baseIter.nextDoc();
                }
                _nextDelDoc = _notIter.advance(_currID);
            }
        }
    }
    return _currID;
  }
}
