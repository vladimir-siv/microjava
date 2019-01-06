package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.Stack;

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
		if
		(
			parent instanceof AssignmentNode
			||
			parent instanceof CalleeNode
			||
			node.obj.getType() == Extensions.enumType
		) return;
		
		Code.load(node.obj);
	}
	public void visit(DesignatorIndexingNode node)
	{
		SyntaxNode parent = node.getParent();
		if
		(
			parent instanceof AssignmentNode
			||
			parent instanceof CalleeNode				// this should not be able to happen
			||
			node.obj.getType() == Extensions.enumType	// this should not be able to happen
		) return;
		
		Code.load(node.obj);
	}
	public void visit(DesignatorChainNode node)
	{
		SyntaxNode parent = node.getParent();
		if
		(
			parent instanceof AssignmentNode
			||
			parent instanceof CalleeNode
			||
			node.obj.getType() == Extensions.enumType	// this should not be able to happen
		) return;
		
		Code.load(node.obj);
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
		if (node.getMethodName().equals("main"))
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
	public void visit(MethodNode node)
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
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(node.getDesignator().obj);
	}
	public void visit(DecrementNode node)
	{
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(node.getDesignator().obj);
	}
	
	private Stack<Integer> lastJumpAddresses = new Stack<>();
	public void visit(IfConditionNode node)
	{
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		lastJumpAddresses.add(Code.pc - 2);
	}
	public void visit(ElseNode node)
	{
		Code.putJump(0);
		int newJumpAddress = Code.pc - 2;
		Code.fixup(lastJumpAddresses.pop());
		lastJumpAddresses.push(newJumpAddress);
	}
	private void EndIf()
	{
		Code.fixup(lastJumpAddresses.pop());
	}
	public void visit(UnmatchedIfNode node) { EndIf(); }
	public void visit(UnmatchedIfElseNode node) { EndIf(); }
	public void visit(MatchedIfNode node) { EndIf(); }
	
	public void visit(PrintNode node)
	{
		PrintSpace printSpace = node.getPrintSpace();
		int space = 1;
		
		if (printSpace instanceof PrintSpaceNode)
		{
			space = ((PrintSpaceNode)printSpace).getValue();
		}
		
		if (node.getExpr().struct == Tab.charType)
		{
			Code.loadConst(space);
			Code.put(Code.bprint);
		}
		else
		{
			Code.loadConst(space);
			Code.put(Code.print);
		}
	}
	public void visit(ReadNode node)
	{
		if (node.getDesignator().obj.getType() == Tab.intType || node.getDesignator().obj.getType() == Extensions.boolType)
		{
			Code.put(Code.read);
		}
		else
		{
			Code.put(Code.bread);
		}
		
		Code.store(node.getDesignator().obj);
	}
	
	public void visit(ConditionOrNode node)
	{
		// A || B
		
		Code.loadConst(0);
		Code.putFalseJump(Code.eq, Code.pc + 15);
		// if (B == 0)
		// {
				Code.loadConst(0);
				Code.putFalseJump(Code.eq, Code.pc + 7);
				// if (A == 0)
				// {
						Code.loadConst(0);
				// }
				Code.putJump(Code.pc + 4);
				// else
				// {
						Code.loadConst(1);
				// }
		// }
		Code.putJump(Code.pc + 5);
		// else
		// {
				Code.put(Code.pop); // don't care A
				Code.loadConst(1);
		// }
	}
	public void visit(CondTermAndNode node)
	{
		// A && B
		
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, Code.pc + 15);
		// if (B != 0)
		// {
				Code.loadConst(0);
				Code.putFalseJump(Code.ne, Code.pc + 7);
				// if (A != 0)
				// {
						Code.loadConst(1);
				// }
				Code.putJump(Code.pc + 4);
				// else
				// {
						Code.loadConst(0);
				// }
		// }
		Code.putJump(Code.pc + 5);
		// else
		// {
				Code.put(Code.pop); // don't care A
				Code.loadConst(0);
		// }
	}
	public void visit(RelCondFactNode node)
	{
		Code.putFalseJump(node.getRelop(), Code.pc + 7);
		Code.loadConst(1);
		Code.putJump(Code.pc + 4);
		Code.loadConst(0);
	}
	
	public void visit(ExprNode node)
	{
		if (node.getUnaryop() instanceof UnaryMinusNode) Code.put(Code.neg);
	}
	public void visit(AddExprNode node)
	{
		if (node.getAddop() instanceof PlusNode) Code.put(Code.add);
		else if (node.getAddop() instanceof MinusNode) Code.put(Code.sub);
	}
	
	public void visit(MulTermNode node)
	{
		if (node.getMulop() instanceof  MultiplyNode) Code.put(Code.mul);
		else if (node.getMulop() instanceof DivideNode) Code.put(Code.div);
		else if (node.getMulop() instanceof ModuloNode) Code.put(Code.rem);
	}
	
	public void visit(NewNode node)
	{
		ArraySize arraySize = node.getArraySize();
		
		if (arraySize instanceof ArraySizeNode)
		{
			Code.put(Code.newarray);
			
			if (node.struct.getElemType() == Tab.charType)
			{
				Code.put(0);
			}
			else Code.put(1);
		}
		//else new for classes only...
	}
	
	public void visit(FuncCallNode node)
	{
		Obj functionObj = ((CalleeNode)node.getCallee()).getDesignator().obj;
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
