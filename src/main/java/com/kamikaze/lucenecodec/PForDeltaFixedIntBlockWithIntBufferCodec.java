package com.kamikaze.lucenecodec;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.codecs.BlockTermsReader;
import org.apache.lucene.codecs.BlockTermsWriter;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.TermsIndexReaderBase;
import org.apache.lucene.codecs.TermsIndexWriterBase;
import org.apache.lucene.codecs.VariableGapTermsIndexReader;
import org.apache.lucene.codecs.VariableGapTermsIndexWriter;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;



/**
 * A codec for fixed sized int block encoders. The int encoder
 * used here writes each block as data encoded by PForDelta.
 */

public class PForDeltaFixedIntBlockWithIntBufferCodec extends Codec {

  private final int blockSize;
  private static final String CODEC_NAME = "PatchedFrameOfRef4";

  public PForDeltaFixedIntBlockWithIntBufferCodec(int blockSize) {
    super(CODEC_NAME);
    this.blockSize = blockSize;
  }

  @Override
  public String toString() {
    return getName() + "(blockSize=" + blockSize + ")";
  }

  @Override
  public PostingsFormat postingsFormat() {
    return new PForDeltaFixedIntBlockWithIntBufferPostingsFormat(getName(), blockSize);
  }

  @Override
  public DocValuesFormat docValuesFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public StoredFieldsFormat storedFieldsFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TermVectorsFormat termVectorsFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public FieldInfosFormat fieldInfosFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SegmentInfoFormat segmentInfoFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NormsFormat normsFormat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LiveDocsFormat liveDocsFormat() {
    // TODO Auto-generated method stub
    return null;
  }
}

