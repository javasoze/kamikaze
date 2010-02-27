package com.kamikaze.docidset.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.BitSet;

import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.utils.IntArray;
import com.kamikaze.docidset.utils.LongSegmentArray;
import com.kamikaze.docidset.utils.MyOpenBitSetArray;

public class TestSizeEstimates
{
 
  public static void estimateIntArraySize(int size)
  {
    try {
      Class clazz =  IntArray.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,size));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  public static void estimateBitSetSize(int size)
  {
    try {
      Class clazz =  BitSet.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,size));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void estimateMyOpenBitSetSize(int size)
  {
    try {
      Class clazz = MyOpenBitSet.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,size));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
 


  private static void estimateArrayListSize(int size) {
    try {
      Class clazz = ArrayList.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,size));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

 
  
  
  public static long sizeOf(Class clazz, int userData) {
    long size= 0;
    Object[] objects = new Object[100];
    try {
      
      Constructor c;
      try{
        c = clazz.getConstructor(long.class);
      }
      catch(Exception e)
      {
        c = null;
      }
      
       if(c == null)
         c= clazz.getConstructor(int.class); 
       
       Object primer = c.newInstance(userData);
       long startingMemoryUse = getUsedMemory();
       for (int i = 0; i < objects.length; i++) {
          objects[i] = c.newInstance(userData);
          fill(objects[i], userData);
          optimize(objects[i]);
       }
       long endingMemoryUse = getUsedMemory();
       float approxSize = (endingMemoryUse - 
                           startingMemoryUse)/100f ;
       size = Math.round(approxSize);
    } catch (Exception e) {
       e.printStackTrace();
       System.out.println("WARNING:couldn't instantiate"
                          +clazz);
       e.printStackTrace();
    }
    return size;
 }
  
 private static void estimateNativeIntArraySize(int userData) {
    
    int array[] = (int[])Array.newInstance(int.class, userData);
    
    long size= 0;
    Object[] objects = new Object[100];
    try {
      
    long startingMemoryUse = getUsedMemory();
       for (int i = 0; i < objects.length; i++) {
          objects[i] = (int[])Array.newInstance(int.class, userData);
          fill(objects[i], userData);
          optimize(objects[i]);
       }
       long endingMemoryUse = getUsedMemory();
       float approxSize = (endingMemoryUse - 
                           startingMemoryUse) /100f;
       size = Math.round(approxSize);
    } catch (Exception e) {
       e.printStackTrace();
       System.out.println("WARNING:couldn't instantiate Native Int Array");

    }
    System.out.println(int[].class.getName()+":"+size);
  } 
  
  
  private static void estimateMyOpenBitSetArraySize(int userData) {
    
    try {
      Class clazz = MyOpenBitSetArray.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,userData));
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 
  
  
  private static void estimateLongSegmentArraySize(int userData) {
    // TODO Auto-generated method stub
    try {
      Class clazz = LongSegmentArray.class;
      System.out.println(clazz.getName()+":"+sizeOf(clazz,userData));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
 private static void fill(Object object, int userData) {
    
   if(object instanceof MyOpenBitSet)
     ((MyOpenBitSet)object).set(userData-1);
   else if(object instanceof ArrayList)
   {
     for(int i = 0 ;i < userData; i++ )
     {
         ((ArrayList)object).add(new Integer(10));
     }
   }
   else if(object instanceof IntArray)
   {
     for(int i = 0 ;i < userData; i++ )
     {
         ((IntArray)object).set(i, 10);
     }
   }
   else if(object instanceof int[] )
   {
     for(int i = 0 ;i < userData; i++ )
     {
        ( (int[])object)[i] = 10;
     }
   }
   else if(object instanceof long[] )
   {
     for(int i = 0 ;i < userData; i++ )
     {
        ( (long[])object)[i] = 10;
     }
   }
   else if(object instanceof MyOpenBitSetArray)
   {
     for(int i = 0 ;i < userData; i++ )
     {
      
       ((MyOpenBitSetArray) object).add(new MyOpenBitSet(1200));
       fill(((MyOpenBitSetArray) object).get(i) ,1200);
     }
   }
   else if(object instanceof LongSegmentArray)
   {
     for(int i = 0 ;i < userData; i++ )
     {
       ((LongSegmentArray) object).add(new long[2000>>>6]);
       fill(((LongSegmentArray) object) .get(i),2000>>>6);
     }
   }
 
  }

private static void optimize(Object object) {
    
   if(object instanceof MyOpenBitSet)
     ((MyOpenBitSet)object).trimTrailingZeros();
   else if(object instanceof ArrayList)
     ((ArrayList)object).trimToSize();
   else if(object instanceof IntArray)
     ((IntArray)object).seal();
   
  
   
  }


private static long getUsedMemory() {
    gc();
    long totalMemory = Runtime.getRuntime().totalMemory();
    gc();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long usedMemory = totalMemory - freeMemory;
    return usedMemory;
 }
 private static void gc() {
    try {
       System.gc();
       Thread.currentThread().sleep(100);
       System.runFinalization();
       Thread.currentThread().sleep(100);
       System.gc();
       Thread.currentThread().sleep(100);
       System.runFinalization();
       Thread.currentThread().sleep(100);
      
    } catch (Exception e) {
       e.printStackTrace();
    }
 }
 public static void main(String[] args) {
   //estimateMyOpenBitSetSize(1200);
   //estimateBitSetSize(1200);
   estimateIntArraySize(1);
   estimateIntArraySize(10);
   estimateIntArraySize(100);
   estimateIntArraySize(1000);
   estimateIntArraySize(10000);
   
   
     //estimateArrayListSize(1024);
   //estimateMyOpenBitSetArraySize(32000);
   estimateLongSegmentArraySize(1);
   estimateLongSegmentArraySize(10);
   estimateLongSegmentArraySize(100);
   estimateLongSegmentArraySize(1000);
   estimateLongSegmentArraySize(10000);
   
   estimateNativeIntArraySize(1);
   estimateNativeIntArraySize(10);
   estimateNativeIntArraySize(100);
   estimateNativeIntArraySize(1000);
   estimateNativeIntArraySize(10000);
   
   
   
   //estimateLongSegmentArraySize(32000);
   System.exit(0);
 }




}
