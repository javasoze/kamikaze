package com.kamikaze.docidset.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generate the the hardwired unpack code to decode an compressed integer in Simple16 
 * @author hyan
 *
 */

public class GenerateUnpackClassS16 {
  
  private static final int S16_NUMSIZE = 16;
  private static final int S16_BITSSIZE = 28;
  
  //the possible number of compressed numbers hold in a single 32-bit integer
  private static final int[] S16_NUM = {28, 21, 21, 21, 14, 9, 8, 7, 6, 6, 5, 5, 4, 3, 2, 1};
  
  //the possible number of bits used to compress one number 
  private static final int[][] S16_BITS = { {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
      {2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0},
      {1,1,1,1,1,1,1,2,2,2,2,2,2,2,1,1,1,1,1,1,1,0,0,0,0,0,0,0},
      {1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,0,0,0,0,0,0,0},
      {2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,3,3,3,3,3,3,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {3,4,4,4,4,3,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,4,4,4,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {5,5,5,5,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {4,4,5,5,5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {6,6,6,5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {5,5,6,6,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {7,7,7,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {10,9,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {14,14,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {28,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} };
  
  public static void main(String[] args)
  {
    try
    {
      generateS16UnpackFunction("/Users/hyan/workspace/UnpackS16Tmp.java", 0);
      generateS16UnpackFunction2("/Users/hyan/workspace/UnpackS16Tmp2.java", 0);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  static public void generateS16UnpackFunction(String filename, int numIdx) throws IOException
  {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println("public static int s16DecompressOneNumberWithHardCodes(int[] out, int outOffset, int value, int numIdx){");
    pw.println("  switch(numIdx){");
    int i,j,shift, s16Bits;
    for(i=0; i<S16_BITS.length; ++i)
    {
      int num = S16_NUM[i];
      pw.println("    case " + i + ":");
      pw.println("    {");
      for(j=0, shift=0; j<num; ++j)
      {
          s16Bits = S16_BITS[i][j];
          if(shift>0)
          {
            if(j>0)
              pw.println("      out[outOffset+" + j + "] = (value >>> " + shift + ") & " + MASK[s16Bits] + ";");
            else
              pw.println("      out[outOffset] = (value >>> " + shift + ") & " + MASK[s16Bits] + ";");
          }
          else
          {
            if(j>0)
              pw.println("      out[outOffset+" + j + "] = value  & " + MASK[s16Bits] + ";");
            else
              pw.println("      out[outOffset] = value  & " + MASK[s16Bits] + ";");
          }
          shift += s16Bits;
      }
      pw.println("      return " + num + ";");
      pw.println("    }");
    }
    pw.println("    default:");
    pw.println("      return -1;");
    pw.println("  }");
    pw.println("}");
    pw.close();
   
  }
  
  static public void generateS16UnpackFunction2(String filename, int numIdx) throws IOException
  {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println("public static int s16DecompressOneNumberWithHardCodesIntegrated(int[] out, int outOffset, int value, int numIdx, int oribits, int[] expPos){");
    pw.println("  switch(numIdx){");
    int i,j,shift, s16Bits;
    for(i=0; i<S16_BITS.length; ++i)
    {
      int num = S16_NUM[i];
      pw.println("    case " + i + ":");
      pw.println("    {");
      for(j=0, shift=0; j<num; ++j)
      {
          s16Bits = S16_BITS[i][j];
          if(shift>0)
          {
            if(j>0)
              pw.println("      out[expPos[outOffset+" + j + "]] |= (((value >>> " + shift + ")  & " + MASK[s16Bits] + ")<<oribits);");
            else
              pw.println("      out[expPos[outOffset]] |= (((value >>> " + shift + ")  & " + MASK[s16Bits] + ")<<oribits);");
          }
          else
          {
            if(j>0)
              pw.println("      out[expPos[outOffset+" + j + "]] |= ((value  & " + MASK[s16Bits] + ")<<oribits);");
            else
              pw.println("      out[expPos[outOffset]] |= ((value  & " + MASK[s16Bits] + ")<<oribits);");
          }
          shift += s16Bits;
      }
      pw.println("      return " + num + ";");
      pw.println("    }");
    }
    pw.println("    default:");
    pw.println("      return -1;");
    pw.println("  }");
    pw.println("}");
    pw.close();
   
  }
  
  private static final String[] MASK = {"0x00000000",
    "0x00000001", "0x00000003", "0x00000007", "0x0000000f", "0x0000001f", "0x0000003f",
    "0x0000007f", "0x000000ff", "0x000001ff", "0x000003ff", "0x000007ff", "0x00000fff",
    "0x00001fff", "0x00003fff", "0x00007fff", "0x0000ffff", "0x0001ffff", "0x0003ffff",
    "0x0007ffff", "0x000fffff", "0x001fffff", "0x003fffff", "0x007fffff", "0x00ffffff",
    "0x01ffffff", "0x03ffffff", "0x07ffffff", "0x0fffffff", "0x1fffffff", "0x3fffffff",
    "0x7fffffff", "0xffffffff"};
  
}
