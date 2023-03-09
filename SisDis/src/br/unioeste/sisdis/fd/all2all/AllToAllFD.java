package br.unioeste.sisdis.fd.all2all;

import br.unioeste.sisdis.fd.AbstractFailureDetector;
import br.unioeste.sisdis.fd.Parameters;
import java.util.Arrays;

import lse.neko.NekoMessage;
import lse.neko.NekoMessageQueue;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.util.TimerTask;


public class AllToAllFD 
extends AbstractFailureDetector {
	protected double simulation_time;

	protected NekoMessageQueue[] replyQueue;

	public AllToAllFD(NekoProcess process, String name) {
            super(process, name);
            simulation_time = NekoSystem.instance().getConfig().getDouble("simulation.time");
            replyQueue = new NekoMessageQueue[n];
            for (int i = 0; i < replyQueue.length; i++) {
                replyQueue[i] = new NekoMessageQueue();
            }
	}
        
        protected int log2(int N) {
            return (int) (Math.log10(N) / Math.log10(2)); //hypercube dimension
        }

        @Override
	public void run() {
            int rounds = 0;        
            //while (process.clock() <= simulation_time) {
            while (rounds < Math.pow(log2(process.getN()),2)) {
            //while (process.clock() <= simulation_time) {
		if (isCrashed())
			return;

		// the number of processes
		int n = process.getN();
		// the id of this process

		int[] allButMe = new int[n - 1];
		for (int i = 0; i < n - 1; i++) {
			allButMe[i] = (i < me) ? i : i + 1;
		}
		int delay=1;
                for (int p: allButMe) {
                    if (states[p] == FAULT_FREE) {
                        NekoMessage m1 = new NekoMessage(me, new int[]{p}, getId(), rounds, ARE_YOU_ALIVE);
                        NekoSystem.instance().getTimer().schedule(new SendTask(m1), delay*Parameters.TS); delay++;
                    }
                }					   

		boolean[] alive = new boolean[process.getN()];
                Arrays.fill(alive, FAULTY);
                alive[me] = FAULT_FREE;
                
                
                for (int j : allButMe) {
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
                }
            } else {
                int from = m.getSource();
                if (states[from] == FAULTY) {
                    unsuspect(from);           
                }
            }
        }

	@Override
	public void deliver(NekoMessage m) {		
		if (isCrashed())
			return;
		if (m.getType()==ARE_YOU_ALIVE) {
			//System.out.println(NekoSystem.instance().clock()+ " "+ this.getId()+" recebeu "+m);
			NekoMessage m1 = new NekoMessage(me, new int[]{m.getSource()}, getId(), states[m.getSource()], I_AM_ALIVE);
                        NekoSystem.instance().getTimer().schedule(new SendTask(m1), Parameters.TS);
		} else if (m.getType()==I_AM_ALIVE) {
			if (states[m.getSource()] == FAULTY) {
                            unsuspect(m.getSource());//System.out.println(NekoSystem.instance().clock()+ " "+ this.getId()+ " recebeu "+m);
                        }   
                        this.replyQueue[m.getSource()].put(m);
                        
		}
	}

        /*
	 * Class to execute a schedule to Halt time
	 */
	private class MyTask extends TimerTask {
		NekoThread t;

		public MyTask(NekoThread t) {
			this.t = t;
		}

		@Override
		public void run() {			
			t.run();			
		}

	}


}
