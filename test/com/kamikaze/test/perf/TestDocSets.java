package com.kamikaze.test.perf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;

public class TestDocSets {

  public static void testOBSDocIdSet(int batch, int length, int max)
      throws IOException {

    System.out.println("Running OpenBitSet test");
    System.out.println("----------------------------");
    OBSDocIdSet set = new OBSDocIdSet(max);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();

    // Minimum 5 bits
    int randomizer = 0;
    double totalCompressionTime = 0;
    double totalDecompressionTime = 0;
    double totalCompressionRatio = 0;

    long now = System.nanoTime();
    for (int i = 1; i < (length); i++)
    // for(int i = 1;i<2;i++)
    {

      // System.out.println("Randomizer ="+randomizer);
      ArrayList<Integer> list = new ArrayList<Integer>();

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }
      Collections.sort(list);
      randomizer += 1000;

      for (int k = 0; k < batch; k++) {
        set.addDoc(list.get(k));
      }

      // System.out.println("At :" + i +" "+(randomizer-1000) +" " +
      // randomizer);
    }

    totalCompressionTime = System.nanoTime() - now;
    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator its = set.iterator();
    int x = 0;
    now = System.nanoTime();
    while ((x=its.nextDoc() )!=DocIdSetIterator.NO_MORE_DOCS) {

    }
    totalDecompressionTime = System.nanoTime() - now;

    System.out.println("Total decompression time :" + totalDecompressionTime
        + ": for " + ((double) batch * length) / 1000000 + " M numbers");
    System.out.println("Compression Ratio:" + ((double) set.sizeInBytes() * 8)
        / (batch * length * 32) + " for max=" + max);

  }

  private static void testIntArrayDocIdSetIterateSanity(int size)
      throws IOException {
    System.out.println("Running IntArrayDocIdSet Iterate sanity test");
    System.out.println("----------------------------");
    IntArrayDocIdSet set = new IntArrayDocIdSet(size);
    // OBSDocIdSet set = new OBSDocIdSet(1000);

    Random random = new Random();
    ArrayList<Integer> list = new ArrayList<Integer>();
    ArrayList<Integer> list2 = new ArrayList<Integer>();

    long now = System.nanoTime();
    for (int i = 0; i < size; i++) {
      list.add((int) (i * 100 + random.nextDouble() * 1000));
    }

    Collections.sort(list);

    for (int k = 0; k < size; k++) {
      set.addDoc(list.get(k));
    }

    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator dcit = set.iterator();
    int x = 0;
    now = System.nanoTime();
    while ((x=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      list2.add(x);
      // System.out.println(dcit.doc());
    }

    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).intValue() != list2.get(i).intValue())
        System.err.println("Expected:" + list.get(i) + " but was:"
            + list2.get(i) + " at index :" + i);
    }

  }

  private static void testIntArrayDocIdSetSkipSanity(int size)
      throws IOException {
    System.out.println("Running IntArrayDocIdSet Skip Sanity test");
    System.out.println("----------------------------");
    IntArrayDocIdSet set = new IntArrayDocIdSet(size);

    Random random = new Random();
    ArrayList<Integer> list = new ArrayList<Integer>();
    ArrayList<Integer> list2 = new ArrayList<Integer>();

    long now = System.nanoTime();
    for (int i = 0; i < size; i++) {
      list.add((int) (i * 100 + random.nextDouble() * 1000));
    }

    Collections.sort(list);

    for (int k = 0; k < size; k++) {
      set.addDoc(list.get(k));
    }

    // System.out.println("Total compression time :"+totalCompressionTime+":
    // for"+((double)batch*length)/1000000+" M numbers");
    StatefulDSIterator dcit = set.iterator();
    for (int i = 0; i < 200000; i += 60) {
      try {
        int doc;
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          // System.out.println("Target:"+i+" Found:"+dcit.doc());
          if (doc != list.get(dcit.getCursor()))
            System.err.println("1." + doc + ":" + dcit.getCursor() + ":"
                + list.get(dcit.getCursor()));
          if ((doc = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("2." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));
          if ((doc = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("3." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));

        }
        // else
        // System.out.println("Number out of range");
      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }

  }

  private static void testSimpleArraySet(int batch, int length) {
    System.out.println("Running No Alloc Integer Array Set test");
    System.out.println("----------------------------");

    int randomizer = 0;
    Random random = new Random();
    int[] source = new int[batch * length];

    for (int i = 0; i < (batch * length); i++) {
      source[i] = randomizer + (int) (random.nextDouble() * 1000);
    }

    long now = System.nanoTime();

    for (int i = 0; i < batch * length; i++)
      randomizer = source[i];

    System.out.println("Total decompression time :"
        + ((double) System.nanoTime() - now) + ": for "
        + ((double) batch * length) / 1000000 + " M numbers");

  }
/*
  private static void testP4DCompressed(int batch, int length)
      throws IOException {
    P4DSetNoBase set = new P4DSetNoBase();
    System.out.println("Running P4Delta Compressed set test");
    System.out.println("----------------------------");
    Random random = new Random();
    OpenBitSet compressedSet = null;
    long now = System.nanoTime();
    int vals[] = null;
    int[] input = new int[batch];
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int exceptionOver = 12;
    int base = 0;
    int randomizer = 0;
    double totalCompressionTime = 0;
    double totalDecompressionTime = 0;
    double totalCompressionRatio = 0;

    for (int i = 1; i < length + 1; i++) {

      // System.out.println("Randomizer ="+randomizer);
      ArrayList<Integer> list = new ArrayList<Integer>();
      int bVal[] = new int[33];

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
        // list.add(newTest[k]);
      }
      Collections.sort(list);
      randomizer += 1000;
      input[0] = list.get(0);
      for (int j = batch - 1; j > 0; j--) {
        try {
          input[j] = list.get(j) - list.get(j - 1);
          if (input[j] == 0)
            bVal[1]++;
          else
            bVal[(int) (Math.log(input[j]) / logBase2) + 1]++;

        } catch (ArrayIndexOutOfBoundsException w) {
          System.out.println(j);
        }

      }

      base = input[0];
      input[0] = 0;

      // formulate b value
      for (int k = 32; k > 4; k--) {
        exceptionCount += bVal[k];
        if (exceptionCount > exceptionOver) {
          b = k;
          exceptionCount -= bVal[k];
          break;
        }
      }

      b += 1;
      set.setParam(base, b, batch, exceptionCount);

      compressedSet = set.compress(input);
      totalCompressionTime += (System.nanoTime() - now);
      // System.out.println("Time to compress:"+ (System.nanoTime() - now )+ "
      // nanos..");

      now = System.nanoTime();

      long nowMillis = System.currentTimeMillis();
      // vals = set.decompress(compressedSet);
      int lastVal = base;
      for (int l = 0; l < batch; l++) {
        lastVal += set.get(compressedSet, l);
      }

      // System.out.println("Time to decompress:"+ (System.nanoTime() - now )+ "
      // nanos..");
      totalDecompressionTime += (System.nanoTime() - now);
      totalCompressionRatio += (double) compressedSet.size() / (batch * 32);*/

      // System.out.println("Average Compression Time after:"+i+" iterations=");
      // writer.write(randomizer-1000+" " + randomizer+ " "
      // +totalCompressionTime/i+" "+totalDecompressionTime/i+"
      // "+totalCompressionRatio/i+"\n");
      // System.out.println(randomizer-1000+" " + randomizer+ " "
      // +totalCompressionTime/i+" "+totalDecompressionTime/i+"
      // "+totalCompressionRatio/i);

      /*
       * lastVal = base; for(int l=0;l<batch;l++) { lastVal +=
       * set.get(compressedSet, l); if(lastVal!=list.get(l)) {
       * System.err.println("ERROR: iteration:"+i+" expected-"+list.get(l)+" but
       * was-"+lastVal +" at index-"+l ); for(int j=0; j< batch; j++){
       * System.out.print(list.get(j)+","); } System.out.println(); lastVal =
       * base; for(int j=0; j< batch; j++){ lastVal +=set.get(compressedSet,
       * j); System.out.print(lastVal+","); } System.exit(0); } }
       */

      // System.out.println("Compression
      // Ratio:"+(double)compressedSet.size()/(128*32));
   /* }

    System.out.println("Total decompression time :" + totalDecompressionTime
        + ": for " + ((double) batch * length) / 1000000 + " M numbers");
    // System.out.println("Average Decompression Time after:"+i+"
    // iterations="+totalDecompressionTime/i);
    System.out.println("Compression Ratio :" + totalCompressionRatio / length);

  }*/

  private static void testP4DDocIdSetNonBoundaryCompressionSanity(int batch,
      int length, int extra) throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out
        .println("Running P4DeltaDocSet Non-Boundary Compression Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    randomizer += 1000;
    for (int i = 0; i < extra; i++)
      list.add(randomizer + (int) (random.nextDouble() * 1000));

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list)
      set.addDoc(c);

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();
    // int x = -1;
    int doc;
    while ((doc = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      list2.add(doc);
      // dcit.doc();
    }

    System.out.println(list);
    System.out.println(list2);
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).intValue() != list2.get(i).intValue())
        System.err.println("Expected:" + list.get(i) + " but was:"
            + list2.get(i) + " at index :" + i);
    }
    System.out.println("Verified..");
  }

  private static void testP4DDocIdSetNonBoundarySkipSanity(int batch,
      int length, int extra) throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet Non-Boundary skip test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    randomizer += 1000;
    for (int i = 0; i < extra; i++)
      list.add(randomizer + (int) (random.nextDouble() * 1000));

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list)
      set.addDoc(c);

    StatefulDSIterator dcit = set.iterator();
    System.out.println(list);
    for (int i = 0; i < 200000; i += 60) {
      try {
        int doc;
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          System.out.println("Target:" + i + " Found:" + doc);
          if (doc != list.get(dcit.getCursor()))
            System.err.println("1." + doc + ":" + dcit.getCursor() + ":"
                + list.get(dcit.getCursor()));
          if ((doc = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("2." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));
          if ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("3." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));

        }
        // else
        // System.out.println("Number out of range");
      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }

  }

  private static void testP4DDocIdSetIteratePerf(int batch, int length)
      throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet Iteration Performance test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list) {
      set.addDoc(c);
    }

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();
    // int x = -1;
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      // list2.add(dcit.doc ());
    }
    totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Decompression time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * length)
        / 1000000 + " M numbers");

  }

  private static void testP4DDocIdSetSkipPerf(int batch, int length)
      throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet Skip Perf test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list) {
      set.addDoc(c);
    }

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();

    // Get a new iterator
    dcit = set.iterator();

    for (int i = 0; i < 2000000; i += 60) {

      if (dcit.advance(i)!=DocIdSetIterator.NO_MORE_DOCS) {
      }
    }

    totalDecompressionTime = System.nanoTime() - now;

    System.out.println("Skipping time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * length)
        / 1000000 + " M numbers");

  }

  private static void testP4DDocIdSet(int batch, int length) throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list) {
      set.addDoc(c);
    }

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();
    // int x = -1;
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      list2.add(doc);
      // dcit.doc();
    }
    totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Decompression time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * length)
        / 1000000 + " M numbers");

    System.out.println(list);
    System.out.println(list2);
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).intValue() != list2.get(i).intValue())
        System.err.println("Expected:" + list.get(i) + " but was:"
            + list2.get(i) + " at index :" + i);
    }

    now = System.nanoTime();
    // Get a new iterator
    dcit = set.iterator();

    for (int i = 0; i < 200000; i += 60) {
      try {
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          System.out.println("Target:" + i + " Found:" + doc);
          if (doc != list.get(dcit.getCursor()))
            System.err.println("1." + doc + ":" + dcit.getCursor() + ":"
                + list.get(dcit.getCursor()));
          if ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("2." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));
          if ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
            if (doc != list.get(dcit.getCursor()))
              System.err.println("3." + doc + ":" + dcit.getCursor()
                  + ":" + list.get(dcit.getCursor()));

        }
        // else
        // System.out.println("Number out of range");
      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }
    totalDecompressionTime = System.nanoTime() - now;

    System.out.println("Skipping time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * length)
        / 1000000 + " M numbers");

  }

  private static void testP4DDocIdSetSkipSanity(int batch, int length)
      throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet Skip Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    Collections.sort(list);

    for (Integer c : list) {
      set.addDoc(c);
    }

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();

    for (int i = 0; i < 200000; i += 60) {
      int doc;
      if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
        System.out.println("Target:" + i + " Found:" + doc);
        if (doc != list.get(dcit.getCursor()))
          System.err.println("1." + doc + ":" + dcit.getCursor() + ":"
              + list.get(dcit.getCursor()));
        if ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
          if (doc != list.get(dcit.getCursor()))
            System.err.println("2." + doc + ":" + dcit.getCursor() + ":"
                + list.get(dcit.getCursor()));
        if ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
          if (doc != list.get(dcit.getCursor()))
            System.err.println("3." + doc + ":" + dcit.getCursor() + ":"
                + list.get(dcit.getCursor()));

      }
      // else
      // System.out.println("Number out of range");

    }
    System.out.println("Verified skipping behavior");

  }

  private static void testP4DDocIdSetCompressionSanity(int batch, int length)
      throws IOException {
    P4DDocIdSet set = new P4DDocIdSet(batch);
    System.out.println("Running P4DeltaDocSet Compression Sanity test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;
    double totalDecompressionTime = 0;
    List<Integer> list = new LinkedList<Integer>();
    LinkedList<Integer> list2 = new LinkedList<Integer>();

    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
    }

    Collections.sort(list);
    // System.out.println(list);
    for (Integer c : list) {
      set.addDoc(c);
    }

    StatefulDSIterator dcit = set.iterator();

    long now = System.nanoTime();
    // int x = -1;
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      list2.add(doc);
      // dcit.doc();
    }
    totalDecompressionTime = System.nanoTime() - now;
    System.out.println("Decompression time for batch size:" + batch + " is "
        + totalDecompressionTime + " for " + ((double) batch * length)
        / 1000000 + " M numbers");

    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).intValue() != list2.get(i).intValue())
        System.err.println("Expected:" + list.get(i) + " but was:"
            + list2.get(i) + " at index :" + i);
    }

  }

  private static void testOrDocIdSetSkip(int batch, int length, int all)
      throws IOException {
    System.out.println("Running OrDocIdSet Skip test");
    System.out.println("----------------------------");
    ArrayList<DocIdSet> docSets = new ArrayList<DocIdSet>();
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    for (int j = 0; j < all; j++) {
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      P4DDocIdSet docSet = new P4DDocIdSet(batch);
      randomizer = 0;
      for (int i = 1; i < length + 1; i++) {

        int bVal[] = new int[33];
        for (int k = 0; k < batch; k++) {
          intSet.add(randomizer + (int) (random.nextDouble() * 1000));
        }

        randomizer += 1000;
        Collections.sort(intSet);

      }
      for (Integer c : intSet) {
        docSet.addDoc(c);
      }
      docSets.add(docSet);
    }

    DocIdSetIterator dcit = new OrDocIdSet(docSets).iterator();

    for (int i = 0; i < 200000; i += 64) {
      try {
        int doc;
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          System.out.println("Target:" + i + " Found:" + doc);
          dcit.nextDoc();
          dcit.nextDoc();
          // if(dcit.doc()!=list.get(dcit.getCursor()))
          // System.err.println("1."+dcit.doc()+":"+dcit.getCursor()+":"+list.get(dcit.getCursor()));
          // if(dcit.next())
          // if(dcit.doc()!=list.get(dcit.getCursor()))
          // System.err.println("2."+dcit.doc()+":"+dcit.getCursor()+":"+list.get(dcit.getCursor()));
          // if(dcit.next())
          // if(dcit.doc()!=list.get(dcit.getCursor()))
          // System.err.println("3."+dcit.doc()+":"+dcit.getCursor()+":"+list.get(dcit.getCursor()));

        }
        // else
        // System.out.println("Number out of range");
      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }

  }

  private static void testOrDocIdSet(int batch, int length, int all)
      throws IOException {

    System.out.println("Running OrDocIdSet test");
    System.out.println("----------------------------");
    ArrayList<DocIdSet> docSets = new ArrayList<DocIdSet>();
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    for (int j = 0; j < all; j++) {
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      P4DDocIdSet docSet = new P4DDocIdSet(batch);
      randomizer = 0;
      for (int i = 1; i < length + 1; i++) {

        int bVal[] = new int[33];
        for (int k = 0; k < batch; k++) {
          intSet.add(randomizer + (int) (random.nextDouble() * 1000));
        }

        randomizer += 1000;
        Collections.sort(intSet);

      }
      for (Integer c : intSet) {
        docSet.addDoc(c);
      }
      System.out.println(intSet);
      docSets.add(docSet);
    }

    DocIdSetIterator oit = new OrDocIdSet(docSets).iterator();
    int doc;
    while ((doc=oit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.println(doc);

  }

  private static void testAndDocIdSet(int batch, int length, int all)
      throws IOException {
    System.out.println("Running AndDocIdSet test");
    System.out.println("----------------------------");
    ArrayList<DocIdSet> docSets = new ArrayList<DocIdSet>();
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    for (int j = 0; j < all; j++) {
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      P4DDocIdSet docSet = new P4DDocIdSet(batch);
      randomizer = 0;
      for (int i = 1; i < length + 1; i++) {

        int bVal[] = new int[33];
        for (int k = 0; k < batch; k++) {
          intSet.add(randomizer + (int) (random.nextDouble() * 1000));
        }

        randomizer += 1000;
        Collections.sort(intSet);

      }
      for (Integer c : intSet) {
        docSet.addDoc(c);
      }
      docSets.add(docSet);
      System.out.println(intSet);
    }

    DocIdSetIterator oit = new AndDocIdSet(docSets).iterator();

    int doc;
    while ((doc=oit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      System.out.println(doc);

    }

  }

  private static void testNotDocIdSetSkipSanity(int batch, int length, int max)
      throws IOException {
    int[] set = new int[] { 7, 7, 22, 32, 62, 69, 69, 78, 84, 91, 93, 95, 109,
        111, 121, 124, 127, 130, 134, 134, 141, 154, 174, 180, 180, 186, 192,
        193, 194, 198, 239, 258, 269, 285, 307, 308, 313, 327, 329, 332, 334,
        341, 341, 361, 373, 375, 381, 390, 401, 405, 414, 426, 428, 436, 441,
        458, 464, 467, 474, 478, 481, 492, 500, 528, 530, 535, 538, 550, 559,
        568, 580, 588, 596, 597, 604, 604, 608, 613, 624, 629, 634, 648, 652,
        668, 670, 670, 670, 683, 686, 688, 693, 704, 705, 705, 707, 712, 718,
        721, 732, 743, 753, 757, 768, 776, 780, 782, 797, 800, 801, 807, 810,
        816, 826, 836, 854, 856, 858, 863, 868, 888, 889, 896, 897, 898, 899,
        900, 901, 902, 903, 904, 905, 913, 917, 946, 958, 987, 2094, 2112,
        2133, 2146, 2146, 2150, 2164, 2214, 2249, 2314, 2323, 2371, 2395, 2423,
        2426, 2472, 2486, 2527, 2561, 2565, 2569, 2584, 2693, 2710, 2715, 2802,
        2803, 2845, 2854, 2874, 2933, 2944, 2952 };
    System.out.println("Running NotDocIdSetSkip test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    ArrayList<Integer> intSet = new ArrayList<Integer>();
    P4DDocIdSet docSet = new P4DDocIdSet(batch);
    randomizer = 0;
    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        intSet.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
      Collections.sort(intSet);

    }
    for (int i = 0; i < intSet.size(); i++) {
      // intSet.add(set[i]);
      docSet.addDoc(intSet.get(i));
    }

    DocIdSetIterator dcit = new NotDocIdSet(docSet, max).iterator();

    for (int i = 0; i < 200000; i += 61) {
      try {
        int doc;
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          // System.out.println("Target:"+i+" Found:"+dcit.doc());
          if (intSet.contains(doc)) {
            System.err.println("Error..." + doc);
            System.out.flush();
          }

          dcit.nextDoc();
          dcit.nextDoc();
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }
    System.out.println("Not Skip test finished");

  }

  private static void testNotDocIdSet(int batch, int length, int max)
      throws IOException {
    System.out.println("Running NotDocIdSet test");
    System.out.println("----------------------------");
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    ArrayList<Integer> intSet = new ArrayList<Integer>();
    P4DDocIdSet docSet = new P4DDocIdSet(batch);
    randomizer = 0;
    for (int i = 1; i < length + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        intSet.add(randomizer + (int) (random.nextDouble() * 1000));
      }

      randomizer += 1000;
      Collections.sort(intSet);

    }
    for (Integer c : intSet) {
      docSet.addDoc(c);
    }

    DocIdSetIterator oit = new NotDocIdSet(docSet, max).iterator();
    System.out.println(intSet);
    int doc;
    while ((doc=oit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {

      // System.out.println(oit.doc());
      if (intSet.contains(doc)) {
        System.err.println("Error..." + doc);
        System.out.flush();
      }
    }
    System.out.println("Not Function performed");

  }

  private static void testCombinationSetOperation(int batch, int length)
      throws IOException {
    System.out.println("Running AndDocIdSet Skip sanity test");
    System.out.println("----------------------------");
    int set1[] = { 8, 27, 30, 35, 53, 59, 71, 74, 87, 92, 104, 113, 122, 126,
        135, 135, 137, 138, 185, 186, 188, 192, 197, 227, 227, 230, 242, 252,
        255, 259, 267, 270, 271, 289, 298, 305, 311, 312, 325, 335, 337, 346,
        351, 360, 365, 371, 375, 380, 387, 391, 406, 407, 408, 419, 425, 430,
        443, 451, 454, 456, 464, 466, 469, 473, 478, 483, 496, 502, 517, 527,
        531, 578, 601, 605, 625, 626, 632, 638, 641, 648, 652, 653, 667, 677,
        682, 697, 700, 711, 713, 733, 764, 780, 782, 796, 798, 801, 804, 812,
        817, 831, 835, 849, 859, 872, 886, 891, 893, 895, 903, 908, 914, 915,
        916, 917, 920, 921, 926, 944, 947, 950, 956, 962, 964, 969, 979, 986,
        994, 996, 1018, 1019, 1022, 1025, 1029, 1029, 1039, 1058, 1062, 1063,
        1064, 1068, 1069, 1071, 1075, 1082, 1085, 1096, 1098, 1102, 1103, 1104,
        1104, 1119, 1120, 1122, 1122, 1123, 1147, 1149, 1179, 1183, 1195, 1197,
        1200, 1201, 1214, 1215, 1220, 1221, 1221, 1225, 1229, 1252, 1260, 1261,
        1268, 1269, 1274, 1279, 1293, 1336, 1336, 1348, 1369, 1370, 1375, 1394,
        1401, 1414, 1444, 1453, 1459, 1468, 1473, 1473, 1474, 1485, 1502, 1505,
        1506, 1517, 1518, 1520, 1521, 1522, 1528, 1537, 1543, 1549, 1550, 1560,
        1565, 1566, 1585, 1599, 1604, 1619, 1637, 1650, 1658, 1679, 1684, 1691,
        1691, 1701, 1701, 1715, 1719, 1720, 1722, 1740, 1740, 1748, 1752, 1756,
        1756, 1776, 1796, 1799, 1799, 1800, 1809, 1811, 1828, 1829, 1849, 1859,
        1865, 1868, 1886, 1900, 1933, 1955, 1959, 1983, 1985, 1999, 2003, 2003,
        2029, 2038, 2048, 2050, 2054, 2056, 2059, 2060, 2079, 2095, 2099, 2104,
        2111, 2113, 2119, 2119, 2122, 2123, 2141, 2142, 2145, 2148, 2160, 2182,
        2183, 2200, 2203, 2209, 2210, 2221, 2232, 2261, 2267, 2268, 2272, 2283,
        2297, 2298, 2313, 2314, 2316, 2316, 2331, 2332, 2338, 2343, 2345, 2350,
        2350, 2365, 2378, 2384, 2392, 2399, 2414, 2420, 2425, 2433, 2445, 2457,
        2461, 2462, 2463, 2497, 2503, 2519, 2522, 2533, 2556, 2568, 2577, 2578,
        2578, 2585, 2589, 2603, 2603, 2613, 2616, 2648, 2651, 2662, 2666, 2667,
        2672, 2675, 2679, 2691, 2694, 2694, 2699, 2706, 2708, 2709, 2711, 2711,
        2732, 2736, 2738, 2749, 2750, 2763, 2764, 2770, 2775, 2781, 2793, 2811,
        2817, 2834, 2842, 2847, 2848, 2852, 2856, 2870, 2872, 2876, 2879, 2887,
        2897, 2903, 2980, 2984, 2994, 2997 };
    int set2[] = { 7, 21, 29, 31, 35, 37, 62, 64, 67, 72, 77, 88, 90, 96, 98,
        116, 152, 154, 156, 162, 163, 173, 179, 188, 189, 201, 203, 217, 224,
        233, 263, 267, 271, 277, 294, 301, 311, 336, 343, 349, 390, 395, 396,
        401, 407, 411, 414, 425, 432, 436, 444, 468, 476, 483, 492, 496, 497,
        501, 508, 513, 517, 519, 531, 541, 543, 552, 555, 555, 568, 571, 587,
        589, 594, 601, 604, 606, 625, 633, 634, 645, 649, 654, 655, 662, 664,
        665, 666, 671, 671, 678, 690, 693, 697, 708, 714, 723, 726, 743, 746,
        747, 772, 784, 806, 811, 812, 824, 834, 836, 844, 850, 863, 867, 890,
        890, 896, 905, 931, 933, 934, 940, 952, 959, 963, 968, 974, 978, 997,
        997, 1013, 1015, 1019, 1023, 1030, 1033, 1035, 1047, 1048, 1054, 1069,
        1087, 1147, 1156, 1158, 1165, 1175, 1199, 1211, 1224, 1252, 1255, 1256,
        1259, 1274, 1280, 1283, 1290, 1292, 1292, 1294, 1297, 1299, 1300, 1301,
        1312, 1323, 1337, 1340, 1351, 1352, 1356, 1363, 1385, 1392, 1395, 1399,
        1409, 1413, 1429, 1437, 1460, 1461, 1465, 1466, 1468, 1482, 1497, 1500,
        1501, 1508, 1517, 1524, 1524, 1529, 1530, 1538, 1538, 1544, 1545, 1552,
        1556, 1561, 1566, 1569, 1583, 1598, 1606, 1610, 1613, 1634, 1642, 1643,
        1656, 1675, 1682, 1704, 1708, 1711, 1711, 1719, 1724, 1736, 1740, 1741,
        1766, 1772, 1774, 1777, 1784, 1793, 1814, 1829, 1833, 1843, 1856, 1857,
        1870, 1874, 1879, 1884, 1886, 1890, 1901, 1909, 1912, 1940, 1944, 1946,
        1947, 1948, 1955, 1962, 1971, 1982, 1989, 1995, 1997, 2012, 2015, 2021,
        2043, 2046, 2049, 2055, 2064, 2068, 2069, 2083, 2088, 2100, 2117, 2122,
        2126, 2132, 2143, 2148, 2152, 2152, 2153, 2159, 2173, 2176, 2198, 2198,
        2201, 2205, 2206, 2207, 2211, 2222, 2230, 2254, 2256, 2264, 2268, 2317,
        2318, 2319, 2330, 2334, 2344, 2353, 2353, 2354, 2369, 2374, 2376, 2392,
        2402, 2403, 2414, 2417, 2422, 2424, 2435, 2445, 2461, 2475, 2530, 2539,
        2541, 2542, 2565, 2566, 2571, 2572, 2577, 2579, 2581, 2582, 2586, 2592,
        2595, 2600, 2642, 2645, 2645, 2651, 2668, 2676, 2699, 2705, 2705, 2709,
        2715, 2720, 2720, 2736, 2753, 2756, 2761, 2788, 2792, 2793, 2796, 2801,
        2815, 2834, 2842, 2857, 2859, 2859, 2861, 2865, 2869, 2875, 2879, 2884,
        2885, 2895, 2901, 2906, 2912, 2935, 2940, 2957, 2958, 2967, 2969, 2976,
        2978, 2981, 2984, 2994, 2997 };
    int set3[] = { 2994, 2997 };

    P4DDocIdSet pset1 = new P4DDocIdSet(batch);
    MyOpenBitSet pset2 = new MyOpenBitSet();
    P4DDocIdSet pset3 = new P4DDocIdSet(batch);

    for (int i = 0; i < set1.length; i++) {
      pset1.addDoc(set1[i]);
      pset2.set(set2[i]);

    }
    for (int i = 0; i < set3.length; i++) {
      pset3.addDoc(set3[i]);
    }

    ArrayList<DocIdSet> orDocs = new ArrayList<DocIdSet>();
    orDocs.add(pset1);
    orDocs.add(pset2);

    List<DocIdSet> its = new ArrayList<DocIdSet>();
    its.add(new OrDocIdSet(orDocs));
    its.add(pset3);

    AndDocIdSet andSet = new AndDocIdSet(its);
    DocIdSetIterator dcit = andSet.iterator();
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.println(doc);

  }

  private static void testAndDocIdSetSkip(int batch, int length, int all)
      throws IOException {
    System.out.println("Running AndDocIdSet Skip test");
    System.out.println("----------------------------");
    // FileWriter writer = new FileWriter("/Users/abhasin/TestOps.txt");

    ArrayList<DocIdSet> docSets = new ArrayList<DocIdSet>();
    Random random = new Random();
    // Minimum 5 bits
    int b = 5;
    int exceptionCount = 0;
    double logBase2 = Math.log(2);
    int randomizer = 0;

    for (int j = 0; j < all; j++) {
      ArrayList<Integer> intSet = new ArrayList<Integer>();
      P4DDocIdSet docSet = new P4DDocIdSet(batch);
      randomizer = 0;
      for (int i = 1; i < length + 1; i++) {

        int bVal[] = new int[33];
        for (int k = 0; k < batch; k++) {
          intSet.add(randomizer + (int) (random.nextDouble() * 1000));
        }

        randomizer += 1000;
        Collections.sort(intSet);

      }
      for (Integer c : intSet) {
        docSet.addDoc(c);
      }
      // writer.write(intSet.toString());
      // writer.write("\n");
      // System.out.println(intSet);

      docSets.add(docSet);
    }
    // writer.flush();
    // writer.close();
    DocIdSetIterator dcit = new AndDocIdSet(docSets).iterator();

    for (int i = 0; i < 200000; i += 64) {
      try {
        int doc;
        if ((doc=dcit.advance(i))!=DocIdSetIterator.NO_MORE_DOCS) {
          // System.out.println("Target:"+i+" Found:"+dcit.doc());
          System.out.print(doc + ",");
          dcit.nextDoc();
          dcit.nextDoc();

        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.flush();
        System.exit(1);
      }
    }

  }

  private static void testOrDocIdSetSanity() throws IOException {

    System.out.println("Running AndDocIdSet Sanity test");
    System.out.println("----------------------------");

    int set1[] = { 8, 27, 30, 35, 53, 59, 71, 74, 87, 92, 104, 113, 122, 126,
        135, 135, 137, 138, 185, 186, 188, 192, 197, 227, 227, 230, 242, 252,
        255, 259, 267, 270, 271, 289, 298, 305, 311, 312, 325, 335, 337, 346,
        351, 360, 365, 371, 375, 380, 387, 391, 406, 407, 408, 419, 425, 430,
        443, 451, 454, 456, 464, 466, 469, 473, 478, 483, 496, 502, 517, 527,
        531, 578, 601, 605, 625, 626, 632, 638, 641, 648, 652, 653, 667, 677,
        682, 697, 700, 711, 713, 733, 764, 780, 782, 796, 798, 801, 804, 812,
        817, 831, 835, 849, 859, 872, 886, 891, 893, 895, 903, 908, 914, 915,
        916, 917, 920, 921, 926, 944, 947, 950, 956, 962, 964, 969, 979, 986,
        994, 996, 1018, 1019, 1022, 1025, 1029, 1029, 1039, 1058, 1062, 1063,
        1064, 1068, 1069, 1071, 1075, 1082, 1085, 1096, 1098, 1102, 1103, 1104,
        1104, 1119, 1120, 1122, 1122, 1123, 1147, 1149, 1179, 1183, 1195, 1197,
        1200, 1201, 1214, 1215, 1220, 1221, 1221, 1225, 1229, 1252, 1260, 1261,
        1268, 1269, 1274, 1279, 1293, 1336, 1336, 1348, 1369, 1370, 1375, 1394,
        1401, 1414, 1444, 1453, 1459, 1468, 1473, 1473, 1474, 1485, 1502, 1505,
        1506, 1517, 1518, 1520, 1521, 1522, 1528, 1537, 1543, 1549, 1550, 1560,
        1565, 1566, 1585, 1599, 1604, 1619, 1637, 1650, 1658, 1679, 1684, 1691,
        1691, 1701, 1701, 1715, 1719, 1720, 1722, 1740, 1740, 1748, 1752, 1756,
        1756, 1776, 1796, 1799, 1799, 1800, 1809, 1811, 1828, 1829, 1849, 1859,
        1865, 1868, 1886, 1900, 1933, 1955, 1959, 1983, 1985, 1999, 2003, 2003,
        2029, 2038, 2048, 2050, 2054, 2056, 2059, 2060, 2079, 2095, 2099, 2104,
        2111, 2113, 2119, 2119, 2122, 2123, 2141, 2142, 2145, 2148, 2160, 2182,
        2183, 2200, 2203, 2209, 2210, 2221, 2232, 2261, 2267, 2268, 2272, 2283,
        2297, 2298, 2313, 2314, 2316, 2316, 2331, 2332, 2338, 2343, 2345, 2350,
        2350, 2365, 2378, 2384, 2392, 2399, 2414, 2420, 2425, 2433, 2445, 2457,
        2461, 2462, 2463, 2497, 2503, 2519, 2522, 2533, 2556, 2568, 2577, 2578,
        2578, 2585, 2589, 2603, 2603, 2613, 2616, 2648, 2651, 2662, 2666, 2667,
        2672, 2675, 2679, 2691, 2694, 2694, 2699, 2706, 2708, 2709, 2711, 2711,
        2732, 2736, 2738, 2749, 2750, 2763, 2764, 2770, 2775, 2781, 2793, 2811,
        2817, 2834, 2842, 2847, 2848, 2852, 2856, 2870, 2872, 2876, 2879, 2887,
        2897, 2903, 2980, 2984, 2986, 2997 };
    int set2[] = { 7, 21, 29, 31, 35, 37, 62, 64, 67, 72, 77, 88, 90, 96, 98,
        116, 152, 154, 156, 162, 163, 173, 179, 188, 189, 201, 203, 217, 224,
        233, 263, 267, 271, 277, 294, 301, 311, 336, 343, 349, 390, 395, 396,
        401, 407, 411, 414, 425, 432, 436, 444, 468, 476, 483, 492, 496, 497,
        501, 508, 513, 517, 519, 531, 541, 543, 552, 555, 555, 568, 571, 587,
        589, 594, 601, 604, 606, 625, 633, 634, 645, 649, 654, 655, 662, 664,
        665, 666, 671, 671, 678, 690, 693, 697, 708, 714, 723, 726, 743, 746,
        747, 772, 784, 806, 811, 812, 824, 834, 836, 844, 850, 863, 867, 890,
        890, 896, 905, 931, 933, 934, 940, 952, 959, 963, 968, 974, 978, 997,
        997, 1013, 1015, 1019, 1023, 1030, 1033, 1035, 1047, 1048, 1054, 1069,
        1087, 1147, 1156, 1158, 1165, 1175, 1199, 1211, 1224, 1252, 1255, 1256,
        1259, 1274, 1280, 1283, 1290, 1292, 1292, 1294, 1297, 1299, 1300, 1301,
        1312, 1323, 1337, 1340, 1351, 1352, 1356, 1363, 1385, 1392, 1395, 1399,
        1409, 1413, 1429, 1437, 1460, 1461, 1465, 1466, 1468, 1482, 1497, 1500,
        1501, 1508, 1517, 1524, 1524, 1529, 1530, 1538, 1538, 1544, 1545, 1552,
        1556, 1561, 1566, 1569, 1583, 1598, 1606, 1610, 1613, 1634, 1642, 1643,
        1656, 1675, 1682, 1704, 1708, 1711, 1711, 1719, 1724, 1736, 1740, 1741,
        1766, 1772, 1774, 1777, 1784, 1793, 1814, 1829, 1833, 1843, 1856, 1857,
        1870, 1874, 1879, 1884, 1886, 1890, 1901, 1909, 1912, 1940, 1944, 1946,
        1947, 1948, 1955, 1962, 1971, 1982, 1989, 1995, 1997, 2012, 2015, 2021,
        2043, 2046, 2049, 2055, 2064, 2068, 2069, 2083, 2088, 2100, 2117, 2122,
        2126, 2132, 2143, 2148, 2152, 2152, 2153, 2159, 2173, 2176, 2198, 2198,
        2201, 2205, 2206, 2207, 2211, 2222, 2230, 2254, 2256, 2264, 2268, 2317,
        2318, 2319, 2330, 2334, 2344, 2353, 2353, 2354, 2369, 2374, 2376, 2392,
        2402, 2403, 2414, 2417, 2422, 2424, 2435, 2445, 2461, 2475, 2530, 2539,
        2541, 2542, 2565, 2566, 2571, 2572, 2577, 2579, 2581, 2582, 2586, 2592,
        2595, 2600, 2642, 2645, 2645, 2651, 2668, 2676, 2699, 2705, 2705, 2709,
        2715, 2720, 2720, 2736, 2753, 2756, 2761, 2788, 2792, 2793, 2796, 2801,
        2815, 2834, 2842, 2857, 2859, 2859, 2861, 2865, 2869, 2875, 2879, 2884,
        2885, 2895, 2901, 2906, 2912, 2935, 2940, 2957, 2958, 2967, 2969, 2976,
        2978, 2981, 2984, 2990, 2991 };
    int set3[] = { 16, 22, 56, 70, 70, 86, 88, 102, 104, 108, 112, 124, 130,
        130, 143, 156, 162, 174, 177, 182, 183, 186, 197, 206, 217, 224, 234,
        238, 242, 245, 246, 259, 275, 278, 288, 289, 295, 301, 313, 316, 358,
        361, 366, 379, 386, 405, 432, 446, 457, 460, 467, 473, 474, 475, 475,
        491, 516, 539, 540, 551, 568, 579, 588, 593, 594, 598, 607, 619, 625,
        634, 636, 649, 670, 671, 677, 682, 690, 693, 713, 718, 723, 724, 730,
        732, 738, 746, 774, 777, 778, 811, 812, 821, 825, 825, 828, 837, 840,
        841, 847, 859, 863, 877, 878, 880, 880, 898, 901, 901, 909, 926, 931,
        931, 932, 941, 957, 961, 964, 980, 981, 982, 984, 989, 993, 996, 998,
        998, 999, 999, 1004, 1006, 1006, 1012, 1013, 1016, 1047, 1050, 1068,
        1069, 1076, 1076, 1080, 1081, 1088, 1092, 1105, 1106, 1109, 1110, 1111,
        1128, 1136, 1137, 1138, 1144, 1144, 1145, 1149, 1152, 1161, 1162, 1163,
        1171, 1177, 1178, 1201, 1204, 1252, 1253, 1263, 1266, 1275, 1279, 1290,
        1303, 1313, 1314, 1314, 1324, 1326, 1326, 1336, 1343, 1346, 1358, 1366,
        1376, 1426, 1439, 1441, 1445, 1456, 1460, 1460, 1463, 1464, 1466, 1467,
        1473, 1481, 1482, 1485, 1487, 1497, 1498, 1523, 1550, 1558, 1568, 1574,
        1581, 1585, 1591, 1592, 1606, 1611, 1619, 1622, 1634, 1636, 1644, 1648,
        1658, 1684, 1685, 1686, 1702, 1711, 1717, 1730, 1747, 1762, 1763, 1766,
        1812, 1826, 1835, 1851, 1855, 1858, 1864, 1865, 1867, 1881, 1886, 1933,
        1937, 1943, 1954, 1966, 1972, 1976, 1980, 1985, 1986, 1991, 1996, 2001,
        2019, 2026, 2032, 2041, 2061, 2069, 2077, 2078, 2082, 2083, 2089, 2098,
        2107, 2114, 2142, 2157, 2159, 2171, 2186, 2189, 2199, 2200, 2201, 2207,
        2212, 2219, 2221, 2236, 2243, 2251, 2256, 2260, 2265, 2265, 2275, 2277,
        2281, 2300, 2308, 2311, 2321, 2325, 2334, 2341, 2346, 2371, 2379, 2380,
        2383, 2397, 2399, 2401, 2404, 2407, 2411, 2450, 2482, 2499, 2505, 2514,
        2531, 2538, 2542, 2544, 2552, 2554, 2557, 2557, 2559, 2561, 2583, 2586,
        2600, 2620, 2622, 2625, 2626, 2632, 2641, 2649, 2649, 2649, 2658, 2661,
        2668, 2675, 2676, 2681, 2692, 2698, 2712, 2716, 2719, 2737, 2764, 2780,
        2781, 2790, 2791, 2793, 2801, 2802, 2807, 2809, 2814, 2815, 2855, 2855,
        2863, 2870, 2878, 2889, 2894, 2900, 2905, 2905, 2920, 2923, 2924, 2935,
        2951, 2952, 2956, 2971, 2983, 2984, 2997 };

    // set1 = new int[]{0,2,4,6,8,10};
    // set2 = new int[]{0,1,3,5,7,10};
    // set3 = new int[]{0,1,2,4,5,10};

    P4DDocIdSet pset1 = new P4DDocIdSet(128);
    P4DDocIdSet pset2 = new P4DDocIdSet(128);
    P4DDocIdSet pset3 = new P4DDocIdSet(128);

    for (int i = 0; i < set1.length; i++) {
      pset1.addDoc(set1[i]);
      pset2.addDoc(set2[i]);
      pset3.addDoc(set3[i]);
    }

    List<DocIdSet> its = new ArrayList<DocIdSet>();
    its.add(pset1);
    its.add(pset2);
    its.add(pset3);

    OrDocIdSet orSet = new OrDocIdSet(its);
    DocIdSetIterator dcit = orSet.iterator();
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.println(doc);

  }

  private static void testAndDocIdSetSkipSanity(int batch) throws IOException {

    System.out.println("Running AndDocIdSet Skip sanity test");
    System.out.println("----------------------------");
    int set1[] = { 8, 27, 30, 35, 53, 59, 71, 74, 87, 92, 104, 113, 122, 126,
        135, 135, 137, 138, 185, 186, 188, 192, 197, 227, 227, 230, 242, 252,
        255, 259, 267, 270, 271, 289, 298, 305, 311, 312, 325, 335, 337, 346,
        351, 360, 365, 371, 375, 380, 387, 391, 406, 407, 408, 419, 425, 430,
        443, 451, 454, 456, 464, 466, 469, 473, 478, 483, 496, 502, 517, 527,
        531, 578, 601, 605, 625, 626, 632, 638, 641, 648, 652, 653, 667, 677,
        682, 697, 700, 711, 713, 733, 764, 780, 782, 796, 798, 801, 804, 812,
        817, 831, 835, 849, 859, 872, 886, 891, 893, 895, 903, 908, 914, 915,
        916, 917, 920, 921, 926, 944, 947, 950, 956, 962, 964, 969, 979, 986,
        994, 996, 1018, 1019, 1022, 1025, 1029, 1029, 1039, 1058, 1062, 1063,
        1064, 1068, 1069, 1071, 1075, 1082, 1085, 1096, 1098, 1102, 1103, 1104,
        1104, 1119, 1120, 1122, 1122, 1123, 1147, 1149, 1179, 1183, 1195, 1197,
        1200, 1201, 1214, 1215, 1220, 1221, 1221, 1225, 1229, 1252, 1260, 1261,
        1268, 1269, 1274, 1279, 1293, 1336, 1336, 1348, 1369, 1370, 1375, 1394,
        1401, 1414, 1444, 1453, 1459, 1468, 1473, 1473, 1474, 1485, 1502, 1505,
        1506, 1517, 1518, 1520, 1521, 1522, 1528, 1537, 1543, 1549, 1550, 1560,
        1565, 1566, 1585, 1599, 1604, 1619, 1637, 1650, 1658, 1679, 1684, 1691,
        1691, 1701, 1701, 1715, 1719, 1720, 1722, 1740, 1740, 1748, 1752, 1756,
        1756, 1776, 1796, 1799, 1799, 1800, 1809, 1811, 1828, 1829, 1849, 1859,
        1865, 1868, 1886, 1900, 1933, 1955, 1959, 1983, 1985, 1999, 2003, 2003,
        2029, 2038, 2048, 2050, 2054, 2056, 2059, 2060, 2079, 2095, 2099, 2104,
        2111, 2113, 2119, 2119, 2122, 2123, 2141, 2142, 2145, 2148, 2160, 2182,
        2183, 2200, 2203, 2209, 2210, 2221, 2232, 2261, 2267, 2268, 2272, 2283,
        2297, 2298, 2313, 2314, 2316, 2316, 2331, 2332, 2338, 2343, 2345, 2350,
        2350, 2365, 2378, 2384, 2392, 2399, 2414, 2420, 2425, 2433, 2445, 2457,
        2461, 2462, 2463, 2497, 2503, 2519, 2522, 2533, 2556, 2568, 2577, 2578,
        2578, 2585, 2589, 2603, 2603, 2613, 2616, 2648, 2651, 2662, 2666, 2667,
        2672, 2675, 2679, 2691, 2694, 2694, 2699, 2706, 2708, 2709, 2711, 2711,
        2732, 2736, 2738, 2749, 2750, 2763, 2764, 2770, 2775, 2781, 2793, 2811,
        2817, 2834, 2842, 2847, 2848, 2852, 2856, 2870, 2872, 2876, 2879, 2887,
        2897, 2903, 2980, 2984, 2994, 2997 };
    int set2[] = { 7, 21, 29, 31, 35, 37, 62, 64, 67, 72, 77, 88, 90, 96, 98,
        116, 152, 154, 156, 162, 163, 173, 179, 188, 189, 201, 203, 217, 224,
        233, 263, 267, 271, 277, 294, 301, 311, 336, 343, 349, 390, 395, 396,
        401, 407, 411, 414, 425, 432, 436, 444, 468, 476, 483, 492, 496, 497,
        501, 508, 513, 517, 519, 531, 541, 543, 552, 555, 555, 568, 571, 587,
        589, 594, 601, 604, 606, 625, 633, 634, 645, 649, 654, 655, 662, 664,
        665, 666, 671, 671, 678, 690, 693, 697, 708, 714, 723, 726, 743, 746,
        747, 772, 784, 806, 811, 812, 824, 834, 836, 844, 850, 863, 867, 890,
        890, 896, 905, 931, 933, 934, 940, 952, 959, 963, 968, 974, 978, 997,
        997, 1013, 1015, 1019, 1023, 1030, 1033, 1035, 1047, 1048, 1054, 1069,
        1087, 1147, 1156, 1158, 1165, 1175, 1199, 1211, 1224, 1252, 1255, 1256,
        1259, 1274, 1280, 1283, 1290, 1292, 1292, 1294, 1297, 1299, 1300, 1301,
        1312, 1323, 1337, 1340, 1351, 1352, 1356, 1363, 1385, 1392, 1395, 1399,
        1409, 1413, 1429, 1437, 1460, 1461, 1465, 1466, 1468, 1482, 1497, 1500,
        1501, 1508, 1517, 1524, 1524, 1529, 1530, 1538, 1538, 1544, 1545, 1552,
        1556, 1561, 1566, 1569, 1583, 1598, 1606, 1610, 1613, 1634, 1642, 1643,
        1656, 1675, 1682, 1704, 1708, 1711, 1711, 1719, 1724, 1736, 1740, 1741,
        1766, 1772, 1774, 1777, 1784, 1793, 1814, 1829, 1833, 1843, 1856, 1857,
        1870, 1874, 1879, 1884, 1886, 1890, 1901, 1909, 1912, 1940, 1944, 1946,
        1947, 1948, 1955, 1962, 1971, 1982, 1989, 1995, 1997, 2012, 2015, 2021,
        2043, 2046, 2049, 2055, 2064, 2068, 2069, 2083, 2088, 2100, 2117, 2122,
        2126, 2132, 2143, 2148, 2152, 2152, 2153, 2159, 2173, 2176, 2198, 2198,
        2201, 2205, 2206, 2207, 2211, 2222, 2230, 2254, 2256, 2264, 2268, 2317,
        2318, 2319, 2330, 2334, 2344, 2353, 2353, 2354, 2369, 2374, 2376, 2392,
        2402, 2403, 2414, 2417, 2422, 2424, 2435, 2445, 2461, 2475, 2530, 2539,
        2541, 2542, 2565, 2566, 2571, 2572, 2577, 2579, 2581, 2582, 2586, 2592,
        2595, 2600, 2642, 2645, 2645, 2651, 2668, 2676, 2699, 2705, 2705, 2709,
        2715, 2720, 2720, 2736, 2753, 2756, 2761, 2788, 2792, 2793, 2796, 2801,
        2815, 2834, 2842, 2857, 2859, 2859, 2861, 2865, 2869, 2875, 2879, 2884,
        2885, 2895, 2901, 2906, 2912, 2935, 2940, 2957, 2958, 2967, 2969, 2976,
        2978, 2981, 2984, 2994, 2997 };
    int set3[] = { 2994, 2997 };

    P4DDocIdSet pset1 = new P4DDocIdSet(batch);
    MyOpenBitSet pset2 = new MyOpenBitSet();
    P4DDocIdSet pset3 = new P4DDocIdSet(batch);

    for (int i = 0; i < set1.length; i++) {
      pset1.addDoc(set1[i]);
      pset2.set(set2[i]);

    }
    for (int i = 0; i < set3.length; i++) {
      pset3.addDoc(set3[i]);
    }

    List<DocIdSet> its = new ArrayList<DocIdSet>();
    its.add(pset1);
    its.add(pset2);
    its.add(pset3);
    AndDocIdSet andSet = new AndDocIdSet(its);
    DocIdSetIterator dcit = andSet.iterator();
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.println(doc);

  }

  private static void testNotDocIdSetSkipSanity() {
    // TODO Auto-generated method stub
  }

  private static void testOrDocIdSetSkipSanity() {
    // TODO Auto-generated method stub
  }

  private static void testSmallSets() throws IOException {

    System.out.println("Running Small Set test");
    System.out.println("----------------------------");

    MyOpenBitSet s1 = new MyOpenBitSet();
    MyOpenBitSet s2 = new MyOpenBitSet();
    MyOpenBitSet s3 = new MyOpenBitSet();
    MyOpenBitSet s4 = new MyOpenBitSet();

    s1.set(0);
    s1.set(4);
    s1.set(5);
    s1.set(6);

    s2.set(5);
    s2.set(6);

    s3.set(1);
    s3.set(5);

    ArrayList<DocIdSet> docSet = new ArrayList<DocIdSet>();
    docSet.add(s1);
    docSet.add(s2);
    docSet.add(s3);

    AndDocIdSet ord = new AndDocIdSet(docSet);

    int doc;
    for (DocIdSetIterator dcit = ord.iterator(); (doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS;)
      System.out.println(doc);

    System.out.println("-----------");
    s1.set(5);
    s2.set(5);
    s3.set(5);
    s4.set(5);

    AndDocIdSet ard = new AndDocIdSet(docSet);

    for (DocIdSetIterator dcit = ard.iterator(); (doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS;)
      System.out.println(doc);

    s1.set(0);

    DocIdSetIterator nsit = new NotDocIdSet(s1, 5).iterator();

    while ((doc=nsit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.println(doc);

  }

  public static void testCombinationSanity() throws IOException {

    int[] set1 = { 4, 19, 21, 35, 36, 43, 43, 73, 85, 104, 105, 106, 112, 118,
        119, 138, 141, 145, 146, 146, 196, 200, 202, 217, 219, 220, 221, 239,
        242, 243, 261, 276, 280, 281, 295, 297, 306, 309, 319, 324, 359, 375,
        376, 387, 398, 401, 406, 438, 442, 450, 450, 462, 469, 475, 495, 499,
        505, 505, 513, 513, 526, 529, 569, 584, 589, 590, 609, 614, 633, 635,
        635, 644, 646, 650, 657, 682, 685, 688, 692, 699, 704, 712, 714, 733,
        736, 739, 746, 748, 766, 768, 774, 776, 778, 786, 799, 801, 812, 814,
        818, 819, 831, 832, 836, 837, 837, 847, 864, 870, 872, 872, 875, 880,
        885, 899, 905, 914, 918, 928, 931, 932, 952, 954, 971, 981, 983, 986,
        992, 998, 1000, 1031, 1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093,
        1100, 1100, 1107, 1109, 1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167,
        1176, 1194, 1200, 1209, 1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246,
        1252, 1277, 1309, 1322, 1325, 1327, 1327, 1329, 1341, 1341, 1342, 1352,
        1359, 1360, 1361, 1363, 1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438,
        1441, 1448, 1449, 1451, 1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505,
        1546, 1555, 1556, 1572, 1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630,
        1638, 1652, 1663, 1664, 1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703,
        1707, 1708, 1708, 1716, 1720, 1720, 1723, 1724, 1727, 1727, 1730, 1733,
        1735, 1738, 1750, 1755, 1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830,
        1848, 1854, 1871, 1888, 1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set2 = { 4, 105, 141, 633, 1953, 1962, 1983, 1990, 1999 };
    int[] set3 = { 4, 145, 146, 146, 196, 200, 202, 217, 219, 1999 };
    int[] set4 = { 4, 200, 202, 217, 219, 220, 221, 239, 242, 243, 261, 276,
        280, 281, 295, 297, 306, 309, 319, 324, 359, 375, 376, 387, 398, 401,
        406, 438, 442, 450, 450, 462, 469, 475, 495, 499, 505, 505, 513, 513,
        526, 529, 569, 584, 589, 590, 609, 614, 633, 635, 635, 644, 646, 650,
        657, 682, 685, 688, 692, 699, 704, 712, 714, 733, 736, 739, 746, 748,
        766, 768, 774, 776, 778, 786, 799, 801, 812, 814, 818, 819, 831, 832,
        836, 837, 837, 847, 864, 870, 872, 872, 875, 880, 885, 899, 905, 914,
        918, 928, 931, 932, 952, 954, 971, 981, 983, 986, 992, 998, 1000, 1031,
        1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093, 1100, 1100, 1107, 1109,
        1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167, 1176, 1194, 1200, 1209,
        1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246, 1252, 1277, 1309, 1322,
        1325, 1327, 1327, 1329, 1341, 1341, 1342, 1352, 1359, 1360, 1361, 1363,
        1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438, 1441, 1448, 1449, 1451,
        1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505, 1546, 1555, 1556, 1572,
        1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630, 1638, 1652, 1663, 1664,
        1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703, 1707, 1708, 1708, 1716,
        1720, 1720, 1723, 1724, 1727, 1727, 1730, 1733, 1735, 1738, 1750, 1755,
        1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830, 1848, 1854, 1871, 1888,
        1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set5 = { 4, 1999 };
    int[] set6 = { 2000 };

    OpenBitSet ps1 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set1.length; i++)
      ps1.set(set1[i]);

    OpenBitSet ps2 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set2.length; i++)
      ps2.set(set2[i]);

    OpenBitSet ps3 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set3.length; i++)
      ps3.set(set3[i]);

    P4DDocIdSet ps4 = new P4DDocIdSet(128);

    // Build open bit set
    for (int i = 0; i < set4.length; i++)
      ps4.addDoc(set4[i]);

    OpenBitSet ps5 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set5.length; i++)
      ps5.set(set5[i]);

    P4DDocIdSet ps6 = new P4DDocIdSet(128);
    ps6.addDoc(2000);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    System.out.println("Running Combination Sanity test CASE 1");
    System.out
        .println("TEST CASE : Or first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");

    OrDocIdSet ord = new OrDocIdSet(sets);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps5);

    AndDocIdSet and = new AndDocIdSet(sets2);

    DocIdSetIterator andit = and.iterator();

    int index = 0;
    int doc;
    while ((doc=andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      if (set5[index++] != doc)
        System.err.println("Error in combination test: expected - "
            + set5[index - 1] + " but was - " + doc);
    }

    if (index != set5.length)
      System.err
          .println("Error: could not recover all and elements: expected length-"
              + set5.length + " but was -" + index);

    System.out.println("Combination sanity complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 2");
    System.out
        .println("TEST CASE : AND first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");

    AndDocIdSet and1 = new AndDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(and1);
    sets2.add(ps5);

    AndDocIdSet and2 = new AndDocIdSet(sets2);

    andit = and2.iterator();

    index = 0;
    while ((doc=andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      if (set5[index++] != doc)
        System.err.println("Error in combination test: expected - "
            + set5[index - 1] + " but was - " + doc);
    }

    if (index != set5.length)
      System.err
          .println("Error: could not recover all and elements: expected length-"
              + set5.length + " but was -" + index);

    System.out.println("Combination sanity CASE 2 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 3");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st should recover set1");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or3 = new OrDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);

    OrDocIdSet or4 = new OrDocIdSet(sets2);

    DocIdSetIterator orit = or4.iterator();

    index = 0;
    int ctr = 0;
    while ((doc=orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      index = ps1.nextSetBit(index);
      if (index == -1)
        System.err
            .println("Error in combination test: no value expected  but was - "
                + doc);
      else if (index != doc)
        System.err.println("Error in combination test: expected - "
            + set1[index - 1] + " but was - " + doc);
      index++;
      ctr++;
    }

    if (ctr != ps1.cardinality())
      System.err
          .println("Error: could not recover all and elements: expected length-"
              + ctr + " but was -" + ps1.cardinality());

    System.out.println("Combination sanity CASE 3 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 4");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st and ~{2000} should recover 0-1999");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or5 = new OrDocIdSet(sets);
    NotDocIdSet not = new NotDocIdSet(ps6, 2001);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);
    sets2.add(not);

    OrDocIdSet or6 = new OrDocIdSet(sets2);

    orit = or6.iterator();

    index = 0;
    ctr = 0;

    while ((doc=orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      if (index != doc)
        System.err.println("Error in combination test: expected - " + index
            + " but was - " + doc);
      index++;

    }

    if (index != set6[0])
      System.err
          .println("Error: could not recover all and elements: expected length-"
              + set6[0] + " but was -" + index);

    System.out.println("Combination sanity CASE 4 complete.");
    System.out.println();

  }

  public static void testBoboFailureCaseSmall() throws IOException {

    System.out.println("Running BOBO Small Test case...");
    System.out.println("----------------------------");

    OpenBitSet bs1 = new OpenBitSet();
    OpenBitSet bs2 = new OpenBitSet();
    OpenBitSet bs3 = new OpenBitSet();

    bs1.set(858);
    bs2.set(857);
    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(bs1);
    sets.add(bs2);
    OrDocIdSet ord = new OrDocIdSet(sets);
    bs3.set(857);
    bs3.set(858);
    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(bs3);

    AndDocIdSet and = new AndDocIdSet(sets2);
    DocIdSetIterator andit = and.iterator();
    int doc;
    while ((doc=andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
      System.out.println(doc);
    }

  }

  public static void testBoboFailureCase() throws IOException {

    System.out.println("Running BOBO Test case...");
    System.out.println("----------------------------");

    int set0[] = { 9, 20, 31, 42, 65, 76, 87, 108, 119, 130, 141, 152, 163,
        186, 197, 208, 219, 230, 241, 265, 276, 287, 298, 309, 332, 343, 354,
        365, 376, 387, 410, 421, 431, 442, 453, 476, 487, 498, 509, 520, 531,
        554, 565, 575, 586, 597, 608, 619, 630, 653, 664, 675, 686, 697, 708,
        717, 728, 739, 750, 773, 784, 814, 820, 831, 842, 853 };
    int set1[] = { 8, 19, 30, 53, 64, 75, 86, 96, 107, 118, 129, 140, 151, 174,
        185, 196, 207, 218, 229, 252, 264, 275, 286, 297, 320, 331, 342, 353,
        364, 375, 398, 409, 420, 430, 441, 464, 475, 486, 497, 508, 519, 542,
        553, 564, 574, 585, 596, 607, 618, 641, 652, 663, 674, 685, 696, 716,
        727, 738, 761, 772, 783, 802, 813, 819, 830, 841 };
    int set2[] = { 7, 41, 52, 63, 74, 85, 106, 117, 128, 139, 162, 173, 184,
        195, 206, 217, 240, 251, 263, 274, 285, 308, 319, 330, 341, 352, 363,
        386, 397, 408, 419, 429, 452, 463, 474, 485, 496, 507, 530, 541, 552,
        563, 573, 584, 595, 606, 629, 640, 651, 662, 673, 684, 707, 715, 726,
        749, 760, 771, 782, 791, 801, 812, 818, 829, 852, 858 };
    int set3[] = { 6, 29, 40, 51, 62, 73, 84, 105, 116, 127, 150, 161, 172,
        183, 194, 205, 228, 239, 250, 262, 273, 296, 307, 318, 329, 340, 351,
        374, 385, 396, 407, 418, 440, 451, 462, 473, 484, 495, 518, 529, 540,
        551, 562, 572, 583, 594, 617, 628, 639, 650, 661, 672, 695, 706, 714,
        737, 748, 759, 770, 781, 790, 793, 800, 811, 840, 851 };
    int set4[] = { 17, 28, 39, 50, 61, 72, 95, 104, 115, 138, 149, 160, 171,
        182, 193, 216, 227, 238, 249, 260, 261, 284, 295, 306, 317, 328, 339,
        362, 373, 384, 395, 406, 417, 439, 450, 461, 472, 483, 506, 517, 528,
        539, 550, 561, 582, 605, 616, 627, 638, 649, 660, 683, 694, 705, 725,
        736, 747, 758, 769, 780, 789, 799, 810, 828, 839, 850 };
    int set5[] = { 5, 16, 27, 38, 49, 60, 83, 94, 103, 126, 137, 148, 159, 170,
        181, 204, 215, 226, 237, 248, 259, 272, 283, 294, 305, 316, 327, 350,
        361, 372, 383, 394, 405, 428, 438, 449, 460, 471, 494, 505, 516, 527,
        538, 549, 593, 604, 615, 626, 637, 648, 671, 682, 693, 704, 724, 735,
        746, 757, 768, 788, 792, 798, 809, 827, 838, 849 };
    int set6[] = { 4, 15, 26, 37, 48, 71, 82, 93, 114, 125, 136, 147, 158, 169,
        192, 203, 214, 225, 236, 247, 271, 282, 293, 304, 315, 338, 349, 360,
        371, 382, 393, 416, 427, 437, 448, 459, 482, 493, 504, 515, 526, 537,
        560, 571, 581, 592, 603, 614, 625, 636, 659, 670, 681, 692, 703, 723,
        734, 745, 756, 779, 787, 796, 797, 826, 837, 848 };
    int set7[] = { 3, 14, 25, 36, 59, 70, 81, 92, 102, 113, 124, 135, 146, 157,
        180, 191, 202, 213, 224, 235, 258, 270, 281, 292, 303, 326, 337, 348,
        359, 370, 381, 404, 415, 426, 436, 447, 470, 481, 492, 503, 514, 525,
        548, 559, 570, 580, 591, 602, 613, 624, 647, 658, 669, 680, 691, 702,
        722, 733, 744, 767, 778, 795, 808, 825, 836, 847 };
    int set8[] = { 2, 13, 24, 47, 58, 69, 80, 91, 101, 112, 123, 134, 145, 168,
        179, 190, 201, 212, 223, 246, 257, 269, 280, 291, 314, 325, 336, 347,
        358, 369, 392, 403, 414, 425, 435, 458, 469, 480, 491, 502, 513, 536,
        547, 558, 569, 579, 590, 601, 612, 635, 646, 657, 668, 679, 690, 713,
        721, 732, 755, 766, 777, 786, 794, 807, 824, 835 };
    int set9[] = { 1, 10, 12, 21, 32, 35, 43, 46, 54, 57, 68, 77, 79, 88, 90,
        97, 100, 111, 120, 122, 131, 133, 142, 153, 156, 164, 167, 175, 178,
        189, 198, 200, 209, 211, 220, 231, 234, 242, 245, 253, 256, 266, 268,
        277, 279, 288, 299, 302, 310, 313, 321, 324, 335, 344, 346, 355, 357,
        366, 377, 380, 388, 391, 399, 402, 413, 422, 424, 432, 443, 446, 454,
        457, 465, 468, 479, 488, 490, 499, 501, 510, 521, 524, 532, 535, 543,
        546, 557, 566, 568, 578, 587, 589, 598, 600, 609, 620, 623, 631, 634,
        642, 645, 656, 665, 667, 676, 678, 687, 698, 701, 709, 712, 718, 720,
        729, 740, 743, 751, 754, 762, 765, 776, 785, 803, 806, 817, 821, 823,
        832, 843, 846, 854 };
    int set10[] = { 23, 34, 45, 56, 67, 78, 99, 110, 121, 144, 155, 166, 177,
        188, 199, 222, 233, 244, 255, 267, 290, 301, 312, 323, 334, 345, 368,
        379, 390, 401, 412, 423, 434, 445, 456, 467, 478, 489, 512, 523, 534,
        545, 556, 567, 577, 588, 611, 622, 633, 644, 655, 666, 689, 700, 711,
        731, 742, 753, 764, 775, 805, 816, 834, 845, 856, 857 };
    int set11[] = { 11, 22, 33, 44, 55, 66, 89, 98, 109, 132, 143, 154, 165,
        176, 187, 210, 221, 232, 243, 254, 278, 289, 300, 311, 322, 333, 356,
        367, 378, 389, 400, 411, 433, 444, 455, 466, 477, 500, 511, 522, 533,
        544, 555, 576, 599, 610, 621, 632, 643, 654, 677, 688, 699, 710, 719,
        730, 741, 752, 763, 774, 804, 815, 822, 833, 844, 855 };

    int set12[] = { 857, 858 };

    OpenBitSet ps0 = new OpenBitSet();
    for (int i = 0; i < set0.length; i++)
      ps0.set(set0[i]);

    OpenBitSet ps1 = new OpenBitSet();
    for (int i = 0; i < set1.length; i++)
      ps1.set(set1[i]);

    OpenBitSet ps2 = new OpenBitSet();
    for (int i = 0; i < set2.length; i++)
      ps2.set(set2[i]);

    OpenBitSet ps3 = new OpenBitSet();
    for (int i = 0; i < set3.length; i++)
      ps3.set(set3[i]);

    OpenBitSet ps4 = new OpenBitSet();
    for (int i = 0; i < set4.length; i++)
      ps4.set(set4[i]);

    OpenBitSet ps5 = new OpenBitSet();
    for (int i = 0; i < set5.length; i++)
      ps5.set(set5[i]);

    OpenBitSet ps6 = new OpenBitSet();
    for (int i = 0; i < set6.length; i++)
      ps6.set(set6[i]);

    OpenBitSet ps7 = new OpenBitSet();
    for (int i = 0; i < set7.length; i++)
      ps7.set(set7[i]);

    OpenBitSet ps8 = new OpenBitSet();
    for (int i = 0; i < set8.length; i++)
      ps8.set(set8[i]);

    OpenBitSet ps9 = new OpenBitSet();
    for (int i = 0; i < set9.length; i++)
      ps9.set(set9[i]);

    OpenBitSet ps10 = new OpenBitSet();
    for (int i = 0; i < set10.length; i++)
      ps10.set(set10[i]);

    OpenBitSet ps11 = new OpenBitSet();
    for (int i = 0; i < set11.length; i++)
      ps11.set(set11[i]);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps0);
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);
    sets.add(ps6);
    sets.add(ps7);
    sets.add(ps8);
    sets.add(ps9);
    sets.add(ps10);
    sets.add(ps11);

    OrDocIdSet ord = new OrDocIdSet(sets);
    DocIdSetIterator dcit = ord.iterator();
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.print(doc + ",");
    System.out.println("");

    OpenBitSet ps12 = new OpenBitSet();
    for (int i = 0; i < set12.length; i++)
      ps12.set(set12[i]);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps12);

    AndDocIdSet andSet = new AndDocIdSet(sets2);
    DocIdSetIterator andit = andSet.iterator();

    while ((doc = andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.print(doc + ",");
    System.out.println("");

  }

  public static void testBoboFailureCase2() throws IOException {

    System.out.println("Running BOBO Test case 2...");
    System.out.println("----------------------------");

    int set0[] = { 9, 20, 31, 42, 65, 76, 87, 108, 119, 130, 141, 152, 163,
        186, 197, 208, 219, 230, 241, 265, 276, 287, 298, 309, 332, 343, 354,
        365, 376, 387, 410, 421, 431, 442, 453, 476, 487, 498, 509, 520, 531,
        554, 565, 575, 586, 597, 608, 619, 630, 653, 664, 675, 686, 697, 708,
        717, 728, 739, 750, 773, 784, 814, 820, 831, 842, 853 };
    int set1[] = { 8, 19, 30, 53, 64, 75, 86, 96, 107, 118, 129, 140, 151, 174,
        185, 196, 207, 218, 229, 252, 264, 275, 286, 297, 320, 331, 342, 353,
        364, 375, 398, 409, 420, 430, 441, 464, 475, 486, 497, 508, 519, 542,
        553, 564, 574, 585, 596, 607, 618, 641, 652, 663, 674, 685, 696, 716,
        727, 738, 761, 772, 783, 802, 813, 819, 830, 841 };
    int set2[] = { 7, 18, 41, 52, 63, 74, 85, 106, 117, 128, 139, 162, 173,
        184, 195, 206, 217, 240, 251, 263, 274, 285, 308, 319, 330, 341, 352,
        363, 386, 397, 408, 419, 429, 452, 463, 474, 485, 496, 507, 530, 541,
        552, 563, 573, 584, 595, 606, 629, 640, 651, 662, 673, 684, 707, 715,
        726, 749, 760, 771, 782, 791, 801, 812, 818, 829, 852 };
    int set3[] = { 6, 29, 40, 51, 62, 73, 84, 105, 116, 127, 150, 161, 172,
        183, 194, 205, 228, 239, 250, 262, 273, 296, 307, 318, 329, 340, 351,
        374, 385, 396, 407, 418, 440, 451, 462, 473, 484, 495, 518, 529, 540,
        551, 562, 572, 583, 594, 617, 628, 639, 650, 661, 672, 695, 706, 714,
        737, 748, 759, 770, 781, 790, 793, 800, 811, 840, 851 };
    int set4[] = { 17, 28, 39, 50, 61, 72, 95, 104, 115, 138, 149, 160, 171,
        182, 193, 216, 227, 238, 249, 260, 261, 284, 295, 306, 317, 328, 339,
        362, 373, 384, 395, 406, 417, 439, 450, 461, 472, 483, 506, 517, 528,
        539, 550, 561, 582, 605, 616, 627, 638, 649, 660, 683, 694, 705, 725,
        736, 747, 758, 769, 780, 789, 799, 810, 828, 839, 850 };
    int set5[] = { 5, 16, 27, 38, 49, 60, 83, 94, 103, 126, 137, 148, 159, 170,
        181, 204, 215, 226, 237, 248, 259, 272, 283, 294, 305, 316, 327, 350,
        361, 372, 383, 394, 405, 428, 438, 449, 460, 471, 494, 505, 516, 527,
        538, 549, 593, 604, 615, 626, 637, 648, 671, 682, 693, 704, 724, 735,
        746, 757, 768, 788, 792, 798, 809, 827, 838, 849 };
    int set6[] = { 4, 15, 26, 37, 48, 71, 82, 93, 114, 125, 136, 147, 158, 169,
        192, 203, 214, 225, 236, 247, 271, 282, 293, 304, 315, 338, 349, 360,
        371, 382, 393, 416, 427, 437, 448, 459, 482, 493, 504, 515, 526, 537,
        560, 571, 581, 592, 603, 614, 625, 636, 659, 670, 681, 692, 703, 723,
        734, 745, 756, 779, 787, 796, 797, 826, 837, 848 };
    int set7[] = { 3, 14, 25, 36, 59, 70, 81, 92, 102, 113, 124, 135, 146, 157,
        180, 191, 202, 213, 224, 235, 258, 270, 281, 292, 303, 326, 337, 348,
        359, 370, 381, 404, 415, 426, 436, 447, 470, 481, 492, 503, 514, 525,
        548, 559, 570, 580, 591, 602, 613, 624, 647, 658, 669, 680, 691, 702,
        722, 733, 744, 767, 778, 795, 808, 825, 836, 847 };
    int set8[] = { 2, 13, 24, 47, 58, 69, 80, 91, 101, 112, 123, 134, 145, 168,
        179, 190, 201, 212, 223, 246, 257, 269, 280, 291, 314, 325, 336, 347,
        358, 369, 392, 403, 414, 425, 435, 458, 469, 480, 491, 502, 513, 536,
        547, 558, 569, 579, 590, 601, 612, 635, 646, 657, 668, 679, 690, 713,
        721, 732, 755, 766, 777, 786, 794, 807, 824, 835 };
    int set9[] = { 1, 10, 12, 21, 32, 35, 43, 46, 54, 57, 68, 77, 79, 88, 90,
        97, 100, 111, 120, 122, 131, 133, 142, 153, 156, 164, 167, 175, 178,
        189, 198, 200, 209, 211, 220, 231, 234, 242, 245, 253, 256, 266, 268,
        277, 279, 288, 299, 302, 310, 313, 321, 324, 335, 344, 346, 355, 357,
        366, 377, 380, 388, 391, 399, 402, 413, 422, 424, 432, 443, 446, 454,
        457, 465, 468, 479, 488, 490, 499, 501, 510, 521, 524, 532, 535, 543,
        546, 557, 566, 568, 578, 587, 589, 598, 600, 609, 620, 623, 631, 634,
        642, 645, 656, 665, 667, 676, 678, 687, 698, 701, 709, 712, 718, 720,
        729, 740, 743, 751, 754, 762, 765, 776, 785, 803, 806, 817, 821, 823,
        832, 843, 846, 854 };
    int set10[] = { 23, 34, 45, 56, 67, 78, 99, 110, 121, 144, 155, 166, 177,
        188, 199, 222, 233, 244, 255, 267, 290, 301, 312, 323, 334, 345, 368,
        379, 390, 401, 412, 423, 434, 445, 456, 467, 478, 489, 512, 523, 534,
        545, 556, 567, 577, 588, 611, 622, 633, 644, 655, 666, 689, 700, 711,
        731, 742, 753, 764, 775, 805, 816, 834, 845, 856, 857, 858 };
    int set11[] = { 11, 22, 33, 44, 55, 66, 89, 98, 109, 132, 143, 154, 165,
        176, 187, 210, 221, 232, 243, 254, 278, 289, 300, 311, 322, 333, 356,
        367, 378, 389, 400, 411, 433, 444, 455, 466, 477, 500, 511, 522, 533,
        544, 555, 576, 599, 610, 621, 632, 643, 654, 677, 688, 699, 710, 719,
        730, 741, 752, 763, 774, 804, 815, 822, 833, 844, 855 };
    int set12[] = { 857, 858 };

    OpenBitSet ps0 = new OpenBitSet();
    for (int i = 0; i < set0.length; i++)
      ps0.set(set0[i]);

    OpenBitSet ps1 = new OpenBitSet();
    for (int i = 0; i < set1.length; i++)
      ps1.set(set1[i]);

    OpenBitSet ps2 = new OpenBitSet();
    for (int i = 0; i < set2.length; i++)
      ps2.set(set2[i]);

    OpenBitSet ps3 = new OpenBitSet();
    for (int i = 0; i < set3.length; i++)
      ps3.set(set3[i]);

    OpenBitSet ps4 = new OpenBitSet();
    for (int i = 0; i < set4.length; i++)
      ps4.set(set4[i]);

    OpenBitSet ps5 = new OpenBitSet();
    for (int i = 0; i < set5.length; i++)
      ps5.set(set5[i]);

    OpenBitSet ps6 = new OpenBitSet();
    for (int i = 0; i < set6.length; i++)
      ps6.set(set6[i]);

    OpenBitSet ps7 = new OpenBitSet();
    for (int i = 0; i < set7.length; i++)
      ps7.set(set7[i]);

    OpenBitSet ps8 = new OpenBitSet();
    for (int i = 0; i < set8.length; i++)
      ps8.set(set8[i]);

    P4DDocIdSet ps9 = new P4DDocIdSet(128);
    for (int i = 0; i < set9.length; i++)
      ps9.addDoc(set9[i]);

    OpenBitSet ps10 = new OpenBitSet();
    for (int i = 0; i < set10.length; i++)
      ps10.set(set10[i]);

    OpenBitSet ps11 = new OpenBitSet();
    for (int i = 0; i < set11.length; i++)
      ps11.set(set11[i]);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps0);
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);
    sets.add(ps6);
    sets.add(ps7);
    sets.add(ps8);
    sets.add(ps9);
    sets.add(ps10);
    sets.add(ps11);

    OrDocIdSet ord = new OrDocIdSet(sets);
    DocIdSetIterator dcit = ord.iterator();
    int doc;
    while ((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.print(doc + ",");
    System.out.println("");

    OpenBitSet ps12 = new OpenBitSet();
    for (int i = 0; i < set12.length; i++)
      ps12.set(set12[i]);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps12);

    AndDocIdSet andSet = new AndDocIdSet(sets2);
    DocIdSetIterator andit = andSet.iterator();

    while ((doc = andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      System.out.print(doc+ ",");
    System.out.println("");

  }

  
  /**
   * Test the representation logic for getNetworkInRange
   *
   * @throws Exception
   */
  public static void testOptimizeRepresentation() throws Exception
  {

    Random random = new Random();
    int length[] = {500,5000,10000};
    int batch = 128;
    int randomizer = 0;
    
    System.out.println("Running Test Optimize Representation ...");
    System.out.println("----------------------------");
    

    for(int x=0; x < length.length; x++)
    {
        randomizer = 0;
        int[] network = new int[length[x]*batch];

        for(int i = 0;i<length[x];i++)
        {

              ArrayList<Integer> list = new ArrayList<Integer>();

              for(int k=0;k<batch;k++)
              {
                  list.add(randomizer+(int)(random.nextDouble()*1000));
              }
              Collections.sort(list);
              randomizer+=1000;

              for(int k=0;k<batch;k++)
              {
                network[(i)*batch+k] = list.get(k);
              }

          }

        DocIdSet  docSet = optimizeRepresentation(network, 0, network.length, x==0);
        DocIdSetIterator dcit = docSet.iterator();
        try
        {
            dcit.nextDoc();
            int doc=-1;
            for(int i=0;i<network.length;i++,doc=dcit.nextDoc())
            {
              if (doc==DocIdSetIterator.NO_MORE_DOCS) continue;
              if(network[i]!= doc)
                System.err.println("Error");
            }
        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

    }
  }

  /**
   * Method to perform optimized representation for the nth degree network.
   * 1st degree is always a IntArrayDocIdSet
   * 2nd and 3rd degree representations are picked on HALF_MILLION or SWITCH_RATIO
   *
   *
   * @param network
   * @param offset
   *            in the original array
   * @param size
   *            of the array to be copied
   * @param includeSource
   *            true if the source needs to be included in the set.
   * @return docidset
   */
  public static DocIdSet optimizeRepresentation(int[] network,
                                          int offset,
                                          int size,
                                          boolean includeSource)

  {

    DocIdSet result = null;
    if (size < 500000 || includeSource)
    {
      IntArrayDocIdSet intSet = new IntArrayDocIdSet(size);
      for(int i=0;i<size;i++)
        intSet.addDoc(network[offset+i]);
      result = intSet;
    }
    // If the ratio is greater than Switch ratio
    else if((network[offset+size-1] - network[offset])/size > 10)
    {
        OBSDocIdSet bitSet = new OBSDocIdSet(size);
        for(int i=0;i<size;i++)
          bitSet.addDoc(network[offset+i]);
        result = bitSet;
    }

    // Between Half Mil and switch ratio.
    else
    {
      P4DDocIdSet compSet = new P4DDocIdSet(size);
      for(int i=0;i<size;i++)
        compSet.addDoc(network[offset+i]);
      result = compSet;
    }

    return result;
  }


  private static void testSpellCheckerUsageSet(int density, int length) throws IOException 
  {
   
    Random rng = new Random();
   
    OpenBitSet a = new OpenBitSet(length);
    System.out.println("Starting Iteration..");
    
    OpenBitSet b =   new OpenBitSet(length);
    OpenBitSet c =   new OpenBitSet(length);
    OpenBitSet d =   new OpenBitSet(length);
    OpenBitSet e =   new OpenBitSet(length);
    OpenBitSet f =   new OpenBitSet(length);
    OpenBitSet g =   new OpenBitSet(length);
    OpenBitSet h =   new OpenBitSet(length);
    OpenBitSet i =   new OpenBitSet(length);
    OpenBitSet j =   new OpenBitSet(length);
    OpenBitSet k =   new OpenBitSet(length);
    OpenBitSet z =   new OpenBitSet(length);
    
    
    for (int x = 0; x < density; x++)
    {
      a.fastSet((int)(rng.nextDouble()*length));
      b.fastSet((int)(rng.nextDouble()*length));
      c.fastSet((int)(rng.nextDouble()*length));
      d.fastSet((int)(rng.nextDouble()*length));
      e.fastSet((int)(rng.nextDouble()*length));
      f.fastSet((int)(rng.nextDouble()*length));
      g.fastSet((int)(rng.nextDouble()*length));
      h.fastSet((int)(rng.nextDouble()*length));
      i.fastSet((int)(rng.nextDouble()*length));
      j.fastSet((int)(rng.nextDouble()*length));
      k.fastSet((int)(rng.nextDouble()*length));
      z.fastSet((int)(rng.nextDouble()*length));
      
    }
    System.out.println("Cardinality a:"+ a.cardinality()); 
    System.out.println("Cardinality b:"+ b.cardinality());
    System.out.println("Cardinality c:"+ c.cardinality());
    
    ArrayList<DocIdSet> or1 = new ArrayList<DocIdSet>();
    or1.add(a);
    or1.add(d);
    or1.add(g);
    
    ArrayList<DocIdSet> or2 = new ArrayList<DocIdSet>();
    or2.add(b);
    or2.add(e);
    or2.add(h);
    
    ArrayList<DocIdSet> or3 = new ArrayList<DocIdSet>();
    or3.add(c);
    or3.add(f);
    or3.add(i);
    
    ArrayList<DocIdSet> or4 = new ArrayList<DocIdSet>();
    or4.add(d);
    or4.add(g);
    or4.add(i);
    
    ArrayList<DocIdSet> or5 = new ArrayList<DocIdSet>();
    or5.add(e);
    or5.add(h);
    or5.add(k);
    
    ArrayList<DocIdSet> and6 = new ArrayList<DocIdSet>();
    and6.add(new OrDocIdSet(or1));
    and6.add(new OrDocIdSet(or2));
    and6.add(new OrDocIdSet(or3));
    and6.add(new OrDocIdSet(or4));
    and6.add(new OrDocIdSet(or5));
    
    AndDocIdSet and = new AndDocIdSet(and6);
    DocIdSetIterator dcit = and.iterator();
    
    long nowMillis = System.currentTimeMillis();
    int cnt = 0;
    int doc;
    while((doc=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS){
      cnt ++ ;
    }
      
    nowMillis =  System.currentTimeMillis() - nowMillis;
    System.out.println("Count hit: "+ cnt);
    System.out.println("Time to execute: "+ nowMillis + " ns..");
    
    
    
    
    
  }

  
  public static void main(String args[]) throws IOException {

    for (int i = 0; i < 50; i++) {
      
      testP4DDocIdSetIteratePerf(128,10000);
      
      //testOBSDocIdSet(128, 50, 12000000);
      //testSpellCheckerUsageSet(100000, 14000000);
      /*
       * testP4DDocIdSetIteratePerf(128,50); testP4DDocIdSetSkipPerf(128,50);
       * testP4DDocIdSetCompressionSanity(128,5);
       * testP4DDocIdSetNonBoundaryCompressionSanity(128, 5, 50);
       * testP4DDocIdSetSkipSanity(128, 5);
       * testP4DDocIdSetNonBoundarySkipSanity(128, 1, 32);
       * 
       * testIntArrayDocIdSetIterateSanity(20000);
       * testIntArrayDocIdSetSkipSanity(200);
       * 
       * testCombinationSetOperation(128,10,3);
       * 
       * testOrDocIdSetSkip(128,50,3); testAndDocIdSetSkip(128,3,3);
       * testAndDocIdSet(128,54,3);
       * testNotDocIdSetSkipSanity(128,50,1000000);
       * 
       * testIntArrayDocIdSetIterateSanity(20000);
       * testIntArrayDocIdSetSkipSanity(200);
       *  ;
       
      testOrDocIdSet(128, 1, 3);
      testSmallSets();
      testNotDocIdSet(128, 2, 1000000);
      testAndDocIdSet(128, 1, 2);
      testOrDocIdSetSanity();
      testCombinationSanity();
      testBoboFailureCase();
      testBoboFailureCaseSmall();
      testBoboFailureCase2();

      // testOrDocIdSetSkipSanity(128);
      // testNotDocIdSetSkipSanity(128);
      testAndDocIdSetSkipSanity(128);
      testOptimizeRepresentation();
    */
      
    }

  }



}
