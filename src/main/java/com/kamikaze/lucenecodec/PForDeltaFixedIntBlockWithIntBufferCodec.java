package com.kamikaze.lucenecodec;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.lucene40.Lucene40DocValuesFormat;
import org.apache.lucene.codecs.lucene40.Lucene40FieldInfosFormat;
import org.apache.lucene.codecs.lucene40.Lucene40LiveDocsFormat;
import org.apache.lucene.codecs.lucene40.Lucene40NormsFormat;
import org.apache.lucene.codecs.lucene40.Lucene40SegmentInfoFormat;
import org.apache.lucene.codecs.lucene40.Lucene40StoredFieldsFormat;
import org.apache.lucene.codecs.lucene40.Lucene40TermVectorsFormat;



/**
 * A codec for fixed sized int block encoders. The int encoder
 * used here writes each block as data encoded by PForDelta.
 */

public class PForDeltaFixedIntBlockWithIntBufferCodec extends Codec {

  private final int blockSize;
  private static final String CODEC_NAME = "PatchedFrameOfRef4";
  
  private final StoredFieldsFormat fieldsFormat = new Lucene40StoredFieldsFormat();
  private final TermVectorsFormat vectorsFormat = new Lucene40TermVectorsFormat();
  private final FieldInfosFormat fieldInfosFormat = new Lucene40FieldInfosFormat();
  private final DocValuesFormat docValuesFormat = new Lucene40DocValuesFormat();
  private final SegmentInfoFormat infosFormat = new Lucene40SegmentInfoFormat();
  private final NormsFormat normsFormat = new Lucene40NormsFormat();
  private final LiveDocsFormat liveDocsFormat = new Lucene40LiveDocsFormat();

  public PForDeltaFixedIntBlockWithIntBufferCodec() {
    super(CODEC_NAME);
    this.blockSize = 128;
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
    return docValuesFormat;
  }

  @Override
  public StoredFieldsFormat storedFieldsFormat() {
    return fieldsFormat;
  }

  @Override
  public TermVectorsFormat termVectorsFormat() {
    return vectorsFormat;
  }

  @Override
  public FieldInfosFormat fieldInfosFormat() {
    return fieldInfosFormat;
  }

  @Override
  public SegmentInfoFormat segmentInfoFormat() {
    return infosFormat;
  }

  @Override
  public NormsFormat normsFormat() {
    return normsFormat;
  }

  @Override
  public LiveDocsFormat liveDocsFormat() {
    return liveDocsFormat;
  }
}

