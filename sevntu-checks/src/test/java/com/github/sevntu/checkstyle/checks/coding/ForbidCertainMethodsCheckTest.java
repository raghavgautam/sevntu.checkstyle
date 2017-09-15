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

public class ForbidCertainMethodsCheckTest extends BaseCheckTestSupport {

    @Test
    public void testMethodCallTokenWithOptionalTrue() throws Exception {
        testMethodCallTokenInner("true");
    }

    @Test
    public void testMethodCallTokenWithOptionalFalse() throws Exception {
        testMethodCallTokenInner("false");
    }

    private void testMethodCallTokenInner(String optional) throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        final String forbiddenMethodConfigFile = getPath("InputForbidCertainMethodsCheck.xml");
        checkConfig.addAttribute("optional", optional);
        checkConfig.addAttribute("file", "file://" + forbiddenMethodConfigFile);
        final String[] expected = {
            "22:20: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit", "exit"),
            "23:53: " + getCheckMessage(MSG_KEY_WITH_ARG, "ForbiddenConstructor", "ForbiddenConstructor", 1, "1"),
            "30:26: " + getCheckMessage(MSG_KEY_WITH_ARG, "assertTrue", "assert(True|False)", 1, "1"),
            "32:31: " + getCheckMessage(MSG_KEY_WITHOUT_ARG, "exit2", "exit2"),
        };
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    @Test
    public void testOptionalWithNonExistingConfigOptionalTrue() throws Exception {
        //invalid url
        testOptionalWithNonExistingConfigInner("true", "file:\\non-existing-file.txt");
        //non existing file
        testOptionalWithNonExistingConfigInner("true", "file://non-existing-file.txt");
    }

    @Test(expected = CheckstyleException.class)
    public void testOptionalWithNonExistingConfigOptionalFalse() throws Exception {
        testOptionalWithNonExistingConfigInner("false", "file:///non-existing-file.txt");
    }

    private void testOptionalWithNonExistingConfigInner(String optional, String nonExistingFile) throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        checkConfig.addAttribute("file", nonExistingFile);
        checkConfig.addAttribute("optional", optional);
        final String[] expected = {};
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

    @Test(expected = CheckstyleException.class)
    public void testOptionalWithWrongConfig() throws Exception {
        final DefaultConfiguration checkConfig = createCheckConfig(ForbidCertainMethodsCheck.class);
        final String wrongConfigFile = getPath("InputForbidCertainMethodsCheck.java");
        checkConfig.addAttribute("file", "file://" + wrongConfigFile);
        checkConfig.addAttribute("optional", "false");
        final String[] expected = {};
        verify(checkConfig, getPath("InputForbidCertainMethodsCheck.java"), expected);
    }

}
