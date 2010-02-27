package com.kamikaze.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;


public class KamikazeTest extends TestCase 
{ 

      public void testMultipleIntersections() throws Exception
      { 
        
        System.out.println("Running Multiple Intersections Test case...");
        System.out.println("-------------------------------------------");
        
              ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
              ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
          Random rand = new Random(System.currentTimeMillis()); 
              int maxDoc = 350000; 
              for(int i=0; i < 3; ++i) 
              { 
                      int numdocs = rand.nextInt(maxDoc); 
                      ArrayList<Integer> nums = new 
      ArrayList<Integer>(); 
                      HashSet<Integer> seen = new HashSet<Integer>(); 
                      for (int j = 0; j < numdocs; j++) 
                  { 
                              int nextDoc = rand.nextInt(maxDoc); 
                              if(seen.contains(nextDoc)) 
                              { 
                                      while(seen.contains(nextDoc)) 
                                      { 
                                              nextDoc = 
      rand.nextInt(maxDoc); 
                                      } 
                              } 
                              nums.add(nextDoc); 
                              seen.add(nextDoc); 
                  } 
                      Collections.sort(nums); 
                      obs.add(createObs(nums, maxDoc)); 
                      docs.add(createDocSet(nums)); 
              } 
              OpenBitSet base = obs.get(0); 
              for(int i = 1; i < obs.size(); ++i) 
              { 
                      base.intersect(obs.get(i)); 
              } 
              
              AndDocIdSet ands = new AndDocIdSet(docs); 
              long card1 = base.cardinality(); 
              long card2 = ands.size(); 
              //System.out.println(card1+":"+card2); 
              assertEquals(card1, card2); 
      } 
      
      
      private OpenBitSet createObs(ArrayList<Integer> nums, int maxDoc) { 
        OpenBitSet bitSet = new OpenBitSet(maxDoc); 
        for(int num:nums) 
          bitSet.set(num); 
        return bitSet; 
      } 
      
      private DocIdSet createDocSet(ArrayList<Integer> nums) throws Exception{ 
        DocSet p4d = DocSetFactory.getDocSetInstance(0, 35000000, 200000, 
            DocSetFactory.FOCUS.OPTIMAL); 
        for(int num:nums) 
         p4d.addDoc(num); 
        return p4d; 
      } 
      
      
      public void testForOutOfBounds() throws Exception
      { 
        
        System.out.println("Running OutOfBounds Test case...");
        System.out.println("-------------------------------------------");
        
          Random rand = new Random(System.currentTimeMillis()); 
          int maxDoc = 350000; 
          ArrayList<Integer> nums = new ArrayList<Integer>(); 
          HashSet<Integer> seen = new HashSet<Integer>(); 
          for(int i=0; i < 68; ++i) 
          { 
              int nextDoc=rand.nextInt(maxDoc); 
              if(seen.contains(nextDoc)) 
              { 
                  while(seen.contains(nextDoc)) 
                  { 
                      nextDoc += rand.nextInt(maxDoc); 
                  } 
              } 
              nums.add(nextDoc); 
              seen.add(nextDoc); 
          } 
          Collections.sort(nums); 
          DocSet docs = new P4DDocIdSet(); 
          boolean saw403 = false;
          for (Integer integer : nums) 
          { 
              saw403=(integer == 403);
              docs.addDoc(integer); 
          } 
          boolean got = docs.find(403); 
          assertEquals(saw403, got);
      } 
      
      public void testPartialEmptyAnd() throws IOException 
      { 
              try 
              { 
                System.out.println("Running Partial Empty And    Test case...");
                System.out.println("-------------------------------------------");
                
                      DocSet ds1 = new P4DDocIdSet(); 
                      DocSet ds2 = new P4DDocIdSet(); 
                      ds2.addDoc(42); 
                      ds2.addDoc(43); 
                      ds2.addDoc(44); 
                      ArrayList<DocIdSet> docs = new 
ArrayList<DocIdSet>(); 
                      docs.add(ds1); 
                      docs.add(ds2); 
                      OrDocIdSet orlist1 = new OrDocIdSet(docs); 
                      DocSet ds3 = new P4DDocIdSet(); 
                      DocSet ds4 = new P4DDocIdSet(); 
                      ds4.addDoc(42); 
                      ds4.addDoc(43); 
                      ds4.addDoc(44); 
                      ArrayList<DocIdSet> docs2 = new 
ArrayList<DocIdSet>(); 
                      docs2.add(ds3); 
                      docs2.add(ds4); 
                      OrDocIdSet orlist2 = new OrDocIdSet(docs2); 
                      ArrayList<DocIdSet> docs3 = new 
ArrayList<DocIdSet>(); 
                      docs3.add(orlist1); 
                      docs3.add(orlist2); 
                      AndDocIdSet andlist = new AndDocIdSet(docs3); 
                      
                      DocIdSetIterator iter = andlist.iterator(); 
                      @SuppressWarnings("unused") 
                      int docId = -1; 
                      while((docId = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) 
                      { 
                      }   
              } 
              catch(Exception e) 
              { 
                      e.printStackTrace();
              } 
              assertTrue(true); 
      } 
}
