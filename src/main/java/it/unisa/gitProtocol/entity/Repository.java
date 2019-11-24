package it.unisa.gitProtocol.entity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

import com.google.common.io.CharStreams;

import java.util.Date;


public class Repository implements Serializable{

    private final Map<File, String> filemap =  new HashMap<File,  String>();;

    private final ArrayList<File> files = new ArrayList<File>();        // list of files
    private ArrayList<Commit> commits;    // list of commits
    public static final String FIRST_COMMIT_MESSAGE = "First commit";
    private String repoName;                    // name of the repository
    private String directory;               // root directory of the repository


    public Repository(String directory, String repoName) throws IOException{
        this.directory = directory;
        this.repoName = repoName;
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

    @Override
    public String toString() {
        return "Repository{" +
                "filemap=" + filemap +
                ", files=" + files +
                ", commits=" + commits +
                ", repoName='" + repoName + '\'' +
                ", directory='" + directory + '\'' +
                '}';
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

    public boolean addFiles(List<File> listFiles) throws IOException {
        if (listFiles.isEmpty()) return false;
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

    //METODO PER IL MERGE DEI COMMIT TRA REPO LOCALE E QUELLA DELLA DHT
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
    public boolean updateRepoWithPending(Repository dhtRepo, boolean merge) throws IOException {

        System.out.println("SONO NEL METODO");
        for(Map.Entry<File, String> entry: dhtRepo.getFilemap().entrySet()) {
            File localFile = null;
            boolean find = false;
            for (Map.Entry<File, String> entryLocal : this.getFilemap().entrySet()) {
                if (entryLocal.getKey().getName().equals(entry.getKey().getName()) && !entryLocal.getValue().equals(entry.getValue())) {
                    OutputStream os = null;
                    //merge
                    System.out.println("dovrei fare il merge del file " + entry.getKey().toString());
                    String text = "\n    MERGE     \n";
                    for (File f : this.files) {
                        System.out.println("nome f " + entry.getKey().getName() + "  files f " + f.getName());
                        if (f.getName().equals(entry.getKey().getName())) {
                            if (merge==true){
                                os = new FileOutputStream(f,true);
                                os.write(text.getBytes());
                                os.write(entry.getValue().getBytes());

                            }else{
                                os = new FileOutputStream(f);
                                os.write(entry.getValue().getBytes());
                            }
                            os.flush();
                            os.close();

                            entryLocal.setValue(getTextFile(f));
                            find = true;
                            break;
                        }
                    }
                }else if(entryLocal.getKey().getName().equals(entry.getKey().getName()) && entryLocal.getValue().equals(entry.getValue()))
                {
                    find= true;
                    System.out.println("trovato cazzo");
                }


            }
            if (find == false) {
                try {
                    System.out.println("ricreo file"  + entry.getKey().getName() );
                    OutputStream os = null;
                    //System.out.println("directory nella UPDATE" + getDirectory());
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
                try {
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
