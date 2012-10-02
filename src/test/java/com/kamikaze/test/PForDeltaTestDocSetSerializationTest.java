package com.kamikaze.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.junit.Test;

import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.Conversion;

public class PForDeltaTestDocSetSerializationTest extends TestCase {

  private static String serial = "serialDocSet";

  public PForDeltaTestDocSetSerializationTest() {

  }


  @Test
  public void testNewPForDeltaDocSetSerialization() throws Exception{
     //using the new static serialize/deserialize methods to test the accuracy of the serializaton and deserialization of PForDeltaDocIdSet objects by verifying if the deserialized object's nextDoc() results match the original object's nextDoc() results
    int batch = 128;
    System.out.println("");
    System.out.println("Running test case: serialization/deserialization of PForDeltaDocIdSet objects via nextDoc() ...");
    System.out.println("----------------------------");

    int result[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
        37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
        91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106,
        107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
        121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134,
        135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148,
        149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162,
        163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176,
        177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190,
        191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204,
        205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218,
        219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232,
        233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246,
        247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260,
        261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274,
        275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288,
        289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302,
        303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316,
        317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330,
        331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344,
        345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358,
        359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372,
        373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386,
        387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400,
        401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414,
        415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428,
        429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442,
        443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456,
        457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470,
        471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484,
        485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498,
        499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512,
        513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526,
        527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540,
        541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554,
        555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568,
        569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582,
        583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596,
        597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610,
        611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624,
        625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638,
        639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652,
        653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666,
        667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680,
        681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694,
        695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708,
        709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722,
        723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736,
        737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750,
        751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764,
        765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778,
        779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792,
        793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806,
        807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820,
        821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834,
        835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848,
        849, 850, 851, 852, 853, 854, 855, 856, 857, 858 };
    PForDeltaDocIdSet docSetOrigin = new PForDeltaDocIdSet(batch);
    
    for (int i = 0; i < result.length; i++) {
      docSetOrigin.addDoc(result[i]);
    }

    try {
      File f = new File(serial);
      OutputStream os = new FileOutputStream(f);
      byte[] serializedBytes = PForDeltaDocIdSet.serialize(docSetOrigin);
      
      os.write(serializedBytes);
      
      os.flush();
      os.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    DocIdSet docSetDeserializd = null;

    try {
      File in = new File(serial);
      InputStream is = new FileInputStream(in);
      byte[] bytesSize = new byte[Conversion.BYTES_PER_INT]; 
      is.read(bytesSize);
      int totalNumInt = Conversion.byteArrayToInt(bytesSize, 0);
      byte[] bytesData = new byte[totalNumInt * Conversion.BYTES_PER_INT];
      is.read(bytesData,0,bytesData.length);
      docSetDeserializd = PForDeltaDocIdSet.deserialize(bytesData, 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    StatefulDSIterator dcitOrigin = docSetOrigin.iterator();
    org.apache.lucene.search.DocIdSetIterator dcitDeserialized = docSetDeserializd.iterator();

    try {
      for (int i = 0; i < result.length; i++) {
        int docid1 = dcitOrigin.nextDoc();
        int docid2 = dcitDeserialized.nextDoc();
        assertEquals(docid1, result[i]);
        assertEquals(docid1, docid2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    File in = new File(serial);
    if(in.exists())
    {
      in.delete();
    }
    System.out.println("-----------------completed--------------------------");

  }

  @Test
  public void testNewPForDeltaDocSetSerializationWithDifferentBatchSize() throws Exception{
     //using the new static serialize/deserialize methods to test the accuracy of the serializaton and deserialization of PForDeltaDocIdSet objects by verifying if the deserialized object's nextDoc() results match the original object's nextDoc() results
    int batch = 512;
    System.out.println("");
    System.out.println("Running test case: serialization/deserialization of PForDeltaDocIdSet objects (with different batch size) via nextDoc() ...");
    System.out.println("----------------------------");

    int maxDoc = 750000;
    int[] originalInput = new int[maxDoc];
    for(int i =0; i<maxDoc; ++i)
    {
      originalInput[i] = i;
    }
    int dataSize = 15000;
    int[] result = generateRandomDataHY(originalInput,maxDoc, dataSize);
    
//    int result[] = new int[dataSize];
//    for(int i=0; i<dataSize; i++)
//    {
//      result[i] = i+1;
//    }
    PForDeltaDocIdSet docSetOrigin = new PForDeltaDocIdSet(batch);
    
    for (int i = 0; i < result.length; i++) {
      docSetOrigin.addDoc(result[i]);
    }

    try {
      File f = new File(serial);
      OutputStream os = new FileOutputStream(f);
      byte[] serializedBytes = PForDeltaDocIdSet.serialize(docSetOrigin);
      
      os.write(serializedBytes);
      
      os.flush();
      os.close();

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    DocIdSet docSetDeserializd = null;

    
    try {
      File in = new File(serial);
      InputStream is = new FileInputStream(in);
      byte[] bytesSize = new byte[Conversion.BYTES_PER_INT]; 
      is.read(bytesSize);
      int totalNumInt = Conversion.byteArrayToInt(bytesSize, 0);
      byte[] bytesData = new byte[totalNumInt * Conversion.BYTES_PER_INT];
      is.read(bytesData,0,bytesData.length);
      docSetDeserializd = PForDeltaDocIdSet.deserialize(bytesData, 0);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    StatefulDSIterator dcitOrigin = docSetOrigin.iterator();
    org.apache.lucene.search.DocIdSetIterator dcitDeserialized = docSetDeserializd.iterator();

    try {
      for (int i = 0; i < result.length; i++) {
        int docid1 = dcitOrigin.nextDoc();
        int docid2 = dcitDeserialized.nextDoc();
        assertEquals(docid1, result[i]);
        assertEquals(docid1, docid2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    File in = new File(serial);
    if(in.exists())
    {
      in.delete();
    }
    System.out.println("-----------------completed--------------------------");

  }
  

  @Test
  public void AAtestNotDocSetSerialization() throws Exception {

    int batch = 128;
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
  
  private static int[] generateRandomDataHY(int[] ori, int maxDoc, int numDocs)
  {
    //System.out.println("generating random data");
    Random rand = new Random(System.currentTimeMillis()); 
    
    int i,j;
    
    
    int num = maxDoc;
    int tmp;
    int[] randomNums = new int[numDocs];
    
    for (j = 0; j < numDocs; j++, num--) 
    { 
      int nextDoc = rand.nextInt(num);
      
      randomNums[j] = ori[nextDoc];
      tmp = ori[num-1];
      ori[num-1] = ori[nextDoc];
      ori[nextDoc] = tmp;
      
      //printArray(ori, 0, maxDoc-1);
      //printArray(randomNums, 0, numDocs-1);
    }
    
    Arrays.sort(randomNums);
    System.out.println("randomly select " + numDocs + " numbers from " + maxDoc +  " numbers");
    //printArray(randomNums, 0, numDocs-1);
    return randomNums;
  }
}
