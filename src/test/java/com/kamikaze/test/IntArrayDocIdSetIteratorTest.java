package com.kamikaze.test;

import java.util.Arrays;

import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;
import com.kamikaze.docidset.impl.IntArrayDocIdSetIterator;

import junit.framework.TestCase;

public class IntArrayDocIdSetIteratorTest {
	
  @Test
  public void testEmptyArray() throws Exception{
	int[] arr = new int[]{};
	IntArrayDocIdSetIterator iter = new IntArrayDocIdSetIterator(arr);
	int doc = iter.nextDoc();
	TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);
	
	doc = iter.advance(1);
	TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);
  }

  @Test
  public void testIntArrayDSI() throws Exception{
    int[] arr = new int[]{-1,-1,1,3,5,7,9};
    IntArrayDocIdSetIterator iter = new IntArrayDocIdSetIterator(arr);
    int doc = iter.nextDoc();
    TestCase.assertEquals(1,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(3,doc);

    iter.reset();
    for (int i=0;i<5;++i){
      doc = iter.nextDoc();
    }
    TestCase.assertEquals(9,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);

    iter.reset();
    doc = iter.advance(6);
    TestCase.assertEquals(7,doc);

    doc = iter.advance(7);
    TestCase.assertEquals(9,doc);

    iter.reset();
    doc = iter.advance(9);
    TestCase.assertEquals(9,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);

    iter.reset();
    doc = iter.advance(10);
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);


    arr = new int[]{1,3,5,7,9};
    iter = new IntArrayDocIdSetIterator(arr);
    doc = iter.nextDoc();
    doc = iter.nextDoc();
    doc = iter.nextDoc();
    doc = iter.advance(1);
    TestCase.assertEquals(5,doc);
    arr = new int[]{1,3,5,7,9};
    iter = new IntArrayDocIdSetIterator(arr);
    doc = iter.advance(1);
    TestCase.assertEquals(1,doc);
    doc = iter.advance(1);
    TestCase.assertEquals(3,doc);
  }
}
