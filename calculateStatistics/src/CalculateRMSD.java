import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

/**This is a chopped out class to calculate RMSD only
 * @author Amr
 *
 */
public class CalculateRMSD {

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
	
//	ArrayList<String> pairIds;
//	Hashtable<String, ReceptorLigandPairData> pairsData= new Hashtable<String, CollectResultsStatistics.ReceptorLigandPairData>();
//	ArrayList<Hashtable<String, PairDockingResult>> allPairsDockingResults = new ArrayList<Hashtable<String,PairDockingResult>>(3);
//	String[] suffixes;
	
	public static void main(String[] args) {
		try {
			new CalculateRMSD(args);
		} catch (Exception e) {
			printHelp(System.err);
			e.printStackTrace();
		}
	}

	final static String AuthorMsg="RMSD Calculation by Amr ALHOSSARY <aalhossary@pmail.ntu.edu.sg>\n"+
	"This code was part of the evaluation done in: "+
			"DOI:10.1093/bioinformatics/btv082 \"Fast, accurate, and reliable molecular docking with QuickVina 2, A. Alhossary et al.; "
			+ "Bioinformatics 31(13):2214-2216 (2015)\"\n";
	
	public CalculateRMSD(String[] args) throws Exception {
		if(Boolean.parseBoolean(Utilities.getParameter(args, "-h", "true" , "false"))
			|| Boolean.parseBoolean(Utilities.getParameter(args, "-help", "true" , "false"))
			|| Boolean.parseBoolean(Utilities.getParameter(args, "--help", "true" , "false"))
			|| Boolean.parseBoolean(Utilities.getParameter(args, "-?", "true" , "false"))){
			printHelp(System.out);
			System.exit(0);
		}

		String preparedLigandFileString	= Utilities.getParameter(args, "-ligand", null , "ligand.pdbqt");
		String dockedLigandsFileString	= Utilities.getParameter(args, "-dockedLigand", null , "docked.pdbqt");
		String outputFileString			= Utilities.getParameter(args, "-out", "out.txt", "-");
		
		File preparedLigandFile = new File(preparedLigandFileString);
		ReceptorLigandPairData preparedPairData = parseFile(preparedLigandFile);
		
		File dockedLigandFile = new File(dockedLigandsFileString);
		ReceptorLigandPairData dockedPairData = parseFile(dockedLigandFile);
		
		PairDockingResult pairDockingResult = new PairDockingResult();
		//set missing values in the PairDockingResult object
		pairDockingResult.allAtoms=dockedPairData.allAtoms;
		
		//calculate RMSD per model to original 
		for (int j = 0; j < pairDockingResult.allAtoms.size(); j++) {//up to 9 models
			pairDockingResult.conformationRMSDsToPDB[j]=calculateRMSD(preparedPairData.allAtoms.get(0),pairDockingResult.allAtoms.get(j));
		}
		
		PrintStream out = null;
		if ("-".equals(outputFileString)) {
			out=System.out;
		}else{
			out = new PrintStream(outputFileString);
		}
		if (out != null) {//this check is not necessary anymore
			if (out != System.out)
				System.out.println(AuthorMsg);
			outputResult(out, dockedPairData, pairDockingResult);
			if (out != System.out)
				out.close();//non effective line
		}
		System.out.println("Done!");
	}

	private static void printHelp(PrintStream printStream) {
		printStream.println();
		printStream.println(AuthorMsg);
		printStream.println("Command line was\n"+System.getProperty("sun.java.command"));
		printStream.println();
		printStream.println("Usage:");
		printStream.print("java -jar "+System.getProperty("sun.java.command").split("\\s+")[0]);
		printStream.println(" [-ligand <ligand>] [-dockedLigand <docked>] [-out [out]] [-h/-?/-help/--help]");
		printStream.println();
		printStream.println("Example:");
		printStream.println("java -jar calculateRMSD.jar -ligand 1a30_lig.pdbqt -dockedLigand 1a30_lig_docked.pdbqt -out 1a30_rmsd.txt");
		printStream.println("calcultes the RMSD between 1a30_lig.pdbqt and 1a30_lig_docked.pdbqt (in the same directory) and sends the output to the file 1a30_rmsd.txt");
		printStream.println();
		printStream.println("ligand\tThe experimental ligand (ligand.pdbqt)");
		printStream.println("docked\tThe docked ligand \t(docked.pdbqt)");
		printStream.println("out\tThe output file\t\t(out.txt)");
		printStream.println("[-h/-?/-help/--help] prints this help screen and exit");
		printStream.println("N.Bs:");
		printStream.println(" 1) [] indicate optional, <> indicate mandatory, () indicate default.");
		printStream.println(" 2) If the -out parameter is omitted altogether, it defaults to the std output.");
		printStream.println(" 3) Please note case sensitivity of typed characters.");
		printStream.println();
	}

	public void outputResult(PrintStream out, ReceptorLigandPairData pairData, PairDockingResult pairDockingResult) {
		out.println();
		out.println(AuthorMsg);
		out.format("Heavy Atoms\t%d\n", pairData.noHeavyAtoms);
		out.format("ActiveBonds\t%d\n", pairData.noActiveBonds);
		for (int j = 0; j < pairDockingResult.allAtoms.size(); j++) {//9
			out.format("RMSD(%d)\t%.3f\n",j+1,pairDockingResult.conformationRMSDsToPDB[j]);
		}
		out.println();
	}

	
	static float  calculateRMSD(Atom[] atoms, Atom[] atoms2) {
		return Math.max(rmsdPrime(atoms,atoms2),rmsdPrime(atoms2,atoms));
	}

	private static float rmsdPrime(Atom[] atoms, Atom[] atoms2) {
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

	public static ReceptorLigandPairData parseFile(File preparedLigandsFile) throws Exception{
		Scanner tempScanner=null;
		ReceptorLigandPairData pairDataToFill = new ReceptorLigandPairData();
		ArrayList<Atom[]> modelsAccummulator= new ArrayList<CalculateRMSD.Atom[]>();
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
					atomsAccumulatorPerModel=new ArrayList<CalculateRMSD.Atom>();
				}else if (line.matches("^ATOM\\s+.+$")||line.matches("^HETATM\\s+.+$")) {
					if (atomsAccumulatorPerModel == null) {
						atomsAccumulatorPerModel=new ArrayList<CalculateRMSD.Atom>();
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
			throw new RuntimeException("File Not Found :"+preparedLigandsFile, e);
		}finally{
			if (tempScanner != null) {
				tempScanner.close();
			}
		}
		pairDataToFill.allAtoms=modelsAccummulator;
		return pairDataToFill;
	}

	private static void moveAtoms(ArrayList<Atom> smallerAtomAccumulator, ArrayList<Atom[]> ret) {
		Atom[] atoms = new Atom[smallerAtomAccumulator.size()];
		smallerAtomAccumulator.toArray(atoms);
		ret.add(atoms);
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
	private static Atom pdb_ATOM_Handler(String line) {
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
