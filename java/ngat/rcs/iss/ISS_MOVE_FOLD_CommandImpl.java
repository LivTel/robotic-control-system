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
package ngat.rcs.iss;

import ngat.rcs.scm.collation.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;

public class ISS_MOVE_FOLD_CommandImpl extends ISS_CommandImpl {

	public static final String CLASS = "ISS_MOVE_FOLD_CommandImpl";

	/** This is set if we are already on the port so we dont bother to send it. */
	protected boolean overrideNoForward = false;

	// by default we will override the MOVE_FOLDand fake a reply !
	private static boolean overrideForwarding = false;

	/** The fold posn requested. */
	int rfold;

	/** The fold position code requested. */
	int rsfold;

	/** Current fold position. */
	int cfold;

	/** Current fold position code. */
	int csfold;

	// default fold deploy position is AG.INLINE
	int foldDeployPosition = TCS_Status.POSITION_INLINE;

	// default fold stow position is AG.RETRACT
	int foldStowPosition = TCS_Status.POSITION_RETRACT;

	// deploy command
	int agFilterDeploy = ngat.message.RCS_TCS.AGFILTER.IN;

	// stow command
	int agFilterStowed = ngat.message.RCS_TCS.AGFILTER.OUT;

	private boolean simulation;

	public ISS_MOVE_FOLD_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
		String sciag = System.getProperty("science.fold.deploy.agfilter", "INLINE");
		simulation = (System.getProperty("science.fold.simulation") != null);
		if (simulation) {
			System.err.println("ISS_MOVE_FOLD::Init<>: Read deploy keyvalue: " + sciag);
			if (sciag.equals("RETRACT")) {
				foldDeployPosition = TCS_Status.POSITION_RETRACT;
				foldStowPosition = TCS_Status.POSITION_INLINE;
				agFilterDeploy = ngat.message.RCS_TCS.AGFILTER.OUT;
				agFilterStowed = ngat.message.RCS_TCS.AGFILTER.IN;
				System.err.println("ISS_MOVE_FOLD::Init<>: Deploy will use AG_OUT, Stow will use AG_IN");
			} else {
				foldDeployPosition = TCS_Status.POSITION_INLINE;
				foldStowPosition = TCS_Status.POSITION_RETRACT;
				agFilterDeploy = ngat.message.RCS_TCS.AGFILTER.IN;
				agFilterStowed = ngat.message.RCS_TCS.AGFILTER.OUT;
				System.err.println("ISS_MOVE_FOLD::Init<>: Deploy will use AG_IN, Stow will use AG_OUT");
			}
		}
	}

	/** Override forwarding for this command. */
	public static void setOverrideForwarding(boolean ovr) {
		overrideForwarding = ovr;
	}

	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
		super.processReceivedCommand(receivedCommand);

		// check for no-change - no send onwards
		if (receivedCommand instanceof ngat.message.ISS_INST.MOVE_FOLD) {
			csfold = StatusPool.latest().mechanisms.foldMirrorPos;
			// int cdeploy = StatusPool.latest().autoguider.agFilterPos;

			// if (cdeploy == foldDeployPosition) {
			switch (csfold) {
			case TCS_Status.POSITION_PORT_1:
				cfold = 1;
				break;
			case TCS_Status.POSITION_PORT_2:
				cfold = 2;
				break;
			case TCS_Status.POSITION_PORT_3:
				cfold = 3;
				break;
			case TCS_Status.POSITION_PORT_4:
				cfold = 4;
				break;
			case TCS_Status.POSITION_PORT_5:
				cfold = 5;
				break;
			case TCS_Status.POSITION_PORT_6:
				cfold = 6;
				break;
			case TCS_Status.POSITION_PORT_7:
				cfold = 7;
				break;
			case TCS_Status.POSITION_PORT_8:
				cfold = 8;
				break;
			}
			// } else if (cdeploy == foldStowPosition) {

			// if agfilter shows stowed value then the fold is stowed
			// (port-0)
			// cfold = 0;
			// } else {
			// we dont really know for sure
			// cfold = -1;
			// }

			rfold = ((ngat.message.ISS_INST.MOVE_FOLD) receivedCommand).getMirror_position();

			System.err.println("ISS_MOVE_FOLD::Processing():Current Fold position: PORT:" + cfold
					+ ", Requested: PORT:" + rfold);

		}

	}

	/** LT fudge - set to 300 sec to allow MF and AGF commands with retries. */
	@Override
	public long calculateTimeToComplete() {
		return 300000L;
	}

	/** Forward if the request is for a new port. */
	@Override
	public boolean doesForward() {

		return false;

	}

	@Override
	public COMMAND_DONE makeResponse() {

		ngat.message.ISS_INST.MOVE_FOLD_DONE done = new ngat.message.ISS_INST.MOVE_FOLD_DONE("Forwarded");
		done.setSuccessful(true);

		if (rfold > 0) {

			// deploy fold
			if (simulation) {
				ngat.message.RCS_TCS.AGFILTER agfilter = new ngat.message.RCS_TCS.AGFILTER(receivedCommand.getId());
				agfilter.setState(agFilterDeploy);

				ACK ack = makeAck(150000L); // 60 sec delay, 60 sec command
				// timeout + 30 spare
				ack.setId("Agfilter single attempt");
				serverImpl.sendAck(ack);

				System.err.println("ISS_MOVE_FOLD::Outgoing():Sending ag-filter command");
				try {
					JMSHandler handler2 = sendCommand(agfilter, 60000);
					if (handler2.isDone()) {

						COMMAND_DONE agf_done = handler2.getResponse();

						if (!agf_done.getSuccessful()) {
							return failed(done, agf_done.getErrorNum(), agf_done.getErrorString());
						}

					} else {
						return failed(done, 60001, "AGFILTER timeout ?");
					}
				} catch (Exception e) {
					return failed(done, 60000, "Exception in deploy-fold forwarding: " + e);
				}
			} else {
				// not simulating using EIP
				try {
					// ScienceFold sfd =
					// RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload()
					// .getScienceFold();
					IssScienceFoldPlugin sfd = new IssScienceFoldPlugin();
					sfd.deploy();

				} catch (Exception e) {
					e.printStackTrace();
					return failed(done, 60002, "Exception while deploying fold: " + e);
				}

			}

			if (rfold != cfold) {

				// move to port if required

				// Send MOVE_FOLD

				ngat.message.RCS_TCS.MOVE_FOLD move_fold = new ngat.message.RCS_TCS.MOVE_FOLD(receivedCommand.getId());
				move_fold.setState(rfold);// the MOVE_FOLD maps directly to port
				// number

				// send an extra ACK to the instrument
				ACK ack = makeAck(150000L); // 60 sec delay, 60 sec command
				// timeout + 30 spare
				ack.setId("Move-fold single attempt");
				serverImpl.sendAck(ack);

				System.err.println("ISS_MOVE_FOLD::Outgoing():Sending move-fold command");
				try {
					JMSHandler handler1 = sendCommand(move_fold, 60000);
					if (handler1.isDone()) {

						COMMAND_DONE mf_done = handler1.getResponse();

						if (!mf_done.getSuccessful()) {
							return failed(done, mf_done.getErrorNum(), mf_done.getErrorString());
						}

					} else {
						return failed(done, 60001, "MOVE_FOLD timeout ?");
					}
				} catch (Exception e) {
					return failed(done, 60000, "Exception in move-fold forwarding: " + e);
				}
			} // ok we moved to a new port

		} else {

			// we are stowing in this case

			if (simulation) {

				// Send AGFILTER command - 60 sec timeout

				ngat.message.RCS_TCS.AGFILTER agfilter = new ngat.message.RCS_TCS.AGFILTER(receivedCommand.getId());
				agfilter.setState(agFilterStowed);

				// we have several tries, keep track of error messages may
				// need
				// them..
				// send an extra ACK to the instrument
				ACK ack = makeAck(150000L); // 60 sec delay, 60 sec command
				// timeout + 30 spare
				ack.setId("Agfilter single attempt");
				serverImpl.sendAck(ack);

				System.err.println("ISS_MOVE_FOLD::Outgoing():Sending ag-filter command");
				try {
					JMSHandler handler2 = sendCommand(agfilter, 60000);
					if (handler2.isDone()) {

						COMMAND_DONE agf_done = handler2.getResponse();

						if (!agf_done.getSuccessful()) {
							return failed(done, agf_done.getErrorNum(), agf_done.getErrorString());
						}

					} else {
						return failed(done, 60001, "AGFILTER timeout ?");
					}
				} catch (Exception e) {
					return failed(done, 60000, "Exception in move-fold forwarding: " + e);
				}

			} else {

				// not simulating using EIP
				try {
					// ScienceFold sfd =
					// RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload()
					// /.getScienceFold();
					IssScienceFoldPlugin sfd = new IssScienceFoldPlugin();
					sfd.stow();

				} catch (Exception e) {
					e.printStackTrace();
					return failed(done, 60002, "Exception while stowing fold: " + e);
				}
			}
		}

		// we only get here if all is well...

		done.setErrorString("Successfully forwarded commands");
		done.setErrorNum(0);
		return done;

	}

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {
		super.translateResponse(response);
		ngat.message.ISS_INST.MOVE_FOLD_DONE done = new ngat.message.ISS_INST.MOVE_FOLD_DONE(receivedCommand.getId());
		done.setSuccessful(response.getSuccessful());
		done.setErrorNum(response.getErrorNum());
		done.setErrorString(response.getErrorString());
		return done;
	}

	private COMMAND_DONE failed(COMMAND_DONE done, int errno, String errmsg) {
		done.setSuccessful(false);
		done.setErrorString(errmsg);
		done.setErrorNum(errno);
		System.err.println("iss-mf: create failure response: " + errno + ", " + errmsg);
		return done;
	}

}
