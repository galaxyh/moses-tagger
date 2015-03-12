package com.yullage.nlp.util;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * Created by Yu-chun Huang on 1/4/15.
 */
public interface Tagger {
    void tagSingleLine(String sentence, Writer writer);

    void tagMultiLine(Reader reader, List<Writer> writers);
}
