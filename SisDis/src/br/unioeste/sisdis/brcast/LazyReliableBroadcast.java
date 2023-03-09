package br.unioeste.sisdis.brcast;


import br.unioeste.sisdis.crash.CrashProtocol;
import java.util.ArrayList;
import java.util.logging.Logger;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.logging.NekoLogger;

/**
 *
 * @author larodrigues
 */
public class LazyReliableBroadcast extends CrashProtocol implements FailureDetectorListener{
    
    public LazyReliableBroadcast(NekoProcess process, String name) {
        super(process, name);
        beb = new BestEffortBroadcast(process, name);
        beb.setSender(process.getDefaultNetwork());
        beb.setId(name);
        
        from = new ArrayList[process.getN()];
        for (int i=0; i<process.getN(); i++) {
            from[i] = new ArrayList<>();
        }
    }
    
    BestEffortBroadcast beb;
    
    ArrayList<NekoMessage> from[];
    ArrayList<Integer> suspected  = new ArrayList<>();
    ArrayList<NekoMessage> delivered = new ArrayList<>();
    
    @Override
    public void run() {
        if (process.getID() == 0) {
            for (int i=0; i<1; i++)
                broadcast(new NekoMessage(process.getID(), new int[]{-1}, getId(), i, beb.APP));
        }
    }
    
    public void broadcast(NekoMessage m) {
        beb.broadcast(m);
    }
    
    @Override
    public void deliver(NekoMessage m) {
        NekoMessage content = (NekoMessage) m.getContent();
        if (!delivered.contains(content)) {
            delivered.add(content);
            logger.info("p"+process.getID()+ " deliver "+content);
            from[content.getSource()].add(content);
            if (suspected.contains(content.getSource())) {
                beb.broadcast(content);
            }
        }
    }

    @Override
    public void statusChange(boolean status, int p) {
        if (status && !suspected.contains(p)) {
            suspected.add(p);
            for (NekoMessage m : from[p]) {
                beb.broadcast(m);
            }
        }
    }   
    
    private static final Logger logger
            = NekoLogger.getLogger("lazy");
    
    
}
