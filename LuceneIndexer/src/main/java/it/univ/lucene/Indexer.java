package it.univ.lucene;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class Indexer {

    /**
     * Crea un PerFieldAnalyzerWrapper con un analyzer dedicato per ciascun campo:
     * - title → WhitespaceAnalyzer (tokenizza solo per spazi)
     * - content → ItalianAnalyzer (con stemming e stopwords)
     */
    private static Analyzer createCustomAnalyzer() throws IOException {
        // Crea l'analyzer personalizzato per il titolo come specificato
        Analyzer titleAnalyzer = CustomAnalyzer.builder()
            .withTokenizer(WhitespaceTokenizerFactory.class)
            .addTokenFilter(WordDelimiterGraphFilterFactory.class, 
                "generateWordParts", "1",     // spider-man -> spider, man
                "catenateNumbers", "1",       // 20.000 -> 20000
                "catenateWords", "1",         // spider-man -> spiderman
                "splitOnCaseChange", "1")     // SpiderMan -> spider, man
            .addTokenFilter(LowerCaseFilterFactory.class)
            .addTokenFilter(ASCIIFoldingFilterFactory.class) // rimuove gli accenti
            .build();
            
        Analyzer contentAnalyzer = new ItalianAnalyzer();

        Map<String, Analyzer> perField = new HashMap<>();
        perField.put("title", titleAnalyzer);
        perField.put("content", contentAnalyzer);

        // Analyzer di default per eventuali altri campi
        Analyzer defaultAnalyzer = new ItalianAnalyzer();

        return new PerFieldAnalyzerWrapper(defaultAnalyzer, perField);
    }

    /**
     * Crea un indice Lucene leggendo tutti i file di testo da una directory.
     * Ogni file viene indicizzato con:
     * - title → nome file senza estensione
     * - content → testo con riferimenti tipo [1], [23] rimossi
     */
    public static void createIndex(String docsPath, String indexPath) {
        try {
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = createCustomAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            iwc.setCodec(new SimpleTextCodec()); // formato leggibile (debug)

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                indexDocs(writer, Paths.get(docsPath));
            }

            System.out.println("✅ Indice creato con successo nella directory: " + indexPath);
        } catch (IOException e) {
            System.err.println("❌ Errore durante la creazione dell'indice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Indicizza ricorsivamente tutti i file di testo in una directory.
     */
    private static void indexDocs(IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    indexDocs(writer, entry);
                }
            }
        } else {
            indexSingleFile(writer, path);
        }
    }


    /**
     * Metodo di debug per visualizzare come un analyzer tokenizza un testo
     */
    public static void debugTokenization(Analyzer analyzer, String text) {
        try {
            System.out.println("\nDebug tokenizzazione per: \"" + text + "\"");
            TokenStream stream = analyzer.tokenStream("title", text);  // Specifichiamo il campo "title"
            CharTermAttribute termAttribute = stream.addAttribute(CharTermAttribute.class);
            
            stream.reset();
            System.out.println("Token generati:");
            while (stream.incrementToken()) {
                String token = termAttribute.toString();
                System.out.println("- \"" + token + "\"");
            }
            stream.end();
            stream.close();
            
        } catch (IOException e) {
            System.err.println("Errore durante il debug della tokenizzazione: " + e.getMessage());
        }
    }

    /**
     * Indicizza un singolo file di testo.
     */
    private static void indexSingleFile(IndexWriter writer, Path filePath) {
        try {
            // Nome file senza estensione
            String fileName = filePath.getFileName().toString();
            String title = fileName.replaceFirst("\\.txt$", "");

            // Legge il contenuto del file
            String rawContent = Files.readString(filePath, StandardCharsets.UTF_8);

            // Rimuove riferimenti tipo [1], [23]...
            String cleanedContent = rawContent.replaceAll("\\[[0-9]+\\]", "");

            // Crea il documento Lucene
            Document doc = new Document();
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("content", cleanedContent, Field.Store.YES));

            // Aggiunge il documento all'indice
            writer.addDocument(doc);

            // Debug della tokenizzazione del titolo
            try {
                Analyzer analyzer = createCustomAnalyzer();
                debugTokenization(analyzer, title);
            } catch (IOException e) {
                System.err.println("Errore durante la creazione dell'analyzer: " + e.getMessage());
            }

            System.out.println("📄 Indicizzato: " + title);

        } catch (IOException e) {
            System.err.println("⚠️ Errore durante la lettura di " + filePath + ": " + e.getMessage());
        }
    }
}
