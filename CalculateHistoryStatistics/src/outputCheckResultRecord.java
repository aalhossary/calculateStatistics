import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Vector;

public class outputCheckResultRecord {

	public static class CheckState{
		boolean firstCheck;
		boolean secondCheck;
		public CheckState(boolean firstCheck, boolean secondCheck) {
			this.firstCheck = firstCheck;
			this.secondCheck = secondCheck;
		}
	}
	
	Vector<Vector<CheckState>> allThreadsCheckStates= new Vector<Vector<CheckState>>();
	
	Scanner scanner;
	PrintStream out;
	public static void main(String[] args) {
		System.out.println("start");
		new outputCheckResultRecord(args);
		System.out.println("end");
	}

	private outputCheckResultRecord(String[] args) {
		readFiles(args[0]);
		System.out.println("finished reading, starting writing output");
		outputResults();
	}

	private void readFiles(String folder) {
		for (int i = 0; i < 64; i++) {
			Vector<CheckState> currentThreadChecks = new Vector<CheckState>();
			allThreadsCheckStates.addElement(currentThreadChecks);
			try {
				scanner= new Scanner(new File(folder,"history_"+i+"_small.txt"));
				System.out.println("reading file # "+i);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			boolean individualCheck=false;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				char state = line.charAt(21);
				if (individualCheck) {
					individualCheck=false;
					if(state=='N'){
						currentThreadChecks.lastElement().secondCheck=true;
						continue;
					}else {
						currentThreadChecks.lastElement().secondCheck=false;//not needed
					}
				}
				switch (state) {
				case 'G':
					currentThreadChecks.addElement(new CheckState(true, false));
					break;
					
				case 'g':
					currentThreadChecks.addElement(new CheckState(false, false));
					individualCheck=true;
					break;
					
				default:
					break;
				}
			}
			scanner.close();
		}
	}
	
	private void outputResults() {
		try {
			out= new PrintStream("checkResultRecords.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//write header
		for (int i = 0; i < 64; i++) {
			out.print(""+i+" G success");
			out.print('\t');
		}
		for (int i = 0; i < 64; i++) {
			out.print(""+i+" N success");
			if (i<63) {
				out.print('\t');
			}
		}
		out.println();
		
		int maxLength=0;
		for (Vector<CheckState> vector : allThreadsCheckStates) {
			if (maxLength < vector.size()){
				maxLength = vector.size();
			}
		}

		for (int i = 0; i < maxLength; i++) {
			for (int j = 0; j < 64; j++) {
				Vector<CheckState> threadVector = allThreadsCheckStates.elementAt(j);
				if (threadVector.size() > i) {
					boolean checkResult = threadVector.elementAt(i).firstCheck;
					out.print(checkResult ? "1" : "0");
				}
				out.print('\t');
			}
			for (int j = 0; j < 64; j++) {
				Vector<CheckState> threadVector = allThreadsCheckStates.elementAt(j);
				if (threadVector.size() > i) {
					boolean checkResult = threadVector.elementAt(i).secondCheck;
					out.print(checkResult ? "1" : "0");
				}
				if (j < 63) {
					out.print('\t');
				}
			}
			out.println();
		}
	}
}
