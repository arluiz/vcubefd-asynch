package br.unioeste.sisdis.fd;

import br.unioeste.sisdis.crash.CrashProtocol;
import br.unioeste.sisdis.fd.vcube.VCubeFDAsynch;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.SenderInterface;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;

public abstract class AbstractFailureDetector
        extends CrashProtocol {

    protected int n; //number of processes
    protected int me; //process id
    protected boolean states[]; //state of other nodes
    protected int numSuspected; //number of suspects

    private List<FailureDetectorListener> listeners;

//	public static final int UNKNOWN = -1;
    public static final boolean FAULT_FREE = false;
    public static final boolean FAULTY = true;

    protected static final double DEFAULT_TIMEOUT = 4 * (Parameters.TS + Parameters.TR + Parameters.TT);
    protected static final double DEFAULT_INTERVAL = 30.0;

    // message types used by this algorithm
    protected static final int ARE_YOU_ALIVE = 1225;
    protected static final int I_AM_ALIVE = 1226;

    // registering the message types and associating names with the types.
    static {
        MessageTypes.instance().register(ARE_YOU_ALIVE, "ARE_YOU_ALIVE");
        MessageTypes.instance().register(I_AM_ALIVE, "I_AM_ALIVE");
    }

    public AbstractFailureDetector(NekoProcess process, String name) {
        super(process, name);
        n = process.getN();
        me = process.getID();
        states = new boolean[process.getN()];
        Arrays.fill(states, FAULT_FREE);// state[me] = FAULT_FREE;
        numSuspected = 0;
        listeners = new LinkedList<FailureDetectorListener>();
    }

    protected SenderInterface sender;

    public final void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    /**
     * Suspects process p.
     *
     * @param id ID of the suspected process
     */
    public synchronized final void suspect(int p) {
        if (states[p] != FAULTY) {
            states[p] = FAULTY;
            numSuspected++;
            logger.fine("suspect " + p);
            for (FailureDetectorListener l : listeners) {
                l.statusChange(FAULTY, p);
            }

        }
    }

    /**
     * Unsuspect process p.
     *
     * @param id ID of the suspected process
     */
    public synchronized final void unsuspect(int p) {
        if (states[p] != FAULT_FREE) {
            states[p] = FAULT_FREE;
            numSuspected--;
            logger.fine("unsuspect " + p);
            for (FailureDetectorListener l : listeners) {
                l.statusChange(FAULT_FREE, p);
            }
        }
    }

    public final boolean getState(int p) {
        return states[p];
    }

    public final void setState(int p, boolean s) {
        if (s == FAULTY) {
            suspect(p);
        } else {
            unsuspect(p);
        }
    }

    public final boolean[] getStates() {
        return states;
    }

    public final int getNumberOfSuspects() {
        return numSuspected;
    }

    public void addListener(FailureDetectorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FailureDetectorListener listener) {
        listeners.remove(listener);
    }

    protected static final Logger logger
            = NekoLogger.getLogger("messages");
    
    
    /*
	 * Class to execute a schedule to send a message
	 */
	protected class SendTask extends TimerTask {
		NekoMessage m;

		public SendTask(NekoMessage m) {
			this.m = m;			
		}

		@Override
		public void run() {
                    if (!isCrashed())
		       sender.send(m);
		}

	}
        
    public synchronized void falseSuspicion(int j) {
       if (!isCrashed()) {
            logger.log(Level.FINE, "falsely suspect p{0}", j);
            suspect(j);
       }
    } 
    
     
    /*
    * Class to execute a schedule to false suspicion
     */
    protected class FaultSuspicionTask extends TimerTask {

        AbstractFailureDetector t;
        int j;

        public FaultSuspicionTask(AbstractFailureDetector t, int j) {
            this.t = t;
            this.j = j;
        }

        @Override
        public void run() {
            //logger.fine(t.getProcess().clock() + " p" + t.getProcess().getID() + " started!!!!!");
            t.falseSuspicion(j);
        }
    }

}


