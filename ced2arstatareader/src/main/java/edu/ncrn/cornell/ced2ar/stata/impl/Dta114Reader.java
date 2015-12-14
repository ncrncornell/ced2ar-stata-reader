package edu.ncrn.cornell.ced2ar.stata.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ncrn.cornell.ced2ar.stata.StataReader;
import edu.ncrn.cornell.ced2ar.stata.exceptions.InvalidDtaFormatException;

/**
* This class reads STATA data file of format 114. 
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

public class Dta114Reader extends Dta113Reader implements StataReader {
	private static final Logger logger = Logger.getLogger(Dta114Reader.class);
	
	/**
	 * This method opens and reads meta data of the STATA data File.
	 * Makes the data file read for reading of data
	 * @param stataFile
	 * @throws IOException
	 */
	public Dta114Reader(String stataFile) throws IOException,InvalidDtaFormatException {
		super(stataFile);
	}

	/**
	 * This method ensures that the data file is in format 114. Throws InvalidDtaFormatException if not.
	 * @throws InvalidDtaFormatException
	 */
	protected void isValidFormat() throws InvalidDtaFormatException{
		if( dtaHeader.getDtaFileFormat() != 114)
			throw new InvalidDtaFormatException("This reader can only read format 114");
	}

	/**
	 * This method assumes that the file pointer is properly positioned.
	 * ie. Header, Type and variable records are read and the file pointer is moved past sortlist record
	 * This method reads the format record. 
	 * Each variable consists of a format descriptor which is 49 bytes long
	 * @throws IOException
	 */
	protected void readFormatRecord() throws IOException{
		int sizeOfFormatRecord = 49;
		byte[] b = new byte[(dtaHeader.getNumberOfVariables())*sizeOfFormatRecord];
		stataDataRAF.read(b);
		int variableFormatStart=0;
		for(DtaVariable dtaVariable: dtaVariables){
			String variableFormat =getStringValue(Arrays.copyOfRange(b, variableFormatStart, (variableFormatStart+49)));
			variableFormatStart+=sizeOfFormatRecord;
			dtaVariable.setVariableFormat(variableFormat);
		}
	}
}
