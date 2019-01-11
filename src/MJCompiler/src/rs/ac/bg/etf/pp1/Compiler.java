package rs.ac.bg.etf.pp1;

import java.io.*;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler
{
	public static void main(String[] args) throws Exception
	{
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigProdFile());
		//Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
		Logger log = Logger.getLogger(Compiler.class);
		
		CLIParser cliParser = new CLIParser(args);
		
		if (cliParser.help())
		{
			log.info(CLIParser.HELP);
			return;
		}
		
		if (!cliParser.passed())
		{
			log.info(CLIParser.ERROR);
			return;
		}
		
		File inputFile = new File(cliParser.getInputFile());
		
		if (!inputFile.isFile() || !inputFile.exists() || !inputFile.canRead() || !cliParser.getInputFile().toLowerCase().endsWith(".mj"))
		{
			log.info("Invalid input file. Make sure it has \'.mj\' extension, and that it actually exists.");
			return;
		}
		
		File outputFile = new File(cliParser.getOutputFile().toLowerCase().endsWith(".obj") ? cliParser.getOutputFile() : cliParser.getOutputFile() + ".obj");
		
		if (!cliParser.getOutputFile().contains(":\\") || cliParser.getOutputFile().contains("\\\\") || cliParser.getOutputFile().endsWith("\\.obj") || outputFile.exists() || outputFile.getParentFile() == null || !outputFile.getParentFile().exists())
		{
			log.info("Invalid output file. Make sure the file path is correct, such directory exists and the file itself does not exist.");
			return;
		}
		
		Reader inputFileReader = null;
		
		try
		{
			log.info("Compiling source file: " + inputFile.getAbsolutePath());
			
			inputFileReader = new BufferedReader(new FileReader(inputFile));
			Yylex lexer = new Yylex(inputFileReader);
			
			MJParser parser = new MJParser(lexer);
			Symbol symbol = parser.parse();
			
			Program prog = (Program)(symbol.value);
			
			Tab.init();
			Extensions.init();
			
			SemanticAnalyzer analyzer = new SemanticAnalyzer();
			prog.traverseBottomUp(analyzer);
			
			if (!analyzer.isMainFound())
			{
				analyzer.report_error("Semantic error: no main method found");
			}
			
			if (!parser.errorDetected && analyzer.passed())
			{
				if (cliParser.dumpTree())
				{
					log.info(prog.toString(""));
				}
				
				if (cliParser.dumpSymbols())
				{
					Tab.dump();
				}
				
				// redundant
				if (outputFile.exists()) outputFile.delete();
				
				CodeGenerator generator = new CodeGenerator(analyzer.getnVars());
				prog.traverseBottomUp(generator);
				
				Code.dataSize = generator.getDataSize();
				Code.mainPc = generator.getMainPC();
				Code.write(new FileOutputStream(outputFile));
				
				log.info("Compilation done! Output: " + outputFile.getAbsolutePath());
			}
			else log.info("Compilation failed due to one or more errors.");
		}
		finally
		{
			if (inputFileReader != null)
			{
				try { inputFileReader.close(); }
				catch (IOException ex) { log.error(ex.getMessage(), ex); }
			}
		}
	}
}
