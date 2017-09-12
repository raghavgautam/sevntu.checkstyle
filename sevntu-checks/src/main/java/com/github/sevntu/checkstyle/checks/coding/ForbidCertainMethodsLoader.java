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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.puppycrawl.tools.checkstyle.api.AbstractLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

/**
 * Loads rules for checking method calls.
 *
 * @author Raghav Kumar Gautam
 */
public final class ForbidCertainMethodsLoader
        extends AbstractLoader {
    /**
     * The public ID for the configuration dtd.
     */
    private static final String DTD_PUBLIC_ID_1_0 =
            "-//Puppy Crawl//DTD Forbidden Methods 1.0//EN";
    /**
     * The resource for the configuration dtd.
     */
    private static final String DTD_RESOURCE_NAME_1_0 =
            "com/puppycrawl/tools/checkstyle/forbidden_methods_1_0.dtd";
    /**
     * File search error message.
     **/
    private static final String UNABLE_TO_FIND_ERROR_MESSAGE = "Unable to find: ";

    /**
     * The filter chain to return in getAFilterChain(),
     * configured during parsing.
     */
    private final List<ForbidCertainMethodsCheck.Rule> ruleList = new ArrayList<>();

    /**
     * Creates a new {@code SuppressionsLoader} instance.
     *
     * @throws ParserConfigurationException if an error occurs
     * @throws SAXException                 if an error occurs
     */
    private ForbidCertainMethodsLoader()
            throws ParserConfigurationException, SAXException {
        super(Collections.singletonMap(DTD_PUBLIC_ID_1_0, DTD_RESOURCE_NAME_1_0));
    }

    @Override
    public void startElement(String namespaceUri,
                             String localName,
                             String qName,
                             Attributes attributes)
            throws SAXException {
        if ("ForbiddenMethod".equals(qName)) {
            //add SuppressElement filter to the filter chain
            final String methodName = attributes.getValue("methodName");
            final String argCount = attributes.getValue("argCount");
            if (methodName == null) {
                // -@cs[IllegalInstantiation] SAXException is in the overridden method signature
                throw new SAXException("missing methodName attribute");
            }
            // ensure that regex is valid
            ruleList.add(ForbidCertainMethodsCheck.Rule.of(methodName, argCount));
        }
    }

    /**
     * Returns the suppression filters in a specified file.
     *
     * @param filename name of the suppression file.
     * @return the filter chain of suppression elements specified in the file.
     * @throws CheckstyleException if an error occurs.
     */
    static List<ForbidCertainMethodsCheck.Rule> loadConfigValue(String filename)
            throws CheckstyleException {
        final URI uri = CommonUtils.getUriByFilename(filename);
        final InputSource source = new InputSource(uri.toString());
        return loadConfigValue(source, filename);
    }

    /**
     * Returns the suppression filters in a specified source.
     *
     * @param source     the source for the suppressors.
     * @param sourceName the name of the source.
     * @return the filter chain of suppression elements in source.
     * @throws CheckstyleException if an error occurs.
     */
    private static List<ForbidCertainMethodsCheck.Rule> loadConfigValue(InputSource source,
            String sourceName) throws CheckstyleException {
        try {
            final ForbidCertainMethodsLoader suppressionsLoader = new ForbidCertainMethodsLoader();
            suppressionsLoader.parseInputSource(source);
            return suppressionsLoader.ruleList;
        }
        catch (final FileNotFoundException ex) {
            throw new CheckstyleException(UNABLE_TO_FIND_ERROR_MESSAGE + sourceName, ex);
        }
        catch (final ParserConfigurationException | SAXException ex) {
            final String message = String.format("Unable to parse %s - %s",
                    sourceName, ex.getMessage());
            throw new CheckstyleException(message, ex);
        }
        catch (final IOException ex) {
            throw new CheckstyleException("Unable to read " + sourceName, ex);
        }
        catch (final NumberFormatException ex) {
            final String message = String.format("Number format exception %s - %s",
                    sourceName, ex.getMessage());
            throw new CheckstyleException(message, ex);
        }
    }
}
