package edu.ncrn.cornell.ced2ar.stata.impl;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.ncrn.cornell.ced2ar.stata.StataReader;
import edu.ncrn.cornell.ced2ar.stata.exceptions.InvalidDtaFormatException;

/**
* This class reads STATA data file of format 115. 
* 
*
	Stata versions and file formats
        Format        Current as of
        ---------------------------------------
          118         Stata 14 http://www.stata.com/help.cgi?dta
          117         Stata 13 http://www.stata.com/help.cgi?dta
          116         internal; never released
          115         Stata 12  http://www.stata.com/help.cgi?dta_115
          114         Stata 10 & 11  http://www.stata.com/help.cgi?dta_114
          113         Stata  8 & 9  http://www.stata.com/help.cgi?dta_113
        --------------------------------------
        
* USAGE 
* 	StataFile stataFile = new StataFile("C:\\java\\info\\Data\\STATA\\IMFFinReform.dta");
	stataFile.getObservation(10);
* 
*@author Cornell University, Copyright 2012-2015
*@author Venky Kambhampaty
*
*@author Cornell Institute for Social and Economic Research
*@author Cornell Labor Dynamics Institute
*@author NCRN Project Team 
*/

public class Dta115Reader extends Dta114Reader implements StataReader {
	private static final Logger logger = Logger.getLogger(Dta115Reader.class);
	
	/**
	 * This method opens and reads meta data of the STATA data File.
	 * Makes the data file read for reading of data
	 * @param stataFile
	 * @throws IOException
	 */
	public Dta115Reader(String stataFile) throws IOException,InvalidDtaFormatException {
		super(stataFile);
	}

	/**
	 * This method ensures that the data file is in format 115. Throws InvalidDtaFormatException if not.
	 * @throws InvalidDtaFormatException
	 */
	protected void isValidFormat() throws InvalidDtaFormatException{
		if( dtaHeader.getDtaFileFormat() != 115)
			throw new InvalidDtaFormatException("This reader can only read format 115");
	}
}
