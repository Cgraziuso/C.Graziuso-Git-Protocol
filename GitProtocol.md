[![Build Status](https://travis-ci.org/Cgraziuso/C.Graziuso-Git-Protocol.svg?branch=master)](https://travis-ci.org/Cgraziuso/C.Graziuso-Git-Protocol)

#Git Protocol
Rete P2P che tenta di replicare una piccola parte del Protocollo di Git. Gli utenti possono creare una repository ed aggiungerci file.
Altri utenti (peer) possono collaborare ad una repository esistente, modificando o aggiungendo file, oppure crearne una nuova.
Il sistema consentirà a più utenti di modificare i file presenti nella versione locale della repository e 
gestirà eventuali merge di documenti in caso di commit pendenti.
```
Autore: Graziuso Catello - Matricola 0522500680
```

#Tecnologie Utilizzate
- Java 8
- Tom P2P
- JUnit
- Apache Maven
- Docker
- IntelliJ

# Struttura del Progetto 
Utilizzando Maven è stata inserita la dipendenza di TomP2P nel file pom.xml.
```
<repositories>
    <repository>
        <id>tomp2p.net</id>
         <url>http://tomp2p.net/dev/mvn/</url>
     </repository>
</repositories>
<dependencies>
   <dependency>
     <groupId>net.tomp2p</groupId>
     <artifactId>tomp2p-all</artifactId>
      <version>5.0-Beta8</version>
   </dependency>
</dependencies>
```
Il package ```/src/main/java/it/unisa/gitProtocol/implementation/``` fornisce le seguenti classi Java:

 - Example, un interfaccia che verrà utilizzata da terminale in grado di interagire con il sistema sviluppato
 - GitProtocol, un'interfaccia che definisce i principali metodi del protocollo Git.	
 - GitProtocolImpl, un'iplementazione dell'interfaccia GitProtocol che utilizza la libreria P2P.	
 - MessageListener, un interfaccia per il listener dei messaggi ricevuti dai peer.	
 - MessageListenerImpl, l'implementazione dell'interfaccia di message listner.	
 - Messaggi, una classe contenente tutti i messaggi di errore utilizzati nel progetto.

Il package ```/src/main/java/it/unisa/gitProtocol/entity/``` fornisce le seguenti classi Java:

 - Commit, la classe rappresentante l'oggetto commit.
 - Repository, la classe rappresentante l'oggetto repository.

# Sviluppo

La classe Repository è costituita dalle seguenti variabili di istanza:
- filemap, hashmap contenente la coppia <nomeFile, contenutoFile>.
- files, lista di file presenti nella repository.
- commits, lista di commit relative a push sulla repository nella DHT.
- FIRST_COMMIT_MESSAGE, primo messaggio di commit presente alla creazione di una repository locale.
- repoName, nome della repository.                                         
- directory, root directory della repository locale.                                       
- peerAddress, lista di peer che partecipano alla repository.

La classe Commit è costituita dalle seguenti variabili di istanza:
- message, messaggio relativo ad una commit.
- repoName, nome della repository a cui le commit si riferiscono.
- timestamp, TimeStamp del momento in cui è stato creato un messaggio di Commit.

## Interfaccia GitProtocol
L'interfaccia fornita per lo sviluppo del progetto GitProtocol è costituita dai seguenti metodi:

1. createRepository
2. addFileToRepository
3. commit
4. push
5. pull

### Metodo createRepository
Il metodo createRepository prende in input i seguenti valori:
	-_repo_name
	-_directory

1. Tale funzione prima controlla se esiste già una repository locale.
2. In un try/catch crea un oggetto localre di tipo Repository.
3. In caso affermativo effettuerà una stampa.
	
##### Implementazione 
```
public boolean createRepository(String _repo_name, File _directory) {
        if (this.repo != null) {
            return false;
        }
        try {
            this.repo = new Repository(_directory.getPath(), _repo_name, peer.peerAddress());
            System.out.println("Repository "+ this.repo.getRepoName() + " creata");
        } catch (IOException e) {
            return false;
        } 
        return true;
    }
```

### Metodo addFileToRepository
Il metodo addFileToRepository prende in input i seguenti valori:
	-_repo_name
	-files

1. Tale funzione prima controlla se esiste già una repository locale.
2. Effettua un secondo controllo in cui verifica se il nome della repository locale coicide con _repo_name
3. In caso affermativo in un try/catch effettua l'aggiunta dei file presenti nella lista files nella repository locale attraverso addFiles(files).

##### Implementazione 
```
public boolean addFilesToRepository(String _repo_name, List<File> files) {
        if (this.repo==null)return false;
        if(!this.repo.getRepoName().equals(_repo_name))return false;
        try {
            return this.repo.addFiles(files);
        } catch (IOException ex) {
            return false;
        }
    }
```

### Metodo commit
Il metodo commit prende in input i seguenti valori:
	-_repo_name
	-_message

1. Effettua un controllo in cui verifica se il nome della repository locale coicide con _repo_name
2. In caso affermativo in un try/catch effettua l'aggiunta del messaggio _message nella repository locale attraverso addCommit(_message).

##### Implementazione 
```
public boolean commit(String _repo_name, String _message) {
        if(!this.repo.getRepoName().equals(_repo_name))return false;
        try {
            return this.repo != null && this.repo.addCommit(_message);
        } catch (Exception ex) {
            return false;
        }
    }
```

### Metodo push
##### Implementazione 

### Metodo pull
##### Implementazione

## Altri metodi implementati
### Metodo createInitialRepository
### Metodo getListFiles
### Metodo getContentFile
### Metodo getRepoFromDht
### Metodo putRepoToDht
### Metodo sendMessage
### Metodo pendingSet
### Metodo leaveNetwork
### Metodo getRepo


# Testing
 I casi di test analizzati sono i seguenti:



# Come Buildare Git Protocol

### In un Container Docker
E' necessario effettuare la build del progetto tramite docker al fine di creare un container. Nella cartella dove è presente il Dockerfile aprire il terminale e digitare il seguente comando.
```
docker build --no-cache -t gitcat .
```
### Avviare il Master Peer
Una volta effettuata la build è necessario avviare il master peer tramite il seguente comando. 
-i, -e sono due argomenti che indicano la modalità interactive (-i) e con due variabili di ambiente (-e). 
```
docker run -i --name MASTER-PEER -e MASTERIP="127.0.0.1" -e ID=0 gitcat
```
La variabile d'ambienbte MASTERIP è l'indirizzo ip del master peer e la variabile d'ambiente ID è l'id unico del peer.

### Avviare un Peer Generico
Soltanto dopo aver avviato il MasterPeer è possibile avviare altri peer attraverso l'istruzione seguente:
```
docker run -i --name PEER-1 -e MASTERIP="172.17.0.2" -e ID=1 gitcat
```
E' necessario cambiare per ogni peer il nome (--name) ed il suo ID (ID)
