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
public class EagerInitializer implements 
        NekoProcessInitializer {

    @Override
    public void init(NekoProcess process, Configurations config) 
            throws Exception {
        SenderInterface network = process.getDefaultNetwork();
        EagerReliableBroadcast be = new EagerReliableBroadcast(process, "eager");
        be.setId("eager");
        be.setSender(network);
        be.launch();        
    }    
    
    
}
