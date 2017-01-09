import java.io.File;
import java.util.Hashtable;

public class CalculateNPBindingEnergy {

	//	static CCollectResultsStatistics.PairDockingResult dockedLigand = null;
	//	ArrayList<CalculateRMSD.ReceptorLigandPairData> refLigands;
	//	ArrayList<CalculateRMSD.ReceptorLigandPairData> dockedLigands;

	public static void main(String[] args) {
		String refFolderString= Utilities.getParameter(args, "-ref", null, "./ref");
		String dockedFolderString = Utilities.getParameter(args, "-docked", null, "./docked");
		String dockedFilesPatternString = Utilities.getParameter(args, "-dockedPattern", null, "MBSC_%06d.out");
		String    refFilesPatternString = Utilities.getParameter(args,    "-refPattern", null, "MBSC_%06d_vina112_large_search_space_crystal.out");
		File refFolder = new File(refFolderString);
		File dockedFolder = new File(dockedFolderString);
		float[] comparison=new float[2];

		printHeader();

		for(int i=1;i <= 54529; i++){
			File dockedFile = new File(dockedFolder, String.format(dockedFilesPatternString, i)); 
			if (!dockedFile.exists()){
				System.out.println("Docked file not exist " + dockedFile.getName());
				continue;
			}

			File refFile = new File(refFolder, String.format(refFilesPatternString, i)); 
			//			System.out.println("this is ref file " + refFile.getName());
			if(!refFile.exists()){
				System.out.println("Ref file not exist " + refFile.getName());
				continue;
			}
			Hashtable<String, CollectResultsStatistics.PairDockingResult> dockedLigandInHashTable = null;
			Hashtable<String, CollectResultsStatistics.PairDockingResult> refLigandInHashTable = null;
			try {
				refLigandInHashTable=CollectResultsStatistics.parseLogFile(refFile.getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				dockedLigandInHashTable = CollectResultsStatistics.parseLogFile(dockedFile.getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
			CollectResultsStatistics.PairDockingResult dockedLigand = dockedLigandInHashTable.get(dockedFile.getName().substring(0, 11));
			CollectResultsStatistics.PairDockingResult refLigand = refLigandInHashTable.get(refFile.getName().substring(0, 11));
//			int dockedFileBEIndex = dockedLigand.conformationBindingEnergies.length-1;
//			for( ; dockedFileBEIndex >=0 ;dockedFileBEIndex--){
//				if(refLigand.conformationBindingEnergies[0] >= dockedLigand.conformationBindingEnergies[dockedFileBEIndex]){
//					break;
//				}
//			}
			comparison=compareBE(refLigand.conformationBindingEnergies, dockedLigand.conformationBindingEnergies);
			printBindingEnergies(dockedFile.getName().substring(0, 11),comparison, dockedLigand.conformationBindingEnergies, refLigand.conformationBindingEnergies);
		}
	}

	private static void printHeader() {
		System.out.print("Name\tWeight\tSign\t");
		for (int i = 0; i < 9; i++) {
			System.out.format("REf BE %d\t", i+1);
		}
		for (int i = 0; i < 9; i++) {
			System.out.format("Docked BE %d\t", i+1);
		}
		System.out.println();
	}

	private static void printBindingEnergies(String dockedName, float[] compare, float[] dockedBindingEnergies, float[] refBindingEnergies) {
		System.out.format("%s\t", dockedName);
	//	System.out.format("%d\t",position);
		System.out.format("%.3f\t",compare[0]);
		System.out.format("%.3f\t",compare[1]);
		for (int i=0;i<refBindingEnergies.length;i++){
			if (refBindingEnergies[i]!=0) {
				System.out.format("%.3f\t", refBindingEnergies[i]);
			} else {
				System.out.format(" \t");
			}
		}

		for (int i=0;i<dockedBindingEnergies.length;i++){
			if (dockedBindingEnergies[i]!=0) {
				System.out.format("%.3f\t", dockedBindingEnergies[i]);
			} else {
				System.out.format(" \t");
			}
		}
		System.out.print("\n");

	}
	
	public static float[] compareBE(float[] refBE, float[] dockedBE){
		float result[] = new float[2];
		float firstRef=refBE[0];
		float firstDocked= dockedBE[0];
		if(firstRef> firstDocked){
			result[0]=-1;
		}else if(firstDocked> firstRef){
			result[0]=1;
		}
		//int refIndex=1;
		//int dockIndex=1;
		int firstIndex=0;
		int secondIndex=0;
		for(int i=0;i<9;i++){
			float ref= refBE[i];
			float docked= dockedBE[i];
			/*
			 * -5	-7
			 * -5	-7
			 * -5	-6
			 * */
			if(Math.abs(firstDocked)> Math.abs(firstRef) &&ref==firstRef){
				firstIndex++;
			}
			if(Math.abs(firstRef)> Math.abs(firstDocked) &&docked==firstDocked){
				secondIndex++;
			}
		}
		float difference=0;
		difference=firstIndex-secondIndex;
		result[1]=difference;
		return result;
	}
	// old with weight
	//	public static float[] compareBE(float[] refBE, float[] dockedBE){
	//		float result[] = new float[2];
	//		float signSum=0;
	//		float weightSum=0;
	//		float difference=0;
	//		
	//		for(int i=0;i<9;i++){
//			float ref= refBE[i];
//			float docked= dockedBE[i];
//			if(ref==0 || docked==0){
//				break;
//			}
//
//			difference=ref-docked;
//			weightSum+=difference;
//			signSum+=Math.signum(difference);
//		}
//		result[0]=weightSum;
//		result[1]=signSum;
//		return result;
//	}
}