////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.sevntu.checkstyle.checks.coding;

import static com.github.sevntu.checkstyle.checks.coding.ForbidCertainMethodsCheck.MSG_KEY_WITHOUT_ARG;
import static com.github.sevntu.checkstyle.checks.coding.ForbidCertainMethodsCheck.MSG_KEY_WITH_ARG;

import org.junit.Test;

import com.github.sevntu.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public class ForbidCertainMethodsCheckTest extends BaseCheckTestSupport {

    @Test
    public void testMethodCallToken() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("ExitCheck", "exit", null, "Execution of exit can shutdown the application leading to DoS attack."));
        checkConfig.addChild(createRuleConf("ExitCheck2", "exit2", "", "Execution of exit2 can shutdown the application leading to DoS attack."));
        checkConfig.addChild(createRuleConf("AssertTrue", "assert(True|False)", "1", "Assertion errors without helpful messages is not recommended."));
        checkConfig.addChild(createRuleConf("ForbiddenConstructor", "ForbiddenConstructor", "1", "ForbiddenConstructors should not be used."));

        final String[] expected = {
            "22:20: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit", "exit", "Execution of exit can shutdown the application leading to DoS attack."),
            "30:26: " + getCheckMessage(MSG_KEY_WITH_ARG, "assertTrue", "assert(True|False)", 1, "1", "Assertion errors without helpful messages is not recommended."),
            "32:31: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit2", "exit2", "Execution of exit2 can shutdown the application leading to DoS attack."),
        };
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    @Test
    public void testIncludeConstructor() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addAttribute("includeConstructor", "true");
        checkConfig.addChild(createRuleConf("ExitCheck", "exit", null, "Execution of exit can shutdown the application leading to DoS attack."));
        checkConfig.addChild(createRuleConf("ExitCheck2", "exit2", "", "Execution of exit2 can shutdown the application leading to DoS attack."));
        checkConfig.addChild(createRuleConf("AssertTrue", "assert(True|False)", "1", "Assertion errors without helpful messages is not recommended."));
        checkConfig.addChild(createRuleConf("ForbiddenConstructor", "ForbiddenConstructor", "1", "ForbiddenConstructors should not be used."));

        final String[] expected = {
            "22:20: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit", "exit", "Execution of exit can shutdown the application leading to DoS attack."),
            "23:53: " + getCheckMessage(MSG_KEY_WITH_ARG, "ForbiddenConstructor", "ForbiddenConstructor", 1, "1", "ForbiddenConstructors should not be used."),
            "30:26: " + getCheckMessage(MSG_KEY_WITH_ARG, "assertTrue", "assert(True|False)", 1, "1", "Assertion errors without helpful messages is not recommended."),
            "32:31: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit2", "exit2", "Execution of exit2 can shutdown the application leading to DoS attack."),
        };
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    @Test(expected = CheckstyleException.class)
    public void testBadValueOfIncludeConstructor() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addAttribute("includeConstructor", "neitherTrueNorFalse");
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test
    public void testEmptyConf() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);

        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testNullRuleName() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf(null, null, null,
                "Empty rule name"));
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testEmptyRuleName() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("", null, null,
                "Null name"));
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testNullMethodNameRegex() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("ExitCheck", null, null,
                "Null method name"));
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testEmptyMethodNameRegex() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("ExitCheck", "", null,
                "Empty method name"));
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testBadMethodNameRegex() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("ExitCheck", "[exit", null,
                "Execution of exit can shutdown the application leading to DoS attack."));
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), new String[]{});
    }

    @Test(expected = CheckstyleException.class)
    public void testBadArgumentCountRegex() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addChild(createRuleConf("ExitCheck", "exit", "[0",
                "Execution of exit can shutdown the application leading to DoS attack."));
        final String[] expected = {
        };
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    @Test(expected = CheckstyleException.class)
    public void testWithWrongConfig() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addAttribute("optional", "false");
        final String[] expected = {};
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    private Configuration createRuleConf(String name, String methodRegex, String argRegex, String reason) {
        final DefaultConfiguration conf = new DefaultConfiguration(name);
        conf.addAttribute("methodName", methodRegex);
        if (argRegex != null) {
            conf.addAttribute("argumentCount", argRegex);
        }
        if (reason != null) {
            conf.addAttribute("reason", reason);
        }
        return conf;
    }

}
