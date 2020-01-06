package it.unisa.gitProtocol;

import it.unisa.gitProtocol.entity.Repository;
import it.unisa.gitProtocol.implementation.*;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class GitProtocolImplTest {

    private static final String _REPO_NAME = "test";
    private static final String COMMIT_MESSAGE = "Messaggio di test";
    private static final String PATH = "files/";
    private static final String TESTO1 = "Testo 1";
    private static final String TESTO2 = "Testo 2";
    private static final String TESTO3 = "Testo 3";
    private static final String TESTO4 = "Testo 4";

    private GitProtocolImpl master;
    private GitProtocolImpl peer1;
    private GitProtocolImpl peer2;
    //private GitProtocolImpl peer3;
    private File PathDir[];

    public GitProtocolImplTest() throws Exception {
        master = new GitProtocolImpl(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new GitProtocolImpl(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new GitProtocolImpl(2, "127.0.0.1", new MessageListenerImpl(2));
        //peer3= new GitProtocolImpl(3, "127.0.0.1", new MessageListenerImpl(3));
        PathDir = new File[4];

    }

    @Before
    public void setUp() throws Exception {

        for (int i = 0; i < 4; i++) {
            String dir_name = "peer" + i;
            File dir = new File(PATH, dir_name);

            if (dir.exists()) {
                FileUtils.deleteDirectory(dir);
            }

            dir.mkdirs();
            PathDir[i]=dir;
        }
    }

    /**
     * TEST CASE 1 : createRepository
     * @result Repository creata con successo.
     */
    @Test
    public void createRepository() {
        assertTrue(master.createRepository(_REPO_NAME, PathDir[0]));
    }

    /**
     * TEST CASE 2 : createRepository
     * @result Creare una seconda Repository.
     */
    @Test
    public void createSecondRepository() {
        master.createRepository(_REPO_NAME, PathDir[0]);
        assertFalse(master.createRepository(_REPO_NAME, PathDir[0]));
        System.out.println("esiste già una repository");
    }

    /**
     * TEST CASE 3 : addFilesToRepository
     * @result Aggiunta di file nella repository locale.
     */
    @Test
    public void addFilesToRepository() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        assertTrue(master.addFilesToRepository(_REPO_NAME, files));
        Repository repo = master.getRepo();
        System.out.println("aggiunti file \n" + repo.getFiles().toString() );
    }

    /**
     * TEST CASE 4 : addFilesToRepository
     * @result Aggiunta di file in una repository locale diversa da quella creata.
     */
    @Test
    public void addFilesToRepositoryRepoErrata() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        assertFalse(master.addFilesToRepository("repo", files));
        System.out.println("repository locale ha un nome diverso");

    }

    /**
     * TEST CASE 5 : addFilesToRepository
     * @result Aggiunta di file in una repository inesistente.
     */
    @Test
    public void addFilesToRepositoryInesistente() throws IOException {

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        assertFalse(master.addFilesToRepository(_REPO_NAME, files));
        System.out.println("repository inesistente");

    }

    /**
     * TEST CASE 6 : commit
     * @result effettuare una commit.
     */
    @Test
    public void commit() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);
        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        master.addFilesToRepository("repo", files);
        assertTrue(master.commit(_REPO_NAME, COMMIT_MESSAGE));
        Repository repo = master.getRepo();
        System.out.println("aggiunta commit \n" + COMMIT_MESSAGE + " alla lista " + repo.getCommits() );

    }

    /**
     * TEST CASE 7 : commit
     * @result effettuare una commit su una repository locale diversa.
     */
    @Test
    public void commitRepoSbagliata() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);
        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        master.addFilesToRepository("repo", files);
        assertFalse(master.commit("repo", COMMIT_MESSAGE));
        System.out.println("repository locale diversa da quella indicata per la commit");
    }

    /**
     * TEST CASE 8 : push
     * @result tre push in sequenza.
     */

    @Test
    public void pushQuattroPeer() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        master.addFilesToRepository(_REPO_NAME, files);
        master.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), master.push(_REPO_NAME));

        peer1.createRepository(_REPO_NAME, PathDir[1]);
        System.out.println("peer 1 ha fatto la pull della seguente repository");
        peer1.pull(_REPO_NAME);

        files.clear();
        for (int i=2; i<4; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[1], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO2));
            files.add(f);
        }

        peer1.addFilesToRepository(_REPO_NAME, files);
        peer1.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), peer1.push(_REPO_NAME));

        peer2.createRepository(_REPO_NAME, PathDir[2]);
        System.out.println("peer 2 ha fatto la pull della seguente repository");
        peer2.pull(_REPO_NAME);

        files.clear();
        for (int i=4; i<6; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[2], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO3));
            files.add(f);
        }

        peer2.addFilesToRepository(_REPO_NAME, files);
        peer2.commit(_REPO_NAME, COMMIT_MESSAGE);
        assertEquals(Messaggi.SUCCESSOPUSH.getMessage(), peer2.push(_REPO_NAME));

        Repository repo = peer2.getRepo();
        System.out.println("peer 2 ha effettuato l'ultima push con file \n" + repo.getFiles().toString() );

    }

    /**
     * TEST CASE 9 : push
     * @result un peer effettua una push ad una repository non aggiornata.
     */
    @Test
    public void ConflittoPush() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);

        List<File> files = new ArrayList<>();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[0], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        master.addFilesToRepository(_REPO_NAME, files);
        master.commit(_REPO_NAME, COMMIT_MESSAGE);
        master.push(_REPO_NAME);

        peer1.createRepository(_REPO_NAME, PathDir[1]);

        files.clear();
        for (int i=0; i<2; i++) {
            String name = "file"+(i+1);
            File f = new File(PathDir[1], name + ".txt");
            FileUtils.writeLines(f, Collections.singleton(TESTO1));
            files.add(f);
        }

        peer1.addFilesToRepository(_REPO_NAME, files);
        peer1.commit(_REPO_NAME, COMMIT_MESSAGE);
        peer1.push(_REPO_NAME);

        assertEquals(Messaggi.REPONONAGGIORNATA.getMessage(), peer1.push(_REPO_NAME));
        System.out.println(Messaggi.REPONONAGGIORNATA.getMessage());
    }

    /**
     * TEST CASE 10 : push
     * @result push con una repository locale non esistente.
     */
    @Test
    public void PushRepositoryInesistente() throws IOException {

        assertEquals(Messaggi.NESSUNAREPOLOCALE.getMessage(), master.push(_REPO_NAME));
        System.out.println("repository locale inesistente");


    }


    /**
     * TEST CASE 11 : pull
     * @result effettua una pull di una repository inesistente.
     */
    @Test
    public void pullRepositoryInesistente() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);
        assertEquals(Messaggi.NONESISTEREPO.getMessage(), master.pull(_REPO_NAME));
        System.out.println(Messaggi.NONESISTEREPO.getMessage());
    }

    /**
     * TEST CASE 12 : pull
     * @result effettua una pull ma la repository è aggiornata.
     */
    @Test
    public void pullRepositoryAggiornata() throws IOException {
        master.createRepository(_REPO_NAME, PathDir[0]);
        List<File> files = new ArrayList<>();
        for(int i=0; i<3; i++)
        {
            String nomeFile = "file"+i;
            String nomeTesto = "TESTO";
            File file = new File(PathDir[0], nomeFile+".txt");
            FileUtils.writeLines(file, Collections.singleton(TESTO1));
            files.add(file);

        }

        master.addFilesToRepository(_REPO_NAME, files);
        master.commit(_REPO_NAME, COMMIT_MESSAGE);
        master.push(_REPO_NAME);
        assertEquals(Messaggi.REPOAGGIORNATA.getMessage(), master.pull(_REPO_NAME));
        System.out.println(Messaggi.REPOAGGIORNATA.getMessage());
    }

    /**
     * TEST CASE 13 : createInitialRepository
     * @result si crea una repository iniziale pronta ad accogliere quella presente nella DHT.
     */
    @Test
    public void createInitialRepository(){

        assertTrue(peer1.createInitialRepository(_REPO_NAME, PathDir[1]));
        System.out.println("repository iniziale creata");

    }

    /**
     * TEST CASE 14 : createInitialRepository
     * @result si crea una seconda repository iniziale  pronta ad accogliere quella presente nella DHT.
     */
    @Test
    public void createSecondInitialRepository(){

        peer1.createInitialRepository(_REPO_NAME, PathDir[1]);
        assertFalse(peer1.createInitialRepository(_REPO_NAME, PathDir[1]));
        System.out.println("repository duplicata");


    }

    @After
    public void tearDown() throws Exception {
        master.leaveNetwork();
        peer1.leaveNetwork();
        peer2.leaveNetwork();
        //peer3.leaveNetwork();
    }




}
