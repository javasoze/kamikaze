package com.kamikaze.lucenecodec.test;

import junit.framework.TestCase;

import org.apache.lucene.index.codecs.sep.IntIndexInput;
import org.apache.lucene.index.codecs.sep.IntIndexOutput;
import org.apache.lucene.index.codecs.sep.IntStreamFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import java.util.Random;

import com.kamikaze.lucenecodec.PForDeltaFixedIntBlockCodec;

public class TestLucenePForDeltaCodec extends TestCase{

  public void testPForDeltaSimpleIntBlocks() throws Exception {
    System.out.println("running test case : PForDeltaSimpleIntBlocks");
      Directory dir = new RAMDirectory();
      int blockSize = 128;
      IntStreamFactory f = new PForDeltaFixedIntBlockCodec(blockSize).getIntFactory();
      int testDataSize = 80024;
      int[] testData = new int[testDataSize];
      Random random = new Random(0);
      for(int i=0; i<testDataSize; ++i)
      {
        testData[i] = random.nextInt() & Integer.MAX_VALUE;
      }
      
      IntIndexOutput out = f.createOutput(dir, "test");
      for(int i=0;i<testDataSize;i++) {
        out.write(testData[i]);
      }
      out.close();

      System.out.println("start to read");
      IntIndexInput in = f.openInput(dir, "test");
      IntIndexInput.Reader r = in.reader();

      for(int i=0;i<testDataSize;i++) {
        int next = r.next();
        assertEquals(testData[i], next);
      }
      in.close();
      
      dir.close();
    }
  
  public void testPForDeltaEmptySimpleIntBlocks() throws Exception {
      Directory dir = new RAMDirectory();

      IntStreamFactory f = new PForDeltaFixedIntBlockCodec(128).getIntFactory();
      IntIndexOutput out = f.createOutput(dir, "test");

      // write no ints
      out.close();

      IntIndexInput in = f.openInput(dir, "test");
      in.reader();
      // read no ints
      in.close();
      dir.close();
    }
  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

}

