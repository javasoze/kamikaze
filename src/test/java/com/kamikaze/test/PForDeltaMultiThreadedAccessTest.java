package com.kamikaze.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;

//public class PForDeltaMultiThreadedAccessTest {
//  public static void main(String[] args)
//  {
//    PForDeltaMultiThreadedAccessTest1 t1= new PForDeltaMultiThreadedAccessTest1();
//    try{
//    t1.testSkipPerformance();
//    t1.testMultiThreadedFind();
//    }
//    catch(IOException eio)
//    {
//      eio.printStackTrace();
//    }
//    catch(InterruptedException ei)
//    {
//      ei.printStackTrace(); 
//    }
//  }
//}

// testing multiple threads: all threads share the same PForDeltaDocId set, and each thread has its own iterator iterating on it (only read operations). 
  public class PForDeltaMultiThreadedAccessTest {
  int _length = 10;
  int _max = 300000;
  //@Test
  public void testSkipPerformance() throws IOException, InterruptedException
  {
    System.out.println("");
    System.out.println("Running PForDelta Doc Skip Multithreaded");
    System.out.println("----------------------------");
    
    int[] list = new int[_length*256];
    int prev = 1;
    for (int i = 0; i < _length*256; i++) {
      prev +=i;
      list[i] = prev;
    }
    
    Arrays.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
    
      //PForDeltaDocIdSet
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
          Thread t = new Thread() {
            public void run()
            {
              StatefulDSIterator dcit = p4d.iterator();
              
              try {
                int docid;
//                String filename = "/Users/hyan/cloudData/" + this.getId(); 
//                PrintWriter pw = new PrintWriter(new FileOutputStream(filename, true));
                while((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
                { 
                  System.out.println("Thread " + this.getId() + ": " + docid);
                  //pw.print(docid + ",");
                  //Thread.sleep(0, 25000);
                }
//                pw.flush();
//                pw.close();
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
      System.out.println("---------completed----------");
  }
  
  
  @Test
  public void testMultiThreadedFind() throws IOException, InterruptedException
  {
    System.out.println("");
    System.out.println("Running PForDelta Doc Find Multithreaded");
    System.out.println("----------------------------");
    
    int[] list = new int[_length*256];
    int prev = 1;
    for (int i = 0; i < _length*256; i++) {
      prev +=i;
      list[i] = prev;
    }
    
    Arrays.sort(list);
    //System.out.println("Largest Element in the List:"+list.get( list.size() -1 ));
   
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
          Thread t = new Thread() {
            public void run()
            {
              StatefulDSIterator dcit = p4d.iterator();
              
              try {
                int docid;
                //String filename = "/Users/hyan/cloudData/" + this.getId(); 
                //PrintWriter pw = new PrintWriter(new FileOutputStream(filename, true));
                while((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
                { 
                  if(!p4d.find(docid))
                  {
                    System.out.println("thread "  + this.getId() + " cannot find " + docid);
                  }
//                  else
//                  {
//                    pw.print(docid + ",");
//                  }
                  assertEquals(true, p4d.find(docid));
                  assertEquals(false,p4d.find(35));
                }
//                pw.flush();
//                pw.close();
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
       System.out.println("------completed-----------");
  }
  
}
