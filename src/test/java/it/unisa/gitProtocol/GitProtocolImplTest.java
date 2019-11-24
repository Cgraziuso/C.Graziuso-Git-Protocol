package it.unisa.gitProtocol;

import it.unisa.gitProtocol.implementation.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class GitProtocolImplTest {

    public static void main(String[] args) throws Exception {



        GitProtocolImpl peer1 = new GitProtocolImpl(0, "127.0.0.1", new MessageListenerImpl(0));
        GitProtocolImpl peer2 = new GitProtocolImpl(1, "127.0.0.1", new MessageListenerImpl(1));

        File dirPeer1 = new File(1+"");
        if(dirPeer1.exists())
        {
            FileUtils.deleteDirectory(dirPeer1);
        }
        dirPeer1.mkdir();
        File dirPeer2 = new File(2+"");
        if(dirPeer2.exists())
        {
            FileUtils.deleteDirectory(dirPeer2);
        }
        dirPeer2.mkdir();


        if(peer1.createRepository("ciao", dirPeer1))
        {
            System.out.println("creazione della repo avvenuta con successo");
        }

        //CREAZIONE FILE PER REPO PEER 1
        ArrayList<File> files = addfilepeer(new ArrayList<File>(), "testo del primo file", "1", "prova", dirPeer1);

        //ADD FILE, COMMIT E PUSH
        addCommitPush(peer1, files, "commit file 1");
        files.clear();

        //PULL PEER 2
        peer2.createInitialRepository("ciao", dirPeer2);
        String pull = peer2.pull("ciao");
        System.out.println(pull);

        //CREAZIONE FILE PER REPO  DA PEER 2
        files = addfilepeer(new ArrayList<File>(), "testo del secondo file", "2", "prova2", dirPeer2);

        //ADD FILE, COMMIT E PUSH
        addCommitPush(peer2, files, "commit file 2");
        files.clear();

        //PULL PEER 1
        pull = peer1.pull("ciao");
        System.out.println(pull);

        //CREAZIONE FILE PER REPO PEER 1
        files = addfilepeer(new ArrayList<File>(), "testo del terzo file", "1", "prova3", dirPeer1);

        //ADD FILE, COMMIT  del peer 1
        addCommit(peer1, files, "commit file 3");
        files.clear();

        //CREAZIONE FILE PER REPO PEER 2
        files = addfilepeer(new ArrayList<File>(), "testo del terzo file file peer 2", "2", "prova3", dirPeer2);

        //ADD FILE, COMMIT E PUSH
        addCommitPush(peer2, files, "commit file 3 del peer 2");
        files.clear();

        //PULL PEER 1
        pull = peer1.pull("ciao");
        System.out.println(pull);


        String push = peer1.push("ciao");
        System.out.println(push);


        //PULL PEER 1
        pull = peer2.pull("ciao");
        System.out.println(pull);




    return;
    }

    public static ArrayList<File> addfilepeer(ArrayList<File> files, String data, String id, String name, File dir){
        File file1 = new File(dir+"/", name+".txt");
        try{
            FileOutputStream fos = new FileOutputStream(file1);
            fos.write(data.getBytes());
            fos.flush();
            fos.close();

        }catch (Exception e){
            e.printStackTrace();
        }
        files.add(file1);
        return  files;

    }

    public static void addCommitPush(GitProtocolImpl peer, ArrayList<File>files, String message)
    {
        if(peer.addFilesToRepository("ciao", files))
        {
            System.out.println("\n SUCCESSFULLY ADDED FILES\n");

            if(peer.commit("ciao", message))
            {

                String push = peer.push("ciao");
                System.out.println(push);

            }else{
                System.out.println("\n TUTTO A PUTTANE \n");
            }
        }
    }

    public static void addCommit(GitProtocolImpl peer, ArrayList<File>files, String message)
    {
        if(peer.addFilesToRepository("ciao", files))
        {
            System.out.println("\n SUCCESSFULLY ADDED FILES\n");

            if(peer.commit("ciao", message))
            {
                peer.pendingSet(1);
                System.out.println("\n SOLO COMMIT OK \n");

            }else{
                System.out.println("\n TUTTO A PUTTANE \n");
            }
        }
    }







}
