package edu.cornell.ncrn.ced2ar.stata.impl;

import java.util.HashMap;
import java.util.Map;

public class DtaVariable {
	public enum Dta117DataType{
		BYTE(65530),INT(65529),LONG(65528),FLOAT(65527),DOUBLE(65526);
		private int dataTypeCode;
		private Dta117DataType(int dataTypeCode){
			this.dataTypeCode = dataTypeCode;
		}
	}
	public enum DtaDataType{
		BYTE(251),INT(252),LONG(253),FLOAT(254),DOUBLE(255);
		private int dataTypeCode;
		private DtaDataType(int dataTypeCode){
			this.dataTypeCode = dataTypeCode;
		}
	}
	
	private boolean isDta117DataType;
	private String name;
	private int variableType;
	private String variableValueLabelName;
	private String variableLabel;
	private String variableFormat;
	private HashMap<String,String> variableValueLabels = new HashMap<String,String>();
	
	public String getRawName(){
		return name;
	}
	public String getName() {
		return getCSVCompatibleString(name);
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getVariableType() {
		return variableType;
	}
	public void setVariableType(int variableType) {
		this.variableType = variableType;
	}
	public String getRawVariableLabel() {
		return variableLabel;
	}
	public String getVariableLabel() {
		return getCSVCompatibleString(variableLabel);
	}
	public void setVariableLabel(String variableLabel) {
		this.variableLabel = variableLabel;
	}
	
	public String getVariableFormat() {
		return variableFormat;
	}
	public void setVariableFormat(String variableFormat) {
		this.variableFormat = variableFormat;
	}
	public boolean isDta117DataType() {
		return isDta117DataType;
	}
	public void setDta117DataType(boolean isDta117DataType) {
		this.isDta117DataType = isDta117DataType;
	}

	public boolean isByte(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType == Dta117DataType.BYTE.dataTypeCode);
		else
			returnValue =  (variableType == DtaDataType.BYTE.dataTypeCode);
		return 	returnValue;	
	}
	
	public boolean isInt(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType == Dta117DataType.INT.dataTypeCode);
		else
			returnValue =  (variableType == DtaDataType.INT.dataTypeCode);
		return 	returnValue;	
	}
	
	public boolean isLong(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType == Dta117DataType.LONG.dataTypeCode);
		else
			returnValue =  (variableType == DtaDataType.LONG.dataTypeCode);
		return 	returnValue;	
	}
	
	public boolean isFloat(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType == Dta117DataType.FLOAT.dataTypeCode);
		else
			returnValue =  (variableType == DtaDataType.FLOAT.dataTypeCode);
		return 	returnValue;	
	}
	
	public boolean isDouble(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType == Dta117DataType.DOUBLE.dataTypeCode);
		else
			returnValue =  (variableType == DtaDataType.DOUBLE.dataTypeCode);
		return 	returnValue;	
	}
	
	public boolean isString(){
		boolean returnValue =  false;
		if(isDta117DataType())
			returnValue =  (variableType >= 1 && variableType<= 2045);
		else
			returnValue =  (variableType >= 1 && variableType<= 244);
		return 	returnValue;	
	}
	
	public String getVariableValueLabelName() {
		return variableValueLabelName;
	}
	public void setVariableValueLabelName(String variableValueLabelName) {
		this.variableValueLabelName= variableValueLabelName;
	}
	public HashMap<String, String> getVariableValueLabels() {
		return variableValueLabels;
	}
	public void setVariableValueLabels(HashMap<String, String> variableValueLabels) {
		this.variableValueLabels = variableValueLabels;
	}
	public boolean isDate(){
		return (variableFormat.startsWith("%d")  ||
				variableFormat.startsWith("%-d") ||		
				variableFormat.startsWith("%t")  ||
				variableFormat.startsWith("%-t"));
	}
	@Override
	public String toString() {
		StringBuilder SB =  new StringBuilder("");
		if(!variableValueLabels.isEmpty()){
			for (Map.Entry<String, String> entry : variableValueLabels.entrySet()) {
			    SB.append( entry.getKey()+"=");
			    SB.append( entry.getValue()+":");
			}
		}
		
		return "StataVariable [name=" + name + ", variableType=" + variableType+ ", variableFormat=" + variableFormat
				+", variableValueLabelName=" + variableValueLabelName+  ", variableLabel=" + variableLabel + ", Value Labels= "+SB.toString()+"]";
	}
	
	
	
	/**
	 * This method makes the String RFC 4180 (CSV format) Compatible 
	 * If String contains comma and double quotes.
	 * 	1. encloses the string with double quotes
	 *  2. adds a double quote to immediately after the quote.	
	 *  Specification http://tools.ietf.org/html/rfc4180#ref-2
	 *  WIKI https://en.wikipedia.org/wiki/Comma-separated_values
	 * @param str
	 * @return CSV Compatible string
	 */
	protected String getCSVCompatibleString(String str){

		if(str.contains("\"")){
			str = str.replaceAll("\"", "\"\"");
		}
		
		if(str.contains(",")){
			str  = "\""+str+"\"";
		}
		return str;
	}

}
