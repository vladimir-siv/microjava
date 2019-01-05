package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor
{
	// ======= [S] GLOBAL =======
	
	private int mainPc;
	public int getMainPC() { return mainPc; }
	
	// ======= [E] GLOBAL =======
	
	
	// ======= [S] PERMA LEAVES =======
	
	public void visit(DesignatorNode node)
	{
		SyntaxNode parent = node.getParent();
		
		if (!(parent instanceof AssignmentNode) && !(parent instanceof FuncCallNode))
		{
			Code.load(node.obj);
		}
	}
	
	// ======= [E] PERMA LEAVES =======
	
	
	// ======= [S] CONSTANTS =======
	
	public void visit(ConstantFactorNode node)
	{
		Obj cnst = Tab.insert(Obj.Con, "$", node.struct);
		
		cnst.setLevel(0);
		Extensions.UpdateConstantValue( node.getConstValue(), cnst);
		
		Code.load(cnst);
	}
	
	// ======= [E] CONSTANTS =======
	
	
	// ======= [S] METHODS =======
	
	public void visit(MethodDeclNode node)
	{
		if (node.getMethodName().equalsIgnoreCase("main"))
		{
			mainPc = Code.pc;
		}
		
		node.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables
		SyntaxNode methodNode = node.getParent();
		
		CounterVisitor counter = new CounterVisitor();
		methodNode.traverseTopDown(counter);
		
		// Generate method entry (enter instruction)
		Code.put(Code.enter);
		Code.put(counter.getParamCount());
		Code.put(counter.getParamCount() + counter.getVarCount());
	}
	public void visit(MethodNode node)
	{
		Code.put(Code.exit);
		Code.put(Code.return_);
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
	
	// ======= [E] METHODS =======
	
	
	// ======= [S] STATEMENTS =======
	
	public void visit(AssignmentNode node)
	{
		Code.store(node.getDesignator().obj);
	}
	public void visit(IncrementNode node)
	{
		ConstantFactorNode cnst = new ConstantFactorNode(new IntConstNode(1));
		visit(cnst);
		Code.put(Code.add);
		Code.store(node.getDesignator().obj);
	}
	public void visit(DecrementNode node)
	{
		ConstantFactorNode cnst = new ConstantFactorNode(new IntConstNode(1));
		visit(cnst);
		Code.put(Code.sub);
		Code.store(node.getDesignator().obj);
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
	
	public void visit(AddExprNode node)
	{
		if (node.getAddop() instanceof PlusNode) Code.put(Code.add);
		else if (node.getAddop() instanceof MinusNode) Code.put(Code.sub);
	}
	public void visit(ExprNode node)
	{
		if (node.getUnaryop() instanceof UnaryMinusNode) Code.put(Code.neg);
	}
	
	public void visit(MulTermNode node)
	{
		if (node.getMulop() instanceof  MultiplyNode) Code.put(Code.mul);
		else if (node.getMulop() instanceof DivideNode) Code.put(Code.div);
		else if (node.getMulop() instanceof ModuloNode) Code.put(Code.rem);
	}
	
	public void visit(FuncCallNode node)
	{
		Obj functionObj = node.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if (node.getParent() instanceof CallNode)
		{
			if (functionObj.getType() != Tab.noType)
			{
				Code.put(Code.pop);
			}
		}
	}
	
	// ======= [E] STATEMENTS =======
}
