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

package org.milyn.container.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.cdr.SmooksResourceConfigurationStore;
import org.milyn.container.ContainerContext;
import org.milyn.device.profile.DefaultProfileConfigDigester;
import org.milyn.device.profile.ProfileStore;
import org.milyn.ioc.BeanFactory;
import org.milyn.resource.ContainerResourceLocator;
import org.xml.sax.SAXException;

/**
 * Standalone container execution context for Smooks.
 * <p/>
 * This context allows Smooks to be executed outside the likes of a 
 * Servlet Container.
 * @author tfennelly
 */
public class StandaloneContainerContext implements ContainerContext {

    private static Log logger = LogFactory.getLog(StandaloneContainerContext.class);
	private static final String DEVICE_PROFILE_XML = "/device-profile.xml";
	private Hashtable attributes = new Hashtable();
	private Hashtable sessions = new Hashtable();
	private ContainerResourceLocator resourceLocator;	
	private SmooksResourceConfigurationStore resStore;
	private ProfileStore profileStore;
    
    /**
     * Public constructor.
     */
    public StandaloneContainerContext() {
        resourceLocator = (ContainerResourceLocator)BeanFactory.getBean("standaloneResourceLocator");
        resStore = new SmooksResourceConfigurationStore(this);
        initProfileStore();
    }

	/**
	 * Public constructor.
	 * <p/>
	 * Context instances constructed in this way can be populated manually with
	 * {@link org.milyn.device.profile.Profile} and {@link org.milyn.cdr.SmooksResourceConfiguration}
	 * info.  This supports non-XML type configuration.
	 * @param profileStore The {@link ProfileStore} for tis context.
	 * @param resourceLocator The {@link ContainerResourceLocator} for this context.
	 */
	public StandaloneContainerContext(ProfileStore profileStore, ContainerResourceLocator resourceLocator) {
        this();
		if(profileStore == null) {
			throw new IllegalArgumentException("null 'profileStore' arg in constructor call.");
		}
		if(resourceLocator == null) {
			throw new IllegalArgumentException("null 'resourceLocator' arg in constructor call.");
		}
		
		this.profileStore = profileStore;
		this.resourceLocator = resourceLocator;
	}
	
	private void initProfileStore() {
		ContainerResourceLocator resLocator = getResourceLocator();
		DefaultProfileConfigDigester profileDigester = new DefaultProfileConfigDigester();
		InputStream configStream;
		try {
			configStream = resLocator.getResource("device-profiles", DEVICE_PROFILE_XML);
            if(configStream != null) {            
                profileStore = profileDigester.parse(configStream);
            } else {
                logger.warn("Device profile config file [" + DEVICE_PROFILE_XML + "] not available from container.");
            }
		} catch (IOException e) {
			IllegalStateException state = new IllegalStateException("Unable to read [" + DEVICE_PROFILE_XML + "] from container context.");
			state.initCause(e);
			throw state;
		} catch (SAXException e) {
			IllegalStateException state = new IllegalStateException("SAX excepting parsing [" + DEVICE_PROFILE_XML + "].");
			state.initCause(e);
			throw state;
		}
	}

	/* (non-Javadoc)
	 * @see org.milyn.container.BoundAttributeStore#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	/* (non-Javadoc)
	 * @see org.milyn.container.BoundAttributeStore#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	/* (non-Javadoc)
	 * @see org.milyn.container.BoundAttributeStore#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public ContainerResourceLocator getResourceLocator() {
		return resourceLocator;
	}
    public void setResourceLocator(ContainerResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

	public SmooksResourceConfigurationStore getStore() {
		return resStore;
	}

	/**
	 * Get the ProfileStore in use within the Standalone Context.
	 * @return The ProfileStore.
	 */
	public ProfileStore getProfileStore() {
		return profileStore;
	}

	/**
	 * Get a session instance for the specified useragent.
	 * @param useragent Useragent identification.
	 * @return Standalone Session instance.
	 */
	public StandaloneContainerSession getSession(String useragent) {
		StandaloneContainerSession session;
		
		if(useragent == null || useragent.trim().equals("")) {
			throw new IllegalArgumentException("null or empty 'useragent' arg in method call.");
		}
		session = (StandaloneContainerSession) sessions.get(useragent);
		if(session == null) {
			// create a new session for the requesting useragent
			session = new StandaloneContainerSession(useragent, this);
			sessions.put(useragent, session);
		}
		
		return session;
	}

}
