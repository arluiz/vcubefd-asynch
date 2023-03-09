package br.unioeste.sisdis.fd.vcube;

import br.unioeste.sisdis.fd.Parameters;
import java.util.logging.Level;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import org.apache.java.util.Configurations;

public class VCubeFDAsynch
        extends VCubeFD {

    public VCubeFDAsynch(NekoProcess process, String name) {
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
            /*get information about all other nodes*/
            VCubeData data = (VCubeData) m.getContent();
            if (data.getStatus()[me] == FAULTY) {
                logger.log(Level.FINE, "p{0} suspect me: die!", m.getSource());
                crash();
                return;
            }
            if (states[m.getSource()] != FAULTY) {
                this.replyQueue[m.getSource()].put(m);
            }
        } else {
            super.deliver(m);
        }
    }

}
