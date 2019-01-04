package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor
{
	private int paramCount;
	public int getParamCount()
	{
		return paramCount;
	}
	
	private int varCount;
	public int getVarCount() { return varCount; }
	
	public void visit(ParamDeclNode node) { ++paramCount; }
	public void visit(VarDeclNode node) { ++varCount; }
}
