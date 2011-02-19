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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.pfor2.LCPForDelta;
import org.apache.lucene.index.codecs.intblock.FixedIntBlockIndexInput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class PForDeltaFixedIntBlockWithIntBufferIndexInput extends FixedIntBlockIndexInput {

  public PForDeltaFixedIntBlockWithIntBufferIndexInput(Directory dir, String fileName, int readBufferSize) throws IOException {
    super(dir.openInput(fileName, readBufferSize));
    
  }

  private static class BlockReader implements FixedIntBlockIndexInput.BlockReader {
    private final LCPForDelta decompressor;
    private final IndexInput input;
    private final int[] decompBlock;
    
    private  final ByteBuffer byteCompBuffer;
    private  final IntBuffer intCompBuffer;
    private  final byte[] byteCompBlock;
    private  final int[] expPosIntBlock;
    private  final int[] expHighBitIntBlock;
    
    private static final int MAX_BLOCK_SIZE = 128;
    
    public BlockReader(IndexInput in, int[] buffer) {
      decompressor = new LCPForDelta();
      input = in;
      decompBlock = buffer;
      
      byteCompBuffer = ByteBuffer.allocate(MAX_BLOCK_SIZE*4*4);
      byteCompBlock = byteCompBuffer.array();
      intCompBuffer = byteCompBuffer.asIntBuffer();
      
      expPosIntBlock = new int[MAX_BLOCK_SIZE];
      expHighBitIntBlock = new int[MAX_BLOCK_SIZE];
    }

    public void seek(long pos) throws IOException {
      //
    }

    public void readBlock() throws IOException {

        //  read the compressed data
        final int compressedSizeInInt = input.readInt();
        
        int blockSize = 128;
        input.readBytes(byteCompBlock, 0, compressedSizeInInt*4);
        intCompBuffer.rewind();
        
        decompressor.decompressOneBlockWithSizeWithIntBuffer(decompBlock, intCompBuffer, blockSize, expPosIntBlock, expHighBitIntBlock, compressedSizeInInt);
    }

    public void skipBlock() throws IOException {
      int numInts = input.readInt(); // nocommit: should PFOR use vint header?
      input.seek(input.getFilePointer() + numInts*4); // seek past block
    }
  }

  protected BlockReader getBlockReader(IndexInput in, int[] buffer) {
    return new BlockReader(in, buffer);
  }
}

