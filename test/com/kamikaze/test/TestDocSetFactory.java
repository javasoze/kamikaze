package com.kamikaze.test;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.DocIdSet;
import org.junit.Test;

import com.kamikaze.docidset.utils.DocSetFactory;
import com.kamikaze.docidset.utils.DocSetFactory.FOCUS;
public class TestDocSetFactory {

  private static int batch = 128;

  private static String serial = "SerialDocSet";

  public TestDocSetFactory() {

  }

  
  
  
  @Test
  public void testDocSetFactory() {
    
    
    int min = 44;
    int max  = 533222;
    int count  = 100;
    int sparseThresholdCount = 500000;
    
    DocIdSet set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.P4DDocIdSet");
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    
    min = 10;
    max = 25000000;
    count = 100;

    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
    
    count *=10000;
    set = DocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.P4DDocIdSet");
    
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
    assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.P4DDocIdSet");
    
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