package edu.cornell.ncrn.ced2ar.stata.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cornell.ncrn.ced2ar.stata.StataReader;
import edu.cornell.ncrn.ced2ar.stata.exceptions.InvalidDtaFormatException;

/**
* This class reads STATA data file of format 117. 
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

public class Dta117Reader extends DtaReader implements StataReader{
	private static final Logger logger = Logger.getLogger(Dta117Reader.class);
	
	protected long startOfStataDataSection;
	protected long startOfMapSection;
	protected long startOfVariableTypesSection;
	protected long startOfVarNamesSection;
	protected long startOfSortListSection;
	protected long startOfFormatsSection;
	protected long startOfValueLabelNamesSection;
	protected long startOfVariableLablesSection;
	protected long startOfCharacteristicsSection;
	protected long startOfDataSection;
	protected long startOfStrlsSection;
	protected long startOfValueLabelsSection;
	protected long startOfEndStataDataSection;
	protected long endOfFile;
	
	
	public Dta117Reader(String stataFile) throws IOException,InvalidDtaFormatException{
		setDataFile(stataFile);
		try{
			openDtaFile();
			readHeader();
			isValidFormat();
			readMap();
			readVariables();
		}
		finally{
			closeDtaFile();
		}
	}

	@Override
	protected void move2ObservationStart(long observationNumber) throws IOException{
		stataDataRAF.seek(	startOfDataSection+"<data>".length() + 
				getObservationLength() * (observationNumber-1));
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
			//Variable type is a 2 byte unsigned integer.
			/*
			byte[] bytes = new byte[4];
			bytes[0]=b[0];
			bytes[1]=b[1];
			dtaVariable.setVariableType((getUnsignedIntValue(bytes, getDtaHeader().isLittleEndian())));
			*/
			dtaVariable.setVariableType((getUnsignedIntValue(b, getDtaHeader().isLittleEndian())));
			dtaVariables.add(dtaVariable);
			
		}
		
		// Read Variable name information
		stataDataRAF.seek(startOfVarNamesSection+"<varnames>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[33];
			stataDataRAF.read(b);
			dtaVariables.get(i).setName(getStringValue(b));
		}
		
		// Read Variable Format information
		stataDataRAF.seek(startOfFormatsSection+"<formats>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[49];
			stataDataRAF.read(b);
			dtaVariables.get(i).setVariableFormat(getStringValue(b));
		}
		

		// Read Value Label names information
		stataDataRAF.seek(startOfValueLabelNamesSection+"<value_label_names>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[33];
			stataDataRAF.read(b);
			dtaVariables.get(i).setVariableValueLabelName(getStringValue(b));
		}

		
		// Read Variable Label information
		stataDataRAF.seek(startOfVariableLablesSection+"<variable_labels>".length());
		for(int i=0;i<getDtaHeader().getNumberOfVariables();i++){
			byte[] b = new byte[81];
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

	/**
	 *  Reads the map of the data file.  Map of a dta117 file points for 
	 *  14 distinct section start locations. This method populates these 
	 *  locations as instance variables
	 *  
                 #        file position of the start of the
                -----------------------------------------------
                 1.       <stata_data>, definitionally 0
                 2.       <map>
                 3.       <variable_types>
                 4.       <varnames>
                 5.       <sortlist>
                 6.       <formats>
                 7.       <value_label_names>
                 8.       <variable_labels>
                 9.       <characteristics>
                10.       <data>
                11.       <strls>
                12.       <value_labels>
                13.       </stata_data>
                14.       end-of-file
                -----------------------------------------------
	 *  
	 */
	protected void readMap() throws IOException{
		int startPositionOfFilePositions= "<map>".length();
		stataDataRAF.seek(startPositionOfFilePositions+stataDataRAF.getFilePointer());
		
		
		byte b[] = new byte[8];
		stataDataRAF.read(b);
		startOfStataDataSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <stata_data> " + startOfStataDataSection );
		
		b = new byte[8];
		stataDataRAF.read(b);
		startOfMapSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <map> " + startOfMapSection);
		
		b = new byte[8];
		stataDataRAF.read(b);
		startOfVariableTypesSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <variable_types> " + startOfVariableTypesSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfVarNamesSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <varnames> " + startOfVarNamesSection);


		b = new byte[8];
		stataDataRAF.read(b);
		startOfSortListSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <sortlist> " + startOfSortListSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfFormatsSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <formats> " + startOfFormatsSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfValueLabelNamesSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <value_label_names> " + startOfValueLabelNamesSection);
		
		b = new byte[8];
		stataDataRAF.read(b);
		startOfVariableLablesSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <variable_labels> " + startOfVariableLablesSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfCharacteristicsSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <characteristics> " + startOfCharacteristicsSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfDataSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <data> " + startOfDataSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfStrlsSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <strls> " + startOfStrlsSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfValueLabelsSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of <value_labels> " + startOfValueLabelsSection);

		b = new byte[8];
		stataDataRAF.read(b);
		startOfEndStataDataSection = getLongValue(b,this.dtaHeader.isLittleEndian());
		logger.debug("Start of </stata_data> " + startOfEndStataDataSection);

		b = new byte[8];
		stataDataRAF.read(b);
		endOfFile = getLongValue(b,dtaHeader.isLittleEndian());
		logger.debug("end of File " + endOfFile);
	}
	
	
	/**
	 * Reads the header record of the stata file.
	 * File pointer is assumed to be at the start location of Header.
	 * After successfule reading of header File position will 
	 * be at the start of <map> (Next section of the dta file) 
	 * Format 117 header
	      <stata_dta> 
	   		<header>
                <release>117</release>
                <byteorder>MSF</byteorder>
                <K>0002</K>
                <N>0001</N>
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
		b = new byte[4];
		stataDataRAF.read(b);
		int numberOfObservations = getUnsignedIntValue(b, dtaHeader.isLittleEndian());
		dtaHeader.setNumberOfObservations(numberOfObservations);		
		logger.debug("numberOfObservations = " +numberOfObservations);

		//Read Data Label
		int startPositionOfDataLabel= "</N><label>".length();
		stataDataRAF.seek(startPositionOfDataLabel+stataDataRAF.getFilePointer()); 
		b = new byte[1];
		stataDataRAF.read(b);
		//int sizeOfLabel = getByteValue(b[0], dtaHeader.isLittleEndian());
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
		//int sizeOfTimestamp = getByteValue(b[0], dtaHeader.isLittleEndian());
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
	 * @throws InvalidDtaFormatException
	 */
	protected void isValidFormat() throws InvalidDtaFormatException{
		if( dtaHeader.getDtaFileFormat() != 117)
			throw new InvalidDtaFormatException("This reader can only read format 117 Stata Files.");
	}
	
	// getter and setter
	public long getStartOfStataDataSection() {
		return startOfStataDataSection;
	}

	public void setStartOfStataDataSection(long startOfStataDataSection) {
		this.startOfStataDataSection = startOfStataDataSection;
	}

	public long getStartOfMapSection() {
		return startOfMapSection;
	}

	public void setStartOfMapSection(long startOfMapSection) {
		this.startOfMapSection = startOfMapSection;
	}

	public long getStartOfVariableTypesSection() {
		return startOfVariableTypesSection;
	}

	public void setStartOfVariableTypesSection(long startOfVariableTypesSection) {
		this.startOfVariableTypesSection = startOfVariableTypesSection;
	}

	public long getStartOfVarNamesSection() {
		return startOfVarNamesSection;
	}

	public void setStartOfVarNamesSection(long startOfVarNamesSection) {
		this.startOfVarNamesSection = startOfVarNamesSection;
	}

	public long getStartOfSortListSection() {
		return startOfSortListSection;
	}

	public void setStartOfSortListSection(long startOfSortListSection) {
		this.startOfSortListSection = startOfSortListSection;
	}

	public long getStartOfFormatsSection() {
		return startOfFormatsSection;
	}

	public void setStartOfFormatsSection(long startOfFormatsSection) {
		this.startOfFormatsSection = startOfFormatsSection;
	}

	public long getStartOfValueLabelNamesSection() {
		return startOfValueLabelNamesSection;
	}

	public void setStartOfValueLabelNamesSection(long startOfValueLabelNamesSection) {
		this.startOfValueLabelNamesSection = startOfValueLabelNamesSection;
	}

	public long getStartOfVariableLablesSection() {
		return startOfVariableLablesSection;
	}

	public void setStartOfVariableLablesSection(long startOfVariableLablesSection) {
		this.startOfVariableLablesSection = startOfVariableLablesSection;
	}

	public long getStartOfCharacteristicsSection() {
		return startOfCharacteristicsSection;
	}

	public void setStartOfCharacteristicsSection(long startOfCharacteristicsSection) {
		this.startOfCharacteristicsSection = startOfCharacteristicsSection;
	}

	public long getStartOfDataSection() {
		return startOfDataSection;
	}

	public void setStartOfDataSection(long startOfDataSection) {
		this.startOfDataSection = startOfDataSection;
	}

	public long getStartOfStrlsSection() {
		return startOfStrlsSection;
	}

	public void setStartOfStrlsSection(long startOfStrlsSection) {
		this.startOfStrlsSection = startOfStrlsSection;
	}

	public long getStartOfValueLabelsSection() {
		return startOfValueLabelsSection;
	}

	public void setStartOfValueLabelsSection(long startOfValueLabelsSection) {
		this.startOfValueLabelsSection = startOfValueLabelsSection;
	}

	public long getStartOfEndStataDataSection() {
		return startOfEndStataDataSection;
	}

	public void setStartOfEndStataDataSection(long startOfEndStataDataSection) {
		this.startOfEndStataDataSection = startOfEndStataDataSection;
	}

	public long getEndOfFile() {
		return endOfFile;
	}

	public void setEndOfFile(long endOfFile) {
		this.endOfFile = endOfFile;
	}
}
