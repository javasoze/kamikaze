package com.kamikaze.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
//@SuiteClasses( { PForDeltaKamikazeTest.class, TestKamikaze.class, TestMultiThreadedAccess.class, TestDocSets.class, TestParameterizedDocSets.class, TestDocSetSerialization.class, TestDocSetFactory.class  })
@SuiteClasses( { PForDeltaKamikazeTest.class, PForDeltaMultiThreadedAccessTest.class, TestBooleanDocIdSet.class, TestDocSetFactory.class, TestDocSets.class, TestDocSetSerialization.class, TestKamikaze.class, TestMultiThreadedAccess.class,  TestParameterizedDocSets.class})
//@SuiteClasses( { PForDeltaMultiThreadedAccessTest.class, TestMultiThreadedAccess.class})
public class TestDocIdSetSuite {
}

