package it.unisa.gitProtocol.implementation;


import it.unisa.gitProtocol.entity.Repository;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import net.tomp2p.p2p.Peer;
import net.tomp2p.rpc.ObjectDataReply;



public class GitProtocolImpl implements GitProtocol {


    //private final Storage storage;
    private Repository repo;
    final private Peer peer;
    final private PeerDHT peerDht;
    final private int DEFAULT_MASTER_PORT=4000;
    private int id;
    private int pendingCommit=0;


    public GitProtocolImpl(int id, String master_peer, final MessageListener ms) throws Exception {
        this.repo = null;
        this.id =id;
        peer= new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT).start();
        peerDht = new PeerBuilderDHT(peer).start();
        FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(master_peer)).ports(DEFAULT_MASTER_PORT).start();
        fb.awaitUninterruptibly();
        if(fb.isSuccess()) {
            peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }else {
            throw new Exception("Error in master peer bootstrap.");
        }

        //creazione root directory
        File fs = new File("app/" + id + "/");
        fs.mkdir();

        peer.objectDataReply(new ObjectDataReply() {

            public Object reply(PeerAddress sender, Object request) throws Exception {
                return ms.reciveMessage(request);

            }
        });


    }

    /**
     * Creates new repository in a directory
     * @param _repo_name a String, the name of the repository.
     * @param _directory a File, the directory where create the repository.
     * @return true if it is correctly created, false otherwise.
     */
    public boolean createRepository(String _repo_name, File _directory) {
        if (this.repo != null) {
            return false;
        }
        try {
            this.repo = new Repository(_directory.getPath(), _repo_name, peer.peerAddress());
            System.out.println("Repository "+ this.repo.getRepoName() + " creata");
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * metodo per la creazione di una repository iniziale se si vuole effettuare una pull di una repository esistente
     * @param _repo_name a String, the name of the repository.
     * @param _directory root directory della repository.
     * @return true se è stata creata una repository iniziale, false altrimenti.
     */
    public boolean createInitialRepository(String _repo_name, File _directory) {
        if(this.repo!=null)return false;
        try {
            this.repo = new Repository();
            this.repo.setRepoName(_repo_name);
            this.repo.setDirectory(_directory.getPath());
            System.out.println(repo.toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Adds a list of File to the given local repository.
     * @param _repo_name a String, the name of the repository.
     * @param files a list of Files to be added to the repository.
     * @return true if it is correctly added, false otherwise.
     */
    public boolean addFilesToRepository(String _repo_name, List<File> files) {
        if (this.repo==null)return false;
        if(!this.repo.getRepoName().equals(_repo_name))return false;
        try {
            return this.repo.addFiles(files);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Apply the changing to the files in  the local repository.
     * @param _repo_name a String, the name of the repository.
     * @param _message a String, the message for this commit.
     * @return true if it is correctly committed, false otherwise.
     */
    public boolean commit(String _repo_name, String _message) {
        if(!this.repo.getRepoName().equals(_repo_name))return false;
        try {
            return this.repo != null && this.repo.addCommit(_message);
        } catch (Exception ex) {
            return false;
        }
    }


    /**
     * Push all commits on the Network. If the status of the remote repository is changed,
     * the push fails, asking for a pull.
     * @param _repo_name _repo_name a String, the name of the repository.
     * @return a String, operation message.
     */
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
            if (dhtRepo == null || this.repo.getCommits().containsAll(dhtRepo.getCommits() ) )  //se la repository nella dht è null oppure le commit della repository locale non contengono quelle della dht
            {
                if(putRepoToDht(_repo_name, this.repo) )                                        //effettuo la push
                {
                    pendingSet(0);                                                          //non ho commit pendenti e quindi cambio tale valore a 0
                    //invio messaggio
                    sendMessage(id, _repo_name, peer.peerAddress());                            //informo eventuali altri utenti che seguono la repository che è stata effettuata una push
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

    /**
     * Pull the files from the Network. If there is a conflict, the system duplicates
     * the files and the user should manually fix the conflict.
     * @param _repo_name _repo_name a String, the name of the repository.
     * @return a String, operation message.
     */
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
            if(pendingCommit>0)                                                             //se ho pending commit, allora effettuo un aggiornamento della repository prevedendo il merge dei file con lo stesso nome ma contenuto differente
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
    }


    /**
     * metodo per conoscere i file presenti nella repository locale
     * @param _repo_name _repo_name a String, the name of the repository.
     * @return  String, lista dei file oppure errore.
     */
    public String getListFiles(String _repo_name)
    {
        if(this.repo==null)
            return Messaggi.NESSUNAREPOLOCALE.getMessage();
        if(this.repo.getFiles().size()==0)
            return Messaggi.NESSUNFILENELLAREPO.getMessage();
        if(this.repo.getRepoName().equals(_repo_name))
        {
            int index=1;
            String list="";
            for(File f: this.repo.getFiles())
            {
                list = list + index +". " + f.getName().toString() +  "\n";
                index++;
            }
            return "FILE DELLA REPOSITORY:'"+_repo_name+"'\n"+ list;
        }else{
            return Messaggi.NONESISTEREPOLOCALE.getMessage();
        }

    }


    /**
     * metodo per conoscere il contenuto di un file presente nella repository locale.
     * @param name name una String, nome del file da leggere .
     * @return  String,  contenuto del file oppure errore.
     */
    public String getContentFile(String name) throws FileNotFoundException {
        for(File f: this.repo.getFiles())
        {
            if(f.getName().equals(name))
            {
                String content="";
                Scanner sc = new Scanner(f);
                while (sc.hasNextLine()){
                    content += sc.nextLine();
                }
                 return "Testo del file "+ name + ":\n\n" + content;
            }
        }
        return "FILE "+ "'"+ name + "'" + " NON TROVATO";
    }

    /**
     * metodo get per scaricare la repository dalla dht
     * @param _repo_name name una String, nome della repository da scaricare .
     * @return  Repository,  oggetto di tipo Repository contenuto della DHT.
     */
    private Repository getRepoFromDht(String _repo_name) throws IOException, ClassNotFoundException {
        FutureGet futureGet = peerDht.get(Number160.createHash(_repo_name)).start();
        futureGet.awaitUninterruptibly();
        if (futureGet.isSuccess()) {
            Collection<Data> dataMapValues = futureGet.dataMap().values();
            if (dataMapValues.isEmpty()) {
                return null;
            }
            return (Repository) futureGet.dataMap().values().iterator().next().object();
        }
        return null;
    }

    /**
     * metodo put per caricare la repository nella DHT
     * @param _repo_name name una String, nome della repository da scaricare.
     * @param repo Repository, oggetto Repository da caricare nella DHT.
     * @return  boolean,  true se la repository è stata caricata nella DHT, false altriemnti.
     */
    public boolean putRepoToDht(String _repo_name, Repository repo) throws IOException {
        peerDht.put(Number160.createHash(_repo_name)).data(new Data(repo)).start().awaitUninterruptibly();
        return true;
    }

    /**
     * metodo put per caricare la repository nella DHT
     * @param _id  un intero, id dell'utente che ha aggiornato la repository.
     * @param _repo_name name una String, nome della repository.
     * @param myIp PeerAddress, peerAddress dell'utente che ha aggiornato la repository nella DHT.
     * @return  boolean,  true se è stato mandato il messaggio ai contributori, false altriemnti.
     */
    public boolean sendMessage(int _id, String _repo_name, PeerAddress myIp) throws IOException, ClassNotFoundException {
        Repository repo_Message;
        FutureGet futureGet = peerDht.get(Number160.createHash(_repo_name)).start();
        futureGet.awaitUninterruptibly();
        if (futureGet.isSuccess()) {
            Collection<Data> dataMapValues = futureGet.dataMap().values();
            if (dataMapValues.isEmpty()) {
                return false;
            }
            repo_Message= (Repository) futureGet.dataMap().values().iterator().next().object();
            ArrayList<PeerAddress> contributors = repo_Message.getPeerAddress();

            for(PeerAddress ip : contributors )
            {
                if(!myIp.equals(ip))
                {

                    FutureDirect futureDirect = peerDht.peer().sendDirect(ip).object("E' stata effettuata una push dall'utente "+_id + ". Fare una pull per aggiornare la repository").start();
                    futureDirect.awaitUninterruptibly();
                }
            }
        }
        return false;
    }

    public void pendingSet(int p)
    {
        this.pendingCommit = p;
    }


    public void leaveNetwork() {
        peerDht.peer().announceShutdown().start().awaitUninterruptibly();

    }

    public Repository getRepo()
    {
        return this.repo;
    }

}
