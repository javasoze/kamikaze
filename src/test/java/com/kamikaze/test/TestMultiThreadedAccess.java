package com.kamikaze.test;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;



public class TestMultiThreadedAccess extends TestCase{

  
  static int _length = 1000;
  static int _max = 300000;
  
  private static class TestThread extends Thread{
    PForDeltaDocIdSet p4d;
    TestThread(PForDeltaDocIdSet p4d){
      this.p4d = p4d;
    }
    public void run()
    {
      StatefulDSIterator dcit = p4d.iterator();
      
      try {
        int docid;
        while((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
        { 
          assertEquals(true, p4d.find(docid));
          
          assertEquals(false,p4d.find(35));
        }
      } catch (IOException e) {
        e.printStackTrace();
        fail(e.getMessage());
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  }
  
  @Test
  public void testMultiThreadedFind() throws IOException, InterruptedException
  {
    System.out.println("");
    System.out.println("Running Doc Find Multithreaded");
    System.out.println("----------------------------");
    
    double booster  = ((_max*1.0)/(1000f*_length));
    PForDeltaDocIdSet set = new PForDeltaDocIdSet();
    Random random = new Random();

    int max = 1000;
  
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();
    int prev = 0;
    for (int i = 55; i < _length*256; i++) {
      prev +=i;
      list.add(prev);
    }
    
    Collections.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
   
    final int maxVal =  list.get(list.size()-1);
    
      //P4D
      final PForDeltaDocIdSet p4d = new PForDeltaDocIdSet();
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
          Thread t = new TestThread(p4d);
          arr[i] = t;
          t.start();
      }
      for(int i=0;i<arr.length;i++)
      {
        arr[i].join();
      }
        
  }
  
}
