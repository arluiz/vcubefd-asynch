package br.unioeste.sisdis.fd.vcube;

import br.unioeste.sisdis.fd.AbstractFailureDetector;
import br.unioeste.sisdis.fd.Parameters;
import java.util.LinkedList;
import java.util.List;

import lse.neko.NekoMessage;
import lse.neko.NekoMessageQueue;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;


public class VCubeFD
        extends AbstractFailureDetector {

    protected int dim; //hypercube dimension

    protected NekoMessageQueue[] replyQueue;
    protected int[] ts;

    public VCubeFD(NekoProcess process, String name) {
        super(process, name);
        dim = log2(n);

        ts = new int[process.getN()];
        replyQueue = new NekoMessageQueue[n];
        for (int i = 0; i < replyQueue.length; i++) {
            replyQueue[i] = new NekoMessageQueue();
        }
    }

    protected int log2(int N) {
        return (int) (Math.log10(N) / Math.log10(2)); //hypercube dimension
    }
    
    /*
	 * Funcao C(i,s): calcula o cluster testado pelo nodo i na rodada s
	 * 
     */
    public static void cis(List<Integer> cluster, int i, int s) {
        //printf("i: %d s: %d \n", i, s);
        int xor = i ^ (int) Math.pow(2, s);
        //printf("pow: %d xor: %d \n", (int)pow(2,s), xor);

        cluster.add(xor);

        for (int j = 0; j < s; j++) {
            cis(cluster, xor, j);
        }

    }

    /*
	 * Identify the first fault-free process j in cluster s of process i
     */
    public int neighbor(int i, int s) {
        List<Integer> cluster = new LinkedList<Integer>();
        boolean st;
        //calculo dos nodos no cluster i na rodada s
        cluster.clear();
        cis(cluster, i, s); //add nodes to cluster(i=me, s=hops)
        do {
            //test next node in C(i,s)
            int target = cluster.remove(0);
            st = getState(target);
            if (st != FAULTY) {
                return target;
            }
        } while (st != FAULT_FREE && !cluster.isEmpty());	//ate (testar nodo sem falha) OU (testar todos os nodos do cluster como falhos)
        return -1; //there is no fault-free node in cluster s
    }

    public void run() {
        //System.out.println("d="+d);
        double simulation_time = NekoSystem.instance().getConfig().getDouble("simulation.time");

        //while (NekoSystem.instance().clock() < simulation_time) {
        List<Integer> responses = new LinkedList<>();
        int rounds = 0;        
        //while (process.clock() <= simulation_time) {
        while (rounds < Math.pow(log2(n),2)) {
            if (isCrashed()) {
                return;
            }
            int delay = 1;
            List<Integer> ceis = new LinkedList<>();
            responses.clear();
            for (int s = 0; s < dim; s++) {
                cis(ceis, me, s);
                for (int j : ceis) {
                    if (neighbor(j, s) == me && states[j] == FAULT_FREE) {
                        responses.add(j);
                        NekoMessage m1 = new NekoMessage(me, new int[]{j}, getId(), rounds, ARE_YOU_ALIVE);
                        NekoSystem.instance().getTimer().schedule(new SendTask(m1), delay*Parameters.TS); delay++;
                    }
                }

            }
            for (int j : responses) {
                checkResponse(j);
            }

            //wait until next round
            try {
                sleep(DEFAULT_INTERVAL);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            rounds++;
        }

    }

    protected void checkResponse(int j) {
        NekoMessage m = replyQueue[j].get(DEFAULT_TIMEOUT);
        if (isCrashed()) {
            return;
        }
        if (m == null) {
            if (states[j] == FAULT_FREE) {
                suspect(j);
                ts[j]++;
            }
        } else {
            int from = m.getSource();
            if (states[from] == FAULTY) {
                unsuspect(from);
                ts[from]++;
            }

            /*get information about all other nodes*/
            VCubeData data = (VCubeData) m.getContent();
            
            for (int i = 0; i < n; i++) {
                if (i != me && i != from) {
                    if (ts[i] < data.getTs()[i]) {
                        ts[i] = data.getTs()[i];
                        if (data.getStatus()[i] == FAULTY) {
                            suspect(i);
                        } else {
                            unsuspect(i);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void deliver(NekoMessage m) {
        if (isCrashed()) {
            return;
        }
        if (DEBUG) {
            System.out.println(NekoSystem.instance().clock() + " " + this.getId() + " recebeu " + m);
        }

        if (m.getType() == ARE_YOU_ALIVE) {
            NekoMessage m1 = new NekoMessage(new int[]{m.getSource()}, getId(), new VCubeData(getStates().clone(), ts.clone()), I_AM_ALIVE);
            NekoSystem.instance().getTimer().schedule(new SendTask(m1), Parameters.TS);
        } else if (m.getType() == I_AM_ALIVE) {
            if (states[m.getSource()] == FAULTY) {
                unsuspect(m.getSource());
                int j = m.getSource();
                VCubeData data = (VCubeData) m.getContent();
                
                for (int i = 0; i < n; i++) {
                    if (i != me && i != j) {
                        if (ts[i] < data.getTs()[i]) {
                            ts[i] = data.getTs()[i];
                            if (data.getStatus()[i] == FAULTY) {
                                suspect(i);
                            } else {
                                unsuspect(i);
                            }
                        }
                    }
                }
            } else {
                this.replyQueue[m.getSource()].put(m);
            }
        } else {
            super.deliver(m);
        }
    }

    /*
	 * Compute Position of Must Significant Bit, from 0
     */
    public static int MSB(int i, int j) {
        int s = 0;
        for (int k = i ^ j; k > 0; k = k >> 1) {
            s++;
        }
        return --s;
    }

    class Channel extends NekoThread {

        private NekoMessage m;
        private double delay;

        public Channel(NekoMessage m, double delay) {
            this.m = m;
            this.delay = delay;
        }

        public void run() {
            sender.send(m);
            int j = m.getDestinations()[0];
            
            NekoMessage m = replyQueue[j].get(DEFAULT_TIMEOUT);
            if (isCrashed()) {
                return;
            }
            if (m == null) {
                if (states[j] == FAULT_FREE) {
                    suspect(j);
                    ts[j]++;
                }
            } else {
                int from = m.getSource();
                if (states[from] == FAULTY) {
                    unsuspect(from);
                    ts[from]++;
                }

                /*get information about all other nodes*/
                VCubeData data = (VCubeData) m.getContent();
               
                for (int i = 0; i < n; i++) {
                    if (i != me && i != from) {
                        if (ts[i] < data.getTs()[i]) {
                            ts[i] = data.getTs()[i];
                            if (data.getStatus()[i] == FAULTY) {
                                suspect(i);
                            } else {
                                unsuspect(i);
                            }
                        }
                    }
                }
            }
        }
    }

}
