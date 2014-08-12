package ngat.rcs.test;

import ngat.message.GUI_RCS.*;

public class SendPhotomUpdate {

    public static void main(String args[]) {

	try {

	    int ext = 1;
	    if (args[0].equalsIgnoreCase("photom"))
		ext = SET_EXTINCTION.PHOTOMETRIC;
	    else
		ext = SET_EXTINCTION.SPECTROSCOPIC;
		
	    SendCommand command = new SendCommand("ltsim1", 9110);

	    SET_EXTINCTION photomUpdate = new SET_EXTINCTION("test");
	    photomUpdate.setExtinction(ext);

	    System.err.println("Set extinction: "+args[0]+" "+ext);

	    command.sendCommand(photomUpdate);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}