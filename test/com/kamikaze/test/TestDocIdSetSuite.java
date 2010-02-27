package com.kamikaze.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { KamikazeTest.class, TestMultiThreadedAccess.class, TestDocSets.class, TestParameterizedDocSets.class, TestDocSetSerialization.class, TestDocSetFactory.class  })
public class TestDocIdSetSuite {
}
