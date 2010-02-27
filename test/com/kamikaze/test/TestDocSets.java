package com.kamikaze.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.junit.Test;

import com.kamikaze.docidset.api.DocSet;
import com.kamikaze.docidset.bitset.MyOpenBitSet;
import com.kamikaze.docidset.impl.AndDocIdSet;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;
import com.kamikaze.docidset.impl.NotDocIdSet;
import com.kamikaze.docidset.impl.OBSDocIdSet;
import com.kamikaze.docidset.impl.OrDocIdSet;
import com.kamikaze.docidset.impl.P4DDocIdSet;
import com.kamikaze.docidset.utils.DocSetFactory;
import com.kamikaze.docidset.utils.DocSetFactory.FOCUS;

public class TestDocSets {

  private static final FOCUS SPACE = null;
  private static int batch = 128;

  public TestDocSets() {

  }
  
  @Test
  public void testOrBoundary() throws IOException
  {
    
    System.out.println("");
    System.out.println("Running Or Boundary Test case...");
    System.out.println("----------------------------");
    
    DocSet dset1 = new IntArrayDocIdSet(2001);
    DocSet dset2 = new IntArrayDocIdSet(2001);
    dset1.addDoc(0);
    dset2.addDoc(1);
    
    List<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(dset1);
    sets.add(dset2);
    
    
    
    OrDocIdSet ord = new OrDocIdSet(sets);
    DocIdSetIterator dcit = ord.iterator();
    int docid = dcit.nextDoc();
    assertEquals(0,docid);
    docid = dcit.nextDoc();
    assertEquals(1,docid);
 
  }
  

  @Test
  public void combinationSetsSanityTest() throws IOException
  {
    System.out.println("");
    System.out.println("Running Combination Sanity Test case...");
    System.out.println("----------------------------");
    
    
    Set<Integer> set = new TreeSet<Integer>();
    Random random = new Random();
    
    DocSet dset1 = DocSetFactory.getDocSetInstance(0, 500000, 50000/3, DocSetFactory.FOCUS.OPTIMAL);
    DocSet dset2 = DocSetFactory.getDocSetInstance(0, 500000, 50000/3, DocSetFactory.FOCUS.OPTIMAL);
    DocSet dset3 = DocSetFactory.getDocSetInstance(0, 500000, 50000/3, DocSetFactory.FOCUS.OPTIMAL);
    
    for(int i=1;i<50001;i++)
    {
       set.add(random.nextInt(i+1)*10);
    }
    int i=0;
    int s1=0,s2=0,s3=0;
    
    for(Integer intr : set)
    {
      if(++i%3==0){
        dset1.addDoc(intr);
        s1++;
      }
      
      else if(i%2==0){
        dset2.addDoc(intr);
        s2++;
      }
      else{
        s3++;
        dset3.addDoc(intr);
      }
    }
    
    assertEquals(s1,dset1.size());
    assertEquals(s2,dset2.size());
    assertEquals(s3,dset3.size());

    List<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(dset1);
    sets.add(dset2);
    sets.add(dset3);
    
    OrDocIdSet ord = new OrDocIdSet(sets);
    NotDocIdSet not = new NotDocIdSet(ord, 5);
    
    org.apache.lucene.search.DocIdSetIterator dcit = ord.iterator();
    assertEquals(set.size(),ord.size());
    org.apache.lucene.search.DocIdSetIterator dcit2 = not.iterator();
    
    Iterator<Integer> it = set.iterator();
    int docid;
    while((docid = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      Integer x = it.next();
      assertEquals(x.intValue(),docid);
      
    }
    
  
    
    it = set.iterator();
    while((docid = dcit2.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      Integer x = it.next();
      assertFalse(x.intValue()==docid);
    }
    
  }
  

  @Test
  public void testSmallSetsCombination() throws Exception{
    System.out.println("");
    System.out.println("Running Small Set And test");
    System.out.println("----------------------------");

    MyOpenBitSet s1 = new MyOpenBitSet();
    MyOpenBitSet s2 = new MyOpenBitSet();
    MyOpenBitSet s3 = new MyOpenBitSet();
    MyOpenBitSet s4 = new MyOpenBitSet();

    s1.set(0);
    s1.set(4);
    s1.set(5);
    s1.set(6);

    s2.set(5);
    s2.set(6);

    s3.set(1);
    s3.set(5);

    ArrayList<DocIdSet> docSet = new ArrayList<DocIdSet>();

    docSet.add(s1);
    docSet.add(s2);
    docSet.add(s3);

    AndDocIdSet ord = new AndDocIdSet(docSet);

    try {
      int docid;
      for (DocIdSetIterator dcit = ord.iterator(); (docid = dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS;)
        assertEquals(docid, 5);
    } catch (IOException e) {
         e.printStackTrace();
    }

    System.out.println("");
    System.out.println("Running Small Set Not test");
    System.out.println("----------------------------");

    s1.set(0);

    DocIdSetIterator nsit = new NotDocIdSet(s1, 5).iterator();

    int i = 1;
    try {
      int docid;
      while ((docid = nsit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
        assertEquals(i++, docid);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testNotDocIdSetSkipSanity() {
    
  }
   

  @Test
  public void testOrDocIdSetSkipSanity() {
  }

  @Test
  public void testCombinationSanitySmallTest() throws Exception{
    System.out.println("");
    System.out.println("Running Combination Small Set Test Case");
    System.out.println("----------------------------");

    OpenBitSet bs1 = new OpenBitSet();
    OpenBitSet bs2 = new OpenBitSet();
    OpenBitSet bs3 = new OpenBitSet();

    bs1.set(858);
    bs2.set(857);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(bs1);
    sets.add(bs2);

    OrDocIdSet ord = new OrDocIdSet(sets);

    assertEquals(0, ord.findWithIndex(857));
    assertEquals(-1, ord.findWithIndex(1000));
    
    
    bs3.set(857);
    bs3.set(858);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(bs3);

    AndDocIdSet and = new AndDocIdSet(sets2);
    org.apache.lucene.search.DocIdSetIterator andit = and.iterator();
    int cursor = 0;
    try {
      int docid;
      while ((docid = andit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        assertEquals(bs3.nextSetBit(docid), docid);
        assertEquals(cursor++,and.findWithIndex(docid));
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }

  }

  
  private void _testWideDocSkips(String message, DocIdSet set) throws Exception
  {
   
    System.out.println(message);
    
    org.apache.lucene.search.DocIdSetIterator dcit = null;
    try {
      
      dcit = set.iterator();
      int docid = dcit.advance(94);
      assertEquals(94,docid);
      docid = dcit.advance(102);
      assertEquals(102,docid);
      docid = dcit.advance(500);
      assertEquals(500,docid);
      docid = dcit.advance(700);
      assertEquals(700,docid);
      //dcit.skipTo(1001);
      //assertEquals(1002, dcit.doc());
      docid = dcit.advance(1788);
      assertEquals(1788, docid);
      docid = dcit.advance(1901);
      assertEquals(1902, docid);
      docid = dcit.advance(2400);
      assertEquals(2400, docid);
      docid = dcit.advance(2401);
      assertEquals(2403, docid); 
      //dcit.skipTo(2403);
      //assertEquals(2406, dcit.doc()); 
      
      assertEquals(DocIdSetIterator.NO_MORE_DOCS,dcit.advance(450000));
     
 
    }
   catch (IOException e) {
     e.printStackTrace();
    fail(e.getMessage());
    }
    
  }
  
  
  @Test
  public void testWideDocSkips() throws Exception {

    System.out.println("");
    System.out.println("Running Wide Doc Skip sanity test");
    System.out.println("----------------------------");
    
    OpenBitSet pset1 = new OpenBitSet();
    OpenBitSet pset2 = new OpenBitSet();
    OpenBitSet pset3 = new OpenBitSet();

    for (int i = 0; i < 1000; i++) {
      pset1.set(i);
      pset2.set(i*2);
      pset3.set(i*3);
    }
   
    

    List<DocIdSet> its = new ArrayList<DocIdSet>();
    its.add(pset1);
    its.add(pset2);
    its.add(pset3);
    OrDocIdSet orSet = new OrDocIdSet(its);
    //_testWideDocSkips("Testing skips on OrDocSets", orSet);
    
    
    OpenBitSet pset4 = new OpenBitSet();
    org.apache.lucene.search.DocIdSetIterator orit = orSet.iterator();
    int docid;
    while((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      pset4.set(docid);
    }
    _testWideDocSkips("Testing skips on OpenBitSets", pset4);
    
    MyOpenBitSet pset5 = new MyOpenBitSet();
    orit = orSet.iterator();
    while((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      pset5.set(docid);
    }
    _testWideDocSkips("Testing skips on MyOpenBitSets", pset5);
    
    IntArrayDocIdSet pset6 = new IntArrayDocIdSet();
    orit = orSet.iterator();
    while((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      pset6.addDoc(docid);
    }
    _testWideDocSkips("Testing skips on IntArrayDocIdSet", pset6);
    
    P4DDocIdSet pset7 = new P4DDocIdSet();
    orit = orSet.iterator();
    while((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      pset7.addDoc(docid);
    }
    _testWideDocSkips("Testing skips on P4DDocIdSet", pset7);
    
    OBSDocIdSet pset8 = new OBSDocIdSet(2000);
    orit = orSet.iterator();
    while((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
    {
      pset8.addDoc(docid);
    }
    _testWideDocSkips("Testing skips on OBSDocIdSet", pset7);
    
    
    NotDocIdSet pset9 = new NotDocIdSet(new NotDocIdSet(orSet, 3001),3001);
    _testWideDocSkips("Testing skips on NotDocIdSet", pset9);
    
  }

  
  
  @Test
  public void testAndDocIdSetSkipSanity() throws Exception{

    System.out.println("");
    System.out.println("Running AndDocIdSet Skip sanity test");
    System.out.println("----------------------------");
    int set1[] = { 8, 27, 30, 35, 53, 59, 71, 74, 87, 92, 104, 113, 122, 126,
        135, 135, 137, 138, 185, 186, 188, 192, 197, 227, 227, 230, 242, 252,
        255, 259, 267, 270, 271, 289, 298, 305, 311, 312, 325, 335, 337, 346,
        351, 360, 365, 371, 375, 380, 387, 391, 406, 407, 408, 419, 425, 430,
        443, 451, 454, 456, 464, 466, 469, 473, 478, 483, 496, 502, 517, 527,
        531, 578, 601, 605, 625, 626, 632, 638, 641, 648, 652, 653, 667, 677,
        682, 697, 700, 711, 713, 733, 764, 780, 782, 796, 798, 801, 804, 812,
        817, 831, 835, 849, 859, 872, 886, 891, 893, 895, 903, 908, 914, 915,
        916, 917, 920, 921, 926, 944, 947, 950, 956, 962, 964, 969, 979, 986,
        994, 996, 1018, 1019, 1022, 1025, 1029, 1029, 1039, 1058, 1062, 1063,
        1064, 1068, 1069, 1071, 1075, 1082, 1085, 1096, 1098, 1102, 1103, 1104,
        1104, 1119, 1120, 1122, 1122, 1123, 1147, 1149, 1179, 1183, 1195, 1197,
        1200, 1201, 1214, 1215, 1220, 1221, 1221, 1225, 1229, 1252, 1260, 1261,
        1268, 1269, 1274, 1279, 1293, 1336, 1336, 1348, 1369, 1370, 1375, 1394,
        1401, 1414, 1444, 1453, 1459, 1468, 1473, 1473, 1474, 1485, 1502, 1505,
        1506, 1517, 1518, 1520, 1521, 1522, 1528, 1537, 1543, 1549, 1550, 1560,
        1565, 1566, 1585, 1599, 1604, 1619, 1637, 1650, 1658, 1679, 1684, 1691,
        1691, 1701, 1701, 1715, 1719, 1720, 1722, 1740, 1740, 1748, 1752, 1756,
        1756, 1776, 1796, 1799, 1799, 1800, 1809, 1811, 1828, 1829, 1849, 1859,
        1865, 1868, 1886, 1900, 1933, 1955, 1959, 1983, 1985, 1999, 2003, 2003,
        2029, 2038, 2048, 2050, 2054, 2056, 2059, 2060, 2079, 2095, 2099, 2104,
        2111, 2113, 2119, 2119, 2122, 2123, 2141, 2142, 2145, 2148, 2160, 2182,
        2183, 2200, 2203, 2209, 2210, 2221, 2232, 2261, 2267, 2268, 2272, 2283,
        2297, 2298, 2313, 2314, 2316, 2316, 2331, 2332, 2338, 2343, 2345, 2350,
        2350, 2365, 2378, 2384, 2392, 2399, 2414, 2420, 2425, 2433, 2445, 2457,
        2461, 2462, 2463, 2497, 2503, 2519, 2522, 2533, 2556, 2568, 2577, 2578,
        2578, 2585, 2589, 2603, 2603, 2613, 2616, 2648, 2651, 2662, 2666, 2667,
        2672, 2675, 2679, 2691, 2694, 2694, 2699, 2706, 2708, 2709, 2711, 2711,
        2732, 2736, 2738, 2749, 2750, 2763, 2764, 2770, 2775, 2781, 2793, 2811,
        2817, 2834, 2842, 2847, 2848, 2852, 2856, 2870, 2872, 2876, 2879, 2887,
        2897, 2903, 2980, 2984, 2994, 2997 };
    int set2[] = { 7, 21, 29, 31, 35, 37, 62, 64, 67, 72, 77, 88, 90, 96, 98,
        116, 152, 154, 156, 162, 163, 173, 179, 188, 189, 201, 203, 217, 224,
        233, 263, 267, 271, 277, 294, 301, 311, 336, 343, 349, 390, 395, 396,
        401, 407, 411, 414, 425, 432, 436, 444, 468, 476, 483, 492, 496, 497,
        501, 508, 513, 517, 519, 531, 541, 543, 552, 555, 555, 568, 571, 587,
        589, 594, 601, 604, 606, 625, 633, 634, 645, 649, 654, 655, 662, 664,
        665, 666, 671, 671, 678, 690, 693, 697, 708, 714, 723, 726, 743, 746,
        747, 772, 784, 806, 811, 812, 824, 834, 836, 844, 850, 863, 867, 890,
        890, 896, 905, 931, 933, 934, 940, 952, 959, 963, 968, 974, 978, 997,
        997, 1013, 1015, 1019, 1023, 1030, 1033, 1035, 1047, 1048, 1054, 1069,
        1087, 1147, 1156, 1158, 1165, 1175, 1199, 1211, 1224, 1252, 1255, 1256,
        1259, 1274, 1280, 1283, 1290, 1292, 1292, 1294, 1297, 1299, 1300, 1301,
        1312, 1323, 1337, 1340, 1351, 1352, 1356, 1363, 1385, 1392, 1395, 1399,
        1409, 1413, 1429, 1437, 1460, 1461, 1465, 1466, 1468, 1482, 1497, 1500,
        1501, 1508, 1517, 1524, 1524, 1529, 1530, 1538, 1538, 1544, 1545, 1552,
        1556, 1561, 1566, 1569, 1583, 1598, 1606, 1610, 1613, 1634, 1642, 1643,
        1656, 1675, 1682, 1704, 1708, 1711, 1711, 1719, 1724, 1736, 1740, 1741,
        1766, 1772, 1774, 1777, 1784, 1793, 1814, 1829, 1833, 1843, 1856, 1857,
        1870, 1874, 1879, 1884, 1886, 1890, 1901, 1909, 1912, 1940, 1944, 1946,
        1947, 1948, 1955, 1962, 1971, 1982, 1989, 1995, 1997, 2012, 2015, 2021,
        2043, 2046, 2049, 2055, 2064, 2068, 2069, 2083, 2088, 2100, 2117, 2122,
        2126, 2132, 2143, 2148, 2152, 2152, 2153, 2159, 2173, 2176, 2198, 2198,
        2201, 2205, 2206, 2207, 2211, 2222, 2230, 2254, 2256, 2264, 2268, 2317,
        2318, 2319, 2330, 2334, 2344, 2353, 2353, 2354, 2369, 2374, 2376, 2392,
        2402, 2403, 2414, 2417, 2422, 2424, 2435, 2445, 2461, 2475, 2530, 2539,
        2541, 2542, 2565, 2566, 2571, 2572, 2577, 2579, 2581, 2582, 2586, 2592,
        2595, 2600, 2642, 2645, 2645, 2651, 2668, 2676, 2699, 2705, 2705, 2709,
        2715, 2720, 2720, 2736, 2753, 2756, 2761, 2788, 2792, 2793, 2796, 2801,
        2815, 2834, 2842, 2857, 2859, 2859, 2861, 2865, 2869, 2875, 2879, 2884,
        2885, 2895, 2901, 2906, 2912, 2935, 2940, 2957, 2958, 2967, 2969, 2976,
        2978, 2981, 2984, 2994, 2997 };
    int set3[] = { 2994, 2997 };

    P4DDocIdSet pset1 = new P4DDocIdSet(batch);
    OpenBitSet pset2 = new OpenBitSet();
    P4DDocIdSet pset3 = new P4DDocIdSet(batch);

    for (int i = 0; i < set1.length; i++) {
      pset1.addDoc(set1[i]);
      pset2.set(set2[i]);

    }
    for (int i = 0; i < set3.length; i++) {
      pset3.addDoc(set3[i]);
    }

    List<DocIdSet> its = new ArrayList<DocIdSet>();
    its.add(pset1);
    its.add(pset2);
    its.add(pset3);
    AndDocIdSet andSet = new AndDocIdSet(its);
    org.apache.lucene.search.DocIdSetIterator dcit = andSet.iterator();
    int x = set1.length - 2;

    try {
      int docid;
      while ((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
        assertEquals(set1[x++], docid);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    
    System.out.println("Testing Skips");
    try {
     
      dcit = andSet.iterator();
      int docid = dcit.advance(94);
      assertEquals(2994,docid);
      docid = dcit.advance(102);
      assertEquals(2997,docid);
      //dcit.skipTo(920);
      //assertEquals(2994,dcit.doc());
      //dcit.skipTo(2994);
      //assertEquals(2994,dcit.doc());
      //dcit.skipTo(2996);
      //assertEquals(2997, dcit.doc());
      //dcit.skipTo(2997);
      //assertEquals(2997, dcit.doc());
      assertEquals(dcit.advance(450000),DocIdSetIterator.NO_MORE_DOCS);
      assertEquals(dcit.advance(2997),DocIdSetIterator.NO_MORE_DOCS);
      assertEquals(0, andSet.findWithIndex(2994));
 
    }
   catch (IOException e) {
    fail(e.getMessage());
  }

  }

  
  
  
  
  @Test
  public void testCombinationSanity()throws Exception {

    System.out.println("");
    int[] set1 = { 4, 19, 21, 35, 36, 43, 43, 73, 85, 104, 105, 106, 112, 118,
        119, 138, 141, 145, 146, 146, 196, 200, 202, 217, 219, 220, 221, 239,
        242, 243, 261, 276, 280, 281, 295, 297, 306, 309, 319, 324, 359, 375,
        376, 387, 398, 401, 406, 438, 442, 450, 450, 462, 469, 475, 495, 499,
        505, 505, 513, 513, 526, 529, 569, 584, 589, 590, 609, 614, 633, 635,
        635, 644, 646, 650, 657, 682, 685, 688, 692, 699, 704, 712, 714, 733,
        736, 739, 746, 748, 766, 768, 774, 776, 778, 786, 799, 801, 812, 814,
        818, 819, 831, 832, 836, 837, 837, 847, 864, 870, 872, 872, 875, 880,
        885, 899, 905, 914, 918, 928, 931, 932, 952, 954, 971, 981, 983, 986,
        992, 998, 1000, 1031, 1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093,
        1100, 1100, 1107, 1109, 1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167,
        1176, 1194, 1200, 1209, 1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246,
        1252, 1277, 1309, 1322, 1325, 1327, 1327, 1329, 1341, 1341, 1342, 1352,
        1359, 1360, 1361, 1363, 1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438,
        1441, 1448, 1449, 1451, 1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505,
        1546, 1555, 1556, 1572, 1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630,
        1638, 1652, 1663, 1664, 1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703,
        1707, 1708, 1708, 1716, 1720, 1720, 1723, 1724, 1727, 1727, 1730, 1733,
        1735, 1738, 1750, 1755, 1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830,
        1848, 1854, 1871, 1888, 1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set2 = { 4, 105, 141, 633, 1953, 1962, 1983, 1990, 1999 };
    int[] set3 = { 4, 145, 146, 146, 196, 200, 202, 217, 219, 1999 };
    int[] set4 = { 4, 200, 202, 217, 219, 220, 221, 239, 242, 243, 261, 276,
        280, 281, 295, 297, 306, 309, 319, 324, 359, 375, 376, 387, 398, 401,
        406, 438, 442, 450, 450, 462, 469, 475, 495, 499, 505, 505, 513, 513,
        526, 529, 569, 584, 589, 590, 609, 614, 633, 635, 635, 644, 646, 650,
        657, 682, 685, 688, 692, 699, 704, 712, 714, 733, 736, 739, 746, 748,
        766, 768, 774, 776, 778, 786, 799, 801, 812, 814, 818, 819, 831, 832,
        836, 837, 837, 847, 864, 870, 872, 872, 875, 880, 885, 899, 905, 914,
        918, 928, 931, 932, 952, 954, 971, 981, 983, 986, 992, 998, 1000, 1031,
        1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093, 1100, 1100, 1107, 1109,
        1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167, 1176, 1194, 1200, 1209,
        1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246, 1252, 1277, 1309, 1322,
        1325, 1327, 1327, 1329, 1341, 1341, 1342, 1352, 1359, 1360, 1361, 1363,
        1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438, 1441, 1448, 1449, 1451,
        1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505, 1546, 1555, 1556, 1572,
        1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630, 1638, 1652, 1663, 1664,
        1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703, 1707, 1708, 1708, 1716,
        1720, 1720, 1723, 1724, 1727, 1727, 1730, 1733, 1735, 1738, 1750, 1755,
        1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830, 1848, 1854, 1871, 1888,
        1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set5 = { 4, 1999 };
    int[] set6 = { 2000 };

    OpenBitSet ps1 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set1.length; i++)
      ps1.set(set1[i]);

    OpenBitSet ps2 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set2.length; i++)
      ps2.set(set2[i]);

    OpenBitSet ps3 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set3.length; i++)
      ps3.set(set3[i]);

    P4DDocIdSet ps4 = new P4DDocIdSet(128);

    // Build open bit set
    for (int i = 0; i < set4.length; i++)
      ps4.addDoc(set4[i]);

    OpenBitSet ps5 = new OpenBitSet();

    // Build open bit set
    for (int i = 0; i < set5.length; i++)
      ps5.set(set5[i]);

    P4DDocIdSet ps6 = new P4DDocIdSet(128);
    ps6.addDoc(2000);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    System.out.println("Running Combination Sanity test CASE 1");
    System.out
        .println("TEST CASE : Or first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");
    OrDocIdSet ord = new OrDocIdSet(sets);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps5);

    AndDocIdSet and = new AndDocIdSet(sets2);

    org.apache.lucene.search.DocIdSetIterator andit = and.iterator();

    int index = 0;
    try {
      int docid;
      while ((docid = andit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        if (set5[index++] != docid)
          System.err.println("Error in combination test: expected - "
              + set5[index - 1] + " but was - " + docid);
      }
    } catch (IOException e) {
       fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements: expected length",
        set5.length, index);
    System.out.println("Combination sanity CASE 1 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 2");
    System.out
        .println("TEST CASE : AND first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");

    AndDocIdSet and1 = new AndDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(and1);
    sets2.add(ps5);

    AndDocIdSet and2 = new AndDocIdSet(sets2);

    andit = and2.iterator();

    index = 0;
    try {
      int docid;
       while ((docid = andit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        if (set5[index++] != docid)
          System.err.println("Error in combination test: expected - "
              + set5[index - 1] + " but was - " + docid);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    assertEquals("Error: could not recover all and elements:", set5.length,
        index);
    System.out.println("Combination sanity CASE 2 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 3");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st should recover set1");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or3 = new OrDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);

    OrDocIdSet or4 = new OrDocIdSet(sets2);

    org.apache.lucene.search.DocIdSetIterator orit = or4.iterator();

    index = 0;
    int ctr = 0;
    try {
      int docid;
      while ((docid = orit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        index = ps1.nextSetBit(index);
        if (index == -1)
          System.err.println("Error in combination test: no value expected  but was - "
                  + docid);
        else if (index != docid)
          System.err.println("Error in combination test: expected - "
              + set1[index - 1] + " but was - " + docid);
        index++;
        ctr++;
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements:", ctr, (int) ps1
        .cardinality());
    System.out.println("Combination sanity CASE 3 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 4");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st and ~{2000} should recover 0-1999");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or5 = new OrDocIdSet(sets);
    NotDocIdSet not = new NotDocIdSet(ps6, 2001);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);
    sets2.add(not);

    OrDocIdSet or6 = new OrDocIdSet(sets2);

    orit = or6.iterator();

    index = 0;
    ctr = 0;

    try {
      int docid;
      while ((docid = orit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        assertEquals(index++, docid);
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements", set6[0], index);

    System.out.println("Combination sanity CASE 4 complete.");
    System.out.println();

  }

  

  @Test
  public void testCombinationSanityWithOBS() throws Exception{

    System.out.println("Runnning testCombinationSanityWithOBS");
    int[] set1 = { 4, 19, 21, 35, 36, 43, 73, 85, 104, 105, 106, 112, 118,
        119, 138, 141, 145, 146, 196, 200, 202, 217, 219, 220, 221, 239,
        242, 243, 261, 276, 280, 281, 295, 297, 306, 309, 319, 324, 359, 375,
        376, 387, 398, 401, 406, 438, 442, 450, 462, 469, 475, 495, 499,
        505, 513, 526, 529, 569, 584, 589, 590, 609, 614, 633, 635,
        644, 646, 650, 657, 682, 685, 688, 692, 699, 704, 712, 714, 733,
        736, 739, 746, 748, 766, 768, 774, 776, 778, 786, 799, 801, 812, 814,
        818, 819, 831, 832, 836, 837, 847, 864, 870, 872, 875, 880,
        885, 899, 905, 914, 918, 928, 931, 932, 952, 954, 971, 981, 983, 986,
        992, 998, 1000, 1031, 1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093,
        1100, 1107, 1109, 1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167,
        1176, 1194, 1200, 1209, 1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246,
        1252, 1277, 1309, 1322, 1325, 1327, 1329, 1341, 1342, 1352,
        1359, 1360, 1361, 1363, 1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438,
        1441, 1448, 1449, 1451, 1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505,
        1546, 1555, 1556, 1572, 1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630,
        1638, 1652, 1663, 1664, 1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703,
        1707,  1708, 1716, 1720, 1723, 1724, 1727, 1730, 1733,
        1735, 1738, 1750, 1755, 1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830,
        1848, 1854, 1871, 1888, 1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set2 = { 4, 105, 141, 633, 1953, 1962, 1983, 1990, 1999 };
    int[] set3 = { 4, 145, 146, 146, 196, 200, 202, 217, 219, 1999 };
    int[] set4 = { 4, 200, 202, 217, 219, 220, 221, 239, 242, 243, 261, 276,
        280, 281, 295, 297, 306, 309, 319, 324, 359, 375, 376, 387, 398, 401,
        406, 438, 442, 450, 450, 462, 469, 475, 495, 499, 505, 505, 513, 513,
        526, 529, 569, 584, 589, 590, 609, 614, 633, 635, 635, 644, 646, 650,
        657, 682, 685, 688, 692, 699, 704, 712, 714, 733, 736, 739, 746, 748,
        766, 768, 774, 776, 778, 786, 799, 801, 812, 814, 818, 819, 831, 832,
        836, 837, 837, 847, 864, 870, 872, 872, 875, 880, 885, 899, 905, 914,
        918, 928, 931, 932, 952, 954, 971, 981, 983, 986, 992, 998, 1000, 1031,
        1032, 1057, 1060, 1061, 1080, 1084, 1090, 1093, 1100, 1100, 1107, 1109,
        1115, 1116, 1139, 1148, 1150, 1159, 1162, 1167, 1176, 1194, 1200, 1209,
        1213, 1217, 1218, 1222, 1225, 1233, 1244, 1246, 1252, 1277, 1309, 1322,
        1325, 1327, 1327, 1329, 1341, 1341, 1342, 1352, 1359, 1360, 1361, 1363,
        1378, 1390, 1391, 1410, 1418, 1427, 1433, 1438, 1441, 1448, 1449, 1451,
        1471, 1488, 1489, 1490, 1500, 1503, 1504, 1505, 1546, 1555, 1556, 1572,
        1575, 1584, 1609, 1614, 1627, 1628, 1629, 1630, 1638, 1652, 1663, 1664,
        1665, 1674, 1686, 1688, 1689, 1692, 1702, 1703, 1707, 1708, 1708, 1716,
        1720, 1720, 1723, 1724, 1727, 1727, 1730, 1733, 1735, 1738, 1750, 1755,
        1758, 1767, 1775, 1786, 1803, 1810, 1812, 1830, 1848, 1854, 1871, 1888,
        1947, 1953, 1962, 1983, 1990, 1999 };
    int[] set5 = { 4, 1999 };
    int[] set6 = { 2000 };

    OBSDocIdSet ps1 = new OBSDocIdSet(3000);

    // Build open bit set
    for (int i = 0; i < set1.length; i++)
      ps1.addDoc(set1[i]);

    OBSDocIdSet ps2 = new OBSDocIdSet(3000);

    // Build open bit set
    for (int i = 0; i < set2.length; i++)
      ps2.addDoc(set2[i]);
    
    
    OBSDocIdSet ps3= new OBSDocIdSet(3000);

    // Build open bit set
    for (int i = 0; i < set3.length; i++)
      ps3.addDoc(set3[i]);

    OBSDocIdSet ps4 = new OBSDocIdSet(3000);

    // Build open bit set
    for (int i = 0; i < set4.length; i++)
      ps4.addDoc(set4[i]);

    OBSDocIdSet ps5 = new OBSDocIdSet(3000);

    // Build open bit set
    for (int i = 0; i < set5.length; i++)
      ps5.addDoc(set5[i]);

    OBSDocIdSet ps6 = new OBSDocIdSet(3000);
    ps6.addDoc(2000);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    System.out.println("Running Combination Sanity test CASE 1");
    System.out
        .println("TEST CASE : Or first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");
    OrDocIdSet ord = new OrDocIdSet(sets);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps5);

    AndDocIdSet and = new AndDocIdSet(sets2);

    org.apache.lucene.search.DocIdSetIterator andit = and.iterator();

    int index = 0;
    try {
      int docid;
      while ((docid = andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        if (set5[index++] != docid)
          System.err.println("Error in combination test: expected - "
              + set5[index - 1] + " but was - " + docid);
      }
    } catch (IOException e) {
       fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements: expected length",
        set5.length, index);
    System.out.println("Combination sanity CASE 1 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 2");
    System.out
        .println("TEST CASE : AND first 4 sets, AND with the 5th should recover set5");
    System.out.println("----------------------------");

    AndDocIdSet and1 = new AndDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(and1);
    sets2.add(ps5);

    AndDocIdSet and2 = new AndDocIdSet(sets2);

    andit = and2.iterator();

    index = 0;
    try {
      int docid;
      while ((docid = andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        if (set5[index++] != docid)
          System.err.println("Error in combination test: expected - "
              + set5[index - 1] + " but was - " + docid);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    assertEquals("Error: could not recover all and elements:", set5.length,
        index);
    System.out.println("Combination sanity CASE 2 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 3");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st should recover set1");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or3 = new OrDocIdSet(sets);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);

    OrDocIdSet or4 = new OrDocIdSet(sets2);

    org.apache.lucene.search.DocIdSetIterator orit = or4.iterator();

    index = 0;
    int ctr = 0;
    try {
      int docid;
      while ((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        index = ps1.findWithIndex(docid);
        assertFalse("Error in combination test: no value expected  but was - "
            + docid,index==-1);
        assertFalse("Error in combination test: expected - "
           + set1[index] + " but was - " + docid, set1[index] != docid);
         
        
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements:", set1.length, (int) ps1.size());
    System.out.println("Combination sanity CASE 3 complete.");
    System.out.println();

    System.out.println("Running Combination Sanity test CASE 4");
    System.out
        .println("TEST CASE : OR last 4 sets, OR with the 1st and ~{2000} should recover 0-1999");
    System.out.println("----------------------------");

    sets.clear();
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);

    OrDocIdSet or5 = new OrDocIdSet(sets);
    NotDocIdSet not = new NotDocIdSet(ps6, 2001);

    sets2 = new ArrayList<DocIdSet>();
    sets2.add(or3);
    sets2.add(ps1);
    sets2.add(not);

    OrDocIdSet or6 = new OrDocIdSet(sets2);

    orit = or6.iterator();

    index = 0;
    ctr = 0;

    try {
      int docid;
      while ((docid = orit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS) {
        assertEquals(index++, docid);
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }

    assertEquals("Error: could not recover all and elements", set6[0], index);

    System.out.println("Combination sanity CASE 4 complete.");
    System.out.println();

  }

  
  @Test
  public void testWideCombinationCase() throws Exception {
    System.out.println("");
    System.out.println("Running Wide Combination Test case...");
    System.out.println("----------------------------");

    int set0[] = { 9, 20, 31, 42, 65, 76, 87, 108, 119, 130, 141, 152, 163,
        186, 197, 208, 219, 230, 241, 265, 276, 287, 298, 309, 332, 343, 354,
        365, 376, 387, 410, 421, 431, 442, 453, 476, 487, 498, 509, 520, 531,
        554, 565, 575, 586, 597, 608, 619, 630, 653, 664, 675, 686, 697, 708,
        717, 728, 739, 750, 773, 784, 814, 820, 831, 842, 853 };
    int set1[] = { 8, 19, 30, 53, 64, 75, 86, 96, 107, 118, 129, 140, 151, 174,
        185, 196, 207, 218, 229, 252, 264, 275, 286, 297, 320, 331, 342, 353,
        364, 375, 398, 409, 420, 430, 441, 464, 475, 486, 497, 508, 519, 542,
        553, 564, 574, 585, 596, 607, 618, 641, 652, 663, 674, 685, 696, 716,
        727, 738, 761, 772, 783, 802, 813, 819, 830, 841 };
    int set2[] = { 7, 41, 52, 63, 74, 85, 106, 117, 128, 139, 162, 173, 184,
        195, 206, 217, 240, 251, 263, 274, 285, 308, 319, 330, 341, 352, 363,
        386, 397, 408, 419, 429, 452, 463, 474, 485, 496, 507, 530, 541, 552,
        563, 573, 584, 595, 606, 629, 640, 651, 662, 673, 684, 707, 715, 726,
        749, 760, 771, 782, 791, 801, 812, 818, 829, 852, 858 };
    int set3[] = { 6, 29, 40, 51, 62, 73, 84, 105, 116, 127, 150, 161, 172,
        183, 194, 205, 228, 239, 250, 262, 273, 296, 307, 318, 329, 340, 351,
        374, 385, 396, 407, 418, 440, 451, 462, 473, 484, 495, 518, 529, 540,
        551, 562, 572, 583, 594, 617, 628, 639, 650, 661, 672, 695, 706, 714,
        737, 748, 759, 770, 781, 790, 793, 800, 811, 840, 851 };
    int set4[] = { 17, 28, 39, 50, 61, 72, 95, 104, 115, 138, 149, 160, 171,
        182, 193, 216, 227, 238, 249, 260, 261, 284, 295, 306, 317, 328, 339,
        362, 373, 384, 395, 406, 417, 439, 450, 461, 472, 483, 506, 517, 528,
        539, 550, 561, 582, 605, 616, 627, 638, 649, 660, 683, 694, 705, 725,
        736, 747, 758, 769, 780, 789, 799, 810, 828, 839, 850 };
    int set5[] = { 5, 16, 27, 38, 49, 60, 83, 94, 103, 126, 137, 148, 159, 170,
        181, 204, 215, 226, 237, 248, 259, 272, 283, 294, 305, 316, 327, 350,
        361, 372, 383, 394, 405, 428, 438, 449, 460, 471, 494, 505, 516, 527,
        538, 549, 593, 604, 615, 626, 637, 648, 671, 682, 693, 704, 724, 735,
        746, 757, 768, 788, 792, 798, 809, 827, 838, 849 };
    int set6[] = { 4, 15, 26, 37, 48, 71, 82, 93, 114, 125, 136, 147, 158, 169,
        192, 203, 214, 225, 236, 247, 271, 282, 293, 304, 315, 338, 349, 360,
        371, 382, 393, 416, 427, 437, 448, 459, 482, 493, 504, 515, 526, 537,
        560, 571, 581, 592, 603, 614, 625, 636, 659, 670, 681, 692, 703, 723,
        734, 745, 756, 779, 787, 796, 797, 826, 837, 848 };
    int set7[] = { 3, 14, 25, 36, 59, 70, 81, 92, 102, 113, 124, 135, 146, 157,
        180, 191, 202, 213, 224, 235, 258, 270, 281, 292, 303, 326, 337, 348,
        359, 370, 381, 404, 415, 426, 436, 447, 470, 481, 492, 503, 514, 525,
        548, 559, 570, 580, 591, 602, 613, 624, 647, 658, 669, 680, 691, 702,
        722, 733, 744, 767, 778, 795, 808, 825, 836, 847 };
    int set8[] = { 2, 13, 24, 47, 58, 69, 80, 91, 101, 112, 123, 134, 145, 168,
        179, 190, 201, 212, 223, 246, 257, 269, 280, 291, 314, 325, 336, 347,
        358, 369, 392, 403, 414, 425, 435, 458, 469, 480, 491, 502, 513, 536,
        547, 558, 569, 579, 590, 601, 612, 635, 646, 657, 668, 679, 690, 713,
        721, 732, 755, 766, 777, 786, 794, 807, 824, 835 };
    int set9[] = { 1, 10, 12, 21, 32, 35, 43, 46, 54, 57, 68, 77, 79, 88, 90,
        97, 100, 111, 120, 122, 131, 133, 142, 153, 156, 164, 167, 175, 178,
        189, 198, 200, 209, 211, 220, 231, 234, 242, 245, 253, 256, 266, 268,
        277, 279, 288, 299, 302, 310, 313, 321, 324, 335, 344, 346, 355, 357,
        366, 377, 380, 388, 391, 399, 402, 413, 422, 424, 432, 443, 446, 454,
        457, 465, 468, 479, 488, 490, 499, 501, 510, 521, 524, 532, 535, 543,
        546, 557, 566, 568, 578, 587, 589, 598, 600, 609, 620, 623, 631, 634,
        642, 645, 656, 665, 667, 676, 678, 687, 698, 701, 709, 712, 718, 720,
        729, 740, 743, 751, 754, 762, 765, 776, 785, 803, 806, 817, 821, 823,
        832, 843, 846, 854 };
    int set10[] = { 23, 34, 45, 56, 67, 78, 99, 110, 121, 144, 155, 166, 177,
        188, 199, 222, 233, 244, 255, 267, 290, 301, 312, 323, 334, 345, 368,
        379, 390, 401, 412, 423, 434, 445, 456, 467, 478, 489, 512, 523, 534,
        545, 556, 567, 577, 588, 611, 622, 633, 644, 655, 666, 689, 700, 711,
        731, 742, 753, 764, 775, 805, 816, 834, 845, 856, 857 };
    int set11[] = { 11, 22, 33, 44, 55, 66, 89, 98, 109, 132, 143, 154, 165,
        176, 187, 210, 221, 232, 243, 254, 278, 289, 300, 311, 322, 333, 356,
        367, 378, 389, 400, 411, 433, 444, 455, 466, 477, 500, 511, 522, 533,
        544, 555, 576, 599, 610, 621, 632, 643, 654, 677, 688, 699, 710, 719,
        730, 741, 752, 763, 774, 804, 815, 822, 833, 844, 855 };

    int set12[] = { 857, 858 };

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
    OpenBitSet ps0 = new OpenBitSet();
    for (int i = 0; i < set0.length; i++)
      ps0.set(set0[i]);

    OpenBitSet ps1 = new OpenBitSet();
    for (int i = 0; i < set1.length; i++)
      ps1.set(set1[i]);

    OpenBitSet ps2 = new OpenBitSet();
    for (int i = 0; i < set2.length; i++)
      ps2.set(set2[i]);

    OpenBitSet ps3 = new OpenBitSet();
    for (int i = 0; i < set3.length; i++)
      ps3.set(set3[i]);

    OpenBitSet ps4 = new OpenBitSet();
    for (int i = 0; i < set4.length; i++)
      ps4.set(set4[i]);

    OpenBitSet ps5 = new OpenBitSet();
    for (int i = 0; i < set5.length; i++)
      ps5.set(set5[i]);

    OpenBitSet ps6 = new OpenBitSet();
    for (int i = 0; i < set6.length; i++)
      ps6.set(set6[i]);

    OpenBitSet ps7 = new OpenBitSet();
    for (int i = 0; i < set7.length; i++)
      ps7.set(set7[i]);

    OpenBitSet ps8 = new OpenBitSet();
    for (int i = 0; i < set8.length; i++)
      ps8.set(set8[i]);

    OpenBitSet ps9 = new OpenBitSet();
    for (int i = 0; i < set9.length; i++)
      ps9.set(set9[i]);

    OpenBitSet ps10 = new OpenBitSet();
    for (int i = 0; i < set10.length; i++)
      ps10.set(set10[i]);

    OpenBitSet ps11 = new OpenBitSet();
    for (int i = 0; i < set11.length; i++)
      ps11.set(set11[i]);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps0);
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);
    sets.add(ps6);
    sets.add(ps7);
    sets.add(ps8);
    sets.add(ps9);
    sets.add(ps10);
    sets.add(ps11);

    OrDocIdSet ord = new OrDocIdSet(sets);
    org.apache.lucene.search.DocIdSetIterator dcit = ord.iterator();

    int x = 0;
    try {
      int docid;
      while ((docid = dcit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        assertEquals(docid, result[x++]);
      }
    } catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    OpenBitSet ps12 = new OpenBitSet();
    for (int i = 0; i < set12.length; i++)
      ps12.set(set12[i]);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps12);

    AndDocIdSet andSet = new AndDocIdSet(sets2);
    org.apache.lucene.search.DocIdSetIterator andit = andSet.iterator();

    x = 0;
    try {
      int docid;
      while ((docid = andit.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
        assertEquals(set12[x++], docid);
    } catch (IOException e) {
      e.printStackTrace();

    }
    System.out.println("");

  }
  
  /*
  public void testDenseConstructionTime()
  {
    System.out.println("");
    System.out.println("Running Dense construction time test case...");
    System.out.println("----------------------------");
    
    for(int test=0;test<5;test++)
    {
        long time =  System.nanoTime();
        IntArrayDocIdSet docSet = new IntArrayDocIdSet(20000000);
        for(int i=0;i<20000000;i++)
        {
          docSet.addDoc(i);
        }
        System.out.println("Time for IntArray construction:"+(System.nanoTime()-time)+" ns");
        time =  System.nanoTime();
        OBSDocIdSet docSet2 = new OBSDocIdSet(20000000);
        for(int i=0;i<20000000;i++)
        {
          docSet2.addDoc(i);
        }
        System.out.println("Time for OpenBitSet construction:"+(System.nanoTime()-time)+" ns");
        time =  System.nanoTime();
        P4DDocIdSet docSet3 = new P4DDocIdSet();
        for(int i=0;i<20000000;i++)
        {
          docSet3.addDoc(i+test);
        }
        System.out.println("Time for P4D Set construction:"+(System.nanoTime()-time)+" ns");
      
    }
  }*/
  
  
  @Test
  public void testContainsCalls() throws Exception
  {
    System.out.println("");
    System.out.println("Running Find time test case...");
    System.out.println("----------------------------");
    
    for(int test=0;test<5;test++)
    {
        IntArrayDocIdSet docSet = new IntArrayDocIdSet(20000000);
        for(int i=0;i<20000000;i++)
        {
          docSet.addDoc(i);
        }
        long time =  System.nanoTime();
         for(int i=0;i<20000000;i+=5) 
           docSet.find(i);
         System.out.println("Time for"+(20000000/5)+ " IntArray Find:"+(System.nanoTime()-time)+" ns");
        time =  System.nanoTime();
        
        OBSDocIdSet docSet2 = new OBSDocIdSet(20000000);
        for(int i=0;i<20000000;i++)
        {
          docSet2.addDoc(i);
        }
        time =  System.nanoTime();
        for(int i=0;i<20000000;i+=5) 
          docSet2.find(i);
        System.out.println("Time for"+(20000000/5)+ " OBSDocSet Find:"+(System.nanoTime()-time)+" ns");
       
        time =  System.nanoTime();
        P4DDocIdSet docSet3 = new P4DDocIdSet();
        for(int i=0;i<20000000;i++)
        {
          docSet3.addDoc(i+5);
        }
        
        time =  System.nanoTime();
        for(int i=0;i<20000000;i+=6) 
          docSet3.find(i);
        System.out.println("Time for"+(20000000/6)+ " P4D Find:"+(System.nanoTime()-time)+" ns");
        
        
    }
  }

  @Test 
  public void testFindOnP4D()
  {
    System.out.println("");
    System.out.println("Running testFindOnP4D...");
    System.out.println("----------------------------");

    
    P4DDocIdSet docSet3 = new P4DDocIdSet();
    ArrayList<Integer> list = new ArrayList<Integer>();
    for(int i=0;i<20000000;i+=5)
    {
      list.add(i);
      docSet3.addDoc(i);
    }
    assertEquals(false,docSet3.find(3));
    
    for(Integer val: list)
    {
      assertEquals(true, docSet3.find(val));
      assertEquals(false, docSet3.find(val-1));
    }
    
    list.clear();
    docSet3 = new P4DDocIdSet();
    for(int i=0;i<20000000;i+=6)
    {
      list.add(i);
      docSet3.addDoc(i);
    }
    
    for(Integer val: list)
    {
      assertEquals(true, docSet3.find(val));
      assertEquals(false, docSet3.find(val+2));
    }
    
    list.clear();
    
    docSet3 = new P4DDocIdSet();
    assertFalse(docSet3.find(34));
    for(int i=1;i<257;i++)
    {
      list.add(i);
      
      docSet3.addDoc(i);
    }
    
    
    for(Integer val : list) 
    {
     
      assertEquals(true, docSet3.find(val));
      assertEquals(false, docSet3.find(val+258));
      assertEquals(false,docSet3.find(555));
      
    }
    assertEquals(false, docSet3.find(258));
    
    
 list.clear();
    
    docSet3 = new P4DDocIdSet();
    assertFalse(docSet3.find(34));
    for(int i=1;i<33;i++)
    {
      list.add(i);
      
      docSet3.addDoc(i);
    }
    
    
    for(Integer val : list) 
    {
     
      assertEquals(true, docSet3.find(val));
      assertEquals(false, docSet3.find(val+258));
      assertEquals(false,docSet3.find(555));
      
    }
    assertEquals(false, docSet3.find(258));
  
    
  }
  
  @Test
  public void testWideCombinationCase2() throws IOException {
    System.out.println("");
    System.out.println("Running Wide Combination Test case 2...");
    System.out.println("----------------------------");

    int set0[] = { 9, 20, 31, 42, 65, 76, 87, 108, 119, 130, 141, 152, 163,
        186, 197, 208, 219, 230, 241, 265, 276, 287, 298, 309, 332, 343, 354,
        365, 376, 387, 410, 421, 431, 442, 453, 476, 487, 498, 509, 520, 531,
        554, 565, 575, 586, 597, 608, 619, 630, 653, 664, 675, 686, 697, 708,
        717, 728, 739, 750, 773, 784, 814, 820, 831, 842, 853 };
    int set1[] = { 8, 19, 30, 53, 64, 75, 86, 96, 107, 118, 129, 140, 151, 174,
        185, 196, 207, 218, 229, 252, 264, 275, 286, 297, 320, 331, 342, 353,
        364, 375, 398, 409, 420, 430, 441, 464, 475, 486, 497, 508, 519, 542,
        553, 564, 574, 585, 596, 607, 618, 641, 652, 663, 674, 685, 696, 716,
        727, 738, 761, 772, 783, 802, 813, 819, 830, 841 };
    int set2[] = { 7, 18, 41, 52, 63, 74, 85, 106, 117, 128, 139, 162, 173,
        184, 195, 206, 217, 240, 251, 263, 274, 285, 308, 319, 330, 341, 352,
        363, 386, 397, 408, 419, 429, 452, 463, 474, 485, 496, 507, 530, 541,
        552, 563, 573, 584, 595, 606, 629, 640, 651, 662, 673, 684, 707, 715,
        726, 749, 760, 771, 782, 791, 801, 812, 818, 829, 852 };
    int set3[] = { 6, 29, 40, 51, 62, 73, 84, 105, 116, 127, 150, 161, 172,
        183, 194, 205, 228, 239, 250, 262, 273, 296, 307, 318, 329, 340, 351,
        374, 385, 396, 407, 418, 440, 451, 462, 473, 484, 495, 518, 529, 540,
        551, 562, 572, 583, 594, 617, 628, 639, 650, 661, 672, 695, 706, 714,
        737, 748, 759, 770, 781, 790, 793, 800, 811, 840, 851 };
    int set4[] = { 17, 28, 39, 50, 61, 72, 95, 104, 115, 138, 149, 160, 171,
        182, 193, 216, 227, 238, 249, 260, 261, 284, 295, 306, 317, 328, 339,
        362, 373, 384, 395, 406, 417, 439, 450, 461, 472, 483, 506, 517, 528,
        539, 550, 561, 582, 605, 616, 627, 638, 649, 660, 683, 694, 705, 725,
        736, 747, 758, 769, 780, 789, 799, 810, 828, 839, 850 };
    int set5[] = { 5, 16, 27, 38, 49, 60, 83, 94, 103, 126, 137, 148, 159, 170,
        181, 204, 215, 226, 237, 248, 259, 272, 283, 294, 305, 316, 327, 350,
        361, 372, 383, 394, 405, 428, 438, 449, 460, 471, 494, 505, 516, 527,
        538, 549, 593, 604, 615, 626, 637, 648, 671, 682, 693, 704, 724, 735,
        746, 757, 768, 788, 792, 798, 809, 827, 838, 849 };
    int set6[] = { 4, 15, 26, 37, 48, 71, 82, 93, 114, 125, 136, 147, 158, 169,
        192, 203, 214, 225, 236, 247, 271, 282, 293, 304, 315, 338, 349, 360,
        371, 382, 393, 416, 427, 437, 448, 459, 482, 493, 504, 515, 526, 537,
        560, 571, 581, 592, 603, 614, 625, 636, 659, 670, 681, 692, 703, 723,
        734, 745, 756, 779, 787, 796, 797, 826, 837, 848 };
    int set7[] = { 3, 14, 25, 36, 59, 70, 81, 92, 102, 113, 124, 135, 146, 157,
        180, 191, 202, 213, 224, 235, 258, 270, 281, 292, 303, 326, 337, 348,
        359, 370, 381, 404, 415, 426, 436, 447, 470, 481, 492, 503, 514, 525,
        548, 559, 570, 580, 591, 602, 613, 624, 647, 658, 669, 680, 691, 702,
        722, 733, 744, 767, 778, 795, 808, 825, 836, 847 };
    int set8[] = { 2, 13, 24, 47, 58, 69, 80, 91, 101, 112, 123, 134, 145, 168,
        179, 190, 201, 212, 223, 246, 257, 269, 280, 291, 314, 325, 336, 347,
        358, 369, 392, 403, 414, 425, 435, 458, 469, 480, 491, 502, 513, 536,
        547, 558, 569, 579, 590, 601, 612, 635, 646, 657, 668, 679, 690, 713,
        721, 732, 755, 766, 777, 786, 794, 807, 824, 835 };
    int set9[] = { 1, 10, 12, 21, 32, 35, 43, 46, 54, 57, 68, 77, 79, 88, 90,
        97, 100, 111, 120, 122, 131, 133, 142, 153, 156, 164, 167, 175, 178,
        189, 198, 200, 209, 211, 220, 231, 234, 242, 245, 253, 256, 266, 268,
        277, 279, 288, 299, 302, 310, 313, 321, 324, 335, 344, 346, 355, 357,
        366, 377, 380, 388, 391, 399, 402, 413, 422, 424, 432, 443, 446, 454,
        457, 465, 468, 479, 488, 490, 499, 501, 510, 521, 524, 532, 535, 543,
        546, 557, 566, 568, 578, 587, 589, 598, 600, 609, 620, 623, 631, 634,
        642, 645, 656, 665, 667, 676, 678, 687, 698, 701, 709, 712, 718, 720,
        729, 740, 743, 751, 754, 762, 765, 776, 785, 803, 806, 817, 821, 823,
        832, 843, 846, 854 };
    int set10[] = { 23, 34, 45, 56, 67, 78, 99, 110, 121, 144, 155, 166, 177,
        188, 199, 222, 233, 244, 255, 267, 290, 301, 312, 323, 334, 345, 368,
        379, 390, 401, 412, 423, 434, 445, 456, 467, 478, 489, 512, 523, 534,
        545, 556, 567, 577, 588, 611, 622, 633, 644, 655, 666, 689, 700, 711,
        731, 742, 753, 764, 775, 805, 816, 834, 845, 856, 857, 858 };
    int set11[] = { 11, 22, 33, 44, 55, 66, 89, 98, 109, 132, 143, 154, 165,
        176, 187, 210, 221, 232, 243, 254, 278, 289, 300, 311, 322, 333, 356,
        367, 378, 389, 400, 411, 433, 444, 455, 466, 477, 500, 511, 522, 533,
        544, 555, 576, 599, 610, 621, 632, 643, 654, 677, 688, 699, 710, 719,
        730, 741, 752, 763, 774, 804, 815, 822, 833, 844, 855 };
    int set12[] = { 857, 858 };

    int result[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
        72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
        90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
        106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
        120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
        134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147,
        148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161,
        162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
        176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189,
        190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203,
        204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217,
        218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231,
        232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245,
        246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259,
        260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273,
        274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287,
        288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301,
        302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315,
        316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329,
        330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343,
        344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357,
        358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371,
        372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385,
        386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399,
        400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413,
        414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427,
        428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441,
        442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455,
        456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469,
        470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483,
        484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497,
        498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511,
        512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525,
        526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539,
        540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553,
        554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567,
        568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581,
        582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595,
        596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609,
        610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623,
        624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637,
        638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651,
        652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 663, 664, 665,
        666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679,
        680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693,
        694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707,
        708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721,
        722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735,
        736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749,
        750, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763,
        764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777,
        778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791,
        792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805,
        806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819,
        820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833,
        834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847,
        848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858 };

    OBSDocIdSet ps0 = new OBSDocIdSet(1000);
    for (int i = 0; i < set0.length; i++)
      ps0.addDoc(set0[i]);

    OBSDocIdSet ps1 = new OBSDocIdSet(1000);
    for (int i = 0; i < set1.length; i++)
      ps1.addDoc(set1[i]);

    OBSDocIdSet ps2 = new OBSDocIdSet(1000);
    for (int i = 0; i < set2.length; i++)
      ps2.addDoc(set2[i]);

    OBSDocIdSet ps3 = new OBSDocIdSet(1000);
    for (int i = 0; i < set3.length; i++)
      ps3.addDoc(set3[i]);

    OBSDocIdSet ps4 = new OBSDocIdSet(1000);
    for (int i = 0; i < set4.length; i++)
      ps4.addDoc(set4[i]);

    OBSDocIdSet ps5 = new OBSDocIdSet(1000);
    for (int i = 0; i < set5.length; i++)
      ps5.addDoc(set5[i]);

    OpenBitSet ps6 = new OpenBitSet();
    for (int i = 0; i < set6.length; i++)
      ps6.set(set6[i]);

    OpenBitSet ps7 = new OpenBitSet();
    for (int i = 0; i < set7.length; i++)
      ps7.set(set7[i]);

    OpenBitSet ps8 = new OpenBitSet();
    for (int i = 0; i < set8.length; i++)
      ps8.set(set8[i]);

    P4DDocIdSet ps9 = new P4DDocIdSet(128);
    for (int i = 0; i < set9.length; i++)
      ps9.addDoc(set9[i]);

    OpenBitSet ps10 = new OpenBitSet();
    for (int i = 0; i < set10.length; i++)
      ps10.set(set10[i]);

    OpenBitSet ps11 = new OpenBitSet();
    for (int i = 0; i < set11.length; i++)
      ps11.set(set11[i]);

    ArrayList<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(ps0);
    sets.add(ps1);
    sets.add(ps2);
    sets.add(ps3);
    sets.add(ps4);
    sets.add(ps5);
    sets.add(ps6);
    sets.add(ps7);
    sets.add(ps8);
    sets.add(ps9);
    sets.add(ps10);
    sets.add(ps11);

    OrDocIdSet ord = new OrDocIdSet(sets);
    org.apache.lucene.search.DocIdSetIterator dcit = ord.iterator();

    int x = 0;

    int docid;
    while ((docid=dcit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      assertEquals(result[x++], docid);

    OpenBitSet ps12 = new OpenBitSet();
    for (int i = 0; i < set12.length; i++)
      ps12.set(set12[i]);

    ArrayList<DocIdSet> sets2 = new ArrayList<DocIdSet>();
    sets2.add(ord);
    sets2.add(ps12);

    AndDocIdSet andSet = new AndDocIdSet(sets2);
    org.apache.lucene.search.DocIdSetIterator andit = andSet.iterator();

    x = 0;

    while ((docid = andit.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      assertEquals(set12[x++], docid);

  }

  @Test
  public void testP4DDocIdSetNoExceptionCompressionRatio()
  {
    boolean failed = false;
    System.out.println("");
    System.out.println("Running P4DeltaDocSet No Exception Compression Ratio test");
    System.out.println("----------------------------");

    final int max = 100000;

    for(int j = 0; j < 31; j++)
    {
      try
      {
        P4DDocIdSet set = new P4DDocIdSet(batch);
        long time = System.nanoTime();
        
        int counter=0;
        for(int c = 0; c >= 0 && counter < max; c += (1 << j))
        {
          set.addDoc(c);
          counter++;
        }
        set.optimize();
        //System.out.println("Time to construct:"+(System.nanoTime() - time)+" ns");
        System.out.println("Delta:" + (1 << j) + " numOfItems:" + counter + " Blob Size:"+set.totalBlobSize());
      }
      catch(Exception ex)
      {
        System.out.println("Delta:" + (1 << j) + " Failed");
        failed = true;
      }
    }
    assertFalse("compresseion failed", failed);
  }
}
