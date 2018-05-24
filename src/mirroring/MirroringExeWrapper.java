package mirroring;

import nl.utwente.hmi.mwdialogue.function.FunctionClass;

import java.io.IOException;

public class MirroringExeWrapper implements FunctionClass {
    private Process mirrorExe;
    
    public MirroringExeWrapper() {
    	
    }
    
    public void init() {
    	
    }
    
    public void launchMirroringExe(){
        // System.out.println("Button mirroring pressed");
        try {
            mirrorExe = Runtime.getRuntime().exec("../Zeno_Mirroring/Zeno_Mirroring.exe");

        } catch (IOException ex) {
            System.out.println("Mirroring program not found");
        }
    }

    public void killMirroringExe(){

    	if(mirrorExe.isAlive()) {
    		mirrorExe.destroy();
    	}

    }

}