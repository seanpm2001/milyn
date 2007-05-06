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
package org.milyn.javabean.decoders;

import org.milyn.javabean.DataDecoder;
import org.milyn.javabean.DataDecodeException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.SmooksConfigurationException;

/**
 * Boolean decoder.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BooleanDecoder implements DataDecoder {

    public void setConfiguration(SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException {
    }

    public Object decode(String data) throws DataDecodeException {
        try {
            return Boolean.parseBoolean(data);
        } catch(NumberFormatException e) {
            throw new DataDecodeException("Failed to decode Boolean value '" + data + "'.", e);
        }
    }
}
