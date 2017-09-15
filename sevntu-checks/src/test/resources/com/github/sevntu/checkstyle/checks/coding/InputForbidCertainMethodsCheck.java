////////////////////////////////////////////////////////////////////////////////
// Test case file for checkstyle.
// Created: 2017
////////////////////////////////////////////////////////////////////////////////
package com.github.sevntu.checkstyle.checks.coding;

import org.junit.Assert;

import java.io.File;
import java.util.Arrays;

/**
 * Test case for detecting usage of forbidden methods & constructors.
 * @author Raghav Kumar Gautam
 **/
class InputForbidCertainMethodsCheck
{
    /**
     * no param constructor
     */
    InputForbidCertainMethodsCheck() {
        System.exit(1);
        ForbiddenConstructor forbiddenConstructor = new ForbiddenConstructor("oneArgument");
    }

    /**
     * non final param method
     */
    void method(String s) {
        Assert.assertTrue(1 != 2);
        Assert.assertTrue("Good assert with some reason.", true);
        ForbiddenMethods.exit2();
        //method call that does not need "." in it
        method("");
        // new usage that does not invoke constructor
        String[] strs = new String[4];
        Arrays.stream(strs).map(File::new).count();
    }

    private class ForbiddenConstructor {
        ForbiddenConstructor(String str) {
        }
    }

    private static class ForbiddenMethods {
        static void exit2() {
        }
    }
}
