package com.kamikaze.docidset.api;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;


/**
 * Represents a sorted integer set
 */

public abstract class DocSet extends DocIdSet
{
	/**
	 * Add a doc id to the set
	 * @param docid
	 */
	public abstract void addDoc(int docid) throws IOException;
	

	/**
     * Return the set size
     * @return true if present, false otherwise
     */
	  public boolean find(int val) throws IOException
	  {
	    return findWithIndex(val)>-1?true:false;
	  }
	
	/**
     * Return the set size
     * @return index if present, -1 otherwise
     */
    public int findWithIndex(int val) throws IOException
    {
      return -1;
    }
    
	/**
     * Gets the number of ids in the set
     * @return size of the docset
     */
    public int size() throws IOException
    {
      return 0;
    }

   /**
    * Return the set size in bytes
    * @return index if present, -1 otherwise
    */
    public long sizeInBytes() throws IOException
    {
      return 0;
    }

    /**
     * Optimize by trimming underlying data structures
     */
     public void optimize() throws IOException
     {
       return;
     }
 
  	
}
