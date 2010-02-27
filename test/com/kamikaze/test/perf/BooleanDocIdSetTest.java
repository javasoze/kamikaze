package com.kamikaze.test.perf;

import java.util.Arrays;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;

public class BooleanDocIdSetTest {
  private static DocIdSet[] DocList;
  static
  {
    DocList = new DocIdSet[5];
    int maxdoc = 1000000;
    for (int i=0;i<DocList.length;++i)
    {
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      for (int k=0;k<maxdoc;k++)
      {
        docset.addDoc(k);
      }
      DocList[i]=docset;
    }
  }
  
  public static void testOrDocIdSet() throws Exception
  {
    OrDocIdSet orset = new OrDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = orset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
    }
  }
  
  public static void testAndDocIdSet() throws Exception
  {
    AndDocIdSet orset = new AndDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = orset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
    }
  }
  
  public static void main(String[] args) throws Exception{
    int numIter = 1000000;
    for (int i=0;i<numIter;++i)
    {
      long start = System.currentTimeMillis();
      //testOrDocIdSet();
      testAndDocIdSet();
      long end = System.currentTimeMillis();
      System.out.println("took: "+(end-start));
    }
  }
}
