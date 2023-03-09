package br.unioeste.sisdis.crash;

import java.util.logging.Level;
import java.util.logging.Logger;

import lse.neko.ActiveReceiver;
import lse.neko.NekoMessage;
import lse.neko.NekoMessageEvent;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;

import org.apache.java.util.Configurations;

public class CrashProtocol
        extends ActiveReceiver
        implements SenderInterface, CrashInterface {

    public static final String LOG_DROP = "d";
    public static final String LOG_CRASH = "crash";
    public static final String LOG_RECOVER = "recover";

    private boolean crashed;

    
    protected static final boolean DEBUG = false;
    protected static Logger logger
            = NekoLogger.getLogger("messages");

    public CrashProtocol(NekoProcess process, String name) {
        super(process, "crash-" + name);
        Configurations config = NekoSystem.instance().getConfig();
        if (config != null) {
            int id = process.getID();

            String cs = config.getString("process." + id + ".crash.start", null);
            if (cs == null) {
                cs = config.getString("process.crash.start", null);
            }

            if (cs != null) {
                String tcs[] = cs.split(",");
                //System.out.println("Crash start for process p"+id+"-"+this.getName()+": "+Arrays.toString(tcs));
                for (int i = 0; i < tcs.length; i++) {
                    try {
                        tcs[i] = tcs[i].trim();
                        NekoSystem.instance().getTimer().schedule(new CrashTask(this), Double.parseDouble(tcs[i]));
                        //System.out.println("p"+process.getID()+" crash scheduled at "+tcs[i]);
                    } catch (Exception e) {
                        throw new RuntimeException("Error em crash.start parameter=\"" + tcs[i] + "\" "
                                + "for process #" + id + ": " + e);
                    }
                }

                String cf = config.getString("process." + id + ".crash.stop", null);
                if (cf == null) {
                    cf = config.getString("process.crash.stop", null);
                }
                if (cf != null) { //there's crash.stop - halting crash						
                    String tcf[] = cf.split(",");
                    //System.out.println("Crash stop for process p"+id+"-"+this.getName()+": "+Arrays.toString(tcf));
                    for (int i = 0; i < tcf.length; i++) {
                        try {
                            NekoSystem.instance().getTimer().schedule(new RecoverTask(this), Double.parseDouble(tcf[i]));
                        } catch (Exception e) {
                            throw new RuntimeException("Error em crash.stop parameter "
                                    + "for process #" + id + ": " + e);
                        }
                    }
                }
            }

            crashed = false;
            //turns off the log of NekoProcess. 
            //Just CrashLayer will log the incoming and outgoing messages
            //process.setLogIncomingMessages(false);
            //process.setLogOutgoingMessages(false);
            //messageLogger.setLevel(Level.FINE);
        }
    }

    protected SenderInterface sender;

    public void setSender(SenderInterface s) {
        this.sender = s;
    }

    @Override
    public void send(NekoMessage m) {
        if (!crashed) {
            sender.send(m);
        } else {// is crashed
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "",
                        new Object[]{
                            new NekoMessageEvent(LOG_DROP, m)
                        });
            }
        }
    }

    
    @Override
    public void deliver(NekoMessage m) {
        if (!crashed) {
            super.deliver(m);
        }
    }
    
    
    @Override
    public synchronized void crash() {
        if (!crashed) {
            crashed = true;
            logger.fine("crash started at " + this.getName());
        } else {
            logger.fine("WARNING: process already crashed!");
        }
    }

    @Override
    public synchronized void recover() {
        if (crashed) {
            crashed = false;
            logger.fine("crash stoped at " + this.getName());
        } else {
            logger.fine("WARNING:process was not crashed!");
        }
    }

    @Override
    public boolean isCrashed() {
        if (crashed) {
            return true;
        } else {
            return false;
        }
    }

    /*
	 * Class to execute a schedule to Halt time
     */
    class CrashTask extends TimerTask {

        CrashProtocol t;

        public CrashTask(CrashProtocol t) {
            this.t = t;
        }

        @Override
        public void run() {
            //logger.fine(t.getProcess().clock() + " p" + t.getProcess().getID() + " crash started!!!!!");
            t.crash();
        }
    }

    class RecoverTask extends TimerTask {

        CrashProtocol t;

        public RecoverTask(CrashProtocol t) {
            this.t = t;
        }

        @Override
        public void run() {
            t.recover();
        }

    }

}
