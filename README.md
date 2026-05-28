# simon-prototype-android

Progetto di Programmazione di Sistemi Embedded 2025/2026

[![Latest Release](https://img.shields.io/github/v/release/alessiomodonesi/simon-prototype-android)](https://github.com/alessiomodonesi/simon-prototype-android/releases/latest)
[![License](https://img.shields.io/github/license/alessiomodonesi/simon-prototype-android)](https://github.com/alessiomodonesi/simon-prototype-android/blob/main/LICENSE)

## Descrizione del Progetto

Il progetto consiste in una app Android che costituisce un prototipo funzionante per l'implementazione di una variante del gioco "Simon". L'interfaccia utente è progettata per funzionare in modo corretto sia in modalità portrait che in modalità landscape. Inoltre, l'applicazione supporta almeno due lingue: [inglese](/app/src/main/res/values/strings.xml) e [italiano](/app/src/main/res/values-it/strings.xml).

Per quanto riguarda il ciclo di vita, l'app gestisce sia lo stato dell'istanza che lo stato persistente. Durante un cambio di configurazione (come la commutazione portrait/landscape), viene preservata la sequenza di rettangoli premuti e, se in corso, continua la riproduzione della sequenza da parte del computer. I dati delle partite concluse sono memorizzati in maniera persistente utilizzando un database SQL (gestito tramite SqliteDatabase o Room), il che permette di mantenere intatto lo storico anche in caso di chiusura dell'app o riavvio del dispositivo.

**Documentazione:**

* I dettagli della consegna intermedia sono disponibili nel documento: [specifications - intermediate.pdf](./specifications%20-%20intermediate.pdf)
* I dettagli completi della consegna finale sono disponibili nel documento: [specifications - final.pdf](./specifications%20-%20final.pdf)

## Architettura dell'Interfaccia

L'interfaccia utente è ora strutturata su tre schermate principali:

### 1. Lista delle Partite: [HistoryActivity.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/ui/history/HistoryActivity.kt)

* Questa è la prima schermata mostrata all'avvio dell'applicazione e mostra una lista dinamica contenente i dati sulle partite concluse dall'installazione dell'app.
* Sulla sinistra di ciascun elemento è indicata la lunghezza massima di una sequenza riprodotta correttamente dal giocatore.
* Sulla destra viene visualizzata la sequenza completa in cui si è verificato il primo errore, che viene mostrato con un colore diverso dal punto in cui si è sbagliato in poi. Nel caso in cui la sequenza risulti troppo lunga per lo spazio disponibile, ne viene mostrata solo la parte iniziale accompagnata da un indicatore grafico di troncamento.
* Cliccando su un elemento della lista, viene visualizzata la partita completa nella schermata "Dettaglio Partita".
* È presente un pulsante (convenzionale o floating action button) che porta alla "Schermata di Gioco".

### 2. Dettaglio Partita: [DetailActivity.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/ui/detail/DetailActivity.kt)

* Si tratta di una schermata molto semplice che visualizza la partita con lo stesso aspetto della Lista delle Partite ma con maggiore spazio a disposizione.
* Da questa schermata si esce utilizzando il tasto "Back" di sistema (fisico, virtuale o touch gesture).

### 3. Schermata di Gioco: [GameActivity.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/ui/game/GameActivity.kt)

* **Matrice dei Colori:** Una matrice composta da 3 righe per 2 colonne di rettangoli.
* **Area di Testo:** Un'area multiriga non editabile in cui viene mostrata la sequenza dei rettangoli premuti. Durante le proposte del computer, quest'area rimane vuota.
* **Controlli della Partita:**
  * Il pulsante **"Avvia partita"** è attivo all'ingresso nella schermata e dà il via alla partita disattivandosi; il computer propone sequenze casuali partendo da una lunghezza di 1, che il giocatore deve replicare.
  * Il pulsante **"Pausa"** è attivo durante la proposta del computer; se premuto, si trasforma in "Riprendi" e interrompe la riproduzione fino a nuova pressione.
  * Il pulsante **"Fine partita"** è attivo mentre si gioca. Se premuto, salva un errore nel punto corrente della sequenza (tranne per la sequenza iniziale di lunghezza 1, in quel caso l'app ignora la partita) e riporta alla Lista delle Partite.
* **Feedback:** Sia durante la proposta del computer che per le interazioni del giocatore, l'app fornisce un duplice feedback al rettangolo attivo: uno visivo (cambiamento di colore o forma) e uno uditivo (riproduzione di un tono per ogni rettangolo). In caso di errore del giocatore, viene mostrata una segnalazione, la partita termina e si resta in attesa della pressione del tasto "Back".

### 4. View Model: [GameViewModel.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/ui/game/GameViewModel.kt)

Il `GameViewModel` funge da ponte tra l'interfaccia utente (UI) e il database (DAO), separando la logica di presentazione dalla gestione dei dati e coordinando il flusso della partita.

#### Gestione dello Stato Reattiva (StateFlow)

* **Stato della Partita:** Espone tramite `StateFlow` le fasi del gioco (Attesa, Turno Computer, Turno Giocatore, Pausa, Game Over). Questo permette alla UI di abilitare o disabilitare i pulsanti ("Avvia", "Pausa", "Fine") in modo dinamico.
* **Sincronizzazione Sequenze:** Gestisce la lista dei colori generati e quella degli inserimenti dell'utente. Durante il turno del giocatore, aggiorna la stringa destinata all'**Area di Testo**, garantendo che venga svuotata durante la fase di proposta del computer.
* **Feedback e Animazioni:** Gestisce l'indice del rettangolo attualmente attivo nella matrice 3x2, notificando alla View quale elemento deve attivare il feedback visivo e sonoro.

#### Logica di Business e Coroutines

* **Controllo Proposta:** Utilizza le Coroutines per gestire i tempi di accensione dei rettangoli durante la sequenza del computer. Implementa la logica di **Pausa/Riprendi** sospendendo l'esecuzione del flusso senza perdere il progresso corrente.
* **Validazione Input:** Confronta in tempo reale ogni pressione sulla matrice 3x2 con la sequenza attesa. In caso di errore, interrompe la partita e prepara i dati per il salvataggio.
* **Integrazione Room:** Al termine della partita (o pressione di "Fine partita"), se la lunghezza è maggiore di 1, interagisce con il DAO per persistere il risultato nel database in modo asincrono.

#### Ottimizzazione e Resilienza

* **Resilienza ai Cambi di Configurazione:** Ereditando da `AndroidViewModel`, protegge lo stato della partita (punteggio, sequenza, stato dei timer) da rotazioni dello schermo o ridimensionamenti della finestra.
* **Efficienza Energetica (`WhileSubscribed`):** La connessione ai dati reattivi del database rimane attiva solo quando la UI è visibile. Il buffer di 5 secondi previene interruzioni superflue durante i rapidi cambi di orientamento del dispositivo (es. passaggio a landscape per la matrice 3x2).

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
