package com.kamikaze.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;

@RunWith(Parameterized.class)
public class TestParameterizedDocSets {

  private static final int batch = 256;

  private static final int all = 3;

  private int _length = -1;

  private int _max = -1;

  public TestParameterizedDocSets(int length, int max) {
    super();
    
    
    _length = length;
    _max = max;

  }

  @Parameters
  public static List data() {
    return Arrays.asList(new Object[][] { { 2000, 30000000 },
        { 4000, 30000000 }, { 8000, 30000000 }, { 12000, 30000000 }, { 16000, 30000000 }
        ,{32000,30000000}, {64000,40000000}
    });
  }

  
  
  
  @Test
  public void testAnnounce()
  {
    System.out.println("");
    System.out.println("");
    System.out.println("################# Initiating Test Cycle with length:"+_length+" and max:"+_max+"################");
  }
 
  @Test
  public void testOBSDocIdSetIterateSanity() throws IOException {
    double booster  = ((_max*1.0)/(1000f*_length));
    System.out.println("");
    System.out.println("Running OBSDocIdSet Iterate Sanity test");
    System.out.println("----------------------------");
    OBSDocIdSet set = new OBSDocIdSet(_max);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();

    // Minimum 5 bits
    int randomizer = 0;
    double totalCompressionTime = 0;
    double totalDecompressionTime = 0;
    double totalCompressionRatio = 0;
    TreeSet<Integer> list1 = new TreeSet<Integer>();
    long now = System.nanoTime();
    for (int i = 1; i < (_length); i++)
    // for(int i = 1;i<2;i++)
    {

      // System.out.println("Randomizer ="+randomizer);
      ArrayList<Integer> list = new ArrayList<Integer>();

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }
      Collections.sort(list);
      randomizer += 1000*booster;

      for (int k = 0; k < batch; k++) {
        list1.add(list.get(k));
        set.addDoc(list.get(k));
      }

      // System.out.println("At :" + i +" "+(randomizer-1000) +" " +
      // randomizer);
    }
  
    totalCompressionTime = System.nanoTime() - now;
    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator its = set.iterator();
    int x = 0;
    now = System.nanoTime();
    int i = -1;
    Iterator<Integer> itd = list1.iterator();
    int docid;
    while ((docid=its.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS && itd.hasNext() ) {
      
       assertEquals(docid, itd.next().intValue());
      }
      // System.out.println(its.doc());

    totalDecompressionTime = System.nanoTime() - now;

  

  }
 
  @Test
  public void testOBSDocIdSetSkipSanity() {
    double booster  = ((_max*1.0)/(1000f*_length));
    System.out.println("");
    System.out.println("Running OBSDocIDSet Skip Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();

    OBSDocIdSet set = new OBSDocIdSet(_max);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    TreeSet<Integer> list = new TreeSet<Integer>();
    ArrayList<Integer> list2 = new ArrayList<Integer>();

    for (int i = 1; i < _length + 1; i++) {

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000*booster;
    }

    //Collections.sort(list);
    int counter=0;
    for (Integer c : list) {
      counter++;
      set.addDoc(c);
    }
    
    
    list2.addAll(list);
 // Measure time to obtain iterator
    long time = System.nanoTime();
    set.optimize();
    System.out.println("Time to optimize set of " + counter + " numbers : "+ (System.nanoTime() - time)+"ns");
    System.out.println("Size in Bytes:"+set.sizeInBytes());
    StatefulDSIterator dcit = set.iterator();   
    
    
    long now = System.nanoTime();

    for (int i = 0; i < _max; i += 600) {
      try {

        int docid = dcit.advance(i);
        if (docid!=DocIdSetIterator.NO_MORE_DOCS) {
          
          //System.out.println(dcit.doc()+":"+list2.get(dcit.getCursor())+":"+dcit.getCursor());
          assertEquals(docid, list2.get(dcit.getCursor()).intValue());
          
          docid = dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS)
          {
            //System.out.println(dcit.doc()+":"+list2.get(dcit.getCursor())+":"+dcit.getCursor());
            assertEquals(docid, list2.get(dcit.getCursor()).intValue());
          }
          docid = dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS)
          {
            //System.out.println(dcit.doc()+":"+list2.get(dcit.getCursor())+":"+dcit.getCursor());
            assertEquals(docid, list2.get(dcit.getCursor()).intValue());
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }

  }
 
  @Test
  public void testOBSDocIdSetPerformance() throws IOException {
    double booster  = ((_max*1.0)/(1000f*_length));
    System.out.println("");
    System.out.println("Running OBSDocIdSet Performance test");
    System.out.println("----------------------------");
    OBSDocIdSet set = new OBSDocIdSet(_max);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();

    // Minimum 5 bits
    int randomizer = 0;
    double totalCompressionTime = 0;
    double totalDecompressionTime = 0;
    double totalCompressionRatio = 0;

    long now = System.nanoTime();
    for (int i = 1; i < (_length); i++)
    // for(int i = 1;i<2;i++)
    {

      // System.out.println("Randomizer ="+randomizer);
      ArrayList<Integer> list = new ArrayList<Integer>();

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }
      Collections.sort(list);
      randomizer += 1000*booster;

      for (int k = 0; k < batch; k++) {
        set.addDoc(list.get(k));
      }

      // System.out.println("At :" + i +" "+(randomizer-1000) +" " +
      // randomizer);
    }

    totalCompressionTime = System.nanoTime() - now;
    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator its = set.iterator();
    int x = 0;
    now = System.nanoTime();
    int docid;
    while ((docid=its.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {

      x = docid;
      // System.out.println(its.doc());

    }
    totalDecompressionTime = System.nanoTime() - now;
    set.optimize();
    System.out.println("Total decompression time :" + totalDecompressionTime
        + ": for " + ((double) batch * _length) / 1000000 + " M numbers");
   
    System.out.println("Compression Ratio:" + ((double) (_max))
        /(batch*_length*32) + " for max=" + _max);

  }
  
 
  @Test
  public void testIntArrayDocIdSetSkipSanity() {
   
    System.out.println("");
    System.out.println("Running IntArrayDocIdSet Skip Sanity test");
    System.out.println("----------------------------");
    int size = batch * _length;
    IntArrayDocIdSet set = new IntArrayDocIdSet(size);

    Random random = new Random();
    ArrayList<Integer> list = new ArrayList<Integer>();
    ArrayList<Integer> list2 = new ArrayList<Integer>();

    long now = System.nanoTime();
    for (int i = 0; i < size; i++) {
      list.add((int) (i * 100 + random.nextDouble() * 1000));
    }

    Collections.sort(list);

    for (int k = 0; k < size; k++) {
      set.addDoc(list.get(k));
    }

    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator dcit = set.iterator();
    for (int i = 0; i < _max; i += 60) {
      try {
        int docid = dcit.advance(i);
        if (docid!=DocIdSetIterator.NO_MORE_DOCS) {

          assertEquals(docid, list.get(dcit.getCursor()).intValue());
          docid = dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS)
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
          docid = dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS)
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
        }
      } catch (Exception e) {
        fail(e.getMessage());
      }
    }

  }

  
 
  @Test
  public void testIntArrayDocIdSetIterateSanity() {
    System.out.println("");
    System.out.println("Running IntArrayDocIdSet Iterate sanity test");
    System.out.println("----------------------------");
    int size = batch * _length;
    IntArrayDocIdSet set = new IntArrayDocIdSet(_length);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();
    ArrayList<Integer> list = new ArrayList<Integer>();
    ArrayList<Integer> list2 = new ArrayList<Integer>();

    long now = System.nanoTime();
    for (int i = 0; i < size; i++) {
      list.add((int) (i * 100 + random.nextDouble() * 1000));
    }

    Collections.sort(list);

    for (int k = 0; k < size; k++) {
      set.addDoc(list.get(k));
    }

    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator dcit = set.iterator();
    int x = 0;
    now = System.nanoTime();
    try {
      int docid;
      while ((docid=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        list2.add(docid);
        // System.out.println(dcit.doc());
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }

    for (int i = 0; i < list.size(); i++) {
      assertEquals(list.get(i).intValue(), list2.get(i).intValue());

    }

  }

 
  @Test
  public void testIntArrayDocIdSetIteratePerformance() {
    System.out.println("");
    System.out.println("Running IntArrayDocIdSet Iterate Performance test");
    System.out.println("----------------------------");
    int size = batch * _length;
    IntArrayDocIdSet set = new IntArrayDocIdSet(_length);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();
    ArrayList<Integer> list = new ArrayList<Integer>();
   
    long now = System.nanoTime();
    for (int i = 0; i < size; i++) {
      list.add((int) (i * 100 + random.nextDouble() * 1000));
    }

    Collections.sort(list);

    for (int k = 0; k < size; k++) {
      set.addDoc(list.get(k));
    }

    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator dcit = set.iterator();
    int x = 0;
    now = System.nanoTime();
    try {
      int docid;
      while ((docid=dcit.nextDoc())!= DocIdSetIterator.NO_MORE_DOCS) {
        x = docid;
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }
    
    double totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Decompression time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * _length)
        / 1000000 + " M numbers");
    

  }
  
  @Test
  public void testP4DDocIdSetIteratePerformance() {
    double booster  = ((_max*1.0)/(1000f*_length));
    
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("");
    System.out.println("Running P4DeltaDocSet Iterate Performance test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits

    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();
    int val = 0 ;
    for (int i = 1; i < _length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        val = randomizer + (int) (random.nextDouble() * 1000);
        list.add(val);
        
      }

      randomizer += 1000*booster;
    }

    Collections.sort(list);
    System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
    
    for (Integer c : list) {
      set.addDoc(c);
    }
    set.optimize();
    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();
    // int x = -1;
    try {
      while (dcit.nextDoc()!=DocIdSetIterator.NO_MORE_DOCS) {
      
      }
    } catch (IOException e1) {
      fail(e1.getMessage());
    }
    totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Decompression time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * _length)
        / 1000000 + " M numbers");
    System.out.println("Compression Ratio : "+ ((double)set.sizeInBytes())/(batch * _length * 4));
  }

  
  @Test
  public void testP4DDocIdSetNonBoundarySkipSanity() {
    double booster  = ((_max*1.0)/(1000f*_length));
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("");
    System.out.println("Running P4DeltaDocSet Non-Boundary skip test");
    System.out.println("----------------------------");
    Random random = new Random();
    int extra = 35;
    int length = 1000;
    if (_length > 100)
      length = _length / 100;

    int size = batch * length;
    int randomizer = 0;
    double totalDecompressionTime = 0;

    List<Integer> list = new LinkedList<Integer>();

    for (int i = 1; i < _length + 1; i++) {
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000*booster;
    }

    randomizer += 1000*booster;
    for (int i = 0; i < extra; i++)
      list.add(randomizer + (int) (random.nextDouble() * 1000));
    int counter = 0;
    
    Collections.sort(list);
    System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
    // System.out.println(list);
    for (Integer c : list)
    {
      counter++;
      set.addDoc(c);
    }
    
    // Measure time to obtain iterator
    long time = System.nanoTime();
    set.optimize();
    System.out.println("Time to optimize set of " + counter + " numbers : "+ (System.nanoTime() - time)+"ns");
    System.out.println("Size in Bytes:"+set.sizeInBytes());
    
    StatefulDSIterator dcit = set.iterator();
    
    
    for (int i = 0; i < size; i += 60) {
      try {
        int docid=dcit.advance(i);
        if (docid!=DocIdSetIterator.NO_MORE_DOCS) {

          assertEquals(docid, list.get(dcit.getCursor()).intValue());
          docid=dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS) 
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
          docid=dcit.nextDoc();
          if (docid!=DocIdSetIterator.NO_MORE_DOCS) 
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
        }
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }

  }
  
  @Test
  public void testP4DDocIdSetNonBoundaryCompressionSanity() throws IOException {
    int extra = 34;
    double booster  = ((_max*1.0)/(1000f*_length));
    int counter = 0;
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("");
    System.out.println("Running P4DeltaDocSet Non-Boundary Compression Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int size = _length;
    if (_length > 100)
      size = _length / 100;

    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();

    for (int i = 1; i < size + 1; i++) {
      for (int k = 0; k < batch; k++) {
        counter++;
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000*booster;
    }

    randomizer += 1000;
    
    for (int i = 0; i < extra; i++)
    {
      counter++;
      list.add(randomizer + (int) (random.nextDouble() * 1000));
    }
    
    Collections.sort(list);
   
    long time = System.nanoTime();
   
    
    for (Integer c : list) {
      
      set.addDoc(c);
    }
    
    
    // Measure time to obtain iterator
    time = System.nanoTime();
    set.optimize();
 
    
    StatefulDSIterator dcit = set.iterator();   

    long now = System.nanoTime();
    int i = 0;
    int docid;
    while ((docid = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      assertEquals(list.get(i++).intValue(), docid);
    }

  }
 
  @Test
  public void testP4DDocIdSetSkipSanity() {
    double booster  = ((_max*1.0)/(1000f*_length));
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("");
    System.out.println("Running P4DeltaDocSet Skip Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();

    int max = 1000;
  
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < _length + 1; i++) {

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000*booster;
    }
    
    Collections.sort(list);
    System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
    long time = System.nanoTime();
   
    int counter=0;
    for (Integer c : list) {
      counter++;
      set.addDoc(c);
    }
    System.out.println("Time to construct:"+(System.nanoTime() - time)+" ns");
    
 // Measure time to obtain iterator
    time = System.nanoTime();
    set.optimize();
    System.out.println("Time to optimize set of " + counter + " numbers : "+ (System.nanoTime() - time)+"ns");
    System.out.println("Size in Bytes:"+set.sizeInBytes());
    StatefulDSIterator dcit = set.iterator();   
    
    
    long now = System.nanoTime();

    for (int i = 0; i < max; i += 600) {
      try {

        int docid = dcit.advance(i);
        if (docid!=DocIdSetIterator.NO_MORE_DOCS) {

          assertEquals(docid, list.get(dcit.getCursor()).intValue());
          if ((docid = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
          if ((docid = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            assertEquals(docid, list.get(dcit.getCursor()).intValue());
        }
      } catch (Exception e) {
        fail(e.getMessage());
      }
    }

  }

  
  @Test
  public void testSkipPerformance() throws IOException
  {
    System.out.println("");
    System.out.println("Running Doc Skip Performance");
    System.out.println("----------------------------");
    
    double booster  = ((_max*1.0)/(1000f*_length));
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("");
    System.out.println("Running P4DeltaDocSet Skip Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();

    int max = 1000;
  
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < _length + 1; i++) {

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000*booster;
    }
    
    Collections.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
   
   
    
      //P4D
      P4DDocIdSet p4d = new P4DDocIdSet();
      int counter=0;
      
      for (Integer c : list) {
        counter++;
        p4d.addDoc(c);
      }
      StatefulDSIterator dcit = p4d.iterator();
     _testSkipPerformance(list.get(list.size()-1),dcit);
   
     // Int Array
     IntArrayDocIdSet iSet = new IntArrayDocIdSet(list.size());
     counter=0;
     
     for (Integer c : list) {
       counter++;
       p4d.addDoc(c);
     }
     dcit = iSet.iterator();
    _testSkipPerformance(list.get(list.size()-1),dcit);
  
    // OBS
    OBSDocIdSet oSet = new OBSDocIdSet(list.size());
    counter=0;
    
    for (Integer c : list) {
      counter++;
      p4d.addDoc(c);
    }
   dcit = oSet.iterator();
   _testSkipPerformance(list.get(list.size()-1),dcit);
  
  }
  
   
  
  private void _testSkipPerformance(int max, StatefulDSIterator dcit) throws IOException {
    
 
    long now = System.nanoTime();
    int ctr = 0;
    for(int i=0;i<max;i++)
    {
        dcit.advance(i);
    }
    
    System.out.println("Skip performance on "+ dcit.getClass().getName()+ ":"+  ( System.nanoTime() - now )+" ns..");
    System.out.flush();
    
  }

  @Test
  @Ignore
  public void testAndDocIdSetPerformance() throws Exception{
    double booster  = ((_max*1.0)/(1000f*_length));
    System.out.println("");
    System.out.println("Running AndDocIdSet Performance test");
    System.out.println("----------------------------");
    int size = _length;

    double totalCompressionTime = 0;
    double totalDecompressionTime = 0;
    double totalCompressionRatio = 0;

    ArrayList<DocIdSet> docSets = new ArrayList<DocIdSet>();
    Random random = new Random();
    // Minimum 5 bits
    int randomizer = 0;

    for (int j = 0; j < all; j++) {
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      P4DDocIdSet docSet = new P4DDocIdSet(batch);
      randomizer = 0;
      for (int i = 1; i < size + 1; i++) {

        for (int k = 0; k < batch; k++) {
          intSet.add(randomizer + (int) (random.nextDouble() * 1000));
        }

        randomizer += 1000*booster;
        Collections.sort(intSet);
      
      }
      for (Integer c : intSet) {
        docSet.addDoc(c);
      }
      docSets.add(docSet);

    }
    System.out.println("Constructed component DocSets");
    org.apache.lucene.search.DocIdSetIterator oit = new AndDocIdSet(docSets).iterator();
    long now = System.nanoTime();
    try {
      int docid;
      while ((docid = oit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      fail(e.getMessage());
    }

    totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Total decompression time :" + totalDecompressionTime
        + ": for " + ((double) batch * size) / 1000000 + " M numbers");

  }

  
  @Test
  @Ignore
  public void testNotDocIdSet() throws IOException {
    System.out.println("");
    System.out.println("Running NotDocIdSet test");
    System.out.println("----------------------------");
    int max = 1000;

    if (_max > 1000)
      max = _max / 1000;

    int length = 100;

    if (_length > 100)
      length = _length / 100;
    Random random = new Random();

    int randomizer = 0;
    int b = 0;
    ArrayList<Integer> intSet = new ArrayList<Integer>();
    P4DDocIdSet docSet = new P4DDocIdSet(batch);
    randomizer = 0;

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        b = randomizer + (int) (random.nextDouble() * 1000);
        intSet.add(b);

      }

      randomizer += 1000;
      Collections.sort(intSet);
      

    }
    for (Integer c : intSet) {
      docSet.addDoc(c);
    }

    org.apache.lucene.search.DocIdSetIterator oit = new NotDocIdSet(docSet, max).iterator();

    int docid;
    while ((docid = oit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      assertFalse(intSet.contains(docid));
    }

  }

}
