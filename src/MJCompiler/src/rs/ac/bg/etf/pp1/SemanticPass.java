package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor
{
	private Logger log = Logger.getLogger(getClass());
	
	private boolean errorDetected = false;
	
	private Obj currentMethod = null;
	private boolean returnFound = false;
	
	private int nVars;
	
	public boolean passed() { return !errorDetected; }
	public int getnVars() { return nVars; }
	
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
	
	public void visit(VarDeclNode node)
	{
		report_info("Var \'" + node.getVarName() + "\' declared", node);
		Obj varNode = Tab.insert(Obj.Var, node.getVarName(), node.getType().struct);
	}
	
	public void visit(PrintNode node)
	{
		if (node.getExpr().struct != Tab.intType && node.getExpr().struct != Tab.charType)
		{
			report_error("Semantic error on line " + node.getLine() + ": Operand of PRINT instruction has to be char or int.", null);
		}
	}
	
	public void visit(ProgNameNode node)
	{
		node.obj = Tab.insert(Obj.Prog, node.getName(), Tab.noType);
		Tab.openScope();
	}
	
	public void visit(ProgramNode node)
	{
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(node.getProgName().obj);
		Tab.closeScope();
	}
	
	public void visit(TypeNode node)
	{
		Obj typeNode = Tab.find(node.getTypeName());
		
		if (typeNode != Tab.noObj)
		{
			if (Obj.Type == typeNode.getKind())
			{
				node.struct = typeNode.getType();
			}
			else
			{
				report_error("Error: Name " + node.getTypeName() + " does not represent a type!", node);
				node.struct = Tab.noType;
			}
		}
		else
		{
			report_error("Type \'" + node.getTypeName() + "\' not found in symbol table", null);
			node.struct = Tab.noType;
		}
	}
	
	public void visit(MethodRegNode node)
	{
		currentMethod = Tab.insert(Obj.Meth, node.getMethodName(), node.getType().struct);
		node.obj = currentMethod;
		Tab.openScope();
		report_info("Processing function " + node.getMethodName(), node);
	}
	
	public void visit(MethodDeclNode node)
	{
		if (!returnFound && currentMethod.getType() != Tab.noType)
		{
			report_error("Semantic error on line " + node.getLine() + ": function \'" + currentMethod.getName() + "\' does not have a return statement!", null);
		}
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		returnFound = false;
		currentMethod = null;
	}
	
	public void visit(DesignatorNode node)
	{
		Obj obj = Tab.find(node.getName());
		
		if (obj == Tab.noObj)
		{
			report_error("Error on line " + node.getLine() + ": name \'" + node.getName() + "\' has not been declared!", null);
		}
		
		node.obj = obj;
	}
	
	public void visit(RetCallNode node)
	{
		Obj func = ((FuncCallNode)node.getFuncCall()).getDesignator().obj;
		if (Obj.Meth == func.getKind())
		{
			if (func.getType() == Tab.noType)
			{
				// is void func
				report_error("Semantic error: \'" + func.getName() + "\' cannot be used in expressions because it does not have a return value", node);
			}
			else node.struct = func.getType();
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": name \'" + func.getName() + "\' is not a function!", null);
			node.struct = Tab.noType;
		}
	}
	
	public void visit(TermNode node)
	{
		node.struct = node.getFactor().struct;
	}
	
	public void visit(TermExprNode node)
	{
		node.struct = node.getTerm().struct;
	}
	
	public void visit(OpExprNode node)
	{
		Struct expr = node.getExpr().struct;
		Struct term = node.getTerm().struct;
		
		if (expr.equals(term) && term == Tab.intType)
		{
			node.struct = expr;
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": types not compatible.", null);
			node.struct = Tab.noType;
		}
	}
	
	public void visit(ConstantNode node)
	{
		node.struct = Tab.intType;
	}
	
	public void visit(VariableNode node)
	{
		node.struct = node.getDesignator().obj.getType();
	}
	
	public void visit(ReturnExprNode node)
	{
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if (!currMethType.compatibleWith(node.getExpr().struct))
		{
			report_error("Error on line " + node.getLine() + ": expression type in return statement does not match with the return type of the surrounding function \'" + currentMethod.getName() + "\'", null);
		}
	}
	
	public void visit(AssignmentNode node)
	{
		if (!node.getExpr().struct.assignableTo(node.getDesignator().obj.getType()))
		{
			report_error("Error on line " + node.getLine() + ": cannot do the assignment due to incompatible types.", null);
		}
	}
}
