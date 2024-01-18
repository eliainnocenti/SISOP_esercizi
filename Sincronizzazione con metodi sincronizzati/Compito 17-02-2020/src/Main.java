import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 10;

        Buffer b = new Buffer(N);

        Tavolo t = new Tavolo();

        Giocatore[] g = new Giocatore[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Giocatore(b,i);
            g[i].setName("G" + i);
            g[i].start();
        }

        Banco bb = new Banco(t,b,g);
        bb.setName("Banco");
        bb.start();

        Thread.sleep(30000);

        bb.interrupt();

        for (Giocatore gg:g) {
            gg.interrupt();
        }

        for (Giocatore gg:g) {
            System.out.println("Il giocatore " + gg.getName() + " ha vinto " + gg.nvincite + " volte e ha terminato " + gg.nparità + " volte in parità");
        }
    }
}

class Giocatore extends Thread {
    private Buffer b;
    private int id;
    public int nvincite;
    public int nparità;
    public Giocatore(Buffer b, int id) {
        this.b = b;
        this.id = id;
        nvincite = 0;
        nparità = 0;
    }
    public void run() {
        try {
            while (true) {
                int value = (int) (Math.random() * 99);
                b.putValue(value,id);
                    //System.out.println(getName() + " ha messo nel buffer il valore: " + value);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}
class Banco extends Thread {
    private Tavolo t;
    private Buffer b;
    private Giocatore[] giocatori;
    private int value = -1;
    public Banco(Tavolo t, Buffer b, Giocatore[] giocatori) {
        this.t = t;
        this.b = b;
        this.giocatori = giocatori;
    }
    public void run() {
        try {
            while (true) {
                value = (int) (Math.random() * 99);
                t.putValue(value);
                    System.out.println("Il valore sul tavolo è: " + value);
                Integer[] nearest = b.getNearest(value);
                if (nearest.length == 1) {
                    System.out.println("Il giocatore più vicino è il giocatore G" + nearest[0] + ". G" + nearest[0] + " ha vinto!");
                    giocatori[nearest[0]].nvincite++;
                        //System.out.println("G" + nearest[0] + " ha vinto " + giocatori[nearest[0]].nvincite + " volte");
                }
                else {
                    System.out.print("Parità: nessuno a vinto, i più vicini sono stati i giocatori");
                    for(int i = 0; i < nearest.length; i++)
                        System.out.print(" G" + nearest[i]);
                    System.out.println();
                    for (int i = 0; i < nearest.length; i++)
                        giocatori[nearest[i]].nparità++;
                }
                sleep(100);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}
class Tavolo {
    private int value;
    public int getValue() {
        return value;
    }
    public void putValue(int value) {
        this.value = value;
    }
}
class Buffer {
    private Integer[] data;
    private int ngiocatori;
    public Buffer(int ngiocatori) {
        data = new Integer[ngiocatori];
        this.ngiocatori = ngiocatori;
    }
    public synchronized Integer[] getNearest(int value) throws InterruptedException {
        while (isthereanEmptyPosition())
            wait();
        int distmin = 99-1;
        int idnearest;
        int Nnearest = 0;
        ArrayList<Integer> nearest = new ArrayList<Integer>();
        Integer[] r = new Integer[1];
        for (int i = 0; i < data.length; i++) {
            if (Math.abs(data[i] - value) < distmin) {
                nearest.clear();
                Nnearest = 0;
                distmin = Math.abs(data[i] - value);
                idnearest = i;
                nearest.add(idnearest);
                continue;
            }
            if (Math.abs(data[i] - value) == distmin) {
                Nnearest++;
                nearest.add(i);
            }
        }

                                System.out.print("I valori giocati dai giocatori sono: ");
                                for (int i = 0; i < data.length; i++)
                                    System.out.print(" " + data[i]);
                                System.out.println();

                                data = new Integer[ngiocatori];

                                /*
                                for (int i = 0; i < data.length; i++)
                                    System.out.print(" " + data[i]);
                                System.out.println();
                                 */

        Nnearest += 1;
        if (Nnearest > 1) {
            r = new Integer[Nnearest];
            for (int i = 0; i < Nnearest; i++)
                r[i] = nearest.remove(0);
            notifyAll();
            return r;
        }
        r[0] = nearest.remove(0);
        notifyAll();
        return r;
    }
    public synchronized void putValue(int v, int idgiocatore) throws InterruptedException {
        while (data[idgiocatore] != null)
            wait();
        data[idgiocatore] = v;
        notifyAll();
    }
    private boolean isthereanEmptyPosition() {
        int nVuote = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null)
                nVuote++;
        }
        if (nVuote != 0)
            return true;
        return false;
    }
}