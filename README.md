# 🎬 LuceneIndexer – Sistema di Indicizzazione e Ricerca Testuale con Apache Lucene

Questo progetto implementa un **sistema di Information Retrieval (IR)** basato su **Apache Lucene**, capace di indicizzare un insieme di testi (trame di film) e consentire **ricerche avanzate** tramite un’interfaccia a linea di comando.  
L’obiettivo è costruire un sistema completo di **indicizzazione e querying testuale**, con un dataset generato a partire da un CSV contenente informazioni sui film.

---

## 📁 Struttura del Progetto
```txt
IR-System-with-Apache-Lucene/
├── dataset/
│   ├── files/                   # File .txt indicizzati (uno per film)
│   ├── a.csv                    # Dataset originale
│   └── genera_dataset.ipynb     # Script Python per la creazione dei file di testo
├── index/                       # Indice Lucene generato automaticamente
├── LuceneIndexer/                       
    ├── pom.xml                      # File Maven di configurazione del progetto Java
    └── src/main/java/it/univ/lucene/
        ├── App.java                 # Classe main: crea indice + avvia la console di ricerca
        ├── Indexer.java             # Gestione della creazione dell'indice
        └── Searcher.java            # Interfaccia interattiva per l'esecuzione delle query
├── relazione/
    ├── relazione.pdf/            # Relazione del progetto implementato

```

## Creazione e Gestione del Dataset

Il dataset è basato su un file CSV (`a.csv`) contenente informazioni su film, inclusi i campi **titolo** e **trama**.  
Il notebook `genera_dataset.ipynb` in `dataset/` si occupa di:

- Caricare il CSV e creare un DataFrame con **pandas**.  
- Rimuovere eventuali **duplicati** nel campo `title`.  
- Selezionare un sottoinsieme di film (es. 3000 righe).  
- Tradurre automaticamente **titolo** e **trama** in lingua italiana.  
- Salvare i testi tradotti come **file `.txt`** nella directory `dataset/files/`, che sarà poi indicizzata da Lucene.

### Librerie Python utilizzate
Nel file `requirements.txt` sono elencate le dipendenze necessarie per eseguire il notebook:

```txt
pandas
tqdm
deep-translator
```
(opzionalmente: notebook, pathlib, re, os sono già inclusi nella libreria standard o nell’ambiente Jupyter).

Per installarle:
```txt
pip install -r requirements.txt
```

## Componenti Java del Sistema
Il progetto è realizzato in Java 21 e gestito tramite Apache Maven.
Le principali librerie utilizzate sono:
```txt 
lucene-core

lucene-analysis-common

lucene-queryparser

lucene-codecs

```
Tutte alla versione 10.3.1 (definite nel pom.xml).

##  Funzionamento del Sistema di Indicizzazione
###  Indexer
La classe Indexer:


Legge tutti i file .txt presenti in dataset/files/;


Costruisce un indice Lucene nella directory index/;


Applica un PerFieldAnalyzerWrapper:


- title: usa un Custom Analyzer basato su tokenizzazione per spazi e filtri per minuscole, accenti e parole composte (es. “Spider-Man” → “spider man”, “spiderman”);


- content: usa l’ItalianAnalyzer (con stemming e stopwords).




Il codec SimpleTextCodec è utilizzato per rendere l’indice leggibile e facilmente ispezionabile.

###  Interfaccia Utente su Console
Una volta creato l’indice, l’applicazione permette di eseguire query interattive da terminale.
#### Esempi di query supportate
```txt
Query > titolo inception
Query > trama "guerra mondiale"
Query > titolo spider man
```


Le virgolette (" ") attivano una PhraseQuery.


I termini multipli vengono interpretati come OR logico.


È possibile specificare il campo (titolo o trama) oppure cercare su entrambi.


Il sistema restituisce per ogni risultato:


- il titolo del film,


- un frammento della trama,


- lo score di ranking calcolato da Lucene.



##  Esecuzione del Progetto
### Compilazione ed Esecuzione tramite Maven
Assicurati di trovarti nella directory del progetto (LuceneIndexer/), quindi esegui:
#### Pulizia della build precedente
```txt
mvn clean
```
#### Compilazione del progetto
```txt
mvn compile

```
#### Creazione del pacchetto .jar
```txt
mvn package

```
#### Esecuzione della classe App (main)
```txt
mvn exec:java -Dexec.mainClass="it.univ.lucene.App"

```
Durante l’esecuzione:


Viene creato l’indice Lucene in index/;


Si avvia l’interfaccia per inserire query testuali.


##  Requisiti e Ambiente


Java: 21


Maven: ≥ 3.9


Python: ≥ 3.9 (solo per la generazione del dataset)


Dipendenze Lucene: versione 10.3.1


Sistema operativo: testato su Ubuntu 24.04

