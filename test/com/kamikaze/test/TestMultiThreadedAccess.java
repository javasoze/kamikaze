package com.kamikaze.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.P4DDocIdSet;



public class TestMultiThreadedAccess {

  
  int _length = 10000;
  int _max = 30000000;
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
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (Exception e) {
                // TODO Auto-generated catch block
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
  
  
  @Test
  public void testMultiThreadedFind() throws IOException, InterruptedException
  {
    System.out.println("");
    System.out.println("Running Doc Find Multithreaded");
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
    for (int i = 55; i < _length*256; i++) {
      prev +=i;
      list.add(prev);
    }
    
    Collections.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
   
    final int maxVal =  list.get(list.size()-1);
    
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
                  assertEquals(true, p4d.find(docid));
                  
                  assertEquals(false,p4d.find(35));
                }
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (Exception e) {
                // TODO Auto-generated catch block
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
  
}
