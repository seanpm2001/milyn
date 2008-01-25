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
package org.milyn.delivery;

import java.io.IOException;

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class TestEntityAndDTDHandler implements DTDHandler, EntityResolver {
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
    }

    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return null;
    }
    
    @Test
    public void test_dummy() {}
    
    public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter( TestEntityAndDTDHandler.class );
	}
    
    
}
