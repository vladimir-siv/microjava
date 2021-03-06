package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;
import java.lang.reflect.*;

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
	
	private static Struct getType(LinkedHashMap<String, Struct> types, String name)
	{
		Struct type = types.get(name);
		if (type != null) return type;
		
		type = new Struct(Struct.Class);
		types.put(name, type);
		
		return type;
	}
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
		return getType(classTypes, className);
	}
	public static Struct interfaceType(String interfaceName)
	{
		return getType(interfaceTypes, interfaceName);
	}
	
	public static void init()
	{
		Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		config();
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
	
	public static Struct GetExtendingType(Struct type)
	{
		Obj superClass = type.getMembers().searchKey("$extends");
		
		if (superClass != Tab.noObj && superClass != null)
		{
			return superClass.getType();
		}
		
		return Tab.noType;
	}
	public static boolean AssignmentPossible(Struct dstType, Struct srcType)
	{
		if (srcType.assignableTo(dstType)) return true;
		
		if (dstType.getKind() == Struct.Class && srcType.getKind() == Struct.Class)
		{
			Struct currentType = srcType;
			
			do
			{
				Iterator<Obj> i = currentType.getMembers().symbols().iterator();
				
				while (i.hasNext())
				{
					Obj obj = i.next();
					
					if (obj.getKind() == Obj.Type)
					{
						if (obj.getType().assignableTo(dstType))
						{
							return true;
						}
					}
				}
				
				currentType = GetExtendingType(currentType);
			} while (currentType != Tab.noType) ;
		}
		
		return false;
	}
	
	private static Field numOfFields = null;
	private static void config()
	{
		try
		{
			numOfFields = Struct.class.getDeclaredField("numOfFields");
			numOfFields.setAccessible(true);
		}
		catch (Exception ex) { ex.printStackTrace(); }
	}
	public static void setNumOfFields(Struct interfaceType, int value)
	{
		try { numOfFields.set(interfaceType, value); }
		catch (Exception ex) { ex.printStackTrace(); }
	}
}
