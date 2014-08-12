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

import java.text.*;
import java.util.*;

public class LT_RGO_ArgParser {

    /** Constant: Indicates that the current token is not set.*/
    public static final int NONE = 0;

    /** Constant: Indicates that the current token is text String.*/
    public static final int TEXT = 1;

    /** Constant: Indicates that the current token is a sequence number.*/
    public static final int CODE = 2;

    /** Constant: Indicates that the current token is not valid.*/
    public static final int ERROR = 3;

     /** Constant: Indicates that the current token is the END-OF-LIST.*/
    public static final int EOL   = 4;

    /** The current token type.*/
    public int type;

    /** The current token sequence number - if (type == CODE).*/
    public int sequence;

    /** The largest sequence no parsed.*/
    protected int maxsequence;

    /** The current token String - if (type == TEXT).*/
    public String token;

    protected StringBuffer sequenceBuffer;

    protected StringBuffer tokenBuffer;

    int parsePosition;

    int parseLength;

    boolean end;

    Properties hash;

    /** The String to parse.*/
    protected String args;
    
    public LT_RGO_ArgParser() {
	hash = new Properties();
	maxsequence = 0;
    }

    /** Clears the Parser for another run.*/
    public void clear() {
	hash.clear();
	maxsequence = 0;
    }


    /** Setup to start parsing the supplied String.
     * @param args The String to start parsing.
     */
    public void parse(String args) throws ParseException, NumberFormatException {
	this.args = args; 
	parsePosition = 0;
	parseLength   = args.length(); 
	end = false;
	hash.clear();
	maxsequence = 0;
	//System.err.println("parse: ["+args+"]");
	while (parsePosition < parseLength) {
	    tokenBuffer    = new StringBuffer();
	    sequenceBuffer = new StringBuffer();
	    skipWhiteSpace();
	    readSequence();
	    if (end) break;
	    skipWhiteSpace();
	    readToken();
	    saveToken(sequence, token);
	}
	//System.err.println("DONE OK");
    }

    protected void skipWhiteSpace() {
	while (parsePosition < parseLength && 
	       (args.charAt(parsePosition) == ' ')) {
	    parsePosition++;
	}
    }
    
    protected void readSequence() throws ParseException {
	readChar('<');
	readChar('<');
	readNumber();
	readChar('>');
	readChar('>');
    }
    
    protected void readNumber() throws NumberFormatException {
	while (parsePosition < parseLength &&
	       (Character.isDigit(args.charAt(parsePosition)) ||
		(args.charAt(parsePosition) == 'X'))) {
	    sequenceBuffer.append(args.charAt(parsePosition)); 
	    //System.err.println("readNumber: "+args.charAt(parsePosition));
	    parsePosition++;  
	}
	//System.err.println("readNumber: sequenceBuffer:"+sequenceBuffer.toString());
	if (sequenceBuffer.toString().equals("X")) {
	    end = true;
	} else {
	    sequence = Integer.parseInt(sequenceBuffer.toString());
	}
    }
    
    protected void readChar(char c) throws ParseException {
	if (parsePosition == parseLength ||
	    (args.charAt(parsePosition) != c))
	    throw new ParseException("Illegal char (readChar) ["+
				     args.charAt(parsePosition)+
				     "] expected ["+c+"]", parsePosition);
	parsePosition++;
	//System.err.println("readChar: was:"+c);
    }
    
    protected void readToken() throws ParseException {
	while (parsePosition < parseLength &&
	       ! (args.charAt(parsePosition) == '<') ) {
	    tokenBuffer.append(args.charAt(parsePosition));
	    //System.err.println("readToken: "+args.charAt(parsePosition));
	    parsePosition++;
	}
	//System.err.println("readToken: tokenBuffer:"+tokenBuffer.toString());
	token = tokenBuffer.toString();
    }

    protected void saveToken(int seq, String tok) {
	hash.put(new Integer(seq), tok);
	if (sequence > maxsequence)
	    maxsequence = sequence;
    }

    public String getToken(int seq) throws IllegalArgumentException {
	Integer ik = new Integer(seq);
	if (hash.containsKey(ik))
	    return (String)hash.get(ik);
	throw new IllegalArgumentException("No element ["+seq+"]");
    }

    /** Returns the map of ( arg-number : value).*/
    public Properties getMap() { return hash; }

    /** Returns the largest sequence number.*/
    public int getMaxSequence() { return maxsequence; }

    public static void main(String args[] ) {
	LT_RGO_ArgParser parser = new LT_RGO_ArgParser();
	try {
	    parser.parse(args[0]);
	} catch (ParseException px) {
	    System.err.println("Failed: "+px+" at: "+px.getErrorOffset());
	} catch (NumberFormatException nx) {
	    System.err.println("Failed: "+nx);
	}
	for (int i = 0; i < 10; i++) {
	    try {
		String tok = parser.getToken(i);
		System.err.println("Seqno: "+i+" Token: "+tok);
	    } catch (IllegalArgumentException ix) {
		System.err.println(ix.toString());
	    }
	}

    }

}


