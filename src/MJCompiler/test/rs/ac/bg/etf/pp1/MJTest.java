package rs.ac.bg.etf.pp1;

import java.io.*;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class MJTest
{
	static
	{
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception
	{
		Logger log = Logger.getLogger(MJTest.class);
		Reader br = null;
		
		try
		{
			File sourceCode = new File("test/program.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
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
			
			//log.info(prog.toString(""));
			//Tab.dump();
			
			if (!parser.errorDetected && analyzer.passed())
			{
				File objFile = new File("test/program.obj");
				
				if (objFile.exists()) objFile.delete();
				
				CodeGenerator generator = new CodeGenerator(analyzer.getnVars());
				prog.traverseBottomUp(generator);
				
				Code.dataSize = generator.getDataSize();
				Code.mainPc = generator.getMainPC();
				Code.write(new FileOutputStream(objFile));
				
				log.info("Compilation done!");
			}
			else log.info("Compilation failed due to one or more errors.");
		}
		finally
		{
			if (br != null)
			{
				try { br.close(); }
				catch (IOException ex) { log.error(ex.getMessage(), ex); }
			}
		}
	}
}

// java -cp lib/mj-runtime.jar rs.etf.pp1.mj.runtime.Run test/program.obj
// java -cp lib/mj-runtime.jar rs.etf.pp1.mj.runtime.disasm test/program.obj >test/program.asm
