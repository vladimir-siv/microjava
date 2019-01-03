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
			
			MJParser p = new MJParser(lexer);
			Symbol s = p.parse();
			
			Program prog = (Program)(s.value);
			Tab.init();
			
			log.info(prog.toString(""));
			log.info("===================================");
			
			SemanticPass v = new SemanticPass();
			prog.traverseBottomUp(v);
			
			log.info("===================================");
			Tab.dump();
			
			if (!p.errorDetected && v.passed())
			{
				log.info("Parsing completed with no errors.");
				
				File objFile = new File("test/program.obj");
				
				if (objFile.exists()) objFile.delete();
				
				CodeGenerator generator = new CodeGenerator();
				prog.traverseBottomUp(generator);
				
				Code.dataSize = v.getnVars();
				Code.mainPc = generator.getMainPC();
				Code.write(new FileOutputStream(objFile));
			}
			else
			{
				log.info("Parsing failed due to one or more errors.");
			}
		}
		finally
		{
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
	}
}