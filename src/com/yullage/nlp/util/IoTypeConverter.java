/**
 * 
 */
package com.yullage.nlp.util;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * @author Yu-chun Huang
 *
 */
public class IoTypeConverter implements IStringConverter<IoType> {
	@Override
	public IoType convert(String value) {
		value = value.toLowerCase().trim();
		if ("stdio".equals(value)) {
			return IoType.STDIO;
		} else if ("file".equals(value)) {
			return IoType.FILE;
		} else {
			throw new ParameterException("IO type \"" + value + "\" is not available.");
		}
	}
}
