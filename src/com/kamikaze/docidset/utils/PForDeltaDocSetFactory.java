package com.kamikaze.docidset.utils;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;

/** 
 * Utility class to choose the optimal data representation method (from IntArrayDocIdSet and PForDeltDocIdSet)
 * based on hints provided and the length of the sorted integer array. Suggestion about how to choose them is provides 
 * at http://sna-projects.com/kamikaze/suggestion.php.
 * 
 * @author hao yan
 */

public class PForDeltaDocSetFactory 
{
  
  public final static int LISTLENGTH_CHOOSING_INTARRAY = 1800;
//1800 is from the experimental results on http://sna-projects.com/kamikaze/suggestion.php (using the com.kamikaze.docidset.utils.PForDeltaEstimateSizeOfMultiChoices)
  
  public static enum FOCUS {PERFORMANCE, SPACE, OPTIMAL};
  
  public static DocSet getPForDeltaDocSetInstance()
  {
    return new PForDeltaDocIdSet();
  }
  
  public static DocSet getDocSetInstance(int min, int max, int count, FOCUS hint)
  {
      switch(hint)
      {
        case PERFORMANCE:
            return new IntArrayDocIdSet(count);
          
        case SPACE:
          if(count<1800) 
            return new IntArrayDocIdSet(count);
          else
            return new PForDeltaDocIdSet();
          
        case OPTIMAL:
          if(count<1800) 
            return new IntArrayDocIdSet(count);
          else
            return new PForDeltaDocIdSet();
      }
      return new IntArrayDocIdSet(count);
  }
}

