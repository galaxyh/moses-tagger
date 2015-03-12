package com.yullage.nlp.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author Yu-chun Huang
 */
public class Boot {
    private static Tagger tagger;

    /**
     * @param args Arguments for main class
     */
    public static void main(String[] args) {
        Config config = new Config();
        JCommander jCommander = new JCommander(config);

        if (args.length == 0) {
            jCommander.usage();
            return;
        }

        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            jCommander.usage();
            return;
        }

        if (config.taggerType == TaggerType.FACTOR) {
            tagger = new FactorTagger(config);
        } else if (config.taggerType == TaggerType.TREE) {
            tagger = new TreeTagger(config);
        } else {
            throw new IllegalArgumentException("No such tagger.");
        }

        if (config.io == IoType.FILE) {
            fileTagger(config);
        } else if (config.io == IoType.STDIO) {
            stdioTagger();
        }
    }

    private static void stdioTagger() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            Writer writer = new OutputStreamWriter(System.out, "UTF-8");

            String line;
            while ((line = reader.readLine()) != null) {
                tagger.tagSingleLine(line, writer);
                System.out.print("\n");
            }

            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileTagger(Config config) {
        final boolean isWritePosDir = (config.taggerType == TaggerType.FACTOR);
        final Path source = Paths.get(config.sourcePath);
        final Path targetAll = Paths.get(config.targetPathAll);
        final Path targetPos = Paths.get(config.targetPathPos);

        FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDirAll = targetAll.resolve(source.relativize(dir));
                Path targetDirPos = null;
                if (isWritePosDir) {
                    targetDirPos = targetPos.resolve(source.relativize(dir));
                }
                try {
                    Files.copy(dir, targetDirAll);
                    if (targetDirPos != null) {
                        Files.copy(dir, targetDirPos);
                    }
                } catch (FileAlreadyExistsException e) {
                    if (!Files.isDirectory(targetDirAll))
                        throw e;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path fileAll = targetAll.resolve(source.relativize(file));
                Path filePos = null;
                if (isWritePosDir) {
                    filePos = targetPos.resolve(source.relativize(file));
                }

                Reader reader = null;
                Writer writerAll = null;
                Writer writerPos = null;
                try {
                    reader = new InputStreamReader(new FileInputStream(file.toString()), "UTF-8");
                    writerAll = new OutputStreamWriter(new FileOutputStream(fileAll.toString()), "UTF-8");
                    if (filePos != null) {
                        writerPos = new OutputStreamWriter(new FileOutputStream(filePos.toString()), "UTF-8");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                tagger.tagMultiLine(reader, Arrays.asList(writerAll, writerPos));

                if (reader != null) {
                    reader.close();
                }

                if (writerAll != null) {
                    writerAll.close();
                }

                if (writerPos != null) {
                    writerPos.close();
                }

                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, fileVisitor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("All files tagged.");
    }
}
