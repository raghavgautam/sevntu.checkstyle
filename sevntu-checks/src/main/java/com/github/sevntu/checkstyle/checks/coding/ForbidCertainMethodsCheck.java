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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Check that forbidden methods are not used. We can forbid methods by ruleName and number of
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
    /** Simple name of this class. */
    private static final String CLASS_NAME = ForbidCertainMethodsCheck.class.getSimpleName();

    /** If we want to include constructor as part of the checks. */
    private boolean includeConstructor;

    /**
     * Information about forbidden methods.
     */
    private List<Rule> rules = new ArrayList<>();

    /**
     * Set it to include constructor as part of the check.
     * @param includeConstructor true to include constructor false to exclude them.
     */
    public void setIncludeConstructor(boolean includeConstructor) {
        this.includeConstructor = includeConstructor;
    }

    /**
     * Enum for allowed key for the rules.
     */
    private enum AllowedKeyInConfig {
        /** Key for reason argument count part the rule. */
        METHOD_ARG_COUNT("argumentCount"),
        /** Key for method name part of the rule. */
        METHOD_NAME("methodName"),
        /** Key for reason for the rule. */
        REASON("reason");

        /** Configuration key name. */
        private String keyName;

        /**
         * Constructor for allowed key names.
         * @param keyName allowed key name
         */
        AllowedKeyInConfig(String keyName) {
            this.keyName = keyName;
        }
    }

    /**
     * Called by configure() for every child of this component's Configuration.
     * <p>
     * The default implementation throws {@link CheckstyleException} if
     * {@code childConf} is {@code null} because it doesn't support children. It
     * must be overridden to validate and support children that are wanted.
     * </p>
     *
     * @param childConf a child of this component's Configuration
     * @throws CheckstyleException if there is a configuration error.
     * @see Configuration#getChildren
     */

    protected void setupChild(Configuration childConf)
            throws CheckstyleException {
        if (childConf != null) {
            final String name = childConf.getName();
            final String[] attributeNames = childConf.getAttributeNames();
            for (String attributeName : attributeNames) {
                if (!Arrays.stream(AllowedKeyInConfig.values()).anyMatch(
                    item -> item.keyName.equals(attributeName))) {
                    throw new CheckstyleException(String.format(
                            "%s is not allowed an attribute in %s of %s",
                            attributeName, name, ForbidCertainMethodsCheck.class.getSimpleName()));
                }
            }
            String argCountRegex = null;
            if (Arrays.stream(childConf.getAttributeNames()).anyMatch(
                item -> item.equals(AllowedKeyInConfig.METHOD_ARG_COUNT.keyName))) {
                argCountRegex = childConf.getAttribute(AllowedKeyInConfig.METHOD_ARG_COUNT.keyName);
            }
            rules.add(new Rule(name,
                    childConf.getAttribute(AllowedKeyInConfig.METHOD_NAME.keyName),
                    argCountRegex,
                    childConf.getAttribute(AllowedKeyInConfig.REASON.keyName)));
        }
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
                if (!includeConstructor) {
                    break;
                }
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
     * @param name ruleName of the the method
     * @param argCount number of arguments of the method
     */
    private void checkForbiddenMethod(DetailAST ast, String name, int argCount) {
        final String argCountStr = Integer.toString(argCount);
        if (rules != null) {
            final Optional<Rule> firstViolatedRule = rules.stream().filter(
                rule -> {
                    return rule.matches(name, argCountStr);
                }).findFirst();
            if (firstViolatedRule.isPresent()) {
                final Rule violation = firstViolatedRule.get();
                if (violation.argCountRegex != null) {
                    log(ast.getLineNo(), ast.getColumnNo(), MSG_KEY_WITH_ARG,
                            name, violation.methodNameRegex,
                            argCount, violation.argCountRegex, violation.reason);
                }
                else {
                    log(ast.getLineNo(), ast.getColumnNo(), MSG_KEY_WITHOUT_ARG,
                            name, violation.methodNameRegex, violation.reason);
                }
            }
        }
    }

    /**
     * Class for keeping information about rules on the method calls.
     */
    static final class Rule {
        /** Name of the rule. */
        private final String ruleName;
        /** Name of the method. */
        private final Pattern methodNameRegex;
        /** Regex/String for number of arguments. */
        private final Pattern argCountRegex;
        /** Reason for the rule. */
        private final String reason;

        /**
         * Rule constructor.
         * @param ruleName name of the rule
         * @param methodNameRegex regex for method name
         * @param argCountRegex regex for argument count
         * @param reason reason for the rule
         * @throws CheckstyleException if regex is invalid
         */
        private Rule(String ruleName, String methodNameRegex, String argCountRegex, String reason)
                throws CheckstyleException {
            if (ruleName == null || ruleName.isEmpty()) {
                throw new CheckstyleException(String.format(
                        "%s is not allowed as name of rule of %s", ruleName, CLASS_NAME));
            }
            this.ruleName = ruleName;
            if (methodNameRegex == null || methodNameRegex.isEmpty()) {
                throw new CheckstyleException(String.format(
                        "empty regex (%s) is not allowed as method name regex in %s",
                        methodNameRegex, CLASS_NAME));
            }
            this.methodNameRegex = compileRegex(methodNameRegex,
                    "Invalid regex for matching method name: " + methodNameRegex);
            if (reason == null || reason.isEmpty()) {
                throw new CheckstyleException(String.format(
                        "reason(%s) is not allowed as a reason in %s", reason, CLASS_NAME));
            }
            this.reason = reason;
            // only argCountRegex can be blank
            if (argCountRegex == null || argCountRegex.isEmpty()) {
                this.argCountRegex = null;
            }
            else {
                this.argCountRegex = compileRegex(argCountRegex,
                        "Invalid regex for matching argument count: " + argCountRegex);
            }
        }

        /**
         * Return compiled regex for given regex string.
         * @param regex given regex
         * @param error the error message to use, if regex is bad
         * @return compiled regex
         * @throws CheckstyleException if regex is invalid
         */
        private Pattern compileRegex(String regex, String error) throws CheckstyleException {
            try {
                return Pattern.compile(regex);
            }
            catch (PatternSyntaxException ex) {
                throw new CheckstyleException(error, ex);
            }
        }

        /**
         * Check if this Rule matches the supplied rule.
         *
         * @param name ruleName of the the method
         * @param argCount number of arguments of the method
         * @return true if rule matches
         */
        public boolean matches(String name, String argCount) {
            final boolean methodNameMatches = methodNameRegex.matcher(name).matches();
            final boolean argCountMatches = argCountRegex == null
                    || argCountRegex.matcher(argCount).matches();
            return methodNameMatches && argCountMatches;
        }
    }
}
