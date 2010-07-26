package com.kamikaze.docidset.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class GenerateUnpackClass {
  
  static int[] POSSIBLE_B = {1,2,3,4,5,6,7,8,9,10,11,12,13,16,20};
  
  private static final int[] MASK = {0x00000000,
    0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f,
    0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff,
    0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff, 0x0001ffff, 0x0003ffff,
    0x0007ffff, 0x000fffff, 0x001fffff, 0x003fffff, 0x007fffff, 0x00ffffff,
    0x01ffffff, 0x03ffffff, 0x07ffffff, 0x0fffffff, 0x1fffffff, 0x3fffffff,
    0x7fffffff, 0xffffffff};
  
  static public void generateUnpackClass(int blockSize, String filename) throws IOException
  {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
    pw.println("package com.kamikaze.docidset.compression;");
    pw.println(" ");
    pw.println(" ");    
    
    pw.println("public class PForDeltaUnpack{");
    pw.println(" ");
    generateFunctionSelectionFile(pw);
    int HEADER_BITS = 32 * 2; // two int header
    for(int i=0; i<POSSIBLE_B.length; ++i)
    {
      pw.println(" ");
      generateUnpackFile(pw, HEADER_BITS, blockSize, POSSIBLE_B[i]);
    }
    
    
    pw.println("}");
    pw.close();
   
  }
  
  static private void generateUnpackFile(PrintWriter pw, int inOffset, int n, int bits)
  {
    pw.println("  static private void unpack" + bits + "(int[] out, int[] in)");
    pw.println("  {");
    
    for(int i=0; i<n; ++i, inOffset+=bits)
    {
      //out[outStart+i] = readBits(in, inStart+i*bits, bits);
      
      final int index = inOffset >>> 5;
      final int skip = inOffset & 0x1f;
      if(skip == 0)
        pw.println("    out[" + i + "] = ((in[" + (index) + "]) & " + MASK[bits] + ");");
      else
        pw.println("    out[" + i + "] = ((in[" + (index) + "] >>> " + (skip) + ") & " + MASK[bits] + ");");
      
      if (32 - skip < bits) {      
        pw.println("    out[" + i + "] |= ((in[" + (index+1) + "] << " + (32-skip) + ") & " + MASK[bits] + ");");
      }   
    }  
    pw.println("  }");
  }

  static private void generateFunctionSelectionFile(PrintWriter pw)
  {
     pw.println("  static public void unpack(int[] out, int[] in, int bits) {" );
     pw.println("    switch (bits) {");
     
     for(int i=0; i<POSSIBLE_B.length; i++)
     {
       int bits = POSSIBLE_B[i];  
       pw.println("      case " + bits + ":  " + "unpack" + bits + "(out, in); break;");
     }
     pw.println("      default: break;");
     pw.println("    }");
     pw.println("  }");
  }
    
}
