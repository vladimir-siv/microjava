package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor
{
	protected int count;
	
	public int getCount()
	{
		return count;
	}
	
	public static class ParamCounter extends CounterVisitor
	{
		public void visit(ParamDeclNode node) { ++count; }
	}
	
	public static class VarCounter extends CounterVisitor
	{
		public void visit(VarDeclNode node) { ++count; }
	}
}
