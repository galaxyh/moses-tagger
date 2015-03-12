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
public class LanguageTypeConverter implements IStringConverter<LanguageType> {
	@Override
	public LanguageType convert(String value) {
		value = value.toLowerCase().trim();
		if ("chinese".equals(value)) {
			return LanguageType.CHINESE;
		} else if ("english".equals(value)) {
			return LanguageType.ENGLISH;
		} else {
			throw new ParameterException("Language type \"" + value + "\" is not available: ");
		}
	}
}
