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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

/**
 * Check that forbidden methods are not used. We can forbid methods by name and number of
 * arguments.
 * Because of limitations of how checkstyle works we can't add checks on type of the arguments.
 * This can be used to enforce things like:
 * <ul>
 * <li> System.exit() should not be called.</li>
 * <li> Assert.assertTrue() has a 1 arg variant that does not provide a helpful message on failure.
 * <li> Thread.sleep() can be prohibited.
 * </ul>
 * E.g.:
 * <p>
 * {@code
 * assertTrue(condition);
 * }
 * </p>
 *
 * @author <a href="mailto:raghavgautam@gmail.com">Raghav Kumar Gautam</a>
 */
public class ForbidCertainMethodsCheck extends AbstractCheck {

    /** Key is pointing to the warning message text in "messages.properties" file. */
    public static final String MSG_KEY_WITH_ARG = "forbid.certain.methods";
    /** Key is pointing to the warning message text in "messages.properties" file. */
    public static final String MSG_KEY_WITHOUT_ARG = "forbid.certain.methods.noarg";

    /** Filename of forbidden methods config file. */
    private String file;
    /** Tells whether config file existence is optional. */
    private boolean optional;

    /**
     * Information about forbidden methods.
     */
    private List<Rule> forbiddenMethods;

    /**
     * Sets name of the config file.
     *
     * @param fileName name of the forbidden methods config file.
     */
    public void setFile(String fileName) {
        file = fileName;
    }

    /**
     * Sets whether config file existence is optional.
     *
     * @param optional tells if config file existence is optional.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            TokenTypes.METHOD_CALL,
            TokenTypes.LITERAL_NEW,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    protected void finishLocalSetup() throws CheckstyleException {
        if (file != null) {
            if (optional) {
                if (configFileExists(file)) {
                    forbiddenMethods = ForbidCertainMethodsLoader.loadConfigValue(file);
                }
                else {
                    forbiddenMethods = new ArrayList<>();
                }
            }
            else {
                forbiddenMethods = ForbidCertainMethodsLoader.loadConfigValue(file);
            }
        }
    }

    /**
     * Checks if config file with given fileName exists.
     *
     * @param fileName name of the config file.
     * @return true if config file exists, otherwise false
     */
    private static boolean configFileExists(String fileName) {
        boolean configFileExists;
        try {
            final URI uriByFilename = CommonUtils.getUriByFilename(fileName);
            final URL url = uriByFilename.toURL();
            try (InputStream sourceInput = url.openStream()) {
                configFileExists = true;
            }
        }
        catch (CheckstyleException | IOException ignored) {
            configFileExists = false;
        }
        return configFileExists;
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.METHOD_CALL: {
                final DetailAST dot = ast.getFirstChild();
                // method that looks like: method()
                if (dot.getType() == TokenTypes.IDENT) {
                    final String methodName = dot.getText();
                    final int numParams = dot.getNextSibling().getChildCount(TokenTypes.EXPR);
                    checkForbiddenMethod(ast, methodName, numParams);
                    break;
                }
                // method that looks like: obj.method()
                else {
                    final String methodName = dot.getLastChild().getText();
                    final int numParams = dot.getNextSibling().getChildCount(TokenTypes.EXPR);
                    checkForbiddenMethod(ast, methodName, numParams);
                    break;
                }
            }
            // constructor
            case TokenTypes.LITERAL_NEW: {
                final DetailAST constructor = ast.getFirstChild();
                // case for java8 expression: File::new
                if (constructor == null) {
                    break;
                }
                final String constructorName = constructor.getText();
                final DetailAST arguments = ast.findFirstToken(TokenTypes.ELIST);
                // case for expression: int int[4]
                if (arguments == null) {
                    break;
                }
                final int numberOfParameters = arguments.getChildCount(TokenTypes.EXPR);
                checkForbiddenMethod(ast, constructorName, numberOfParameters);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Check if the method/constructor call against defined rules.
     * @param ast ast for the call
     * @param name name of the the method
     * @param argCount number of arguments of the method
     */
    private void checkForbiddenMethod(DetailAST ast, String name, int argCount) {
        final String argCountStr = Integer.toString(argCount);
        if (forbiddenMethods != null) {
            final Optional<Rule> firstViolatedRule = forbiddenMethods.stream().filter(
                rule -> {
                    return rule.matches(name, argCountStr);
                }).findFirst();
            if (firstViolatedRule.isPresent()) {
                final Rule violation = firstViolatedRule.get();
                if (violation.argCountRegex != null) {
                    log(ast.getLineNo(), ast.getColumnNo(), MSG_KEY_WITH_ARG,
                            name, violation.nameRegex,
                            argCount, violation.argCountRegex);
                }
                else {
                    log(ast.getLineNo(), ast.getColumnNo(), MSG_KEY_WITHOUT_ARG,
                            name, violation.nameRegex);
                }
            }
        }
    }

    /**
     * Class for keeping information about method calls or rules on the method calls.
     */
    static final class Rule {
        /**
         * Name of the method.
         */
        private final Pattern nameRegex;
        /**
         * Regex/String for number of arguments.
         */
        private final Pattern argCountRegex;

        /**
         * Rule constructor.
         *
         * @param name name of the the method
         * @param argCount number of arguments of the method
         */
        private Rule(String name, String argCount) {
            this.nameRegex = Pattern.compile(name);
            if (argCount == null || argCount.isEmpty()) {
                this.argCountRegex = null;
            }
            else {
                this.argCountRegex = Pattern.compile(argCount);
            }
        }

        /**
         * Factory method for creating Rule instance.
         *
         * @param methodName nameRegex of the method
         * @param argCount   number of arguments
         * @return instance of Rule
         */
        public static Rule of(String methodName, String argCount) {
            return new Rule(methodName, argCount);
        }

        /**
         * Check if this Rule matches the supplied rule.
         *
         * @param name name of the the method
         * @param argCount number of arguments of the method
         * @return true if rule matches
         */
        public boolean matches(String name, String argCount) {
            return nameRegex.matcher(name).matches()
                    && (argCountRegex == null || argCountRegex.matcher(argCount).matches());
        }
    }
}
