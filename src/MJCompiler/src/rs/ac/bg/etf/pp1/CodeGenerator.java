package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class CodeGenerator extends VisitorAdaptor
{
	// ======= [S] GLOBAL =======
	
	public CodeGenerator(int initialDataSize) { dataSize = initialDataSize; }
	
	private int dataSize;
	public int getDataSize() { return dataSize; }
	
	private int mainPc;
	public int getMainPC() { return mainPc; }
	
	private ArrayList<Consumer<Void>> vtable = new ArrayList<>();
	private void vtableInit()
	{
		for (int i = 0; i < vtable.size(); ++i)
		{
			vtable.get(i).accept(null);
		}
	}
	
	// ======= [E] GLOBAL =======
	
	
	// ======= [S] PERMA LEAVES =======
	
	public void visit(Designator node)
	{
		SyntaxNode parent = node.getParent();
		if
		(
			parent instanceof DesignatorStatement
			||
			parent instanceof ReadNode
			||
			parent instanceof CalleeNode
			||
			node.obj.getType() == Extensions.enumType	// only should be able to happen with DesignatorNode
		) return;
		
		Code.load(node.obj);
	}
	public void visit(DesignatorNode node)
	{
		if (node.obj.getKind() == Obj.Fld || (inClass && node.obj.getKind() == Obj.Meth))
		{
			String designatorName = node.obj.getName();
			
			if
			(
				!designatorName.equals("ord")
				&&
				!designatorName.equals("chr")
				&&
				!designatorName.equals("len")
			)
			{
				// implicit this
				Code.put(Code.load_n);
			}
		}
		
		visit((Designator)node);
	}
	public void visit(DesignatorIndexingNode node) { visit((Designator)node); }
	public void visit(DesignatorChainNode node) { visit((Designator)node); }
	
	// ======= [E] PERMA LEAVES =======
	
	
	// ======= [S] CONSTANTS =======
	
	public void visit(ConstantFactorNode node)
	{
		//Obj cnst = Tab.insert(Obj.Con, "$", node.struct);
		//cnst.setLevel(0);
		
		Obj cnst = new Obj(Obj.Con, "$", node.struct);
		Extensions.UpdateConstantValue(node.getConstValue(), cnst);
		Code.load(cnst);
	}
	
	// ======= [E] CONSTANTS =======
	
	
	// ======= [S] CLASSES =======
	
	private boolean inClass = false;
	public void visit(ClassDeclNode node)
	{
		inClass = true;
		
		// Set vtp
		node.obj.setAdr(dataSize);
		
		Iterator<Obj> enumerator = node.obj.getType().getMembers().symbols().iterator();
		
		while (enumerator.hasNext())
		{
			Obj method = enumerator.next();
			
			if (method.getKind() == Obj.Meth)
			{
				dataSize += method.getName().length() + 2;
			}
		}
		
		// for -2
		++dataSize;
	}
	public void visit(ClassNode node)
	{
		// Generate the vtable
		vtable.add(e ->
		{
			int position = node.obj.getAdr();
			
			Iterator<Obj> enumerator = node.obj.getType().getMembers().symbols().iterator();
			
			while (enumerator.hasNext())
			{
				Obj method = enumerator.next();
				
				if (method.getKind() == Obj.Meth)
				{
					// Generate vtp structure for this method
					String methodName = method.getName();
					
					// Generate method name
					for (int i = 0; i < methodName.length(); ++i)
					{
						int ascii = (int) methodName.charAt(i);
						
						Code.loadConst(ascii);
						Code.put(Code.putstatic); Code.put2(position++);
					}
					
					// Generate -1 (end of method)
					Code.loadConst(-1);
					Code.put(Code.putstatic); Code.put2(position++);
					
					// Generate the address where the method resides
					Code.loadConst(method.getAdr());
					Code.put(Code.putstatic); Code.put2(position++);
				}
			}
			
			Code.loadConst(-2);
			Code.put(Code.putstatic); Code.put2(position++);
		});
		
		inClass = false;
	}
	public void visit(InterfaceDeclNode node)
	{
		inClass = true;
	}
	public void visit(InterfaceNode node)
	{
		inClass = false;
	}
	
	// ======= [E] CLASSES =======
	
	
	// ======= [S] METHODS =======
	
	public void visit(MethodDeclNode node)
	{
		if (node.obj.getLevel() < 0) return;
		
		boolean initVTable = false;
		
		if (!inClass && node.getMethodName().equals("main"))
		{
			mainPc = Code.pc;
			initVTable = true;
		}
		
		node.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables
		SyntaxNode methodNode = node.getParent().getParent();
		
		CounterVisitor counter = new CounterVisitor();
		methodNode.traverseTopDown(counter);
		
		if (counter.getParamCount() != node.obj.getLevel())
		{
			org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(getClass());
			log.info("Warning: parameter count does not match visitor param count.");
		}
		
		// Generate method entry (enter instruction)
		Code.put(Code.enter);
		Code.put((inClass ? 1 : 0) + counter.getParamCount());
		Code.put((inClass ? 1 : 0) + counter.getParamCount() + counter.getVarCount());
		
		if (initVTable) vtableInit();
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
		if (node.obj.getType() == Tab.noType)
		{
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
	}
	
	// ======= [E] METHODS =======
	
	
	// ======= [S] STATEMENTS =======
	
	// ### DesignatorStatements
	
	public void visit(AssignmentNode node)
	{
		Code.store(node.getDesignator().obj);
	}
	public void visit(IncrementNode node)
	{
		if (node.getDesignator() instanceof DesignatorIndexingNode) Code.put(Code.dup2);
		if (node.getDesignator().obj.getKind() == Obj.Fld) Code.put(Code.dup);
		Code.load(node.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(node.getDesignator().obj);
	}
	public void visit(DecrementNode node)
	{
		if (node.getDesignator() instanceof DesignatorIndexingNode) Code.put(Code.dup2);
		if (node.getDesignator().obj.getKind() == Obj.Fld) Code.put(Code.dup);
		Code.load(node.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(node.getDesignator().obj);
	}
	
	// ### DesignatorStatements
	
	// ### If
	
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
	public void visit(IfNode node) { Code.fixup(lastJumpAddresses.pop()); }
	public void visit(IfElseNode node) { Code.fixup(lastJumpAddresses.pop()); }
	
	// ### If
	
	// ### For
	
	private static class ForContext
	{
		public int conditionAddress = -1;
		public int stepAddress = -1;
		public int bodyAddress = -1;
		public int forEndAddress = -1;
		
		public List<Integer> breaks = new ArrayList<>();
		
		public ForContext()
		{
			conditionAddress = Code.pc;
		}
		
		public void beginBody()
		{
			Code.fixup(bodyAddress);
			bodyAddress = Code.pc;
		}
		
		public void endFor()
		{
			if (forEndAddress != -1) Code.fixup(forEndAddress);
			for (int breakAddress : breaks) Code.fixup(breakAddress);
			forEndAddress = Code.pc;
		}
	}
	
	private Stack<ForContext> forContexts = new Stack<>();
	
	public void visit(ForInitNode node) { visit((ForInit)node); }
	public void visit(EmptyForInitNode node) { visit((ForInit)node); }
	public void visit(ForInit node)
	{
		forContexts.push(new ForContext());
	}
	public void visit(ForConditionNode node) { visit((ForCondition)node); }
	public void visit(EmptyForConditionNode node) { visit((ForCondition)node); }
	public void visit(ForCondition node)
	{
		ForContext currentFor = forContexts.peek();
		
		if (node instanceof ForConditionNode)
		{
			// Condition check, still loop if value on estack != 0
			Code.loadConst(0);
			Code.putFalseJump(Code.ne, 0);
			currentFor.forEndAddress = Code.pc - 2;
		}
		
		// Jump on body (skip step)
		Code.putJump(0);
		currentFor.bodyAddress = Code.pc - 2;
		
		currentFor.stepAddress = Code.pc;
	}
	public void visit(ForStepNode node) { visit((ForStep)node); }
	public void visit(EmptyForStepNode node) { visit((ForStep)node); }
	public void visit(ForStep node)
	{
		ForContext currentFor = forContexts.peek();
		Code.putJump(currentFor.conditionAddress);
		currentFor.beginBody();
	}
	public void visit(ForNode node)
	{
		ForContext currentFor = forContexts.pop();
		Code.putJump(currentFor.stepAddress);
		currentFor.endFor();
	}
	
	public void visit(BreakNode node)
	{
		if (forContexts.size() == 0) return;
		Code.putJump(0);
		forContexts.peek().breaks.add(Code.pc - 2);
	}
	public void visit(ContinueNode node)
	{
		if (forContexts.size() == 0) return;
		Code.putJump(forContexts.peek().stepAddress);
	}
	
	// ### For
	
	// ### Regular statements
	
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
		else
		{
			Code.put(Code.new_);
			Code.put2(node.struct.getNumberOfFields() * 4);
			
			// Insert vtp address in the object
			
			// [FTP] solution
			int vtpAddress = Tab.currentScope.findSymbol(((TypeNode)node.getType()).getTypeName()).getAdr();
			
			Code.put(Code.dup);
			Code.loadConst(vtpAddress);
			Code.put(Code.putfield); Code.put2(0);
		}
	}
	public void visit(NullNode node)
	{
		Code.loadConst(0);
	}
	
	public void visit(FuncCallNode node)
	{
		CalleeNode callee = (CalleeNode)node.getCallee();
		boolean isVirtualCall = true;
		
		if (!inClass && callee.getDesignator() instanceof DesignatorNode)
		{
			isVirtualCall = false;
		}
		
		Obj functionObj = callee.getDesignator().obj;
		
		if
		(
			!functionObj.getName().equals("chr")
			&&
			!functionObj.getName().equals("ord")
		)
		{
			if (!functionObj.getName().equals("len"))
			{
				if (isVirtualCall)
				{
					String functionName = functionObj.getName();
					
					// load this
					Designator current = callee.getDesignator();
					current.traverseBottomUp(this);
					
					// get vtp
					Code.put(Code.getfield); Code.put2(0);
					
					// invoke
					Code.put(Code.invokevirtual);
					for (int i = 0; i < functionName.length(); ++i)
						Code.put4((int)functionName.charAt(i));
					Code.put4(-1);
				}
				else
				{
					int offset = functionObj.getAdr() - Code.pc;
					Code.put(Code.call);
					Code.put2(offset);
				}
			}
			else Code.put(Code.arraylength);
		}
		
		if (node.getParent() instanceof CallNode)
		{
			if (functionObj.getType() != Tab.noType)
			{
				Code.put(Code.pop);
			}
		}
	}
	
	// ### Regular statements
	
	// ======= [E] STATEMENTS =======
}
