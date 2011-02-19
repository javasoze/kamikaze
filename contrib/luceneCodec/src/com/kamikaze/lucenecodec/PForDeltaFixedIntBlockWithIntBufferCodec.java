package com.kamikaze.lucenecodec;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.codecs.Codec;
import org.apache.lucene.index.codecs.FieldsConsumer;
import org.apache.lucene.index.codecs.FieldsProducer;
import org.apache.lucene.index.codecs.sep.SepPostingsReaderImpl;
import org.apache.lucene.index.codecs.sep.SepPostingsWriterImpl;
import org.apache.lucene.index.codecs.VariableGapTermsIndexReader;
import org.apache.lucene.index.codecs.VariableGapTermsIndexWriter;
import org.apache.lucene.index.codecs.PostingsWriterBase;
import org.apache.lucene.index.codecs.PostingsReaderBase;
import org.apache.lucene.index.codecs.BlockTermsReader;
import org.apache.lucene.index.codecs.BlockTermsWriter;
import org.apache.lucene.index.codecs.TermsIndexReaderBase;
import org.apache.lucene.index.codecs.TermsIndexWriterBase;
import org.apache.lucene.index.codecs.standard.StandardCodec;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;



/**
 * A codec for fixed sized int block encoders. The int encoder
 * used here writes each block as data encoded by PForDelta.
 */

public class PForDeltaFixedIntBlockWithIntBufferCodec extends Codec {

  private final int blockSize;

  public PForDeltaFixedIntBlockWithIntBufferCodec(int blockSize) {
    this.blockSize = blockSize;
    name = "PatchedFrameOfRef4";
  }

  @Override
  public String toString() {
    return name + "(blockSize=" + blockSize + ")";
  }


  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    PostingsWriterBase postingsWriter = new SepPostingsWriterImpl(state, new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize));

    boolean success = false;
    TermsIndexWriterBase indexWriter;
    try {
      indexWriter = new VariableGapTermsIndexWriter(state, new VariableGapTermsIndexWriter.EveryNTermSelector(state.termIndexInterval));
      success = true;
    } finally {
      if (!success) {
        postingsWriter.close();
      }
    }

    success = false;
    try {
      FieldsConsumer ret = new BlockTermsWriter(indexWriter, state, postingsWriter, BytesRef.getUTF8SortedAsUnicodeComparator());
      success = true;
      return ret;
    } finally {
      if (!success) {
        try {
          postingsWriter.close();
        } finally {
          indexWriter.close();
        }
      }
    }
  }

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase postingsReader = new SepPostingsReaderImpl(state.dir,
                                                                      state.segmentInfo,
                                                                      state.readBufferSize,
                                                                      new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize), state.codecId);

    TermsIndexReaderBase indexReader;
    boolean success = false;
    try {
      indexReader = new VariableGapTermsIndexReader(state.dir,
                                                    state.fieldInfos,
                                                    state.segmentInfo.name,
                                                    state.termsIndexDivisor,
                                                    state.codecId);
      success = true;
    } finally {
      if (!success) {
        postingsReader.close();
      }
    }

    success = false;
    try {
      FieldsProducer ret = new BlockTermsReader(indexReader,
                                                       state.dir,
                                                       state.fieldInfos,
                                                       state.segmentInfo.name,
                                                       postingsReader,
                                                       state.readBufferSize,
                                                       BytesRef.getUTF8SortedAsUnicodeComparator(),
                                                       StandardCodec.TERMS_CACHE_SIZE,
                                                       state.codecId);
      success = true;
      return ret;
    } finally {
      if (!success) {
        try {
          postingsReader.close();
        } finally {
          indexReader.close();
        }
      }
    }
  }

  @Override
  public void files(Directory dir, SegmentInfo segmentInfo, String codecId, Set<String> files) {
    SepPostingsReaderImpl.files(segmentInfo, codecId, files);
    BlockTermsReader.files(dir, segmentInfo, codecId, files);
    VariableGapTermsIndexReader.files(dir, segmentInfo, codecId, files);
  }

  @Override
  public void getExtensions(Set<String> extensions) {
    SepPostingsWriterImpl.getExtensions(extensions);
    BlockTermsReader.getExtensions(extensions);
    VariableGapTermsIndexReader.getIndexExtensions(extensions);
  }
}

