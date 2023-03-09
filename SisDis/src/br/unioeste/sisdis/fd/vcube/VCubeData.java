package br.unioeste.sisdis.fd.vcube;

import br.unioeste.sisdis.fd.AbstractFailureDetector;
import java.util.Arrays;

public class VCubeData {
	private boolean[] status;
	private int[] ts;
	
		
	public VCubeData(boolean[] status, int[] ts) {
		this.status = status;
		this.ts = ts;
	}

	public boolean[] getStatus() {
		return status;
	}
	
	public void setStatus(boolean[] status) {
		this.status = status;
	}
	
	public int[] getTs() {
		return ts;
	}
	
	public void setTs(int[] ts) {
		this.ts = ts;
	}
	
	@Override
	public String toString() {
            String suspects = "";
            for (int i=0; i<status.length; i++)
                if (status[i] == AbstractFailureDetector.FAULTY)
                    suspects = suspects.concat(i+" ");
		//return "VCUBE";//Data [st=" + Arrays.toString(status).replace("true", "1").replace("false", "0") + ", ts="
            return suspects;			//+ Arrays.toString(ts).replace("true", "1").replace("false", "0") + "]";
	}	

}
