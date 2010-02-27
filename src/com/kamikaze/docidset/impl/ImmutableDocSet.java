package com.kamikaze.docidset.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;

public abstract class ImmutableDocSet extends DocSet
{
  private static final long serialVersionUID = 1L;
  
  private int size = -1;
  private Logger log = Logger.getLogger(ImmutableDocSet.class.getName());
  
  @Override
  public void addDoc(int docid)
  {
    throw new java.lang.UnsupportedOperationException("Attempt to add document to an immutable data structure");
    
  }

   
  @Override
  public int size() throws IOException
  {
    // Do the size if we haven't done it so far.
    if(size < 0)
    {
      DocIdSetIterator dcit = this.iterator();
      size = 0;
      try {
        while(dcit.nextDoc() != DocIdSetIterator.NO_MORE_DOCS)
          size++;
      } catch (IOException e) {
        log.log(Level.SEVERE, "Error computing size..");
        return -1;
      }
    }
    return size;
  }

}
