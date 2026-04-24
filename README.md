# simon-prototype-android
Progetto di Sistemi Embedded 2025/2026

## Descrizione del Progetto
Il progetto consiste in una app Android che costituisce un primo prototipo per l'implementazione di una variante del gioco "Simon". L'interfaccia utente è progettata per funzionare in modo corretto sia in modalità portrait che in modalità landscape. Inoltre, l'applicazione supporta almeno due lingue: [italiano](/app/src/main/res/values-it/strings.xml) e [inglese](/app/src/main/res/values/strings.xml).

Per quanto riguarda il ciclo di vita, l'app gestisce lo stato dell'istanza: ad esempio, durante una commutazione portrait/landscape vengono preservati sia la sequenza di rettangoli premuti ([MainActivity.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/MainActivity.kt)) che il contenuto della lista dei risultati ([HistoryActivity.kt](/app/src/main/java/it/unipd/dei/esp2526/simon/HistoryActivity.kt)). Non è invece richiesto né gestito lo stato persistente; terminando l'applicazione, tutti i dati sulle partite giocate fino a quel momento vengono persi.

I dettagli completi della consegna originaria sono disponibili nel documento: [specifiche - (intermediate).pdf](./specifiche%20-%20(intermediate).pdf)

## Architettura dell'Interfaccia
L'interfaccia utente è strutturata su due schermate principali:

### MainActivity (Area di Gioco)
Questa è la schermata visualizzata all'avvio dell'applicazione. I suoi elementi sono disposti uno sotto l'altro in modalità portrait, mentre in modalità landscape la griglia di colori si trova a sinistra e gli altri elementi al suo fianco.
* **Matrice dei Colori:** Una matrice composta da 3 righe per 2 colonne di rettangoli. I colori disponibili sono rosso (R), verde (G), blu (B), magenta (M), giallo (Y) e ciano (C). L'ordine in cui questi colori vengono disposti può essere qualsiasi e la matrice mantiene le dimensioni 3x2 in qualsiasi orientamento del dispositivo.
* **Area di Testo:** Un'area multiriga non editabile in cui viene mostrata la sequenza dei colori premuti fino a quel momento. I colori sono indicati tramite la prima lettera del loro nome in inglese (indipendentemente dalla lingua impostata nell'app) e le lettere sono separate da virgole.
* **Controlli della Partita:**
  * Il pulsante **"Cancella"** azzera immediatamente il contenuto dell'area di testo e la sequenza in corso.
  * Il pulsante **"Fine partita"** termina la sequenza corrente, la rimuove dall'area di testo e la memorizza (anche se si tratta di una sequenza con zero elementi). Subito dopo richiama la HistoryActivity.

### HistoryActivity (Storico delle Partite)
Questa schermata viene richiamata dalla MainActivity e presenta una lista dinamica contenente i dati sulle partite che sono state concluse dall'avvio dell'applicazione.
* Ogni elemento della lista mostra sulla sinistra il numero totale di rettangoli premuti.
* Sulla destra viene visualizzata l'intera sequenza dei rettangoli premuti. Nel caso in cui la sequenza risulti troppo lunga per lo spazio disponibile, ne viene mostrata solo la parte iniziale accompagnata da un indicatore grafico di troncamento.
* Utilizzando il tasto "back" di sistema (fisico, virtuale o touch gesture) è possibile tornare alla MainActivity, che sarà pronta per raccogliere una nuova sequenza.

## Dispositivi di Sviluppo e Test

Come richiesto dalle specifiche del progetto, l'applicazione è stata sviluppata e testata per garantire la compatibilità su diversi formati di schermo, utilizzando i seguenti dispositivi:

### 1. Dispositivo Virtuale (Riferimento di base)
* **Tipo di dispositivo:** Emulatore (Android Virtual Device)
* **Modello:** Google Pixel 2 (Aspect Ratio 16:9)
* **Versione Android:** API Level 36.0
* **Risoluzione Schermo:** 1920 x 1080 px
* **Densità Schermo:** 420 dpi
* **RAM:** 4 GB (4096 MB)

### 2. Dispositivo Fisico (Test su schermo allungato)
* **Tipo di dispositivo:** Smartphone fisico
* **Modello:** Samsung Galaxy S22 (Aspect Ratio ~19.5:9)
* **Versione Android:** API Level 36.0
* **Risoluzione Schermo:** 2340 x 1080 px
* **Densità Schermo:** 425 dpi
* **RAM:** 8 GB
