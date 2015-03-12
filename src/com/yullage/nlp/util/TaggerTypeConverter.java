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
public class TaggerTypeConverter implements IStringConverter<TaggerType> {
	@Override
	public TaggerType convert(String value) {
		value = value.toLowerCase().trim();
		if ("factor".equals(value)) {
			return TaggerType.FACTOR;
		} else if ("tree".equals(value)) {
			return TaggerType.TREE;
		} else {
			throw new ParameterException("Tagger type \"" + value + "\" is not available.");
		}
	}
}
