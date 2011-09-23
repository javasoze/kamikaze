package com.kamikaze.test;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;

public class PForDeltaTestBooleanDocIdSetTest  extends TestCase  {
  @Test  
  public void testOrDocIdSet() throws Exception
  {
    System.out.println("Running testOrDocIdSet() Test case...");
    DocIdSet[] DocList;
    DocList = new DocIdSet[5];
    int maxdoc = 2;
    ArrayList<Integer> intSet = new ArrayList<Integer>();
    
    for (int i=0;i<DocList.length;++i)
    {
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      docset.addDoc((i+1)*10);
      intSet.add((i+1)*10);
      docset.addDoc((i+1)*100);
      intSet.add((i+1)*100);
      DocList[i]=docset;
    }
    OrDocIdSet orset = new OrDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = orset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      assertTrue("ERROR: testOrDocIdSet ", intSet.contains(doc));
    }
    System.out.println("--------completed-----------");
  }
  
  @Test  
  public void testNotDocIdSet() throws Exception
  {
    System.out.println("Running testNotDocIdSet() Test case...");
    int maxdoc = 5;
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      docset.addDoc(1);
      intSet.add(1);
      docset.addDoc(3);
      intSet.add(3);
      
    NotDocIdSet notset = new NotDocIdSet(docset, 5);
    DocIdSetIterator iter = notset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      assertFalse("ERROR: testOrDocIdSet ", intSet.contains(doc));
    }
    System.out.println("--------completed-----------");
  }
  
  @Test
  public void testAndDocIdSet() throws Exception
  {
    System.out.println("Running testOrDocIdSet() Test case...");
    DocIdSet[] DocList;
    DocList = new DocIdSet[5];
    int maxdoc = 2;
    ArrayList<Integer> intSet = new ArrayList<Integer>();
    
    for (int i=0;i<DocList.length;++i)
    {
      IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
      docset.addDoc(5);
      intSet.add(5);
      docset.addDoc((i+1)*100);
      intSet.add((i+1)*100);
      DocList[i]=docset;
    }
    AndDocIdSet andset = new AndDocIdSet(Arrays.asList(DocList));
    DocIdSetIterator iter = andset.iterator();
    int doc;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      assertTrue("ERROR: testAndDocIdSet ", intSet.contains(doc));
    }
    System.out.println("--------completed-----------");
  }
  
}
