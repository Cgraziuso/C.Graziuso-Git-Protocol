package it.unisa.gitProtocol.implementation;

public enum Messaggi {

    //push
    SUCCESSOPUSH("Push avvenuta con successo"),

    //repo
    NONESISTEREPOLOCALE("NON ESISTE UNA REPOSITORY LOCALE CON QUESTO NOME"),
    NONESISTEREPO("NON ESISTE UNA REPOSITORY CON QUESTO NOME"),
    NESSUNAREPOLOCALE("NON HAI UNA REPOSITORY LOCALE."),
    NESSUNFILENELLAREPO("NON HAI FILE NELLA REPOSITORY LOCALE."),

    //file
    ERROREAGGIUNTAFILE ("\nERRORE NELL'AGGIUNTA DEI FILE NELLA REPOSITORY\n"),

    //creazione repo
    ERRORECREAZIONEREPO ("\nERRORE NELLA CREAZIONE DELLA REPOSITORY\n"),
    REPOCREATA ("\nREPOSITORY %s CREATA CON SUCCECSSO\n"),

    //stato della repository
    REPONONAGGIORNATA("ERRORE NELLA PUSH, LA REPOSITORY NON E' AGGIORNATA. FARE LA PULL"),
    REPOAGGIORNATA("HAI GIA' LA VERSIONE PIU' AGGIORNATA DELLA REPOSITORY"),

    //pull
    SUCCESSOPULL("PULL AVVENUTA CON SUCCESSO"),

    //commit
    ERROREMESSAGGIOCOMMIT("\nERRORE NELLA CREAZIONE DEL MESSAGGIO DI COMMIT\n");




    private String message;
    Messaggi(String s) {
        this.message = s;
    }

    public String getMessage(){
        return message;
    }
}