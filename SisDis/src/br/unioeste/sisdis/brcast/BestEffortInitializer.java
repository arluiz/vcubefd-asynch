package br.unioeste.sisdis.brcast;


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
public class BestEffortInitializer implements 
        NekoProcessInitializer {

    @Override
    public void init(NekoProcess process, Configurations config) 
            throws Exception {
        SenderInterface network = process.getDefaultNetwork();
        BestEffortBroadcast be = new BestEffortBroadcast(process, "best-effort");
        be.setId("best-effort");
        be.setSender(network);
        be.launch();        
    }    
    
    
}
