package com.kamikaze.test.perf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.junit.Test;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaAndDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;

public class TestCompDecomp  {
  public static void main(String args[]) throws Exception {
    CompDecomp testObj = new CompDecomp();
    int batchSize = 256;
    testObj.init(batchSize);
    for (int i = 0; i < 1; i++) 
    {
      System.out.println("");
      System.out.println("");
      System.out.println("Round " + i);
      System.out.println("");
      testObj.init(batchSize);
      testObj.testAndIntersections();
      testObj.testFind();
      testObj.testCompSizeAndDecompSpeedOfAdvance();
      testObj.testCompSizeAndDecompSpeedOfNextDoc();
      testObj.testSkipPerformance();
      testObj.freeMem();
    }      
  }
}

class CompDecomp{  

   private ArrayList<OpenBitSet> _obs; 
   private ArrayList<DocIdSet> _docs ; 
   private ArrayList<DocIdSet> _docsOld ; 
   
   private int _maxDoc =  1500000;
   private int _numDocs = 650000;
//   private int _maxDoc =  2550;
//   private int _numDocs = 2000;
   
   private int _listNum = 3;
   
   private int _batchSize = 256;
   
   private int _numDocs1;
   private int _numDocs2;
   private int _numDocs3;
   
   private int[] _input1;
   private int[] _input2;
   private int[] _input3;
   
   private OpenBitSet _base;
   
   private long _expectedIntersectionSize ;
   
  
   public void init(int batchSize) throws Exception
   {
     _batchSize = batchSize;
     int randomRange = _maxDoc;
     int randomBase = 0;
     
     Random rand = new Random(System.currentTimeMillis()); 
     
      _numDocs1 = randomBase + rand.nextInt(randomRange);
      _numDocs2 = randomBase + rand.nextInt(_maxDoc);  
      _numDocs3 = randomBase + rand.nextInt(randomRange);
      
    
     
     _input1 = generateRandomDataHY(_maxDoc, _numDocs1);
     _input2 = generateRandomDataHY(_maxDoc, _numDocs2);
     _input3 = generateRandomDataHY(_maxDoc, _numDocs3);
     
     _obs = new ArrayList<OpenBitSet>(); 
     _docs = new ArrayList<DocIdSet>(); 
     _docsOld = new ArrayList<DocIdSet>(); 
     
     loadRandomDataSets(_input1, _obs, _docs, _docsOld, _numDocs1);
     loadRandomDataSets(_input2, _obs, _docs, _docsOld, _numDocs2);
     loadRandomDataSets(_input3, _obs, _docs, _docsOld, _numDocs3);
     
     // get the expected result

     _base = _obs.get(0); 
     for(int i = 1; i < _obs.size(); ++i) 
     { 
       _base.intersect(_obs.get(i)); 
     }     
     _expectedIntersectionSize = _base.cardinality();
     
   }
   
   public void freeMem() throws Exception
   {
     _input1 =  null;
     _input2 =  null;
     _input3 =  null;
     _obs = null;
     _docsOld = null;
     _docs = null;
     
   }
  public void testAndIntersections() throws Exception
  { 
     System.out.println("Running And Intersections Test case...");
     
    
     // test old version
     AndDocIdSet andsOld = new AndDocIdSet(_docs); 
     long card1Old = _base.cardinality(); 
     long startOld = System.currentTimeMillis();
     long card2Old = andsOld.size(); 
     long endOld = System.currentTimeMillis();
     System.out.println("time spent for the old version: "+(endOld-startOld));
     
//     ArrayList<Integer> intersectionResultOld = andsOld.getIntersection();
//     if(card1Old != card2Old  || !compareTwoLists(intersectionResultOld, _expectedIntersectionResult) )
//     {
//       System.out.println("The result for the old version does not match the expectation");
//     }
     //assertEquals(card1Old, card2Old); 
     //printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
     //printList(intersectionResultOld, 0, intersectionResultOld.size()-1);
    //assertEquals(true, compareTwoLists(intersectionResultOld, expectedIntersectionResult));
   
     // test new version
     PForDeltaAndDocIdSet ands = new PForDeltaAndDocIdSet(_docs); 
     long card1 = _base.cardinality(); 
     long start = System.currentTimeMillis();
     long card2 = ands.size(); // hy: this calls the nextDoc() of AndDocIdIterator
     long end = System.currentTimeMillis();
     System.out.println("time spent for the new version: "+(end-start));
     
     //ArrayList<Integer> intersectionResult = ands.getIntersection();
     
     // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
     // printList(intersectionResult, 0, intersectionResult.size()-1);
     //if(card1 != card2 || !compareTwoLists(intersectionResult, _expectedIntersectionResult))
     if(card1 != card2)
     {
       System.out.println("The result for the new version does not match the expectation");
     }
     //assertEquals(card1, card2); 
     //assertEquals(true, compareTwoLists(intersectionResult, expectedIntersectionResult));
     ands = null;
     
     System.out.println("----------------completed---------------------------");
  } 
 

// test the PForDeltaDocIdSet.find() 
  public void testFind() throws Exception
  {     
    System.out.println("Running Find() Test case...");
    
//     numDocs1 = 1493699; 
//     numDocs2 = 2480781;
//     numDocs3 = 2377754; 

     int i;
    
     // test the old version
    // ArrayList<Integer> intersectionResultOld = new ArrayList<Integer>();
     P4DDocIdSet pfd0Old = (P4DDocIdSet)_docsOld.get(0);
     P4DDocIdSet pfd1Old = (P4DDocIdSet)_docsOld.get(1);
     P4DDocIdSet pfd2Old = (P4DDocIdSet)_docsOld.get(2);
     DocIdSetIterator iterOld = pfd0Old.iterator();
     long startOld = System.currentTimeMillis();
     
     int docIdOld;
     int intersectionSizeOld = 0;
     for(int j=0; j<_numDocs1; j++)
     {
       docIdOld = _input1[j];
       if(pfd1Old.find(docIdOld) && pfd2Old.find(docIdOld))
       {
         //intersectionResultOld.add(docIdOld);
         intersectionSizeOld++;
       }
     }
     long endOld = System.currentTimeMillis();
     System.out.println("time spent for the old version:  "+(endOld-startOld));
     if(intersectionSizeOld != _expectedIntersectionSize)
     {
       System.out.println("The result for the new version does not match the expectation");
     }
     
     // test the new version
     //ArrayList<Integer> intersectionResult = new ArrayList<Integer>();
     PForDeltaDocIdSet pfd0 = (PForDeltaDocIdSet)_docs.get(0);
     PForDeltaDocIdSet pfd1 = (PForDeltaDocIdSet)_docs.get(1);
     PForDeltaDocIdSet pfd2 = (PForDeltaDocIdSet)_docs.get(2);
     DocIdSetIterator iter = pfd0.iterator();
     
     long start = System.currentTimeMillis();
     
     int docId;
     int intersectionSize = 0;
     for(i=0; i<_numDocs1; i++)
     {
       docId = _input1[i];
       if(pfd1.find(docId) && pfd2.find(docId))
       {
         //intersectionResult.add(docId);
         intersectionSize++;
       }
     }
 
     long end = System.currentTimeMillis();
     System.out.println("time spent for the new version:  "+(end-start));
    
     //System.out.println("Intersectoin result");
     //printList(intersectionResult, 0, intersectionResult.size()-1);
     // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
     
     //if(!compareTwoLists(intersectionResult, _expectedIntersectionResult))
     if(intersectionSize != _expectedIntersectionSize)
     {
       System.out.println("The result for the new version does not match the expectation");
     }
     
     pfd0 = null;
     pfd1 = null;
     pfd2 = null;
     System.out.println("-------------------completed------------------------");
  } 
  
// test decompression speed using nextDoc 
  public void testCompSizeAndDecompSpeedOfNextDoc() throws Exception
  {     
    System.out.println("Running Comp/Decomp Test for nextDoc()...");
   
  
    //ArrayList<Integer> input = bitSetToArrayList(_obs.get(0));
      
    int docId;
    
   // test the old version
    //ArrayList<Integer> outputOld = new ArrayList<Integer>();
    int intersectionSizeOld = 0;
    long startOld = System.currentTimeMillis();
    P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(0);
    DocIdSetIterator iterOld = pfdOld.iterator();
    docId = iterOld.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      //outputOld.add(docId);
      intersectionSizeOld++;
      docId = iterOld.nextDoc();
      //docId = iterOld.advance(docId+1);
    }
    long endOld = System.currentTimeMillis();
    System.out.println("time spent for the old version: : "+(endOld-startOld));
    System.out.println("compressed size for the old version: " + pfdOld.getCompressedBitSize()/32 + " ints");
    
    // test the new version
    //ArrayList<Integer> output = new ArrayList<Integer>();
   
    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)_docs.get(0);
    System.out.println("compressed size for the new version: " + pfdDS.getCompressedBitSize()/32 + " ints");
    DocIdSetIterator iter = pfdDS.iterator();
    long start = System.currentTimeMillis();
    docId = iter.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      //output.add(docId);
      //intersectionSize++;
      docId = iter.nextDoc();
      //docId = iter.advance(docId+1);
    }
    long end = System.currentTimeMillis();
    System.out.println("time spent for the new version: "+(end-start));
   
    //printList(input, 0, input.size()-1);
    //printList(output, 0, output.size()-1);
    //if(!compareTwoLists(_input1, _numDocs1, output))
   
    pfdOld = null;
    pfdDS = null;
    System.out.println("-------------------completed------------------------");
  } 
  

  public void testSkipPerformance() throws IOException
  {
    System.out.println("");
    System.out.println("Running Doc Skip Performance");
    System.out.println("----------------------------");
    
    int length = _numDocs1/_batchSize;
    
    double booster  = 5;
    P4DDocIdSet set = new P4DDocIdSet(_batchSize);
    System.out.println("");
    System.out.println("Running skip performance test");
    System.out.println("----------------------------");
    Random random = new Random();

    int NN = 1000;
  
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length; i++) {

      for (int k = 0; k < _batchSize; k++) {
        list.add(randomizer + (int) (random.nextDouble() * NN));
      }

      randomizer += NN*booster;
    }
    
    Collections.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
  
    
      //P4D
      P4DDocIdSet p4dOld = new P4DDocIdSet();
      int counter=0;
      
      for (Integer c : list) {
        counter++;
        p4dOld.addDoc(c);
      }
      StatefulDSIterator dcitOld = p4dOld.iterator();
     _testSkipPerformance(list.get(list.size()-1),dcitOld, false);
   
     
  
    PForDeltaDocIdSet p4d = new PForDeltaDocIdSet();
    counter=0;
    
    for (Integer c : list) {
      counter++;
      p4d.addDoc(c);
    }
    StatefulDSIterator dcit = p4d.iterator();
   _testSkipPerformance(list.get(list.size()-1),dcit, true);
   
  
  }
  
   
  
  private void _testSkipPerformance(int max, StatefulDSIterator dcit, boolean usingNewVersion) throws IOException {
    
 
    long now = System.currentTimeMillis();
    int ctr = 0;
    for(int i=0;i<max;i++)
    {
        dcit.advance(i);
    }
    if(usingNewVersion)
      System.out.println("New Skip performance on "+ dcit.getClass().getName()+ ":"+  ( System.currentTimeMillis() - now )+" ms..");
    else
      System.out.println("Old Skip performance on "+ dcit.getClass().getName()+ ":"+  ( System.currentTimeMillis() - now )+" ms..");
    System.out.flush();
    
  }
  
//test decompression speed using nextDoc 
  public void testCompSizeAndDecompSpeedOfAdvance() throws Exception
  {     
    System.out.println("Running Comp Decomp Test case for advance()...");
    
   
    //ArrayList<Integer> input = bitSetToArrayList(_obs.get(0));
      
    int docId;
    
 // test the old version
    //ArrayList<Integer> outputOld = new ArrayList<Integer>();
    int intersectionsizeOld = 0;
    long startOld = System.currentTimeMillis();
    P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(0);
    DocIdSetIterator iterOld = pfdOld.iterator();
    docId = iterOld.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      //outputOld.add(docId);
      intersectionsizeOld++;
      //docId = iterOld.nextDoc();
      docId = iterOld.advance(docId+1);
    }
    long endOld = System.currentTimeMillis();
    System.out.println("time spent for the old version: : "+(endOld-startOld));
    System.out.println("compressed size for the old version: " + pfdOld.getCompressedBitSize()/32 + " ints");
    
    // test the new version
    //ArrayList<Integer> output = new ArrayList<Integer>();
   
    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)_docs.get(0);
    System.out.println("compressed size for the new version: " + pfdDS.getCompressedBitSize()/32 + " ints");
    DocIdSetIterator iter = pfdDS.iterator();
    long start = System.currentTimeMillis();
    docId = iter.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      //output.add(docId);
 
      //docId = iter.nextDoc();
      docId = iter.advance(docId+1);
    }
    long end = System.currentTimeMillis();
    System.out.println("time spent for the new version: "+(end-start));
   
    //printList(input, 0, input.size()-1);
    //printList(output, 0, output.size()-1);
    //if(!compareTwoLists(_input1, _numDocs1, output))
 
    pfdDS = null;
    pfdOld = null;
    System.out.println("-------------------completed------------------------");
  } 
  
  private OpenBitSet createObs(int nums[], int maxDoc) { 
    OpenBitSet bitSet = new OpenBitSet(maxDoc); 
    for(int num:nums) 
      bitSet.set(num); 
    return bitSet; 
  } 
  
  private DocIdSet createDocSet(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSetOld(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  }
  
  
  
  // hy: convert an openBitSet to an array list 
  private ArrayList<Integer> bitSetToArrayList(OpenBitSet bs)
  {
    ArrayList<Integer> listRes = new ArrayList<Integer>();
    for(int i=0; i<bs.capacity(); i++)
    {
      if(bs.get(i))
      {
         listRes.add(i);
      }
     }
     return listRes;
  }
  
  private void loadRandomDataSets(int[] data, ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, ArrayList<DocIdSet> docsOld, int maxDoc) throws Exception
  {
    obs.add(createObs(data, maxDoc)); 
    docs.add(createDocSet(data)); 
    
    docsOld.add(createDocSetOld(data));
    
  }
 
//hy: compare two lists to see if they are equal and print all the different numbers
  private boolean compareTwoLists(int[] input, int size, ArrayList<Integer> output)
  {
    //System.out.println("inputSize:" + input.size() + "outputSize:" + output.size());
    int i=0;
    boolean ret = true;
    for(i=0; i<size && i<output.size(); ++i)
    {
      if(input[i] != output.get(i).intValue())
      {
        System.out.println("in[" + i + "]" + input[i] + " != out[" + i + "]" + output.get(i));
        ret = false;
      }
    }
    if(i<size)
    {
      printList(input, i, size);
      ret = false;
    }
    if(i<output.size())
    {
      printList(output, i, output.size());
      ret = false;
    }
    return ret;
  }
  
  
  // hy: compare two lists to see if they are equal and print all the different numbers
  private boolean compareTwoLists(ArrayList<Integer> input, ArrayList<Integer> output)
  {
    System.out.println("inputSize:" + input.size() + "outputSize:" + output.size());
    int i=0;
    boolean ret = true;
    for(i=0; i<input.size() && i<output.size(); ++i)
    {
      if(input.get(i).intValue() != output.get(i).intValue())
      {
        System.out.println("in[" + i + "]" + input.get(i) + " != out[" + i + "]" + output.get(i));
        ret = false;
      }
    }
    if(i<input.size())
    {
      printList(input, i, input.size());
      ret = false;
    }
    if(i<output.size())
    {
      printList(output, i, output.size());
      ret = false;
    }
    return ret;
  }
  
  // hy: print a list 
  private void printList(ArrayList<Integer> list, int start, int end)
  {
    System.out.print("(" + (end-start+1) + ")[");
    for(int i=start; i<=end; ++i)
    {
      System.out.print(list.get(i));
      //System.out.print(", ");
      System.out.print(", " + i + "; ");
   
    }
    System.out.println("]");
  }
  
  //hy: print an array
  private void printList(int[] list, int start, int end)
  {
    System.out.print("(" + (end-start+1) + ")[");
    for(int i=start; i<=end; ++i)
    {
      System.out.print(list[i]);
      System.out.print(", ");
    }
    System.out.println("]");
  }
  
//hy: generate numDocs numbers out of maxDoc numbers
  private int[] generateRandomDataHY(int maxDoc, int numDocs)
  {
    //System.out.println("generating random data");
    Random rand = new Random(System.currentTimeMillis()); 
    int[] ori = new int[maxDoc];
    int i,j;
    
    for(i =0; i<maxDoc; ++i)
    {
      ori[i] = i;
    }
    
    int num = maxDoc;
    int tmp;
    int[] randomNums = new int[numDocs];
    
    for (j = 0; j < numDocs; j++, num--) 
    { 
      int nextDoc = rand.nextInt(num);
      
      randomNums[j] = ori[nextDoc];
      tmp = ori[num-1];
      ori[num-1] = ori[nextDoc];
      ori[nextDoc] = tmp;
      
      //printArray(ori, 0, maxDoc-1);
      //printArray(randomNums, 0, numDocs-1);
    }
    
    Arrays.sort(randomNums);
    System.out.println("randomly select " + numDocs + " numbers from " + maxDoc +  " numbers");
    //printArray(randomNums, 0, numDocs-1);
    return randomNums;
  }
  
}
