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
package io.github.scrier.opus.common.nuke;

import java.io.IOException;

import io.github.scrier.opus.common.aoc.BaseDataC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

public class NukeCommand extends BaseDataC {
	
	private static Logger log = LogManager.getLogger(NukeCommand.class);
	
	private String command;
	private String folder;
	private String response;
	private CommandState state;
	private long component;
	private boolean repeated;
	
	public NukeCommand() {
		super(NukeFactory.FACTORY_ID, NukeFactory.NUKE_COMMAND);
		setCommand("");
		setFolder("");
		setResponse("");
		setState(CommandState.UNDEFINED);
		setComponent(-1L);
		setRepeated(false);
	}
	
	public NukeCommand(NukeCommand obj2copy) {
		super(obj2copy);
		setCommand(obj2copy.getCommand());
		setFolder(obj2copy.getFolder());
		setResponse(obj2copy.getResponse());
		setState(obj2copy.getState());
		setComponent(obj2copy.getComponent());
		setRepeated(obj2copy.isRepeated());
	}
	
	public NukeCommand(BaseDataC input) throws ClassCastException {
		super(input);
		if( input instanceof NukeCommand ) {
			NukeCommand obj2copy = (NukeCommand)input;
			setCommand(obj2copy.getCommand());
			setFolder(obj2copy.getFolder());
			setResponse(obj2copy.getResponse());
			setState(obj2copy.getState());
			setComponent(obj2copy.getComponent());
			setRepeated(obj2copy.isRepeated());
		} else {
			throw new ClassCastException("Data with id " + input.getId() + " is not an instanceof NukeCommand[" + NukeFactory.NUKE_COMMAND + "], are you using correct class?");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);
		log.trace("readData(" + in + ")");
		setCommand(in.readUTF());
		setFolder(in.readUTF());
		setResponse(in.readUTF());
		setState(CommandState.valueOf(in.readUTF()));
		setComponent(in.readLong());
		setRepeated(in.readBoolean());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		super.writeData(out);
		log.trace("writeData(" + out + ")");
		out.writeUTF(getCommand());
		out.writeUTF(getFolder());
		out.writeUTF(getResponse());
		out.writeUTF(getState().toString());
		out.writeLong(getComponent());
		out.writeBoolean(isRepeated());
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @param command the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @return the folder
	 */
  public String getFolder() {
	  return folder;
  }

	/**
	 * @param folder the folder to set
	 */
  public void setFolder(String folder) {
	  this.folder = folder;
  }

	/**
	 * @return the response
	 */
	public String getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * @return the state
	 */
	public CommandState getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(CommandState state) {
		this.state = state;
	}

	/**
	 * @return the component
	 */
  public long getComponent() {
	  return component;
  }

	/**
	 * @param component the component to set
	 */
  public void setComponent(long component) {
	  this.component = component;
  }

	/**
	 * @return the repeated
	 */
	public boolean isRepeated() {
		return repeated;
	}

	/**
	 * @param repeated the repeated to set
	 */
	public void setRepeated(boolean repeated) {
		this.repeated = repeated;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String retValue = super.toString();
		retValue += " - NukeCommand{command:" + getCommand();
		retValue += ", folder:" + getFolder();
		retValue += ", response:" + getResponse();
		retValue += ", state:" + getState();
		retValue += ", component:" + getComponent();
		retValue += ", repeated:" + isRepeated() + "}";
		return retValue;
	}

}
