package it.unisa.gitProtocol.entity;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.peers.PeerAddress;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

import com.google.common.io.CharStreams;

import java.util.Date;
import java.util.concurrent.Future;


public class Repository implements Serializable{


    private final Map<File, String> filemap =  new HashMap<File,  String>();;    //haashmap per mappare i file presente ella repository in base al loro contenuto.
    private final ArrayList<File> files = new ArrayList<File>();        // lista dei file presenti nella repository
    private ArrayList<Commit> commits;                               // lista di commit
    public static final String FIRST_COMMIT_MESSAGE = "First commit";
    private String repoName;                                         // nome della repository
    private String directory;                                        // root directory della repository
    private ArrayList<PeerAddress> peerAddress = new ArrayList<PeerAddress>(); // lista dei peer address che hanno modificato la repository


    //costruttori
    public Repository(String directory, String repoName, PeerAddress ip) throws IOException
    {
        this.directory = directory;
        this.repoName = repoName;
        peerAddress.add(ip);  // fondatore della repository
        commits = new ArrayList<Commit>();
        this.commits.add(new Commit("Prima commit:Creazione Repository", this.repoName));

    }

    public Repository(){
        commits = new ArrayList<Commit>();
    }

    public Map<File,  String> getFilemap() {
        return filemap;
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public ArrayList<Commit> getCommits() {
        return commits;
    }

    public void setCommits(ArrayList<Commit> commits) {
        this.commits = commits;
    }

    public static String getFirstCommitMessage() {
        return FIRST_COMMIT_MESSAGE;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory= directory;
    }

    public ArrayList<PeerAddress> getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(ArrayList<PeerAddress> peerAddress) {
        this.peerAddress = peerAddress;
    }



    @Override
    public String toString() {
        return  "NOME REPOSITORY: "+repoName + "\n"+
                "FILEMAP:\n" + filemapToString() +
                "FILES:\n" + filesToString() +
                "COMMITS:\n" + commitToString() +
                "DIRECTORY: '" + directory + '\'' +"\n";
    }

    public String filemapToString()
    {
        String result= "";
        for (Map.Entry<File, String> entry: filemap.entrySet())
            result += entry.getKey() + " " + entry.getValue() +"";

        return result;
    }
    public String commitToString()
    {
        String result= "";
        for (Commit c: commits)
            result += c.toString() +"\n";

        return result;
    }
    public String filesToString()
    {
        String result= "";
        for (File f: files)
            result += f.toString() +"\n";

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return Objects.equals(filemap, that.filemap) &&
                Objects.equals(files, that.files) &&
                Objects.equals(commits, that.commits) &&
                Objects.equals(repoName, that.repoName) &&
                Objects.equals(directory, that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filemap, files, commits, repoName);
    }

    //metodo utile all'aggiunta di dile nella repository. Ogni file ricevuto vuene mappato dell'hashmap in base al suo contenuto
    public boolean addFiles(List<File> listFiles) throws IOException {
        if (listFiles.isEmpty()) return false;                         //ricevuto una lista vuota
        for ( File f : listFiles){
            if (this.files.contains(f))
            {
                this.files.remove(f);
                this.files.add(f);
                try{
                    this.filemap.remove(f);
                    this.filemap.put(f, getTextFile(f) );
                }catch (Exception e){
                    System.out.println(e);
                }
            }else{
                 this.files.add(f);
                 try{
                     this.filemap.put(f, getTextFile(f) );
                 }catch (Exception e){
                     System.out.println(e);
                 }
            }
        }
        return true;
    }

    //metodo utile all'aggiunta di un messaggio di commit alla lista pre-esistente
    public boolean addCommit(String message)
    {
        try{
            this.commits.add(new Commit(message, this.repoName));
        }catch (Exception e){
            System.out.println(e);
            return false;
        }
        return true;
    }

    //altra implementazione del metodo addCommit che permette il merge tra le commit della repository locale e quella presente nella dht
    public boolean addCommit(ArrayList<Commit> commitsDht )
    {
        try {
            for(Commit commit : commitsDht)
            {
                if (!commits.contains(commit))
                    commits.add(commit);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }



    public boolean updateRepo(Repository dhtRepo) throws IOException {
        for(Map.Entry<File, String> entry: dhtRepo.getFilemap().entrySet()) {
            OutputStream os = null;
            File localFile = null;
            try {
                System.out.println("directory nella UPDATE" + getDirectory());
                localFile = new File(getDirectory(), entry.getKey().getName());
                os = new FileOutputStream(localFile);
                os.write(entry.getValue().getBytes());
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            try {
                this.filemap.put(localFile, entry.getValue() );
                if(!this.files.contains(localFile))
                    this.files.add(localFile);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    //metodo di aggiornamento della repository della DHT in grado di gestire anche eventuali merge tra file dovute a commit pendenti
    public boolean updateRepoWithPending(Repository dhtRepo, boolean merge) throws IOException {
        for(Map.Entry<File, String> entry: dhtRepo.getFilemap().entrySet()) { //per ogni elemento dell'hashmap della repository nella dht
            File localFile = null;
            boolean find = false;                                               //flag indica se il file è stato trovato
            for (Map.Entry<File, String> entryLocal : this.getFilemap().entrySet()) {
                if (entryLocal.getKey().getName().equals(entry.getKey().getName()) && !entryLocal.getValue().equals(entry.getValue())) { //i file hanno lo stesso nome ma contenuto diverso
                    OutputStream os = null;
                    //merge
                    String text = "\n    MERGE     \n";
                    for (File f : this.files) {                                 //individuo il file locale di cui fare il merge
                        if (f.getName().equals(entry.getKey().getName())) {     // se ha il nome del file in questione
                            if (merge==true){                                   //se devo effettuare il merge
                                os = new FileOutputStream(f,true);      //creo outputStream
                                os.write(text.getBytes());                      //accodo il contenuto del file della dht
                                os.write(entry.getValue().getBytes());

                            }else{
                                os = new FileOutputStream(f);                   //se non devo fare il merge, sostituisco il contenuto del file con quello nella dht
                                os.write(entry.getValue().getBytes());
                            }
                            os.flush();
                            os.close();                                         //flush e chiusura dell'output stream
                            entryLocal.setValue(getTextFile(f));                //aggiorno hashmap con il nuovo contenuto
                            find = true;                                        //flag che attesta che il file è stato già trovato
                            break;
                        }
                    }
                }else if(entryLocal.getKey().getName().equals(entry.getKey().getName()) && entryLocal.getValue().equals(entry.getValue())) //i file hanno lo stesso nome e lo stesso contenuto
                {
                    find= true;                                                //flag che attesta che il file è stato già trovato
                }
            }
            if (find == false) {                                               //flag a false indica che il file non è presente nella repository locale
                try { //creazione file con lo stesso contenuto
                    OutputStream os = null;
                    localFile = new File(getDirectory() + "/" + entry.getKey().getName());
                    os = new FileOutputStream(localFile);
                    String text = "";
                    os.write(entry.getValue().getBytes());
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                try {//inserimento nell'hashmap della repository locale
                    this.filemap.put(localFile, entry.getValue());
                    if (!this.files.contains(localFile))
                        this.files.add(localFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }

    //metodo che permette la lettura del testo presente in un file
    public String getTextFile(File f) throws FileNotFoundException {
        InputStream is = new FileInputStream(f);
        String text="";
        try (final Reader reader = new InputStreamReader(is)) {
            text = CharStreams.toString(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;

    }





}
