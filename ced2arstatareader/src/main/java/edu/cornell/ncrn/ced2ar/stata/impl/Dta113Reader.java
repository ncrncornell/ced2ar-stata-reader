package edu.cornell.ncrn.ced2ar.stata.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cornell.ncrn.ced2ar.stata.StataReader;
import edu.cornell.ncrn.ced2ar.stata.exceptions.InvalidDtaFormatException;

/**
* This class reads STATA data file of format 113. 
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

public class Dta113Reader extends DtaReader implements StataReader {
	private static final Logger logger = Logger.getLogger(Dta115Reader.class);
	

	public enum HeaderRecord{
		HEADER_RECORD(0,108),
		DS_FORMAT(0,1),
		BYTEORDER(1,1),
		FILE_TYPE(2,1),
		UNUSED(3,1),
		NUMBER_OF_VARIABLES(4,2),
		NUMBER_OF_OBSERVATIONS(6,4),
		DATA_LABEL(10,81),
		TIME_STAMP(91,18);
		
		private int start;
		private int size;
		private HeaderRecord(int start, int size){
			this.start = start;
			this.size = size;
		}
	}
	protected long startOfDataRecord;
	protected long startOfValueLabelsRecord;
	protected long dataRecordLength;
	protected long lengthOfFile;
	protected boolean containsValueLabels;  //These are possible valid or invalid value list
	

	/**
	 * This method opens and reads meta data of the STATA data File.
	 * Makes the data file ready for reading of data
	 * @param stataFile
	 * @throws IOException
	 */
	public Dta113Reader(String stataFile) throws IOException,InvalidDtaFormatException {
		setDataFile(stataFile);
		try{
			openDtaFile();
			readHeader();
			isValidFormat();
			readDtaMetaData();
		}
		finally{
			this.closeDtaFile();
		}
	}

	/**
	 * This method ensures that the data file is in format 113. Throws InvalidDtaFormatException if not.
	 * @throws InvalidDtaFormatException
	 */
	protected void isValidFormat() throws InvalidDtaFormatException{
		if( dtaHeader.getDtaFileFormat() != 113)
			throw new InvalidDtaFormatException("This reader can only read format 113" );
	}

	/**
	 * 	This method reads Meta data 
	 * 	1. Header record please see enumeration HeaderRecord
	 *  2. Type List. Type list is the data type of each variable
	 *  3. Variable List. Name of each variable
	 *  4. Sort List. currently it is being ignored.
	 *  5. Format List.  Formatting of each of the variable
	 *  6. Label List.  Variable Labels
	 *  7. Expansion Field Record.  This record is ignored.
	 *  8. If there are variable value labels, they are read  
	 * @throws IOException
	 */
	protected void readDtaMetaData() throws IOException{
		lengthOfFile = stataDataRAF.length();
		List<DtaVariable> dtaVariables = new ArrayList<DtaVariable>();
		setDtaVariables(dtaVariables);
		readTypeList();
		readVariableList();
		readSortOrderRecord();
		readFormatRecord();
		readVariableValueFormatRecord();
		readVariableLabelRecord();
		readExpansionFieldRecord();

		startOfDataRecord = stataDataRAF.getFilePointer();
		dataRecordLength = getObservationLength();
		startOfValueLabelsRecord = startOfDataRecord +  (dtaHeader.getNumberOfObservations()*dataRecordLength);
		containsValueLabels = (startOfValueLabelsRecord<this.lengthOfFile);
		logger.info("Contains Value Labels: " + containsValueLabels);
		
		if(containsValueLabels){
			stataDataRAF.seek(startOfValueLabelsRecord); 
			readValueLabels();
		}
		for(DtaVariable dtaVariable:dtaVariables){
			logger.debug(dtaVariable);
		}
	}

	
	/**
	 * This method moves the file pointer to the start of the observation number
	 * Internal use only.
	 */
	protected void move2ObservationStart(long observationNumber) throws IOException{
		stataDataRAF.seek(startOfDataRecord +  ((observationNumber-1)*dataRecordLength));
	}

	/**
	 * Expansion Field record is not important for this program and for most programs.
	 * Expansion field is of the size of 5 bytes. 
	 * You read a 5 byte chunk; last four of which less your expansion fields length.  
	 * After that read next 5 bytes chuck until you have all 5 bytes are zero.
	 * From Stata documentation....
	 *  Expansion fields conclude with code 0 and len 0; before the termination
     *	marker, there may be no or many separate data blocks.  Expansion fields
     *	are used to record information that is unique to Stata and has no
     *	equivalent in other data management packages.  Expansion fields are
     *	always optional when writing data and, generally, programs reading Stata
     *	datasets will want to ignore the expansion fields.  The format makes this
     *	easy.  When writing, write 5 bytes of zeros for this field.  When
     *	reading, read five bytes; the last four bytes now tell you the size of
     *	the next read, which you discard.  You then continue like this until you
     *	read 5 bytes of zeros
	 * 
	 * @throws IOException
	 */
	protected void readExpansionFieldRecord() throws IOException{
		byte[] b;
		while(true){
			b = new byte[5];
			stataDataRAF.read(b);
			byte[] lastFour = Arrays.copyOfRange(b,1,5);
			int expansionFieldLength = getIntValue(lastFour, dtaHeader.isLittleEndian());
			if(expansionFieldLength ==0) 
				break;
			else{
				b = new byte[expansionFieldLength];
				stataDataRAF.read(b);// read content and ignore
			}
		}
		
	}
	
	/**
	 * This method read variable  labels record 
	 * Each variable label is 81 bytes long
	 * This method assumes that the file pointer is properly positioned.
	 * @throws IOException
	 */
	protected void readVariableLabelRecord() throws IOException{
		byte[] b = new byte[(dtaHeader.getNumberOfVariables())*81];
		stataDataRAF.read(b);
		int variableLabelStart=0;
		for(DtaVariable dtaVariable: dtaVariables){
			String variableLabel=getStringValue(Arrays.copyOfRange(b, variableLabelStart, (variableLabelStart+81)));
			variableLabelStart+=81;		
			dtaVariable.setVariableLabel(variableLabel);
		}
	}

	/**
	 * This method read variable value labels record (lbllist)
	 * This record conatins value labels such as yesno for the values of the variable
	 * This method assumes that the file pointer is properly positioned.
	 * @throws IOException
	 */
	protected void readVariableValueFormatRecord() throws IOException{
		byte[] b = new byte[(dtaHeader.getNumberOfVariables())*33];
		stataDataRAF.read(b);
		int variableValueLabelNameStart=0;
		for(DtaVariable dtaVariable: dtaVariables){
			String variableValueLabelName=getStringValue(Arrays.copyOfRange(b, variableValueLabelNameStart, (variableValueLabelNameStart+33)));
			variableValueLabelNameStart+=33;		
			dtaVariable.setVariableValueLabelName(variableValueLabelName);
		}
	}

	/**
	 * This method assumes that the file pointer is properly positioned.
	 * ie. Header, Type and variable records are read and the file pointer is moved past sortlist record
	 * This method reads the format record. 
	 * Each variable consists of a format descriptor which is 12 bytes long
	 * @throws IOException
	 */
	protected void readFormatRecord() throws IOException{
		int sizeOfFormatRecord = 12;
		byte[] b = new byte[(dtaHeader.getNumberOfVariables())*sizeOfFormatRecord];
		stataDataRAF.read(b);
		int variableFormatStart=0;
		for(DtaVariable dtaVariable: dtaVariables){
			String variableFormat =getStringValue(Arrays.copyOfRange(b, variableFormatStart, (variableFormatStart+49)));
			variableFormatStart+=sizeOfFormatRecord;
			dtaVariable.setVariableFormat(variableFormat);
		}
	}

	/**
	 * This method moves the file pointer past sortlist record.  
	 * We are not keeping track of the sort order.
	 * SortOrder record consists of 2*(numberOfVariables+1);
	 * This method assumes that the file pointer is properly positioned.
	 * ie. Header, Type and variable records are read.
 	 */
	protected void readSortOrderRecord() throws IOException{
		byte[] b = new byte[(dtaHeader.getNumberOfVariables()+1)*2];
		stataDataRAF.read(b);
		
	}

	/**
	 * This method reads the variable list record from Stata Data File
	 * This list contains the names of the variables.
	 * Each variable record list is 33 bytes long.  
	 * This method assumes that the file pointer is properly positioned.
	 * ie. Header and Type records are read.
	 * @throws IOException
	 */
	protected void readVariableList() throws IOException{
		byte[] b = new byte[dtaHeader.getNumberOfVariables()*33];
		stataDataRAF.read(b);
		int variableNameStart=0;
		for(DtaVariable dtaVariable: dtaVariables){
			String variableName =getStringValue(Arrays.copyOfRange(b, variableNameStart, (variableNameStart +33)));
			variableNameStart+=33;		
			dtaVariable.setName(variableName);
		}
	}
	
	/**
	 * This method reads the typelist record from the stata data file.
	 * It is assumed that header record is already read and file pointer is 
	 * at proper location. (ie. this method should be called after header is read)
	 * Type list describes the data type of each of the variables.
	 * Type list is stored as a byte array of numberOfVariables size.
	 * Each element of the byte array describes the variable type.
	 * Elements of the array are encoded as below.
     *   str1        1 = 0x01
     *   str2        2 = 0x02
     *   ...
     *   str244    244 = 0xf4
     *   byte      251 = 0xfb  
     *   int       252 = 0xfc
     *   long      253 = 0xfd
     *   float     254 = 0xfe
     *   double    255 = 0xff
	 * @param stataDataRAF
	 * @throws IOException
	 */
	protected void readTypeList() throws IOException{
		byte[] b = new byte[dtaHeader.getNumberOfVariables()];
		stataDataRAF.read(b);
		int variableType = 0;
		for(int i=0;i<b.length;i++){
			variableType = getByteValue(b[i],dtaHeader.isLittleEndian());
			if((variableType>0 && variableType<=244) || (variableType>=251 && variableType<=255)){
				DtaVariable dtaVariable = new DtaVariable();
				dtaVariable.setVariableType(variableType);
				dtaVariables.add(dtaVariable);
			}
			else{
				throw new RuntimeException("Invalid variable Type. Variable types must be betwee 1 and 255.  Variable type found is  "  +variableType);
			}
		}
	}
	
	/**
	 * Reads the header record of the stata file.
	 * File pointer is assumed to be at the start location.
	 * 
	 * @param stataDataRAF
	 * @return Header 
	 * @throws IOException 
	 */
	protected void readHeader() throws IOException{
		DtaHeader dtaHeader = new DtaHeader();

		dtaHeader.setDtaFileFormat(stataDataRAF.readByte());
		dtaHeader.setByteOrder(stataDataRAF.readByte());
		stataDataRAF.readByte();// File type is read and discorded
		stataDataRAF.readByte();// Unused byte is read and discorded.
		
		
		byte[] numberOfVariablesBytes = new byte[HeaderRecord.NUMBER_OF_VARIABLES.size];
		stataDataRAF.read(numberOfVariablesBytes);
		int numberOfVariables = getShortValue(numberOfVariablesBytes,dtaHeader.isLittleEndian());
		dtaHeader.setNumberOfVariables(numberOfVariables);
		
		byte[] numberOfObservationsBytes = new byte[HeaderRecord.NUMBER_OF_OBSERVATIONS.size];
		stataDataRAF.read(numberOfObservationsBytes);
		int numberOfObservations = getIntValue(numberOfObservationsBytes,dtaHeader.isLittleEndian());
		dtaHeader.setNumberOfObservations(numberOfObservations);
		
		byte[] dataLabelBytes = new byte[HeaderRecord.DATA_LABEL.size];
		stataDataRAF.read(dataLabelBytes);
		String dataLabelString =  getStringValue(dataLabelBytes);
		dtaHeader.setDataLabel(dataLabelString);
		

		byte[] timestampBytes = new byte[HeaderRecord.TIME_STAMP.size];
		stataDataRAF.read(timestampBytes);
		String timestampString =  getStringValue(timestampBytes);
		dtaHeader.setTimeStamp(timestampString);

		setDtaHeader(dtaHeader);
		logger.debug("dtaHeader = " +dtaHeader);

	}
}
