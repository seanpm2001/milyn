/*
 * Milyn - Copyright (C) 2006
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
package org.milyn.routing.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.SmooksException;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.cdr.annotation.ConfigParam.Use;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.annotation.Initialize;
import org.milyn.delivery.annotation.VisitAfterIf;
import org.milyn.delivery.annotation.VisitBeforeIf;
import org.milyn.delivery.dom.DOMElementVisitor;
import org.milyn.delivery.sax.SAXElement;
import org.milyn.delivery.sax.SAXVisitAfter;
import org.milyn.delivery.sax.SAXVisitBefore;
import org.milyn.io.file.FileListAccessor;
import org.milyn.javabean.BeanAccessor;
import org.milyn.routing.file.naming.NamingStrategy;
import org.milyn.routing.file.naming.NamingStrategyException;
import org.milyn.routing.file.naming.TemplatedNamingStrategy;
import org.w3c.dom.Element;

import java.io.*;

/**
 * FileRouter is a fragment Visitor (DOM/SAX) that can be used to route
 * context beans ({@link org.milyn.javabean.BeanAccessor} beans) to file. 
 * <p/>
 * The name of the output file(s) is determined by a NamingStrategy, the
 * default being {@link TemplatedNamingStrategy}.  The beans being routed can
 * be the result of a "bindto" templating operation (see Templating Cartridge),
 * allowing you to perform <b>splitting</b>, <b>transformation</b> and <b>routing</b> of huge
 * messages.
 * <p/>
 * As the number of files produced by a single transformation can be
 * quite large, the filename are not stored in memory.<br>
 * Instead, they are appended to a file. This file contains the names of files created
 * during the transformation. <br>
 * <p/>
 * Example configuration:
 * <pre>
 * &lt;resource-config selector="orderItems"&gt;
 *    &lt;resource&gt;org.milyn.routing.file.FileRouter&lt;/resource&gt;
 *    &lt;param name="beanId">beanId&lt;/param&gt;
 *    &lt;param name="destinationDirectory">dir&lt;/param&gt;
 *    &lt;param name="listFileName">OrdersToSystemB.lst&lt;/param&gt;
 *    &lt;param name="fileNamePattern">${orderid}-${customName}.txt&lt;/param&gt;
 * &lt;/resource-config&gt;
 * 
 * Optional parameters:
 *    &lt;param name="encoding"&gt;UTF-8&lt;/param&gt;
 * </pre>
 * 
 * Description of configuration properties:
 * <ul>
 * <li><code>beanId </code> is key used search the execution context for the content to be written to a file
 * <li><code>destinationDirectory </code> is the destination directory for files created by this router
 * <li><code>fileListName </code> is name of the file that will contain the file names generated by this configuration. Supports templating 
 * <li><code>fileNamePattern </code> is the pattern that will be used to name the generated files. Supports templating
 * <li><code>encoding </code> is the encoding used when writing a characters to file
 * </ul>
 * 
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 * @since 1.0
 *
 */
@VisitAfterIf(	condition = "!parameters.containsKey('visitBefore') || parameters.visitBefore.value != 'true'")
@VisitBeforeIf(	condition = "!parameters.containsKey('visitAfter') || parameters.visitAfter.value != 'true'")
public class FileRouter implements DOMElementVisitor, SAXVisitBefore, SAXVisitAfter
{
	private final Log log = LogFactory.getLog( FileRouter.class );
    
    /*
     * 	System line separator
     */
	private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

	/*
	 * 	Name of directory where files will be created.
	 */
	@ConfigParam ( name = "destinationDirectory", use = Use.REQUIRED )
	private String destDirName;

	/*
	 * 	File name pattern for the created file
	 */
	@ConfigParam ( name = "fileNamePattern", use = Use.REQUIRED )
	private String fileNamePattern;

	/*
	 * 	beanId is a key that is used to look up a bean in the execution context
	 */
    @ConfigParam( use = ConfigParam.Use.REQUIRED )
    private String beanId;

    /*
     *	Character encoding to be used when writing character output
     */
    @ConfigParam( use = ConfigParam.Use.OPTIONAL, defaultVal = "UTF-8" )
	private String encoding;
    
	/*
	 * 	File name for the list file 
	 */
	@ConfigParam ( name = "listFileName", use = Use.REQUIRED )
	private String listFileName;
	
    /*
     * 	File object for the destination directory
     */
    private File destinationDir;

    /*
     * Naming strategy for generating the file pattern for output files.
     */
    private NamingStrategy namingStrategy = new TemplatedNamingStrategy();

	@Initialize
	public void initialize()
	{
		destinationDir = new File ( destDirName );
    	if ( !destinationDir.exists() || !destinationDir.isDirectory() )
    	{
    		throw new SmooksException ( "Destination directory [" + destDirName + "] does not exist or is not a directory.");
    	}
	}

	//	Vistor methods

    public void visitBefore( final Element element, final ExecutionContext executionContext ) throws SmooksException
    {
        visit( executionContext );
    }

    public void visitAfter( final Element element, final ExecutionContext executionContext ) throws SmooksException
	{
		visit( executionContext );
	}

    public void visitBefore( final SAXElement saxElement, final ExecutionContext executionContext ) throws SmooksException, IOException
    {
        visit( executionContext );
    }

    public void visitAfter( final SAXElement saxElement, final ExecutionContext executionContext ) throws SmooksException, IOException
	{
		visit( executionContext );
	}

	//	protected
	protected String generateFilePattern( final String fileNamePattern, final Object bean )
	{
    	try
		{
			return namingStrategy.generateFileName( fileNamePattern, bean );
		} 
    	catch (NamingStrategyException e)
		{
    		throw new SmooksException( e.getMessage(), e );
		}
	}
	
	//	private
	

	/**
	 * 	Extracts the bean identified by beanId and appends that object
	 * 	to the destination file.
	 *
	 * 	@param executionContext		- Smooks ExecutionContext
	 *  @throws SmooksException	if the bean cannot be found in the ExecutionContext
	 */
	private void visit( final ExecutionContext executionContext ) throws SmooksException
	{
        Object bean = BeanAccessor.getBean( executionContext, beanId );
        if ( bean == null )
        {
        	throw new SmooksException( "A bean with id [" + beanId + "] was not found in the executionContext");
        }
        
		String generatedFileName = destinationDir.getAbsolutePath() + File.separator + generateFilePattern( fileNamePattern, bean );
		
		OutputStream out = null;
		try
		{
			final FileOutputStream fileOut = new FileOutputStream( generatedFileName, true );
			if ( bean instanceof String )
			{
            	out = new BufferedOutputStream( fileOut );
        		out.write( ( (String)bean).getBytes(encoding ) );
			}
			else if ( bean instanceof byte[] )
			{
            	out = new BufferedOutputStream( fileOut );
        		out.write( new String( (byte[]) bean, encoding ).getBytes() ) ;
			}
			else 
			{
        		out = new ObjectOutputStream( fileOut);
        		((ObjectOutputStream)out).writeObject( bean );
			}
			out.flush();
			
    		addFileToFileList( generatedFileName, executionContext, bean );
		}
		catch (IOException e)
		{
    		final String errorMsg = "IOException while trying to append to file [" + destinationDir + "/" + fileNamePattern + "]";
    		throw new SmooksException( errorMsg, e );
		}
		finally
		{
			if ( out != null )
			{
				try
				{
					out.close();
				} 
				catch (IOException e)
				{
					log.error( "IOException while trying to close output stream to file [" + generatedFileName + "]", e );
				}
			}
		}
	}
	
	/**
	 * For every FileRouter a <code>fileListName</code> can be specified. This is the name of a file that will
	 * be created in the <code>destinationDirectory</code> and will contain the names of all the generated files
	 * for the current transformtion.
	 * 
	 * @param transformedFileName	- The name of the created transformation file
	 * @param executionContext		- The Smooks ExecutionContext
	 * @throws IOException
	 */
	private void addFileToFileList( 
			final String transformedFileName, 
			final ExecutionContext executionContext,
			final Object bean) throws IOException
	{
		final String listFilePath = destDirName + File.separator + generateFilePattern( listFileName, bean );
		FileListAccessor.addFileName( listFilePath, executionContext );
		
		FileWriter writer = null;
		try
		{
			writer = new FileWriter( listFilePath, true );
    		writer.write( transformedFileName + LINE_SEPARATOR );
    		writer.flush();
		}
		finally
		{
			if ( writer != null )
			{
				writer.close();
			}
		}
	}

}
