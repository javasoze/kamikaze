package com.kamikaze.lucenecodec;

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

import java.io.IOException;

import org.apache.lucene.codecs.sep.IntIndexInput;
import org.apache.lucene.codecs.sep.IntIndexOutput;
import org.apache.lucene.codecs.sep.IntStreamFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

public class PForDeltaFixedIntBlockWithIntBufferFactory extends IntStreamFactory {
  private final int blockSize;

  /** blockSize is only used when creating the
   *  IntIndexOutput */
  public PForDeltaFixedIntBlockWithIntBufferFactory(int blockSize) {
    this.blockSize = blockSize;
  }

  @Override
  public IntIndexInput openInput(Directory dir, String fileName, IOContext context) throws IOException {
    return new PForDeltaFixedIntBlockWithIntBufferIndexInput(dir, fileName, context);
  }
  

  @Override
  public IntIndexOutput createOutput(Directory dir, String fileName, IOContext context) throws IOException {
    return new PForDeltaFixedIntBlockWithIntBufferIndexOutput(dir, fileName, blockSize, context);
  }
}
