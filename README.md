# CED2AR STATA READER
----------------------------
This Project contains java classes that will allow you to read several versions of STATA data sets.
Following versions of STATA datasets are supported by the data readers in the package. 
#
    Format        Current as of
    ---------------------------------------
      118         Stata 14 http://www.stata.com/help.cgi?dta
      117         Stata 13 http://www.stata.com/help.cgi?dta
      116         internal; never released
      115         Stata 12  http://www.stata.com/help.cgi?dta_115
      114         Stata 10 & 11  http://www.stata.com/help.cgi?dta_114
      113         Stata  8 & 9  http://www.stata.com/help.cgi?dta_113
    --------------------------------------
    
Usage ....

	StataReaderFactory factory = new StataReaderFactory();
	StataReader SR = factory.getStataReader("C:\\java\\info\\Data\\STATA\\auto13WithLabel80.dta");
	List<String> observation = SR.getObservation(8);
	You can call any methods defined in the interface.
	Java doc for the classes is at /info/index.html 
#

Code Structure

1. All STATA readers are implementations of StataReader interface. 
2. DtaReader is the super class of all readers. Basic operations such as methods 
   to convert stream of bytes to various kinds of decimal values are coded here. This class also contains methods that are common 
   to all readers; some methods are overridden in the readers.  
3. Dta113Reader, Dta114Reader and Dta115Readers form a class hierarchy that  adds code to reflect the changes in the 113, 114 and 115.
5. Beginning Format 117 stata data files format has changed significantly. Hence, Dta117Reader and Dta118Reader form different hierarchy to that adds code to to reflect the changes in the 117 and 118.  
   
# Limitations

	** STATA Version 13 and version 14 the large string formats are ignored by the readers
	** STATA version 14 is not tested yet.
	** These readers are tested for byte both byte formats LitleEndian and BigEndian.
	** Code is tested on 64 bit machine.  (Will need to test against 32???) 
	** Code is not tested against multilingual data sets. 
