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
package ngat.rcs.control;

import ngat.rcs.*;

import ngat.net.*;
import ngat.ems.MutableSkyModel;
import ngat.message.GUI_RCS.*;

/** Tries to set the seeing used by the OSS.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: SET_SEEINGImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/SET_SEEINGImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SET_SEEINGImpl extends CtrlCommandImpl {

    public SET_SEEINGImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	SET_SEEING set = (SET_SEEING)command;

	SET_SEEING_DONE done = new SET_SEEING_DONE(command.getId());

	int    seeing = set.getSeeing();
	double value = 1.5;
	switch (seeing) {
	case SET_SEEING.SEEING_POOR:
	    value = 1.5;
	    break;
	case SET_SEEING.SEEING_AVERAGE:
	  value = 1.0;
		break;
	case SET_SEEING.SEEING_EXCELLENT:
	   value = 0.5;
	    break;
	}

	try {
		MutableSkyModel sm = (MutableSkyModel)RCS_Controller.controller.getSkyModel();
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-8000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-7000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-6000L, true, "GUI","NONE");		
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-5000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-4000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-3000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-2000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis()-1000L, true, "GUI","NONE");
		sm.updateSeeing(value, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis(), true, "GUI","NONE");
		done.setSuccessful(true);
	} catch (Exception e) {
		e.printStackTrace();
		done.setSuccessful(false);
	}
	
	sendDone(done);

    }

}
