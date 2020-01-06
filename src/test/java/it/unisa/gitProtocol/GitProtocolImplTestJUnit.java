package it.unisa.gitProtocol;
import it.unisa.gitProtocol.implementation.*;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class GitProtocolImplTestJUnit {

    private static final String _REPO_NAME = "test";
    private static final String COMMIT_MESSAGE = "Messaggio di test";
    private static final String PATH = "files/";
    private static final String TESTO1 = "Testo 1";
    private static final String TESTO2 = "Testo 2";
    private static final String TESTO3 = "Testo 3";

    private GitProtocolImpl master;
    private GitProtocolImpl peer1;
    private GitProtocolImpl peer2;
    private GitProtocolImpl peer3;
    private File pathDir[];

    public GitProtocolImplTestJUnit() throws Exception {
        master = new GitProtocolImpl(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new GitProtocolImpl(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new GitProtocolImpl(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3= new GitProtocolImpl(3, "127.0.0.1", new MessageListenerImpl(3));
        pathDir = new File[4];

        for (int i = 0; i < 4; i++) {
            String dir_name = "peer" + i;
            File dir = new File(PATH, dir_name);

            if (dir.exists()) {
                FileUtils.deleteDirectory(dir);
            }

            dir.mkdirs();
            pathDir[i]=dir;
        }

    }


    /**
     * TEST CASE 1 : push & pull
     * @result quattro push e pull in sequenza.
     */

    @Test
    public void pushPullQuattroPeer() throws IOException {
        System.out.println("master sta creando la repository");
        master.createRepository(_REPO_NAME, pathDir[0]);

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(pathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        master.addFilesToRepository(_REPO_NAME, files);
        master.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), master.push(_REPO_NAME));
        System.out.println("master push");

        System.out.println("peer1 sta creando la repository");
        peer1.createRepository(_REPO_NAME, pathDir[1]);
        System.out.println("peer 1 effettua la pull della seguente repository");
        assertEquals(Messaggi.SUCCESSOPULL.getMessage(), peer1.pull(_REPO_NAME));

        files.clear();
        for (int i=2; i<4; i++) {
            String name = "file"+(i+1);
            File f = new File(pathDir[1], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO2));
            files.add(f);
        }

        peer1.addFilesToRepository(_REPO_NAME, files);
        peer1.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), peer1.push(_REPO_NAME));
        System.out.println("peer1 push");

        System.out.println("peer2 sta creando la repository");
        peer2.createRepository(_REPO_NAME, pathDir[2]);
        System.out.println("peer 2 effettua la pull della seguente repository");
        assertEquals(Messaggi.SUCCESSOPULL.getMessage(), peer2.pull(_REPO_NAME));

        files.clear();
        for (int i=4; i<6; i++) {
            String name = "file"+(i+1);
            File f = new File(pathDir[2], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO3));
            files.add(f);
        }

        peer2.addFilesToRepository(_REPO_NAME, files);
        peer2.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), peer2.push(_REPO_NAME));
        System.out.println("peer2 push");

        System.out.println("peer3 sta creando la repository");
        peer3.createRepository(_REPO_NAME, pathDir[3]);
        System.out.println("peer 3 effettua la pull della seguente repository");
        assertEquals(Messaggi.SUCCESSOPULL.getMessage(), peer3.pull(_REPO_NAME));

        files.clear();
        for (int i=6; i<8; i++) {
            String name = "file"+(i+1);
            File f = new File(pathDir[3], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO3));
            files.add(f);
        }

        peer3.addFilesToRepository(_REPO_NAME, files);
        peer3.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), peer3.push(_REPO_NAME));
        System.out.println("peer3 push");

        System.out.println("master effettua la pull della seguente repository");
        assertEquals(Messaggi.SUCCESSOPULL.getMessage(), master.pull(_REPO_NAME));

    }

    /**
     * TEST CASE 2 : exploreRepository
     * @result vari peer eplorano la repository.
     */
    @Test
    public void exploreRepo() throws IOException {
        master.createRepository(_REPO_NAME, pathDir[0]);
        List<File> files = new ArrayList<>();
        for(int i=0; i<2; i++)
        {
            String nomeFile = "file"+(i+1);
            File file = new File(pathDir[0], nomeFile+".txt");
            FileUtils.writeLines(file, Collections.singleton(TESTO1));
            files.add(file);

        }

        master.addFilesToRepository(_REPO_NAME, files);
        master.commit(_REPO_NAME, COMMIT_MESSAGE);
        master.push(_REPO_NAME);
        String result="FILE DELLA REPOSITORY:'test'\n" + "1. file1.txt\n" + "2. file2.txt\n";
        assertEquals(result, master.getListFiles(_REPO_NAME));

        //un peer vuole leggere la lista di file presenti nella repository ma la repository locale non esiste.
        assertEquals(Messaggi.NESSUNAREPOLOCALE.getMessage(), peer1.getListFiles("Test"));


        //un peer vuole leggere il contenuto di un file presente nella repository.
        String result2 ="Testo del file file1.txt:\n" + "\n" +
                "Testo 1";
        assertEquals(result2, master.getContentFile("file1.txt"));
        // un peer vuole leggere il contenuto di un file non presente nella repository.
        String result3 = "FILE 'prova' NON TROVATO";
        assertEquals(result3, master.getContentFile("prova"));

        //un peer vuole leggere la lista di file presenti nella repository ma la repository è errata.
        peer1.createRepository("repoErrata", pathDir[1]);
        peer1.addFilesToRepository("repoErrata", files);
        peer1.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.NONESISTEREPOLOCALE.getMessage(), peer1.getListFiles(_REPO_NAME));

        //un peer vuole leggere la lista di file presenti nella repository ma la repository è vuota.
        peer2.createRepository("repoErrata2", pathDir[2]);
        assertEquals(Messaggi.NESSUNFILENELLAREPO.getMessage(), peer2.getListFiles(_REPO_NAME));

    }



    @After
    public void tearDown() throws Exception {
        master.leaveNetwork();
        peer1.leaveNetwork();
        peer2.leaveNetwork();
        peer3.leaveNetwork();
    }

}

