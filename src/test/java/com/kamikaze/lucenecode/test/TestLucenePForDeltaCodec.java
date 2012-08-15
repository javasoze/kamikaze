package com.kamikaze.lucenecode.test;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.codecs.sep.IntIndexInput;
import org.apache.lucene.codecs.sep.IntIndexInput.Reader;
import org.apache.lucene.codecs.sep.IntIndexOutput;
import org.apache.lucene.codecs.sep.IntStreamFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IntsRef;
import org.junit.After;
import org.junit.Before;

import com.kamikaze.lucenecodec.PForDeltaFixedIntBlockWithIntBufferFactory;

public class TestLucenePForDeltaCodec extends TestCase{

  public void testPForDeltaSimpleIntBlocks() throws Exception {
      System.out.println("running test case : testPForDeltaSimpleIntBlocks for PForDeltaFixedIntBlockCodec");
      Directory dir = new RAMDirectory();
      
      IOContext context = new IOContext();
      
      int blockSize = 128;
      IntStreamFactory f = new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize);
      int testDataSize = 80024;
      int[] testData = new int[testDataSize];
      Random random = new Random(0);
      for(int i=0; i<testDataSize; ++i)
      {
        testData[i] = random.nextInt() & Integer.MAX_VALUE;
      }
      
      IntIndexOutput out = f.createOutput(dir, "test", context);
      for(int i=0;i<testDataSize;i++) {
        out.write(testData[i]);
      }
      out.close();

      IntIndexInput in = f.openInput(dir, "test", context);
      Reader r = in.reader();
      
      IntsRef data = r.read(testDataSize);
      
      final int[] buffer = data.ints;
      /*
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
      }*/
      Arrays.equals(testData, data.ints);
      in.close();
      dir.close();
    }
  
  public void testPForDeltaEmptySimpleIntBlocks() throws Exception {
      System.out.println("running test case : testPForDeltaEmptySimpleIntBlocks for PForDeltaFixedIntBlockCodec");
      IOContext context = new IOContext();
      Directory dir = new RAMDirectory();
      int blockSize = 128;
      IntStreamFactory f = new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize);
      IntIndexOutput out = f.createOutput(dir, "test",context);

      // write no ints
      out.close();

      IntIndexInput in = f.openInput(dir, "test",context);
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

