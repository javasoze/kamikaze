package com.kamikaze.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.junit.Test;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.api.StatefulDSIterator;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.AndNotDocIdSetIterator;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.PForDeltaDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;
import com.kamikaze.pfordelta.PForDelta;

public class PForDeltaKamikazeTest extends TestCase 
{ 
  private static int batch = 256;
  private static String serial = "PForDeltaSerial";
  private static String serial2 = "PForDeltaSerial2";
  
  @Test
  public void testCorrectness() throws Exception{
    int numdocs = 257;
    Random rand = new Random();
    HashSet<Integer> set = new HashSet<Integer>();
    while(set.size() < numdocs){
      int n = Math.abs(rand.nextInt());
      set.add(n);
    }
    
    int[] numArray = new int[set.size()];
    
    
    
    int i = 0;
    for (int n : set){
      numArray[i++] = n;
    }
    Arrays.sort(numArray);
    
    PForDeltaDocIdSet docset = new PForDeltaDocIdSet();
    
    for (int n : numArray){
      docset.addDoc(n); 
    }
    
    DocIdSetIterator iter = docset.iterator();
    
    int doc;
    i = 0;
    while((doc=iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS){
      TestCase.assertEquals(numArray[i++], doc);
    }
  }

  @Test
  public void testAddDocNextDoc() throws Exception
  {     
    // test the accuracy of PForDeltaDocIdSet.addDoc() (Compression) and PForDeltaDocIdSet.PForDeltaDocIdIterator.nextDoc() (Decompression).
    System.out.println("Running test case: addDoc() (compression) and nextDoc() (decompression)...");
    
    ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
    ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
    
    int maxDoc = 35000;
    int listNum = 1;
   
    // compresssion (generate random data sets and add them into PForDeltaDocIdSet docs)
    getRandomDataSets(obs, docs, maxDoc, listNum);
    
    // get the original input (used to verify the accuracy of nextDoc())
    ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
    
    // decompression (iterating the compressed object)
    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)docs.get(0);
    DocIdSetIterator iter = pfdDS.iterator();
    ArrayList<Integer> output = new ArrayList<Integer>();
    int docId = iter.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      output.add(docId);
      docId = iter.nextDoc();
    }
   
    //printList(input, 0, input.size()-1);
    //printList(output, 0, output.size()-1);
    assertEquals(true, compareTwoLists(input, output));
    System.out.println("-------------------completed------------------------");
  } 

  @Test
  public void testAddNotDocsIdIterator() throws Exception
  {     
    System.out.println("Running test case: testAddNotDocsIdIterator()...");
    
    int n = 10;
    OBSDocIdSet evenset = new OBSDocIdSet(n);
    for (int i=0;i<n;i+=2){
      evenset.addDoc(i);
    }
    
    OBSDocIdSet fullset = new OBSDocIdSet(n);
    for (int i=0;i<n;++i){
      fullset.addDoc(i);
    }
    
    AndNotDocIdSetIterator iter = new AndNotDocIdSetIterator(fullset.iterator(),evenset.iterator());
    int doc;
    int i=1;
    while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS){
      assertEquals(i,doc);
      i+=2;
    }
    
    assertEquals(11,i);
  } 
  
  @Test
  public void testAddDocsNextDoc() throws Exception
  {     
    // test the accuracy of PForDeltaDocIdSet.addDoc() (Compression) and PForDeltaDocIdSet.PForDeltaDocIdIterator.nextDoc() (Decompression).
    System.out.println("Running test case: addDocs() (compression) and nextDoc() (decompression)...");
    
    ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
    ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
    
    int maxDoc = 35000;
    int listNum = 1;
   
    // compresssion (generate random data sets and add them into PForDeltaDocIdSet docs)
    getRandomDataSetsBatch(obs, docs, maxDoc, listNum);
    //getRandomDataSets(obs, docs, maxDoc, listNum);
    
    // get the original input (used to verify the accuracy of nextDoc())
    ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
    
    // decompression (iterating the compressed object)
    PForDeltaDocIdSet pfdDS = (PForDeltaDocIdSet)docs.get(0);
    DocIdSetIterator iter = pfdDS.iterator();
    ArrayList<Integer> output = new ArrayList<Integer>();
    int docId = iter.nextDoc();
    while(docId !=DocIdSetIterator.NO_MORE_DOCS)
    {      
      output.add(docId);
      docId = iter.nextDoc();
    }
   
    //printList(input, 0, input.size()-1);
    //printList(output, 0, output.size()-1);
    assertEquals(true, compareTwoLists(input, output));
    System.out.println("-------------------completed------------------------");
  } 
  
  @Test
  public void testVeryBigNumbers() throws Exception
  {     
    // test the accuracy of compressing/decompressing a sequence of big numbers 
    System.out.println("Running test case: testVeryBigNumbers ");
    Random random = new Random(0); 
    int blockNum = 10;
    int blockSize = 128;
    int[][] input = new int[blockNum][];
    
    for(int i=0; i<blockNum; i++)
    {
      input[i] = new int[blockSize];
      for(int j=0; j<blockSize; j++)
      {
        input[i][j] = random.nextInt() & Integer.MAX_VALUE;
      }

      int[] middleOutput = null;
      try{
        //middleOutput = new int[blockSize];
        middleOutput = PForDelta.compressOneBlockOpt(input[i], blockSize);
        int[] output = new int[blockSize];
        long start = System.currentTimeMillis();
        PForDelta.decompressOneBlock(output, middleOutput, blockSize);
        long end = System.currentTimeMillis();
        System.out.println("time: " + (end-start));
        //printList(input, 0, blockSize-1);
        //printList(output, 0, blockSize-1);
        assertEquals(true, compareTwoArrays(input[i], output));
        System.out.println("-------------------completed------------------------");
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  } 
  
  @Test
  public void testFind() throws Exception
  {     
      // test the accuracy of PForDeltaDocIdSet.find()
      System.out.println("Running test case: PForDeltaDocIdSet.find() ...");
    
      ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
      ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
      int maxDoc = 35000;
      int listNum = 1;
      
      getRandomDataSets(obs, docs, maxDoc, listNum);
      
      ArrayList<Integer> input = bitSetToArrayList(obs.get(0));
      
      DocSet docSet = new PForDeltaDocIdSet(); 
      
      boolean saw403 = false;
      for (Integer integer : input) 
      { 
          if(integer == 403)
          {
            saw403 = true;
            System.out.println("find the guy");
          }
          docSet.addDoc(integer); 
      } 
      boolean got = docSet.find(403); 
      assertEquals(saw403, got);
      System.out.println("---------------completed----------------------------");      
  }    
  
  
@Test
public void testPartialEmptyAnd() throws IOException 
{ 
    // test the accuracy of PForDeltaOrDocIdSet
    try 
    {
        ArrayList<Integer> output = new ArrayList<Integer>();
        System.out.println("Running OrDocIdSet of PForDeltaDocIdSet test case:  partial empty and ...");       
          
        DocSet ds1 = new PForDeltaDocIdSet(); 
        DocSet ds2 = new PForDeltaDocIdSet(); 
        ds2.addDoc(42); 
        ds2.addDoc(57); 
        ds2.addDoc(99); 
        ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
        docs.add(ds1);  // ds1 is empty
        docs.add(ds2);         
        
        DocSet ds3 = new PForDeltaDocIdSet(); 
        DocSet ds4 = new PForDeltaDocIdSet(); 
        ds4.addDoc(57); 
        ds4.addDoc(80); 
        ds4.addDoc(99);         
        ArrayList<DocIdSet> docs2 = new ArrayList<DocIdSet>(); 
        docs2.add(ds3); // ds3 is empty
        docs2.add(ds4); 
        
       // PForDeltaOrDocIdSet orlist1 = new PForDeltaOrDocIdSet(docs); 
       // PForDeltaOrDocIdSet orlist2 = new PForDeltaOrDocIdSet(docs2); 
        OrDocIdSet orlist1 = new OrDocIdSet(docs); 
        OrDocIdSet orlist2 = new OrDocIdSet(docs2); 
        
        ArrayList<DocIdSet> docs3 = new ArrayList<DocIdSet>(); 
        docs3.add(orlist1); 
        docs3.add(orlist2); 
        
        AndDocIdSet andlist = new AndDocIdSet(docs3); 

        
        DocIdSetIterator iter = andlist.iterator(); 
        int docId = iter.nextDoc(); 
        
        while(docId != DocIdSetIterator.NO_MORE_DOCS) 
        { 
          output.add(docId);
          docId = iter.nextDoc();
        }   
        assertEquals(57, output.get(0).intValue());
        assertEquals(99, output.get(1).intValue());
      } 
      catch(Exception e) 
      { 
              e.printStackTrace();
      } 
      
      System.out.println("-----------------completed--------------------------");
} 
 

@Test
public void testAndIntersections() throws Exception
{ 
     System.out.println("Running test case: intersections, PForDeltaAndDocIdSet.nextDoc() ...");
  
     ArrayList<OpenBitSet> obs = new ArrayList<OpenBitSet>(); 
     ArrayList<DocIdSet> docs = new ArrayList<DocIdSet>(); 
     ArrayList<Integer> expectedIntersectionResult = new ArrayList<Integer>();
      
     int maxDoc = 5000;
     int numDoc1 = 1000;
     int numDoc2 = 2000;
     int numDoc3 = 4000;
     int[] originalInput = null;
     int[] input1 = null;
     int[] input2 = null;
     int[] input3 = null;
     originalInput = new int[maxDoc];
     for(int i =0; i<maxDoc; ++i)
     {
       originalInput[i] = i;
     }
     
     // generate random numbers and add them into PForDeltaDocIdSets
     input1 = generateRandomDataNew(originalInput, maxDoc, numDoc1);
     loadRandomDataSets(input1, obs, docs, numDoc1);
     input2 = generateRandomDataNew(originalInput, maxDoc, numDoc2);
     loadRandomDataSets(input2, obs, docs, numDoc2);
     input3 = generateRandomDataNew(originalInput, maxDoc, numDoc3);
     loadRandomDataSets(input3, obs, docs, numDoc3);
      
     // get the expected result
     OpenBitSet base = obs.get(0); 
     for(int i = 1; i < obs.size(); ++i) 
     { 
       base.intersect(obs.get(i)); 
     }     
     for(int k=0; k<base.size(); ++k)
     {
       if(base.get(k))
         expectedIntersectionResult.add(k);
     }
     
     // get the results from PForDeltaAndDocIdSet
     ArrayList<Integer> intersectionResult = new ArrayList<Integer>();
     AndDocIdSet ands = new AndDocIdSet(docs); 
     DocIdSetIterator iter = ands.iterator();
     int docId = iter.nextDoc();
     while(docId != DocIdSetIterator.NO_MORE_DOCS)
     {
       intersectionResult.add(docId);
       docId = iter.nextDoc();
     }
   
     if(!compareTwoLists(intersectionResult, expectedIntersectionResult))
     {
       System.out.println("The result for the new version does not match the expectation");
     }
   System.out.println("----------------completed---------------------------");
} 


@Test
public void testPForDeltaDocSetSerialization() throws Exception{
   //test the accuracy of the serializaton and deserialization of PForDeltaDocIdSet objects by verifying if the deserialized object's nextDoc() results match the original object's nextDoc() results
  
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
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
    oos.writeObject(docSetOrigin);
    oos.flush();
    oos.close();

  } catch (Exception e) {
    e.printStackTrace();
    fail(e.getMessage());
  }

  DocIdSet docSetDeserializd = null;

  try {
    File in = new File(serial);
    InputStream f = new FileInputStream(in);
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(f));
    docSetDeserializd = (DocIdSet) (ois.readObject());
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
public void testPForDeltaDocSetSerializationAndFind() throws Exception{
//test the accuracy of the serialization and deserialization of PForDeltaDocIdSet objects by using two data sets (one is the subset of the other) and verifying the
//deserialized super set can always find() the deserialized subset's elements
  System.out.println("");
  System.out.println("Running test case: serialization/deserialization of PForDeltaDocIdSet objects via find() ......");
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
  
  int result2[] = { 1, 10, 11, 12, 13, 14, 15, 16, 17,
      19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
      37, 38, 39, 40, 41, 50, 51, 52, 53, 54,
      55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
      73, 74, 75, 76,  129, 130, 131, 132, 133, 134,
      135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148,
      149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162,
      163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176,
      177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190,
      191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204,
      205, 206, 270, 271, 272, 273, 274,
      275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288,
      289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302,
      303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316,
      317, 318, 319,
      345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358,
      359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372,
      373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386,
      387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400,
      401, 402, 453, 454, 455, 456,
      457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470,
      471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484,
      485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498,
      499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512,
      513, 514, 592, 593, 594, 595, 596,
      597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610,
      611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624,
      625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638,
      639, 640, 641, 642, 772, 773, 774, 775, 776, 777, 778,
      779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792,
      793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806,
      807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820,
      821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834,
      835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848,
      849, 850, 851, 852, 853, 854, 855, 856, 857, 858 };
  
  
  PForDeltaDocIdSet docSet1 = new PForDeltaDocIdSet(batch);
  PForDeltaDocIdSet docSet2 = new PForDeltaDocIdSet(batch);
  
  for (int i = 0; i < result.length; i++) {
    docSet1.addDoc(result[i]);
  }

  for (int i = 0; i < result2.length; i++) {
    docSet2.addDoc(result2[i]);
  }
  
  try {
    File f = new File(serial);
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
    oos.writeObject(docSet1);
    oos.flush();
    oos.close();
    
    File f2 = new File(serial2);
    ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(f2));
    oos2.writeObject(docSet2);
    oos2.flush();
    oos2.close();
  } catch (Exception e) {
    e.printStackTrace();
    fail(e.getMessage());
  }

  PForDeltaDocIdSet docSetDeserialized = null;
  PForDeltaDocIdSet docSetDeserialized2 = null;
  try {
    File in = new File(serial);
    InputStream f = new FileInputStream(in);
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(f));
    docSetDeserialized = (PForDeltaDocIdSet) (ois.readObject());
    
    File in2 = new File(serial2);
    InputStream f2 = new FileInputStream(in2);
    ObjectInputStream ois2 = new ObjectInputStream(new BufferedInputStream(f2));
    docSetDeserialized2 = (PForDeltaDocIdSet) (ois2.readObject());
    
  } catch (Exception e) {
    e.printStackTrace();
    fail(e.getMessage());
  }

  org.apache.lucene.search.DocIdSetIterator dcit2 = docSetDeserialized2.iterator();

  try {
    for (int i = 0; i < result2.length; i++) {
      int docid = dcit2.nextDoc();
      boolean bFound = docSetDeserialized.find(docid);
      assertEquals(true, bFound);
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
  File in2 = new File(serial2);
  if(in2.exists())
  {
    in2.delete();
  }
  System.out.println("-----------------completed--------------------------");
}

@Test
public void testOrDocIdSetWithIntArrayPForDelta() throws Exception
{
  // test OrDocIdSet with IntArrayDocIdSet and PForDeltaDocIdSet
  System.out.println("Running testOrDocIdSetWithIntArrayPForDelta() Test case...");
  DocIdSet[] DocList;
  DocList = new DocIdSet[5];
  int maxdoc = 100000;
  for (int i=0;i<DocList.length;++i)
  {
    IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
    for (int k=0;k<maxdoc;k++)
    {
      docset.addDoc(k);
    }
    DocList[i]=docset;
  }
  //PForDeltaOrDocIdSet orset = new PForDeltaOrDocIdSet(Arrays.asList(DocList));
  OrDocIdSet orset = new OrDocIdSet(Arrays.asList(DocList));
  DocIdSetIterator iter = orset.iterator();
  int doc;
  while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
  {
  }
  System.out.println("--------completed-----------");
}

@Test
public void testAndDocIdSetWithIntArrayPForDelta() throws Exception
{
//test AndDocIdSet with IntArrayDocIdSet and PForDeltaDocIdSet
  System.out.println("Running testAndDocIdSetWithIntArrayPForDelta() Test case...");
  DocIdSet[] DocList;
  DocList = new DocIdSet[5];
  int maxdoc = 100000;
  for (int i=0;i<DocList.length;++i)
  {
    IntArrayDocIdSet docset = new IntArrayDocIdSet(maxdoc);
    for (int k=0;k<maxdoc;k++)
    {
      docset.addDoc(k);
    }
    DocList[i]=docset;
  }
  AndDocIdSet orset = new AndDocIdSet(Arrays.asList(DocList));
  DocIdSetIterator iter = orset.iterator();
  int doc;
  while((doc=iter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
  {
  }
  System.out.println("--------completed-----------");
}

//@Test
//public void testPForDeltaDocSetFactory() {
//  int min = 1;
//  int max  = 100;
//  int count  = 100;
//  
//  DocIdSet set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
//  set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
//  set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
//  
//  min = 1;
//  max = 10000;
//  count = 10000;
//
//  set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.PERFORMANCE);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.IntArrayDocIdSet");
//  set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.SPACE);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
//  set = PForDeltaDocSetFactory.getDocSetInstance(min, max, count, FOCUS.OPTIMAL);
//  assertEquals(set.getClass().getName(), "com.kamikaze.docidset.impl.PForDeltaDocIdSet");
//}


  private OpenBitSet createObs(ArrayList<Integer> nums, int maxDoc) { 
    OpenBitSet bitSet = new OpenBitSet(maxDoc); 
    for(int num:nums) 
      bitSet.set(num); 
    return bitSet; 
  } 
  
  private OpenBitSet createObs(int nums[], int maxDoc) { 
    OpenBitSet bitSet = new OpenBitSet(maxDoc); 
    for(int num:nums) 
      bitSet.set(num); 
    return bitSet; 
  } 
  
  private DocIdSet createDocSet(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSet(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSetOld(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSetOld(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    for(int num:nums) 
    {         
      p4d.addDoc(num);
    }
    return p4d; 
  } 
  
  private DocIdSet createDocSetBatch(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    int[] numsArray = new int[nums.size()];
    int i=0;
    for(Integer num : nums)
    {
      numsArray[i++] = num;
    }
    p4d.addDocs(numsArray, 0, numsArray.length); 
    return p4d; 
  } 
  
  private DocIdSet createDocSetBatch(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getPForDeltaDocSetInstance(); 
    p4d.addDocs(nums, 0, nums.length); 
    return p4d; 
  } 
  
  private DocIdSet createDocSetOldBatch(int[] nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    p4d.addDocs(nums, 0, nums.length); 
    return p4d; 
  } 
  
  private DocIdSet createDocSetOldBatch(ArrayList<Integer> nums) throws Exception{ 
    DocSet p4d = DocSetFactory.getP4DDocSetInstance(); 
    int[] numsArray = new int[nums.size()];
    int i=0;
    for(Integer num : nums)
    {
      numsArray[i++] = num;
    }
    p4d.addDocs(numsArray, 0, numsArray.length); 
    return p4d; 
  } 
  
  
  private void loadRandomDataSets(int[] data, ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, ArrayList<DocIdSet> docsOld, int maxDoc) throws Exception
  {
    obs.add(createObs(data, maxDoc)); 
    docs.add(createDocSet(data)); 
    docsOld.add(createDocSetOld(data));
  }
  
  private void loadRandomDataSets(int[] data, ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs,  int maxDoc) throws Exception
  {
    obs.add(createObs(data, maxDoc)); 
    docs.add(createDocSet(data)); 
  }
  
  // generate random numbers and insert them into docIdSets (another more efficient version is generateRandomDataNew) 
  private void getRandomDataSets(ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, int maxDoc, int listNum)  throws Exception
  { 
    Random rand = new Random(System.currentTimeMillis()); 
    int numdocs;
    for(int i=0; i < listNum; ++i) 
    { 
      numdocs = maxDoc;
      
      ArrayList<Integer> nums = new ArrayList<Integer>(); 
      HashSet<Integer> seen = new HashSet<Integer>(); 
      for (int j = 0; j < numdocs; j++) 
      { 
        int nextDoc = rand.nextInt(maxDoc); 
        if(seen.contains(nextDoc)) 
        { 
          while(seen.contains(nextDoc)) 
          { 
            nextDoc = rand.nextInt(maxDoc); 
          } 
        } 
        nums.add(nextDoc); 
        seen.add(nextDoc); 
       } 
       Collections.sort(nums); 
       
       //printList(nums, 0, nums.size()-1);
       obs.add(createObs(nums, maxDoc)); 
       docs.add(createDocSet(nums)); 
    }
  }
  
//generate random numbers and insert them into docIdSets (another more efficient version is generateRandomDataNew) 
  private void getRandomDataSetsBatch(ArrayList<OpenBitSet> obs, ArrayList<DocIdSet>docs, int maxDoc, int listNum)  throws Exception
  { 
    Random rand = new Random(System.currentTimeMillis()); 
    int numdocs;
    for(int i=0; i < listNum; ++i) 
    { 
      numdocs = maxDoc;
      
      ArrayList<Integer> nums = new ArrayList<Integer>(); 
      HashSet<Integer> seen = new HashSet<Integer>(); 
      for (int j = 0; j < numdocs; j++) 
      { 
        int nextDoc = rand.nextInt(maxDoc); 
        if(seen.contains(nextDoc)) 
        { 
          while(seen.contains(nextDoc)) 
          { 
            nextDoc = rand.nextInt(maxDoc); 
          } 
        } 
        nums.add(nextDoc); 
        seen.add(nextDoc); 
       } 
       Collections.sort(nums); 
       
       //printList(nums, 0, nums.size()-1);
       obs.add(createObs(nums, maxDoc)); 
       docs.add(createDocSetBatch(nums)); 
    }
  }
    
    // convert an openBitSet to an array list 
    private ArrayList<Integer> bitSetToArrayList(OpenBitSet bs)
    {
      ArrayList<Integer> listRes = new ArrayList<Integer>();
      for(int i=0; i<bs.capacity(); i++)
      {
        if(bs.get(i))
        {
           listRes.add(i);
        }
       }
       return listRes;
    }
    
    //  print a openBitSet object
    private ArrayList<Integer> printBitSet(OpenBitSet bs)
    {
      ArrayList<Integer> listRes = new ArrayList<Integer>();
      System.out.print("bitSet(" + bs.capacity() +") [");
      for(int i=0; i<bs.capacity(); i++)
      {
        if(bs.get(i))
        {
           listRes.add(i);
           System.out.print(i);
           System.out.print(" ");
        }
       }
       System.out.println("]");
       return listRes;
    }
    
    //  print a list 
    private void printList(ArrayList<Integer> list, int start, int end)
    {
      System.out.print("(" + (end-start+1) + ")[");
      for(int i=start; i<=end; ++i)
      {
        System.out.print(list.get(i));
        //System.out.print(", ");
        System.out.print(", " + i + "; ");
     
      }
      System.out.println("]");
    }
    
    // print an array
    private void printList(int[] list, int start, int end)
    {
      System.out.print("(" + (end-start+1) + ")[");
      for(int i=start; i<=end; ++i)
      {
        System.out.print(list[i]);
        System.out.print(", ");
      }
      System.out.println("]");
    }
    
    void printListWithIndex(ArrayList<Integer> list, int start, int end)
    {
      System.out.print("(" + (end-start+1) + ")[");
      for(int i=start; i<=end; ++i)
      {
        System.out.print(list.get(i));
        System.out.print(":"+i+", ");
      }
      System.out.println("]");
    }
    
    
    //  compare two lists to see if they are equal and print all the different numbers
    private boolean compareTwoLists(ArrayList<Integer> input, ArrayList<Integer> output)
    {
      System.out.println("inputSize:" + input.size() + "outputSize:" + output.size());
      int i=0;
      boolean ret = true;
      for(i=0; i<input.size() && i<output.size(); ++i)
      {
        if(input.get(i).intValue() != output.get(i).intValue())
        {
          System.out.println("in[" + i + "]" + input.get(i) + " != out[" + i + "]" + output.get(i));
          ret = false;
        }
      }
      if(i<input.size())
      {
        printList(output, i, output.size());
        ret = false;
      }
      if(i<output.size())
      {
        printList(input, i, input.size());
        ret = false;
      }
      return ret;
    }
        
    // compare two arrays, print out the difference        
    private boolean compareTwoArrays(int[] input,  int[] output)
    {
      //System.out.println("inputSize:" + input.length + "outputSize:" + output.length);
      int i=0;
      boolean ret = true;
      for(i=0; i<input.length && i<output.length; ++i)
      {
        if(input[i] != output[i])
        {
          System.out.println("in[" + i + "]" + input[i] + " != out[" + i + "]" + output[i]);
          ret = false;
        }
      }
      if(i<input.length)
      {
        printList(output, i, output.length);
        ret = false;
      }
      if(i<output.length)
      {
        printList(input, i, input.length);
        ret = false;
      } 
      return ret;
    }
    
  // generate numDocs numbers out of maxDoc numbers
    private int[] generateRandomDataNew(int[] ori, int maxDoc, int numDocs)
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
    
    
    
    // generate numDocs numbers out of maxDoc numbers
    private int[] generateRandomDataNew(int maxDoc, int numDocs)
    {
      //System.out.println("generating random data");
      Random rand = new Random(System.currentTimeMillis()); 
      int[] ori = new int[maxDoc];
      int i,j;
      
      for(i =0; i<maxDoc; ++i)
      {
        ori[i] = i;
      }
      
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

