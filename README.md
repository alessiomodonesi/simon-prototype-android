# simon-prototype-android

Progetto di Programmazione di Sistemi Embedded 2025/2026

[![Release Intermedia](https://img.shields.io/github/v/tag/alessiomodonesi/simon-prototype-android/v0.9?label=Release%20Intermedia)](https://github.com/alessiomodonesi/simon-prototype-android/releases/tag/v0.9)
[![Release Finale](https://img.shields.io/github/v/tag/alessiomodonesi/simon-prototype-android/v1.0?label=Release%20Finale)](https://github.com/alessiomodonesi/simon-prototype-android/releases/tag/v1.0)
[![License](https://img.shields.io/github/license/alessiomodonesi/simon-prototype-android)](https://github.com/alessiomodonesi/simon-prototype-android/blob/main/LICENSE)

## Descrizione del Progetto

Il progetto consiste in una app Android che costituisce un prototipo funzionante per l'implementazione di una variante del gioco "Simon". L'interfaccia utente è progettata per funzionare in modo corretto sia in modalità portrait che in modalità landscape. Inoltre, l'applicazione supporta almeno due lingue: [inglese](app/src/main/res/values/strings.xml) e [italiano](app/src/main/res/values-it/strings.xml).

Per quanto riguarda il ciclo di vita, l'app gestisce sia lo stato dell'istanza che lo stato persistente. Durante un cambio di configurazione (come la commutazione portrait/landscape), viene preservata la sequenza di rettangoli premuti e, se in corso, continua la riproduzione della sequenza da parte del computer. 

I dati delle partite concluse sono memorizzati in maniera persistente in un database locale SQLite, gestito in modo sicuro e tipizzato tramite la libreria **Jetpack Room** (tabella `games_history`). Questo permette di mantenere intatto lo storico anche in caso di chiusura dell'app o riavvio del dispositivo.

**Documentazione:**

* I dettagli della consegna intermedia sono disponibili nel documento: [specifications - intermediate.pdf](doc/specifications%20-%20intermediate.pdf)
* I dettagli completi della consegna finale sono disponibili nel documento: [specifications - final.pdf](doc/specifications%20-%20final.pdf)

## Requisiti Tecnici e Compilazione

Per compilare ed avviare correttamente l'applicazione sono necessari i seguenti strumenti:
* **Android Studio** (versione Koala / Ladybug / Meerkat o superiore)
* **JDK 17** (consigliata per il compilatore Gradle del progetto)
* **Android SDK 36** (compilazione impostata a `compileSdk = 36`)
* **Kotlin 2.0.x** (gestito tramite Gradle Compose Compiler)

Per avviare l'app da terminale, esegui il comando:
```bash
./gradlew installDebug
```

---

## Architettura dell'Interfaccia

L'interfaccia utente è strutturata su tre schermate principali realizzate interamente in **Jetpack Compose**:

### 1. Lista delle Partite: [HistoryActivity.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/history/HistoryActivity.kt)

* Questa è la prima schermata mostrata all'avvio dell'applicazione e mostra una lista dinamica contenente i dati sulle partite concluse dall'installazione dell'app.
* Sulla sinistra di ciascun elemento è indicata la lunghezza massima di una sequenza riprodotta correttamente dal giocatore.
* Sulla destra viene visualizzata la sequenza completa in cui si è verificato il primo errore, che viene mostrato con un colore diverso dal punto in cui si è sbagliato in poi. Nel caso in cui la sequenza risulti troppo lunga per lo spazio disponibile, ne viene mostrata solo la parte iniziale accompagnata da un indicatore grafico di troncamento.
* Cliccando su un elemento della lista, viene visualizzata la partita completa nella schermata "Dettaglio Partita".
* È presente un comodo Floating Action Button che porta alla "Schermata di Gioco".

### 2. Dettaglio Partita: [DetailActivity.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/detail/DetailActivity.kt)

* Si tratta di una schermata molto semplice che visualizza la partita con lo stesso aspetto della Lista delle Partite ma con maggiore spazio a disposizione.
* Da questa schermata si esce utilizzando il tasto "Back" di sistema (fisico, virtuale o touch gesture).

### 3. Schermata di Gioco: [GameActivity.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/game/GameActivity.kt)

* **Matrice dei Colori:** Una matrice composta da 3 righe per 2 colonne di rettangoli.
* **Area di Testo:** Un'area multiriga non editabile in cui viene mostrata la sequenza dei rettangoli premuti. Durante le proposte del computer, quest'area rimane vuota.
* **Controlli della Partita:**
  * Il pulsante **"Avvia partita"** è attivo all'ingresso nella schermata e dà il via alla partita disattivandosi; il computer propone sequenze casuali partendo da una lunghezza di 1, che il giocatore deve replicare.
  * Il pulsante **"Pausa"** è attivo durante la proposta del computer; se premuto, si trasforma in "Riprendi" e interrompe la riproduzione fino a nuova pressione.
  * Il pulsante **"Fine partita"** è attivo mentre si gioca. Se premuto, salva un errore nel punto corrente della sequenza (tranne per la sequenza iniziale di lunghezza 1, in quel caso l'app ignora la partita) e riporta alla Lista delle Partite.
* **Feedback:** Sia durante la proposta del computer che per le interazioni del giocatore, l'app fornisce un duplice feedback al rettangolo attivo: uno visivo (cambiamento di colore/alpha) e uno uditivo (riproduzione di un tono dedicato per ogni rettangolo tramite `AudioTrack` statica PCM). In caso di errore del giocatore, viene mostrato un dialog di segnalazione, la partita termina e si resta in attesa del tasto "Back" per uscire.

---

## Architettura MVVM (I 3 View Model Dedicati)

L'applicazione aderisce alle linee guida ufficiali di Google sull'architettura e rispetta il **Principio di Singola Responsabilità (SRP)**, separando i comportamenti su tre ViewModel indipendenti e autogestiti:

*   **[GameViewModel.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/game/GameViewModel.kt)** (associato a `GameActivity`): Coordina l'intero gameplay del Simon. Gestisce l'avvio della partita, la pausa/riprendi, i click dell'utente e la riproduzione asincrona tramite Coroutines (senza mai bloccare il Main Thread). Mantiene lo stato di gioco e si occupa di scrivere i record nel database al termine.
*   **[HistoryViewModel.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/history/HistoryViewModel.kt)** (associato a `HistoryActivity`): Espone in modo reattivo il flusso di dati della tabella SQL. Utilizza l'operatore `.stateIn()` con `SharingStarted.WhileSubscribed(5000L)` per massimizzare il risparmio energetico disconnettendo Room quando l'app va in background, tollerando al contempo i cambi di configurazione.
*   **[DetailViewModel.kt](app/src/main/java/it/unipd/dei/esp2526/simon/ui/detail/DetailViewModel.kt)** (associato a `DetailActivity`): Si occupa unicamente di caricare asincronamente i dettagli di un singolo record dal DB. Integra il recupero dell'ID direttamente tramite `SavedStateHandle` per una resilienza assoluta contro la terminazione del processo (Process Death).

Ogni ViewModel è dotato della propria classe **Factory** (`GameVMFactory`, `HistoryVMFactory`, `DetailVMFactory`) posizionata in fondo al rispettivo file come classe top-level, favorendo un design modulare ed auto-contenuto.

---

## Resilienza, Persistenza e Ottimizzazioni Avanzate

*   **Schema Database Room:**
    La persistenza dei dati si appoggia ad una tabella SQLite strutturata in questo modo:
    *   `id`: `INTEGER PRIMARY KEY AUTOINCREMENT`
    *   `max_length`: `INTEGER` (punteggio del round massimo riprodotto correttamente)
    *   `sequence`: `TEXT` (stringa della sequenza di colori delimitata da virgole)
*   **Evidenziazione Fedele dell'Errore (Asterisk Formatting):** Per rispettare in modo matematicamente fedele la specifica di visualizzazione della sequenza dall'errore alla fine in un colore diverso senza appesantire lo schema SQLite, l'app implementa un meccanismo personalizzato. Quando la partita termina, l'esatto colore in cui il giocatore ha sbagliato viene contrassegnato con un asterisco (es. `"R, G, *B, M, Y, C"`). Il formatter grafico rileva l'asterisco, colora di bianco tutto ciò che c'è prima e di rosso l'errore e tutti i successivi colori non raggiunti. La lista e la schermata di dettaglio sono perciò visivamente coerenti al 100%!
*   **Gestione del Process Death:** Grazie a `SavedStateHandle` e alla conversione dello stato `GameUiState` all'efficiente interfaccia nativa **`Parcelable`** (tramite il plugin `@Parcelize` di Kotlin), l'intera partita in corso (stato del timer, note riprodotte, pausa, input parziali dell'utente) viene salvata automaticamente nei Bundle di sistema e ripristinata dal punto esatto anche se il sistema operativo uccide l'activity per recuperare risorse.
*   **Script di Importazione SQL:** Per facilitare la correzione e consentire di testare immediatamente l'applicazione (LazyColumn, scroll orizzontale, troncamento testi, Detail Screen) senza dover giocare per ore, nella cartella `doc` del progetto è presente il file [import_test_data.sql](doc/import_test_data.sql) che contiene lo script SQL per importare un set esteso di partite di prova.

### Come importare i dati di test via Android Studio:
1. Collega un emulatore o un dispositivo fisico ed avvia l'applicazione Simon.
2. In Android Studio, apri la scheda **App Inspection** posizionata nei pannelli in basso.
3. Seleziona il processo attivo dell'app (`it.unipd.dei.esp2526.simon`) e clicca sulla scheda **Database Inspector**.
4. Seleziona il database `simon_db` nell'elenco a sinistra e clicca su **Open Query Console**.
5. Copia e incolla l'intero contenuto del file [import_test_data.sql](doc/import_test_data.sql) all'interno dell'editor SQL di Android Studio e premi **Run**. Lo storico si popolerà istantaneamente con le 20 partite di test pre-configurate!

---

## Risorse Utili

* [Documentazione ufficiale ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
* [Guida a StateFlow e SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

### Dispositivi di Sviluppo e Test

Come richiesto dalle specifiche del progetto, l'applicazione è stata sviluppata e testata per garantire la compatibilità su diversi formati di schermo, utilizzando i seguenti dispositivi:

#### 1. Dispositivo Virtuale (Riferimento di base)

* **Tipo di dispositivo:** Emulatore (Android Virtual Device)
* **Modello:** Google Pixel 2 (Aspect Ratio 16:9)
* **Versione Android:** API Level 36.0
* **Risoluzione Schermo:** 1920 x 1080 px
* **Densità Schermo:** 420 dpi
* **RAM:** 4 GB (4096 MB)

#### 2. Dispositivo Fisico (Test su schermo allungato)

* **Tipo di dispositivo:** Smartphone fisico
* **Modello:** Samsung Galaxy S22 (Aspect Ratio ~19.5:9)
* **Versione Android:** API Level 36.0
* **Risoluzione Schermo:** 2340 x 1080 px
* **Densità Schermo:** 425 dpi
* **RAM:** 8 GB

### Licenza

Questo progetto è distribuito sotto licenza **MIT**. Sentiti libero di utilizzare, studiare e modificare il codice per i tuoi progetti, a patto di includere l'informativa sul copyright originale.
Per maggiori dettagli, consulta il file [LICENSE](LICENSE) all'interno della repository.
