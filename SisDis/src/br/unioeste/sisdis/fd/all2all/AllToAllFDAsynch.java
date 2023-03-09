package br.unioeste.sisdis.fd.all2all;

import br.unioeste.sisdis.fd.Parameters;
import java.util.logging.Level;

import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.NekoThread;
import lse.neko.util.TimerTask;
import org.apache.java.util.Configurations;

public class AllToAllFDAsynch
        extends AllToAllFD {

    public AllToAllFDAsynch(NekoProcess process, String name) {
        super(process, name);
        Configurations config = NekoSystem.instance().getConfig();

        if (config != null) {
            for (int p = 0; p < n; p++) {
                if (p != me) {
                    String falseSuspectProcessId = config.getString("process." + me + ".false_suspect.process." + p, null);

                    if (falseSuspectProcessId != null) {
                        if (DEBUG) {
                            System.out.println("Process #" + me + " false suspects process IDs: " + p);
                        }

                        String tcs[] = falseSuspectProcessId.split(",");

                        for (int i = 0; i < tcs.length; i++) {
                            try {
                                tcs[i] = tcs[i].trim();
                                NekoSystem.instance().getTimer().schedule(new FaultSuspicionTask(this, p), Double.parseDouble(tcs[i]));
                                if (DEBUG) {
                                    System.out.println("p" + me + " false suspicion scheduled for " + p + " at " + tcs[i]);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Error em false_suspicon.start parameter=\"" + tcs[i] + "\" "
                                        + "for process #" + me + ": " + e);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    protected void checkResponse(int j) {
            NekoMessage m = replyQueue[j].get(DEFAULT_TIMEOUT);
            if (isCrashed() || states[j] == FAULTY) {
                return;
            }
            if (m == null) {
                if (states[j] == FAULT_FREE) {
                    suspect(j);
                }
            } 
        }

    @Override
    public void deliver(NekoMessage m) {
        if (isCrashed()) {
            return;
        }
        if (m.getType() == ARE_YOU_ALIVE) {
            //System.out.println(NekoSystem.instance().clock()+ " "+ this.getId()+" recebeu "+m);
            NekoMessage m1 = new NekoMessage(me, new int[]{m.getSource()}, getId(), states[m.getSource()], I_AM_ALIVE);
            NekoSystem.instance().getTimer().schedule(new SendTask(m1), Parameters.TS);
        } else if (m.getType() == I_AM_ALIVE) {
            //if (states[m.getSource()] == FAULT_FREE) {
            boolean myStatus = (boolean) m.getContent();
            if (myStatus == FAULTY) {
                logger.log(Level.FINE, "p{0} suspect me: die!", m.getSource());
                crash();
                return;
            }
            //}   
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
