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

package org.milyn.templating.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.annotation.Configurator;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.ContentHandler;
import org.milyn.delivery.ContentHandlerFactory;
import org.milyn.delivery.annotation.Resource;
import org.milyn.delivery.dom.serialize.ContextObjectSerializationUnit;
import org.milyn.javabean.BeanAccessor;
import org.milyn.templating.AbstractTemplateProcessingUnit;
import org.milyn.xml.DomUtils;
import org.milyn.event.report.annotation.VisitBeforeReport;
import org.milyn.event.report.annotation.VisitAfterReport;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * StringTemplate {@link org.milyn.delivery.dom.DOMElementVisitor} Creator class.
 * <p/>
 * Creates {@link org.milyn.delivery.dom.DOMElementVisitor} instances for applying
 * <a href="http://www.stringtemplate.org/">StringTemplate</a> transformations (i.e. ".st" files).
 * <p/>
 * This templating solution relies on the <a href="http://milyn.codehaus.org/downloads">Smooks JavaBean Cartridge</a>
 * to perform the JavaBean population that's required by <a href="http://www.stringtemplate.org/">StringTemplate</a>.
 *
 * <h2>Targeting ".st" Files for Transformation</h2>
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- See {@link org.milyn.resource.URIResourceLocator} --&gt;
 *     &lt;resource&gt;<b>/com/acme/AcmeStringTemplate.st</b>&lt;/resource&gt;
 *
 *     &lt;!-- (Optional) The action to be applied on the template content. Should the content
 *          generated by the template:
 *          1. replace ("replace") the target element, or
 *          2. be added to ("addto") the target element, or
 *          3. be inserted before ("insertbefore") the target element, or
 *          4. be inserted after ("insertafter") the target element.
 *          5. be bound to ("bindto") an ExecutionContext variable named by the "bindId" param.
 *          Default "replace".--&gt;
 *     &lt;param name="<b>action</b>"&gt;<i>replace/addto/insertbefore/insertafter</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) Should the template be applied before (true) or
 *             after (false) Smooks visits the child elements of the target element.
 *             Default "false".--&gt;
 *     &lt;param name="<b>applyTemplateBefore</b>"&gt;<i>true/false</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) Template encoding.
 *          Default "UTF-8".--&gt;
 *     &lt;param name="<b>encoding</b>"&gt;<i>encoding</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) bindId when "action" is "bindto".
 *     &lt;param name="<b>bindId</b>"&gt;<i>xxxx</i>&lt;/param&gt;
 *
 * &lt;/resource-config&gt;
 * </pre>
 *
 * @author tfennelly
 */
@Resource(type="st")
public class StringTemplateContentHandlerFactory implements ContentHandlerFactory {

	/**
	 * Create a StringTemplate based ContentHandler.
     * @param resourceConfig The SmooksResourceConfiguration for the StringTemplate.
     * @return The StringTemplate {@link org.milyn.delivery.ContentHandler} instance.
	 */
	public synchronized ContentHandler create(SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException, InstantiationException {
        try {
            return Configurator.configure(new StringTemplateTemplateProcessor(), resourceConfig);
        } catch (SmooksConfigurationException e) {
            throw e;
        } catch (Exception e) {
			InstantiationException instanceException = new InstantiationException("StringTemplate ProcessingUnit resource [" + resourceConfig.getResource() + "] not loadable.  StringTemplate resource invalid.");
			instanceException.initCause(e);
			throw instanceException;
		}
	}

	/**
	 * StringTemplate template application ProcessingUnit.
	 * @author tfennelly
	 */
    @VisitBeforeReport(condition = "false")
    @VisitAfterReport(summary = "Applied StringTemplate Template.", detailTemplate = "reporting/StringTemplateTemplateProcessor_After.html")
	private static class StringTemplateTemplateProcessor extends AbstractTemplateProcessingUnit {

        private StringTemplate template;

        protected void loadTemplate(SmooksResourceConfiguration config) {
            String path = config.getResource();

            if(path.charAt(0) == '/') {
                path = path.substring(1);
            }
            if(path.endsWith(".st")) {
                path = path.substring(0, path.length() - 3);
            }

            StringTemplateGroup templateGroup = new StringTemplateGroup(path);
            templateGroup.setFileCharEncoding(getEncoding().displayName());
            template = templateGroup.getInstanceOf(path);
        }

        protected void visit(Element element, ExecutionContext executionContext) {
            // First thing we do is clone the template for this transformation...
            StringTemplate thisTransTemplate = template.getInstanceOf();
            Map beans = BeanAccessor.getBeanMap(executionContext);
            String templatingResult;
            Node resultNode;

            // Set the document data beans on the template and apply it...
            thisTransTemplate.setAttributes(beans);
            templatingResult = thisTransTemplate.toString();

            if(getAction() != Action.ADDTO && element == element.getOwnerDocument().getDocumentElement()) {
                // We can't replace the root node with a text node (or insert before/after), so we need
                // to replace the root node with a <context-object key="xxx" /> element and bind the result to the
                // execution context under the specified key. The ContextObjectSerializationUnit will take
                // care of the rest.

                String key = "StringTemplateObject:" + DomUtils.getXPath(element);
                executionContext.setAttribute(key, templatingResult);
                resultNode = ContextObjectSerializationUnit.createElement(element.getOwnerDocument(), key);
            } else {
                // Create the replacement DOM text node containing the applied template...
                resultNode = element.getOwnerDocument().createTextNode(templatingResult);
            }

            // Process the templating action, supplying the templating result...
            processTemplateAction(element, resultNode);
        }
	}
}
