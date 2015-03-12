/**
 *
 */
package com.yullage.nlp.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.ReflectionLoading;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yu-chun Huang
 */
public class TreeTagger implements Tagger {
    public static final String[] DEFAULT_SENTENCE_DELIMS = {".", "?", "!", "。", "？", "！"};

    private static TokenizerFactory<CoreLabel> ptbTokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(),
            "untokenizable=noneKeep");
    private static String[] parserOptionsEng = {"-maxLength", "80", "-retainTmpSubcategories"};
    private static String[] parserOptionsCh = {"-maxLength", "80"};

    private Function<List<HasWord>, List<HasWord>> escaperCh = ReflectionLoading.loadByReflection("edu.stanford.nlp.trees.international.pennchinese.ChineseEscaper");

    private LexicalizedParser lexicalizedParser;
    private TreebankLanguagePack tlp;

    private Config config;

    private Map<String, String> adjComparative = new HashMap<String, String>();
    private Map<String, String> adjSuperlative = new HashMap<String, String>();

    public TreeTagger(Config config) {
        this.config = config;

        if (config.language == LanguageType.CHINESE) {
            this.lexicalizedParser = LexicalizedParser.loadModel(config.grammarModel, parserOptionsCh);
        } else {
            this.lexicalizedParser = LexicalizedParser.loadModel(config.grammarModel, parserOptionsEng);
        }

        this.tlp = lexicalizedParser.getOp().langpack();

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
    public void tagSingleLine(String sentence, Writer writer) {
        TreePrint treePrint = new TreePrint("oneline", "stem,removeTopBracket", tlp);
        try {
            printTree(writer, treePrint, sentence);

            if (config.eofMark) {

                writer.write("__EOF__");
                writer.write("\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tagMultiLine(Reader reader, List<Writer> writers) {
        Writer writer = writers.get(0);

        long curTimestamp = System.currentTimeMillis();
        int lineCount = 0;

        BufferedReader br = new BufferedReader(reader);
        BufferedWriter bw = new BufferedWriter(writer);

        TreePrint treePrint = new TreePrint("oneline", "stem,removeTopBracket", tlp);

        try {
            String sentence;
            while ((sentence = br.readLine()) != null) {
                lineCount++;

                printTree(bw, treePrint, sentence);

                if (lineCount % 500 == 0) {
                    System.out.println(lineCount + " lines done.");
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            bw.flush();
            bw.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double timeElapsed = (System.currentTimeMillis() - curTimestamp) / 1000.0;
        System.out.println("Tagging completed. (" + (int) (lineCount / timeElapsed) + " lines/sec)");
    }

    private void printTree(Writer writer, TreePrint treePrint, String sentence) throws IOException {
        Reader stringReader = new StringReader(sentence);
        DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(stringReader);
        docPreprocessor.setTokenizerFactory(ptbTokenizerFactory);
        docPreprocessor.setSentenceFinalPuncWords(DEFAULT_SENTENCE_DELIMS);
        if (config.language == LanguageType.CHINESE) {
            docPreprocessor.setEscaper(escaperCh);
        }

        try {
            String outString = "";
            int sentenceCount = 0;
            for (List<HasWord> s : docPreprocessor) {
                sentenceCount++;
                Tree parse = lexicalizedParser.parse(s);
                customizeTree(parse, null);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter pw = new PrintWriter(baos, true);
                treePrint.printTree(parse, pw);
                pw.flush();

                String content = baos.toString(StandardCharsets.UTF_8.name());
                outString += content;
            }

            if (sentenceCount > 1) {
                if (config.autoSplitSentence) {
                    writer.write(outString);
                } else {
                    writer.write("(MULTIS " + outString.replaceAll("(\\r?\\n)+", " ").trim() + ")\n");
                }
            } else if (sentenceCount == 1) {
                writer.write(outString);
            }

            writer.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stringReader.close();
    }

    private void customizeTree(Tree t, Tree preTerminalNode) {
        if (t.isPreTerminal()) {
            preTerminalNode = t;
        }

        if (t.isLeaf()) {
            customizeStem(t, preTerminalNode);
            customizeFactor(t, preTerminalNode);
        } else {
            for (Tree kid : t.children()) {
                customizeTree(kid, preTerminalNode);
            }
        }
    }

    private void customizeFactor(Tree t, Tree preTerminalNode) {
        String factor = preTerminalNode.label().value();
        String word = t.label().value();

        if (factor != null) {
            if (config.spNnPosProcess) {
                if (factor.startsWith("NN")) {
                    preTerminalNode.label().setValue("NN");
                }
            }

            if (config.spVbPosProcess) {
                if (factor.equals("VBP") || factor.equals("VBZ")) {
                    preTerminalNode.label().setValue("VB");
                }
            }
        }

        if (word != null) {
            if (config.language == LanguageType.CHINESE) {
                if ((factor != null) && factor.startsWith("PU")) {
                    if ("。".equals(word) || "？".equals(word) || "！".equals(word))
                        preTerminalNode.label().setValue(word);
                }
            }
        }
    }

    private void customizeStem(Tree t, Tree preTerminalNode) {
        if (config.spLemmaProcess) {
            String factor = preTerminalNode.label().value();
            String word = t.label().value();

            if ("JJR".equals(factor) || "RBR".equals(factor)) { // Comparative forms
                if (adjComparative.containsKey(word)) {
                    t.label().setValue(adjComparative.get(word));
                }
            } else if ("JJS".equals(factor) || "RBS".equals(factor)) { // Superlative forms.
                if (adjSuperlative.containsKey(word)) {
                    t.label().setValue(adjSuperlative.get(word));
                }
            }
        }
    }
}
