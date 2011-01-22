package com.kamikaze.docidset.utils;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Derived from org.apache.lucene.util.ScorerDocQueue of July 2008 */

import java.io.IOException;
import org.apache.lucene.search.DocIdSetIterator;

/** A DisiDocQueue maintains a partial ordering of its DocIdSetIterators such that the
 *  least DocIdSetIterator (disi) can always be found in constant time.
 *  Put()'s and pop()'s require log(size) time.
 *  The ordering is by DocIdSetIterator.doc().
 */
public final class DisiDocQueue {
  private final HeapedDisiDoc[] heap;
  private final int maxSize;
  private int size;
  
  private static final class HeapedDisiDoc {
    DocIdSetIterator disi;
    int doc;
    
    HeapedDisiDoc(DocIdSetIterator disi) { this(disi, disi.docID()); }
    
    HeapedDisiDoc(DocIdSetIterator disi, int doc) {
      this.disi = disi;
      this.doc = doc;
    }
    
    final void adjust() { doc = disi.docID(); }
  }
  
  private HeapedDisiDoc topHDD; // same as heap[1], only for speed

  /** Create a DisiDocQueue with a maximum size. */
  public DisiDocQueue(int maxSize) {
    // assert maxSize >= 0;
    size = 0;
    int heapSize = maxSize + 1;
    heap = new HeapedDisiDoc[heapSize];
    this.maxSize = maxSize;
    topHDD = heap[1]; // initially null
  }

  /**
   * Adds a Scorer to a ScorerDocQueue in log(size) time.
   * If one tries to add more Scorers than maxSize
   * a RuntimeException (ArrayIndexOutOfBound) is thrown.
   */
  public final void put(DocIdSetIterator disi) {
    size++;
    heap[size] = new HeapedDisiDoc(disi);
    upHeap();
  }

  /**
   * Adds a DocIdSetIterator to the DisiDocQueue in log(size) time if either
   * the DisiDocQueue is not full, or not lessThan(disi, top()).
   * @param disi
   * @return true if DocIdSetIterator is added, false otherwise.
   */
  public final boolean insert(DocIdSetIterator disi){
    if (size < maxSize) {
      put(disi);
      return true;
    } else {
      int docNr = disi.docID();
      if ((size > 0) && (! (docNr < topHDD.doc))) { // heap[1] is top()
        heap[1] = new HeapedDisiDoc(disi, docNr);
        downHeap();
        return true;
      } else {
        return false;
      }
    }
   }

  /** Returns the least DocIdSetIterator of the DisiDocQueue in constant time.
   * Should not be used when the queue is empty.
   */
  public final DocIdSetIterator top() {
    return topHDD.disi;
  }

  /** Returns document number of the least Scorer of the ScorerDocQueue
   * in constant time.
   * Should not be used when the queue is empty.
   */
  public final int topDoc() {
    return topHDD.doc;
  }
  
  public final boolean topNextAndAdjustElsePop() throws IOException {
    return checkAdjustElsePop( topHDD.disi.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
  }

  public final boolean topSkipToAndAdjustElsePop(int target) throws IOException {
    return checkAdjustElsePop( topHDD.disi.advance(target) != DocIdSetIterator.NO_MORE_DOCS);
  }
  
  private final boolean checkAdjustElsePop(boolean cond) {
    if (cond) { // see also adjustTop
      topHDD.doc = topHDD.disi.docID();
    } else { // see also popNoResult
      heap[1] = heap[size]; // move last to first
      heap[size] = null;
      size--;
    }
    downHeap();
    return cond;
  }

  /** Removes and returns the least disi of the DisiDocQueue in log(size)
   * time.
   * Should not be used when the queue is empty.
   */
  public final DocIdSetIterator pop() {
    DocIdSetIterator result = topHDD.disi;
    popNoResult();
    return result;
  }
  
  /** Removes the least disi of the DisiDocQueue in log(size) time.
   * Should not be used when the queue is empty.
   */
  private final void popNoResult() {
    heap[1] = heap[size]; // move last to first
    heap[size] = null;
    size--;
    downHeap();	// adjust heap
  }

  /** Should be called when the disi at top changes doc() value.
   * Still log(n) worst case, but it's at least twice as fast to <pre>
   *  { pq.top().change(); pq.adjustTop(); }
   * </pre> instead of <pre>
   *  { o = pq.pop(); o.change(); pq.push(o); }
   * </pre>
   */
  public final void adjustTop() {
    topHDD.adjust();
    downHeap();
  }

  /** Returns the number of disis currently stored in the DisiDocQueue. */
  public final int size() {
    return size;
  }

  /** Removes all entries from the DisiDocQueue. */
  public final void clear() {
    for (int i = 0; i <= size; i++) {
      heap[i] = null;
    }
    size = 0;
  }

  private final void upHeap() {
    int i = size;
    HeapedDisiDoc node = heap[i];		  // save bottom node
    int j = i >>> 1;
    while ((j > 0) && (node.doc < heap[j].doc)) {
      heap[i] = heap[j];			  // shift parents down
      i = j;
      j = j >>> 1;
    }
    heap[i] = node;				  // install saved node
    topHDD = heap[1];
  }

  private final void downHeap() {
    int i = 1;
    HeapedDisiDoc node = heap[i];	          // save top node
    int j = i << 1;				  // find smaller child
    int k = j + 1;
    if ((k <= size) && (heap[k].doc < heap[j].doc)) {
      j = k;
    }
    while ((j <= size) && (heap[j].doc < node.doc)) {
      heap[i] = heap[j];			  // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && (heap[k].doc < heap[j].doc)) {
	j = k;
      }
    }
    heap[i] = node;				  // install saved node
    topHDD = heap[1];
  }
}
