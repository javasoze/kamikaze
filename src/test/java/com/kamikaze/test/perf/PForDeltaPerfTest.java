package com.kamikaze.test.perf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.junit.Test;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet; 
import com.kamikaze.docidset.impl.P4DDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;

/**
 * This class provides a variety of tests to compare the performance of the old and new versions of Kamikaze. In particular,
 * 1) the compression size and decompression speed for DocIdSet
 * 2) the serialized size of DocIdSet objects
 * 3) the speed to find the intersection of multiple DocIdSets (AndDocIdSet.nextDoc()).
 * 4) the speed of DocIdSet.find()
 * @author hao yan
 *
 */
public class PForDeltaPerfTest  {
  public static enum METHODS {IPAND, COMPDECOMP, AND, FIND, SERIAL};

  public static void main(String args[]) throws Exception {
    PerfTests testObj = new PerfTests();
    
    // for each of the following tests, we use three lists with different lengths for each run. 
    // For example, in test 1, we run 4 runs. For the first run we use three lists with 15000, 37500, 75000 random integers respectively 
    // For the second run, we use lists with 150000,375000,750000 random integers respectively
    
    // test 1
    int[][] inputSizeCompDecomp = {
      {15000,37500,75000},
      {150000,375000,750000}, 
      {1500000, 3750000, 7500000},
      {1500,3750,7500}};
    System.out.println("COMPDECOMP test ---------- ");
    for(int i=0; i<inputSizeCompDecomp.length; ++i)
    {
       doTests(testObj, METHODS.COMPDECOMP, inputSizeCompDecomp[i]);
    }
    System.out.println("--------- COMPDECOMP test is completed ----------------");
    System.out.println(" ");
    
//    // test 2
    int[][] inputSizeCompSerial = {
        {15000,37500,75000},
        {150000,375000,750000}, 
        {1500000, 3750000, 7500000},
        {1500,3750,7500}};
    System.out.println("SERIAL test ---------- ");
    for(int i=0; i<inputSizeCompSerial.length; ++i)
    {
       doTests(testObj, METHODS.SERIAL,inputSizeCompSerial[i]);
    }
    System.out.println("--------- SERIAL test is completed ----------------");
    System.out.println(" ");
    
//    // test 3
    int[][] inputSizeCompAnd = {
        {15000,37500,75000},
        {150000,375000,750000}, 
        {1500000, 3750000, 7500000},
        {3750000,7500000,15000000},
        {15000,3750000,750000},
        {15000,375000,75000},
        {1500,375000,75000}};
    System.out.println("AND test ---------- ");
    for(int i=0; i<inputSizeCompAnd.length; ++i)
    {
       doTests(testObj, METHODS.AND, inputSizeCompAnd[i]);
    }
    System.out.println("--------- AND test is completed ----------------");
    System.out.println(" ");
      
    // test 4
    int[][] inputSizeCompFind = {
          {15000,37500,75000},
          {150000,375000,750000}, 
          {1500000, 3750000, 7500000},
          {3750000,7500000,15000000},
          {15000,3750000,750000},
          {15000,375000,75000},
          {1500,375000,75000}};
    System.out.println("FIND test ---------- ");
    for(int i=0; i<inputSizeCompFind.length; ++i)
    {
     doTests(testObj, METHODS.FIND,inputSizeCompFind[i]);
    }
    System.out.println("--------- FIND test is completed ----------------");
    System.out.println(" ");
      
//    // test 5
    int[][] inputSizeIPAND = {{15000,37500,75000},
        {150000,375000,750000}, 
        {1500000, 3750000, 7500000},
        {3750000,7500000,15000000},
        {15000,3750000,750000},
        {15000,375000,75000},
        {1500,375000,75000}};
    System.out.println("IPAND test ---------- ");
    for(int i=0; i<inputSizeIPAND.length; ++i)
    {
      doTests(testObj, METHODS.IPAND, inputSizeIPAND[i]);
    }
    System.out.println("--------- IPAND test is completed ----------------");
    System.out.println(" ");
//    
  }

 // we do tryTimes times of tests and get the average results of the last (tryTimes-1) tests
 public static void doTests(PerfTests testObj, METHODS method, int[] numDocs) throws Exception
 {
   int batchSize = 256;
    int tryTimes = 10;
    testObj.reset(numDocs);
    boolean onlyTestBatchComp = false;
    System.out.println("tryTimes: " + tryTimes);
    for (int i = 0; i < tryTimes; i++) 
    {
      System.out.println("");
      System.out.println("");
      System.out.println("Round " + i);
      System.out.println("");
      
      if(!onlyTestBatchComp)
      {
        testObj.init(batchSize);
        switch(method)
        {
          case  IPAND: 
            testObj.testCompareIntArrayAndPForDeltaWithBaseForAndIntersection(); 
            break;
          case COMPDECOMP:
            testObj.testCompSizeAndDecompSpeedOfNextDoc();
            break;
          case AND:
            testObj.testAndIntersections(); 
            break;
          case FIND:
            testObj.testFind(); 
            break;
          case SERIAL:
            testObj.testSerializationFileStrm(); 
            break;
        }
      }
      else
      {
        testObj.initAndTestCompTime(batchSize);
      }
      
      
     //testObj.testSerializationByteStrmOld();
     // testObj.testSerializationByteStrmNew();
      
      // first round is warm-up , does not count it in 
      if(i==0)
      {
        Arrays.fill(testObj._intArrayTime,0);
        Arrays.fill(testObj._oldTime,0);
        Arrays.fill(testObj._newTime,0);
      }
      
      testObj.freeMem();
    }    //for
    
    System.out.println(" ");
    
    if(onlyTestBatchComp)
    {
      for(int i=0; i<testObj._listNum; ++i)
      { 
        System.out.println("final statistics for ONLY batch comp time test: ---------- ");
        System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
        System.out.println(", avg no batch comp time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg batch comp time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
      }
      return;
    }
    
    for(int i=0; i<testObj._listNum; ++i)
    { 
      switch(method)
      {
        case  IPAND: 
          System.out.println("final statistics for IPAND: ---------- ");
          System.out.println("intersection size: " + testObj._expectedIntersectionSize);
          System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
          System.out.println("avg intArray time:" + (double)testObj._intArrayTime[i]/(double)(tryTimes-1) + ", avg old time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg new time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
          break;
        case COMPDECOMP:
          System.out.println("final statistics for COMPDECOMP: ---------- ");
          System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
          System.out.println("avg old comp size: " + testObj._oldCompByteSize[i]/(tryTimes-1) + ",  avg new comp size: " + testObj._newCompByteSize[i]/(tryTimes-1));
          System.out.println("avg old time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg new time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
          break;
        case AND:
          System.out.println("final statistics for AND: ---------- ");
          System.out.println("intersection size: " + testObj._expectedIntersectionSize);
          System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
          System.out.println("avg old time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg new time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
          break;
        case FIND:
          System.out.println("final statistics for FIND: ---------- ");
          System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
          System.out.println("avg old time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg new time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
          break;
        case SERIAL:
          System.out.println("final statistics for SERIAL: ---------- ");
          System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
          System.out.println("avg old serialization size: " + testObj._oldSerializationByteSize[i]/(tryTimes-1) + ",  avg new serialization size: " + testObj._newSerializationByteSize[i]/(tryTimes-1));
          break;
      }
      //System.out.println("intersection size: " + testObj._expectedIntersectionSize);
      //System.out.println("Input size: " + testObj._numDocs[i] + " random numbers out of " + testObj._maxDoc +  " numbers; ");
      //System.out.println("avg old comp size: " + testObj._oldCompByteSize[i]/(tryTimes-1) + ",  avg new comp size: " + testObj._newCompByteSize[i]/(tryTimes-1));
      //System.out.println("avg intArray time:" + (double)testObj._intArrayTime[i]/(double)(tryTimes-1) + ", avg old time: " + (double)testObj._oldTime[i]/(double)(tryTimes-1) + ",  avg new time: " + (double)testObj._newTime[i]/(double)(tryTimes-1));
      //System.out.println("avg old serialization size: " + testObj._oldSerializationByteSize[i]/(tryTimes-1) + ",  avg new serialization size: " + testObj._newSerializationByteSize[i]/(tryTimes-1));
    } // for
    System.out.println("------------------------------ ");
 }
}

class PerfTests{  

   private ArrayList<OpenBitSet> _obs; 
   private ArrayList<DocIdSet> _docs ; 
   private ArrayList<DocIdSet> _docsOld ; 
   private ArrayList<DocIdSet> _docsIntArray; 
   
   
   public int _maxDoc =  75000000;
   
   public int _listNum = 3;
   
   public int _batchSize = 256;
   
   public int[] _numDocs;
   
   private int[] _originalInput;
   private int[][] _input;
   
   public OpenBitSet _base;
   
   public long _expectedIntersectionSize ;
   
  
   public long[] _intArrayTime;
   public long[] _oldTime;
   public long[] _newTime;
   public long[] _listLength;
   public long _intersectionLength;
   public long[] _oldCompByteSize;
   public long[] _newCompByteSize;
   public long[] _oldSerializationByteSize;
   public long[] _newSerializationByteSize;
   public ArrayList<Integer> _expectedIntersectionResult;
   
   public PerfTests() {
     _intArrayTime = new long[_listNum];
     _oldTime = new long[_listNum];
     _newTime = new long[_listNum];
     _numDocs  = new int[_listNum];
     _input = new int[_listNum][];
     _listLength = new long[_listNum];
     _oldCompByteSize = new long[_listNum];
     _newCompByteSize = new long[_listNum];
     _oldSerializationByteSize = new long[_listNum];
     _newSerializationByteSize = new long[_listNum];
     Arrays.fill(_intArrayTime,0);
     Arrays.fill(_oldTime,0);
     Arrays.fill(_newTime,0);
     Arrays.fill(_oldCompByteSize, 0);
     Arrays.fill(_newCompByteSize, 0);
     Arrays.fill(_oldSerializationByteSize, 0);
     Arrays.fill(_newSerializationByteSize, 0);
     
     _intersectionLength = 0;
   }
   
   public void reset(int[] numDocs)
   {
     Arrays.fill(_intArrayTime,0);
     Arrays.fill(_oldTime,0);
     Arrays.fill(_newTime,0);
     Arrays.fill(_oldCompByteSize, 0);
     Arrays.fill(_newCompByteSize, 0);
     Arrays.fill(_oldSerializationByteSize, 0);
     Arrays.fill(_newSerializationByteSize, 0);
     
     _listNum = numDocs.length;
     System.arraycopy(numDocs,0,_numDocs,0, _listNum);
   }
   
   public void init(int batchSize) throws Exception
   {
     _batchSize = batchSize;

     // specify the test data length
//      _numDocs[0] = _maxDoc/50;
//      _numDocs[1] = _maxDoc/20;
//      _numDocs[2] = _maxDoc/10;  
      
  // _numDocs[0] = _maxDoc/50;
  // _numDocs[1] = _maxDoc/20;
  // _numDocs[2] = _maxDoc/10;  
     
//      _numDocs[0] = _maxDoc/20;
//      _numDocs[1] = _maxDoc/10;
//      _numDocs[2] = _maxDoc/5;  
      
      _obs = new ArrayList<OpenBitSet>(); 
      _docs = new ArrayList<DocIdSet>(); 
      _docsOld = new ArrayList<DocIdSet>(); 
      _docsIntArray = new ArrayList<DocIdSet>();
      _expectedIntersectionResult = new ArrayList<Integer>();
      
      _originalInput = new int[_maxDoc];
      for(int i =0; i<_maxDoc; ++i)
      {
        _originalInput[i] = i;
      }
      
      for(int i=0; i<_listNum; ++i)
      {
        _input[i] = generateRandomDataHY(_originalInput,_maxDoc, _numDocs[i]);
        //loadRandomDataSets(_input[i], _obs, _docs, _docsOld, _numDocs[i]);
        loadRandomDataSets(_input[i], _obs, _docs, _docsOld, _numDocs[i]);
        loadRandomDataSetsOldIsIntArray(_input[i], _docsIntArray);
       //printList(_input[i],0, _numDocs[i]-1);
      }     
      
     // get the expected result
     _base = _obs.get(0); 
     for(int i = 1; i < _obs.size(); ++i) 
     { 
       _base.intersect(_obs.get(i)); 
     }     
     _expectedIntersectionSize = _base.cardinality();
     
     System.out.println("_base.cardinality()" + _base.cardinality());
     for(int k=0; k<_base.size(); ++k)
     {
       if(_base.get(k))
         _expectedIntersectionResult.add(k);
     }
     
     //printList(_expectedIntersectionResult,0,_expectedIntersectionResult.size()-1);
   }

   public void initAndTestCompTime(int batchSize) throws Exception
   {
     _batchSize = batchSize;

      _obs = new ArrayList<OpenBitSet>(); 
      _docs = new ArrayList<DocIdSet>(); 
      _docsOld = new ArrayList<DocIdSet>(); 
      _docsIntArray = new ArrayList<DocIdSet>();
      _expectedIntersectionResult = new ArrayList<Integer>();
      
      _originalInput = new int[_maxDoc];
      for(int i =0; i<_maxDoc; ++i)
      {
        _originalInput[i] = i;
      }
      
      for(int i=0; i<_listNum; ++i)
      {
        _input[i] = generateRandomDataHY(_originalInput,_maxDoc, _numDocs[i]);
        
        long start = System.currentTimeMillis();
        DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
        for(int in:_input[i]) 
        {         
          p4d.addDoc(in);
        }
        long end = System.currentTimeMillis();
        _oldTime[i] += (end - start);
        
        long startBatch = System.currentTimeMillis();
        DocSet p4dBatch = DocSetFactory.getPForDeltaDocSetInstance(); 
        p4dBatch.addDocs(_input[i], 0, _input[i].length); 
        long endBatch = System.currentTimeMillis();
        _newTime[i] += (endBatch - startBatch);
        
      }     
      
      
   }
   
   public void freeMem() throws Exception
   {
     for(int i=0; i<_listNum; i++)
     {
       _input[i] = null;
     }
     _originalInput = null;
     _obs = null;
     _docsOld = null;
     _docs = null;
   } 
  
   // test PForDeltaAndDocIdSet.nextDoc()
  public void testAndIntersections() throws Exception
  { 
     System.out.println("Running And Intersections Test case...");
    
     for(int testNo=0; testNo<_listNum; ++testNo)
     {
       // test old version
       AndDocIdSet andsOld = new AndDocIdSet(_docsOld); 
       long card1Old = _base.cardinality(); 
     
       long startOld = System.currentTimeMillis();
       long card2Old = andsOld.size(); 
       long endOld = System.currentTimeMillis();
       
       System.out.println("time spent for the old version: "+(endOld-startOld));
       _oldTime[testNo] += (endOld - startOld);
       //ArrayList<Integer> intersectionResultOld = andsOld.getIntersection();
       //if(card1Old != card2Old  || !compareTwoLists(intersectionResultOld, _expectedIntersectionResult) )
       if(card1Old != card2Old )
       {
         System.out.println("The result for the old version does not match the expectation");
       }
   
       // test new version
       AndDocIdSet ands = new AndDocIdSet(_docs); 
       long card1 = _base.cardinality(); 
       long start = System.currentTimeMillis();
       long card2 = ands.size(); // hy: this calls the nextDoc() of AndDocIdIterator
       long end = System.currentTimeMillis();
       System.out.println("time spent for the new version: "+(end-start));
       _newTime[testNo] += (end-start);
     
       //  ArrayList<Integer> intersectionResult = ands.getIntersection();
      // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
      // printList(intersectionResult, 0, intersectionResult.size()-1);
      // if(card1 != card2 || !compareTwoLists(intersectionResult, _expectedIntersectionResult))
       if(card1 != card2)
       {
         System.out.println("The result for the new version does not match the expectation");
       }
       andsOld = null;
       ands = null;
     }
     System.out.println("----------------completed---------------------------");
  } 
 
  public void testCompareIntArrayAndPForDeltaWithBaseForAndIntersection() throws Exception
  {
    System.out.println("Running Comparing IntArray and PForDelta And Intersections Test case...");
    
    for(int testNo=0; testNo<_listNum; ++testNo)
    {
      // test IntArrayDocIdSet
      AndDocIdSet andsIntArray = new AndDocIdSet(_docsIntArray); 
      long card1IntArray = _base.cardinality(); 
      long startIntArray = System.currentTimeMillis();
      long card2IntArray = andsIntArray.size(); 
      long endIntArray = System.currentTimeMillis();
      
      System.out.println("time spent for the intArray version: "+(endIntArray-startIntArray));
      _intArrayTime[testNo] += (endIntArray - startIntArray);
      //ArrayList<Integer> intersectionResultOld = andsOld.getIntersection();
      //if(card1Old != card2Old  || !compareTwoLists(intersectionResultOld, _expectedIntersectionResult) )
      if(card1IntArray != card2IntArray )
      {
        System.out.println("The result for the old version does not match the expectation");
      }
      
      // test old version
      AndDocIdSet andsOld = new AndDocIdSet(_docsOld); 
      long card1Old = _base.cardinality(); 
    
      long startOld = System.currentTimeMillis();
      long card2Old = andsOld.size(); 
      long endOld = System.currentTimeMillis();
      
      System.out.println("time spent for the old version: "+(endOld-startOld));
      _oldTime[testNo] += (endOld - startOld);
      //ArrayList<Integer> intersectionResultOld = andsOld.getIntersection();
      //if(card1Old != card2Old  || !compareTwoLists(intersectionResultOld, _expectedIntersectionResult) )
      if(card1Old != card2Old )
      {
        System.out.println("The result for the old version does not match the expectation");
      }
  
      // test new version
      AndDocIdSet ands = new AndDocIdSet(_docs); 
      long card1 = _base.cardinality(); 
      long start = System.currentTimeMillis();
      long card2 = ands.size(); // hy: this calls the nextDoc() of AndDocIdIterator
      long end = System.currentTimeMillis();
      System.out.println("time spent for the new version: "+(end-start));
      _newTime[testNo] += (end-start);
    
      //  ArrayList<Integer> intersectionResult = ands.getIntersection();
     // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
     // printList(intersectionResult, 0, intersectionResult.size()-1);
     // if(card1 != card2 || !compareTwoLists(intersectionResult, _expectedIntersectionResult))
      if(card1 != card2)
      {
        System.out.println("The result for the new version does not match the expectation");
      }
      andsOld = null;
      ands = null;
    }
    System.out.println("----------------completed---------------------------");
  }
  
// test PForDeltaDocIdSet.find() 
  public void testFind() throws Exception
  {     
    System.out.println("Running Find() Test case...");
    
    int i,k;
    for(int testNo=0; testNo<1; ++testNo)
    {
       // test the old version
       P4DDocIdSet[] pfdOld = new P4DDocIdSet[_listNum];
       for(i=0; i<_listNum; i++)
       {
         pfdOld[i] = (P4DDocIdSet)_docsOld.get(i);
       }
       
       ArrayList<Integer> intersectionResultOld = new ArrayList<Integer>();
      
       int docIdOld;
       int intersectionSizeOld = 0;
       long startOld = System.currentTimeMillis();
       for(i=0; i<_numDocs[0]; ++i)
       {
         docIdOld = _input[0][i];
         for(k=1; k<_listNum; ++k)
         {
           if(!pfdOld[k].find(docIdOld))
             break;
         }
         if(k == _listNum)
         {
           //intersectionResult.add(docId);
           intersectionSizeOld++;
        }
       }
       long endOld = System.currentTimeMillis();
       System.out.println("time spent for the old version:  "+(endOld-startOld));
       if(intersectionSizeOld != _expectedIntersectionSize)
       {
         System.out.println("The result for the old version does not match the expectation");
       }
       //printList(intersectionResultOld, 0, _numDocs2-1);
       _oldTime[testNo] += (endOld - startOld);
     
       // test the new version
       int docId;
       int intersectionSize = 0;
       ArrayList<Integer> intersectionResult = new ArrayList<Integer>();
       PForDeltaDocIdSet[]  pfd = new PForDeltaDocIdSet[_listNum];
       for(i=0; i<_listNum; i++)
       {
         pfd[i] = (PForDeltaDocIdSet)_docs.get(i);
       }
     
       long start = System.currentTimeMillis();
       for(i=0; i<_numDocs[0]; ++i)
       {
         docId = _input[0][i];
         for(k=1; k<_listNum; ++k)
         {
           if(!pfd[k].find(docId))
             break;
         }
         if(k == _listNum)
         {
           //intersectionResult.add(docId);
           intersectionSize++;
         }
       }
       long end = System.currentTimeMillis();
       System.out.println("time spent for the new version:  "+(end-start));

       if(intersectionSize != _expectedIntersectionSize)
       {
         System.out.println("The result for the new version does not match the expectation, intersectionSize: " + intersectionSize + ", _expectedIntersectionSize: " + _expectedIntersectionSize);
       }
       // printList(intersectionResult, 0,intersectionSize-1);
       _newTime[testNo] += (end - start);
       _intersectionLength += intersectionSize;
//     if(!compareTwoLists(intersectionResult, intersectionResultOld))
//     {
//       System.out.println("The result for the new version does not match the expectation, intersectionSize: " + intersectionSize + ", _expectedIntersectionSize: " + _expectedIntersectionSize);
//     }
     
       for(i=0; i<_listNum; ++i)
       {
          pfdOld[i] = null;
          pfd[i] = null;
       }
       intersectionResultOld = null;
       intersectionResult = null;
    }
     System.out.println("-------------------completed------------------------");
  } 

  // test decompression speed using nextDoc 
  public void testCompSizeAndDecompSpeedOfNextDoc() throws Exception
  {     
    System.out.println("Running Comp/Decomp Test for nextDoc()...");
   
    int docId;
    for(int testNo=0; testNo<_listNum; ++testNo)
    {
      P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(testNo);
      DocIdSetIterator iterOld = pfdOld.iterator();
      
      long startOld = System.currentTimeMillis();
      docId = iterOld.nextDoc();
      int i=0;
      //System.out.println();
      //System.out.print("old: { (" + docId + "," + i + ");");
      while(docId !=DocIdSetIterator.NO_MORE_DOCS)
      {      
        docId = iterOld.nextDoc();
        i++;
        //System.out.print("(" + docId + "," + i + "),");
      }
      //System.out.println("}");
      long endOld = System.currentTimeMillis();
      
      System.out.println("time spent for the old version: : "+(endOld-startOld));
      System.out.println("compressed size for the old version: " + pfdOld.getCompressedBitSize()/8 + " bytes");
      _oldCompByteSize[testNo] += pfdOld.getCompressedBitSize()/8;
      _oldTime[testNo] += (endOld - startOld);
      
      ArrayList<Integer> output = new ArrayList<Integer>();
      PForDeltaDocIdSet pfd = (PForDeltaDocIdSet)_docs.get(testNo);
      
      DocIdSetIterator iter = pfd.iterator();
     
      long start = System.currentTimeMillis();
      //i = 0;
      docId = iter.nextDoc();
      //System.out.println();
      //System.out.print("new: { (" + docId + "," + i + ");");
      while(docId !=DocIdSetIterator.NO_MORE_DOCS)
      {      
        //output.add(docId);
        docId = iter.nextDoc();
        //i++;
        //System.out.print("(" + docId + "," + i + "),");
      }
      //System.out.println("}");
      long end = System.currentTimeMillis();
      
      System.out.println("time spent for the new version: "+(end-start));
      System.out.println("compressed size for the new version: " + pfd.getCompressedBitSize()/8 + " bytes");
      _newCompByteSize[testNo] += pfd.getCompressedBitSize()/8;
      _newTime[testNo] += (end-start);
//      if(!compareTwoLists(_input[testNo], _numDocs[testNo], output))
//      {
//        System.out.println("wrong output");
//        return;
//      }
      
      pfdOld = null;
      pfd = null;
    }
    System.out.println("-------------------completed------------------------");
  } 
  
  
  
//test serialization size (serialize data to a file and then read the size of the file)  
 public void testSerializationFileStrm() throws Exception
 {     
   System.out.println("Running Serialization FileStream Test");
  
   int docId;

   int intersectionSizeOld = 0;
   for(int testNo=0; testNo<_listNum; ++testNo)
   {
     // test old version
     
     // serialize the object into a file 
     File fOld = new File("serialOld");
     P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(testNo);
     ObjectOutputStream oosOld = new ObjectOutputStream(new FileOutputStream(fOld));
     oosOld.writeObject(pfdOld);
     oosOld.flush();
     oosOld.close();
     System.out.println("old serialized size: " + fOld.length() + " bytes");
     
     _oldSerializationByteSize[testNo] += fOld.length();
     // deserialize it to pfdOld_DE
     ObjectInputStream oisOld = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fOld)));
     P4DDocIdSet pfdOld_DE  = (P4DDocIdSet)oisOld.readObject();
     oisOld.close();
   
     // check if the deserialized object == the original object
     DocIdSetIterator iterOld = pfdOld_DE.iterator();
     docId = iterOld.nextDoc();
     ArrayList<Integer> outputOld = new ArrayList<Integer>();
     while(docId !=DocIdSetIterator.NO_MORE_DOCS)
     {       
       outputOld.add(docId);
       intersectionSizeOld++;
       docId = iterOld.nextDoc();
     }
     if(!compareTwoLists(_input[testNo], _numDocs[testNo], outputOld))
     {
       System.out.println("wrong output for old");
       return;
     }
   
     // test new version
     // serialize the object into a file 
     File f = new File("serial");
     PForDeltaDocIdSet pfd = (PForDeltaDocIdSet)_docs.get(testNo);
     ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
     oos.writeObject(pfd);
     oos.flush();
     oos.close();
     System.out.println("new serialized size: " + f.length() + " bytes");
     _newSerializationByteSize[testNo] += f.length();
   
     // deserialize it to pfd_DE
     ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
     PForDeltaDocIdSet pfd_DE  = (PForDeltaDocIdSet)ois.readObject();
     ois.close();
   
     // check if the deserialized object == the original object
     int intersectionSize = 0;
     DocIdSetIterator iter = pfd_DE.iterator();
     docId = iter.nextDoc(); 
     ArrayList<Integer> output = new ArrayList<Integer>();
     while(docId !=DocIdSetIterator.NO_MORE_DOCS)
     {      
       output.add(docId);
       intersectionSize++;
       docId = iter.nextDoc();
     }
     if(!compareTwoLists(_input[testNo], _numDocs[testNo], output))
     {
       System.out.println("wrong output for PForDelta");
     }
   }
   System.out.println("-------------------completed------------------------");
 } 

//test serialization speed  
 public void testSerializationByteStrmNew() throws Exception
 {     
   System.out.println("Running Serialization ByteStream Test");
  
   int docId;

   long start, end;
   
   for(int testNo=0; testNo<_listNum; ++testNo)
   {
     System.out.println("round " + testNo + " started");
   PForDeltaDocIdSet pfd = (PForDeltaDocIdSet)_docs.get(testNo);
   ByteArrayOutputStream buf = new ByteArrayOutputStream(_numDocs[0]*4*2);
   ObjectOutputStream oos = new ObjectOutputStream(buf);
   start = System.currentTimeMillis();
   oos.writeObject(pfd);
   end = System.currentTimeMillis();
   oos.flush();
   oos.close();
   System.out.println("serialization time for PForDelta: " + (end-start) + "ms, size: " + (buf.size()) + "bytes");

   
   ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
   start = System.currentTimeMillis();
   PForDeltaDocIdSet pfd_DE  = (PForDeltaDocIdSet)ois.readObject();
   end = System.currentTimeMillis();
   ois.close();
   System.out.println("deserialization time for PForDelta: " + (end-start) + "ms");
   
   int intersectionSize = 0;
   DocIdSetIterator iter = pfd_DE.iterator();
   docId = iter.nextDoc();
   ArrayList<Integer> output = new ArrayList<Integer>();
   while(docId !=DocIdSetIterator.NO_MORE_DOCS)
   {      
     output.add(docId);
     intersectionSize++;
     docId = iter.nextDoc();
   }
//   if(!compareTwoLists(_input1, _numDocs1, output))
//   {
//     System.out.println("wrong output for PForDelta");
//   }
  
   }
//   P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(0);
//  ByteArrayOutputStream bufOld = new ByteArrayOutputStream(_numDocs1*4*2);
//  
//   ObjectOutputStream oosOld = new ObjectOutputStream(bufOld);
//   start = System.currentTimeMillis();
//   oosOld.writeObject(pfdOld);
//   end = System.currentTimeMillis();
//   oosOld.flush();
//   oosOld.close();
//   System.out.println("serialization time for OLD: " + (end-start) + "ms, size: " + (bufOld.size()/4)  + "ints");
//   
//   ObjectInputStream oisOld = new ObjectInputStream(new ByteArrayInputStream(bufOld.toByteArray()));
//   start = System.currentTimeMillis();
//   P4DDocIdSet pfdOld_DE  = (P4DDocIdSet)oisOld.readObject();
//   end = System.currentTimeMillis();
//   oisOld.close();
//   System.out.println("deserialization time for OLD: " + (end-start) + "ms");
//   
//   DocIdSetIterator iterOld = pfdOld_DE.iterator();
//   docId = iterOld.nextDoc();
//   ArrayList<Integer> outputOld = new ArrayList<Integer>();
//   while(docId !=DocIdSetIterator.NO_MORE_DOCS)
//   {      
//     outputOld.add(docId);
//     intersectionSizeOld++;
//     docId = iterOld.nextDoc();
//   }
//   if(!compareTwoLists(_input1, _numDocs1, outputOld))
//   {
//     System.out.println("wrong output for old");
//     return;
//   }
//   
   
   System.out.println("-------------------completed------------------------");
 } 
 
 public void testSerializationByteStrmOld() throws Exception
 {     
   System.out.println("Running Serialization ByteStream Test");
  
   int docId;

   int intersectionSizeOld = 0;
   long start, end;
   
  
   for(int testNo=0; testNo<_listNum; ++testNo)
   {
     System.out.println("round " + testNo + " started");
   P4DDocIdSet pfdOld = (P4DDocIdSet)_docsOld.get(testNo);
  ByteArrayOutputStream bufOld = new ByteArrayOutputStream(_numDocs[0]*4*2);
  
   ObjectOutputStream oosOld = new ObjectOutputStream(bufOld);
   start = System.currentTimeMillis();
   oosOld.writeObject(pfdOld);
   end = System.currentTimeMillis();
   oosOld.flush();
   oosOld.close();
   System.out.println("serialization time for OLD: " + (end-start) + "ms, size: " + (bufOld.size())  + "bytes");
   
   ObjectInputStream oisOld = new ObjectInputStream(new ByteArrayInputStream(bufOld.toByteArray()));
   start = System.currentTimeMillis();
   P4DDocIdSet pfdOld_DE  = (P4DDocIdSet)oisOld.readObject();
   end = System.currentTimeMillis();
   oisOld.close();
   System.out.println("deserialization time for OLD: " + (end-start) + "ms");
   
   DocIdSetIterator iterOld = pfdOld_DE.iterator();
   docId = iterOld.nextDoc();
   ArrayList<Integer> outputOld = new ArrayList<Integer>();
   while(docId !=DocIdSetIterator.NO_MORE_DOCS)
   {      
     outputOld.add(docId);
     intersectionSizeOld++;
     docId = iterOld.nextDoc();
   }
//   if(!compareTwoLists(_input1, _numDocs1, outputOld))
//   {
//     System.out.println("wrong output for old");
//     return;
//   }
   
   }
   System.out.println("-------------------completed------------------------");
 } 
 
 static int _length = 10;
 static int _max = 300000;
  
 @Test
 public void testSkipPerformance() throws IOException, InterruptedException
 {
   System.out.println("");
   System.out.println("Running Doc Skip Multithreaded");
   System.out.println("----------------------------");
   
   double booster  = ((_max*1.0)/(1000f*_length));
   P4DDocIdSet set = new P4DDocIdSet();
   Random random = new Random();

   int max = 1000;
 
   int randomizer = 0;
   double totalDecompressionTime = 0;
   List<Integer> list = new LinkedList<Integer>();
   LinkedList<Integer> list2 = new LinkedList<Integer>();
   int prev = 0;
   for (int i = 0; i < _length*256; i++) {
     prev +=i;
     list.add(prev);
   }
   
   Collections.sort(list);
   //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
  
  
   
     //P4D
     final P4DDocIdSet p4d = new P4DDocIdSet();
     int counter=0;
     
     for (Integer c : list) {
       counter++;
       //System.out.println(c);
       p4d.addDoc(c);
     }
     System.out.println("Set Size:"+ p4d.size());
     Thread arr [] = new Thread[5]; 
     for(int i=0;i<arr.length;i++)
     {
         Thread t = new Thread() {
           public void run()
           {
             StatefulDSIterator dcit = p4d.iterator();
             
             try {
               int docid;
               while((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
               { 
                 //Thread.sleep(0, 25000);
               }
             } catch (IOException e) {
               e.printStackTrace();
             } catch (Exception e) {
               e.printStackTrace();
             }
           }
         };
         arr[i] = t;
         t.start();
     }
     for(int i=0;i<arr.length;i++)
     {
       arr[i].join();
     }
       
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
  
  
  private DocIdSet createDocSetBatch(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    p4d.addDocs(nums, 0, nums.length); 
    return p4d; 
  } 
  
  private DocIdSet createDocSetOldBatch(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    p4d.addDocs(nums, 0, nums.length); 
    return p4d; 
  } 
  
  private DocIdSet createDocSetIntArray(int[] nums) throws Exception{ 
    DocSet p4d = new IntArrayDocIdSet(); 
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
  
  private void loadRandomDataSetsBatch(int[] data, ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, ArrayList<DocIdSet> docsOld, int maxDoc) throws Exception
  {
    obs.add(createObs(data, maxDoc)); 
    
    docsOld.add(createDocSetOld(data));
    docs.add(createDocSetBatch(data)); 
  }
  
  private void loadRandomDataSetsOldIsIntArray(int[] data,  ArrayList<DocIdSet> docsIntArray) throws Exception
  {
    docsIntArray.add(createDocSetIntArray(data));
  }
 
//hy: compare two lists to see if they are equal and print all the different numbers
  private boolean compareTwoLists(int[] input, int size, ArrayList<Integer> output)
  {
    System.out.println("inputSize:" + input.length + "outputSize:" + output.size());
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
  private int[] generateRandomDataHY(int[] ori, int maxDoc, int numDocs)
  {
    //System.out.println("generating random data");
    Random rand = new Random(System.currentTimeMillis()); 
    
    int i,j;
    
    
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
