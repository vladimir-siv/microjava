package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.Iterator;
import java.util.Stack;

public class SemanticAnalyzer extends VisitorAdaptor
{
	// ======= [S] GLOBAL =======
	
	private Logger log = Logger.getLogger(getClass());
	
	private boolean errorDetected = false;
	public boolean passed() { return !errorDetected; }
	
	private Stack<Integer> varKindContext = new Stack<>();
	private int currentVarKind() { return varKindContext.peek(); }
	private void openScope(int varKind) throws Error
	{
		if (varKind != Obj.Var && varKind != Obj.Fld)
		{
			throw new Error("Invalid var kind!");
		}
		
		Tab.openScope();
		varKindContext.push(varKind);
	}
	private void closeScope()
	{
		Tab.closeScope();
		varKindContext.pop();
	}
	
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
		openScope(Obj.Var);
	}
	public void visit(ProgramNode node)
	{
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(node.getProgDecl().obj);
		closeScope();
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
				report_error("Error on line " + node.getLine() + ": name " + node.getTypeName() + " does not represent a type");
				node.struct = Tab.noType;
			}
		}
		else
		{
			report_error("Error on line " + node.getLine() + ": type \'" + node.getTypeName() + "\' has not been defined");
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
		int chainKind = chain.obj.getKind();
		
		if
		(
			chainKind == Obj.Type
			||
			chainKind == Obj.Var
			||
			chainKind == Obj.Elem
			||
			chainKind == Obj.Fld
		)
		{
			if (chain.obj.getType() == Extensions.enumType)
			{
				Obj enumConst = Extensions.FindEnumConstant(chain.obj, node.getChainedDesignatorName());
				if (enumConst != Tab.noObj) node.obj = enumConst;
				else report_error("Error on line " + node.getLine() + ": enum \'" + chain.obj.getName() + "\' does not have a constant named \'" + node.getChainedDesignatorName() + "\'");
			}
			else if (chain.obj.getType().getKind() == Struct.Class)
			{
				Struct type = chain.obj.getType();
				Obj memberObj = type.getMembers().searchKey(node.getChainedDesignatorName());
				
				if (memberObj != Tab.noObj && memberObj != null)
				{
					int memberKind = memberObj.getKind();
					
					if (memberKind == Obj.Meth || memberKind == Obj.Fld)
					{
						node.obj = memberObj;
						return;
					}
				}
				
				report_error("Error on line " + node.getLine() + ": could not resolve name \'" + node.getChainedDesignatorName() + "\' (it is neither a method nor a field)");
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
			int currentVarKind = currentVarKind();
			
			if (node.getArrayType() instanceof ArrayTypeNode)
			{
				Tab.insert(currentVarKind, node.getVarName(), Extensions.arrayType(type));
			}
			else
			{
				Tab.insert(currentVarKind, node.getVarName(), type);
			}
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
	}
	
	// ======= [E] VARIABLES =======
	
	
	// ======= [S] ENUMS =======
	
	private Obj currentEnum = null;
	private int currentValue = 0;
	
	public void visit(EnumDeclNode node)
	{
		currentEnum = null;
		currentValue = 0;
		
		Obj declared = Tab.currentScope.findSymbol(node.getEnumName());
		
		if (declared == Tab.noObj || declared == null)
		{
			currentEnum = Tab.insert(Obj.Type, node.getEnumName(), Extensions.enumType);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
		
		openScope(Obj.Var);	// var kind is actually not important (and ignored here), since enum (by syntax) can't have var declarations inside it
	}
	public void visit(EnumConstDeclNode node)
	{
		if (currentEnum == null) return;
		
		if (Tab.currentScope.findSymbol(node.getEnumConstName()) == null)
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
			Tab.currentScope.addToLocals(enumConstObj);
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + node.getEnumConstName() + "\' has already been defined in this enumeration");
	}
	public void visit(EnumNode node)
	{
		if (currentEnum != null) Tab.chainLocalSymbols(currentEnum);
		closeScope();
	}
	
	// ======= [E] ENUMS =======
	
	
	// ======= [S] CLASSES =======
	
	private Obj currentClass = null;
	
	public void visit(ClassDeclNode node)
	{
		currentClass = null;
		String className = node.getClassName();
		
		Obj declared = Tab.currentScope.findSymbol(className);
		
		if (declared == Tab.noObj || declared == null)
		{
			currentClass = Tab.insert(Obj.Type, className, Extensions.classType(className));
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
		
		openScope(Obj.Fld);
	}
	public void visit(InterfaceDeclNode node)
	{
		currentClass = null;
		String interfaceName = node.getInterfaceName();
		
		Obj declared = Tab.currentScope.findSymbol(interfaceName);
		
		if (declared == Tab.noObj || declared == null)
		{
			currentClass = Tab.insert(Obj.Type, interfaceName, Extensions.interfaceType(interfaceName));
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
		
		openScope(Obj.Fld);	// interfaces actually can't have fields, so var kind is ignored
	}
	
	public void visit(ClassExtendsNode node)
	{
		Struct extendType = node.getType().struct;
		
		if (currentClass.getType() != extendType)
		{
			if (extendType.getKind() == Struct.Class)
			{
				int fields = extendType.getNumberOfFields();
				
				if (fields >= 0)
				{
					// ONLY LOCAL SYMBOL, IT IS NOT A DECLARATION/CREATION OF A TYPE !!!
					// Also it is necessary to put any symbol in current scope, so that method inheritance will be possible
					// (check the code below, it wouldn't be possible to do that if nothing was inserted in the tab, because in that case, SymbolDataStructure references would not be set - Struct members are invalid - getMembers() would return new HashTable() which will be lost upon next chaining)
					Tab.insert(Obj.Type, "$extends", extendType);
					// refresh to put the reference to the SymbolDataStructure inside the Struct
					Tab.chainLocalSymbols(currentClass.getType());
					
					for (int i = 0; i < fields; ++i)
					{
						Obj fld = Extensions.FindClassField(extendType, i);
						
						if (fld != Tab.noObj)
						{
							// Inherit the field
							Tab.insert(fld.getKind(), fld.getName(), fld.getType());
						}
						else report_error("Fatal error on line " + node.getLine() + ": could not inherit field \'" + fld.getName() + "\'");
					}
					
					for (Iterator<Obj> i = extendType.getMembers().symbols().iterator(); i.hasNext(); )
					{
						Obj obj = i.next();
						
						if (obj.getKind() == Obj.Meth)
						{
							// Inherit the method
							currentClass.getType().getMembers().insertKey(obj);
						}
					}
				}
				else report_error("Error on line " + node.getLine() + ": a class cannot extend an interface; did you mean to implement int?");
			}
			else report_error("Error on line " + node.getLine() + ": class type expected after extends keyword");
		}
		else report_error("Error on line " + node.getLine() + ": a class cannot extend itself");
	}
	public void visit(InterfaceImplementationNode node)
	{
	
	}
	
	public void visit(ClassNode node)
	{
		if (currentClass != null) Tab.chainLocalSymbols(currentClass.getType());
		closeScope();
		currentClass = null;
	}
	public void visit(InterfaceNode node)
	{
		if (currentClass != null) Tab.chainLocalSymbols(currentClass.getType());
		closeScope();
		currentClass = null;
	}
	
	// ======= [E] CLASSES =======
	
	
	// ======= [S] METHODS =======
	
	private boolean mainFound = false;
	public boolean isMainFound() { return mainFound; }
	
	private Obj currentMethod = null;
	private int paramNo = 0;
	
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
		if (currentClass == null && node.getMethodName().equals("main"))
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
		
		if (currentClass != null)
		{
			Tab.chainLocalSymbols(currentClass.getType()); // refresh symbols - find another way to do this peacefully
		}
		
		paramNo = 0;
		openScope(Obj.Var);
		currentMethod = node.obj;
		
		IfContext.beginMethod();
		
		if (currentClass != null)
		{
			Tab.insert(Obj.Var, "this", currentClass.getType());
		}
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
			
			if (currentMethod.getName().equals("main"))
			{
				report_error("Semantic error on line " + node.getLine() + ": main function cannot have parameters");
			}
		}
		else report_error("Error on line " + node.getLine() + ": name \'" + declared.getName() + "\' has already been declared in this scope");
	}
	public void visit(MethodPrototypeNode node)
	{
		node.obj = node.getMethodReg().obj;
	}
	public void visit(ReturnExprNode node)
	{
		IfContext.returnDetected();
		
		Struct currentMethodType = currentMethod.getType();
		
		if (currentMethodType == Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": return type of this function is void");
		}
		else if (!node.getExpr().struct.assignableTo(currentMethodType))
		{
			report_error("Error on line " + node.getLine() + ": expression type in return statement does not match with the return type of the surrounding function \'" + currentMethod.getName() + "\'");
		}
	}
	public void visit(ReturnVoidNode node)
	{
		IfContext.returnDetected();
		
		Struct currentMethodType = currentMethod.getType();
		
		if (currentMethodType != Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": must return an expression");
		}
	}
	public void visit(MethodNode node)
	{
		if (!IfContext.endMethod() && currentMethod.getType() != Tab.noType)
		{
			report_error("Error on line " + node.getLine() + ": not all code paths in function \'" + currentMethod.getName() + "\' lead to a return statement");
		}
		
		node.obj = node.getMethodPrototype().obj;
		currentMethod.setLevel(paramNo);
		Tab.chainLocalSymbols(currentMethod);
		closeScope();
		
		currentMethod = null;
	}
	
	private static final class CalleeMethodContext
	{
		private static class Callee
		{
			public Obj callee = null;
			public int argNo = 0;
			public Callee(Obj callee) { this.callee = callee; }
		}
		
		private CalleeMethodContext() { }
		
		private static Stack<Callee> calleeMethodContext = new Stack<>();
		
		public static void beginCall(Obj callee)
		{
			calleeMethodContext.push(new Callee(callee));
		}
		public static Obj getCurrentCallee()
		{
			return calleeMethodContext.peek().callee;
		}
		public static int getCurrentArgNo()
		{
			return calleeMethodContext.peek().argNo;
		}
		public static int incCurrentArgNo()
		{
			return ++calleeMethodContext.peek().argNo;
		}
		public static Obj endCall()
		{
			return calleeMethodContext.pop().callee;
		}
	}
	
	public void visit(CalleeNode node)
	{
		Obj currentCalleeMethod = node.getDesignator().obj;
		
		if (currentCalleeMethod == Tab.noObj)
		{
			errorDetected = true;
			currentCalleeMethod = null;
		}
		else if (currentCalleeMethod.getKind() != Obj.Meth)
		{
			report_error("Error on line " + node.getLine() + ": name \'" + currentCalleeMethod.getName() + "\' is not a function");
			currentCalleeMethod = null;
		}
		
		CalleeMethodContext.beginCall(currentCalleeMethod);
	}
	public void visit(ArgDeclNode node)
	{
		Obj currentCalleeMethod = CalleeMethodContext.getCurrentCallee();
		
		if (currentCalleeMethod == null) return;
		
		int argNo = CalleeMethodContext.incCurrentArgNo();
		
		Obj param = Extensions.FindMethodParameter(currentCalleeMethod, argNo);
		
		if (param == Tab.noObj) return;
		
		if (!node.getExpr().struct.assignableTo(param.getType()))
		{
			report_error("Error on line " + node.getLine() + ": argument " + argNo + " does not have a type that can be assigned to the method parameter");
		}
	}
	public void visit(FuncCallNode node)
	{
		Obj currentCalleeMethod = CalleeMethodContext.getCurrentCallee();
		int argNo = CalleeMethodContext.getCurrentArgNo();
		
		CalleeMethodContext.endCall();
		
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
		int objKind = node.getDesignator().obj.getKind();
		
		if (objKind != Obj.Var && objKind != Obj.Fld && objKind != Obj.Elem)
		{
			report_error("Error on line " + node.getLine() + ": assignment can only be used on lvalues");
		}
		
		if (!node.getExpr().struct.assignableTo(node.getDesignator().obj.getType()))
		{
			report_error("Error on line " + node.getLine() + ": cannot do the assignment due to incompatible types");
		}
	}
	public void visit(IncrementNode node)
	{
		int objKind = node.getDesignator().obj.getKind();
		
		if (objKind != Obj.Var && objKind != Obj.Fld && objKind != Obj.Elem)
		{
			report_error("Error on line " + node.getLine() + ": increment can only be used on lvalues");
		}
		
		if (node.getDesignator().obj.getType() != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": increment can only be applied on int");
		}
	}
	public void visit(DecrementNode node)
	{
		int objKind = node.getDesignator().obj.getKind();
		
		if (objKind != Obj.Var && objKind != Obj.Fld && objKind != Obj.Elem)
		{
			report_error("Error on line " + node.getLine() + ": decrement can only be used on lvalues");
		}
		
		if (node.getDesignator().obj.getType() != Tab.intType)
		{
			report_error("Error on line " + node.getLine() + ": decrement can only be applied on int");
		}
	}
	
	// ### DesignatorStatements
	
	// ### If
	
	private static final class IfContext
	{
		private boolean currentlyInIf = true;
		private boolean hasElse = false;
		
		private boolean ifHasReturn = false;
		private boolean elseHasReturn = false;
		
		private IfContext() { }
		
		private static boolean allPathsHaveReturn = false;
		private static int forContext = 0;
		private static Stack<IfContext> ifContexts = new Stack<>();
		
		public static void beginMethod()
		{
			allPathsHaveReturn = false;
			forContext = 0;		// just in case
			ifContexts.clear();	// just in case
		}
		public static void returnDetected()
		{
			if (forContext > 0) return;
			
			if (ifContexts.size() > 0)
			{
				IfContext current = ifContexts.peek();
				if (current.currentlyInIf) current.ifHasReturn = true;
				else current.elseHasReturn = true;
			}
			else allPathsHaveReturn = true;
		}
		public static boolean endMethod()
		{
			return allPathsHaveReturn;
		}
		
		public static void beginIf()
		{
			if (forContext > 0) return;
			ifContexts.push(new IfContext());
		}
		public static void beginElse()
		{
			if (forContext > 0) return;
			IfContext current = ifContexts.peek();
			current.currentlyInIf = false;
			current.hasElse = true;
		}
		public static IfContext endIf()
		{
			if (forContext > 0) return null;
			
			IfContext current = ifContexts.pop();
			
			if (current.ifHasReturn && (current.hasElse && current.elseHasReturn))
			{
				returnDetected();
			}
			
			return current;
		}
		
		public static void beginFor() { ++forContext; }
		public static void endFor() { --forContext; }
	}
	
	public void visit(IfConditionNode node) { IfContext.beginIf(); }
	public void visit(ElseNode node) { IfContext.beginElse(); }
	public void visit(IfNode node) { IfContext.endIf(); }
	public void visit(IfElseNode node) { IfContext.endIf(); }
	
	// ### If
	
	// ### For
	
	public void visit(ForInitNode node) { IfContext.beginFor(); }
	public void visit(EmptyForInitNode node) { IfContext.beginFor(); }
	public void visit(ForNode node) { IfContext.endFor(); }
	
	// ### For
	
	// ### Regular statements
	
	public void visit(PrintNode node)
	{
		Struct objType = node.getExpr().struct;
		
		if (objType != Tab.intType && objType != Tab.charType && objType != Extensions.boolType && objType != Extensions.enumType)
		{
			report_error("Error on line " + node.getLine() + ": Operand of PRINT instruction has to be an int, char, bool or enum");
		}
	}
	public void visit(ReadNode node)
	{
		int objKind = node.getDesignator().obj.getKind();
		Struct objType = node.getDesignator().obj.getType();
		
		if (objKind != Obj.Var && objKind != Obj.Fld && objKind != Obj.Elem)
		{
			report_error("Error on line " + node.getLine() + ": READ can only be used on lvalues");
		}
		
		if (objType != Tab.intType && objType != Tab.charType && objType != Extensions.boolType)
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
		
		if (node.getExpr().struct.isRefType() || node.getExpr1().struct.isRefType())
		{
			int relop = node.getRelop();
			
			if (relop != Code.eq && relop != Code.ne)
			{
				report_error("Error on line " + node.getLine() + ": only relational operators == and != can be used for reference types/arrays");
			}
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
			errorDetected = true;	// this should already be true (from CalleeNode visit)
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
			
			if (node.struct.getKind() != Struct.Class)
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
