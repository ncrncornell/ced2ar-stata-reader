package edu.cornell.ncrn.ced2ar.impl;


//LSF=LITTLE_ENDIAN
//MSF=BIG_ENDIAN


public class DtaHeader {
	public enum ByteOrder{
		BIG_ENDIAN(1),LITTLE_ENDIAN(2);
		private int value;
		private ByteOrder(int value){
			this.value = value;
		}
	}
	private byte dtaFileFormat;
	private byte byteOrder;
	private int numberOfVariables;
	private long numberOfObservations;
	private String dataLabel;
	private String timeStamp;

	
	public byte getDtaFileFormat() {
		return dtaFileFormat;
	}
	public void setDtaFileFormat(byte dtaFileFormat) {
		this.dtaFileFormat = dtaFileFormat;
	}
	public byte getByteOrder() {
		return byteOrder;
	}
	public void setByteOrder(byte byteOrder) {
		this.byteOrder = byteOrder;
	}
	public int getNumberOfVariables() {
		return numberOfVariables;
	}
	public void setNumberOfVariables(int numberOfVariables) {
		this.numberOfVariables = numberOfVariables;
	}
	public long getNumberOfObservations() {
		return numberOfObservations;
	}
	public void setNumberOfObservations(long numberOfObservations) {
		this.numberOfObservations = numberOfObservations;
	}
	public String getDataLabel() {
		return dataLabel;
	}
	public void setDataLabel(String dataLabel) {
		this.dataLabel = dataLabel;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public boolean isLittleEndian(){
		boolean isLittleEndian=false;
		isLittleEndian  = (byteOrder == ByteOrder.LITTLE_ENDIAN.value);
		return isLittleEndian;
	}

	
	@Override
	public String toString() {
		return "DtaHeader [dtaFileFormat=" + dtaFileFormat + ", byteOrder="
				+ byteOrder + ", numberOfVariables=" + numberOfVariables
				+ ", numberOfObservations=" + numberOfObservations
				+ ", dataLabel=" + dataLabel + ", timeStamp=" + timeStamp + "]";
	}


	
}



