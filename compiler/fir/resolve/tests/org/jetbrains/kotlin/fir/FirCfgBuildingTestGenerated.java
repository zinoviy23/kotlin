/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@RunWith(JUnit3RunnerWithInners.class)
public class FirCfgBuildingTestGenerated extends AbstractFirCfgBuildingTest {
    @TestMetadata("compiler/fir/resolve/testData/resolve/cfg")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Cfg extends AbstractFirCfgBuildingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
        }

        public void testAllFilesPresentInCfg() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/fir/resolve/testData/resolve/cfg"), Pattern.compile("^([^.]+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("binaryOperations.kt")
        public void testBinaryOperations() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/binaryOperations.kt");
        }

        @TestMetadata("booleanOperatorsWithConsts.kt")
        public void testBooleanOperatorsWithConsts() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/booleanOperatorsWithConsts.kt");
        }

        @TestMetadata("complex.kt")
        public void testComplex() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/complex.kt");
        }

        @TestMetadata("emptyWhen.kt")
        public void testEmptyWhen() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/emptyWhen.kt");
        }

        @TestMetadata("initBlock.kt")
        public void testInitBlock() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/initBlock.kt");
        }

        @TestMetadata("initBlockAndInPlaceLambda.kt")
        public void testInitBlockAndInPlaceLambda() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/initBlockAndInPlaceLambda.kt");
        }

        @TestMetadata("jumps.kt")
        public void testJumps() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/jumps.kt");
        }

        @TestMetadata("lambdas.kt")
        public void testLambdas() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/lambdas.kt");
        }

        @TestMetadata("loops.kt")
        public void testLoops() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/loops.kt");
        }

        @TestMetadata("propertiesAndInitBlocks.kt")
        public void testPropertiesAndInitBlocks() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/propertiesAndInitBlocks.kt");
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/simple.kt");
        }

        @TestMetadata("tryCatch.kt")
        public void testTryCatch() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/tryCatch.kt");
        }

        @TestMetadata("when.kt")
        public void testWhen() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/cfg/when.kt");
        }
    }

    @TestMetadata("compiler/fir/resolve/testData/resolve/smartcasts")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Smartcasts extends AbstractFirCfgBuildingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
        }

        public void testAllFilesPresentInSmartcasts() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/fir/resolve/testData/resolve/smartcasts"), Pattern.compile("^([^.]+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("booleanOperators.kt")
        public void testBooleanOperators() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/booleanOperators.kt");
        }

        @TestMetadata("boundSmartcasts.kt")
        public void testBoundSmartcasts() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/boundSmartcasts.kt");
        }

        @TestMetadata("casts.kt")
        public void testCasts() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/casts.kt");
        }

        @TestMetadata("elvis.kt")
        public void testElvis() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/elvis.kt");
        }

        @TestMetadata("endlessLoops.kt")
        public void testEndlessLoops() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/endlessLoops.kt");
        }

        @TestMetadata("equalsAndIdentity.kt")
        public void testEqualsAndIdentity() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/equalsAndIdentity.kt");
        }

        @TestMetadata("equalsToBoolean.kt")
        public void testEqualsToBoolean() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/equalsToBoolean.kt");
        }

        @TestMetadata("implicitReceiverAsWhenSubject.kt")
        public void testImplicitReceiverAsWhenSubject() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/implicitReceiverAsWhenSubject.kt");
        }

        @TestMetadata("implicitReceivers.kt")
        public void testImplicitReceivers() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/implicitReceivers.kt");
        }

        @TestMetadata("inPlaceLambdas.kt")
        public void testInPlaceLambdas() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/inPlaceLambdas.kt");
        }

        @TestMetadata("nullability.kt")
        public void testNullability() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/nullability.kt");
        }

        @TestMetadata("returns.kt")
        public void testReturns() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/returns.kt");
        }

        @TestMetadata("simpleIf.kt")
        public void testSimpleIf() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/simpleIf.kt");
        }

        @TestMetadata("when.kt")
        public void testWhen() throws Exception {
            runTest("compiler/fir/resolve/testData/resolve/smartcasts/when.kt");
        }
    }
}
