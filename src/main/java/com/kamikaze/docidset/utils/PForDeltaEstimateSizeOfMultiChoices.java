package com.kamikaze.docidset.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.OpenBitSet;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;

/** 
 * Utility class to compare the docIdset sizes of using IntArrayDocIdSet and PForDeltaDocIdSet, for a variety of integer array lengths.
 * The results are shown at http://sna-projects.com/kamikaze/suggestion.php. 
 * 
 * @author hao yan
 */

public class PForDeltaEstimateSizeOfMultiChoices {

  public static void main(String[] args) throws Exception
  {
     int maxDoc =  75000000;
    
     int batchSize = 256;
     int[] numDocs = {100, 500, 1000, 1200, 1400, 1600, 1800, 2000, 2500, 3000, 5000, 10000, 20000, 50000, 100000};
     int[] originalInput = new int[maxDoc];
     int testNum = numDocs.length;
  
     ArrayList<OpenBitSet> obs; 
    
    long sizeSerializedPForDelta = 0;
    long sizeIntArray = 0;
    for(int i =0; i<maxDoc; ++i)
    {
      originalInput[i] = i;
    }
    
    PrintWriter pw = new PrintWriter(new FileOutputStream("statSize.txt"));
    for(int m=0; m<testNum; ++m)
    {
    int numDoc = numDocs[m]; 
    for(int k=0; k<testNum; ++k)
    {
    
     int[] input = new int[numDoc];
    // get PForDelta size
    input = generateRandomDataHY(originalInput,maxDoc, numDoc);
    PForDeltaDocIdSet pfd  = (PForDeltaDocIdSet)createDocSet(input);
    File fpdf = new File("testSerializedPForDeltaSize");
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fpdf));
    oos.writeObject(pfd);
    oos.flush();
    oos.close();
   
    sizeSerializedPForDelta += fpdf.length(); 
    
    // get int Array Size
    IntArrayDocIdSet iad = new IntArrayDocIdSet(numDoc);
    for(int i=0; i<numDoc; ++i)
    {
      iad.addDoc(input[i]);
    }
    File fiad = new File("testIntArraySize");
    oos = new ObjectOutputStream(new FileOutputStream(fiad));
    oos.writeObject(iad);
    oos.flush();
    oos.close();
    sizeIntArray += fiad.length(); 
    }
    
    //System.out.println("serialized PForDelta object size: " + sizeSerializedPForDelta/testNum);
    //System.out.println("int array object size: " + sizeIntArray/testNum);
    System.out.println(numDoc + "\t" + sizeSerializedPForDelta/testNum + "\t" + sizeIntArray/testNum);
    pw.println(numDoc + "\t" + sizeSerializedPForDelta/testNum + "\t" + sizeIntArray/testNum);
    }
    pw.close();
  }
  
  static DocIdSet createDocSet(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  static int[] generateRandomDataHY(int[] ori, int maxDoc, int numDocs)
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
    //System.out.println("randomly select " + numDocs + " numbers from " + maxDoc +  " numbers");
    //printArray(randomNums, 0, numDocs-1);
    return randomNums;
  }
  
}