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

package org.milyn.cdr;

import java.io.IOException;
import java.util.List;

import org.xml.sax.SAXException;
import org.milyn.profile.ProfileSet;
import org.milyn.profile.DefaultProfileStore;
import org.milyn.Smooks;
import org.milyn.SmooksUtil;
import org.milyn.container.ExecutionContext;

import junit.framework.TestCase;

/**
 * Unit tests forthe ArciveDef class.
 * @author tfennelly
 */
public class XMLConfigDigesterTest extends TestCase {

	public void test_digestConfig_v10() throws SAXException, IOException {
		// Valid doc
        SmooksResourceConfigurationList resList = XMLConfigDigester.digestConfig("test", getClass().getResourceAsStream("testconfig1.cdrl"));
        
        assertResourceConfigOK(resList);
	}

    public void test_digestConfig_v20() throws SAXException, IOException {
        // Valid doc
        SmooksResourceConfigurationList resList = XMLConfigDigester.digestConfig("test", getClass().getResourceAsStream("testconfig2.cdrl"));

        assertResourceConfigOK(resList);

        // Check the profiles...
        List<ProfileSet> profiles = resList.getProfiles();
        assertEquals(2, profiles.size());
        assertEquals("profileA", profiles.get(0).getBaseProfile());
        assertTrue(profiles.get(0).isMember("profileA"));
        assertTrue(profiles.get(0).isMember("profile1"));
        assertTrue(profiles.get(0).isMember("profile2"));
        assertTrue(!profiles.get(0).isMember("profile100"));
        assertEquals("profileB", profiles.get(1).getBaseProfile());
        assertTrue(profiles.get(1).isMember("profile3"));
        assertTrue(profiles.get(1).isMember("profileA"));
        assertTrue(!profiles.get(1).isMember("profile1")); // not expanded
    }

    public void test_profile_expansion() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("testconfig2.cdrl"));

        assertProfilesOK(smooks);
        // register the same resources again - including the same profiles...
        SmooksUtil.registerResources("x", getClass().getResourceAsStream("testconfig2.cdrl"), smooks);
        assertProfilesOK(smooks);
    }

    private void assertProfilesOK(Smooks smooks) {
        ExecutionContext execContext;
        execContext = smooks.createExecutionContext("profileA");
        ProfileSet profileA = execContext.getTargetProfiles();
        assertTrue(profileA.isMember("profileA"));
        assertTrue(profileA.isMember("profile1"));
        assertTrue(profileA.isMember("profile2"));
        assertTrue(!profileA.isMember("profileB"));
        assertTrue(!profileA.isMember("profile3"));

        execContext = smooks.createExecutionContext("profileB");
        ProfileSet profileB = execContext.getTargetProfiles();
        assertTrue(profileB.isMember("profileB"));
        assertTrue(profileB.isMember("profile3"));
        assertTrue(profileB.isMember("profileA"));
        assertTrue(profileB.isMember("profile1"));
        assertTrue(profileB.isMember("profile2"));
    }

    private void assertResourceConfigOK(SmooksResourceConfigurationList resList) {
        assertEquals(3, resList.size());

        // Test the overridden attribute values from the 1st config entry.
        assertEquals("a", resList.get(0).getSelector());
        assertEquals("xxx", resList.get(0).getUseragentExpressions()[0].getExpression());
        assertEquals("x.txt", resList.get(0).getResource());
        assertEquals("http://milyn.codehaus.org/smooks", resList.get(0).getNamespaceURI());

        // Test the default inherited attribute values from the 2nd config entry.
        assertEquals("b", resList.get(1).getSelector());
        assertEquals("yyy", resList.get(1).getUseragentExpressions()[0].getExpression());
        assertEquals("/org/milyn/cdr/test-resource.txt", resList.get(1).getResource());
        assertEquals("Hi there :-)", new String(resList.get(1).getBytes()));
        assertEquals("http://milyn.codehaus.org/smooks-default", resList.get(1).getNamespaceURI());

        // Test the parameters on the 2nd config entry.
        assertEquals("param1Val", resList.get(1).getStringParameter("param1"));
        assertEquals(true, resList.get(1).getBoolParameter("param2", false));
        assertEquals(false, resList.get(1).getBoolParameter("param3", true));
        assertEquals(false, resList.get(1).getBoolParameter("param4", false));

        // Test the 3rd config entry.
        assertEquals("abc", resList.get(2).getResourceType());
        assertEquals("Howya", new String(resList.get(2).getBytes()));
    }

    
}
