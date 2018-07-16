package edu.cornell.ncrn.ced2ar;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.cornell.ncrn.ced2ar.exceptions.InvalidDtaFormatException;
import edu.cornell.ncrn.ced2ar.impl.Dta113Reader;
import edu.cornell.ncrn.ced2ar.impl.Dta114Reader;
import edu.cornell.ncrn.ced2ar.impl.Dta115Reader;
import edu.cornell.ncrn.ced2ar.impl.Dta117Reader;
import edu.cornell.ncrn.ced2ar.impl.Dta118Reader;

public class StataReaderFactory {
	private static final Logger logger = Logger.getLogger(StataReaderFactory.class);
	public StataReader getStataReader(String stataFile) throws IOException, InvalidDtaFormatException{
		try{
			return (new Dta115Reader(stataFile));
		}
		catch(InvalidDtaFormatException IDFE){
			logger.info("Stata Data file " + stataFile + " is not a Format 115.");
		}
		try{
			return (new Dta114Reader(stataFile));
		}
		catch(InvalidDtaFormatException IDFE){
			logger.info("Stata Data file " + stataFile + " is not a Format 114.");
		}
		try{
			return (new Dta113Reader(stataFile));
		}
		catch(InvalidDtaFormatException IDFE){
			logger.info("Stata Data file " + stataFile + " is not a Format 113.");
		}
		try{
			return (new Dta117Reader(stataFile));
		}
		catch(InvalidDtaFormatException IDFE){
			logger.info("Stata Data file " + stataFile + " is not a Format 117");
		}
		try{
			return (new Dta118Reader(stataFile));
		}
		catch(InvalidDtaFormatException IDFE){
			logger.info("Stata Data file " + stataFile + " is not a Format 113, 114, 115, 117 or 118 stata file. ie This datafile is not stata v8, v10, v12, v13 or v14");
			throw IDFE;
		}

	}
	
	
	public static void main(String argc[]) throws Exception{
		StataReaderFactory factory = new StataReaderFactory();
		// this file is failing on reading value labels'
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\STATA\\Expert-Survey-STATAVersion-2.2.dta");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\STATA\\dc2010UR1_ALL_VARS.DTA");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\Michelle  Data\\04275-0001-Data.dta");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\Michelle  Data\\07948-0001-Data.dta");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\Michelle  Data\\29662-0001-Data.dta");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\STATA\\auto2.dta");
		//StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\STATA\\statacarwithLabel.dta");
		//"C:\\java\\info\\Data\\STATA\\statacarwithLabel.dta"  117

		//stataFile.readStataFile("C:\\java\\info\\Data\\STATA\\IMFFinReform.dta");
		//stataFile.readStataFile("C:\\java\\info\\Data\\STATA\\me2010ur1_all_vars.DTA");
		//stataFile.readStataFile("C:\\java\\info\\Data\\STATA\\dc2010UR1_ALL_VARS.DTA");
		//stataFile.readStataFile("C:\\java\\info\\Data\\STATA\\p10i6.dta");
		//stataFile.readStataFile("C:\\java\\info\\Data\\STATA\\ssb_v6_0_synthetic1_1.dta");
		//StataFile stataFile = new StataFile("C:\\java\\info\\Data\\STATA\\statacar.dta");
		//SR.dumpData();
		//System.out.println(SR.getObservation(7));
		//System.out.println(SR.getObservation(8));
		
	}


}
