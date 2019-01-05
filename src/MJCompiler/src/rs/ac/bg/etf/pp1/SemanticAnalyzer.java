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
	
	private void report_error(String message) { report_error(message, null); }
	private void report_error(String message, SyntaxNode info)
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
	private void report_info(String message) { report_info(message, null); }
	private void report_info(String message, SyntaxNode info)
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
	
	// ======= [E] CONSTANTS =======
	
	
	// ======= [S] VARIABLES =======
	
	private Type typeNode = null;
	
	public void visit(VarSectDeclNode node)
	{
		typeNode = node.getType();
	}
	
	public void visit(VarDeclNode node)
	{
		Tab.insert(Obj.Var, node.getVarName(), typeNode.struct);
	}
	
	// ======= [E] VARIABLES =======
	
	
	// ======= [S] METHODS =======
	
	private Obj currentMethod = null;
	private boolean returnFound = false;
	
	public void visit(MethodDeclNode node)
	{
		node.obj = Tab.insert(Obj.Meth, node.getMethodName(), node.getMethodType().struct);
		Tab.openScope();
		
		currentMethod = node.obj;
	}
	public void visit(MethodNode node)
	{
		if (!returnFound && currentMethod.getType() != Tab.noType)
		{
			report_error("Semantic error on line " + node.getLine() + ": function \'" + currentMethod.getName() + "\' does not have a return statement");
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
	
	public void visit(PrintNode node)
	{
		if (node.getExpr().struct != Tab.intType && node.getExpr().struct != Tab.charType)
		{
			report_error("Semantic error on line " + node.getLine() + ": Operand of PRINT instruction has to be char or int");
		}
	}
	
	public void visit(AddExprNode node)
	{
		Struct exprType = node.getExpr().struct;
		Struct termType = node.getTerm().struct;
		
		if (exprType.equals(termType) && termType == Tab.intType)
		{
			node.struct = exprType;
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": types not compatible for such operator");
			node.struct = Tab.noType;
		}
	}
	public void visit(ExprNode node)
	{
		node.struct = node.getTerm().struct;
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
				report_error("Semantic error: \'" + obj.getName() + "\' cannot be used in expressions because it does not have a return type (it is void)", node);
			}
			else node.struct = obj.getType();
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": name \'" + obj.getName() + "\' is not a function");
			node.struct = Tab.noType;
		}
	}
	
	// ======= [E] STATEMENTS =======
}
