package io.github.scrier.opus.duke.commander.state;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.scrier.opus.common.Shared;
import io.github.scrier.opus.common.aoc.BaseNukeC;
import io.github.scrier.opus.duke.commander.ClusterDistributorProcedure;

/**
 * State handling for Peak Delay transactions.
 * @author andreas.joelsson
 * {@code
 * PEAK_DELAY -> ABORTED
 * PEAK_DELAY -> RAMPING_DOWN
 * }
 */
public class PeakDelay extends State {

	private final Logger log = LogManager.getLogger("io.github.scrier.opus.duke.commander.ClusterDistributorProcedure.PeakDelay");
	
	public PeakDelay(ClusterDistributorProcedure parent) {
	  super(parent);
  }
	
	/**
	 * PeakDelay handling on init methods.
	 */
	@Override
	public void init() {
		log.trace("init()");
		log.info("We have reached peak and we stay idle for " + Shared.Methods.formatTime(getPeakDelaySeconds()) + " before ramping down.");
		startTimeout(getPeakDelaySeconds(), getTimerID());
	}

	/**
	 * PeakDelay handling on update methods.
	 * @param data BaseNukeC
	 */
	@Override
	public void updated(BaseNukeC data)  {
		log.trace("updated(" + data + ")");
		assertState();
	}  

	/**
	 * PeakDelay handling on evicted methods.
	 * @param data BaseNukeC
	 */
	@Override
	public void evicted(BaseNukeC data) {
		log.trace("evicted(" + data + ")");
		assertState();
	}

	/**
	 * PeakDelay handling on removed methods.
	 * @param key Long:
	 */
	@Override
	public void removed(Long key) {
		log.trace("removed(" + key + ")");
		assertState();
	}

	/**
	 * PeakDelay handling on timeout methods.
	 * @param id long
	 */
	@Override
	public void timeout(long id) {
		log.trace("timeout(" + id + ")");
		assertState();
		if( id == getTimerID() ) {
			handleTimerTick();
		} else if ( id == getTerminateID() ) {
			log.error("Received terminate timeout during state PEAK_DELAY.");
			setState(TERMINATING);
		} else {
			log.fatal("Received unknown timer id: " + id + " in state PEAK_DELAY.");
			throw new RuntimeException("Received unknown timer id: " + id + " in state PEAK_DELAY.");
		}
	}
	
	/**
	 * Method to handle next timer tick to create new instances of commands to execute.
	 */
	private void handleTimerTick() {
		log.trace("handleTimerTick()");
		setState(RAMPING_DOWN);
	}
	
	 /**
   * Method to assure that we are called in the correct state.
   */
  private void assertState() {
  	if( PEAK_DELAY != getState() ) {
			log.error("Called state PEAK_DELAY(" + PEAK_DELAY + "), when in state " + getState() + ".");
			throw new RuntimeException("Called state PEAK_DELAY(" + PEAK_DELAY + "), when in state " + getState() + ".");
		} 
  }

}
