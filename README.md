[![Build Status](https://travis-ci.org/Cgraziuso/C.Graziuso-Git-Protocol.svg?branch=master)](https://travis-ci.org/Cgraziuso/C.Graziuso-Git-Protocol)

# Git Protocol 
P2P network attempting to replicate a small part of the Git protocol. Users can create a repository and add files. Other users (peers) can collaborate on an existing repository, modifying or adding files, or create a new one. The system will allow multiple users to edit the files in the local version of the repository and it will manage any merge of document in case of pending commits.
```
Autore: Graziuso Catello - Matricola 0522500680
```

# Used Technologies
- Java 8
- Tom P2P
- JUnit
- Apache Maven
- Docker
- IntelliJ

# Project structure 
Using Maven the TomP2P dependency has been inserted in the pom.xml file.
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
The package ```/src/main/java/it/unisa/gitProtocol/implementation/``` provides the following Java classes:

  - Example, an interface that will be used by the terminal. It is able to interact with the developed system
  - GitProtocol, an interface that defines the main methods of the Git protocol.
  - GitProtocolImpl, an implementation of GitProtocol interface that uses the P2P library.
  - MessageListener, an interface for the listener of messages received from peers.
  - MessageListenerImpl, the implementation of the message listner interface.
  - Messages, a class containing all the error messages used in the project.

The package ```/src/main/java/it/unisa/gitProtocol/entity/``` provides the following Java classes:

  - Commit, the class representing the commit object.
  - Repository, the class representing the repository object.

# Development

The Repository class is made up of the following instance variables:
- filemap, hashmap containing the pair <FileName, ContentFile>.
- files, list of files present in the repository.
- commits, list of commits related to push on the repository in DHT.
- FIRST_COMMIT_MESSAGE, first commit message present when creating a local repository.
- repoName, name of the repository.
- directory, root directory of the local repository.
- peerAddress, list of peers participating in the repository.

The Commit class is made up of the following instance variables:
- message, message related to a commit.
- repoName, name of the repository to which the commits refer.
- timestamp, TimeStamp of the moment when a Commit message was created.

## GitProtocol Interface
The interface provided for the development of the GitProtocol project consists of the following methods:

1. createRepository
2. addFileToRepository
3. commit
4. push
5. pull

### createRepository Method
The createRepository method takes the following values as input:
	-_repo_name
	-_directory
	
1. This function first checks if a local repository already exists.
2. In a try / catch create a local object of type Repository.
3. If so, it will print.
	
##### Implementation 
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

### addFileToRepository Method
The addFileToRepository method takes the following values as input:
	-_repo_name
	-files

1. This function first checks if a local repository already exists.
2. Do a second check to see if the local repository name coicides with _repo_name
3. If yes, in a try / catch, add the files present in the list of files in the local repository through addFiles (files).

##### Implementation 
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

### commit Method
The commit method takes the following values as input:
	-_repo_name
	-_message

1. Do a check to see if the local repository name coicides with _repo_name
2. If yes, in a try / catch, add the _message message to the local repository through addCommit (_message).

##### Implementation 
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

### push Method
The commit method takes the following values as input:
	-_repo_name

1. Do two checks to see if the local repository is null and, if not, if the name matches _repo_name.
2. Check if the repository in the dht is null or if the local one contains all the commits of the one loaded on the network
3. If yes, push by replacing the repository in the dht with the local one
4. Send a message to all contributors in peerAddress
5. Return a successful message.
##### Implementation 
```
public String push(String _repo_name) {
        if (this.repo == null)
            return Messaggi.NESSUNAREPOLOCALE.getMessage();
        if(!this.repo.getRepoName().equals(_repo_name))
            return Messaggi.NONESISTEREPO.getMessage();
        try {
            Repository dhtRepo = getRepoFromDht(_repo_name);
            int check=0;
            if(pendingCommit>1)
                check = pendingCommit-1;
            if (dhtRepo == null || this.repo.getCommits().containsAll(dhtRepo.getCommits() ) )  
            {
                if(putRepoToDht(_repo_name, this.repo) )                                       
                {
                    pendingSet(0);                                                          
                    //invio messaggio
                    sendMessage(id, _repo_name, peer.peerAddress());                            
                    return Messaggi.SUCCESSOPUSH.getMessage();
                }
            }else{
                return  Messaggi.REPONONAGGIORNATA.getMessage();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return "ERRORE NELLA DHT GET/PUT: " + e.getMessage();
        }
        return "ERRORE NELLA PUSH";
    }
```

### pull Method
The commit method takes the following values as input:
	-_repo_name
	
1. Do two checks to check if the repository in the dht is null and, if not, if the name of the local repository coincides with _repo_name.
2. Check if the hash of the local repository coincides with the hash of the repository in the dht, if so, you already have the most updated version.
3. The local repository is updated by managing possible pending commits.
4. The commits and peerAddress array are updated.
5. Return a successful message.

##### Implementation
```
public String pull(String _repo_name) {
        try{
            Repository dhtRepo = getRepoFromDht(_repo_name);
            if (dhtRepo == null)
                return Messaggi.NONESISTEREPO.getMessage();
            if(!this.repo.getRepoName().equals(_repo_name))
                return Messaggi.NONESISTEREPO.getMessage();
            System.out.println(dhtRepo.toString());
            if(dhtRepo.hashCode()==this.repo.hashCode()) {
                return Messaggi.REPOAGGIORNATA.getMessage();
            }
            if(pendingCommit>0)                                                            
            {
                this.repo.updateRepoWithPending(dhtRepo, true);
                pendingSet(0);
            }else{
                this.repo.updateRepoWithPending(dhtRepo, false);
            }
                this.repo.addCommit(dhtRepo.getCommits());
                this.repo.setPeerAddress(dhtRepo.getPeerAddress());
                if(!this.repo.getPeerAddress().contains(peer.peerAddress()))
                {
                    this.repo.getPeerAddress().add(peer.peerAddress());
                }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return Messaggi.SUCCESSOPULL.getMessage();
 ```

## Other methods implemented
### createInitialRepository method
Create an initial repository before pulling only if the local repository is non-existent.
### getListFiles method
It allows to know the files present in the local repository.
### getContentFile method
It allows to know the content of a file present in the local repository.
### getRepoFromDht method
It allows you to download, if any, the repository present in the DHT.
### putRepoToDht method
Allows you to load the repository into DHT.
### sendMessage method
It allows you to send a message to all contributors of a repository once a push has been made.
### pendingSet method
Allows you to change the number of pending commits.
### leaveNetwork method
Allows you to leave the P2P network.
### getRepo method

## Improvements
### File Exploration
For a better readability of the local files, each user is given the possibility to explore the files present in his own repository. In particular, through the getListFiles and getContentFile methods it is possible to obtain a complete list of the files present and read their contents. These operations are very useful for verifying a file merge following pending commits.

### Messages
In order to help other peers in the network, when a peer push a repository, all peers participating in it will receive a message inviting them to perform a pull. This was possible by adding a PeerAddress arrayList to the Repository entity.



# Testing
The test cases analyzed are the following:

 ## GitProtocolImpl
 1. createRepository: creates a new repository; (TRUE)
 2. createSecondRepository: creation of a new repository but it's duplicated; (FAILS)
 3. addFilesToRepository: adds a list of File in a local repository; (TRUE)
 4. addFilesToRepositoryInesistente: a peer adds a list of files to an repository that not exist; (FAILS)
 5. commit: a peer commits after adding files to the local repository; (TRUE)
 6. commitRepoSbagliata: a peer commits after adding files to an incorrect repository; (FAILS)
 7. pushTrePeer : three peers push in sequence; (TRUE)
 8. ConflittoPush : the push of all commits fails because the repository isn't updated; (FAILS)
 9. pullSuccess : pulls the files from the network successfully; (TRUE)
 10. PushRepositoryInesistente :  the push of all commits fails because the repository not exist; (FAILS)
 11. pullRepositoryInesistente : a peer pulls to a non-existent repository; (FAILS) 
 12. pullRepositoryAggiornata : a peer pulls but the repository is already updated; (TRUE)
 13. createInitialRepository : creates a new repository with no all arguments; (TRUE)
 14. createSecondInitialRepository : creation of a new inital repository fails but it's duplicated; (FAILS)
 15. pushRepoDiversa: a peer pushes to a repository different than the local one; (FAILS)
 16. pullRepoDiversa : a peer pulls a repository different than the local one; (FAILS)

 

 ## GitProtocolImplUsers
This class attempts to simulate an iteration that four peers have with the system.
In particular, it explores all possible cases of Repository exploration and simulates a communication of four peers through 4 push / pull in sequence. Obviously all the operations of create, addFile and commit have been used.

# Dockerfile
1. FROM maven:3 as builder
2. RUN apt-get update && apt-get -y install git
3. ARG url
4. WORKDIR /app
5. RUN git clone ${url}
6. ARG project
7. WORKDIR /app/${project} 
8. RUN mvn package

9. FROM openjdk:8-jre-alpine
10. WORKDIR /app
11. ARG project
12. ARG artifactid
13. ARG version
14. ENV artifact ${artifactid}-${version}.jar
15. ENV MASTERIP=127.0.0.1
16. ENV ID=0
17. COPY --from=builder /app/${project}/target/${artifact} /app
18. CMD /usr/bin/java -jar ${artifact} -m $MASTERIP -id $ID

For better readability of the Dockerfile, labels have been inserted to better identify the various images used.
The first maven:3 image was tagged with "builder".
To adapt the Dockerfile to all projects of the same type, arguments have been added. In Docker, parameters can be passed using the ENV or ARG options. Both are set using the --build-arg option on the command line. The url, project, artifactid and version arguments which are respectively the url of the Git-hub project, the name of this project, the artifactid present in pom.xml and finally the version of the package. These arguments passed by command line during the build will allow the creation of the image of the developed project.


# How to Build Git Protocol

### In a Container Docker
It is necessary to build the project via docker in order to create a container. In the folder where the Dockerfile is present, open the terminal and type the following command.
```
sudo docker build --build-arg url=https://github.com/Cgraziuso/C.Graziuso-Git-Protocol.git --build-arg project=C.Graziuso-Git-Protocol --build-arg artifactid=gitCat --build-arg version=1.0-jar-with-dependencies --no-cache -t gitcat .
```
### Start the Master Peer
Once the build has been carried out, the master peer must be started using the following command.
-i, -e are two arguments that indicate interactive mode (-i) and with two environment variables (-e).
```
docker run -i --name MASTER-PEER -e MASTERIP="127.0.0.1" -e ID=0 gitcat
```
The MASTERIP env variable is the IP address of the master peer and the env variable ID is the unique id of the peer.

### Start a Generic Peer
Only after starting the MasterPeer it is possible to start other peers through the following instruction:
```
docker run -i --name PEER-1 -e MASTERIP="172.17.0.2" -e ID=1 gitcat
```
It is necessary to change for each peer the name (--name) and its ID (ID)
