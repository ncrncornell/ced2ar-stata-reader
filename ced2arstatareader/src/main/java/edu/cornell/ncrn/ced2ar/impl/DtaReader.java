package edu.cornell.ncrn.ced2ar.impl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cornell.ncrn.ced2ar.exceptions.InvalidDtaFormatException;

/**
* This class is base class for stata readers. 
* This class provides some basic methods to read stata data file
* Stata data files are formatted differently for different versions of the stata
* Stata data file that confirms to format 113, 114, 115 and 117
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
        
*@author Cornell University, Copyright 2012-2015
*@author Venky Kambhampaty
*
*@author Cornell Institute for Social and Economic Research
*@author Cornell Labor Dynamics Institute
*@author NCRN Project Team 
*/


public  abstract class DtaReader{
	private static final Logger logger = Logger.getLogger(DtaReader.class);
	
	public enum VALID_BYTE_VALUES{
		MIN_VALUE((byte)-127),
		MAX_VALUE((byte)100);
		private byte value;
		private VALID_BYTE_VALUES(byte value){
			this.value = value;
		}
		public byte getValue(){
			return value;
		}
	}
	public enum VALID_INT_VALUES{
		MIN_VALUE(-32767),
		MAX_VALUE(32740);
		private int value;
		private VALID_INT_VALUES(int value){
			this.value = value;
		}
		public int getValue(){
			return value;
		}

	}
	public enum VALID_LONG_VALUES{
		MIN_VALUE(-2147483647),
		MAX_VALUE(2147483620);
		private long value;
		private VALID_LONG_VALUES(long value){
			this.value = value;
		}
		public long getValue(){
			return value;
		}

	}
	public enum VALID_FLOAT_VALUES{
		MIN_VALUE(-1.701e+38f),
		MAX_VALUE(+1.701e+38f);
		private float value;
		private VALID_FLOAT_VALUES(float value){
			this.value = value;
		}
		public float getValue(){
			return value;
		}

	}
 
	public enum MISSING_DOUBLE_VALUES{
		MIN_VALUE((+8.988e+307)+1),
		MAX_VALUE((+8.988e+307)+27);
		private double value;
		private MISSING_DOUBLE_VALUES(double value){
			this.value = value;
		}
		public double getValue(){
			return value;
		}
	}

	protected String dataFile;
	protected RandomAccessFile stataDataRAF;
	protected DtaHeader dtaHeader;
	protected List<DtaVariable> dtaVariables;
	
	/**
	 * This method opens STATA data file 
	 * @throws IOException
	 */
	public void openDtaFile() throws IOException{
		try{
			stataDataRAF = new RandomAccessFile(new File(dataFile),"r");
		}
		catch(IOException ex){
			throw ex;
		}
	}
	
	/**
	 * This method Closes STATA data file
	 * @throws IOException
	 */
	public void closeDtaFile() throws IOException{
		if(stataDataRAF !=null) stataDataRAF.close();
	}

	
	/**
	 * This method returns the length of observation by adding  
	 * all the variable data type lengths
	 * @return long 
	 */
	public long getObservationLength(){
		long lengthOfObservation = 0;
		for(DtaVariable dtaVariable:dtaVariables){
			if(dtaVariable.isByte()){
				lengthOfObservation++;
			}
			else if(dtaVariable.isInt()){
				lengthOfObservation+=2;
			}
			else if(dtaVariable.isLong() || dtaVariable.isFloat()){
				lengthOfObservation+=4;
			}
			else if(dtaVariable.isDouble()){
				lengthOfObservation+=8;
			}
			else{
				lengthOfObservation+=dtaVariable.getVariableType();
			}
		}
		return lengthOfObservation;
	}

	/**
	 * This method dumps the data to console (log4j configuration required) in a csv format
	 * Can print large amounts of data
	 * @throws IOException
	 */
	public void dumpData() throws IOException{
		try{
			openDtaFile();
			for(long l=1;l<=dtaHeader.getNumberOfObservations();l++){
				logger.debug(getObservationAsCSV(readObservation(l))+"\n");
			}
		}
		finally{
			this.closeDtaFile();
		}
	}

	/**
	 * Can be memory intensive. Not recommended for huge data sets. 
	 * @return list of all observations as a list of Strings in CSV format
	 * @throws IOException
	 */
	public List<List<String>> getObservations() throws IOException {
		try{
			openDtaFile();
			List<List<String>> observations = new ArrayList<List<String>>();
			for(long l=1;l<=dtaHeader.getNumberOfObservations();l++){
				observations.add(readObservation(l));
			}
			return observations;
		}
		finally{
			closeDtaFile();
		}
	}

	/**
	 * returns one observation as a CSV string.   
	 * @param observationNumber
	 * @return CSV formatted string if the observationNumber is valid. An Empty string otherwise.
	 * @throws IOException
	 */
	public List<String> getObservation(long observationNumber) throws IOException {
		try{
			openDtaFile();
			return readObservation(observationNumber);
		}
		finally{
			closeDtaFile();
		}
	}

	/**
	 * This method is a placeholder to move the file pointer to the start of the data section
	 * of the the stata file.  Data section start calculation can vary between various versions 
	 * of the stata file.
	 *  
	 * @param observationNumber
	 * @throws IOException
	 */
	protected void move2ObservationStart(long observationNumber) throws IOException{
		throw new RuntimeException("This method should have been overridden");
	}
	

	/**
	 * @param observation
	 * @return CSV version of observation
	 * @throws IOException
	 */
	protected String getObservationAsCSV(List<String> observation) throws IOException{
		StringBuilder SB = new StringBuilder("");
		for(String s:observation){
			SB.append(s+",");
		}
		return SB.toString().substring(0,SB.toString().length()-1);
	}
	
	/**
	 * Returns a string containing observation values in CSV format.
	 * Returns an empty string if the observation number is invalid
	 * This method is format specific 
	 * @param observationNumber
	 * @return An Observation in CSV format;
	 * @throws IOException
	 */
	protected List<String> readObservation(long observationNumber) throws IOException{
		List<String> observation = new ArrayList<String>();
		if(observationNumber <=0 || observationNumber > dtaHeader.getNumberOfObservations()){
			return observation;
		}
		else{
			move2ObservationStart(observationNumber);
		}
		
		for(DtaVariable dtaVariable : dtaVariables){
			int variableType = dtaVariable.getVariableType();
			if(dtaVariable.isString()){
				byte b[] = new byte[variableType];
				stataDataRAF.read(b);
				observation.add(getStringValue(b));
			}
			else if(dtaVariable.isByte()){
				byte b[] = new byte[1];
				stataDataRAF.read(b);
				int  intValue =getByteValue(b[0], dtaHeader.isLittleEndian());
				if(intValue >= VALID_BYTE_VALUES.MIN_VALUE.value && intValue <= VALID_BYTE_VALUES.MAX_VALUE.value){
					observation.add(""+intValue);
				}
				else{
					observation.add(".");
					//SB.append(".,");
				}
			}
			else if(dtaVariable.isInt()){ // two byte stata-integer is a short in java
				byte b[] = new byte[2];
				stataDataRAF.read(b);
				int  intValue =getShortValue(b, dtaHeader.isLittleEndian());
				if(intValue >= VALID_INT_VALUES.MIN_VALUE.value && intValue <= VALID_INT_VALUES.MAX_VALUE.value){
					observation.add(""+intValue);
					//SB.append(intValue+",");	
				}
				else{
					observation.add(".");
					//SB.append(".,");
				}
			}
			else if(dtaVariable.isLong()){ // 4 byte stata-long is an integer in java
				byte b[] = new byte[4];
				stataDataRAF.read(b);
				int  intValue =getIntValue(b, dtaHeader.isLittleEndian());
				if(intValue >= VALID_LONG_VALUES.MIN_VALUE.value && intValue <= VALID_LONG_VALUES.MAX_VALUE.value){
					observation.add(""+intValue);
					//SB.append(intValue+",");	
				}
				else{
					observation.add(".");
					//SB.append(".,");
				}
			}
			else if(dtaVariable.isFloat()){
				byte b[] = new byte[4];
				stataDataRAF.read(b);
				float f;
				if(dtaHeader.isLittleEndian())
					f = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
				else
					f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
				
				if(f >= VALID_FLOAT_VALUES.MIN_VALUE.value && f <= VALID_FLOAT_VALUES.MAX_VALUE.value){
					observation.add(""+f);
					//SB.append(f+",");	
				}
				else{
					observation.add(".");
					//SB.append(".,");
				}
			}
			else if(dtaVariable.isDouble()){
				byte b[] = new byte[8];
				stataDataRAF.read(b);
				double d;
				if(dtaHeader.isLittleEndian())
					d = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble();
				else
					d = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getDouble();

				if(d >= MISSING_DOUBLE_VALUES.MIN_VALUE.value && d <= MISSING_DOUBLE_VALUES.MAX_VALUE.value){
					observation.add(".");
					//SB.append(".,");
				}
				else{
					observation.add(""+d);
					//SB.append(d+",");
				}
			}
			else{
				logger.info("Unable to find the datatype");
				
			}
		}
		return observation;
//		return SB.toString().substring(0,SB.toString().length()-1);
	}

	
	/**
	 * Returns the observations in as a list. 
	 * Each element of the list represents one observation in csv format
	 * @param start Start of the observation Number
	 * @param end End of the observation number.
	 * @return
	 * @throws IOException
	 */
	public List<List<String>> getObservations(long start, long end)throws IOException {
		try{
			openDtaFile();
			List<List<String>> observations = new ArrayList<List<String>>();
			for(long l=start;l<=end;l++){
				observations.add(getObservation(l));
			}
			return observations;
		}
		finally{
			closeDtaFile();
		}
	}

	/**
	 * reads the value lables and adds them to appropriate variable(s)
	 *  each value label is written
    	as
        Contents               len   format     comment
        -------------------------------------------------------------------
        len                      4   int        length of value_label_table
        labname                 33   char       \0 terminated
        padding                  3
        value_label_table      len              see next table
        -------------------------------------------------------------------
    	and this is repeated for each value label included in the file.  The
    	format of the value_label_table is
        Contents               len   format     comment
        ----------------------------------------------------------
        n                        4   int        number of entries
        txtlen                   4   int        length of txt[]
        off[]                  4*n   int array  txt[] offset table
        val[]                  4*n   int array  sorted value table
        txt[]               txtlen   char       text table
        ----------------------------------------------------------
    	len, n, txtlen, off[], and val[] are encoded per byteorder.  The maximum
    	length of txt[] for a label is 32,000 characters.  Stata is robust to
    	datasets which might contain labels longer than this; labels which exceed
    	the limit, if any, will be dropped during a use.
	 * @throws IOException
	 */
	protected void readValueLabels() throws IOException{
		logger.debug("Start of Value Label Section: " + stataDataRAF.getFilePointer());
		while(true){
			byte b[] = new byte[4];
			stataDataRAF.read(b);
			int lengthOfValueTable = getIntValue(b, dtaHeader.isLittleEndian());
			
			b = new byte[33];
			stataDataRAF.read(b);
			String valueLabelName = getStringValue(b);
	
			
			b = new byte[3];
			stataDataRAF.read(b);
			String padding = getStringValue(b);
	
			
			b = new byte[4];
			stataDataRAF.read(b);
			int numberOfEntries = getIntValue(b, dtaHeader.isLittleEndian());
	
			b = new byte[4];
			stataDataRAF.read(b);
			int textLength = getIntValue(b, dtaHeader.isLittleEndian());
			
			int offsets[] = new int[numberOfEntries];
			for(int i=0;i<numberOfEntries;i++){
				b = new byte[4];
				stataDataRAF.read(b);
				offsets[i]= getIntValue(b, dtaHeader.isLittleEndian());
			}
			int values[] = new int[numberOfEntries];
			for(int i=0;i<numberOfEntries;i++){
				b = new byte[4];
				stataDataRAF.read(b);
				values[i]= getIntValue(b, dtaHeader.isLittleEndian());
			}
			
			b = new byte[textLength];
			stataDataRAF.read(b);
			String[] labels = new String[numberOfEntries];
			for(int i=0;i<numberOfEntries;i++){
				if(offsets.length>=(i+1)){
					byte[] text = Arrays.copyOfRange(b,offsets[i],  b.length);
					labels[i]= getStringValue(text);
				}
				else{
					byte[] text = Arrays.copyOfRange(b,offsets[i],  offsets[i+1]);
					labels[i]= getStringValue(text);
				}
			}
			//Add value label pairs to variables 
			for (DtaVariable dtaVariable: dtaVariables){
				if(dtaVariable.getVariableValueLabelName().equals(valueLabelName)){
					HashMap<String,String> valueLabelMap = dtaVariable.getVariableValueLabels();
					for(int i=0;i<values.length;i++){
						valueLabelMap.put(""+values[i], labels[i]);
					}
				}
			}
			if(dtaHeader.getDtaFileFormat() == 117){
				if(stataDataRAF.length()<= stataDataRAF.getFilePointer()+"</lbl></value_labels></stata_dta>".length())
					break;
				else
					stataDataRAF.seek(stataDataRAF.getFilePointer() + "</lbl><lbl>".length());
			}
			else{
				if(stataDataRAF.length()<= stataDataRAF.getFilePointer()) break;
			}
		}
	}

	/**
	 * This method calculates int value from the signed byte array.  
	 * Byte Array size is expected to be  4 
	 * 
	 * @param bytes
	 * @param isLittleEndian
	 * @return int value of the byte array
	 */
	protected int getIntValue(byte[] bytes, boolean isLittleEndian){
		int i;
		if(isLittleEndian)
			i = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
		else
			i = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
		return i;
	}

	/**
	 * This method calculates long value from the signed byte array.  
	 * Byte Array size is expected to be  8 
	 * 
	 * @param bytes
	 * @param isLittleEndian
	 * @return long value of the byte array
	 */
	protected long getLongValue(byte[] bytes, boolean isLittleEndian){
		long l;
		if(isLittleEndian)
			l = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
		else
			l = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
		return l;
	}
	
	/**
	 * Considers the bytes a unsigned and calculates int value 
	 * @param bytes
	 * @param isLittleEndian
	 * @return value of the bytes.
	 */
	protected int getUnsignedIntValue(byte[] bytes,  boolean isLittleEndian){
		byte[] b = new byte[4];
		for(int i=0;i<bytes.length;i++){
			b[i]=bytes[i];
		}
		return getIntValue(b,isLittleEndian);
	}

	/**
	 * Considers the bytes a unsigned and calculates long value 
	 * @param bytes
	 * @param isLittleEndian
	 * @return vlaue of the bytes.
	 */
	protected long getUnsignedLongValue(byte[] bytes,  boolean isLittleEndian){
		byte[] b = new byte[8];
		for(int i=0;i<bytes.length;i++){
			b[i]=bytes[i];
		}
		return getLongValue(b,isLittleEndian);
	}

	/**
	 * This method calculates short value from the signed byte array.  
	 * Byte Array size is expected to be 2 
	 * 
	 * @param bytes
	 * @param isLittleEndian
	 * @return short value of the byte array
	 */
	protected short getShortValue(byte[] bytes, boolean isLittleEndian){
		short s;
		if(isLittleEndian)
			s = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
		else
			s = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
		return s;
	}

	/**
	 * This method calculates int value from the signed byte.  
	 * @param b
	 * @param isLittleEndian
	 * @return
	 */
	protected int getByteValue(byte b, boolean isLittleEndian){
		int intValue = 0;
		if(isLittleEndian){
			intValue |= (b & 0xFF) << (0 * 8);
		}
		else{
			//TODO this block of code is not tested
			intValue |= (b & 0xFF) << (0 * 8);
		}
		return intValue;
	}

	/**
	 * Returns the string value of the bytes array.
	 * Uses delimiter '\0' to find the end of string
	 * @param bytes
	 * @return String value of the bytes array;
	 */
	protected String getStringValue(byte[] bytes){
		StringBuffer sb = new StringBuffer("");
		
		for(int i=0;i<bytes.length;i++){
			byte b = bytes[i];
			if(b=='\0') break;
			sb.append(""+(char)b);
		}
			
		return sb.toString();
	}

	
	/**
	 * This method returns double value from byte array.
	 * 
	 * @param b  expected to be 8 bytes long
	 * @param isLittleEndian
	 * @return
	 */
	protected double getDoubleValue(byte[] b, boolean isLittleEndian){
		double d;
		if(isLittleEndian)
			d = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble();
		else
			d = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getDouble();
		return d;
	}
	
	/**
	 * This method returns float value from byte array.
	 * 
	 * @param b  expected to be 4 bytes long
	 * @param isLittleEndian
	 * @return
	 */
	protected float getFloatValue(byte[] b, boolean isLittleEndian){
		float f;
		
		if(isLittleEndian)
			f = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		else
			f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
		
		return f;
	}
	
	/**
	 * This method converts the byte, int, long, float and double values from byte form to their decimal form.
	 * Then checks to see of the value is a missing value as defined by the STATA. 
	 * If missing value, returns the code for the missing value; otherwise returns the decimal value in the form of a string  
	 * @param b
	 * @param isLittleEndian
	 * @return Returns the value in the String form
	 */
	protected String getValue(byte[] b, boolean isLittleEndian){
		throw new RuntimeException("Invalid Mathod Call");
	}


	// getter and setters
	public String getDataFile() {
		return dataFile;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	public RandomAccessFile getStataDataRAF() {
		return stataDataRAF;
	}

	public void setStataDataRAF(RandomAccessFile stataDataRAF) {
		this.stataDataRAF = stataDataRAF;
	}

	public DtaHeader getDtaHeader() {
		return dtaHeader;
	}

	public void setDtaHeader(DtaHeader dtaHeader) {
		this.dtaHeader = dtaHeader;
	}

	public List<DtaVariable> getDtaVariables() {
		return dtaVariables;
	}

	public void setDtaVariables(List<DtaVariable> dtaVariables) {
		this.dtaVariables = dtaVariables;
	}
}