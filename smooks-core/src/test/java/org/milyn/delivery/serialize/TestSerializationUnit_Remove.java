/*
	Milyn - Copyright (C) 2003

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

package org.milyn.delivery.serialize;

import java.io.IOException;
import java.io.Writer;

import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.container.ContainerRequest;
import org.w3c.dom.Element;

/**
 * Test Serialization unit which removes an element from the delivered markup
 * by simply "not writing" the element to the output Writer.
 * @author tfennelly
 */
public class TestSerializationUnit_Remove extends  DefaultSerializationUnit {

	/**
	 * @param unitDef
	 */
	public TestSerializationUnit_Remove(SmooksResourceConfiguration unitDef) {
		super(unitDef);
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementStart(org.w3c.dom.Element, java.io.Writer, org.milyn.device.UAContext)
	 */
	public void writeElementStart(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
		// Remove by not printing the start or end of the element.
		// Leaves the child content.
	}

	/* (non-Javadoc)
	 * @see org.milyn.serialize.SerializationUnit#writeElementEnd(org.w3c.dom.Element, java.io.Writer, org.milyn.device.UAContext)
	 */
	public void writeElementEnd(Element element, Writer writer, ContainerRequest containerRequest) throws IOException {
		// Remove by not printing the start or end of the element.
		// Leaves the child content.
	}
}
