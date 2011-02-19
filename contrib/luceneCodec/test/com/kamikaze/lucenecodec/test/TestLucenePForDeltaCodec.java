package com.kamikaze.lucenecodec.test;

import junit.framework.TestCase;

import org.apache.lucene.index.BulkPostingsEnum;
import org.apache.lucene.index.codecs.sep.*;
import org.apache.lucene.index.codecs.sep.IntStreamFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import java.util.Random;

import com.kamikaze.lucenecodec.PForDeltaFixedIntBlockWithIntBufferFactory;

public class TestLucenePForDeltaCodec extends TestCase{

  public void testPForDeltaSimpleIntBlocks() throws Exception {
      System.out.println("running test case : testPForDeltaSimpleIntBlocks for PForDeltaFixedIntBlockCodec");
      Directory dir = new RAMDirectory();
      int blockSize = 128;
      IntStreamFactory f = new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize);
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

      IntIndexInput in = f.openInput(dir, "test");
      BulkPostingsEnum.BlockReader r = in.reader();
      final int[] buffer = r.getBuffer();
      int pointer = 0;
      int pointerMax = r.fill();
      assertTrue(pointerMax > 0);

      for(int i=0;i<testDataSize;i++) {
        final int expected = testData[i];
        final int actual = buffer[pointer++];
        assertEquals(actual + " != " + expected, expected, actual);
        if (pointer == pointerMax) {
          pointerMax = r.fill();
          assertTrue(pointerMax > 0);
          pointer = 0;
        }
      }
      in.close();
      dir.close();
    }
  
  public void testPForDeltaEmptySimpleIntBlocks() throws Exception {
      System.out.println("running test case : testPForDeltaEmptySimpleIntBlocks for PForDeltaFixedIntBlockCodec");
      Directory dir = new RAMDirectory();
      int blockSize = 128;
      IntStreamFactory f = new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize);
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

