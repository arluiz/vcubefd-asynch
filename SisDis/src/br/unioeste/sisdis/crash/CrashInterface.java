package br.unioeste.sisdis.crash;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;

public interface CrashInterface 
	extends ReceiverInterface, SenderInterface{

	public void crash();
	public void recover();
	public boolean isCrashed();
}
