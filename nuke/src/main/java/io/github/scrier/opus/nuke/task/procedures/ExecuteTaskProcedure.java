package io.github.scrier.opus.nuke.task.procedures;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.scrier.opus.common.aoc.BaseNukeC;
import io.github.scrier.opus.common.nuke.CommandState;
import io.github.scrier.opus.common.nuke.NukeCommand;
import io.github.scrier.opus.common.nuke.NukeFactory;
import io.github.scrier.opus.nuke.task.BaseTaskProcedure;

public class ExecuteTaskProcedure extends BaseTaskProcedure implements Callable<String> {
	
	private static Logger log = LogManager.getLogger(ExecuteTaskProcedure.class);
	
	private NukeCommand command;
	
	boolean stopped;
	boolean terminated;
	
	public final int RUNNING = CREATED + 1;
	
	public ExecuteTaskProcedure(NukeCommand command) {
		log.trace("ExecuteTaskProcedure(" + command + ")");
		setCommand(new NukeCommand(command));
		setStopped(false);
		setTerminated(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void init() throws Exception {
		log.trace("init()");
	  getExecutor().submit(this);
	  getNukeInfo().setActiveCommands(getNukeInfo().getActiveCommands() + 1);
	  getNukeInfo().setRequestedCommands(getNukeInfo().getRequestedCommands() + 1);
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void shutDown() throws Exception {
		log.trace("shutDown()");
		if( true != isProcedureFinished() ) {
			log.fatal("shutDown called in a state where we arent finished.");
			throw new RuntimeException("shutDown called in a state where we arent finished.");
		} else {
		  getNukeInfo().setActiveCommands(getNukeInfo().getActiveCommands() - 1);
		  getNukeInfo().setCompletedCommands(getNukeInfo().getCompletedCommands() + 1);
		}
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public int handleOnUpdated(BaseNukeC data) {
		log.trace("handleOnUpdated(" + data + ")");
		if( data.getKey() == getCommand().getKey() ) {
			switch ( data.getId() ) {
				case NukeFactory.NUKE_COMMAND: {
					NukeCommand nukeCommand = new NukeCommand(data);
					handleUpdate(nukeCommand);
					break;
				}
				default: {
					throw new RuntimeException("Unhandled id: " + data.getId() + " received in RepeatedExecuteTaskProcedure.handleOnUpdated(" + data + ").");
				}
			}
		}
	  return getState();
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public int handleOnEvicted(BaseNukeC data) {
		log.trace("handleOnEvicted(" + data + ")");
		if( data.getKey() == getCommand().getKey() ) {
			log.error("[" + getTxID() + "] NukeCommand: " + getNukeInfo() + " was evicted.");
			setState(ABORTED);
		}
	  return getState();
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public int handleOnRemoved(Long key) {
		log.trace("handleOnRemoved(" + key + ")");
		if( key == getCommand().getKey() && true != isProcedureFinished() ) {
			log.error("[" + getTxID() + "] NukeCommand: " + getNukeInfo() + " was removed before we were finished.");
			setState(ABORTED);
		}
	  return getState();
  }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
  public String call() throws Exception {
		log.trace("call()");
		setState(RUNNING);
		getCommand().setState(CommandState.WORKING);
		if( true != updateEntry(getCommand()) ) {
			log.error("[" + getTxID() + "] Unable to update command: " + getCommand() + " in ExecuteTaskProcedure.call");
			setState(ABORTED);
  		return "Unable to update command: " + getCommand() + " in ExecuteTaskProcedure.call";
  	} 
	  String executeString = getCommand().getCommand();
	  boolean result = false;
  	if( getCommand().getFolder().isEmpty() ) {
  		result = executeProcess(executeString, null, null);
  	} else {
  		result = executeProcess(executeString, new File(getCommand().getFolder()), null);
  	}
	  log.debug("[" + getTxID() + "] Process returns: " + result + ".");
	  if( result ) {
	  	getCommand().setState(CommandState.DONE);
	  	setState(COMPLETED);
	  } else {
	  	getCommand().setState(CommandState.ABORTED);
	  	setState(ABORTED);
	  }
  	if( true != updateEntry(getCommand()) ) { // this command should trigger call to OnUpdated that should terminate this procedure.
  		log.error("[" + getTxID() + "] Unable to update command: " + getCommand() + " in ExecuteTaskProcedure.call");
  		return "Unable to update command: " + getCommand() + " in ExecuteTaskProcedure.call";
  	} 
	  return null;
  }
	
	/**
	 * Method to handle updates to the NukeCommand associated with this task procedure.
	 * @param nukeCommand NukeCommand to handle.
	 */
	private void handleUpdate(NukeCommand nukeCommand) {
		log.trace("handleUpdate(" + nukeCommand + ")");
		if( CommandState.STOP == nukeCommand.getState() ) {
			log.info("[" + getTxID() + "] Received command to stop execution from " + nukeCommand.getComponent() + ".");
			setStopped(true);
		} else if ( CommandState.TERMINATE == nukeCommand.getState() ) {
			log.info("[" + getTxID() + "] Received command to terminate execution from " + nukeCommand.getComponent() + ".");
			terminateProcess();
			setStopped(false);    // not necessary as terminated has precedence.
			setTerminated(true);
		}
	}

	/**
	 * @return the command
	 */
  public NukeCommand getCommand() {
	  return command;
  }

	/**
	 * @param command the command to set
	 */
  public void setCommand(NukeCommand command) {
	  this.command = command;
  }
  
  /**
   * @param stopped the stopped to set
   */
  public void setStopped(boolean stopped) {
  	this.stopped = stopped;
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public boolean isStopped() {
	  return this.stopped;
  }
	
  /**
   * @param terminated the terminated to set
   */
  public void setTerminated(boolean terminated) {
  	this.terminated = terminated;
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public boolean isTerminated() {
	  return this.terminated;
  }

}
