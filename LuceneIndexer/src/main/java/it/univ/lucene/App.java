package it.univ.lucene;

public class App {
    public static void main(String[] args) {
        String docsPath = "../dataset/files";
        String indexPath = "../index";
        long start = System.currentTimeMillis();
        Indexer.createIndex(docsPath, indexPath);
        long end = System.currentTimeMillis();
        System.out.println("⏱️ Tempo di indicizzazione: " + (end - start) + " ms");

         // Avvia la console per le query
        Searcher.interactiveSearch(indexPath);
    }
    
    }
