package com.kamikaze.test;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;

public class TestBooleanDocIdSet  extends TestCase  {
  @Test  
  public void testOrDocIdSet() throws Exception
  {
    System.out.println("Running testOrDocIdSet() Test case...");
    DocIdSet[] DocList;
    DocList = new DocIdSet[5];
    int maxdoc = 100000;
    for (int i=0;i<DocList.length;++i)
    {
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      for (int k=0;k<maxdoc;k++)
      {
        docset.addDoc(k);
      }
      DocList[i]=docset;
    }
    OrDocIdSet orset = new OrDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = orset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
    }
    System.out.println("--------completed-----------");
  }
  @Test
  public void testAndDocIdSet() throws Exception
  {
    System.out.println("Running testAndDocIdSet() Test case...");
    DocIdSet[] DocList;
    DocList = new DocIdSet[5];
    int maxdoc = 100000;
    for (int i=0;i<DocList.length;++i)
    {
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      for (int k=0;k<maxdoc;k++)
      {
        docset.addDoc(k);
      }
      DocList[i]=docset;
    }
    AndDocIdSet orset = new AndDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = orset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
    }
    System.out.println("--------completed-----------");
  }
  
}
