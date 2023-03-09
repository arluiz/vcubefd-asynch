package br.unioeste.sisdis.brcast;


import java.util.logging.Logger;
import lse.neko.ActiveReceiver;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;


/**
 *
 * @author larodrigues
 */
public class BestEffortBroadcast extends ActiveReceiver {
    
    public static final int BRCAST = 1111;
    public static final int APP = 1112;
    
    static {
        MessageTypes.instance().register(BRCAST, "BRCAST");
        MessageTypes.instance().register(APP, "APP");
    }
    
    public BestEffortBroadcast(NekoProcess process, String name) {
        super(process, name);
    }
    
    SenderInterface sender;
    
    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }
    
    public void broadcast(NekoMessage m) {
        //enviar para todos, incluindo o sender
        for (int p=0; p<process.getN(); p++) {
            sender.send(new NekoMessage(new int[]{p}, getId(), m, BRCAST));
        }
    }
    
    @Override
    public void run() {
        if (process.getID() == 0) {
            for (int i=0; i<1; i++)
                broadcast(new NekoMessage(new int[]{-1}, getId(), i, APP));
        }
    }
    
    public void deliver(NekoMessage m) {
        logger.info("p"+process.getID()+ " deliver "+m.getContent());
    }
    
    private static final Logger logger
            = NekoLogger.getLogger("best-effort");
    
}
