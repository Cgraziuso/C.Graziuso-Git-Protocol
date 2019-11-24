package it.unisa.gitProtocol.implementation;

import it.unisa.gitProtocol.entity.Commit;
import it.unisa.gitProtocol.entity.Repository;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import net.tomp2p.p2p.Peer;

public class GitProtocolImpl implements GitProtocol {


    //private final Storage storage;
    private Repository repo;
    final private Peer peerb;
    final private PeerDHT peer;
    final private int DEFAULT_MASTER_PORT=4000;
    private int pendingCommit=0;


    public GitProtocolImpl(int id, String master_peer, final MessageListener ms) throws Exception {
        //this.storage = _storage;
        this.repo = null;
        peerb= new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT).start();
        peer = new PeerBuilderDHT(peerb).start();
        //peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT).start()).start();

        FutureBootstrap fb = peerb.bootstrap().inetAddress(InetAddress.getByName(master_peer)).ports(DEFAULT_MASTER_PORT).start();
        fb.awaitUninterruptibly();
        if(fb.isSuccess()) {
            peerb.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }else {
            throw new Exception("Error in master peer bootstrap.");
        }

        File fs = new File("app/" + id + "/");
        fs.mkdir();

    }

    public boolean createRepository(String _repo_name, File _directory) {
        if (this.repo != null) {
            return false;
        }
        try {
            this.repo = new Repository(_directory.getPath(), _repo_name);
            System.out.println(repo.toString());
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean createInitialRepository(String _repo_name, File _directory) {
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

    public boolean addFilesToRepository(String _repo_name, List<File> files) {
        try {
            return this.repo != null && this.repo.addFiles(files);
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean commit(String _repo_name, String _message) {

        try {
            return this.repo != null && this.repo.addCommit(_message);
        } catch (Exception ex) {
            return false;
        }
    }


    //push
    public String push(String _repo_name) {
        if (this.repo == null)
            return "Nessuna repository locale.";
        try {
            Repository dhtRepo = getRepoFromDht(_repo_name);
            //contains lists
            int check=0;
            if(pendingCommit>1)
                check = pendingCommit-1;
            if (dhtRepo == null || this.repo.getCommits().containsAll(dhtRepo.getCommits() ) )
            {
                if(dhtRepo!=null)
                    System.out.println(" push dht repo "  + dhtRepo.toString());
                if(putRepoToDht(_repo_name, this.repo) )
                {
                    pendingSet(0);
                    return "Push avvenuta con successo";
                }
            }else{
                return  "ERRORE NELLA PUSH, LA REPOSITORY NON E' AGGIORNATA. FARE LA PULL";
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return "ERRORE NELLA DHT GET/PUT: " + e.getMessage();
        }
        return "ERRORE NELLA PUSH";
    }

    public String pull(String _repo_name) {
        try{
            Repository dhtRepo = getRepoFromDht(_repo_name);
            System.out.println("inizio pull dth repo"  + dhtRepo.toString());
            if (dhtRepo == null) {
                return "Non esiste una repository con questo nome";
            }
            if(dhtRepo.hashCode()==this.repo.hashCode()) { //nn va bene
                return "hai già la versione più recente della repository";
            }

            System.out.println("le mie pending commit  "+ pendingCommit);
            if(pendingCommit>0)
            {
                this.repo.updateRepoWithPending(dhtRepo, true);
                pendingSet(0);
            }else{
                System.out.println("SONO IN ELSE " );
                this.repo.updateRepoWithPending(dhtRepo, false);         // possibile errore
            }

                this.repo.addCommit(dhtRepo.getCommits());

            System.out.println("dopo pull mia repo " + this.repo.toString());
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("la mia repo"  + this.repo.toString());
        return "Operazione di pull avvenuta con successo";
    }


    private Repository getRepoFromDht(String repo_name) throws IOException, ClassNotFoundException {
        FutureGet futureGet = peer.get(Number160.createHash(repo_name)).start();
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

    public boolean putRepoToDht(String _repo_name, Repository repo) throws IOException {
        peer.put(Number160.createHash(_repo_name)).data(new Data(repo)).start().awaitUninterruptibly();
        return true;
    }

    public void pendingSet(int p)
    {
        this.pendingCommit = p;
    }


}
