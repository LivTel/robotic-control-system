/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package ngat.rcs.tms;

import ngat.rcs.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;



/** Stores information to allow a Manager TA to recover after failure of a
 * lower level (managed) task. The normal sequence of events will be something like:
 *
 * RETRY upto N times with delay T secs, then SKIP / or FAIL with code C
 *
 * Options are:
 *
 * RETRY:   MAX=tries, DELAY=time_secs, BACKOFF=true/false
 * SKIP:
 * FAIL:    CODE=code, PASS=true/false
 * CALLOUT: SUBJECT=subject, MESSAGE=text
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskRecoveryInfo.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskRecoveryInfo.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class TaskRecoveryInfo {

    /** Max number of retries.*/
    protected int maxTries;

    /** Delay before retry attempt (millis).*/
    protected long delay;

    /** Error code to pass to manager after run out of attempts.*/
    protected int failCode;

    /** True if the task errorcode should be passed on to manager.*/
    protected boolean passCode;

    /** Log category.*/
    protected String logCategory = "NORMAL";

    /** Optional subject for TMC callout message.*/
    protected String calloutSubject;

    /** Optional text for TMC callout message.*/
    protected String calloutText;

    /** True if retries are allowed (@see maxTries).*/
    protected boolean retryEnabled;

    /** True if we skip after maxTries. */
    protected boolean skipEnabled;

    /** True if this is a default handler.*/
    protected boolean isdefault;
    
    public void setMaxTries(int maxTries) { this.maxTries = maxTries; }

    public int getMaxTries() { return maxTries; }

    public void setDelay(long delay) { this.delay = delay; }

    public long getDelay() { return delay; }

    public void setFailCode(int failCode) { this.failCode = failCode; }

    public int getFailCode() { return failCode; }

    public void setPassCode(boolean passCode) { this.passCode = passCode; }

    public boolean isPassCode() { return passCode; }

    public void setLogCategory(String logCategory) { this.logCategory = logCategory; }

    public String getLogCategory() { return logCategory; }

    public void setCalloutSubject(String calloutSubject) { this.calloutSubject = calloutSubject; }

    public String getCalloutSubject() { return calloutSubject; }

    public void setCalloutText(String calloutText) { this.calloutText = calloutText; } 

    public String getCalloutText() { return calloutText; }

    public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }

    public boolean isRetryEnabled() { return retryEnabled; }

    public void setSkipEnabled(boolean skipEnabled) { this. skipEnabled = skipEnabled; }

    public boolean isSkipEnabled() { return skipEnabled; }

    public void setDefault(boolean isdefault) { this.isdefault = isdefault;}

    public boolean isDefault() { return isdefault; }

    @Override
	public String toString() {
	// RecInfo : Retry: Attempts=3, Delay=30, Skip

	return "[RecoveryInfo : "+(isdefault ? "DEFAULT" : "")+
	    ( retryEnabled ? " Retry: Attempts="+maxTries+", Delay="+delay : " No-retry")+
	    ( skipEnabled ? " Skip" : " Fail:"+" LogCat: "+logCategory+", "+(passCode ? " UseTaskCode" : " Code="+failCode))+
	    "]";
    }
    
}

/** $Log: TaskRecoveryInfo.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/** */
