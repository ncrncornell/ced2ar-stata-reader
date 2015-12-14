package edu.ncrn.cornell.ced2ar.stata.exceptions;

public class InvalidDtaFormatException  extends Exception{
	private static final long serialVersionUID = -2609814390907701757L;

	public InvalidDtaFormatException(String message){
		super(message);
	}
}
