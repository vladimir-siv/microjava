package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	private Symbol new_symbol(int type)
	{
		return new Symbol(type, yyline + 1, yycolumn);
	}

	private Symbol new_symbol(int type, Object value)
	{
		return new Symbol(type, yyline + 1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

// # COMMENTS
<YYINITIAL>	"//"				{ yybegin(COMMENT); }
<COMMENT>	.					{ yybegin(COMMENT); }
<COMMENT>	"\r\n"				{ yybegin(YYINITIAL); }

// # WHITESPACE CHARACTERS
" "								{ }
"\b"							{ }
"\t"							{ }
"\r\n"							{ }
"\f"							{ }

// # KEYWORDS
"program"						{ return new_symbol(sym.PROG, yytext()); }

"const"							{ return new_symbol(sym.CONST, yytext()); }
"enum"							{ return new_symbol(sym.ENUM, yytext()); }

"print"							{ return new_symbol(sym.PRINT, yytext()); }
"read"							{ return new_symbol(sym.READ, yytext()); }

"return"						{ return new_symbol(sym.RETURN, yytext()); }
"void"							{ return new_symbol(sym.VOID, yytext()); }

"new"							{ return new_symbol(sym.NEW, yytext()); }
"null"							{ return new_symbol(sym.NULL, yytext()); }

"if"							{ return new_symbol(sym.IF, yytext()); }
"else"							{ return new_symbol(sym.ELSE, yytext()); }

"for"							{ return new_symbol(sym.FOR, yytext()); }
"break"							{ return new_symbol(sym.BREAK, yytext()); }
"continue"						{ return new_symbol(sym.CONTINUE, yytext()); }

// # CONSTANTS
[0-9]+							{ return new_symbol(sym.INT, new Integer(yytext())); }
"'"[ -~]"'"						{ return new_symbol(sym.CHAR, yytext().charAt(1)); }
("false"|"true")				{ return new_symbol(sym.BOOL, new Boolean(yytext())); }

// # IDENTIFIERS
([a-z]|[A-Z])[a-z|A-Z|0-9|_]*	{ return new_symbol(sym.IDENT, yytext()); }

// # OPERATORS
"+"								{ return new_symbol(sym.PLUS, yytext()); }
"-"								{ return new_symbol(sym.MINUS, yytext()); }
"*"								{ return new_symbol(sym.MULTIPLY, yytext()); }
"/"								{ return new_symbol(sym.DIVIDE, yytext()); }
"%"								{ return new_symbol(sym.MODULO, yytext()); }

"=="							{ return new_symbol(sym.EQU, yytext()); }
"!="							{ return new_symbol(sym.NEQ, yytext()); }
">"								{ return new_symbol(sym.GTR, yytext()); }
">="							{ return new_symbol(sym.GEQ, yytext()); }
"<"								{ return new_symbol(sym.LSS, yytext()); }
"<="							{ return new_symbol(sym.LEQ, yytext()); }

"&&"							{ return new_symbol(sym.AND, yytext()); }
"||"							{ return new_symbol(sym.OR, yytext()); }

"="								{ return new_symbol(sym.EQUAL, yytext()); }

"++"							{ return new_symbol(sym.INC, yytext()); }
"--"							{ return new_symbol(sym.DEC, yytext()); }

"."								{ return new_symbol(sym.DOT, yytext()); }
","								{ return new_symbol(sym.COMMA, yytext()); }
";"								{ return new_symbol(sym.SEMI, yytext()); }

"("								{ return new_symbol(sym.LPAREN, yytext()); }
")"								{ return new_symbol(sym.RPAREN, yytext()); }
"["								{ return new_symbol(sym.LBRACKET, yytext()); }
"]"								{ return new_symbol(sym.RBRACKET, yytext()); }
"{"								{ return new_symbol(sym.LBRACE, yytext()); }
"}"								{ return new_symbol(sym.RBRACE, yytext()); }

.								{ System.err.println("Lex error (" + yytext() + ") in line " + (yyline + 1) + "."); }
