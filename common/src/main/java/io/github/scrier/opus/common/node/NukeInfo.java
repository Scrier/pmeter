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
package io.github.scrier.opus.common.node;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class NukeInfo implements IdentifiedDataSerializable {
	
	private static Logger log = LogManager.getLogger(NukeInfo.class);
	
	private long nukeID;
	private int numberOfUsers;
	private int requestedUsers;
	private boolean repeated;
	private NukeState state;
	
	public NukeInfo() {
		log.trace("NukeInfo()");
		nukeID = 0L;
		numberOfUsers = 0;
		requestedUsers = 0;
		repeated = false;
		state = NukeState.UNDEFINED;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		log.trace("readData(" + in + ")");
		setNukeID(in.readLong());
		setNumberOfUsers(in.readInt());
		setRequestedUsers(in.readInt());
		setRepeated(in.readBoolean());
		setState(NukeState.valueOf(in.readUTF()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		log.trace("writeData(" + out + ")");
		out.writeLong(getNukeID());
		out.writeInt(getNumberOfUsers());
		out.writeInt(getRequestedUsers());
		out.writeBoolean(isRepeated());
		out.writeUTF(getState().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getFactoryId() {
		log.trace("getFactoryId()");
		return NukeFactory.FACTORY_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getId() {
		log.trace("getId()");
		return NukeFactory.NUKE_INFO;
	}

	/**
	 * @return the nukeID
	 */
	public long getNukeID() {
		return nukeID;
	}

	/**
	 * @param nukeID the nukeID to set
	 */
	public void setNukeID(long nukeID) {
		this.nukeID = nukeID;
	}

	/**
	 * @return the numberOfUsers
	 */
	public int getNumberOfUsers() {
		return numberOfUsers;
	}

	/**
	 * @param numberOfUsers the numberOfUsers to set
	 */
	public void setNumberOfUsers(int numberOfUsers) {
		this.numberOfUsers = numberOfUsers;
	}

	/**
	 * @return the requestedUsers
	 */
	public int getRequestedUsers() {
		return requestedUsers;
	}

	/**
	 * @param requestedUsers the requestedUsers to set
	 */
	public void setRequestedUsers(int requestedUsers) {
		this.requestedUsers = requestedUsers;
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
	 * @return the state
	 */
	public NukeState getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(NukeState state) {
		this.state = state;
	}	

}