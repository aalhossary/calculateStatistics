
public final class Utilities {
	
	
	public static String getParameter(String[] args, String parameter, String defaultValueIfParameterFound, String valueIfNotFound) {
		boolean flagFound=false;
		String input= defaultValueIfParameterFound;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(parameter)) {
				flagFound=true;
				if (i+1 >= args.length) {
					break;
				}
				String nextArg = args[i+1];
				if (nextArg.startsWith("-")) {
					break;
				} else {
					input = nextArg;
				}
				i+=1;
			}
		}
		if (!flagFound) {
			return valueIfNotFound;
		}
		return input;
	}

}
