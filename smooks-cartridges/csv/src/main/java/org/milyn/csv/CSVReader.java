/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/

package org.milyn.csv;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.xml.XMLConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.container.ExecutionContext;
import org.milyn.xml.SmooksXMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * CSV Reader.
 * <p/>
 * This CSV Reader can be plugged into the Smooks (for example) in order to convert a
 * CSV based message stream into a stream of SAX events to be consumed by the DOMBuilder.
 *
 * <h3>.cdrl Configuration</h3>
 * <pre>
 * &lt;resource-config selector="org.xml.sax.driver"&gt;
 *  &lt;resource&gt;org.milyn.csv.CSVReader&lt;/resource&gt;
 *  &lt;!--
 *      (Mandatory) Comma separated list of CSV record field names.
 *  --&gt;
 *  &lt;param name="<b>fields</b>"&gt;<i>&lt;csv-record-fields&gt;</i>&lt;/param&gt;
 *  &lt;!--
 *      (Optional) Field separator character.  Default of ','.
 *  --&gt;
 *  &lt;param name="<b>separator</b>"&gt;<i>&lt;separator-character&gt;</i>&lt;/param&gt;
 *  &lt;!--
 *      (Optional) Quote character.  Default of '"'.
 *  --&gt;
 *  &lt;param name="<b>quote-char</b>"&gt;<i>&lt;quote-character&gt;</i>&lt;/param&gt;
 *  &lt;!--
 *      (Optional) Number of lines to skip before processing starts.  Default of 0.
 *  --&gt;
 *  &lt;param name="<b>skip-line-count</b>"&gt;<i>&lt;skip-line-count&gt;</i>&lt;/param&gt;
 *
 * &lt;/resource-config&gt;
 * </pre>
 *
 * <h3>Example Usage</h3>
 * So the following configuration could be used to parse a CSV stream into
 * a stream of SAX events:
 * <pre>&lt;resource-config selector="org.xml.sax.driver"&gt;
 *  &lt;resource&gt;org.milyn.csv.CSVReader&lt;/resource&gt;
 *  &lt;param name="fields"&gt;name,address,item,quantity&lt;/param&gt;
 * &lt;/smooks-resource&gt;</pre>
 * <p/>
 * Within Smooks, the stream of SAX events generated by the "Acme-Order-List" message (and this parser) will generate
 * a DOM equivalent to the following:
 * <pre> &lt;csv-set&gt;
 * 	&lt;csv-record&gt;
 * 		&lt;name&gt;Tom Fennelly&lt;/name&gt;
 * 		&lt;address&gt;Ireland&lt;/address&gt;
 * 		&lt;item&gt;V1234&lt;/item&gt;
 * 		&lt;quantity&gt;3&lt;/quantity&gt;
 * 	&lt;csv-record&gt;
 * 	&lt;csv-record&gt;
 * 		&lt;name&gt;Joe Bloggs&lt;/name&gt;
 * 		&lt;address&gt;England&lt;/address&gt;
 * 		&lt;item&gt;D9123&lt;/item&gt;
 * 		&lt;quantity&gt;7&lt;/quantity&gt;
 * 	&lt;csv-record&gt;
 * &lt;/csv-set&gt;</pre>
 * <p/>
 * Other profile based transformations can then be used to transform the CSV records in accordance with the requirements
 * of the consuming entities.
 *
 * @author tfennelly
 */
public class CSVReader implements SmooksXMLReader {

	private static Log logger = LogFactory.getLog(CSVReader.class);
    private static Attributes EMPTY_ATTRIBS = new AttributesImpl();

    private ContentHandler contentHandler;
	private ExecutionContext request;

    @ConfigParam(name = "fields")
    private String[] csvFields;

    @ConfigParam(defaultVal = ",")
    private char separator;

    @ConfigParam(name = "quote-char", defaultVal = "\"")
    private char quoteChar;

    @ConfigParam(name = "skip-line-count", defaultVal = "0")
    private int skipLines;

    @ConfigParam(defaultVal = "UTF-8")
    private Charset encoding;

    @ConfigParam(defaultVal="csv-set")
    private String rootElementName;

    @ConfigParam(defaultVal="csv-record")
    private String recordElementName;

    /* (non-Javadoc)
	 * @see org.milyn.xml.SmooksXMLReader#setExecutionContext(org.milyn.container.ExecutionContext)
	 */
	public void setExecutionContext(ExecutionContext request) {
		this.request = request;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
	 */
	public void parse(InputSource csvInputSource) throws IOException, SAXException {
        if(contentHandler == null) {
            throw new IllegalStateException("'contentHandler' not set.  Cannot parse CSV stream.");
        }
        if(request == null) {
            throw new IllegalStateException("Smooks container 'request' not set.  Cannot parse CSV stream.");
        }

		Reader csvStreamReader;
		au.com.bytecode.opencsv.CSVReader csvLineReader;
        String[] csvRecord;

		// Get a reader for the CSV source...
        csvStreamReader = csvInputSource.getCharacterStream();
        if(csvStreamReader == null) {
            csvStreamReader = new InputStreamReader(csvInputSource.getByteStream(), encoding);
        }

        // Create the CSV line reader...
        csvLineReader = new au.com.bytecode.opencsv.CSVReader(csvStreamReader, separator, quoteChar, skipLines);

        // Start the document and add the root "csv-set" element...
        contentHandler.startDocument();
        contentHandler.startElement(XMLConstants.NULL_NS_URI, rootElementName, "", EMPTY_ATTRIBS);

        // Output each of the CVS line entries...
        int lineNumber = 0;
        while ((csvRecord = csvLineReader.readNext()) != null) {
        	lineNumber++; // First line is line "1"

        	if(csvRecord.length != csvFields.length) {
        		logger.warn("[CORRUPT-CSV] CSV line #" + lineNumber + " invalid [" + Arrays.asList(csvRecord) + "].  The line should contain the following " + csvFields.length + " fields [" + csvFields + "], but contains " + csvRecord.length + " fields.  Ignoring!!");
        		continue;
        	}

            contentHandler.startElement(XMLConstants.NULL_NS_URI, recordElementName, "", EMPTY_ATTRIBS);
        	for(int i = 0; i < csvRecord.length; i++) {
                String fieldName = csvFields[i];

                contentHandler.startElement(XMLConstants.NULL_NS_URI, fieldName, "", EMPTY_ATTRIBS);
                contentHandler.characters(csvRecord[i].toCharArray(), 0, csvRecord[i].length());
                contentHandler.endElement(XMLConstants.NULL_NS_URI, fieldName, "");
        	}
            contentHandler.endElement(null, recordElementName, "");
        }

        // Close out the "csv-set" root element and end the document..
        contentHandler.endElement(XMLConstants.NULL_NS_URI, rootElementName, "");
        contentHandler.endDocument();
	}

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    /****************************************************************************
     *
     * The following methods are currently unimplemnted...
     *
     ****************************************************************************/

    public void parse(String systemId) throws IOException, SAXException {
        throw new UnsupportedOperationException("Operation not supports by this reader.");
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        return false;
    }

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setDTDHandler(DTDHandler arg0) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setEntityResolver(EntityResolver arg0) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public Object getProperty(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        return null;
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
    }
}
