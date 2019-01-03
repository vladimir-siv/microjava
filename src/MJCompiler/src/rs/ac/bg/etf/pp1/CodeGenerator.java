package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.CounterVisitor.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor
{
	private int mainPc;
	
	public int getMainPC()
	{
		return mainPc;
	}
	
	public void visit(PrintNode node)
	{
		if (node.getExpr().struct == Tab.intType)
		{
			Code.loadConst(5);
			Code.put(Code.print);
		}
		else
		{
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}
	
	public void visit(ConstantNode node)
	{
		Obj cnst = Tab.insert(Obj.Con, "$", node.struct);
		cnst.setLevel(0);
		cnst.setAdr(node.getN1());
		
		Code.load(cnst);
	}
	
	public void visit(DesignatorNode node)
	{
		SyntaxNode parent = node.getParent();
		Class parentType = parent.getClass();
		
		if (parentType != AssignmentNode.class && parentType != FuncCallNode.class)
		{
			Code.load(node.obj);
		}
	}
	
	public void visit(MethodRegNode node)
	{
		if (node.getMethodName().equalsIgnoreCase("main"))
		{
			mainPc = Code.pc;
		}
		
		node.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables
		SyntaxNode methodNode = node.getParent();
		
		ParamCounter paramCnt = new ParamCounter();
		methodNode.traverseTopDown(paramCnt);
		
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		// Generate method entry (enter instruction)
		Code.put(Code.enter);
		Code.put(paramCnt.getCount());
		Code.put(paramCnt.getCount() + varCnt.getCount());
	}
	
	public void visit(MethodDeclNode node)
	{
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(AssignmentNode node)
	{
		Code.store(node.getDesignator().obj);
	}
	
	public void visit(FuncCallNode node)
	{
		Obj functionObj = node.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if (node.getParent() instanceof NoRetCallNode)
		{
			if (functionObj.getType() != Tab.noType)
			{
				Code.put(Code.pop);
			}
		}
	}
	
	public void visit(ReturnExprNode node)
	{
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnVoidNode node)
	{
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(OpExprNode node)
	{
		Code.put(Code.add);
	}
}
