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
        boolean create = false;
        int pendingCommit=0;

        try
        {
            parser.parseArgument(args);
            TextIO textIO = TextIoFactory.getTextIO();
            TextTerminal terminal = textIO.getTextTerminal();
            MessageListener ms = new MessageListenerImpl(id);

            GitProtocolImpl peer = new GitProtocolImpl(id, master, ms);

            System.out.println("id ed ip: " +  id +  master);

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
                        .withMaxVal(4)
                        .withMinVal(1)
                        .read("Option");
                switch (option) {
                    case 1:
                        terminal.printf("\nENTER REPOSITORY NAME\n");
                        String name = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        //terminal.printf("\nENTER DIRECTORY\n");
                        //CREAZIONE PATH
                        /*String dirName="";
                        dirName = textIO.newStringInputReader()
                                .withDefaultValue("/repo")
                                .read("Name:");
                        File dir = new File("/app/"+dirName );
                        dir.mkdir();*/
                        if(peer.createRepository(name, dir ))
                        {
                            terminal.printf("\nREPOSITORY %s SUCCESSFULLY CREATED\n",name);
                            create = true;
                        }else
                            terminal.printf("\nERROR IN REPOSITORY CREATION\n");
                        break;
                    case 2:
                        terminal.printf("\nENTER REPOSITORY NAME\n");
                        String rname = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        List<File> fs = new ArrayList<File>();
                        fs= createFile((ArrayList<File>) fs,textIO, terminal, dir);
                        if(peer.addFilesToRepository(rname, fs))
                        {
                            terminal.printf("\n SUCCESSFULLY ADDED FILES\n");
                            terminal.printf("\nENTER COMMIT MESSAGE\n");
                            String commit = textIO.newStringInputReader()
                                .withDefaultValue("default-commit")
                                .read("commit:");
                            if(peer.commit(rname, commit))
                            {
                                terminal.printf("\n DO YOU WANT TO PUSH? \n");
                                String check = textIO.newStringInputReader()
                                        .withDefaultValue("1")
                                        .read("(1= SI/ENTER, 2= NO):");
                                if(check.equals("1"))
                                {
                                    terminal.printf("\n TRYING PUSH \n");
                                    terminal.printf("\n Message push:%s \n",peer.push(rname));
                                }else{
                                    pendingCommit++;
                                    peer.pendingSet(pendingCommit);
                                    terminal.printf("\n REMEMBER!, YOU HAVE %d PENDING COMMIT \n", pendingCommit);
                                }
                            }else
                                terminal.printf("\nERROR IN COMMIT CREATION\n");
                        }else
                            terminal.printf("\nERROR IN ADD FILE TO REPOSITORY\n");
                        break;
                    case 3:
                        terminal.printf("\nENTER REPOSITORY NAME\n");
                        String rname2 = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        if(create==false)
                            if(peer.createInitialRepository(rname2, dir) )
                                create=true;

                        terminal.printf("\n%s\n",peer.pull(rname2));
                        break;
                    case 4:
                        terminal.printf("\nENTER REPOSITORY NAME\n");
                        String rname3 = textIO.newStringInputReader()
                                .withDefaultValue("default-name repo")
                                .read("Name:");
                        terminal.printf("\n TRYING PUSH \n");
                        String pushCeck=peer.push(rname3);
                        terminal.printf("\n Message push:%s \n",pushCeck);
                        if(pushCeck.equals("Push avvenuta con successo"))
                            pendingCommit=0;
                        break;
                    case 5:
                        terminal.printf("\nARE YOU SURE TO LEAVE THE NETWORK?\n");
                        boolean exit = textIO.newBooleanInputReader().withDefaultValue(false).read("exit?");
                        if(exit) {
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
        String nFilesS = textIO.newStringInputReader()
                .withDefaultValue("1")
                .read("Number:");
        for(int i=0; i<Integer.parseInt(nFilesS); i++)
        {
            terminal.printf("\nENTER FILE NAME NUMBER(%d)\n", i+1);
            String fname = textIO.newStringInputReader()
                    .withDefaultValue("prova")
                    .read("Name:");
            File f = new File(  dir, fname+".txt");

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
                    System.out.println("file========== "+ st);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            files.add(f);
        }
        return files;
    }

    public static void printMenu(TextTerminal terminal) {
        terminal.printf("\n1 - CREATE REPOSITORY\n");
        //terminal.printf("\n2 - SUBSCRIBE TO REPOSITORY\n");
        terminal.printf("\n2 - ADD FILES TO REPOSITORY\n");
        terminal.printf("\n3 - PULL\n");
        terminal.printf("\n5 - PUSH\n");
        terminal.printf("\n4 - EXIT\n");

    }




}



