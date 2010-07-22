package com.kamikaze.test;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.compression.PForDeltaCompressedSortedIntegerSegment;
import com.kamikaze.docidset.impl.PForDeltaAndDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;
import com.sun.tools.javac.code.Attribute.Array;
import com.kamikaze.docidset.utils.GenerateUnpackClass;

import com.kamikaze.docidset.impl.P4DDocIdSet;



public class PForDeltaKamikazeTest extends TestCase 
{ 
//  public void testWriteUnpackFile() throws IOException
//  {
//    GenerateUnpackClass.generateUnpackClass(128, "/Users/hyan/workspace/UnpackTmp.txt");
//    assertEquals(true, true);
//  }
  
//  public void testMultipleIntersections() throws Exception
//  { 
//     System.out.println("Running Multiple Intersections Test case...");
//     
//     ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
//     ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
//    
//     int maxDoc = 350000;
//     int listNum = 3;
//     getRandomDataSets(obs, docs, maxDoc, listNum);
//     //getSpecificDataSets(obs, docs, maxDoc, listNum);
//     
//     // hy: get the expected result using OpenBitSet
//     OpenBitSet base = obs.get(0); 
//     for(int i = 1; i < obs.size(); ++i) 
//     { 
//       base.intersect(obs.get(i)); 
//     }     
//     //System.out.println("base's intersection:");
//     ArrayList<Integer> expectedIntersectionResult = bitSetToArrayList(base);
//     
//     // hy: get our results
//     PForDeltaAndDocIdSet ands = new PForDeltaAndDocIdSet(docs); 
//     long card1 = base.cardinality(); 
//     long card2 = ands.size(); // hy: this calls the nextDoc() of AndDocIdIterator
//     
//     ArrayList<Integer> intersectionResult = ands.getIntersection();
//     
//     // System.out.println("Intersectoin result");
//     // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
//     // printList(intersectionResult, 0, intersectionResult.size()-1);
//     
//     assertEquals(card1, card2); 
//     assertEquals(true, intersectionResult.equals(expectedIntersectionResult));
//     
//     System.out.println("----------------completed---------------------------");
//  } 
//  
//
//  // hy: test the PForDeltaDocIdSet.find() 
//  public void testFind() throws Exception
//  {     
//    System.out.println("Running Multiple Intersections Test case...");
//    
//    ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
//    ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
//    
//    int maxDoc = 350000;
//    int listNum = 3;
//    getRandomDataSets(obs, docs, maxDoc, listNum);
//    //getSpecificDataSets(obs, docs, maxDoc, listNum);
//     
//     OpenBitSet base = obs.get(0); 
//     for(int i = 1; i < obs.size(); ++i) 
//     { 
//       base.intersect(obs.get(i)); 
//     } 
//     
//     //System.out.println("base's intersection:");
//     //ArrayList<Integer> expectedIntersectionResult = printBitSet(base);
//     ArrayList<Integer> expectedIntersectionResult = bitSetToArrayList(base);
//     
//     ArrayList<Integer> intersectionResult = new ArrayList<Integer>();
//     PForDeltaDocIdSet pfd0 = (PForDeltaDocIdSet)docs.get(0);
//     PForDeltaDocIdSet pfd1 = (PForDeltaDocIdSet)docs.get(1);
//     PForDeltaDocIdSet pfd2 = (PForDeltaDocIdSet)docs.get(2);
//     DocIdSetIterator iter = pfd0.iterator();
//     
//     int docId=iter.nextDoc();
//     while(docId !=DocIdSetIterator.NO_MORE_DOCS)
//     {
//        if(pfd1.find(docId) && pfd2.find(docId))
//        {
//          intersectionResult.add(docId);
//        }
//        docId = iter.nextDoc();
//    }
//    
//     //System.out.println("Intersectoin result");
//     //printList(intersectionResult, 0, intersectionResult.size()-1);
//     // printList(expectedIntersectionResult, 0, expectedIntersectionResult.size()-1);
//     
//     assertEquals(true, intersectionResult.equals(expectedIntersectionResult));
//     
//     System.out.println("-------------------completed------------------------");
//  } 
//
  public void testDecompSpeed() throws Exception
  {     
    System.out.println("Running Comp Decomp Test case...");
    
    ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
    ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
    ArrayList<DocIdSet> docsOld = new ArrayList<DocIdSet>(); 
    
    int maxDoc = 3500000;
    int numDocs = 2000000;
    String filename = "/Users/hyan/workspace/KamekazeTestDataDecompSpeed.txt";
    saveRandomDataSetsToFileHY(maxDoc, numDocs, filename);
    //int[] input = new int[numDocs];
    
    //loadReandomDataSetsFromFileHY(filename, input,  numDocs);
    
   
    //saveRandomDataSetsToFile(maxDoc,  filename);
    
    //loadReandomDataSetsFromFile(filename, obs, docs, docsOld, maxDoc);
    loadReandomDataSetsFromFile(filename, obs, docs, docsOld, numDocs);
    
    ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
    
    
    ArrayList<Integer> outputOld = new ArrayList<Integer>();
    
    int docId;
    
    long startOld = System.currentTimeMillis();
    P4DDocIdSet pfdOld = (P4DDocIdSet)docsOld.get(0);
    DocIdSetIterator iterOld = pfdOld.iterator();
    docId = iterOld.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      outputOld.add(docId);
      docId = iterOld.nextDoc();
      //docId = iterOld.advance(docId+1);
    }
    long endOld = System.currentTimeMillis();
    System.out.println("old took: "+(endOld-startOld));
    System.out.println("old compressed size: " + pfdOld.getCompressedBitSize()/32 + " ints");
    
    ArrayList<Integer> output = new ArrayList<Integer>();
    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)docs.get(0);
    System.out.println("compressed size: " + pfdDS.getCompressedBitSize()/32 + " ints");
    DocIdSetIterator iter = pfdDS.iterator();
    long start = System.currentTimeMillis();
    docId = iter.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      output.add(docId);
      docId = iter.nextDoc();
      //docId = iter.advance(docId+1);
    }
    long end = System.currentTimeMillis();
    System.out.println("took: "+(end-start));
   
    //printList(input, 0, input.size()-1);
    //printList(output, 0, output.size()-1);
    assertEquals(true, compareTwoLists(input, output));
    System.out.println("-------------------completed------------------------");

   
  } 
  
//   hy: test compression and decompression, in particular, nextDoc() of PForDeltaDocIdIterator
//  public void testCompDecomp() throws Exception
//  {     
//    System.out.println("Running Comp Decomp Test case...");
//    
//    ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
//    ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
//    
//    int maxDoc = 150;
//    int listNum = 1;
//   
//    getRandomDataSets(obs, docs, maxDoc, listNum, false);
//    //getSpecificDataSets(obs, docs, maxDoc, listNum);
//    
//    ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
//    
//    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)docs.get(0);
//    DocIdSetIterator iter = pfdDS.iterator();
//    
//   
//    ArrayList<Integer> output = new ArrayList<Integer>();
//    int docId = iter.nextDoc();
//   
//    long start = System.currentTimeMillis();
//    
//    
//    
//    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
//    {      
//      output.add(docId);
//      docId = iter.nextDoc();
//    }
//    long end = System.currentTimeMillis();
//    System.out.println("took: "+(end-start));
//   
//    //printList(input, 0, input.size()-1);
//    //printList(output, 0, output.size()-1);
//    assertEquals(true, compareTwoLists(input, output));
//    System.out.println("-------------------completed------------------------");
//
//  } 

//  public void testForOutOfBounds() throws Exception
//  {     
//    System.out.println("Running OutOfBounds Test case...");
//    
//      ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
//      ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
//      
//      int maxDoc = 350000;
//      int listNum = 1;
//      getRandomDataSets(obs, docs, maxDoc, listNum);
//      //getSpecificDataSets(obs, docs, maxDoc, listNum);
//      
//      ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
//      
//      DocSet docSet = new PForDeltaDocIdSet(); 
//      
//      boolean saw403 = false;
//      for (Integer integer : input) 
//      { 
//          if(integer == 403)
//          {
//            saw403 = true;
//            System.out.println("find the guy");
//          }
//          docSet.addDoc(integer); 
//      } 
//      boolean got = docSet.find(403); 
//      assertEquals(saw403, got);
//      System.out.println("---------------completed----------------------------");      
//  }    
//  
//public void testPartialEmptyAnd() throws IOException 
//{ 
//    try 
//    {
//        System.out.println("Running Partial Empty And    Test case...");       
//          
//        DocSet ds1 = new PForDeltaDocIdSet(); 
//        DocSet ds2 = new PForDeltaDocIdSet(); 
//        ds2.addDoc(42); 
//        ds2.addDoc(43); 
//        ds2.addDoc(44); 
//        ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
//        docs.add(ds1);  // hy: ds1 is empty
//        docs.add(ds2);         
//        
//        DocSet ds3 = new PForDeltaDocIdSet(); 
//        DocSet ds4 = new PForDeltaDocIdSet(); 
//        ds4.addDoc(42); 
//        ds4.addDoc(43); 
//        ds4.addDoc(44);         
//        ArrayList<DocIdSet> docs2 = new ArrayList<DocIdSet>(); 
//        docs2.add(ds3); // hy: ds3 is empty
//        docs2.add(ds4); 
//        
//        OrDocIdSet orlist1 = new OrDocIdSet(docs); 
//        OrDocIdSet orlist2 = new OrDocIdSet(docs2); 
//        ArrayList<DocIdSet> docs3 = new ArrayList<DocIdSet>(); 
//        docs3.add(orlist1); 
//        docs3.add(orlist2); 
//        
//        PForDeltaDocIdSet andlist = new PForDeltaDocIdSet(docs3); 
//        
//        DocIdSetIterator iter = andlist.iterator(); 
//        @SuppressWarnings("unused") 
//        int docId = -1; 
//        while((docId = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) 
//        { 
//        }   
//      } 
//      catch(Exception e) 
//      { 
//              e.printStackTrace();
//      } 
//      assertTrue(true); 
//      System.out.println("-----------------completed--------------------------");
//} 
  
  
  
  private OpenBitSet createObs(ArrayList<Integer> nums, int maxDoc) { 
    OpenBitSet bitSet = new OpenBitSet(maxDoc); 
    for(int num:nums) 
      bitSet.set(num); 
    return bitSet; 
  } 
  
  private DocIdSet createDocSet(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSetOld(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private void saveRandomDataSetsToFile(int maxDoc, String filename) throws Exception
  {
    ArrayList<OpenBitSet> obs = new  ArrayList<OpenBitSet>();
    ArrayList<DocIdSet>docs = new ArrayList<DocIdSet>();
    
    getRandomDataSets(obs, docs, maxDoc, 1, false);
    ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
    
    DataOutputStream outStrm = new DataOutputStream(new FileOutputStream(filename));
    for(int i=0; i<maxDoc; ++i)
    {
      outStrm.writeInt(input.get(i));
    }
    outStrm.close();
    
  }
  
  private void loadReandomDataSetsFromFile(String filename, ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, ArrayList<DocIdSet> docsOld, int maxDoc) throws Exception
  {
    DataInputStream inStrm = new DataInputStream(new FileInputStream(filename));
    ArrayList<Integer> data = new ArrayList<Integer>();
    for(int i=0; i<maxDoc; ++i)
    {
      data.add(inStrm.readInt());
    }
    inStrm.close();
    
    obs.add(createObs(data, maxDoc)); 
    docs.add(createDocSet(data)); 
    
    docsOld.add(createDocSetOld(data));
    
  }
  
  // hy: create listNum DocIdSets, each one has random number of elements
  private void getRandomDataSets(ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, int maxDoc, int listNum, boolean randomNumDocs)  throws Exception
  { 
    Random rand = new Random(System.currentTimeMillis()); 
    int numdocs;
    for(int i=0; i < listNum; ++i) 
    { 
     
      if(randomNumDocs)
         numdocs = rand.nextInt(maxDoc);
      else
         numdocs = maxDoc;
      
      ArrayList<Integer> nums = new ArrayList<Integer>(); 
      HashSet<Integer> seen = new HashSet<Integer>(); 
      for (int j = 0; j < numdocs; j++) 
      { 
        int nextDoc = rand.nextInt(maxDoc); 
        if(seen.contains(nextDoc)) 
        { 
          while(seen.contains(nextDoc)) 
          { 
            nextDoc = rand.nextInt(maxDoc); 
          } 
        } 
        nums.add(nextDoc); 
        seen.add(nextDoc); 
       } 
       Collections.sort(nums); 
       
       //nums = getSpecialArrayList(i);
       
       //System.out.println("list " + i);
       //printList(nums, 0, nums.size()-1);
       obs.add(createObs(nums, maxDoc)); 
       docs.add(createDocSet(nums)); 
    }
  }
  
  // hy: create listNum DocIdSets, each of which is composed of a specific array of elements. Currently only support 3 such sets. You can change getSpecialArrayList() to get more
  private void getSpecificDataSets(ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, int maxDoc, int listNum)  throws Exception
  { 
    Random rand = new Random(System.currentTimeMillis()); 
    
    for(int i=0; i < listNum; ++i) 
    { 
      ArrayList<Integer> nums = new ArrayList<Integer>(); 
      nums = getSpecialArrayList(i);
       
       //System.out.println("list " + i);
       //printList(nums, 0, nums.size()-1);
       obs.add(createObs(nums, maxDoc)); 
       docs.add(createDocSet(nums)); 
    }
  }
  // hy: get 3 particular arrays as input (for debugging)
      private ArrayList<Integer> getSpecialArrayList(int arrayIndex)
      {
        int[]  in=null;
        if(arrayIndex == 0)
        {
          //in = new int[]{1, 2, 6, 7, 11, 12, 13, 14, 16, 18, 19, 22, 23, 26, 27, 28, 31, 32, 33, 36, 37, 39, 41, 42, 43, 45, 46, 47, 48, 50, 52, 56, 58, 59, 60, 62, 63, 66, 67, 68, 69, 71, 72, 74, 75, 76, 79, 80, 83, 89, 90, 91, 94, 95, 97, 98, 99, 107, 109, 111, 115, 116, 117, 119, 124, 127, 128, 130, 132, 133, 135, 136, 137, 140, 144, 145, 146, 149, 151, 153, 154, 156, 157, 158, 159, 161, 163, 164, 165, 166, 167, 168, 170, 171, 172, 174, 175, 179, 187, 188, 189, 190, 192, 193, 196, 197, 198, 199, 202, 203, 205, 208, 209, 214, 215, 216, 219, 221, 222, 224, 225, 226, 228, 229, 231, 232, 233, 234, 235, 236, 237, 238, 239, 241, 243, 245, 249, 250, 251, 252, 253, 255, 257, 260, 262, 263, 266, 268, 271, 272, 275, 277, 278, 280, 281, 284, 285, 288, 291, 293, 294, 296, 297, 298, 301, 308, 311, 314, 316, 320, 321, 322, 325, 327, 332, 333, 336, 337, 338, 343, 344, 345, 346, 348};
          //in = new int[] {0, 10, 16, 17, 22, 25, 29, 32, 34, 35, 38, 39, 43, 47, 48, 49, 53, 55, 57, 61, 63, 67, 68, 73, 75, 76, 77, 79, 80, 84, 88, 91, 94, 97, 98, 99, 100, 101, 106, 108, 109, 113, 120, 125, 128, 132, 136, 139, 142, 144, 145, 147, 149, 153, 154, 158, 162, 167, 169, 172, 174, 178, 181, 182, 199, 200, 204, 212, 217, 218, 222, 223, 224, 226, 228, 229, 231, 235, 236, 239, 240, 241, 243, 248, 255, 263, 264, 265, 268, 269, 273, 275, 277, 281, 283, 284, 287, 289, 291, 303, 307, 315, 323, 325, 336, 339, 342, 347, 348, 349};
          //in = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 268, 269, 270, 271, 272, 273, 274, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349};
          in = new int[] {0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 19, 21, 23, 25, 26, 27, 28, 29, 30, 31, 33, 34, 35, 37, 38, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 59, 60, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 75, 76, 78, 79, 80, 82, 83, 86, 87, 88, 90, 91, 92, 94, 95, 97, 98, 99, 100, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 120, 121, 122, 123, 125, 126, 127, 128, 129, 130, 131, 132, 135, 136, 137, 138, 139, 140, 141, 143, 144, 145, 146, 148, 149, 150, 151, 153, 154, 155, 156, 157, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 170, 171, 172, 174, 175, 176, 177, 178, 179, 181, 182, 183, 184, 185, 186, 187, 188, 190, 192, 193, 195, 197, 199, 200, 201, 202, 203, 204, 207, 208, 209, 210, 211, 212, 213, 214, 216, 217, 218, 219, 220, 221, 222, 223, 225, 228, 231, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 260, 261, 262, 263, 265, 266, 267, 268, 270, 271, 272, 273, 275, 276, 277, 278, 279, 282, 283, 285, 286, 287, 288, 289, 291, 292, 293, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 308, 309, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 326, 327, 328, 331, 332, 333, 334, 335, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 349};
        }
        else if(arrayIndex == 1)
        {
          //in = new int[]{1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 18, 19, 20, 22, 23, 24, 26, 27, 29, 30, 34, 36, 38, 39, 42, 44, 45, 46, 47, 49, 52, 54, 55, 59, 63, 65, 69, 70, 73, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 91, 92, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 107, 108, 109, 110, 111, 112, 114, 115, 117, 118, 120, 121, 123, 125, 126, 127, 129, 131, 133, 134, 135, 136, 137, 138, 140, 141, 142, 144, 146, 149, 150, 152, 153, 154, 156, 157, 158, 159, 161, 162, 163, 165, 166, 168, 169, 171, 172, 174, 175, 179, 182, 185, 187, 189, 192, 193, 195, 196, 197, 199, 200, 202, 204, 205, 206, 209, 210, 211, 212, 213, 215, 220, 221, 222, 223, 224, 226, 227, 228, 230, 233, 234, 236, 237, 239, 240, 243, 244, 247, 248, 249, 250, 251, 254, 255, 258, 259, 262, 263, 267, 269, 270, 273, 274, 276, 277, 279, 280, 281, 282, 286, 287, 289, 291, 293, 296, 298, 301, 303, 306, 307, 308, 310, 314, 315, 317, 318, 320, 321, 322, 325, 326, 327, 328, 329, 330, 332, 334, 335, 336, 337, 338, 342, 343, 344, 348};
          //in = new int[] {8, 11, 46, 51, 68, 77, 81, 86, 92, 100, 105, 116, 119, 140, 145, 149, 152, 162, 186, 195, 199, 202, 228, 232, 235, 251, 252, 260, 261, 270, 275, 286, 301, 305, 307, 311, 324, 330, 340, 344};
         // in = new int[] {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 167, 168, 169, 170, 171, 173, 174, 175, 177, 178, 180, 181, 182, 183, 185, 187, 189, 190, 191, 192, 193, 194, 195, 196, 198, 199, 200, 201, 202, 203, 204, 205, 206, 208, 209, 210, 211, 212, 213, 214, 215, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 246, 247, 248, 249, 250, 251, 252, 253, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 298, 299, 302, 303, 304, 305, 306, 307, 309, 310, 311, 312, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349};
           in = new int[]{0, 11, 12, 17, 20, 24, 27, 48, 53, 54, 56, 67, 71, 73, 79, 82, 83, 92, 104, 108, 113, 118, 121, 124, 127, 130, 133, 134, 135, 155, 159, 161, 163, 168, 169, 170, 176, 178, 188, 195, 196, 206, 210, 213, 218, 222, 225, 232, 238, 242, 243, 247, 250, 251, 254, 257, 261, 265, 266, 267, 272, 279, 280, 282, 283, 285, 288, 289, 298, 306, 314, 319, 323, 325, 326, 332, 342, 348};
        }
        else if(arrayIndex == 2)
        {
          //in = new int[] {0, 1, 4, 5, 7, 8, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 26, 27, 29, 30, 31, 32, 33, 34, 35, 37, 39, 40, 41, 42, 43, 44, 46, 48, 49, 50, 53, 58, 59, 60, 63, 66, 67, 68, 69, 70, 71, 72, 73, 77, 78, 79, 82, 83, 84, 85, 86, 87, 88, 89, 90, 92, 93, 94, 95, 100, 101, 102, 103, 105, 106, 107, 108, 109, 110, 111, 112, 113, 115, 116, 117, 119, 120, 121, 122, 123, 124, 126, 130, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 149, 150, 151, 152, 153, 154, 156, 157, 158, 159, 160, 161, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 175, 177, 178, 179, 181, 182, 183, 184, 186, 190, 193, 195, 198, 199, 200, 201, 202, 204, 205, 207, 209, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 224, 225, 227, 228, 229, 230, 232, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 247, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 262, 263, 265, 266, 267, 268, 269, 272, 273, 274, 276, 277, 278, 281, 282, 283, 286, 287, 288, 289, 292, 295, 299, 301, 302, 304, 305, 306, 307, 309, 311, 312, 313, 314, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 333, 337, 338, 339, 343, 344, 346, 347, 348, 349};
          //in = new int[] {0, 1, 3, 6, 7, 9, 10, 11, 12, 13, 15, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 31, 32, 34, 35, 36, 37, 38, 39, 40, 42, 43, 46, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 60, 61, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 92, 93, 96, 99, 101, 102, 104, 105, 106, 108, 109, 111, 112, 113, 116, 117, 118, 120, 122, 123, 124, 125, 126, 128, 129, 130, 131, 132, 135, 138, 139, 140, 141, 142, 143, 144, 145, 146, 149, 150, 154, 156, 157, 158, 161, 165, 166, 167, 168, 169, 170, 171, 172, 173, 175, 178, 179, 180, 181, 182, 183, 184, 185, 186, 188, 189, 190, 191, 193, 194, 195, 196, 197, 199, 200, 201, 202, 203, 205, 206, 208, 210, 211, 212, 215, 217, 218, 220, 221, 222, 223, 224, 225, 226, 227, 228, 232, 233, 234, 236, 237, 238, 239, 240, 242, 243, 244, 245, 246, 247, 248, 250, 251, 252, 254, 255, 256, 257, 262, 263, 264, 265, 266, 268, 269, 270, 271, 272, 276, 277, 278, 279, 280, 281, 283, 285, 289, 290, 291, 294, 295, 296, 297, 298, 299, 300, 302, 304, 305, 306, 307, 308, 309, 313, 314, 315, 316, 317, 318, 319, 321, 324, 326, 327, 329, 330, 331, 332, 333, 334, 336, 341, 342, 344, 346, 347};  
          //in = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 26, 27, 28, 29, 32, 33, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 47, 48, 49, 50, 51, 52, 53, 54, 56, 58, 61, 62, 63, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 86, 87, 88, 89, 90, 91, 92, 93, 94, 96, 97, 98, 99, 100, 101, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 117, 118, 119, 122, 124, 125, 126, 127, 128, 129, 131, 132, 133, 134, 135, 136, 138, 139, 140, 141, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 157, 159, 161, 162, 163, 166, 167, 168, 169, 170, 174, 175, 176, 177, 178, 179, 180, 181, 183, 184, 185, 187, 189, 190, 192, 193, 194, 195, 196, 197, 199, 200, 202, 203, 204, 206, 207, 209, 210, 211, 212, 213, 214, 216, 217, 220, 222, 223, 224, 225, 226, 228, 229, 230, 231, 232, 233, 234, 235, 237, 240, 241, 242, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 270, 271, 272, 273, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 294, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 316, 317, 318, 319, 320, 321, 322, 324, 326, 327, 328, 329, 331, 332, 333, 334, 336, 337, 338, 339, 341, 345, 346, 347, 348, 34};
          in = new int[] {1, 5, 13, 14, 17, 18, 23, 24, 25, 27, 28, 33, 34, 35, 38, 47, 50, 54, 56, 67, 75, 76, 77, 80, 85, 87, 88, 89, 91, 95, 97, 101, 102, 103, 106, 113, 115, 122, 126, 136, 139, 141, 145, 150, 153, 155, 156, 158, 163, 174, 177, 182, 187, 188, 191, 198, 203, 209, 213, 214, 217, 226, 227, 229, 231, 234, 236, 243, 252, 253, 254, 260, 271, 274, 277, 281, 283, 286, 287, 292, 295, 298, 299, 307, 310, 313, 315, 316, 327, 328, 329, 330, 332, 335, 336, 338, 340, 343, 345};
        }
        else
        {
          in = null;
          return null;
        }
        System.out.println("input's size:" + in.length);
        ArrayList<Integer> input = new ArrayList<Integer>();
        for(int i=0; i<in.length;++i)
        {
         input.add(in[i]);
        }
        return input;
      }
      
      // hy: get 1 particular array as input
    private ArrayList<Integer> getSpecialArrayList()
    {
      //int[] in = {0, 2, 6, 7, 8, 9, 10, 11, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 25, 26, 28, 30, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 45, 46, 48, 49, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 63, 65, 67, 72, 73, 74, 75, 76, 77, 78, 81, 82, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 100, 101, 102, 105, 107, 108, 109, 110, 112, 113, 115, 117, 118, 119, 121, 122, 123, 124, 125, 127, 128, 129, 130, 131, 132, 133, 134, 140, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 171, 172, 174, 175, 178, 179, 180, 182, 183, 184, 186, 187, 188, 189, 191, 192, 193, 194, 195, 196, 198, 199, 200, 201, 203, 204, 205, 206, 207, 208, 210, 211, 212, 213, 214, 215, 217, 218, 221, 222, 223, 224, 225, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 243, 244, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 260, 261, 266, 267, 268, 269, 274, 275, 276, 277, 278, 279, 280, 281, 283, 284, 285, 287, 289, 295, 298, 299, 301, 302, 303, 306, 307, 308, 309, 311, 312, 313, 314, 315, 318, 319, 320, 321, 324, 325, 326, 327, 328, 329, 331, 332, 333, 335, 336, 337, 338, 339, 340, 342, 343, 344, 346, 347, 348, 349, 350, 354, 356, 358, 362, 363, 364, 365, 366, 367, 369, 370, 372, 373, 374, 375, 376, 378, 379, 381, 382, 385, 386, 387, 389, 390, 391, 392, 394, 395, 397, 398, 399, 400, 402, 403, 404, 405, 407, 408, 409, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 425, 428, 429, 430, 431, 433, 434, 435, 436, 438, 439, 440, 442, 443, 445, 447, 448, 449, 450, 451, 452, 453, 454, 456, 457, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 487, 488, 489, 491, 492, 494, 495, 497, 498, 499, 500, 501, 502, 503, 506, 507, 512, 513, 515, 516, 517, 519, 520, 521, 522, 523, 524, 525, 526, 527, 529, 530, 531, 533, 534, 535, 537, 539, 541, 542, 543, 544, 545, 546, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 559, 560, 561, 562, 563, 564, 565, 566, 569, 571, 572, 573, 576, 578, 580, 581, 584, 585, 586, 587, 588, 590, 591, 592, 593, 595, 596, 597, 598, 599, 600, 602, 603, 604, 605, 606, 607, 609, 610, 612, 613, 614, 615, 616, 617, 620, 621, 622, 623, 624, 627, 628, 629, 630, 632, 633, 634, 635, 637, 638, 640, 643, 645, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 662, 663, 664, 665, 666, 667, 669, 672, 673, 674, 677, 679, 680, 681, 682, 684, 688, 689, 690, 691, 692, 697, 700, 702, 703, 704, 705, 706, 707, 708, 710, 712, 713, 714, 715, 717, 719, 721, 724, 725, 726, 730, 731, 733, 734, 736, 737, 738, 739, 740, 744, 747, 748, 749, 750, 751, 752, 753, 754, 755, 757, 758, 759, 760, 761, 762, 765, 767, 770, 771, 775, 776, 778, 779, 780, 781, 782, 785, 786, 787, 788, 789, 791, 792, 793, 794, 795, 797, 798, 799};
      //int[] in = {0, 2, 4, 6, 7, 8, 9, 10, 11, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 25, 26, 28, 30, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 45, 46, 48, 49, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 63, 65, 67, 72, 73, 74, 75, 76, 77, 78, 81, 82, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 100, 101, 102, 105, 107, 108, 109, 110, 112, 113, 115, 117, 118, 119, 121, 122, 123, 124, 125, 127, 128, 129, 130, 131, 132, 133, 134, 140, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 171, 172, 174, 175, 178, 179, 180, 182, 183, 184, 186, 187, 188, 189, 191, 192, 193, 194, 195, 196, 198, 199, 200, 201, 203, 204, 205, 206, 207, 208, 210, 211, 212, 213, 214, 215, 217, 218, 221, 222, 223, 224, 225, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 243, 244, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 260, 261, 266, 267, 268, 269, 274, 275, 276, 277, 278, 279, 280, 281, 283, 284, 285, 287, 289, 295, 298, 299, 301, 302, 303, 306, 307, 308, 309, 311, 312, 313, 314, 315, 318, 319, 320, 321, 324, 325, 326, 327, 328, 329, 331, 332, 333, 335, 336, 337, 338, 339, 340, 342, 343, 344, 346, 347, 348, 349, 350, 354, 356, 358, 362, 363, 364, 365, 366, 367, 369, 370, 372, 373, 374, 375, 376, 378, 379, 381, 382, 385, 386, 387, 389, 390, 391, 392, 394, 395, 397, 398, 399, 400, 402, 403, 404, 405, 407, 408, 409, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 425, 428, 429, 430, 431, 433, 434, 435, 436, 438, 439, 440, 442, 443, 445, 447, 448, 449, 450, 451, 452, 453, 454, 456, 457, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 487, 488, 489, 491, 492, 494, 495, 497, 498, 499, 500, 501, 502, 503, 506, 507, 512, 513, 515, 516, 517, 519, 520, 521, 522, 523, 524, 525, 526, 527, 529, 530, 531, 533, 534, 535, 537, 539, 541, 542, 543, 544, 545, 546, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 559, 560, 561, 562, 563, 564, 565, 566, 569, 571, 572, 573, 576, 578, 580, 581, 584, 585, 586, 587, 588, 590, 591, 592, 593, 595, 596, 597, 598, 599, 600, 602, 603, 604, 605, 606, 607, 609, 610, 612, 613, 614, 615, 616, 617, 620, 621, 622, 623, 624, 627, 628, 629, 630, 632, 633, 634, 635, 637, 638, 640, 643, 645, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 662, 663, 664, 665, 666, 667, 669, 672, 673, 674, 677, 679, 680, 681, 682, 684, 688, 689, 690, 691, 692, 697};
     
     // int[] in = {0, 2, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 25, 26, 28, 30, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 45, 46, 48, 49, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 63, 65, 67, 72, 73, 74, 75, 76, 77, 78, 81, 82, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 100, 101, 102, 105, 107, 108, 109, 110, 112, 113, 115, 117, 118, 119, 121, 122, 123, 124, 125, 127, 128, 129, 130, 131, 132, 133, 134, 140, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 171, 172};
      //int[] in = {0, 2, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21};
      int[]  in = {1, 2, 6, 7, 11, 12, 13, 14, 16, 18, 19, 22, 23, 26, 27, 28, 31, 32, 33, 36, 37, 39, 41, 42, 43, 45, 46, 47, 48, 50, 52, 56, 58, 59, 60, 62, 63, 66, 67, 68, 69, 71, 72, 74, 75, 76, 79, 80, 83, 89, 90, 91, 94, 95, 97, 98, 99, 107, 109, 111, 115, 116, 117, 119, 124, 127, 128, 130, 132, 133, 135, 136, 137, 140, 144, 145, 146, 149, 151, 153, 154, 156, 157, 158, 159, 161, 163, 164, 165, 166, 167, 168, 170, 171, 172, 174, 175, 179, 187, 188, 189, 190, 192, 193, 196, 197, 198, 199, 202, 203, 205, 208, 209, 214, 215, 216, 219, 221, 222, 224, 225, 226, 228, 229, 231, 232, 233, 234, 235, 236, 237, 238, 239, 241, 243, 245, 249, 250, 251, 252, 253, 255, 257, 260, 262, 263, 266, 268, 271, 272, 275, 277, 278, 280, 281, 284, 285, 288, 291, 293, 294, 296, 297, 298, 301, 308, 311, 314, 316, 320, 321, 322, 325, 327, 332, 333, 336, 337, 338, 343, 344, 345, 346, 348};
      //System.out.println("input's size:" + in.length);
      ArrayList<Integer> input = new ArrayList<Integer>();
      for(int i=0; i<in.length;++i)
      {
        input.add(in[i]);
      }
      return input;
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
    
    // hy: print a openBitSet object
    private ArrayList<Integer> printBitSet(OpenBitSet bs)
    {
      ArrayList<Integer> listRes = new ArrayList<Integer>();
      System.out.print("bitSet(" + bs.capacity() +") [");
      for(int i=0; i<bs.capacity(); i++)
      {
        if(bs.get(i))
        {
           listRes.add(i);
           System.out.print(i);
           System.out.print(" ");
        }
       }
       System.out.println("]");
       return listRes;
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
    
    void printListWithIndex(ArrayList<Integer> list, int start, int end)
    {
      System.out.print("(" + (end-start+1) + ")[");
      for(int i=start; i<=end; ++i)
      {
        System.out.print(list.get(i));
        System.out.print(":"+i+", ");
      }
      System.out.println("]");
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
        printList(output, i, output.size());
        ret = false;
      }
      if(i<output.size())
      {
        printList(input, i, input.size());
        ret = false;
      }
      return ret;
    }
        
    // hy: compare two arrays, print out the difference        
    private void compareTwoArrays(int[] input,  int[] output)
    {
      System.out.println("inputSize:" + input.length + "outputSize:" + output.length);
      int i=0;
      for(i=0; i<input.length && i<output.length; ++i)
      {
        if(input[i] != output[i])
        {
          System.out.println("in[" + i + "]" + input[i] + " != out[" + i + "]" + output[i]);
        }
      }
      if(i<input.length)
      {
        printList(output, i, output.length);
      }
      if(i<output.length)
      {
        printList(input, i, input.length);
      } 
    }
    
    // hy: generate numDocs numbers out of maxDoc numbers
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
    
    private void saveRandomDataSetsToFileHY(int maxDoc, int numDoc, String filename) throws IOException
    {
      int[] input = generateRandomDataHY(maxDoc, numDoc);
      
      DataOutputStream outStrm = new DataOutputStream(new FileOutputStream(filename));
      for(int i=0; i<numDoc; ++i)
      {
        outStrm.writeInt(input[i]);
      }
      outStrm.close();
      
    }
    
    private void loadReandomDataSetsFromFileHY(String filename, int[] input, int maxDoc) throws IOException
    {
      DataInputStream inStrm = new DataInputStream(new FileInputStream(filename));
      
      for(int i=0; i<maxDoc; ++i)
      {
        input[i] = inStrm.readInt();
      }
      inStrm.close();   
    }
}

