package edu.cornell.ncrn.ced2ar;

import java.io.IOException;
import java.util.List;

import edu.cornell.ncrn.ced2ar.impl.DtaHeader;
import edu.cornell.ncrn.ced2ar.impl.DtaVariable;

public interface StataReader {
	
	/**
	 * Returns a list of observations as CSV formatted String
	 * Can potentially memory intensive
	 * @return
	 * @throws IOException
	 */
	public List<List<String>> getObservations() throws IOException;
	
	/**
	 * Returns an observation as a CSV formatted String
	 * Returns empty string if the observation number is invalid  
	 * @param observationNumber
	 * @return
	 * @throws IOException
	 */
	public List<String> getObservation(long observationNumber) throws IOException;
	
	/**
	 * Returns a list of observations as CSV formatted String
	 * 
	 * @param start Start number of the  observation
	 * @param end End number of the observation
	 * @return
	 * @throws IOException
	 */
	public List<List<String>> getObservations(long start,long end) throws IOException;
	
	/**
	 * Opens the STATA file 
	 * @throws IOException
	 */
	public void openDtaFile() throws IOException;
	
	/**
	 * Closes the STATA file
	 * @throws IOException
	 */
	public void closeDtaFile()throws IOException;
	
	/**
	 * Returns the header information of the STATA file
	 * @return
	 */
	public DtaHeader getDtaHeader();
	
	/**
	 * Returns a list of STATA variables
	 * @return
	 */
	public List<DtaVariable> getDtaVariables();
	
	/**
	 * Dumps the observations to a log file or console depending upon the log4j configuration 
	 * @throws IOException
	 */
	public void dumpData()throws IOException;
	
}
