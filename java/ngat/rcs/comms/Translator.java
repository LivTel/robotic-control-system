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
package ngat.rcs.comms;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;


/** Provides an abstraction of the process involved in translating
 * objects - typically received from some system via a stream into
 * appropriate objects to be used by another system. There is no
 * imposed regulation of the actual mechanism or types of object
 * which can be treated as sources or targets - this is entirely
 * the responsibility of the implementor. The translate() method
 * should however typically check that the supplied source object
 * is of an appropriate type and throw a TranslationException if
 * not. In addition it should cast the resultant object appropriately. 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Translator.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/comms/RCS/Translator.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface Translator {
    
    /** This method should implement the translation process.
     * The supplied source argument should be translated into an
     * appropriate target object.
     * @param obj The object to translate.
     * @return An appropriate target object.
     * @exception TranslationException If the supplied object is
     * not of the appropriate class or any problem occurs during
     * the translation process.
     */
    public Object translate(Object obj) throws TranslationException;
    
}

/** $Log: Translator.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
