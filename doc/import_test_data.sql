-- Inserimento di 20 partite di test ricche e variegate
INSERT INTO games_history (max_length, sequence) VALUES 
(0, '*R'),                                                                    -- Sconfitta immediata al primo colore
(0, '*G'),                                                                    -- Altro errore al primissimo colore
(1, 'R, *G'),                                                                 -- Errore al 2° colore del round 2
(1, 'G, *B'),                                                                 -- Altro errore al 2° colore
(2, 'R, G, *B'),                                                              -- Errore al 3° colore del round 3
(2, 'B, M, *Y'),                                                              -- Altro errore al 3° colore
(3, 'R, G, B, *M'),                                                           -- Errore al 4° colore del round 4 (ultimo colore)
(3, 'R, *G, B, M'),                                                           -- Errore precoce: 2° colore del round 4 (score 3)
(4, 'R, G, B, M, *Y'),                                                        -- Errore al 5° colore del round 5
(4, 'Y, C, R, G, *M'),                                                        -- Altro errore al 5° colore
(5, 'G, R, B, M, C, *Y'),                                                     -- Errore al 6° colore del round 6 (ultimo colore)
(5, 'R, G, *B, M, Y, C'),                                                     -- Errore precoce: 3° colore del round 6 (score 5)
(6, 'R, G, B, M, Y, C, *R'),                                                  -- Errore al 7° colore del round 7
(7, 'R, G, B, M, Y, C, R, *G'),                                               -- Errore al 8° colore del round 8 (ultimo colore)
(7, 'R, G, B, *M, Y, C, R, G'),                                               -- Errore precoce: 4° colore del round 8 (score 7)
(8, 'R, G, B, M, Y, C, R, G, *B, M, Y'),                                      -- Errore al 9° colore del round 11 (score 8)
(10, 'R, G, B, M, Y, C, R, G, B, M, *Y, C, R'),                               -- Errore precoce: 11° colore del round 13 (score 10)
(10, 'R, G, B, M, Y, *C, R, G, B, M, Y'),                                     -- Errore precoce: 6° colore del round 11 (score 10)
(12, 'R, G, B, M, Y, C, R, G, B, M, Y, C, *R, G, B'),                         -- Sequenza molto lunga con errore al 13° colore
(15, 'R, G, B, M, Y, C, R, G, B, M, Y, C, R, G, B, *M, Y, C');                -- Sequenza lunghissima per testare lo scroll e il troncamento
