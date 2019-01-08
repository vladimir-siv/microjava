package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class Extensions
{
	private static LinkedHashMap<Struct, Struct> arrayTypes = new LinkedHashMap<>();
	
	public enum Bool
	{
		False(0), True(1);
		private int value;
		Bool(int value) { this.value = value; }
		public int v() { return value; }
	}
	
	public static final Struct classType = new Struct(Struct.Class);
	public static final Struct boolType = new Struct(5);
	public static final Struct enumType = new Struct(6);
	
	public static Struct arrayType(Struct elemType)
	{
		Struct type = arrayTypes.get(elemType);
		if (type != null) return type;
		
		type = new Struct(Struct.Array, elemType);
		arrayTypes.put(elemType, type);
		return type;
	}
	
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
	
	public static Obj FindEnumConstant(Obj _enum, String constName)
	{
		Iterator<Obj> i = _enum.getLocalSymbols().iterator();
		
		while (i.hasNext())
		{
			Obj obj = i.next();
			
			if (obj.getName().equals(constName))
			{
				return obj;
			}
		}
		
		return Tab.noObj;
	}
	public static Obj FindMethodParameter(Obj method, int paramNo)
	{
		Iterator<Obj> i = method.getLocalSymbols().iterator();
		
		while (i.hasNext())
		{
			Obj obj = i.next();
			
			if (obj.getFpPos() == paramNo)
			{
				return obj;
			}
		}
		
		return Tab.noObj;
	}
}
