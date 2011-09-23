package com.kamikaze.test;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.DocIdSet;
import org.junit.Test;
import junit.framework.TestCase;


import com.kamikaze.docidset.utils.DocSetFactory;
import com.kamikaze.docidset.utils.DocSetFactory.FOCUS;
public class PForDeltaTestDocSetFactoryTest extends TestCase {

  private static int batch = 128;

  private static String serial = "SerialDocSet";

  public PForDeltaTestDocSetFactoryTest() {

  }

  
  @Test
  public void testDocSetFactory() {
    
    
    int min = 44;
    int max  = 533222;
    int count  = 100;
    int sparseThresholdCount = 500000;
    
    DocIdSet set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
    System.out.println("set.getClass().getName():" + set.getClass().getName());
    
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    //assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    
    min = 10;
    max = 25000000;
    count = 100;

    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    
    count *=10000;
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    
//    count *=10000;
//    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
//    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    
    max = 1000000000;
    count*=1000;
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.OBSDocIdSet");
    
    min = 10;
    max = 30000000;
    count = 10000000;
    
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.OBSDocIdSet");
    
    count /= 10000000;
    
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    
//    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
//    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
    
    min = 10;
    max = 30000000;
    count = 10000000;
    
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.OBSDocIdSet");
    
    count /= 10000000;
    
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
 
    
  }
}