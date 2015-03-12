/**
 *
 */
package com.yullage.nlp.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.*;
import java.util.*;

/**
 * @author Yu-chun Huang
 */
public class FactorTagger implements Tagger {
    public static final String[] DEFAULT_SENTENCE_DELIMS = {".", "?", "!", "。", "？", "！"};

    private static TokenizerFactory<CoreLabel> ptbTokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(),
            "untokenizable=noneKeep");
    private MaxentTagger tagger;
    private Morphology morph;

    private Config config;

    private Map<String, String> adjComparative = new HashMap<String, String>();
    private Map<String, String> adjSuperlative = new HashMap<String, String>();

    private List<FactorType> factorList = new ArrayList<FactorType>();

    public FactorTagger(Config config) {
        this.tagger = new MaxentTagger(config.posModel);
        this.morph = new Morphology();

        this.config = config;

        String flist = config.factorList.trim();
        if (flist.equals("")) {
            factorList.add(FactorType.SURFACE);
            factorList.add(FactorType.LEMMA);
            factorList.add(FactorType.POS);
        } else {
            String[] factors = flist.split("\\s+");
            for (String factor : factors) {
                factor = factor.toLowerCase();
                if ("surface".equals(factor)) {
                    factorList.add(FactorType.SURFACE);
                } else if ("lemma".equals(factor)) {
                    factorList.add(FactorType.LEMMA);
                } else if ("pos".equals(factor)) {
                    factorList.add(FactorType.POS);
                } else if ("lemma-pos".equals(factor)) {
                    factorList.add(FactorType.LEMMA_POS);
                }
            }
        }

        if ((config.adjFormDictionary != null) && (!"".equals(config.adjFormDictionary))) {
            try {
                Reader r = new InputStreamReader(new FileInputStream(config.adjFormDictionary), "UTF-8");
                BufferedReader br = new BufferedReader(r);

                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    String[] wordForms = line.split("\t");
                    if (!adjComparative.containsKey(wordForms[1])) {
                        adjComparative.put(wordForms[1], wordForms[0]);
                    }

                    if (!adjSuperlative.containsKey(wordForms[2])) {
                        adjSuperlative.put(wordForms[2], wordForms[0]);
                    }
                }

                br.close();
                r.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void tagMultiLine(Reader reader, List<Writer> writers) {
        Writer writerAll = writers.get(0);
        Writer writerPos = writers.get(1);

        long curTimestamp = System.currentTimeMillis();
        int wordCount = 0;
        int lineCount = 0;

        BufferedReader br = new BufferedReader(reader);
        BufferedWriter bwAll = new BufferedWriter(writerAll);
        BufferedWriter bwPos = null;
        if (writerPos != null) {
            bwPos = new BufferedWriter(writerPos);
        }

        try {
            boolean isFirstLineRead = false;
            String line = br.readLine();

            while (line != null) {
                lineCount++;

                Reader stringReader = new StringReader(line);
                DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(stringReader);
                docPreprocessor.setTokenizerFactory(ptbTokenizerFactory);
                docPreprocessor.setSentenceFinalPuncWords(DEFAULT_SENTENCE_DELIMS);

                boolean isSentenceRead = false;
                for (List<HasWord> sentence : docPreprocessor) {
                    List<TaggedWord> taggedSentence = tagger.tagSentence(sentence);

                    if (isSentenceRead && !config.autoSplitSentence) {
                        if (bwPos != null) {
                            bwPos.write(" ");
                        }
                        bwAll.write(" ");
                    }
                    isSentenceRead = true;

                    if (isFirstLineRead && config.autoSplitSentence) {
                        if (bwPos != null) {
                            bwPos.newLine();
                        }
                        bwAll.newLine();
                    }
                    isFirstLineRead = true;

                    try {
                        Iterator<TaggedWord> it = taggedSentence.iterator();
                        TaggedWord tw;
                        while (it.hasNext()) {
                            tw = it.next();

                            if (bwPos != null) {
                                bwPos.write(tw.tag());
                            }

                            bwAll.write(getFactorString(tw));

                            if (it.hasNext()) {
                                if (bwPos != null) {
                                    bwPos.write(" ");
                                }
                                bwAll.write(" ");
                            }

                            wordCount++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                String nextLine = br.readLine();
                if (!config.autoSplitSentence && (nextLine != null)) {
                    if (bwPos != null) {
                        bwPos.newLine();
                    }
                    bwAll.newLine();
                }

                stringReader.close();
                line = nextLine;

                if (lineCount % 500 == 0) {
                    System.out.println(lineCount + " lines done.");
                }
            }

            bwAll.newLine();
            if (bwPos != null) {
                bwPos.newLine();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            bwAll.flush();
            bwAll.close();
            if (bwPos != null) {
                bwPos.flush();
                bwPos.close();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double timeElpased = (System.currentTimeMillis() - curTimestamp) / 1000.0;
        System.out.println("Tagging completed. (" + (int) (wordCount / timeElpased) + " words/sec)");
    }

    @Override
    public void tagSingleLine(String sentence, Writer writerAll) {
        BufferedWriter bwAll = new BufferedWriter(writerAll);

        try {
            Reader stringReader = new StringReader(sentence);
            DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(stringReader);
            docPreprocessor.setTokenizerFactory(ptbTokenizerFactory);
            docPreprocessor.setSentenceFinalPuncWords(DEFAULT_SENTENCE_DELIMS);

            int sentenceCount = 0;
            for (List<HasWord> s : docPreprocessor) {
                if (!config.autoSplitSentence && (sentenceCount > 0)) {
                    bwAll.write(" ");
                }

                sentenceCount++;
                List<TaggedWord> taggedSentence = tagger.tagSentence(s);

                Iterator<TaggedWord> it = taggedSentence.iterator();
                TaggedWord tw;
                while (it.hasNext()) {
                    tw = it.next();

                    bwAll.write(getFactorString(tw));

                    if (it.hasNext()) {
                        bwAll.write(" ");
                    }
                }

                if (config.autoSplitSentence) {
                    bwAll.newLine();
                }
            }

            if (!config.autoSplitSentence && sentenceCount > 0) {
                bwAll.newLine();
            }

            if (config.eofMark) {
                bwAll.write("__EOF__");
                bwAll.newLine();
            }

            bwAll.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void customizePos(TaggedWord taggedWord) {
        if (config.spNnPosProcess) {
            if (taggedWord.tag().startsWith("NN")) {
                taggedWord.setTag("NN");
            }
        }

        if (config.spVbPosProcess) {
            if (taggedWord.tag().equals("VBP")) {
                taggedWord.setTag("VB");
            } else if (taggedWord.tag().equals("VBZ")) {
                taggedWord.setTag("VB");
            }
        }

        if (config.language == LanguageType.CHINESE) {
            if (taggedWord.tag().startsWith("PU")) {
                if ("。".equals(taggedWord.word()) || "？".equals(taggedWord.word()) || "！".equals(taggedWord.word()))
                    taggedWord.setTag(taggedWord.word());
            }
        }
    }

    private String getLemma(TaggedWord taggedWord) {
        String lemma = morph.lemma(taggedWord.word(), taggedWord.tag());

        if (config.spLemmaProcess) {
            if ("JJR".equals(taggedWord.tag()) || "RBR".equals(taggedWord.tag())) { // Comparative forms
                if (adjComparative.containsKey(taggedWord.word())) {
                    lemma = adjComparative.get(taggedWord.word());
                }
            } else if ("JJS".equals(taggedWord.tag()) || "RBS".equals(taggedWord.tag())) { // Superlative forms.
                if (adjSuperlative.containsKey(taggedWord.word())) {
                    lemma = adjSuperlative.get(taggedWord.word());
                }
            }
        }

        return lemma;
    }

    private String getFactorString(TaggedWord taggedWord) {
        String lemma = getLemma(taggedWord);
        customizePos(taggedWord);

        String factorString = "";
        int numFactors = factorList.size();
        int lastIdx = numFactors - 1;
        for (int i = 0; i < numFactors; i++) {
            FactorType type = factorList.get(i);
            if (type == FactorType.SURFACE) {
                factorString += taggedWord.word();
            } else if (type == FactorType.LEMMA) {
                factorString += lemma;
            } else if (type == FactorType.POS) {
                factorString += taggedWord.tag();
            } else if (type == FactorType.LEMMA_POS) {
                factorString += lemma + config.factorInnerDelimiter + taggedWord.tag();
            }

            if (i < lastIdx) {
                factorString += config.factorDelimiter;
            }
        }

        return factorString;
    }
}
