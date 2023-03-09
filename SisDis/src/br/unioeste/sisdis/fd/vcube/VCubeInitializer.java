package br.unioeste.sisdis.fd.vcube;
// lse.neko imports:
import br.unioeste.sisdis.fd.AbstractFailureDetector;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;

import org.apache.java.util.Configurations;



/**
 * This class initalizes the protocol stack of a process
 * for the basic example.
 * Its <code>init</code> method adds two layers on top of the
 * NekoProcess:
 * <ul><li>(top layer) An algorithm. The implementing
 *   class is specified by the <code>algorithm</code>
 *   config option.</li>
 * <li>(bottom layer) A layer that transforms outgoing multicast
 *   messages into several unicast messages (one per destination).</li>
 * </ul>
 */
public class VCubeInitializer
    implements NekoProcessInitializer
{

    public VCubeInitializer() {
    }

    @Override
    public void init(NekoProcess process, Configurations config)
        throws Exception
    {    	
    	 AbstractFailureDetector fd = new VCubeFDAsynch(process, "vcube-asynch");
         fd.setId("vcube-asynch");        

         SenderInterface net = process.getDefaultNetwork();
         
         fd.setSender(net);
 		
         fd.launch();
    }     

}
