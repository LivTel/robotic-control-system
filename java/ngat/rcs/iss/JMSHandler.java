package ngat.rcs.iss;

import ngat.net.*;
import ngat.message.base.*;

/** Handle the ongoing request...*/

public class JMSHandler extends JMSMA_ClientImpl {
	
    private volatile boolean done;
    private volatile boolean failed;

    JMSMA_ProtocolServerImpl serverImpl;

    COMMAND_DONE response;
    
    JMSHandler(JMSMA_ProtocolServerImpl serverImpl, COMMAND command){
	super();
	timeout = 600000L;
	this.serverImpl = serverImpl;
	this.command = command;
	done   = false;
	failed = false;
    }
	
    public void failedConnect(Exception e) {	   	  
	System.err.println("ISS_MOVE_FOLD::Failed connect: "+e); 	  
	failed = true;
    }
    
    public void failedDespatch(Exception e) {	   
	System.err.println("ISS_MOVE_FOLD::Failed despatch: "+e);	 
	failed = true;
    }
    
    public void failedResponse(Exception e) {	   
	System.err.println("ISS_MOVE_FOLD::Failed response: "+e); 	 
	failed = true;
    }
	
    public void exceptionOccurred(Object source, Exception e) {	   
	System.err.println("ISS_MOVE_FOLD::Exception: Source: "+source+" Exc: "+e);  
	failed = true;	   
    }

    public void handleAck(ACK ack) {	  
	System.err.println("ISS_MOVE_FOLD::Received Ack: "+ack.getTimeToComplete()+" millis.");
	serverImpl.sendAck(ack);
    }
    
    public void handleDone(COMMAND_DONE response) {	   
	System.err.println("ISS_MOVE_FOLD::Received Response: "+
			   "\n\tClass:   "+response.getClass().getName()+
			   "\n\tSuccess: "+response.getSuccessful()+
			   "\n\tError:   "+response.getErrorNum()+
			   "\n\tString:  "+response.getErrorString());	 
	done = true;
	this.response = response;
    }
	
    public void sendCommand(COMMAND command) {}
    
    public synchronized boolean isDone() { return done; }
    
    public synchronized boolean isFailed() { return failed; }
    
    public COMMAND_DONE  getResponse() {  return response; }

}
        
    
