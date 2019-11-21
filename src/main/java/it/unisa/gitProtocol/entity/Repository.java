package it.unisa.gitProtocol.entity;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

import com.google.common.io.CharStreams;

import java.util.Date;


public class Repository implements Serializable{

    private final Map<File, Integer> filemap =  new HashMap<File, Integer>();;

    private final ArrayList<File> files =new ArrayList<File>();        // list of files
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



    public Map<File, Integer> getFilemap() {
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

    public boolean addFiles(List<File> listFiles) throws IOException {
        if (listFiles.isEmpty()) return false;
        for ( File f : listFiles){
            if (this.files.contains(f))
            {
                System.out.println("contain "+ f.getName() );
                this.files.remove(f);
                this.files.add(f);
                try{
                    this.filemap.remove(f);
                    this.filemap.put(f, f.hashCode() );
                }catch (Exception e){
                    System.out.println(e);
                }
            }else{
                System.out.println("add " + f.getName());
                 this.files.add(f);
                 try{
                     System.out.println(f.hashCode());
                     this.filemap.put(f, f.hashCode() );
                 }catch (Exception e){
                     System.out.println(e);
                 }

            }

        }
        System.out.println("Riepilogo filemap di taglia:" + filemap.size());
        for( Map.Entry<File, Integer> entry : filemap.entrySet() )
        {
            File f =  entry.getKey();
            int l = entry.getValue();
            System.out.println("file: " + f.getName() + " value= " + l);
        }
        return true;
    }

    public boolean addCommit(String message)
    {
        try{
            this.commits.add(new Commit(message, this.repoName));
            System.out.println(message);
        }catch (Exception e){
            System.out.println(e);
            return false;
        }
        return true;
    }
    public boolean addCommit(ArrayList<Commit> commitsDht )
    {
        System.out.println("quella che ho io ++++++++++++++++ " + commits.toString());
        try {
            for(Commit commit : commitsDht)
            {

                System.out.println("controllo commit fuori " +commits.contains(commit));
                if (!commits.contains(commit))
                {
                    System.out.println("controllo commit dentro " +commits.contains(commit));
                    commits.add(commit);
                }
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public int randomNumber() {


        Random rand = new Random(System.currentTimeMillis());
        int number = rand.nextInt();
        System.out.println("data:"+ new Date());
        return number;
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


    public boolean updateRepo(Repository dhtRepo) throws FileNotFoundException {


        for(Map.Entry<File, Integer> entry: dhtRepo.getFilemap().entrySet()) {
            OutputStream os = null;
            InputStream is = new FileInputStream(entry.getKey());
            File localFile = null;
            try {
                //System.out.println("directory nella UPDATE" + getDirectory());
                localFile = new File(getDirectory() + "/" +entry.getKey().getName());
                os = new FileOutputStream(localFile);
                String text = "";
                try (final Reader reader = new InputStreamReader(is)) {
                    text = CharStreams.toString(reader);
                }
                os.write(text.getBytes());
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
    public boolean updateRepoWithPending(Repository dhtRepo) throws FileNotFoundException {


        for(Map.Entry<File, Integer> entry: dhtRepo.getFilemap().entrySet()) {
            OutputStream os = null;
            InputStream is = new FileInputStream(entry.getKey());
            File localFile = null;
            if(this.filemap.containsKey(entry.getKey()))
            {
                if(!this.filemap.get(entry).equals(entry.getValue())){

                    //merge
                    System.out.println("dovrei fare il merge del file " + entry.getKey().toString());
                }
            }else{
                try {
                    //System.out.println("directory nella UPDATE" + getDirectory());
                    localFile = new File(getDirectory() + "/" +entry.getKey().getName());
                    os = new FileOutputStream(localFile);
                    String text = "";
                    try (final Reader reader = new InputStreamReader(is)) {
                        text = CharStreams.toString(reader);
                    }
                    os.write(text.getBytes());
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

        }

        return true;
    }



}
