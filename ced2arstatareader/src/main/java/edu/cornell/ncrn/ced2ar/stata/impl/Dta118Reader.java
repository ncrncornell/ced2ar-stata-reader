package edu.cornell.ncrn.ced2ar.stata.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cornell.ncrn.ced2ar.stata.StataReader;
import edu.cornell.ncrn.ced2ar.stata.exceptions.InvalidDtaFormatException;

/**
 
* strLs not supported  
* This class reads STATA data file of format 118. 
* 
* Differences between Format 117 and format 118
* 1. Number of variables is a 4 byte unsigned int value in 117.  8 byte unsigned int value in 118 
* 2. Lable length is a 1 byte unsigned int value in 117.  2 byte unsigned int value in 118
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

public class Dta118Reader extends Dta117Reader implements StataReader{
	
	private static final Logger logger = Logger.getLogger(Dta118Reader.class);
	
	public Dta118Reader(String stataFile) throws IOException,InvalidDtaFormatException{
		super(stataFile);
	}
	/**
	 * @throws InvalidDtaFormatException
	 */
	protected void isValidFormat() throws InvalidDtaFormatException{
		if( dtaHeader.getDtaFileFormat() != 118)
			throw new InvalidDtaFormatException("This reader can only read format 118 Stata Files.");
	}

	/**
	 * Reads the header record of the stata file.
	 * File pointer is assumed to be at the start location of Header.
	 * After successfule reading of header File position will 
	 * be at the start of <map> (Next section of the dta file)
	 *  
	 *  Number of observations in format 118 is 8 byte unsigned int
	 *  Number of observations in format 117 is 4 byte unsigned int
	 * Format 118 header
	      <stata_dta> 
	   		<header>
                <release>117</release>
                <byteorder>MSF</byteorder>
                <K>0002</K>
                <N>00000001</N>
                <label>00</label>
                <timestamp>1110 Jul 2013 14:23</timestamp>
            </header>
	 * @throws IOException 
	 */

	protected void readHeader() throws IOException{
		DtaHeader dtaHeader = new DtaHeader();
		//Read File Format
		int startPositionOfFileFormat = "<stata_dta><header><release>".length();
		stataDataRAF.seek(startPositionOfFileFormat+stataDataRAF.getFilePointer()); 
		byte b[] = new byte[3];
		stataDataRAF.read(b);
		String fileFormatId = getStringValue(b);
		logger.debug("File format Id = " +fileFormatId);
		dtaHeader.setDtaFileFormat(Byte.parseByte(fileFormatId));
		
		//Read Byte Order
		int startPositionOfByteOrder= "</release><byteorder>".length();
		stataDataRAF.seek(startPositionOfByteOrder+stataDataRAF.getFilePointer()); 
		b = new byte[3];
		stataDataRAF.read(b);
		String byteOrder = getStringValue(b);
		logger.debug("byteOrder = " +byteOrder);
		if(byteOrder.equals("LSF")){
			dtaHeader.setByteOrder((byte)2);
		}
		else if(byteOrder.equals("MSF")){
			dtaHeader.setByteOrder((byte)1);
		}
		else{
			throw new RuntimeException("Unknown byte order: " + byteOrder);
		}
		
		//Read Number of Variables 
		int startPositionOfNumberOfVariables= "</byteorder><K>".length();
		stataDataRAF.seek(startPositionOfNumberOfVariables+stataDataRAF.getFilePointer()); 
		b = new byte[2];
		stataDataRAF.read(b);
		int numberOfVariables = getShortValue(b, dtaHeader.isLittleEndian());
		dtaHeader.setNumberOfVariables(numberOfVariables);		
		logger.debug("numberOfVariables = " +numberOfVariables);
		

		//Read Number of Variables 
		int startPositionOfNumberOfObservations= "</K><N>".length();
		stataDataRAF.seek(startPositionOfNumberOfObservations+stataDataRAF.getFilePointer()); 
		b = new byte[8];
		stataDataRAF.read(b);
		long numberOfObservations = getUnsignedLongValue(b, dtaHeader.isLittleEndian());
		dtaHeader.setNumberOfObservations(numberOfObservations);		
		logger.debug("numberOfObservations = " +numberOfObservations);

		//Read Data Label
		int startPositionOfDataLabel= "</N><label>".length();
		stataDataRAF.seek(startPositionOfDataLabel+stataDataRAF.getFilePointer()); 
		b = new byte[2];
		stataDataRAF.read(b);
		int sizeOfLabel = getUnsignedIntValue(b, dtaHeader.isLittleEndian());
		b = new byte[sizeOfLabel];
		stataDataRAF.read(b);
		String label = getStringValue(b);
		dtaHeader.setDataLabel(label);		
		logger.debug("Data Label = " +label);
		

		//Read Timestamp
		int startPositionOfTimestamp= "</label><timestamp>".length();
		stataDataRAF.seek(startPositionOfTimestamp+stataDataRAF.getFilePointer()); 
		b = new byte[1];
		stataDataRAF.read(b);
		int sizeOfTimestamp = getUnsignedIntValue(b, dtaHeader.isLittleEndian());
		b = new byte[sizeOfTimestamp];
		stataDataRAF.read(b);
		String timeStamp = getStringValue(b);
		dtaHeader.setTimeStamp(timeStamp);		
		logger.debug("timeStamp = " +timeStamp);
		
		setDtaHeader(dtaHeader);
		logger.debug("dtaHeader = " +dtaHeader);

		int startPositionOfMap= "</timestamp></header>".length();
		stataDataRAF.seek(startPositionOfMap+stataDataRAF.getFilePointer());
	}
	
	/**
	 * This method reads variable information. 
	 * Variable information includes
	 * variable name
	 * variable type
	 * variable format
	 * variable label
	 * 
	 * @throws IOException
	 */
	protected void readVariables() throws IOException{
		List<DtaVariable> dtaVariables = new ArrayList<DtaVariable>();
		stataDataRAF.seek(startOfVariableTypesSection+"<variable_types>".length());
		
		// Read Variable Type information
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			DtaVariable dtaVariable = new DtaVariable();
			dtaVariable.setDta117DataType(true);
			byte[] b = new byte[2];
			stataDataRAF.read(b);
			dtaVariable.setVariableType((getUnsignedIntValue(b, getDtaHeader().isLittleEndian())));
			dtaVariables.add(dtaVariable);
			
		}
		
		// Read Variable name information
		stataDataRAF.seek(startOfVarNamesSection+"<varnames>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[129];
			stataDataRAF.read(b);
			dtaVariables.get(i).setName(getStringValue(b));
		}
		
		// Read Variable Format information
		stataDataRAF.seek(startOfFormatsSection+"<formats>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[57];
			stataDataRAF.read(b);
			dtaVariables.get(i).setVariableFormat(getStringValue(b));
		}
		
		// Read Value Label names information
		stataDataRAF.seek(startOfValueLabelNamesSection+"<value_label_names>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[129];
			stataDataRAF.read(b);
			dtaVariables.get(i).setVariableValueLabelName(getStringValue(b));
		}

		
		// Read Variable Label information
		stataDataRAF.seek(startOfVariableLablesSection+"<variable_labels>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[321];
			stataDataRAF.read(b);
			dtaVariables.get(i).setVariableLabel(getStringValue(b));
		}

		setDtaVariables(dtaVariables);

		
		// Read Value Labels add them to the variables
		stataDataRAF.seek(startOfValueLabelsSection+"<value_labels><lbl>".length());
		readValueLabels();
		// for debugging
		for(DtaVariable dtaVariable:dtaVariables){
			logger.debug(dtaVariable);
		}
	}

	

}
