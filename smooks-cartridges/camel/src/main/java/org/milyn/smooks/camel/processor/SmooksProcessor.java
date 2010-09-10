/*
 * Milyn - Copyright (C) 2006 - 2010
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License (version 2.1) as published
 * by the Free Software Foundation.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU Lesser General Public License for more details:
 * http://www.gnu.org/licenses/lgpl.txt
 */
package org.milyn.smooks.camel.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.Exports;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.Visitor;
import org.milyn.delivery.VisitorAppender;
import org.milyn.event.report.HtmlReportGenerator;
import org.xml.sax.SAXException;

/**
 * Smooks {@link Processor} for Camel.
 * 
 * @version $Revision$
 * @author Christian Mueller
 * @author Daniel Bevenius
 */
public class SmooksProcessor implements Processor, Service
{
    public static final String SMOOKS_EXECUTION_CONTEXT = "CamelSmooksExecutionContext";
    private final Log log = LogFactory.getLog(SmooksProcessor.class);
    private Smooks smooks;
    private String configUri;
    private String reportPath;

    private Set<VisitorAppender> visitorAppenders = new HashSet<VisitorAppender>();
    private Map<String, Visitor> selectorVisitorMap = new HashMap<String, Visitor>();

    public SmooksProcessor(Smooks smooks)
    {
        this.smooks = smooks;
    }

    public SmooksProcessor(String configUri) throws IOException, SAXException
    {
        this.configUri = configUri;
    }

    public void process(Exchange exchange) throws Exception
    {
        ExecutionContext executionContext = smooks.createExecutionContext();
        executionContext.setAttribute(Exchange.class, exchange);
        exchange.getOut().setHeader(SMOOKS_EXECUTION_CONTEXT, executionContext);
        setupSmooksReporting(executionContext);

        Exports exports = Exports.getExports(smooks.getApplicationContext());
        if (exports.hasExports())
        {
            Result[] results = exports.createResults();
	        smooks.filterSource(executionContext, getSource(exchange), results);
	        setResultOnBody(exports, results, exchange);
        }
        else
		{
	        smooks.filterSource(executionContext, getSource(exchange));
        }
        
        executionContext.removeAttribute(Exchange.class);
    }
    
    protected void setResultOnBody(final Exports exports, final Result[] results, final Exchange exchange)
    {
        final Message message = exchange.getOut();
        List<Object> objects = Exports.extractResults(results, exports);
        if (objects.size() == 1)
        {
	        message.setBody(objects.get(0));
        }
        else
        {
	        message.setBody(objects);
        }
        
    }
    
    private void setupSmooksReporting(ExecutionContext executionContext)
    {
        if (reportPath != null)
        {
            try
            {
                executionContext.setEventListener(new HtmlReportGenerator(reportPath));
            } catch (IOException e)
            {
                log.info("Could not generate Smooks Report. The reportPath specified was [" + reportPath + "].", e);
            }
        }
    }

    private Source getSource(Exchange exchange)
    {
        return exchange.getIn().getBody(Source.class);
    }

    public String getSmooksConfig()
    {
        return configUri;
    }

    public void setSmooksConfig(String smooksConfig)
    {
        this.configUri = smooksConfig;
    }

    /**
     * Add a visitor instance.
     * 
     * @param visitor
     *            The visitor implementation.
     * @param targetSelector
     *            The message fragment target selector.
     * @return This instance.
     */
    public SmooksProcessor addVisitor(Visitor visitor, String targetSelector)
    {
        selectorVisitorMap.put(targetSelector, visitor);
        return this;
    }

    /**
     * Add a visitor instance to <code>this</code> Smooks instance via a
     * {@link VisitorAppender}.
     * 
     * @param appender
     *            The visitor appender.
     * @return This instance.
     */
    public SmooksProcessor addVisitor(VisitorAppender appender)
    {
        visitorAppenders.add(appender);
        return this;
    }

    public void setReportPath(String reportPath)
    {
        this.reportPath = reportPath;
    }

    public void start() throws Exception
    {
        if (smooks == null)
        {
            smooks = createSmooks(configUri);
        }
        addAppenders(smooks, visitorAppenders);
        addVisitors(smooks, selectorVisitorMap);
        log.info(this + " Started");
    }

    private Smooks createSmooks(String configUri) throws IOException, SAXException
    {
        if (smooks != null)
            return smooks;

        return new Smooks(configUri);
    }

    private void addAppenders(Smooks smooks, Set<VisitorAppender> appenders)
    {
        for (VisitorAppender appender : visitorAppenders)
            smooks.addVisitor(appender);
    }

    private void addVisitors(Smooks smooks, Map<String, Visitor> selectorVisitorMap)
    {
        for (Entry<String, Visitor> entry : selectorVisitorMap.entrySet())
            smooks.addVisitor(entry.getValue(), entry.getKey());
    }

    public void stop() throws Exception
    {
        if (smooks != null)
        {
            smooks.close();
            smooks = null;
        }
        log.info(this + " Stopped");
    }

    @Override
    public String toString()
    {
        return "SmooksProcessor [configUri=" + configUri + "]";
    }

}