-- Svuota la tabella prima dell'import per evitare duplicati (opzionale)
DELETE FROM games_history;

-- Reset dell'autoincremento degli ID
DELETE FROM sqlite_sequence WHERE name = 'games_history';

-- Inserimento di partite di test variegate (utilizzando il formato asterisco dell'Opzione C)
INSERT INTO games_history (max_length, sequence) VALUES 
(0, '*R'),                                                                     -- Sconfitta immediata al primo colore
(2, 'R, G, *B, M, Y, C'),                                                     -- Errore al 3° colore nel round 6
(5, 'G, R, B, M, C, *Y'),                                                     -- Errore al 6° colore nel round 6
(3, 'C, Y, M, *B'),                                                           -- Errore al 4° colore nel round 4
(8, 'R, G, B, M, Y, C, R, G, *B, M, Y'),                                     -- Sequenza lunga con errore al 9° colore
(1, 'M, *C'),                                                                 -- Errore al 2° colore nel round 2
(12, 'R, G, B, M, Y, C, R, G, B, M, Y, C, *R, G'),                           -- Sequenza molto lunga per testare lo scroll orizzontale
(4, 'Y, C, R, G, *M');                                                        -- Errore al 5° colore nel round 5
