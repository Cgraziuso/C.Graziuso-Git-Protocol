package it.unisa.gitProtocol.implementation;

import org.apache.commons.io.FileUtils;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Example {

    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;

    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;

    public static void main(String[] args) throws Exception {

        Example example = new Example();
        final CmdLineParser parser = new CmdLineParser(example);
        boolean create = false;                                                     //flag per verificare la creazione della repository
        int pendingCommit=0;                                                        //intero utile per verificare la presenza di commit pendenti
        String nome = "";
        String commit = "";
        String check = "";
        try
        {
            parser.parseArgument(args);
            TextIO textIO = TextIoFactory.getTextIO();
            TextTerminal terminal = textIO.getTextTerminal();

            MessageListener ms = new MessageListenerImpl(id);                       //Message Listner per lo scambio di messaggi
            GitProtocolImpl peer = new GitProtocolImpl(id, master, ms);             //Istanza di gitProtocolImpl per la gestione della repository

            //CREAZIONE PATH
            File dir = new File(id+"");
            if(dir.exists())
                FileUtils.deleteDirectory(dir);
            dir.mkdir();

            //INIZIO COMUNICAZIONE
            terminal.printf("\nStaring peer id: %d on master node: %s\n", id, master);
            while(true) {
                printMenu(terminal);
                int option = textIO.newIntInputReader()
                        .withMaxVal(6)
                        .withMinVal(1)
                        .read("Option");
                switch (option) {
                    case 1:
                        terminal.printf("\nINSERIRE NOME DELLA REPOSITORY\n");
                        nome = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        if(peer.createRepository(nome, dir ))
                        {
                            terminal.printf(Messaggi.REPOCREATA.getMessage(),nome);
                            create = true;
                        }else
                            terminal.printf(Messaggi.ERRORECREAZIONEREPO.getMessage());
                        break;
                    case 2:
                        terminal.printf("\nINSERIRE NOME DELLA REPOSITORY\n");
                        nome = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        List<File> fs = new ArrayList<File>();
                        fs= createFile((ArrayList<File>) fs,textIO, terminal, dir);
                        if(peer.addFilesToRepository(nome, fs))
                        {
                            terminal.printf("\n FILE AGGIUNTI CON SUCCESSO\n");
                            terminal.printf("\nINSERIRE IL MESSAGGIO DI COMMIT\n");
                            commit = textIO.newStringInputReader()
                                .withDefaultValue("default-commit")
                                .read("commit:");
                            if(peer.commit(nome, commit))
                            {
                                terminal.printf("\n VUOI EFFETTUARE UNA PUSH? \n");
                                 check = textIO.newStringInputReader()
                                        .withDefaultValue("1")
                                        .read("(1= SI/ENTER, 2= NO):");
                                if(check.equals("1"))
                                {
                                    terminal.printf("\n MESSAGGIO PUSH:%s \n",peer.push(nome));
                                }else{
                                    pendingCommit++;
                                    peer.pendingSet(pendingCommit);
                                    terminal.printf("\nHAI %d COMMIT PENDENTE/I \n", pendingCommit);
                                }
                            }else
                                terminal.printf(Messaggi.ERROREMESSAGGIOCOMMIT.getMessage());
                        }else
                            terminal.printf(Messaggi.ERROREAGGIUNTAFILE.getMessage());
                        break;
                    case 3:
                        terminal.printf("\nINSERIRE NOME DELLA REPOSITORY\n");
                        nome = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        if(create==false)
                            if(peer.createInitialRepository(nome, dir) )
                                create=true;

                        terminal.printf("\n%s\n",peer.pull(nome));
                        break;
                    case 4:
                        terminal.printf("\nINSERIRE NOME DELLA REPOSITORY\n");
                        nome = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        String pushCeck=peer.push(nome);
                        terminal.printf("\n MESSAGGIO PUSH: %s \n",pushCeck);
                        if(pushCeck.equals(Messaggi.SUCCESSOPUSH.getMessage()))
                            pendingCommit=0;
                        break;
                    case 5:
                        terminal.printf("\nINSERIRE NOME DELLA REPOSITORY\n");
                        nome = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        String list = (peer.getListFiles(nome));
                        if(list.equals(Messaggi.NESSUNAREPOLOCALE.getMessage()) || list.equals(Messaggi.NESSUNFILENELLAREPO.getMessage()) || list.equals(Messaggi.NONESISTEREPOLOCALE.getMessage())) {
                            terminal.printf("\n" + list + "\n");
                            break;
                        }else{
                            String lettura ="";
                           do{
                                terminal.printf("\n" + list + "\n");
                                terminal.printf("\nVUOI LEGGERE IL CONTENUTO DI UN FILE?\n");
                                String option2 = textIO.newStringInputReader()
                                        .withDefaultValue("1")
                                        .read("1/si 2/no:");
                                if (option2.equals("2") || option2.equals("no")) {
                                    break;
                                } else {
                                    terminal.printf("\n" + list + "\n");
                                    terminal.printf("\nINSERIRE IL NOME DEL FILE CHE VUOI LEGGERE (ANCHE L'ESTENSIONE)\n");
                                    String nameFile = textIO.newStringInputReader()
                                            .read("name:");
                                    terminal.printf("\n" + peer.getContentFile(nameFile) + "\n");
                                    terminal.printf("\nVUOI LEGGERE IL CONTENUTO DI UN ALTRO FILE?\n");
                                    lettura = textIO.newStringInputReader()
                                            .withDefaultValue("1")
                                            .read("1/si 2/no:");
                                }
                            }while (lettura.equals("1") || lettura.equals("si"));
                                break;
                        }
                    case 6:
                        terminal.printf("\nSEI SICURO DI VOLER LASCIARE LA NETWORK?\n");
                        boolean exit = textIO.newBooleanInputReader().withDefaultValue(false).read("exit?");
                        if(exit) {
                            peer.leaveNetwork();
                            System.exit(0);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        catch (CmdLineException clEx)
        {
            System.err.println("ERROR: Unable to parse command-line options: " + clEx);
        }
    }

    public static ArrayList<File> createFile(ArrayList<File> files, TextIO textIO,  TextTerminal terminal, File dir) throws IOException {


        terminal.printf("\nQUANTI FILE VUOI CREARE?\n");
        int nFiles = textIO.newIntInputReader()
                .withDefaultValue(1)
                .read("Number:");
        for(int i=0; i<nFiles; i++)
        {
            terminal.printf("\nINSERIRE IL NOME DEL FILE NUMERO %d\n", i+1);
            String name = textIO.newStringInputReader()
                    .withDefaultValue("prova")
                    .read("Name:");
            File f = new File(  dir, name +".txt");
            terminal.printf("\nENTER TEXT OF FILE\n");
            String data = textIO.newStringInputReader()
                    .withDefaultValue("Lorem ipsum dolor sit amet")
                    .read("Text:");
            try{
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data.getBytes());
                fos.flush();
                fos.close();
                BufferedReader br = new BufferedReader(new FileReader(f));
                String st;
                while ((st = br.readLine()) != null){
                    System.out.println("file "+ st);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            files.add(f);
        }
        return files;
    }

    public static void printMenu(TextTerminal terminal) {
        terminal.printf("\n1 - CREARE UNA REPOSITORY\n");
        terminal.printf("\n2 - AGGIUNGERE FILE ALLA REPOSITORY\n");
        terminal.printf("\n3 - PULL\n");
        terminal.printf("\n4 - PUSH\n");
        terminal.printf("\n5 - ESPLORA LA REPOSITORY\n");
        terminal.printf("\n6 - EXIT\n");

    }




}



