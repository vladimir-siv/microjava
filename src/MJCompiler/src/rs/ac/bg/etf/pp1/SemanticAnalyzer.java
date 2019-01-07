package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.factory.SymbolTableFactory;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

import java.util.Iterator;

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
	public void visit(DesignatorIndexingNode node)
	{
		node.obj = Tab.noObj;
		
		if (node.getDesignator().obj.getType().getKind() != Struct.Array)
		{
			report_error("Error on line " + node.getLine() + ": trying to index a non-array type");
			return;
		}
		
		IndexingNode indexingNode = (IndexingNode)node.getIndex();
		
		if (indexingNode.getExpr().struct != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": indexer has to be an int");
			return;
		}
		
		node.obj = new Obj(Obj.Elem, "*", node.getDesignator().obj.getType().getElemType());
	}
	public void visit(DesignatorChainNode node)
	{
		node.obj = Tab.noObj;
		
		Designator chain = node.getDesignator();
		
		if (chain.obj.getKind() == Obj.Type)
		{
			if (chain.obj.getType() == Extensions.enumType)
			{
				Obj enumConst = null;
				
				Iterator<Obj> i = chain.obj.getLocalSymbols().iterator();
				
				while (enumConst == null && i.hasNext())
				{
					Obj obj = i.next();
					
					if (obj.getName().equals(node.getChainedDesignatorName()))
					{
						enumConst = obj;
					}
				}
				
				if (enumConst != null) node.obj = enumConst;
				else report_error("Error on line " + node.getLine() + ": enum \'" + chain.obj.getName() + "\' does not have a constant named \'" + node.getChainedDesignatorName() + "\'");
			}
			else report_error("Error on line " + node.getLine() + ": invalid use of dot operator [x02]");
		}
		else report_error("Error on line " + node.getLine() + ": invalid use of dot operator [x01]");
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
			Struct type = varTypeNode.struct != Extensions.enumType ? varTypeNode.struct : Tab.intType;
			
			if (node.getArrayType() instanceof ArrayTypeNode)
			{
				Tab.insert(Obj.Var, node.getVarName(), Extensions.arrayType(type));
			}
			else
			{
				Tab.insert(Obj.Var, node.getVarName(), type);
			}
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
	}
	
	// ======= [E] VARIABLES =======
	
	
	// ======= [S] ENUMS =======
	
	private SymbolDataStructure currentEnumConstants = null;
	private int currentValue = 0;
	
	public void visit(EnumDeclNode node)
	{
		currentEnumConstants = null;
		currentValue = 0;
		
		Obj declared = Tab.currentScope.findSymbol(node.getEnumName());
		
		if (declared == Tab.noObj || declared == null)
		{
			Obj enumObj = Tab.insert(Obj.Type, node.getEnumName(), Extensions.enumType);
			currentEnumConstants = SymbolTableFactory.instance().createSymbolTableDataStructure();
			enumObj.setLocals(currentEnumConstants);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
	}
	public void visit(EnumConstDeclNode node)
	{
		if (currentEnumConstants == null) return;
		
		if (currentEnumConstants.searchKey(node.getEnumConstName()) == null)
		{
			int constValue = 0;
			
			EnumOptValue enumOptValue = node.getEnumOptValue();
			
			if (enumOptValue instanceof  EnumValueNode)
			{
				constValue = ((EnumValueNode)enumOptValue).getValue();
				
				if (constValue < currentValue)
				{
					report_error("Error on line " + node.getLine() + ": enum constant \'" + node.getEnumConstName() + "\' cannot have value equal to " + constValue + " - the least it must have is " + currentValue);
					constValue = currentValue;
				}
				
				currentValue = constValue + 1;
			}
			else constValue = currentValue++;
			
			Obj enumConstObj = new Obj(Obj.Con, node.getEnumConstName(), Tab.intType, constValue, 1);
			currentEnumConstants.insertKey(enumConstObj);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + node.getEnumConstName() + "\' has already been defined in this enumeration");
	}
	
	// ======= [E] ENUMS =======
	
	
	// ======= [S] METHODS =======
	
	private boolean mainFound = false;
	public boolean isMainFound() { return mainFound; }
	
	private Obj currentMethod = null;
	private int paramNo = 0;
	private boolean returnFound = false;
	
	public void visit(TypedMethodNode node)
	{
		node.struct = node.getType().struct;
	}
	public void visit(VoidMethodNode node)
	{
		node.struct = Tab.noType;
	}
	public void visit(MethodDeclNode node)
	{
		if (node.getMethodName().equals("main"))
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
		
		paramNo = 0;
		Tab.openScope();
		currentMethod = node.obj;
	}
	public void visit(ParamDeclNode node)
	{
		Obj declared = Tab.currentScope.findSymbol(node.getParamName());
		
		if (declared == Tab.noObj || declared == null)
		{
			Struct type = node.getType().struct != Extensions.enumType ? node.getType().struct : Tab.intType;
			
			if (node.getArrayType() instanceof ArrayTypeNode)
			{
				node.obj = Tab.insert(Obj.Var, node.getParamName(), Extensions.arrayType(type));
			}
			else
			{
				node.obj = Tab.insert(Obj.Var, node.getParamName(), type);
			}
			
			node.obj.setFpPos(++paramNo);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
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
	public void visit(MethodNode node)
	{
		if (!returnFound && currentMethod.getType() != Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": function \'" + currentMethod.getName() + "\' does not have a return statement");
		}
		
		currentMethod.setLevel(paramNo);
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		returnFound = false;
		currentMethod = null;
	}
	
	private Obj currentCalleeMethod = null;
	private int argNo = 0;
	
	public void visit(CalleeNode node)
	{
		currentCalleeMethod = Tab.find(node.getDesignator().obj.getName());
		
		if (currentMethod == Tab.noObj || currentCalleeMethod.getKind() != Obj.Meth)
		{
			report_error("Error on line " + node.getLine() + ": name \'" + node.getDesignator().obj.getName() + "\' is not a function");
			currentCalleeMethod = null;
		}
		
		argNo = 0;
	}
	public void visit(ArgDeclNode node)
	{
		if (currentCalleeMethod == null) return;
		
		++argNo;
		
		Obj param = Extensions.FindMethodParameter(currentCalleeMethod, argNo);
		
		if (param == Tab.noObj) return;
		
		if (!node.getExpr().struct.assignableTo(param.getType()))
		{
			report_error("Error on line " + node.getLine() + ": argument " + argNo + " does not have a type that can be assigned to the method parameter");
		}
	}
	public void visit(FuncCallNode node)
	{
		if (currentCalleeMethod == null) return;
		
		if (currentCalleeMethod.getLevel() != argNo)
		{
			report_error("Error on line " + node.getLine() + ": invalid number of arguments");
			return;
		}
	}
	
	// ======= [E] METHODS =======
	
	
	// ======= [S] STATEMENTS =======
	
	// ### DesignatorStatements
	
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
	
	// ### DesignatorStatements
	
	// ### Regular statements
	
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
	
	public void visit(CondFactNode node)
	{
		node.struct = Extensions.boolType;
	}
	public void visit(RelCondFactNode node)
	{
		if (!node.getExpr().struct.compatibleWith(node.getExpr1().struct))
		{
			report_error("Error on line " + node.getLine() + ": types are not compatible for relational operation");
		}
		
		node.struct = Extensions.boolType;
	}
	
	public void visit(ExprNode node)
	{
		if (node.getUnaryop() instanceof UnaryMinusNode && node.getTerm().struct != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": cannot apply unary minus on type other than int");
		}
		
		node.struct = node.getTerm().struct;
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
		Callee callee = ((FuncCallNode)node.getFuncCall()).getCallee();
		Obj obj = ((CalleeNode)callee).getDesignator().obj;
		
		if (obj.getKind() == Obj.Meth)
		{
			if (obj.getType() != Tab.noType)
			{
				node.struct = obj.getType();
			}
			else report_error("Error on line " + node.getLine() + ": \'" + obj.getName() + "\' cannot be used in expressions because it does not have a return type (it is void)");
		}
		else
		{
			node.struct = Tab.noType;
			report_error("Error on line " + node.getLine() + ": name \'" + obj.getName() + "\' is not a function");
		}
	}
	public void visit(NewNode node)
	{
		ArraySize arraySize = node.getArraySize();
		Struct type = node.getType().struct != Extensions.enumType ? node.getType().struct : Tab.intType;
		
		if (arraySize instanceof ArraySizeNode)
		{
			node.struct = Extensions.arrayType(type);
			
			ArraySizeNode arraySizeNode = (ArraySizeNode)arraySize;
			
			if (arraySizeNode.getExpr().struct != Tab.intType)
			{
				report_error("Error on line " + node.getLine() + ": array size must be an int");
			}
		}
		else
		{
			node.struct = type;
			
			if (node.struct != Extensions.classType)
			{
				report_error("Error on line " + node.getLine() + ": new operator cannot be used on anything other than class types or arrays");
			}
		}
	}
	public void visit(NullNode node)
	{
		node.struct = Tab.nullType;
	}
	public void visit(PriorityFactorNode node)
	{
		node.struct = node.getExpr().struct;
	}
	
	// ### Regular statements
	
	// ======= [E] STATEMENTS =======
}
