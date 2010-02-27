package com.kamikaze.docidset.utils;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.impl.AbstractDocSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;

/** 
 * Utility class to make appropriate measurement calls to recognize optimal
 * representation for an ordered document set based on hints provided and 
 * min/max/count values on the docset if available. 
 * 
 * 
 * @author abhasin
 */
public class DocSetFactory 
{
  
  
  private static enum ACT { MIN, MAX, COUNT};
  
  private final static int INT_SIZE = 32;
  private final static int LONG_SHIFT = 6;
  private final static int BITSET_COMP_SWAP_RATIO = 15;
  private  static int DEFAULT_MIN = 0;
  private static int DEFAULT_MAX = 3000000;
  private static int DEFAULT_COUNT = 1000;
  private static long DEFAULT_INVOKE = 10000L;
  private static long INVOKE = DEFAULT_INVOKE;
  private static long INT_ARRAY_MAX = 500000;
  
  
  public static enum FOCUS {PERFORMANCE, SPACE, OPTIMAL};
  
  
  public static DocSet getDocSetInstance(int min, int max, int count, FOCUS hint)
  {
    // Default to Medians
      if( min==-1||max==-1 || count==-1)
      {
        min  = DEFAULT_MIN;
        max = DEFAULT_MAX;
        count = DEFAULT_COUNT;
        
      }
      else
      {
        bucket(min, ACT.MIN);
        bucket(max, ACT.MAX);
        bucket(count, ACT.COUNT);
      }
      
      
      INVOKE++;
      if(INVOKE==Long.MAX_VALUE)
        INVOKE=10000L;
     
      switch(hint)
      {
        // Always Favor IntArray or OpenBitSet
        case PERFORMANCE:
          if((((max-min)>>>LONG_SHIFT)+1)*2*INT_SIZE >  count * INT_SIZE)
            return new IntArrayDocIdSet(count);
          else
            //return new IntArrayDocIdSet(count);
            return new OBSDocIdSet(max-min+1);
          
       // Always Favor BitSet or Compression   
        case SPACE:
          if((max-min)/count<BITSET_COMP_SWAP_RATIO)
            return new OBSDocIdSet(max-min+1);
       
          else
            return new P4DDocIdSet();
          
        // All cases in consideration  
        case OPTIMAL:
          if((max-min)/count>BITSET_COMP_SWAP_RATIO)
          {
           if(count < AbstractDocSet.DEFAULT_BATCH_SIZE)
              return new IntArrayDocIdSet(count);
           else 
             return new P4DDocIdSet();
          }   
          else if((((max-min)>>>LONG_SHIFT)+1)*2*INT_SIZE >  count * INT_SIZE)
            return new IntArrayDocIdSet(count);
          else
            return new OBSDocIdSet(max-min+1);
          
          
      }
      
      return new IntArrayDocIdSet(count);
    
    
  }

  private static void bucket(int val, ACT act ) {
    
    switch (act)
    {
      case MIN:
      {
        DEFAULT_MIN = (int) ((DEFAULT_MIN*INVOKE + val)/(INVOKE+1));
        break;
      }
        
      case MAX:
      {
        DEFAULT_MAX = (int) ((DEFAULT_MAX*INVOKE + val)/(INVOKE+1));
        break;
      }
      case COUNT:  
      {
        DEFAULT_COUNT = (int) ((DEFAULT_COUNT*INVOKE + val)/(INVOKE+1));
        break;
      }
  
    }
  }

  
}
