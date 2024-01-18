public class Main {
    public static void main (String[] args) throws InterruptedException {
        int N = 20;
        Piano pi = new Piano(N);

        Persona[] p = new Persona[N];
        for(int i = 0; i < p.length; i++) {
            p[i] = new Persona(pi,i);
            p[i].setName("P" + i);
            p[i].start();
        }

        Thread.sleep(30000);

        for(int i = 0; i < p.length; i++) {
            p[i].interrupt();
            p[i].join();
            System.out.println(p[i].getName() + " nchanges: " + p[i].nchanges + " nwait: " +p[i].nwait + " totDisit: " + p[i].totDist);
        }
    }
}

class Position {
    private double x, y;

    public Position (double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Position (Position p) {
        x = p.x;
        y = p.y;
    }

    public void add(Position p) {
        x += p.x;
        y += p.y;
    }

    public double dist(Position p) {
        return Math.sqrt((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y));
    }

    @Override
    public String toString() { return x + ", " + y + " "; }
}
class Piano {
    Position[] pos;

    public Piano (int n) {
        pos = new Position[n];
        for (int i = 0; i < pos.length; i++)
            pos[i] = new Position(0,0);
    }

    public boolean checkPos(int idPersona, Position dp) {
        Position newPos = new Position(pos[idPersona]);
        newPos.add(dp);
        for(int i = 0; i < pos.length; i++) {
            if(i != idPersona && pos[i].dist(newPos) < 1) {
                return true;
            }
        }
        return false;
    }
    public synchronized int updatePos(int idPersona, Position dp) throws InterruptedException {
        int nwait = 0;
        while(checkPos(idPersona,dp)) {
            nwait++;
            wait();
        }
        pos[idPersona].add(dp);
        notifyAll();
        return nwait;
    }


}
class Persona extends Thread {
    Piano pi;
    int id;
    int nchanges = 0;
    int nwait = 0;
    double totDist = 0.0;

    public Persona (Piano pi, int id) {
        this.pi = pi;
        this.id = id;
    }

    public void run() {
        try {
            while (true) {
                Position dxdy = new Position(Math.random()*20-10,Math.random()*20-10);
                System.out.println("P" + id + " sposta a " + dxdy);
                nwait += pi.updatePos(id, dxdy);
                nchanges++;
                totDist += dxdy.dist(new Position(0,0));
                sleep(100);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}