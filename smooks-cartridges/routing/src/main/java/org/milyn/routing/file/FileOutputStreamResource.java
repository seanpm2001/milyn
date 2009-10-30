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

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.SmooksException;
import org.milyn.assertion.AssertArgument;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.cdr.annotation.ConfigParam.Use;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.annotation.Initialize;
import org.milyn.expression.MVELExpressionEvaluator;
import org.milyn.expression.ExpressionEvaluator;
import org.milyn.io.AbstractOutputStreamResource;
import org.milyn.javabean.context.BeanContext;
import org.milyn.javabean.decoders.MVELExpressionEvaluatorDecoder;
import org.milyn.javabean.repository.BeanRepository;
import org.milyn.javabean.repository.BeanRepositoryManager;
import org.milyn.routing.SmooksRoutingException;
import org.milyn.templating.freemarker.FreeMarkerUtils;
import org.milyn.util.DollarBraceDecoder;
import org.milyn.util.FreeMarkerTemplate;

/**
 * FileOutputStreamResouce is a {@link AbstractOutputStreamResource} implementation
 * that handles file output streams.
 * <p/>
 *
 * Example configuration:
 * <pre>
 * &lt;resource-config selector="order-item"&gt;
 *    &lt;resource&gt;org.milyn.io.file.FileOutputStreamResource&lt;/resource&gt;
 *    &lt;param name="resourceName"&gt;resourceName&lt;/param&gt;
 *    &lt;param name="fileNamePattern"&gt;orderitem-${order.orderId}-${order.orderItem.itemId}.xml&lt;/param&gt;
 *    &lt;param name="destinationDirectoryPattern"&gt;order-${order.orderId}&lt;/param&gt;
 *    &lt;param name="listFileNamePattern"&gt;orderitems-${order.orderId}.lst&lt;/param&gt;
 * &lt;/resource-config&gt;
 *
 * Optional properties (default values shown):
 *    &lt;param name="highWaterMark"&gt;200&lt;/param&gt;
 *    &lt;param name="highWaterMarkTimeout"&gt;60000&lt;/param&gt;
 * </pre>
 *
 * Description of configuration properties:
 * <ul>
 * <li><i>resourceName</i>: the name of this resouce. Will be used to identify this resource.
 * <li><i>fileNamePattern</i>: is the pattern that will be used to generate file names. The file is
 *      created in the destinationDirectory.  Supports templating.
 * <li><i>listFileNamePattern</i>: is name of the file that will contain the file names generated by this
 *      configuration. The file is created in the destinationDirectory.  Supports templating.
 * <li><i>destinationDirectoryPattern</i>: is the destination directory for files created by this router.   Supports templating.
 * <li><i>highWaterMark</i>: max number of output files in the destination directory at any time.
 * <li><i>highWaterMarkTimeout</i>: number of ms to wait for the system to process files in the destination
 * 		directory so that the number of files drops below the highWaterMark.
 * <li><i>highWaterMarkPollFrequency</i>: number of ms to wait between checks on the High Water Mark, while
 *      waiting for it to drop.
 * <li><i>closeOnCondition</i>: An MVEL expression. If it returns true then the output stream is closed on the visitAfter event
 * 		else it is kept open. If the expression is not set then output stream is closed by default.
 * </ul>
 * <p>
 * <b>When does a new file get created?</b><br>
 * As soon as an object tries to retrieve the Writer or the OutputStream from this OutputStreamResource and
 * the Stream isn't open then a new file is created. Using the 'closeOnCondition' property you can control
 * whenn a stream get closed. As long as the stream isn't closed, the same file is used to write too. At then
 * end of the filter process the stream always gets closed. Nothing stays open.
 *
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class FileOutputStreamResource extends AbstractOutputStreamResource
{
    private static final String TMP_FILE_CONTEXT_KEY_PREFIX = FileOutputStreamResource.class.getName() + "#tmpFile:";

	private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

	private static Log logger = LogFactory.getLog( FileOutputStreamResource.class );

    @ConfigParam
    private String fileNamePattern;
    private FreeMarkerTemplate fileNameTemplate;

    @ConfigParam
	private String destinationDirectoryPattern;
    private FreeMarkerTemplate destinationDirectoryTemplate;
    private FileFilter fileFilter;

    @ConfigParam(use = ConfigParam.Use.OPTIONAL)
    private String listFileNamePattern;
    private FreeMarkerTemplate listFileNameTemplate;

    private String listFileNamePatternCtxKey;

    @ConfigParam(defaultVal = "200")
    private int highWaterMark = 200;
    @ConfigParam(defaultVal = "60000")
    private long highWaterMarkTimeout = 60000;
    @ConfigParam(defaultVal = "1000")
    private long highWaterMarkPollFrequency = 1000;

    @ConfigParam(use=Use.OPTIONAL, decoder = MVELExpressionEvaluatorDecoder.class)
    private ExpressionEvaluator closeOnCondition;

    //	public

    public FileOutputStreamResource setFileNamePattern(String fileNamePattern) {
        AssertArgument.isNotNullAndNotEmpty(fileNamePattern, "fileNamePattern");
        this.fileNamePattern = fileNamePattern;
        return this;
    }

    public FileOutputStreamResource setDestinationDirectoryPattern(String destinationDirectoryPattern) {
        AssertArgument.isNotNullAndNotEmpty(destinationDirectoryPattern, "destinationDirectoryPattern");
        this.destinationDirectoryPattern = destinationDirectoryPattern;
        return this;
    }

    public FileOutputStreamResource setListFileNamePattern(String listFileNamePattern) {
        AssertArgument.isNotNullAndNotEmpty(listFileNamePattern, "listFileNamePattern");
        this.listFileNamePattern = listFileNamePattern;
        return this;
    }

    public FileOutputStreamResource setListFileNamePatternCtxKey(String listFileNamePatternCtxKey) {
        AssertArgument.isNotNullAndNotEmpty(listFileNamePatternCtxKey, "listFileNamePatternCtxKey");
        this.listFileNamePatternCtxKey = listFileNamePatternCtxKey;
        return this;
    }

    public FileOutputStreamResource setHighWaterMark(int highWaterMark) {
        this.highWaterMark = highWaterMark;
        return this;
    }

    public FileOutputStreamResource setHighWaterMarkTimeout(long highWaterMarkTimeout) {
        this.highWaterMarkTimeout = highWaterMarkTimeout;
        return this;
    }

    public FileOutputStreamResource setHighWaterMarkPollFrequency(long highWaterMarkPollFrequency) {
        this.highWaterMarkPollFrequency = highWaterMarkPollFrequency;
        return this;
    }

    public void setCloseOnCondition(String closeOnCondition) {
        AssertArgument.isNotNullAndNotEmpty(closeOnCondition, "closeOnCondition");
        this.closeOnCondition = new MVELExpressionEvaluator();
        this.closeOnCondition.setExpression(closeOnCondition);
    }

    @Initialize
    public void intialize() throws SmooksConfigurationException {
        if(fileNamePattern == null) {
            throw new SmooksConfigurationException("Null 'fileNamePattern' configuration parameter.");
        }
        if(destinationDirectoryPattern == null) {
            throw new SmooksConfigurationException("Null 'destinationDirectoryPattern' configuration parameter.");
        }

        fileNameTemplate = new FreeMarkerTemplate(fileNamePattern);
        destinationDirectoryTemplate = new FreeMarkerTemplate(destinationDirectoryPattern);

        fileFilter = new SplitFilenameFilter(fileNamePattern);

        if(listFileNamePattern != null) {
        	listFileNameTemplate = new FreeMarkerTemplate(listFileNamePattern);
            listFileNamePatternCtxKey = FileOutputStreamResource.class.getName() + "#" + listFileNamePattern;
        }
    }

    @Override
	public FileOutputStream getOutputStream( final ExecutionContext executionContext ) throws SmooksRoutingException, IOException {
        Map<String, Object> beanMap = FreeMarkerUtils.getMergedModel(executionContext);
        String destinationDirName = destinationDirectoryTemplate.apply(beanMap);
        File destinationDirectory = new File(destinationDirName);

        assertTargetDirectoryOK(destinationDirectory);
        waitWhileAboveHighWaterMark(destinationDirectory);

        final File tmpFile = File.createTempFile( "." + UUID.randomUUID().toString(), ".working", destinationDirectory );
		final FileOutputStream fileOutputStream = new FileOutputStream( tmpFile , true );
		executionContext.setAttribute( TMP_FILE_CONTEXT_KEY_PREFIX + getResourceName(), tmpFile );
		return fileOutputStream;
	}

    private void assertTargetDirectoryOK(File destinationDirectory) throws SmooksRoutingException {
        if(destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
            throw new SmooksRoutingException("The file routing target directory '" + destinationDirectory.getAbsolutePath() + "' exist but is not a directory. destinationDirectoryPattern: '" + destinationDirectoryPattern + "'");
        }
        if(!destinationDirectory.exists()) {
            if(!destinationDirectory.mkdirs()) {
                throw new SmooksRoutingException("Failed to create file routing target directory '" + destinationDirectory.getAbsolutePath() + "'. destinationDirectoryPattern: '" + destinationDirectoryPattern + "'");
            }
        }
    }

    private void waitWhileAboveHighWaterMark(File destinationDirectory) throws SmooksRoutingException {
        if(highWaterMark == -1) {
            return;
        }

        File[] currentList = destinationDirectory.listFiles(fileFilter);
        if(currentList.length >= highWaterMark) {
            long start = System.currentTimeMillis();

            if(logger.isDebugEnabled()) {
                logger.debug("Destination directoy '" + destinationDirectory.getAbsolutePath() + "' contains " + currentList.length +  " file matching pattern '" + listFileNamePattern + "'.  High Water Mark is " + highWaterMark +  ".  Waiting for file count to drop.");
            }

            while(System.currentTimeMillis() < start + highWaterMarkTimeout) {
                try {
                    Thread.sleep(highWaterMarkPollFrequency);
                } catch (InterruptedException e) {
                    logger.error("Interrupted", e);
                    return;
                }
                currentList = destinationDirectory.listFiles(fileFilter);
                if(currentList.length < highWaterMark) {
                    return;
                }
            }

            throw new SmooksRoutingException("Failed to route message to Filesystem destination '" + destinationDirectory.getAbsolutePath() + "'. Timed out (" + highWaterMarkTimeout + " ms) waiting for the number of '" + listFileNamePattern + "' files to drop below High Water Mark (" + highWaterMark + ").  Consider increasing 'highWaterMark' and/or 'highWaterMarkTimeout' param values.");
        }
    }

    /* (non-Javadoc)
     * @see org.milyn.io.AbstractOutputStreamResource#closeCondition(org.milyn.container.ExecutionContext)
     */
    @Override
    protected boolean closeCondition(ExecutionContext executionContext) {

    	if( closeOnCondition == null ) {
    		return true;
    	}

    	return closeOnCondition.eval(executionContext.getBeanContext().getBeanMap());
    }

    @Override
	protected void closeResource( ExecutionContext executionContext )
	{
        try {
            super.closeResource(executionContext);
        } finally {
            File newFile = renameWorkingFile(executionContext);
            if(newFile != null) {
                addToListFile( executionContext, newFile );
            }
        }
	}

    //	private

    private File renameWorkingFile(ExecutionContext executionContext) {
        File workingFile = (File) executionContext.getAttribute( TMP_FILE_CONTEXT_KEY_PREFIX + getResourceName() );

        if ( workingFile == null || !workingFile.exists() )
        {
            return null;
        }

        String newFileName;
        Map<String, Object> beanMap = FreeMarkerUtils.getMergedModel(executionContext);

        //	BeanAccessor guarantees to return a beanMap... run the filename pattern
        // through FreeMarker to generate the file name...
        newFileName = fileNameTemplate.apply( beanMap );

        //	create a new file in the destination directory
        File newFile = new File( workingFile.getParentFile(), newFileName );

        if(newFile.exists()) {
            throw new SmooksException( "Could not rename [" + workingFile.getAbsolutePath() + "] to [" + newFile.getAbsolutePath() + "]. [" + newFile.getAbsolutePath() + "] already exists.");
        }

        //	try to rename the tmp file to the new file
        boolean renameTo = workingFile.renameTo( newFile ) ;
        if ( !renameTo )
        {
            throw new SmooksException( "Could not rename [" + workingFile.getAbsolutePath() + "] to [" + newFile.getAbsolutePath() + "]");
        }
        workingFile.delete();

        return newFile;
    }

    private void addToListFile( ExecutionContext executionContext, File newFile )
	{
        if(listFileNamePatternCtxKey != null) {
            FileWriter writer = (FileWriter) executionContext.getAttribute(listFileNamePatternCtxKey);

            if(writer == null) {
                String listFileName = getListFileName(executionContext);
                File listFile = new File ( newFile.getParentFile(), listFileName );

                FileListAccessor.addFileName( listFile.getAbsolutePath(), executionContext );
                try {
                    writer = new FileWriter( listFile );
                    executionContext.setAttribute(listFileNamePatternCtxKey, writer);
                } catch (IOException e) {
                    throw new SmooksException("", e);
                }
            }

            try
            {
                writer.write( newFile.getAbsolutePath() + LINE_SEPARATOR );
                writer.flush();
            }
            catch (IOException e)
            {
                throw new SmooksException ( "IOException while trying to write to list file [" + getListFileName(executionContext) + "] :", e );
            }
        }
    }

    @Override
	public void executeExecutionLifecycleCleanup(ExecutionContext executionContext) {
        super.executeExecutionLifecycleCleanup(executionContext);

        // Close the list file, if there's one open...
        if(listFileNamePatternCtxKey != null) {
            FileWriter writer = (FileWriter) executionContext.getAttribute(listFileNamePatternCtxKey);

            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Failed to close list file '" + getListFileName(executionContext) + "'.", e);
                }
            }
        }
    }

    private String getListFileName(ExecutionContext executionContext) {
        Map<String, Object> beanMap = FreeMarkerUtils.getMergedModel(executionContext);
        return listFileNameTemplate.apply(beanMap);
    }

    public static class SplitFilenameFilter implements FileFilter {

        private final Pattern regexPattern;

        private SplitFilenameFilter(String filenamePattern) {
            // Convert the filename pattern to a regexp...
            String pattern = DollarBraceDecoder.replaceTokens(filenamePattern, ".*");
            regexPattern = Pattern.compile(pattern);
        }

        public boolean accept(File file) {
            Matcher matcher = regexPattern.matcher(file.getName());
            return matcher.matches();
        }
    }
}
