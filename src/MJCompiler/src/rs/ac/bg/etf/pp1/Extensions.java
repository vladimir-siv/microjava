package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Extensions
{
	public enum Bool
	{
		False(48), True(49);
		private int value;
		Bool(int value) { this.value = value; }
		public int v() { return value; }
	}
	
	public static final Struct boolType = new Struct(5);
	public static final Struct enumType = new Struct(6);
	
	public static void init()
	{
		Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}
	
	public static void UpdateConstantValue(ConstValue constValue, Obj cnst)
	{
		if (constValue instanceof IntConstNode) cnst.setAdr(((IntConstNode)constValue).getValue());
		else if (constValue instanceof CharConstNode) cnst.setAdr(((CharConstNode)constValue).getValue());
		else if (constValue instanceof BoolConstNode) cnst.setAdr(((BoolConstNode)constValue).getValue() ? Bool.True.v() : Bool.False.v());
		else cnst.setAdr(0);
	}
}
