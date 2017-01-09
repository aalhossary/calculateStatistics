import java.io.File;
import java.util.ArrayList;

public class CalculateNP {
	static CalculateRMSD.ReceptorLigandPairData refLigand = null;
//	static CalculateRMSD.ReceptorLigandPairData dockedLigand = null;
//	ArrayList<CalculateRMSD.ReceptorLigandPairData> refLigands;
//	ArrayList<CalculateRMSD.ReceptorLigandPairData> dockedLigands;

	public static void main(String[] args) {
		String refFolderString= Utilities.getParameter(args, "-ref", null, "./");
		String dockedFolderString = Utilities.getParameter(args, "-docked", null, "./");
		File refFolder = new File(refFolderString);
		File dockedFolder = new File(dockedFolderString);
		System.out.println("Name\tHeavyAtoms\tActiveBonds\tPositionOfNearest\tRMSD1\tRMSD2\tRMSD3\tRMSD4\tRMSD5\tRMSD6\tRMSD7\tRMSD8\tRMSD9");

		// loop on the ligand files
	//	ArrayList<Float> rmsds = new ArrayList<Float>();
		for(int i=1;i<=54529;i++){
			ArrayList<Float> rmsds = new ArrayList<Float>(9);
			File dockedFile = new File(dockedFolder, String.format("MBSC_%06d_lig_docked_qvina02_octree_5A1N_4N5N_X4_large_search_space_crystal.pdbqt", i)); 
			File refFile = new File(refFolder, String.format("MBSC_%06d_lig_docked_vina112_large_search_space_crystal.pdbqt", i)); 
			if (!dockedFile.exists()){
				System.out.println("Docked file not eexist " + dockedFile.getName());
				continue;
			}
			if(!refFile.exists()){
				System.out.println("Ref file not exist " + refFile.getName());
				continue;
			}
			try {
				refLigand=CalculateRMSD.parseFile(refFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			CalculateRMSD.ReceptorLigandPairData dockedLigand = null;
			try {
				dockedLigand = CalculateRMSD.parseFile(dockedFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for(int x=0; x< dockedLigand.allAtoms.size();x++){
				float r = CalculateRMSD.calculateRMSD(refLigand.allAtoms.get(0), dockedLigand.allAtoms.get(x));
				rmsds.add(r);
			}
			printRMSD(dockedFile,rmsds, dockedLigand.noHeavyAtoms, dockedLigand.noActiveBonds);
		}
	}
	

	private static int positionOfNearest(ArrayList<Float> rmsds){
		float smallest=rmsds.get(0);
		int position=1;
		for (int i=1;i<rmsds.size();i++){
			if(smallest >rmsds.get(i)){
				smallest= rmsds.get(i);
		//		System.out.println("this is the smallest '"+ smallest + "in  " + i);
				position = i;
			}
		}
		return position;
	}
	private static void printRMSD(File dockedFileName, ArrayList<Float> rmsds, int heavyAtoms, int activeBonds) {
	//	System.out.println("Name\tPositionOfNearest\tRMSD1\tRMSD2\tRMSD3\tRMSD4\tRMSD5\tRMSD6\tRMSD7\tRMSD8\tRMSD9");
		System.out.print(dockedFileName.getName());
		System.out.print("\t");
		System.out.print(heavyAtoms);
		System.out.print("\t");
		System.out.print(activeBonds);
		System.out.print("\t");
		System.out.print(positionOfNearest(rmsds));
		System.out.print("\t");
		for (int i=0;i<rmsds.size();i++){
			System.out.print(rmsds.get(i));
			System.out.print("\t");
		}
		System.out.print("\n");
		
	}
}


