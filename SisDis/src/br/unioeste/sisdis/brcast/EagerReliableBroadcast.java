package br.unioeste.sisdis.brcast;


import java.util.ArrayList;
import java.util.logging.Logger;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.logging.NekoLogger;


/**
 *
 * @author larodrigues
 */
public class EagerReliableBroadcast extends BestEffortBroadcast {
    
    public EagerReliableBroadcast(NekoProcess process, String name) {
        super(process, name);
    }
    
    ArrayList<NekoMessage> delivered = new ArrayList<>();
    
    private static final Logger logger
            = NekoLogger.getLogger("eager");
    
    @Override
    public void broadcast(NekoMessage m) {
        delivered.add(m);
        logger.info("p"+process.getID()+ " deliver "+m);
        super.broadcast(m);//broadcast do best-effort
    }
    
    @Override
    public void deliver(NekoMessage m) {
        NekoMessage content = (NekoMessage)m.getContent(); //mensagem APP
        if (!delivered.contains(content)) {
            delivered.add(content);
            logger.info("p"+process.getID()+ " deliver "+content);
            super.broadcast(content);
        }
    }
    
}
