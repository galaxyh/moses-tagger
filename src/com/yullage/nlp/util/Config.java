/**
 * 
 */
package com.yullage.nlp.util;

import com.beust.jcommander.Parameter;

/**
 * @author Yu-chun Huang
 *
 */
public class Config {
	@Parameter(names = "-taggerType", description = "Tagger type. Currently, \"factor\" and \"tree\" are supported.", converter = TaggerTypeConverter.class)
	public TaggerType taggerType = TaggerType.FACTOR;

	@Parameter(names = "-io", description = "IO type. Currently, \"stdio\" and \"file\" are supported.", converter = IoTypeConverter.class)
	public IoType io = IoType.STDIO;

	@Parameter(names = "-language", description = "Corpus language. Currently, \"chinese\" and \"english\" are supported.", converter = LanguageTypeConverter.class, required = true)
	public LanguageType language;

	@Parameter(names = "-autoSplitSentence", description = "Auto split sentences into different lines.")
	public boolean autoSplitSentence = false;

	@Parameter(names = "-eofMark", description = "Add \"__EOF__\\n\" to the end of output.")
	public boolean eofMark = false;

	@Parameter(names = "-spNnPosProcess", description = "Special NN POS process. Change all noun to NN.")
	public boolean spNnPosProcess = false;

	@Parameter(names = "-spVbPosProcess", description = "Special VB POS process. Change VBP and VBZ to VB.")
	public boolean spVbPosProcess = false;

	@Parameter(names = "-spLemmaProcess", description = "Special lemma process. Change JJR, JJS, RBR and RBS to base form.")
	public boolean spLemmaProcess = false;

	@Parameter(names = "-factorDelimiter", description = "Factor delimiter.")
	public String factorDelimiter = "|";

	@Parameter(names = "-factorInnerDelimiter", description = "Factor inner delimiter. e.g., delimiter for lemma-pos factor.")
	public String factorInnerDelimiter = "_";

	@Parameter(names = "-factorList", description = "Factor list. Available factors are \"surface\", \"lemma\", \"pos\" and \"lemma-pos\". The order indicates the output factor sequence.")
	public String factorList = "surface lemma pos";

	@Parameter(names = "-treeStem", description = "Use stem in tree form.")
	public boolean treeStem = false;

	@Parameter(names = "-adjFormDictionary", description = "ADJ forms dictionary file name.")
	public String adjFormDictionary;

	@Parameter(names = "-posModel", description = "POS model file name.")
	public String posModel;

	@Parameter(names = "-grammarModel", description = "Grammar model file name.")
	public String grammarModel;

	@Parameter(names = "-sourcePath", description = "Source corpus folder. Only used when IO type is set to file.")
	public String sourcePath;

	@Parameter(names = "-targetPathAll", description = "Corpus output folder for all factors. Only used when IO type is set to file.")
	public String targetPathAll;

	@Parameter(names = "-targetPathPos", description = "Corpus output folder for POS factor. Only used when IO type is set to file.")
	public String targetPathPos;
}
