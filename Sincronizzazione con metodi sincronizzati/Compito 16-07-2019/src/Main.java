/*

Si vuole realizzare il seguente sistema:

- sono presenti N thread T1..TN e M contenitori C1..CM
- ogni contenitore contiene dei token e ogni thread Ti iterativamente preleva un token da un sottoinsieme Ai
  dei contenitori con |Ai| = ki (preleva solo se c’è almeno un token in ogni contenitore appartentente ad Ai)
  e inserisce un token in un sottoinsieme dei contenitori Bi tale che |Bi|= li e aspettando un tempo causale
  compreso in [mini, maxi) tra l’aquisizione dei token e l’invio dei token.
- ogni volta che lo stato dei contenitori cambia stampare il numero di token in ogni contenitore

Realizzare in java le classi generiche per permettano di rappresentare un qualsiasi sistema del tipo indicato,
usando i metodi sincronizzati per la sincronizzazione tra thread.

Il programma principale deve usare le classi definite per realizzare il sistema indicato nella seguente figura:

****

Dove i contenitori C1 e C2 contengono inizialmente un token e T1, T2, T3 e T4 hanno rispettivamente tempo
di risposta [0, 200), [100,200), [500,600) e [200, 300). Inoltre dopo 30 secondi terminare tutti i thread.
Per l’implementazione da portare all’orale fare in modo di caricare da file di testo la struttura della rete.

*/

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TokensManager tm = new TokensManager(new int[] {1,1,0,0}); //con {0,0,0,0} il sistema non parte, parte con {0,0,1,0} e {0,0,0,1}
        Contenitore[] t = new Contenitore[4];
        t[0] = new Contenitore(tm, new int[] {0,1}, new int[] {2}, 0, 200);
        t[1] = new Contenitore(tm, new int[] {0,1}, new int[] {2}, 100, 200);
        t[2] = new Contenitore(tm, new int[] {2}, new int[] {0,3}, 500, 600);
        t[3] = new Contenitore(tm, new int[] {3}, new int[] {1}, 200, 300);

        for (int i = 0; i < t.length; i++) {
            t[i].setName("T" + (i+1));
            t[i].start();
        }

        Thread.sleep(30000);

        for (int i = 0; i < t.length; i++) {
            t[i].interrupt();
        }
    }
}

class TokensManager {
    private int[] tokens;

    public TokensManager (int[] tokens) {
        this.tokens = tokens;
    }

    public synchronized void getTokens (int[] p) throws InterruptedException {
        while (!checkTokens(p))
            wait();
        for (int i = 0; i < p.length; i++) {
            tokens[p[i]]--; //aggiorno lo stato
        }
        print();
    }

    private boolean checkTokens (int[] p) { //controlla se c'è almeno un token in ognuna di queste posizioni
        for (int i = 0; i < p.length; i++) {
            if (tokens[p[i]] == 0)
                return false;
        }
        return true;
    }

    public synchronized void setTokens (int p[]) {
        for (int i = 0; i < p.length; i++) {
            tokens[p[i]]++;
        }
        print();
        notifyAll();
    }

    public void print() {
        System.out.print(Thread.currentThread().getName() + " ");
        for (int i = 0; i < tokens.length; i++) {
            System.out.print(tokens[i] + " ");
        }
        System.out.println();
    }
}

class Contenitore extends Thread {
    private TokensManager tm;
    private int[] tokensIn, tokensOut;
    private int dmin, dmax; //delay minimo e delay massimo

    public Contenitore (TokensManager tm, int[] tokensIn, int[] tokensOut, int dmin, int dmax) {
        this.tm = tm;
        this.tokensIn = tokensIn;
        this.tokensOut = tokensOut;
        this.dmin = dmin;
        this.dmax = dmax;
    }

    public void run() {
        try {
            while (true) {
                tm.getTokens(tokensIn);
                sleep(dmin + (int) Math.random() * (dmax-dmin));
                tm.setTokens(tokensOut);
            }
        } catch (InterruptedException e) {

        }
    }
}