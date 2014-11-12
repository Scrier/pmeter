package io.github.scrier.opus.duke.commander;

import io.github.scrier.opus.common.aoc.BaseNukeC;

public class BaseProcedureMock extends BaseProcedure {
	
	private boolean initCalled;
	private boolean shutDownCalled;
	private BaseNukeC onUpdated;
	private int onUpdateReturn;
	private BaseNukeC onEvicted;
	private int onEvictedReturn;
	private BaseNukeC onRemoved;
	private int onRemovedReturn;
	
	BaseProcedureMock() {
		setInitCalled(false);
		setShutDownCalled(false);
		setOnUpdated(null);
		setOnUpdateReturn(CREATED);
		setOnEvicted(null);
		setOnEvictedReturn(CREATED);
		setOnRemoved(null);
		setOnRemovedReturn(CREATED);
	}

	@Override
  public void init() throws Exception {
	  setInitCalled(true);
  }

	@Override
  public void shutDown() throws Exception {
	  setShutDownCalled(true);
  }

	@Override
  public int handleOnUpdated(BaseNukeC data) {
	  setOnUpdated(data);
	  return getOnUpdateReturn();
  }

	@Override
  public int handleOnEvicted(BaseNukeC data) {
		setOnEvicted(data);
		return getOnEvictedReturn();
  }

	@Override
  public int handleOnRemoved(BaseNukeC data) {
		setOnRemoved(data);
		return getOnRemovedReturn();
  }

	/**
	 * @return the initCalled
	 */
  public boolean isInitCalled() {
	  return initCalled;
  }

	/**
	 * @param initCalled the initCalled to set
	 */
  public void setInitCalled(boolean initCalled) {
	  this.initCalled = initCalled;
  }

	/**
	 * @return the shutDownCalled
	 */
  public boolean isShutDownCalled() {
	  return shutDownCalled;
  }

	/**
	 * @param shutDownCalled the shutDownCalled to set
	 */
  public void setShutDownCalled(boolean shutDownCalled) {
	  this.shutDownCalled = shutDownCalled;
  }

	/**
	 * @return the onUpdated
	 */
	public BaseNukeC getOnUpdated() {
		return onUpdated;
	}

	/**
	 * @param onUpdated the onUpdated to set
	 */
	public void setOnUpdated(BaseNukeC onUpdated) {
		this.onUpdated = onUpdated;
	}

	/**
	 * @return the onUpdateReturn
	 */
	public int getOnUpdateReturn() {
		return onUpdateReturn;
	}

	/**
	 * @param onUpdateReturn the onUpdateReturn to set
	 */
	public void setOnUpdateReturn(int onUpdateReturn) {
		this.onUpdateReturn = onUpdateReturn;
	}

	/**
	 * @return the onEvicted
	 */
	public BaseNukeC getOnEvicted() {
		return onEvicted;
	}

	/**
	 * @param onEvicted the onEvicted to set
	 */
	public void setOnEvicted(BaseNukeC onEvicted) {
		this.onEvicted = onEvicted;
	}

	/**
	 * @return the onEvictedReturn
	 */
	public int getOnEvictedReturn() {
		return onEvictedReturn;
	}

	/**
	 * @param onEvictedReturn the onEvictedReturn to set
	 */
	public void setOnEvictedReturn(int onEvictedReturn) {
		this.onEvictedReturn = onEvictedReturn;
	}

	/**
	 * @return the onRemoved
	 */
	public BaseNukeC getOnRemoved() {
		return onRemoved;
	}

	/**
	 * @param onRemoved the onRemoved to set
	 */
	public void setOnRemoved(BaseNukeC onRemoved) {
		this.onRemoved = onRemoved;
	}

	/**
	 * @return the onRemovedReturn
	 */
	public int getOnRemovedReturn() {
		return onRemovedReturn;
	}

	/**
	 * @param onRemovedReturn the onRemovedReturn to set
	 */
	public void setOnRemovedReturn(int onRemovedReturn) {
		this.onRemovedReturn = onRemovedReturn;
	}

}