package com.kamikaze.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;

public class PForDeltaTestDocSetSerializationTest extends TestCase {

  private static int batch = 128;

  private static String serial = "src/test/test-data/SerialDocSet";

  public PForDeltaTestDocSetSerializationTest() {

  }


  @Test
  public void testNotDocSetSerialization() throws Exception {

    System.out.println("");
    System.out.println("Running NotDocIdSet Serialization Test case...");
    System.out.println("----------------------------");

    Random random = new Random();

    int randomizer = 0;
    int b = 0;
    int length = 1000;
    int max = 5400;

    Set<Integer> intSet = new TreeSet<Integer>();
    PForDeltaDocIdSet docSet = new PForDeltaDocIdSet(batch);
    randomizer = 0;

    for (int i = 1; i < 1000 + 1; i++) {

      int bVal[] = new int[33];
      for (int k = 0; k < batch; k++) {
        b = randomizer + (int) (random.nextDouble() * 1000);
        intSet.add(b);

      }

      randomizer += 1000;
    }
    for (Integer c : intSet) {
      docSet.addDoc(c);
    }

    DocIdSet not = new NotDocIdSet(docSet, max);

    byte[] serialized = null;
    try {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bout);
      oos.writeObject(not);
      oos.flush();
      oos.close();
      serialized = bout.toByteArray();

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    NotDocIdSet not2 = null;

    try {
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized));
      not2 = (NotDocIdSet) (ois.readObject());
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    org.apache.lucene.search.DocIdSetIterator noit = not.iterator();
    org.apache.lucene.search.DocIdSetIterator noit2 = not2.iterator();

    try {
      int docid;
      while ((docid=noit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        int docid2 = noit2.nextDoc();
        assertFalse(intSet.contains(docid));
        assertEquals(docid, docid2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }


}
