package br.unioeste.sisdis.brcast;


import br.unioeste.sisdis.fd.vcube.VCubeFD;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author larodrigues
 */
public class LazyInitializer implements 
        NekoProcessInitializer {

    @Override
    public void init(NekoProcess process, Configurations config) 
            throws Exception {
        SenderInterface network = process.getDefaultNetwork();
        VCubeFD fd = new VCubeFD(process, "vcube");
        fd.setId("vcube");
        fd.setSender(network);        
        
        LazyReliableBroadcast be = new LazyReliableBroadcast(process, "lazy");
        be.setId("lazy");
        be.setSender(network);        

        fd.addListener(be);
        fd.launch();
        be.launch();
    }    
    
    
}
