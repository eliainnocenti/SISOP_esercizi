/*

************************************************************************************************

ELIA INNOCENTI (matricola 7080561) - Compito 4-07-2023 - File Main.java

************************************************************************************************

*/

import java.util.ArrayList;

//la classe pubblica Main viene spostata al primo posto
//le altre classi con i relativi attributi e metodi rispettano l'ordine del compito svolto su carta

public class Main {
    public static void main(String[] args) throws InterruptedException {

        int N = 10; //può essere scelto qualsiasi numero di giocatori

        Tavolo t = new Tavolo(N);

        //dichiaro e inizializzo prima i giocatori, visto che banco deve ricevere anche l'array di giocatori

        Giocatore[] g = new Giocatore[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Giocatore(t, i);
            g[i].setName("G" + i);
            g[i].start();
        }

        Banco b = new Banco(t, g);
        b.setName("Banco");
        b.start();

        b.join();

        for (Giocatore gg:g) {
            //il main non chiama più direttamente il metodo per l'interruzione dei thread Giocatore, ma l'interruzione
            // viene gestita dal thread Banco
            //-> viene tutto spiegato più avanti
            gg.join();
        }

        for (Giocatore gg:g) {
            System.out.println(gg.getName() + " ha vinto " + gg.getNVictory() + " volte");
        }

        System.out.println("Programma terminato");
    }
}

class Giocata {
    private int value;              //ho posto il livello di accesso degli attributi come privato
    private int idGiocatore;        //nel compito è rimasto pubblico poiché ho aggiunto i metodi get successivamente

    public Giocata(int v, int id) {
        this.value = v;
        this.idGiocatore = id;
    }

    public int getValue() {
        return value;
    }

    //ho aggiunto un metodo get per accedere all'attributo idGiocatore
    public int getIdGiocatore() {
        return idGiocatore;
    }
}

class Tavolo {
    private ArrayList<Giocata> tavolo = new ArrayList<Giocata>();
    private int nGiocatori;
    private ArrayList<Integer> giocatore = new ArrayList<Integer>();
    private boolean[] giocato;
    private int nGiocate = 0;
    private boolean ready = false;

    public Tavolo(int N) {
        this.nGiocatori = N;
        giocato = new boolean[N];
        for (int i = 0; i < giocato.length; i++) {
            giocato[i] = false;
        }
    }

    public synchronized void play(int idGiocatore, Giocata play) throws InterruptedException {
        while (!isHisTurn(idGiocatore) && giocato[idGiocatore] == true)
            wait();
        tavolo.add(play);
        System.out.println(Thread.currentThread().getName() + " ha giocato il numero: " + play.getValue());
        giocato[idGiocatore] = true;
        nGiocate++;
        giocatore.remove(0);
        notifyAll();
    }

    public synchronized int getNearest(int value) throws InterruptedException {
        while (nGiocate != nGiocatori)
            wait();
        int mindist = 100-1;
        //nel compito scritto ho lavorato direttamente sugli indici (infatti, la lista qui sottostante, nell'esame su carta è dichiarata
        // come una lista generica), ma poiché l'ordine con cui i giocatori mettono il loro valore
        // sul tavolo cambia ad ogni turno, ho dovuto lavorare con le Giocate, prendendo l'indice da esse stesse tramite
        // il metodo getIdGiocatore appartente alla classe Giocata.
        ArrayList<Giocata> nearest = new ArrayList<Giocata>();
        int nNearest = 0;
        for (int i = 0; i < nGiocate; i++) {
            //per un corretto funzionamento della funzione ho invertito i due if
            if (Math.abs(tavolo.get(i).getValue() - value) == mindist) {
                nearest.add(tavolo.get(i)); //naturalmente alla lista nearest non aggiungo più l'indice, ma direttamente la giocata
                nNearest++;
            }
            if (Math.abs(tavolo.get(i).getValue() - value) < mindist) {
                //System.out.println(" Il valore più vicino per ora è: " + tavolo.get(i).getValue()); //stampa aggiuntiva utile in fase di debugging per seguire il processo di ricerca del valore più vicino
                nearest.clear();
                nNearest = 0;
                nearest.add(tavolo.get(i)); //anche qui aggiungo la giocata e non più l'indice
                mindist = Math.abs(tavolo.get(i).getValue() - value);
            }
        }
        nNearest++; //statement spostato al di fuori del corpo del for per un corretto funzionamento della funzione
        tavolo.clear(); //aggiunta necessaria che mi ero scordato -> il tavolo viene svuotato ad ogni turno

        //ritorno comunque gli indici (come nel compito svolto su carta), ma mettendo nella lista le giocate; prendo
        // gli indici con i metodi get
        if (nNearest == 1)
            return nearest.get(0).getIdGiocatore();
        return nearest.get((int) (Math.random() * nearest.size())).getIdGiocatore();
    }

    public synchronized Giocata[] getGiocate(int idGiocatore) throws InterruptedException {
        if (nGiocate == 0)
            while (!ready)
                wait();
        while (!isHisTurn(idGiocatore))
            wait();
        //System.out.println(Thread.currentThread().getName() + " è il suo turno"); //stampa aggiuntiva utile in fase di debugging
        Giocata[] play = new Giocata[tavolo.size()];
        for (int i = 0; i < play.length; i++) //tavolo.size() == play.length
            play[i] = tavolo.get(i);
        return play;
    }

    //definisco il metodo ishisturn come un metodo sincronizzato, poiché bisogna che venga implementata una wait() al suo interno
    private synchronized boolean isHisTurn(int idGiocatore) throws InterruptedException {
        //aggiungo questa condizione di attesa per evitare che il metodo get(), chiamato nella riga 151 (giocatore.get(0)), generi una IndexOutOfBoundsException
        while (giocatore.size() == 0)
            wait();
        if (giocatore.get(0) == idGiocatore) {
            return true;
        }
        return false;
    }

    public synchronized void setReady(int idVincitore) throws InterruptedException {
        while (nGiocate == nGiocatori) //correggo la guardia del while con l'operatore == per un corretto funzionamento del programma
            wait();
        //a differenza di come scritto sul compito svolto su carta, invece di riniziallizare l'attributo nGiocate del tavolo
        // in questa funzione, decido di chiamare una metodo apposito (sempre appartenente alla classe Tavolo) nella run() del thread Banco
        //giocatore.clear(); //se effettuato o non effettuato non cambia niente, visto che la lista giocatore viene svuotata nel metodo play()

        //utilizzo un while invece di un for
        int k = idVincitore, j = 0;
        while (j != nGiocatori) {
            giocatore.add(k);
            k = (k+1) % nGiocatori;
            j++;
            //System.out.println("Aggiunto il giocatore: " + k); //stampa aggiuntiva utile in fase di debugging
        }

        //stampa aggiuntiva che va a mostrare ad ogni turno il nuovo ordine dei giocatori
        System.out.print("In questo turno, l'ordine dei giocatori è: [");
        for (int i = 0; i < giocatore.size(); i++) {
            System.out.print(" " + giocatore.get(i));
        }
        System.out.println(" ]");


        for (int i = 0; i < nGiocatori; i++)
            giocato[i] = false;

        ready = true;
        notifyAll();
    }

    //come precedentemente commentato, ho aggiunto un metodo per rinizializzare l'attributo nGiocate
    public void resetNGiocate() {
        nGiocate = 0;
    }
}

class Giocatore extends Thread {
    private int value = 0;
    private int id;
    private Giocata[] giocata;
    //rimosso l'attributo booleano che veniva usato nella guardia del while all'interno della run
    // per l'interruzione dei thread Giocatore viene usato invece il metodo interrupt(), chiamato dal thread Banco
    private Tavolo t;
    private int nVictory = 0;

    public Giocatore(Tavolo t, int id) {
        this.t = t;
        this.id = id;
    }

    public void run() {
        try {
            while (true) { //come già precedentemente commentato, non viene usato più un booleano per l'interruzione del thread
                value = (int) (Math.random() * 100);
                giocata = t.getGiocate(id);
                t.play(id, new Giocata(value, id));
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    //con la rimozione dell'attributo booleano isGameAlive, viene conseguentemente rimosso anche il metodo gameFinish
    // che andava a cambiare il valore del booleano

    public void setNVictory() {
        nVictory++;
    }

    public int getNVictory() { //errore di distrazione effettuato nel compito su carta -> il metodo deve ritornare un intero
        return nVictory;
    }
}

class Banco extends Thread {
    private int value = 0;
    private int idVincitore = 0;
    private Tavolo t;
    private Giocatore[] g;

    public Banco(Tavolo t, Giocatore[] g) {
        this.t = t;
        this.g = g;
    }

    public void run() {
        try {
            for (int i = 0; i < 10; i++) { // 10 == turni di gioco
                value = (int) (Math.random() * 100);
                t.setReady(idVincitore);
                idVincitore = t.getNearest(value);
                System.out.println("Il numero giocato dal banco è: " + value); //stampa aggiuntiva che rivela il valore segreto
                                                                               // del banco, una volte che tutti i giocatori hanno giocato
                System.out.println("Ha vinto il giocatore G" + idVincitore + ". Finisce il turno " + (i+1));
                t.resetNGiocate();  //come precedentemente commentato, viene aggiunta la chiamata a questo metodo per la rinizializzazione di un attributo di t
                g[idVincitore].setNVictory();
                //sleep(100);
            }
            //come precedentemente commentato, i thread Giocatore non vengono più interrotti dal cambiamento di un booleano
            // all'interno della guardia del loro while nella loro run(), ma vengono interrotti dal thread Banco una volta
            // che sono finiti i turni di gioco
            for (int i = 0; i < g.length; i++) {
                g[i].interrupt();
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}