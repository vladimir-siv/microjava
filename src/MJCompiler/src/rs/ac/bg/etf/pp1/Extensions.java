package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class Extensions
{
	private static LinkedHashMap<Struct, Struct> arrayTypes = new LinkedHashMap<>();
	private static LinkedHashMap<String, Struct> classTypes = new LinkedHashMap<>();
	private static LinkedHashMap<String, Struct> interfaceTypes = new LinkedHashMap<>();
	
	public enum Bool
	{
		False(0), True(1);
		private int value;
		Bool(int value) { this.value = value; }
		public int v() { return value; }
	}
	
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
	public static Struct classType(String className)
	{
		Struct type = classTypes.get(className);
		if (type != null) return type;
		
		type = new Struct(Struct.Class);
		classTypes.put(className, type);
		
		return type;
	}
	public static Struct interfaceType(String interfaceName)
	{
		Struct type = interfaceTypes.get(interfaceName);
		if (type != null) return type;
		
		type = new Struct(Struct.Class);
		interfaceTypes.put(interfaceName, type);
		
		try { numOfFields.set(type, -1); }
		catch (Exception ex) { ex.printStackTrace(); }
		
		return type;
	}
	
	public static void init()
	{
		Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		
		try
		{
			numOfFields = Struct.class.getDeclaredField("numOfFields");
			numOfFields.setAccessible(true);
		}
		catch (Exception ex) { ex.printStackTrace(); }
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
		
		if
		(
			method.getName().equals("chr")
			||
			method.getName().equals("ord")
			||
			method.getName().equals("len")
		) return i.next();
		
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
	public static Obj FindClassField(Struct _class, int field)
	{
		Iterator<Obj> i = _class.getMembers().symbols().iterator();
		
		while (i.hasNext())
		{
			Obj obj = i.next();
			
			if (obj.getKind() == Obj.Fld && obj.getAdr() == field)
			{
				return obj;
			}
		}
		
		return Tab.noObj;
	}
	
	private static java.lang.reflect.Field numOfFields = null;
}
