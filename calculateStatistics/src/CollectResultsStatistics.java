import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Scanner;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CollectResultsStatistics {
	public static final int REFERENCE=0;
	
	ArrayList<String> pairIds;
	Hashtable<String, ReceptorLigandPairData> pairsData= new Hashtable<String, CollectResultsStatistics.ReceptorLigandPairData>();
	ArrayList<Hashtable<String, PairDockingResult>> allPairsDockingResults = new ArrayList<Hashtable<String,PairDockingResult>>(3);
	String[] suffixes;
	
	public static class ReceptorLigandPairData{
		String pairID;
		int noHeavyAtoms=-1;
		int noActiveBonds=-1;
		ArrayList<Atom[]> allAtoms;//size is only one (one model) here
		//float searchVolume;
	}
	public static class PairDockingResult{
		int dockingProgram;
		float time;
		float[] conformationBindingEnergies= new float[9];
		float[] conformationRMSDsToPDB= new float[9];
		ArrayList<Atom[]> allAtoms;//size is up to 9 (9 models) here
	}
	public static class Atom{
		String atomType;
		int id;
		double x, y, z;
	}
	
	public static void main(String[] args) {
		new CollectResultsStatistics(args);
	}

	public CollectResultsStatistics(String[] args) {
		String preparedLigandsFilesFolderString= Utilities.getParameter(args, "-ligands", null , "ligands_pdbqt");
		String logsFolderString= Utilities.getParameter(args, "-logsFolder", null , "./");
		String dockedLigandsFilesFolderString= Utilities.getParameter(args, "-dockedLigands", null , "docked");
		String outputFileString= Utilities.getParameter(args, "-out", null ,"comparisonOut.xlsx");
		String refFileString = Utilities.getParameter(args, "-ref", null, "Ref.xlsx");
		String suffixString= Utilities.getParameter(args, "-suffixes", null ,"");

		suffixes = suffixString.split(" ");
		
		for (int i = 0; i < suffixes.length; i++) {
			allPairsDockingResults.add(parseLogFile((new File(logsFolderString, suffixes[i]+".out").getAbsolutePath())));
		}

		Hashtable<String, PairDockingResult> vinaDockingResults = allPairsDockingResults.get(REFERENCE);
		pairIds=new ArrayList<String>(vinaDockingResults.keySet());
		Collections.sort(pairIds);
		
		for (String pairId : pairIds) {
			File preparedLigandsFile = new File(preparedLigandsFilesFolderString, (pairId+"_ligand.pdbqt"));
			ReceptorLigandPairData preparedPairData = parseFile(preparedLigandsFile);
			//add to pairsData
			pairsData.put(pairId, preparedPairData);
			for (int i = 0; i < suffixes.length; i++) {//3 tools
				File dockedLigandFile = new File(dockedLigandsFilesFolderString, (pairId+"_lig_docked_"+suffixes[i]+".pdbqt"));
				ReceptorLigandPairData dockedPairData = parseFile(dockedLigandFile);
				PairDockingResult pairDockingResult = allPairsDockingResults.get(i).get(pairId);
				//set missing values in the PairDockingResult object
				pairDockingResult.dockingProgram=i;
				pairDockingResult.allAtoms=dockedPairData.allAtoms;
//				pairDockingResult.time;//already done
//				pairDockingResult.conformationBindingEnergies;//already done
				//calculate RMSD per model (3*9) to original 
				for (int j = 0; j < pairDockingResult.allAtoms.size(); j++) {//up to 9 models
					pairDockingResult.conformationRMSDsToPDB[j]=calculateRMSD(preparedPairData.allAtoms.get(0),pairDockingResult.allAtoms.get(j));
				}
			}
		}
		outputResultExcel(outputFileString, refFileString, pairIds, pairsData, allPairsDockingResults);
//		PrintStream out = null;
//		try {
//			out = new PrintStream(outputFileString);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		if (out != null) {
//			outputResultExcel(out, pairIds, pairsData, allPairsDockingResults);
//		}
//		outputResult(System.out, pairIds, pairsData, allPairsDockingResults);
		System.out.println("Done!");
	}

	public void outputResult(PrintStream out, ArrayList<String> pairIds, Hashtable<String, ReceptorLigandPairData> pairsData, ArrayList<Hashtable<String, PairDockingResult>> allPairsDockingResults) {
		//print header
		out.format("PairId\tHeavy Atoms\tActiveBonds");
		for (int i = 0; i < allPairsDockingResults.size(); i++) {//3
			out.format("\t%s time",suffixes[i]);
			Hashtable<String, PairDockingResult> pairDockingResults = allPairsDockingResults.get(i);
			for (int j = 0; j < pairDockingResults.get(pairIds.get(0)).conformationBindingEnergies.length; j++) {//9
				out.format("\t%s B.Energy(%d)\t%s RMSD(%d)",suffixes[i],j+1,suffixes[i],j+1);
			}
		}
		out.println();
		
		//print data
		for (int i = 0; i < pairIds.size(); i++) {
			String pairId = pairIds.get(i);
			ReceptorLigandPairData pairData=pairsData.get(pairId);
			out.format("%s\t%d\t%d", pairId,pairData.noHeavyAtoms,pairData.noActiveBonds);
			for (int j = 0; j < allPairsDockingResults.size(); j++) {
				PairDockingResult dockingResult=allPairsDockingResults.get(j).get(pairId);
				out.format("\t%.3f",dockingResult.time);
				float[] bindingEnergies = dockingResult.conformationBindingEnergies;
				for (int k = 0; k < bindingEnergies.length; k++) {
					if (bindingEnergies[k]!=0) {
						out.format("\t%.3f\t%.3f", bindingEnergies[k],dockingResult.conformationRMSDsToPDB[k]);
					} else {
						out.format("\t \t ");
					}
				}
			}
			out.println();
		}
	}

	public void outputResultExcel(String outputFileString, String refExcelFileString, ArrayList<String> pairIds, Hashtable<String, ReceptorLigandPairData> pairsData, ArrayList<Hashtable<String, PairDockingResult>> allPairsDockingResults) {
		int rowNum =0;
		int colNum=0;
		// open excel file
		try {
			FileInputStream fileInputStream = new FileInputStream(outputFileString);
			Workbook workBook = new XSSFWorkbook(fileInputStream);
			Sheet sheet = workBook.getSheet("RawData");
			workBook.setSheetOrder("RawData", 0);
//			 Workbook workBook = new SXSSFWorkbook();
//			 Sheet sheet = workBook.createSheet("RawData");  

			//print header
			Row rowHead = sheet.createRow(rowNum++);
			rowHead.createCell(colNum++).setCellValue("Pair Id");
			rowHead.createCell(colNum++).setCellValue("Heavy Atoms");
			rowHead.createCell(colNum++).setCellValue("Active Bonds");

			for (int i = 0; i < allPairsDockingResults.size(); i++) {//3
				rowHead.createCell(colNum++).setCellValue(suffixes[i] +" Time");
				Hashtable<String, PairDockingResult> pairDockingResults = allPairsDockingResults.get(i);
				for (int j = 0; j < pairDockingResults.get(pairIds.get(0)).conformationBindingEnergies.length; j++) {//9
					rowHead.createCell(colNum++).setCellValue(String.format("%s B.Energy(%d)",suffixes[i],j+1));
					rowHead.createCell(colNum++).setCellValue(String.format("%s RMSD(%d)",suffixes[i],j+1));
				}
			}

			//print data
			for (int i = 0; i < pairIds.size(); i++) {
				Row row = sheet.createRow((rowNum++));
				colNum=0;
				String pairId = pairIds.get(i);
				ReceptorLigandPairData pairData=pairsData.get(pairId);
				row.createCell(colNum++).setCellValue(pairId);
				row.createCell(colNum++).setCellValue(pairData.noHeavyAtoms);
				row.createCell(colNum++).setCellValue(pairData.noActiveBonds);
				for (int j = 0; j < allPairsDockingResults.size(); j++) {
					PairDockingResult dockingResult=allPairsDockingResults.get(j).get(pairId);
					row.createCell(colNum++).setCellValue(Math.round(dockingResult.time * 1000.0)/1000.0);
					float[] bindingEnergies = dockingResult.conformationBindingEnergies;
					for (int k = 0; k < bindingEnergies.length; k++) {
						if (bindingEnergies[k]!=0) {
							row.createCell(colNum++).setCellValue(Math.round(bindingEnergies[k] * 1000.0)/1000.0);
							row.createCell(colNum++).setCellValue(Math.round(dockingResult.conformationRMSDsToPDB[k] * 1000.0)/1000.0);
						} else {
							row.createCell(colNum++).setCellValue(" ");
							row.createCell(colNum++).setCellValue(" ");
						}
					}
				}
			}

			if (refExcelFileString != null) {
				addRefExcelFile(workBook, refExcelFileString);
			}

			updateWorkbook(workBook);

			fileInputStream.close();

			FileOutputStream out = null;
			try {
				out= new FileOutputStream(outputFileString);
				workBook.write(out);
				workBook.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return;
		} catch ( IOException ex ) {
			System.out.println(ex);
		}
	}
	private void updateWorkbook(Workbook workBook) {
		for (int sheetNum = 0; sheetNum < workBook.getNumberOfSheets(); sheetNum++) {
			Sheet sheet = workBook.getSheetAt(sheetNum);
			sheet.setForceFormulaRecalculation(true);
		}
	}

	private void addRefExcelFile(Workbook workBook, String refExcelFileString) throws IOException{
		int rowNum = 0;int colNum = 0;
		Workbook refWorkbook = new XSSFWorkbook(refExcelFileString);
		Sheet refSheet = refWorkbook.getSheetAt(0);
		Sheet newSheet = workBook.getSheet("Reference");
		workBook.setSheetOrder("Reference", 1);
		
		for (Row refRow : refSheet) {
			Row newRow = newSheet.createRow(rowNum++);
			colNum = 0;
			for (Cell refCell : refRow) {
				Cell newCell = newRow.createCell(colNum++);
				switch (refCell.getCellType()) {
				case Cell.CELL_TYPE_FORMULA: 
					newCell.setCellValue(refCell.getCellFormula());
					break;
				case Cell.CELL_TYPE_NUMERIC:
					newCell.setCellValue(refCell.getNumericCellValue());
					break;
				case Cell.CELL_TYPE_STRING:
					newCell.setCellValue(refCell.getStringCellValue());
				}
			}
			refWorkbook.close();
		}
	}

	float calculateRMSD(Atom[] atoms, Atom[] atoms2) {
		return Math.max(rmsdPrime(atoms,atoms2),rmsdPrime(atoms2,atoms));
	}

	private float rmsdPrime(Atom[] atoms, Atom[] atoms2) {
		float sumOfMin=0;
		int n=0;
		for (Atom atom : atoms) {
			double minR2 = Float.MAX_VALUE;
			if (! (atom.atomType.equals("H") || atom.atomType.equals("HD"))) {
				n++;
				for (Atom atom2 : atoms2) {
					if (atom2.atomType.equals(atom.atomType)) {
						//calculate distance (atom,atom2)
						double dx = atom.x-atom2.x;
						double dy = atom.y-atom2.y;
						double dz = atom.z-atom2.z;
						double r2=dx*dx+dy*dy+dz*dz;
						//save it if less than min
						if (r2<minR2) {
							minR2=r2;
						}
					}
				}
				sumOfMin += minR2;
			}
		}
		return (float) Math.sqrt(sumOfMin/n);
	}

	private ReceptorLigandPairData parseFile(File preparedLigandsFile) {
		Scanner tempScanner=null;
		ReceptorLigandPairData pairDataToFill = new ReceptorLigandPairData();
		ArrayList<Atom[]> modelsAccummulator= new ArrayList<CollectResultsStatistics.Atom[]>();
		try {
			tempScanner = new Scanner(preparedLigandsFile);
			ArrayList<Atom> atomsAccumulatorPerModel= null;
			while (tempScanner.hasNextLine()) {
				String line = tempScanner.nextLine();
				if (line.matches("^REMARK  (\\d+) active torsions:$")) {
					//get Number of Active Bonds
					int noActiveBonds = Integer.parseInt(line.split("\\s+")[1]);
					if (pairDataToFill.noActiveBonds == -1) {
						pairDataToFill.noActiveBonds=noActiveBonds;
					}else {
						if (pairDataToFill.noActiveBonds != noActiveBonds) {
							throw new RuntimeException("pairData.noActiveBonds "+pairDataToFill.noActiveBonds+" != "+noActiveBonds);
						}
					}
				}else if (line.matches("^MODEL \\d+$")) {
					atomsAccumulatorPerModel=new ArrayList<CollectResultsStatistics.Atom>();
				}else if (line.matches("^ATOM\\s+.+$")||line.matches("^HETATM\\s+.+$")) {
					if (atomsAccumulatorPerModel == null) {
						atomsAccumulatorPerModel=new ArrayList<CollectResultsStatistics.Atom>();
					}
					Atom atom = pdb_ATOM_Handler(line);
					atomsAccumulatorPerModel.add(atom);
				}else if (line.matches("ENDMDL")) {
					moveAtoms(atomsAccumulatorPerModel, modelsAccummulator);
					atomsAccumulatorPerModel = null;
				}
			}
			
			if (atomsAccumulatorPerModel!= null) {
				moveAtoms(atomsAccumulatorPerModel, modelsAccummulator);
			}
			
			for (int i = 0; i < modelsAccummulator.size(); i++) {
				Atom[] modelAtoms = modelsAccummulator.get(i);
				int heavyAtomsCounter=0;
				for (int j = 0; j < modelAtoms.length; j++) {
					if ( ! (modelAtoms[j].atomType.equals("HD") || modelAtoms[j].atomType.equals("H"))) {
						heavyAtomsCounter++;
					}
				}
				
				//get Number of Heavy Atoms
				if (pairDataToFill.noHeavyAtoms == -1) {
					pairDataToFill.noHeavyAtoms=heavyAtomsCounter;
				} else {
					if (pairDataToFill.noHeavyAtoms != heavyAtomsCounter) {
						throw new RuntimeException("pairData.noHeavyAtoms "+pairDataToFill.noHeavyAtoms+" != "+heavyAtomsCounter);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally{
			tempScanner.close();
		}
		pairDataToFill.allAtoms=modelsAccummulator;
		return pairDataToFill;
	}

	private void moveAtoms(ArrayList<Atom> smallerAtomAccumulator, ArrayList<Atom[]> ret) {
		Atom[] atoms = new Atom[smallerAtomAccumulator.size()];
		smallerAtomAccumulator.toArray(atoms);
		ret.add(atoms);
	}

	Hashtable<String, PairDockingResult> parseLogFile(String logFileString) {
		Hashtable<String, PairDockingResult> ret = new Hashtable<String, CollectResultsStatistics.PairDockingResult>(195);
		try {
			Scanner scanner = new Scanner(new File(logFileString));
			String pairId;
			PairDockingResult pairDockingResult=null;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.matches("^\\s+Processing (\\d\\w\\w\\w)$")) {
					pairId=line.substring(line.length()-4);
					pairDockingResult= new PairDockingResult();
					ret.put(pairId, pairDockingResult);
				}else if (line.matches("^\\s+(\\d)\\s+([\\d\\-\\.]+)\\s+([\\d\\-\\.]+)\\s+([\\d\\-\\.]+)$")) {
					String[] split = line.split("\\s+");
					int mode=Integer.parseInt(split[1]);
					pairDockingResult.conformationBindingEnergies[mode-1]=Float.parseFloat(split[2]);
				}else if (line.matches("^searching finished in ([\\d\\.]+) seconds$")) {
					String secString=line.substring("searching finished in ".length());
					secString=secString.substring(0,secString.indexOf(" "));
					pairDockingResult.time=Float.parseFloat(secString);
				}else{
					continue;
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return ret;
	}

	

	/**
	 Handler for
	 ATOM Record Format
	 *
	 <pre>
         ATOM      1  N   ASP A  15     110.964  24.941  59.191  1.00 83.44           N
	 *
	 COLUMNS        DATA TYPE       FIELD         DEFINITION
	 ---------------------------------------------------------------------------------
	 1 -  6        Record name     "ATOM  "
	 7 - 11        Integer         serial        Atom serial number.
	 13 - 16        Atom            name          Atom name.
	 17             Character       altLoc        Alternate location indicator.
	 18 - 20        Residue name    resName       Residue name.
	 22             Character       chainID       Chain identifier.
	 23 - 26        Integer         resSeq        Residue sequence number.
	 27             AChar           iCode         Code for insertion of residues.
	 31 - 38        Real(8.3)       x             Orthogonal coordinates for X in Angstroms.
	 39 - 46        Real(8.3)       y             Orthogonal coordinates for Y in Angstroms.
	 47 - 54        Real(8.3)       z             Orthogonal coordinates for Z in Angstroms.
	 55 - 60        Real(6.2)       occupancy     Occupancy.
	 61 - 66        Real(6.2)       tempFactor    Temperature factor.
	 73 - 76        LString(4)      segID         Segment identifier, left-justified.
	 77 - 78        LString(2)      element       Element symbol, right-justified.
	 79 - 80        LString(2)      charge        Charge on the atom.
	 </pre>
	 */
	private Atom pdb_ATOM_Handler(String line) {
//		String chain_id      = line.substring(21,22);
		// process group data:
		// join residue numbers and insertion codes together
//		String recordName     = line.substring (0, 6).trim ();
		//String pdbCode = line.substring(22,27).trim();
//		String groupCode3     = line.substring(17,20);
		// pdbCode is the old way of doing things...it's a concatenation
		//of resNum and iCode which are now defined explicitly
//		String resNum  = line.substring(22,26).trim();
		Character iCode = line.substring(26,27).charAt(0);
		if ( iCode == ' ')
			iCode = null;
//		ResidueNumber residueNumber = new ResidueNumber(chain_id, Integer.valueOf(resNum), iCode);

		//recordName      groupCode3
		//|                |    resNum
		//|                |    |   iCode
		//|     |          | |  |   ||
		//ATOM      1  N   ASP A  15     110.964  24.941  59.191  1.00 83.44           N
		//ATOM   1964  N   ARG H 221A      5.963 -16.715  27.669  1.00 28.59           N

//		Character aminoCode1 = null;
//
//		if ( recordName.equals("ATOM") ){
//			aminoCode1 = StructureTools.get1LetterCode(groupCode3);
//		} else {
//			// HETATOM RECORDS are treated slightly differently
//			// some modified amino acids that we want to treat as amino acids
//			// can be found as HETATOM records
//			aminoCode1 = StructureTools.get1LetterCode(groupCode3);
//			if ( aminoCode1 != null)
//				if ( aminoCode1.equals(StructureTools.UNKNOWN_GROUP_LABEL))
//					aminoCode1 = null;
//		}

		//          1         2         3         4         5         6
		//012345678901234567890123456789012345678901234567890123456789
		//ATOM      1  N   MET     1      20.154  29.699   5.276   1.0
		//ATOM    112  CA  ASP   112      41.017  33.527  28.371  1.00  0.00
		//ATOM     53  CA  MET     7      23.772  33.989 -21.600  1.00  0.00           C
		//ATOM    112  CA  ASP   112      37.613  26.621  33.571     0     0


//		String fullname = line.substring (12, 16);

		// create new atom
		Atom atom = new Atom() ;

//		int pdbnumber = Integer.parseInt (line.substring (6, 11).trim ());
//		atom.setPDBserial(pdbnumber) ;
//		atom.setFullName(fullname);
//		atom.setName(fullname.trim());

		atom.x = Double.parseDouble (line.substring (30, 38).trim());
		atom.y = Double.parseDouble (line.substring (38, 46).trim());
		atom.z = Double.parseDouble (line.substring (46, 54).trim());

//		double occu  = 1.0;
//		if ( line.length() > 59 ) {
//			try {
//				// occu and tempf are sometimes not used :-/
//				occu = Double.parseDouble (line.substring (54, 60).trim());
//			}  catch (NumberFormatException e){}
//		}
//
//		double tempf = 0.0;
//		if ( line.length() > 65) {
//			try {
//				tempf = Double.parseDouble (line.substring (60, 66).trim());
//			}  catch (NumberFormatException e){}
//		}
//		atom.setOccupancy(  occu  );
//		atom.setTempFactor( tempf );

		// Parse element from the element field. If this field is
		// missing (i.e. misformatted PDB file), then parse the
		// name from the atom name.
//		Element element = Element.R;
		if ( line.length() > 77 ) {
			// parse element from element field
			try {
				String element = line.substring (76, 78).trim();
				atom.atomType=element;
			}  catch (IllegalArgumentException e){}
		} 
//		else {
//			// parse the name from the atom name
//			String elementSymbol = null;
//			// for atom names with 4 characters, the element is
//			// at the first position, example HG23 in Valine
//			if (fullname.trim().length() == 4) {
//				elementSymbol = fullname.substring(0, 1);
//			} else if ( fullname.trim().length() > 1){
//				elementSymbol = fullname.substring(0, 2).trim();
//			} else {
//				// unknown element...
//				elementSymbol = "R";
//			}
//
//			try {
//				element = Element.valueOfIgnoreCase(elementSymbol);
//			}  catch (IllegalArgumentException e){}
//		}
//		atom.setElement(element);
		return atom;
		//System.out.println("current group: " + current_group);
	}
}
