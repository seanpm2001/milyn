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

package org.milyn.cdres.trans;

import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

public class BaseTransUnitsTest extends TestCase {

	public void test_RenameAttributeTU() {
		Document doc = parseCPResource("testpage1.html");
		SmooksResourceConfiguration SmooksResourceConfiguration = new SmooksResourceConfiguration("body", "device", "xxx");
		Element body = (Element)XmlUtil.getNode(doc, "/html/body");
		RenameAttributeTU tu;

		SmooksResourceConfiguration.setParameter("attributeName", "attrib1");
		SmooksResourceConfiguration.setParameter("attributeNewName", "attrib2");
		tu = new RenameAttributeTU(SmooksResourceConfiguration);
		tu.visit(body, null);
		assertEquals("Default overwrite protection failed.", "value2", body.getAttribute("attrib2"));

		SmooksResourceConfiguration.setParameter("overwrite", "true");
		tu = new RenameAttributeTU(SmooksResourceConfiguration);
		tu.visit(body, null);
		assertFalse("Rename failed to remove target attribute.", body.hasAttribute("attrib1"));
		assertEquals("Overwrite failed.", "value1", body.getAttribute("attrib2"));
	}

	public void test_RemoveAttributeTU() {
		Document doc = parseCPResource("testpage1.html");
		SmooksResourceConfiguration SmooksResourceConfiguration = new SmooksResourceConfiguration("body", "device", "xxx");
		Element body = (Element)XmlUtil.getNode(doc, "/html/body");
		RemoveAttributeTU tu;

		SmooksResourceConfiguration.setParameter("attributeName", "attrib1");
		tu = new RemoveAttributeTU(SmooksResourceConfiguration);

		assertTrue("XPath failed - test corrupted.", body.hasAttribute("attrib1"));
		tu.visit(body, null);
		assertFalse("Failed to remove target attribute.", body.hasAttribute("attrib1"));
	}

	public void test_RenameElementTU() {
		Document doc = parseCPResource("testpage1.html");
		SmooksResourceConfiguration SmooksResourceConfiguration = new SmooksResourceConfiguration("body", "device", "xxx");
		Element body = (Element)XmlUtil.getNode(doc, "/html/body");
		RenameElementTU tu;

		SmooksResourceConfiguration.setParameter("replacementElement", "head");
		tu = new RenameElementTU(SmooksResourceConfiguration);

		tu.visit(body, null);
		assertNull("Failed to rename target element.", XmlUtil.getNode(doc, "/html/body"));
		assertNotNull("Failed to rename target element.", XmlUtil.getNode(doc, "/html/head"));
	}

	public void test_RemoveElementTU() {
		Document doc = parseCPResource("testpage1.html");
		SmooksResourceConfiguration SmooksResourceConfiguration = new SmooksResourceConfiguration("body", "device", "xxx");
		Element body = (Element)XmlUtil.getNode(doc, "/html/body");
		RemoveElementTU tu;

		tu = new RemoveElementTU(SmooksResourceConfiguration);

		tu.visit(body, null);
		assertNull("Failed to remove target element.", XmlUtil.getNode(doc, "/html/body"));
	}

	public void test_SetAttributeTU() {
		Document doc = parseCPResource("testpage1.html");
		SmooksResourceConfiguration SmooksResourceConfiguration = new SmooksResourceConfiguration("body", "device", "xxx");
		Element body = (Element)XmlUtil.getNode(doc, "/html/body");
		SetAttributeTU tu;

		SmooksResourceConfiguration.setParameter("attributeName", "attrib1");
		SmooksResourceConfiguration.setParameter("attributeValue", "value3");
		tu = new SetAttributeTU(SmooksResourceConfiguration);
		tu.visit(body, null);
		assertEquals("Default overwrite protection failed.", "value1", body.getAttribute("attrib1"));

		SmooksResourceConfiguration.setParameter("overwrite", "true");
		tu = new SetAttributeTU(SmooksResourceConfiguration);
		tu.visit(body, null);
		assertEquals("Overwrite failed.", "value3", body.getAttribute("attrib1"));
	}
	
	
	public Document parseCPResource(String classpath) {
		try {
			return XmlUtil.parseStream(getClass().getResourceAsStream(classpath), false, true);
		} catch (Exception e) {
			e.printStackTrace();
			TestCase.fail(e.getMessage());
		}
		return null;
	}
}
