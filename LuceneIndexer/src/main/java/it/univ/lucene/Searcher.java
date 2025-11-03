package it.univ.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;

public class Searcher {

    // Crea lo stesso PerFieldAnalyzerWrapper usato in Indexer
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

        Analyzer defaultAnalyzer = new ItalianAnalyzer();
        return new PerFieldAnalyzerWrapper(defaultAnalyzer, perField);
    }

    /**
     * Interfaccia interattiva per interrogare l'indice.
     * indexPath -> path alla directory dell'indice (stesso usato da Indexer)
     */
    public static void interactiveSearch(String indexPath) {
        try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
             Scanner scanner = new Scanner(System.in)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer;
            try {
                analyzer = createCustomAnalyzer();
            } catch (IOException e) {
                System.err.println("Errore nella creazione dell'analyzer: " + e.getMessage());
                return;
            }

            System.out.println("\n🔎 Inserisci una query (es.: 'titolo Matrix' oppure 'trama \"film di fantascienza\"')");
            System.out.println("Usa 'exit' per uscire.\n");

            while (true) {
                System.out.print("Query > ");
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("exit")) break;
                if (line.isEmpty()) continue;

                String field;
                String queryText;
                if (line.startsWith("titolo ")) {
                    field = "title";
                    queryText = line.substring(7).trim();
                } else if (line.startsWith("trama ")) {
                    field = "content";
                    queryText = line.substring(6).trim();
                } else {
                    System.out.println("⚠️ La query deve iniziare con 'titolo ' o 'trama '");
                    continue;
                }

                try {
                    QueryParser parser = new QueryParser(field, analyzer);
                    // Sostituiamo i trattini con spazi nella query
                    queryText = queryText.replace('-', ' ');
                    
                    var query = parser.parse(queryText);
                    System.out.println("Query interpretata: " + query.toString());
                    long t0 = System.currentTimeMillis();
                    TopDocs topDocs = searcher.search(query, 10); // top 10
                    long t1 = System.currentTimeMillis();

                    System.out.println("🔍 Trovati " + topDocs.totalHits.value() + " documenti (ricerca in " + (t1 - t0) + " ms):");

                    StoredFields storedFields = searcher.storedFields();
                    for (ScoreDoc sd : topDocs.scoreDocs) {
                        Document doc = storedFields.document(sd.doc); // <<--- uso corretto
                        String title = doc.get("title");
                        String snippet = doc.get("content");
                        if (snippet != null && snippet.length() > 200) snippet = snippet.substring(0, 200) + "...";
                        System.out.printf("⚪ %s (docId=%d, score=%.4f)\n  %s\n", title, sd.doc, sd.score, snippet == null ? "" : snippet);
                    }
                    System.out.println();

                } catch (Exception e) {
                    System.out.println("❌ Errore durante il parsing/esecuzione della query: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Errore nell'aprire l'indice: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
