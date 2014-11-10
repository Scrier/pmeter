package io.github.scrier.opus.duke.commander;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.scrier.opus.common.aoc.BaseNukeC;
import io.github.scrier.opus.common.nuke.NukeCommand;
import io.github.scrier.opus.common.nuke.CommandState;
import io.github.scrier.opus.common.nuke.NukeFactory;

public class CommandProcedure extends BaseProcedure {
	
	private static Logger log = LogManager.getLogger(CommandProcedure.class);
	
	public final int WORKING = CREATED + 1;
	public final int REMOVING = CREATED + 2;
	
	private NukeCommand nukeCommand;
	private long id;

	public CommandProcedure(String command, CommandState state) {
		this(command, state, false);
	}
	
	public CommandProcedure(String command, CommandState state, boolean repeated) {
		log.trace("CommandProcedure(" + command + ", " + state + ", " + repeated + ")");
		this.nukeCommand = new NukeCommand();
		this.nukeCommand.setCommand(command);
		this.nukeCommand.setRepeated(repeated);
		this.nukeCommand.setState(state);
		this.nukeCommand.setTxID(getTxID());
	}

	@Override
	public void init() throws Exception {
		log.trace("init()");
		setID(addEntry(getNukeCommand()));
		setState(WORKING);
	}

	@Override
	public void shutDown() throws Exception {
		log.trace("shutDown()");
	}

	@Override
	public int handleOnUpdated(BaseNukeC data) {
		log.trace("handleOnUpdated(" + data + ")");
		switch( data.getId() ) {
			case NukeFactory.NUKE_COMMAND: {
				NukeCommand command = new NukeCommand(data);
				handleUpdate(command);
				break;
			}
			case NukeFactory.NUKE_INFO:
			default: {
				// do nothing
				break;
			}
		}
		return getState();
	}

	@Override
	public int handleOnEvicted(BaseNukeC data) {
		log.trace("handleOnEvicted(" + data + ")");
		if( getTxID() == data.getTxID() ) {
			log.error("Our command was evicted from the map, cannot continue.");
			//@TODO: Handle evicted state better by commanding a new command or similar.
			setState(ABORTED);
		}
		return getState();
	}

	@Override
	public int handleOnRemoved(BaseNukeC data) {
		log.trace("handleOnRemoved(" + data + ")");
		if( getTxID() == data.getTxID() ) {
			if( REMOVING != getState() ) {
				log.error("Someone else removed our command data " + data + ", aborting.");
				//@TODO: Handle removed in wrong state better. Renew or terminate application perhaps?
				setState(ABORTED);
			} else {
				log.info("Command was removed from map, we are done.");
				setState(COMPLETED);
			}
		}
		return getState();
	}
	
	private void handleUpdate(NukeCommand command) {
		log.trace("handleUpdate(" + command + ")");
		if( getTxID() == command.getTxID() ) {
			switch( command.getState() ) {
				case EXECUTE: {
					log.info("Command is working.");
					break;
				} 
				case DONE: {
					log.info("Command is done, lets remove it.");
					removeEntry(getID());
					setState(REMOVING);
					break;
				}
				case ABORTED:
				case QUERY:
				case UNDEFINED:
				case WORKING: {
					log.error("Unimplemented command " + command.getState() + ", aborting.");
					setState(ABORTED);
					break;
				}
				default: {
					log.error("Unknown state of command " + command.getState() + ", aborting.");
					setState(ABORTED);
					break;
				}
			}
		}
	}
	
//  Might be used later. Therefor we dont remove it for now.
//	/**
//	 * Method to handle update of the command data and set to aborted if not working.
//	 * @param data BaseNukeC instance to update.
//	 */
//	private void updateEntry(BaseNukeC data) {
//		log.trace("handleOnRupdateEntryemoved(" + data + ")");
//		if( true != updateEntry(data, getID()) ) {
//			log.error("Unable to update entry " + data + ", aborting.");
//			setState(ABORTED);
//		}
//	}
	
	/**
	 * @return the nukeCommand
	 */
	protected NukeCommand getNukeCommand() {
		return nukeCommand;
	}

	/**
	 * @return the id
	 */
  private long getID() {
	  return id;
  }

	/**
	 * @param id the id to set
	 */
  private void setID(long id) {
	  this.id = id;
  }
	
}
