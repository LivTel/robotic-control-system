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
package ngat.rcs.tocs;

import ngat.net.*;

/** Factory for generating handlers for TOC commands received from
 * a TOCS Service Agent (SA).
 * This class can only be used via the singleton pattern, by calling
 * the static method getInstance(). A typical use might be as follows:-
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = TOC_CommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(aProtocolImpl, aCommand);
 *    ..
 *    ..
 * </pre>
 * <br><br>
 * $Id: TOC_CommandImplFactory.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOC_CommandImplFactory implements RequestHandlerFactory {

    private static TOC_CommandImplFactory instance = null;

    public static TOC_CommandImplFactory getInstance() {
	if (instance == null)
	    instance = new TOC_CommandImplFactory();
	return instance;
    }

    /** Selects the appropriate handler for the specified command. 
     * May return <i>null</i> if the ProtocolImpl is not defined or not an
     * instance of TOC_ProtocolServerImpl or the request is not
     * defined or not an instance of String. */
    public RequestHandler createHandler(ProtocolImpl serverImpl,
					Object       request) {
	
	// Deal with undefined and illegal args.
	if ( (serverImpl == null) ||
	     ! (serverImpl instanceof TOC_ProtocolServerImpl) ) return null;
	if ( (request == null)    || 
	     ! (request instanceof String) ) return null;
	
	// Cast to correct subclass.
	String command = (String)request;
	
	// Choose an POS_CommandImpl - for now ALL are generic.	
	return new TOC_GenericCommandImpl((TOC_ProtocolServerImpl)serverImpl, command);		    
	
    }
    
    /** Private constructor for singleton instance.*/
    private TOC_CommandImplFactory() {}

}    

/** $Log: TOC_CommandImplFactory.java,v */
