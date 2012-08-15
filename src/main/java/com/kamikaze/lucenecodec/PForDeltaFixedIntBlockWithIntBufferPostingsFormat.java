package com.kamikaze.lucenecodec;

import java.io.IOException;

import org.apache.lucene.codecs.BlockTermsReader;
import org.apache.lucene.codecs.BlockTermsWriter;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.TermsIndexReaderBase;
import org.apache.lucene.codecs.TermsIndexWriterBase;
import org.apache.lucene.codecs.VariableGapTermsIndexReader;
import org.apache.lucene.codecs.VariableGapTermsIndexWriter;
import org.apache.lucene.codecs.sep.SepPostingsReader;
import org.apache.lucene.codecs.sep.SepPostingsWriter;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

public class PForDeltaFixedIntBlockWithIntBufferPostingsFormat extends
    PostingsFormat {

  public final static int TERMS_CACHE_SIZE = 1024;
  
  private int blockSize;
  
  
  protected PForDeltaFixedIntBlockWithIntBufferPostingsFormat(String name,int blockSize) {
    super(name);
    this.blockSize = blockSize;
  }

  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state)
      throws IOException {
    PostingsWriterBase postingsWriter = new SepPostingsWriter(state, new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize));

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
      FieldsConsumer ret = new BlockTermsWriter(indexWriter, state, postingsWriter);
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
  public FieldsProducer fieldsProducer(SegmentReadState state)
      throws IOException {
    PostingsReaderBase postingsReader = new SepPostingsReader(
        state.dir,
        state.fieldInfos,
        state.segmentInfo,
        state.context,
        new PForDeltaFixedIntBlockWithIntBufferFactory(blockSize), state.segmentSuffix);
    
    TermsIndexReaderBase indexReader;
    boolean success = false;
    try {

      indexReader = new VariableGapTermsIndexReader(state.dir,
                                                    state.fieldInfos,
                                                    state.segmentInfo.name,
                                                    state.termsIndexDivisor,
                                                    state.segmentSuffix,
                                                    state.context);
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
                                                       state.context,
                                                       TERMS_CACHE_SIZE,
                                                       state.segmentSuffix);
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

}
