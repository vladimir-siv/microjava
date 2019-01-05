package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor
{
	// ======= [S] GLOBAL =======
	
	private Logger log = Logger.getLogger(getClass());
	
	private boolean errorDetected = false;
	public boolean passed() { return !errorDetected; }
	
	private int nVars;
	public int getnVars() { return nVars; }
	
	// ======= [E] GLOBAL =======
	
	
	// ======= [S] ERROR REPORTING =======
	
	public void report_error(String message) { report_error(message, null); }
	public void report_error(String message, SyntaxNode info)
	{
		errorDetected = true;
		
		StringBuilder msg = new StringBuilder(message);
		int line = info == null ? 0 : info.getLine();
		
		if (line != 0)
		{
			msg
				.append(" on line ")
				.append(line);
		}
		
		log.error(msg.toString());
	}
	public void report_info(String message) { report_info(message, null); }
	public void report_info(String message, SyntaxNode info)
	{
		StringBuilder msg = new StringBuilder(message);
		int line = info == null ? 0 : info.getLine();
		
		if (line != 0)
		{
			msg
				.append(" on line ")
				.append(line);
		}
		
		log.info(msg.toString());
	}
	
	// ======= [E] ERROR REPORTING =======
	
	
	// ======= [S] PROGRAM =======
	
	public void visit(ProgDeclNode node)
	{
		node.obj = Tab.insert(Obj.Prog, node.getProgName(), Tab.noType);
		Tab.openScope();
	}
	public void visit(ProgramNode node)
	{
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(node.getProgDecl().obj);
		Tab.closeScope();
	}
	
	// ======= [E] PROGRAM =======
	
	
	// ======= [S] PERMA LEAVES =======
	
	public void visit(TypeNode node)
	{
		Obj obj = Tab.find(node.getTypeName());
		
		if (obj != Tab.noObj)
		{
			if (obj.getKind() == Obj.Type)
			{
				node.struct = obj.getType();
			}
			else
			{
				report_error("Error: Name " + node.getTypeName() + " does not represent a type", node);
				node.struct = Tab.noType;
			}
		}
		else
		{
			report_error("Type \'" + node.getTypeName() + "\' has not been defined");
			node.struct = Tab.noType;
		}
	}
	public void visit(DesignatorNode node)
	{
		Obj obj = Tab.find(node.getName());
		
		if (obj == Tab.noObj)
		{
			report_error("Error on line " + node.getLine() + ": name \'" + node.getName() + "\' has not been declared");
		}
		
		node.obj = obj;
	}
	
	// ======= [E] PERMA LEAVES =======
	
	
	// ======= [S] CONSTANTS =======
	
	public void visit(IntConstNode node)
	{
		node.struct = Tab.intType;
	}
	public void visit(CharConstNode node)
	{
		node.struct = Tab.charType;
	}
	public void visit(BoolConstNode node)
	{
		node.struct = Extensions.boolType;
	}
	
	private Type constTypeNode = null;
	public void visit(ConstSectDeclNode node)
	{
		constTypeNode = node.getType();
	}
	public void visit(ConstDeclNode node)
	{
		ConstValue constValue = node.getConstValue();
		if (constValue.struct == constTypeNode.struct)
		{
			Obj declared = Tab.currentScope.findSymbol(node.getConstName());
			
			if (declared == Tab.noObj || declared == null)
			{
				Obj obj = Tab.insert(Obj.Con, node.getConstName(), constTypeNode.struct);
				Extensions.UpdateConstantValue(constValue, obj);
			}
			else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
		}
		else report_error("Error on line " + node.getLine() + ": invalid constant type");
	}
	
	// ======= [E] CONSTANTS =======
	
	
	// ======= [S] VARIABLES =======
	
	private Type varTypeNode = null;
	public void visit(VarSectDeclNode node)
	{
		varTypeNode = node.getType();
	}
	public void visit(VarDeclNode node)
	{
		Obj declared = Tab.currentScope.findSymbol(node.getVarName());
		
		if (declared == Tab.noObj || declared == null)
		{
			Tab.insert(Obj.Var, node.getVarName(), varTypeNode.struct);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
	}
	
	// ======= [E] VARIABLES =======
	
	
	// ======= [S] METHODS =======
	
	private Obj currentMethod = null;
	private boolean returnFound = false;
	
	private boolean mainFound = false;
	public boolean isMainFound() { return mainFound; }
	
	public void visit(MethodDeclNode node)
	{
		if (node.getMethodName().equalsIgnoreCase("main"))
		{
			mainFound = true;
			if (node.getMethodType().struct != Tab.noType)
			{
				report_error("Error on line " + node.getLine() + ": main must be of type void");
			}
		}
		
		Obj declared = Tab.currentScope.findSymbol(node.getMethodName());
		
		if (declared == Tab.noObj || declared == null)
		{
			node.obj = Tab.insert(Obj.Meth, node.getMethodName(), node.getMethodType().struct);
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
			node.obj = Tab.noObj;
		}
		
		Tab.openScope();
		currentMethod = node.obj;
	}
	public void visit(MethodNode node)
	{
		if (!returnFound && currentMethod.getType() != Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": function \'" + currentMethod.getName() + "\' does not have a return statement");
		}
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		returnFound = false;
		currentMethod = null;
	}
	public void visit(TypedMethodNode node)
	{
		node.struct = node.getType().struct;
	}
	public void visit(VoidMethodNode node)
	{
		node.struct = Tab.noType;
	}
	public void visit(ReturnExprNode node)
	{
		returnFound = true;
		Struct currentMethodType = currentMethod.getType();
		
		if (currentMethodType == Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": return type of this function is void");
		}
		else if (!currentMethodType.compatibleWith(node.getExpr().struct))
		{
			report_error("Error on line " + node.getLine() + ": expression type in return statement does not match with the return type of the surrounding function \'" + currentMethod.getName() + "\'");
		}
	}
	public void visit(ReturnVoidNode node)
	{
		returnFound = true;
		Struct currentMethodType = currentMethod.getType();
		
		if (currentMethodType != Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": must return an expression");
		}
	}
	
	// ======= [E] METHODS =======
	
	
	// ======= [S] STATEMENTS =======
	
	public void visit(AssignmentNode node)
	{
		if (!node.getExpr().struct.assignableTo(node.getDesignator().obj.getType()))
		{
			report_error("Error on line " + node.getLine() + ": cannot do the assignment due to incompatible types");
		}
	}
	public void visit(IncrementNode node)
	{
		if (node.getDesignator().obj.getType() != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": increment can only be applied on int");
		}
	}
	public void visit(DecrementNode node)
	{
		if (node.getDesignator().obj.getType() != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": decrement can only be applied on int");
		}
	}
	
	public void visit(PrintNode node)
	{
		if
		(
			node.getExpr().struct != Tab.intType
			&&
			node.getExpr().struct != Tab.charType
			&&
			node.getExpr().struct != Extensions.boolType
			&&
			node.getExpr().struct != Extensions.enumType
		)
		{
			report_error("Error on line " + node.getLine() + ": Operand of PRINT instruction has to be an int, char, bool or enum");
		}
	}
	public void visit(ReadNode node)
	{
		if
		(
			node.getDesignator().obj.getType() != Tab.intType
			&&
			node.getDesignator().obj.getType() != Tab.charType
			&&
			node.getDesignator().obj.getType() != Extensions.boolType
		)
		{
			report_error("Error on line " + node.getLine() + ": Operand of READ instruction has to be an int, char or bool");
		}
	}
	
	public void visit(AddExprNode node)
	{
		Struct exprType = node.getExpr().struct;
		Struct termType = node.getTerm().struct;
		
		if (exprType == Tab.intType && termType == Tab.intType)
		{
			node.struct = Tab.intType;
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": addition/subtraction can only be done on ints");
			node.struct = Tab.noType;
		}
	}
	public void visit(ExprNode node)
	{
		node.struct = node.getTerm().struct;
	}
	
	public void visit(MulTermNode node)
	{
		Struct termType = node.getTerm().struct;
		Struct factorType = node.getFactor().struct;
		
		if (termType == Tab.intType && factorType == Tab.intType)
		{
			node.struct = Tab.intType;
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": multiplication/division/modulo can only be done on ints");
			node.struct = Tab.noType;
		}
	}
	public void visit(TermNode node)
	{
		node.struct = node.getFactor().struct;
	}
	
	public void visit(ConstantFactorNode node)
	{
		node.struct = node.getConstValue().struct;
	}
	public void visit(VariableFactorNode node)
	{
		node.struct = node.getDesignator().obj.getType();
	}
	public void visit(CallFactorNode node)
	{
		Obj obj = ((FuncCallNode)node.getFuncCall()).getDesignator().obj;
		
		if (obj.getKind() == Obj.Meth)
		{
			if (obj.getType() == Tab.noType)
			{
				// is void func
				report_error("Error: \'" + obj.getName() + "\' cannot be used in expressions because it does not have a return type (it is void)", node);
			}
			else node.struct = obj.getType();
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": name \'" + obj.getName() + "\' is not a function");
			node.struct = Tab.noType;
		}
	}
	public void visit(PriorityFactorNode node)
	{
		node.struct = node.getExpr().struct;
	}
	
	// ======= [E] STATEMENTS =======
}
