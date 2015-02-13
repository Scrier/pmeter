/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Andreas Joelsson (andreas.joelsson@gmail.com)
 */
package io.github.scrier.opus.nuke.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.scrier.opus.common.Shared;
import io.github.scrier.opus.common.aoc.BaseListener;
import io.github.scrier.opus.common.aoc.BaseDataC;
import io.github.scrier.opus.common.exception.InvalidOperationException;
import io.github.scrier.opus.common.nuke.CommandState;
import io.github.scrier.opus.common.nuke.NukeCommand;
import io.github.scrier.opus.common.nuke.NukeFactory;
import io.github.scrier.opus.common.nuke.NukeInfo;
import io.github.scrier.opus.nuke.task.procedures.ExecuteTaskProcedure;
import io.github.scrier.opus.nuke.task.procedures.QueryTaskProcedure;
import io.github.scrier.opus.nuke.task.procedures.RepeatedExecuteTaskProcedure;
import io.github.scrier.opus.nuke.task.procedures.NukeProcedure;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapEvent;

public class NukeTasks extends BaseListener {
	
	private static Logger log = LogManager.getLogger(NukeTasks.class);
	
	private Context theContext;
	private Long identity;
	
	private NukeInfo nukeInfo;
	
	private int proceduresStopping;
	private int proceduresTerminating;
	
	private NukeCommand stopCommand;
	private NukeCommand terminateCommand;
	
	private List<BaseTaskProcedure> procedures;
	private List<BaseTaskProcedure> proceduresToAdd;
	private List<BaseTaskProcedure> toRemove;
	
	public NukeTasks(HazelcastInstance instance) {
	  super(instance, Shared.Hazelcast.BASE_NUKE_MAP);
	  log.trace("NukeTasks(" + instance + ")");
	  theContext = Context.INSTANCE;
	  procedures = new ArrayList<BaseTaskProcedure>();
	  proceduresToAdd = new ArrayList<BaseTaskProcedure>();
	  toRemove = new ArrayList<BaseTaskProcedure>();
	  setNukeInfo(new NukeInfo());
	  setProceduresStopping(0);
	  setProceduresTerminating(0);
	  setStopCommand(null);
	  setTerminateCommand(null);
  }
	
	public void init() {
		log.trace("init()");
		try {
		  setIdentity(theContext.getIdentity());
		  registerProcedure(new NukeProcedure(getIdentity()));
		} catch(InvalidOperationException e) {
	    log.error("Received InvalidOperationException when calling NukeTasks init.", e);
		}
		intializeProcedures();
	}
	
	public void shutDown() {
		log.trace("shutDown()");
		clear(getProceduresToAdd());
		clear(getProcedures());
		clear(getProceduresToRemove());
		// Remove the info about this nuke from the map.
		removeEntry(getNukeInfo());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void preEntry() {
		log.trace("preEntry()");
		// put this first so that any init method comes with the updates to nuke info.
	  getNukeInfo().resetValuesModified();
	  intializeProcedures();
	  toRemove.clear();
  }

	private synchronized void intializeProcedures() {
		log.trace("intializeProcedures()");
		if( true != getProceduresToAdd().isEmpty() ) {
			log.debug("Adding " + getProceduresToAdd().size() + " procedures.");
			for( BaseTaskProcedure procedure : getProceduresToAdd() ) {
				try {
					procedure.init();
					procedures.add(procedure);
				} catch (Exception e) {
					log.error("init of procedure: " + procedure + " threw Exception", e);
				}
			}
			proceduresToAdd.clear();
		}
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void entryAdded(Long component, BaseDataC data) {
		log.trace("entryAdded(" + component + ", " + data + ")");
		switch( data.getId() ) {
			case NukeFactory.NUKE_INFO:
			{
				// do nothing
				break;
			}
			case NukeFactory.NUKE_COMMAND: 
			{
				NukeCommand command = new NukeCommand(data);
				handleCommand(command);
				break;
			}
			default:
			{
				log.error("Unknown id of data handler with id: " + data.getId() + ".");
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void entryEvicted(Long component, BaseDataC data) {
		log.trace("entryEvicted(" + component + ", " + data + ")");
		for( BaseTaskProcedure procedure : getProcedures() ) {
			int result = procedure.handleOnEvicted(data);
			if( procedure.COMPLETED == result ) {
				log.debug("Procedure " + procedure + " completed.");
				removeProcedure(procedure);
			} else if ( procedure.ABORTED == result ) {
				log.debug("Procedure " + procedure + " aborted.");
				removeProcedure(procedure);
			}
		}
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void entryRemoved(Long key) {
		log.trace("entryRemoved(" + key + ")");
		if( null != getStopCommand() && key == getStopCommand().getKey() ) {
			setStopCommand(null);
		} else if ( null != getTerminateCommand() && key == getTerminateCommand().getKey() ) {
			setTerminateCommand(null);
		} else {
			for( BaseTaskProcedure procedure : getProcedures() ) {
				int result = procedure.handleOnRemoved(key);
				if( procedure.COMPLETED == result ) {
					log.debug("Procedure " + procedure + " completed.");
					removeProcedure(procedure);
				} else if ( procedure.ABORTED == result ) {
					log.debug("Procedure " + procedure + " aborted.");
					removeProcedure(procedure);
				}
			}
		}
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void entryUpdated(Long component, BaseDataC data) {
		log.trace("entryUpdated(" + component + ", " + data + ")");
		for( BaseTaskProcedure procedure : getProcedures() ) {
			int result = procedure.handleOnUpdated(data);
			if( procedure.COMPLETED == result ) {
				log.debug("Procedure " + procedure + " completed.");
				removeProcedure(procedure);
			} else if ( procedure.ABORTED == result ) {
				log.debug("Procedure " + procedure + " aborted.");
				removeProcedure(procedure);
			}
		}
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void postEntry() {
		log.trace("postEntry()");
	  intializeProcedures();
	  for( BaseTaskProcedure procedure : getProceduresToRemove() ) {
	  	try {
	  		handleInterrupted(procedure);
	  		procedure.shutDown();
	  		procedures.remove(procedure);
	  	} catch (Exception e) {
	  		log.fatal("shutDown of procedure: " + procedure + " threw Exception", e);
	  	}
	  }
	  // Update entry in global map if change is made, put this last if shutdown method is calling them.
	  if( true == getNukeInfo().isValuesModified() ) {
	  	log.info("Updating NukeInfo with: " + getNukeInfo() + ".");
	  	updateEntry(getNukeInfo());
	  }
  }

	protected void handleInterrupted(BaseTaskProcedure procedure) {
		log.trace("handleInterrupted(" + procedure + ")");
	  if( null != getTerminateCommand() && procedure.isTerminated()) {
	  	if( 0 == getProceduresTerminating() ) {
	  		log.fatal("Received a terminating procedure " + procedure + " when we weren't expecting one.");
	  		throw new RuntimeException("Received a terminating procedure " + procedure + " when we weren't expecting one.");
	  	} else if ( 0 > decProceduresTerminating() ) {
	  		log.fatal("Decrease terminating procedure below 0 when removing " + procedure + ".");
	  		throw new RuntimeException("Decrease terminating procedure below 0 when removing " + procedure + ".");
	  	} else if ( 0 != getProceduresTerminating() )
	  		log.debug("Procedure " + procedure + " finished terminating execution, we have " + getProceduresTerminating() + " left.");
	  	else {
	  		log.info("Terminate is finished, sending done command.");
	  		getTerminateCommand().setState(CommandState.DONE);
	  		updateEntry(getTerminateCommand());
	  	}
	  } else if ( null != getStopCommand() ) {
	  	if( 0 == getProceduresStopping() ) {
	  		log.fatal("Received a terminating procedure " + procedure + " when we weren't expecting one.");
	  		throw new RuntimeException("Received a stopping procedure " + procedure + " when we weren't expecting one.");
	  	} else if ( 0 > decProceduresStopping() ) {
	  		log.fatal("Decrease stopping procedure below 0 when removing " + procedure + ".");
	  		throw new RuntimeException("Decrease stopping procedure below 0 when removing " + procedure + ".");
	  	} else if ( 0 != getProceduresStopping() )
	  		log.debug("Procedure " + procedure + " finished stopping execution, we have " + getProceduresStopping() + " left.");
	  	else {
	  		log.info("Terminate is finished, sending done command.");
	  		getStopCommand().setState(CommandState.DONE);
	  		updateEntry(getStopCommand());
	  	}
	  }
  }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
  public void mapCleared(MapEvent cleared) {
		log.trace("mapCleared(" + cleared + ")");
		log.error("Map was cleared, removing all.");
		removeAllProcedures();
  }

	/**
	 * {@inheritDoc}
	 */
	@Override
  public void mapEvicted(MapEvent evicted) {
		log.trace("mapEvicted(" + evicted + ")");
		log.error("Map was evicted, removing all.");
		removeAllProcedures();
  }
	
	/**
	 * Method to handle commands.
	 * @param command NukeCommand to handle.
	 */
	private void handleCommand(NukeCommand command) {
		log.trace("handleCommand(" + command + ")");
	  switch( command.getState() ) {
	  	case EXECUTE: {
	  		if( getIdentity() != command.getComponent() ) {
	  			log.debug("NukeCommand " + command.getState() + " not for us, expected " + getIdentity() + " but was " + command.getComponent() +  ".");
	  		} else {
	  			if( Shared.Commands.Execute.STOP_EXECUTION.equals(command.getCommand()) ) {
	  				log.info("Received command to stop all executions.");
	  				setProceduresStopping(distributeExecuteUpdateCommands(CommandState.STOP));
	  				if( 0 < getProceduresStopping() ) {
		  				log.info("Issued stop command to " + getProceduresStopping() + " procedures, waiting for done.");
		  				setStopCommand(command);
	  				} else {
	  					log.info("All procedures stopped, updating command.");
	  					command.setState(CommandState.DONE);
	  					updateEntry(command);
	  				}
	  			} else if ( Shared.Commands.Execute.TERMINATE_EXECUTION.equals(command.getCommand()) ) {
	  				log.info("Received command to terminate all executions.");
	  				setProceduresTerminating(distributeExecuteUpdateCommands(CommandState.TERMINATE));
	  				log.info("Issued terminate command to " + getProceduresTerminating() + " procedures, we hade " + getProceduresStopping() + " that failed stopping, waiting for done.");
	  				setTerminateCommand(command);
	  				if( null != getStopCommand() ) {
	  					log.info("Received terminate command before stopped, aborting stop command.");
	  					setProceduresStopping(0);
	  					getStopCommand().setState(CommandState.ABORTED);
	  					updateEntry(getStopCommand());
	  				}
	  			} else {
	  				log.info("Received common command: " + command + ".");
	  				if( command.isRepeated() ) {
	  					registerProcedure(new RepeatedExecuteTaskProcedure(command));
	  				} else {
	  					registerProcedure(new ExecuteTaskProcedure(command));
	  				}
	  			}
	  		}
	  		break;
	  	}
	  	case QUERY: {
	  		if( getIdentity() != command.getComponent() ) {
	  			log.debug("NukeCommand " + command.getState() + " not for us, expected " + getIdentity() + " but was " + command.getComponent() +  ".");
	  		} else {
	  			registerProcedure(new QueryTaskProcedure(command));
	  		}
	  		break;
	  	}
	  	case ABORTED: 
	  	case DONE: 
	  	case UNDEFINED: 
	  	case WORKING: {
	  		log.error("Unhandled data: " + command.getState() + ", from NukeCommand: " + command + ".");
	  		break;
	  	}
	  	default: {
	  		throw new RuntimeException("Unimplemented state " + command.getState() + " from class NukeCommand in class NukeTasks.");
	  	}
	  }
	}
	
	public int distributeExecuteUpdateCommands(CommandState state) {
		log.trace("distributeExecuteUpdateCommands(" + state + ")");
		List<BaseTaskProcedure> procs = getProcedures(ExecuteTaskProcedure.class, RepeatedExecuteTaskProcedure.class);
		log.debug("We have " + procs.size() + " procedures to update locally.");
		for( BaseTaskProcedure proc : procs ) {
			NukeCommand command = null;
			if( ExecuteTaskProcedure.class.getName() == proc.getClass().getName() ) {
				ExecuteTaskProcedure procEx = (ExecuteTaskProcedure)proc;
				command = procEx.getCommand();
			} else if ( RepeatedExecuteTaskProcedure.class.getName() == proc.getClass().getName() ) {
				RepeatedExecuteTaskProcedure procEx = (RepeatedExecuteTaskProcedure)proc;
				command = procEx.getCommand();
			} else {
				log.fatal("Received proc that did not match RepeatedExecuteTaskProcedure or ExecuteTaskProcedure.");
				throw new RuntimeException("Received proc that did not match RepeatedExecuteTaskProcedure or ExecuteTaskProcedure.");
			}
			command.setState(state);
			log.debug("Sending local command to " + proc.getTxID() + " to change state to " + state);
			proc.updateEntry(command);
		}
		return procs.size();
	}
	
	/**
	 * Method to get a list of procedures of a specific class.
	 * @param procs the class(es) to look for.
	 * @return List List with BaseTaskProcedure
	 * {@code
	 * List<BaseTaskProcedure> commandProcedures = getProcedurs(CommandProcedure.class);
	 * for( BaseTaskProcedure procedure : commandProcedures ) {
	 *   ...
	 * }
	 * ...
	 * List<BaseTaskProcedure> commandProcedures = getProcedurs(CommandProcedure.class, NukeProcedure.class);
	 * for( BaseTaskProcedure procedure : commandProcedures ) {
	 *   ...
	 * }
	 * }
	 */
	public List<BaseTaskProcedure> getProcedures(Class<?>... procs) {
		List<BaseTaskProcedure> retVal = new ArrayList<BaseTaskProcedure>();
		for( Class<?> proc : procs ) {
			for( BaseTaskProcedure procedure : getProcedures() ) {
				if( proc.getName() == procedure.getClass().getName() ) {
					retVal.add(procedure);
				}
			}
		}
		return retVal;
	}
	
	/**
	 * @return the identity
	 */
	protected Long getIdentity() {
		return identity;
	}

	/**
	 * @param identity the identity to set
	 */
	private void setIdentity(Long identity) {
		this.identity = identity;
	}
	
	/**
	 * @return the procedures
	 */
	protected List<BaseTaskProcedure> getProcedures() {
		return procedures;
	}
	
	/**
	 * @return the proceduresToAdd
	 */
	protected List<BaseTaskProcedure> getProceduresToAdd() {
		return proceduresToAdd;
	}
	
	/**
	 * @return the toRemove
	 */
	protected List<BaseTaskProcedure> getProceduresToRemove() {
		return toRemove;
	}
	
	private void removeProcedure(BaseTaskProcedure procedure) {
		log.trace("removeProcedure(" + procedure + ")");
		toRemove.add(procedure);
	}
	
	public boolean registerProcedure(BaseTaskProcedure procedure) {
		log.trace("registerProcedure(" + procedure + ")");
		boolean retValue = true;
		if( contains(procedure) ) {
			retValue = false;
		} else {
			retValue = proceduresToAdd.add(procedure);
		}
		return retValue;
	}
	
	private boolean contains(BaseTaskProcedure procedure) {
		log.trace("contains(" + procedure + ")");
		boolean retValue = procedures.contains(procedure);
		retValue = ( true == retValue ) ? true : proceduresToAdd.contains(procedure);
		return retValue;
	}
	
	private void removeAllProcedures() {
		log.trace("removeAllProcedures()");
		clear(getProcedures());
	}
	
	public void clear(List<BaseTaskProcedure> toClear) {
		log.trace("clear(" + toClear + ")");
		for( BaseTaskProcedure procedure : toClear ) {
			try {
	      procedure.shutDown();
      } catch (Exception e) {
      	log.error("shutDown of procedure: " + procedure + " threw Exception", e);
      }
		}
		toClear.clear();
	}

	/**
	 * @return the nukeInfo
	 */
  public NukeInfo getNukeInfo() {
	  return nukeInfo;
  }

	/**
	 * @param nukeInfo the nukeInfo to set
	 */
  public void setNukeInfo(NukeInfo nukeInfo) {
	  this.nukeInfo = nukeInfo;
  }

	/**
	 * @return the proceduresStopping
	 */
  public int getProceduresStopping() {
	  return proceduresStopping;
  }
  
	/**
	 * @return the proceduresStopping after decreased by one
	 */
  public int decProceduresStopping() {
	  return --this.proceduresStopping;
  }

	/**
	 * @param proceduresStopping the proceduresStopping to set
	 */
  public void setProceduresStopping(int proceduresStopping) {
	  this.proceduresStopping = proceduresStopping;
  }

	/**
	 * @return the proceduresTerminating
	 */
  public int getProceduresTerminating() {
	  return proceduresTerminating;
  }
  
	/**
	 * @return the proceduresTerminating after decreased by one
	 */
  public int decProceduresTerminating() {
	  return --this.proceduresTerminating;
  }

	/**
	 * @param proceduresTerminating the proceduresTerminating to set
	 */
  public void setProceduresTerminating(int proceduresTerminating) {
	  this.proceduresTerminating = proceduresTerminating;
  }

	/**
	 * @return the stopCommand
	 */
  public NukeCommand getStopCommand() {
	  return stopCommand;
  }

	/**
	 * @param stopCommand the stopCommand to set
	 */
  public void setStopCommand(NukeCommand stopCommand) {
	  this.stopCommand = stopCommand;
  }

	/**
	 * @return the terminateCommand
	 */
  public NukeCommand getTerminateCommand() {
	  return terminateCommand;
  }

	/**
	 * @param terminateCommand the terminateCommand to set
	 */
  public void setTerminateCommand(NukeCommand terminateCommand) {
	  this.terminateCommand = terminateCommand;
  }

}
