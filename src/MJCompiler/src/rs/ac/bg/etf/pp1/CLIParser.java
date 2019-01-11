package rs.ac.bg.etf.pp1;

public final class CLIParser
{
	public static final String HELP =
		"Compiler for MicroJava. Compiles MicroJava codes from *.mj files to *.obj files\r\n\r\n" +
		
			"\t/help\t\tDisplays options for the compiler\r\n" +
			"\t/in:<filepath>\t\tInput file used for compilation\r\n" +
			"\t/out:<filepath>\t\tOutput file to be produced (will add .obj extension if it is not)\r\n" +
			"\t/dumptree\t\tDump parsed tree if the parsing was successful\r\n" +
			"\t/dumpsymbols\t\tDump symbol table if the parsing was successful";
	
	public static final String ERROR =
		"Arguments unknown, not specified or invalid format. Use /help for details.";
	
	private String inputFile = "";
	private String outputFile = "";
	private boolean dumpTree = false;
	private boolean dumpSymbols = false;
	private boolean help = false;
	
	private void rollback()
	{
		inputFile = "";
		outputFile = "";
		dumpTree = false;
		dumpSymbols = false;
		help = false;
	}
	
	public CLIParser(String[] args)
	{
		for (String arg : args)
		{
			if (arg.toLowerCase().equals("/help"))
			{
				if (args.length == 1)
				{
					help = true;
				}
				else
				{
					rollback();
					break;
				}
			}
			else if (arg.toLowerCase().startsWith("/in:"))
			{
				inputFile = arg.substring(4);
				if (inputFile.toLowerCase().endsWith(".mj") && outputFile.equals(""))
					outputFile = inputFile.substring(0, inputFile.length() - 3) + ".obj";
			}
			else if (arg.toLowerCase().startsWith("/out:"))
			{
				outputFile = arg.substring(5);
			}
			else if (arg.toLowerCase().equals("/dumptree"))
			{
				dumpTree = true;
			}
			else if (arg.toLowerCase().equals("/dumpsymbols"))
			{
				dumpSymbols = true;
			}
			else
			{
				rollback();
				break;
			}
		}
		
		if (!outputFile.equals("") && !outputFile.contains(":\\"))
			outputFile = System.getProperty("user.dir") + "\\" + outputFile;
	}
	
	public String getInputFile()
	{
		return inputFile;
	}
	
	public String getOutputFile()
	{
		return outputFile;
	}
	
	public boolean dumpTree()
	{
		return dumpTree;
	}
	
	public boolean dumpSymbols()
	{
		return dumpSymbols;
	}
	
	public boolean help()
	{
		return help;
	}
	
	public boolean passed()
	{
		return !inputFile.equals("") && !outputFile.equals("");
	}
}
