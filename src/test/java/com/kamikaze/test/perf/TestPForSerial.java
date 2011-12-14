package com.kamikaze.test.perf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import org.apache.lucene.search.DocIdSet;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.Conversion;

/**
 * This class provides tests for comparing the serialization performance of standard java serialization and our own serialization
 * defined in PForDeltaDocIdSet
 * 
 * @author hao yan
 *
 */
public class TestPForSerial  {
  public static void main(String args[]) throws Exception {
    int batch = 128;
    int maxDoc = 75000000;
    int[] originalInput = new int[maxDoc];
    for(int i =0; i<maxDoc; ++i)
    {
      originalInput[i] = i;
    }
    
    int[] numDocs = {150000,375000,750000};
    
    for(int k=0; k<numDocs.length; k++)
    {
      String serial = "mySerialFile";
      int[] input = generateRandomDataHY(originalInput,maxDoc, numDocs[k]);
      PForDeltaDocIdSet docSetOrigin = new PForDeltaDocIdSet(batch);
      
      for (int i = 0; i < input.length; i++) {
        docSetOrigin.addDoc(input[i]);
      }

      try {
        File f = new File(serial);
        OutputStream os = new FileOutputStream(f);
        byte[] serializedBytes = PForDeltaDocIdSet.serialize(docSetOrigin);
        
        os.write(serializedBytes);
        
        os.flush();
        os.close();

      } catch (Exception e) {
        e.printStackTrace();
      }
      
      DocIdSet docSetDeserializd = null;
      long start = System.currentTimeMillis();
      try {
        File in = new File(serial);
        InputStream is = new FileInputStream(in);
        byte[] bytesSize = new byte[Conversion.BYTES_PER_INT]; 
        is.read(bytesSize);
        int totalNumInt = Conversion.byteArrayToInt(bytesSize, 0);
        byte[] bytesData = new byte[totalNumInt * Conversion.BYTES_PER_INT];
        is.read(bytesData,0,bytesData.length);
        docSetDeserializd = PForDeltaDocIdSet.deserialize(bytesData, 0);
      } catch (Exception e) {
        e.printStackTrace();
      }
      long end = System.currentTimeMillis();
      
      StatefulDSIterator dcitOrigin = docSetOrigin.iterator();
      org.apache.lucene.search.DocIdSetIterator dcitDeserialized = docSetDeserializd.iterator();

      try {
        for (int i = 0; i < input.length; i++) {
          int docid1 = dcitOrigin.nextDoc();
          int docid2 = dcitDeserialized.nextDoc();
          if(docid1 != input[i] || docid1 != docid2)
          {
            System.out.println("docid1: " + docid1 + ", docid2: " + docid2 + ", input: " + input[i]);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      File in = new File(serial);
      if(in.exists())
      {
        System.out.println("our Serial: numDocs:  " + numDocs[k] + ", size: " + in.length() + ", time: " + (end-start) + " ms");
        in.delete();
      }
      
      // java serial
      try {
        String serial2 = "javaSerialFile";
        File f = new File(serial2);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(docSetOrigin);
        oos.flush();
        oos.close();
        
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serial2));
        start = System.currentTimeMillis();
        PForDeltaDocIdSet pfd_DE  = (PForDeltaDocIdSet)ois.readObject();
        end = System.currentTimeMillis();
        ois.close();
        
        
        File in2 = new File(serial2);
        if(in2.exists())
        {
          System.out.println("java Serial: numDocs:  " + numDocs[k] + ", size: " + in2.length() + " time: " + (end-start) + " ms");
          in2.delete();
        }
        
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    
      input=null;
      
    }
   
    
    
    
   

    
  }
  
  private static int[] generateRandomDataHY(int[] ori, int maxDoc, int numDocs)
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
