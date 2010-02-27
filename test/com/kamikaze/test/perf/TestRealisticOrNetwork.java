package com.kamikaze.test.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;

public class TestRealisticOrNetwork {

  
  public static void main(String args[]) throws IOException
  {
    
    ArrayList<DocIdSet> arr = new ArrayList<DocIdSet>() ;
   
    for(int i=0;i<4;i++)
    {
        arr.add(loadDegree(i));
     
    }
    OrDocIdSet ord = new OrDocIdSet(arr);
    DocIdSetIterator orit  = ord.iterator();
    
    int doc;
    while((doc = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS);
  }

  private static DocSet loadDegree(int degree) throws IOException {
    
    
    
    
    switch (degree)
    {
      case 0:
        DocSet docSet  = DocSetFactory.getDocSetInstance(2448149, 2448149, 1, DocSetFactory.FOCUS.OPTIMAL);
        docSet.addDoc(2448149);
        return docSet;
      case 1:
      {
         BufferedReader bfr = new BufferedReader(new FileReader(new File("/Users/abhasin/degree1s.txt")));
         DocSet d1 = new IntArrayDocIdSet();
         
         while(true)
         {
           String line = bfr.readLine();
           if(line == null||line == "")
             return d1;
           else
             d1.addDoc(Integer.parseInt(line.trim()));
         } 
      }
      case 2:
      {
        BufferedReader bfr = new BufferedReader(new FileReader(new File("/Users/abhasin/degree2s.txt")));
        DocSet d2 = new IntArrayDocIdSet();
        while(true)
        {
          String line = bfr.readLine();
          if(line == null||line == "")
            return d2;
          else
            d2.addDoc(Integer.parseInt(line.trim()));
        }
        
      } 
      case 3:
      {
        BufferedReader bfr = new BufferedReader(new FileReader(new File("/Users/abhasin/degree3s.txt")));
        DocSet d3 = new P4DDocIdSet();
        ArrayList<Integer> data = new ArrayList<Integer>();
        
        while(true)
        {
          String line = bfr.readLine();
          if(line == null||line == "")
            break;
          else
          {
            data.add(Integer.parseInt(line.trim()));
          }
            
        }
        Collections.sort(data);
        for(Integer d : data)
        {
          System.out.println(d);
          d3.addDoc(d);
        }
          return d3;
      } 
        
       
    }
    return null;
      
    
  }
}
