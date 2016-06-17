# SimpleSocial2

Il progetto consiste nello sviluppo di una rete sociale caratterizzata da un semplice insieme di funzionalità. Per utilizzare queste funzionalità, gli utenti si devono registrare, quindi possono:

- effettuare il login
- ricercare un utente specificandone il nome
- stabilire amicizie con altri utenti
- pubblicare contenuti (testo)
- ricevere contenuti pubblicati dai loro amici, se hanno manifestato interesse in tali
contenuti
- effettuare logout

##Implementazioni
###Server
Il server lavora tramite NIO, gestendo ogni client in un channel separato. 
La richiesta di messaggi di KeepAlive viene mandata in multicast all'indirizzo impostato nel file di configurazione.
I client devono conoscere l'indirizzo per potersi registrare e ricevere i messaggi.

###Client
Il client lavora con le classiche socket bloccanti, andando a gestire tramite vari thread i vari tipi di
richieste che possono arrivare dal server.

Per leggere la specifica completa: http://alessandro308.com/files/ProgettoLPR2016.pdf
