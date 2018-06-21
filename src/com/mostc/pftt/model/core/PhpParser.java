package com.mostc.pftt.model.core;

import java.io.File;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NumberValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.CallExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.ClassDef.FieldEntry;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.quercus.statement.BlockStatement;
import com.caucho.quercus.statement.ExprStatement;
import com.caucho.quercus.statement.Statement;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.FileReadStream;
import com.caucho.vfs.NullPath;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StringStream;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.ISerializer;
import com.mostc.pftt.results.TestCaseCodeCoverage;

/** parses php code into a tree which can be navigated using this API or
 * serialized to XML.
 * 
 * enables easily navigating php code with simple tools like xpath or xquery, which
 * can also navigate test results and code coverage data (since they're all xml trees)
 * 
 * The hard part of code analysis (static or runtime/dynamic) is navigating all the data.
 * This class can help because it lets you easily navigate the code that was analyzed.
 * 
 * @see TestCaseCodeCoverage
 * 
 */

public class PhpParser {
	
	/** parses file into PhpScript
	 * 
	 * @param file
	 * @return
	 */
	public static PhpScript parseScript(File file) {
		//
		// with all the `non-technical` obstacles around developing PFTT(nearly preventing it
		//    from working, trying to limit its features, trying to limit its Linux support,
		//    calling it an `automation tool` instead of `test tool`, resistance to adding PHPT
		//    support, resistance to adding PhpUnit support, resistance to UI testing support,
		//    resistance to adding Apache support and even resistance to IIS support, etc...)
		// and the time/energy I have to spend dealing with those `non-technical` obstacles,
		// I just don't have the time to develop a simple PHP parser here... have to use Quercus to get this done
		//
		try {
			QuercusContext qctx = new QuercusContext();
			QuercusParser p = new QuercusParser(
					qctx, 
					new FilePath(file.getAbsolutePath()), 
					new ReadStream(new FileReadStream(new FileInputStream(file)))
				);
			String php_code_str = IOUtil.toString(new BufferedInputStream(new FileInputStream(file)), IOUtil.ONE_MEGABYTE);
			return new PhpScript(file, php_code_str, new Env(qctx), p.parse());
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PhpParser.class, ex);
		}
		return new PhpScript(file, "", null, null);
	} // end public static PhpScript parseScript
	
	/** parses PhpScript from string containing php code
	 * 
	 * @param php_code_str
	 * @param filename - filename the string is from
	 * @return
	 */
	public static PhpScript parseScript(String php_code_str, String filename) {
		try {
			QuercusContext qctx = new QuercusContext();
			QuercusParser p = new QuercusParser(
					qctx, 
					filename == null ? new NullPath("<unknown>") : new FilePath(filename),  
					StringStream.open(php_code_str)
				);
			return new PhpScript(php_code_str, php_code_str, new Env(qctx), p.parse());
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PhpParser.class, ex);
		}
		return new PhpScript(php_code_str, php_code_str, null, null);
	}
	
	public static PhpScript parseScript(String php_code_str) {
		return parseScript(php_code_str, null);
	}

	public static abstract class PhpCode implements ISerializer {
		protected final Env env;
		
		protected PhpCode(Env env) {
			this.env = env;
		}
		
		@Override
		public abstract int hashCode();
		
		@Override
		public abstract boolean equals(Object o);
		
		@Override
		public abstract String toString();
		
	} // end public static abstract class PhpCode
	
	public static class PhpScript extends PhpCode {
		protected final QuercusProgram prog;
		protected final Object source;
		protected final String source_code;
		
		protected PhpScript(Object source, String source_code, Env env, QuercusProgram prog) {
			super(env);
			this.source = source;
			this.source_code = source_code;
			this.prog = prog;
		}
		
		/** returns the classes defined by this php script
		 * 
		 * @return
		 */
		public ClassDefinition[] getClasses() {
			if (prog==null) {
				return new ClassDefinition[]{};
			} else {
				Collection<InterpretedClassDef> clazzes = prog.getClasses();
				ClassDefinition[] defs = new ClassDefinition[clazzes.size()+1];
				int i=0;
				defs[i++] = new MainClassDefinition(env, this, prog.getStatement());
				for ( InterpretedClassDef clazz : clazzes )
					defs[i++] = new ClassDefinition(env, this, clazz);
				return defs;
			}
		}
		
		public List<String> getUserFunctionNames() {
			return getBuiltinFunctionNames(null);
		}
		
		public List<String> getUserFunctionNames(TestCaseCodeCoverage cc) {
			List<UserFunctionCall> in = getUserFunctions(cc);
			ArrayList<String> out = new ArrayList<String>(in.size());
			for ( FunctionCall func : in )
				out.add(func.getFunctionName());
			return out;
		}
		
		public List<String> getFunctionNames() {
			return getBuiltinFunctionNames(null);
		}
		
		public List<String> getFunctionNames(TestCaseCodeCoverage cc) {
			List<FunctionCall> in = getFunctions(cc);
			ArrayList<String> out = new ArrayList<String>(in.size());
			for ( FunctionCall func : in )
				out.add(func.getFunctionName());
			return out;
		}
		
		public List<String> getBuiltinFunctionNames() {
			return getBuiltinFunctionNames(null);
		}
		
		public List<String> getBuiltinFunctionNames(TestCaseCodeCoverage cc) {
			List<BuiltinFunctionCall> in = getBuiltinFunctions(cc);
			ArrayList<String> out = new ArrayList<String>(in.size());
			for ( BuiltinFunctionCall func : in )
				out.add(func.getFunctionName());
			return out;
		}
		
		/** returns the builtin functions called by this php script
		 * 
		 * this returns all the builtin functions the script could call.
		 * 
		 * @return
		 */
		public List<BuiltinFunctionCall> getBuiltinFunctions() {
			return getBuiltinFunctions(null);
		}
		
		/** returns the builtin functions that were actually called by this php script
		 * when it was run (based on the code coverage data).
		 * 
		 * @param cc
		 * @return
		 */
		public List<BuiltinFunctionCall> getBuiltinFunctions(TestCaseCodeCoverage cc) {
			LinkedList<BuiltinFunctionCall> out = new LinkedList<BuiltinFunctionCall>();
			readFunctions(true, false, out, cc);
			return out;
		}
		
		/** get all function calls to user defined functions in this PhpScript
		 * 
		 * @return
		 */
		public List<UserFunctionCall> getUserFunctions() {
			return getUserFunctions(null);
		}
		/** get all function calls to user defined functions in this PhpScript,
		 * called from this code coverage data
		 * 
		 * @param cc
		 * @return
		 */
		public List<UserFunctionCall> getUserFunctions(TestCaseCodeCoverage cc) {
			LinkedList<UserFunctionCall> out = new LinkedList<UserFunctionCall>();
			readFunctions(false, false, out, cc);
			return out;
		}
		/** get all functions called from this PhpScript
		 * 
		 * @return
		 */
		public List<FunctionCall> getFunctions() {
			return getFunctions(null);
		}
		/** get all function calls from this PhpScript that were called from this code coverage data
		 * 
		 * @param cc
		 * @return
		 */
		public List<FunctionCall> getFunctions(TestCaseCodeCoverage cc) {
			LinkedList<FunctionCall> out = new LinkedList<FunctionCall>();
			readFunctions(true, true, out, cc);
			return out;
		}
		
		@SuppressWarnings("unchecked")
		protected void readFunctions(boolean builtin, boolean both, @SuppressWarnings("rawtypes") List out, TestCaseCodeCoverage cc) {
			for ( ClassDefinition clazz : getClasses() ) {
				for ( FunctionDefinition def : clazz.getFunctions() ) {
					for ( FunctionCall call : def.getCalls() ) {
						if (
									(both||builtin==call.isBuiltin())
									&& (cc==null||cc.isExecuted(call.getFileName(), call.getLineNumber()))
							) {
								out.add(call);
						}
					}
				}
			}
		}
		
		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "php");
			for ( ClassDefinition clazz : getClasses() )
				clazz.serial(serial);
			serial.endTag("pftt", "php");
		}

		@Override
		public int hashCode() {
			return source.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return o == this ? true : toString().equals(o.toString());
		}

		@Override
		public String toString() {
			return source.toString();
		}
				
	} // end public static class PhpScript
	
	public static class Field extends CodeBlock {
		protected final String name;
		protected final VariableValue value;
		
		protected Field(Env env, String name, VariableValue value) {
			super(env);
			this.name = name;
			this.value = value;
		}
		
		/** variable this class field stores
		 * 
		 * @return
		 */
		public VariableValue getValue() {
			return value;
		}
		
		/** name of this class field
		 * 
		 */
		@Override
		public String getName() {
			return name;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "field");
			serial.attribute("pftt", "name", getName());
			getValue().serial(serial);
			serial.endTag("pftt", "field");
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		
		@Override
		public String toString() {
			return "{"+name+"="+value+"}";
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof Field)
				return ((Field)o).getName().equals(name);
			else
				return toString().equals(o.toString());
		}
		
	} // end public static class Field
	
	public static abstract class VariableValue extends PhpCode {
		protected final FunctionDefinition fdef; // can be null, if this is a class field
		
		protected VariableValue(Env env, FunctionDefinition fdef) {
			super(env);
			this.fdef = fdef;
		}
		
		/** function that contains this variable
		 * 
		 * @return
		 */
		public FunctionDefinition getParent() {
			return fdef;
		}
		
		protected static VariableValue toVariableValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.Value v) {
			if (v instanceof com.caucho.quercus.env.NumberValue) {
				return new NumericValue(env, fdef, (com.caucho.quercus.env.NumberValue)v);
			} else if (v instanceof com.caucho.quercus.env.StringValue) {
				return new StringValue(env, fdef, (com.caucho.quercus.env.StringValue)v);
			} else if (v instanceof AbstractFunction) {
				// TODO null - get location of this call
				return new UserFunctionCall(env, fdef, null, new FunctionDefinition(env, fdef.getClassDefinition(), (AbstractFunction)v));
			} else if (v instanceof com.caucho.quercus.env.ArrayValue) {
				com.caucho.quercus.env.ArrayValue av = (com.caucho.quercus.env.ArrayValue) v;
				// PHP arrays can either be a list/array or a map
				return av.entrySet().isEmpty() ? new ArrayValue(env, fdef, av) : new MapValue(env, fdef, av);
			} else if (v instanceof com.caucho.quercus.env.BooleanValue) {
				return new BooleanValue(env, fdef, v);
			} else {
				return new ObjectValue(env, fdef, v);
			}
		}

		public static VariableValue toVariableValue(Env env, FunctionDefinition fdef, Expr e) {
			return toVariableValue(env, fdef, e.eval(env));
		}
		
	} // end public static abstract class VariableValue
	
	public static class MapValue extends VariableValue implements Iterable<String> {
		protected final HashMap<String,VariableValue> map;
		
		public MapValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.ArrayValue v) {
			super(env, fdef);
			map = new HashMap<String,VariableValue>();
			for ( java.util.Map.Entry<Value, Value> e : v.entrySet() ) {
				map.put(
						e.getKey().toJavaString(),
						VariableValue.toVariableValue(env, fdef, e.getValue())
					);
			}
		}
		
		@Override
		public Iterator<String> iterator() {
			return getNames();
		}
		
		public Iterator<String> getNames() {
			return map.keySet().iterator();
		}
		
		public VariableValue getValue(String name) {
			return map.get(name);
		}
		
		public Iterator<VariableValue> getValues() {
			return map.values().iterator();
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "map");
			Iterator<String> it = getNames();
			String name;
			while (it.hasNext()) {
				name = it.next();
				
				serial.startTag("pftt", "value");
				serial.attribute("pftt", "name", name);
				getValue(name).serial(serial);
				serial.endTag("pftt", "value");
			}
			serial.endTag("pftt", "map");
		}
		
		@Override
		public int hashCode() {
			return map.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof MapValue)
				return ((MapValue)o).map.equals(this.map);
			else
				return toString().equals(o.toString());
		}
		
		@Override
		public String toString() {
			return map.toString();
		}
		
	} // end public static class MapValue
	
	public static class ObjectValue extends VariableValue {
		protected final com.caucho.quercus.env.Value v;
		
		public ObjectValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.Value v) {
			super(env, fdef);
			this.v = v;
		}
		
		public com.caucho.quercus.env.Value value() {
			return v;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "object");
			serial.text(v.toJavaString());
			serial.endTag("pftt", "object");
		}
		
		@Override
		public int hashCode() {
			return v.hashCode();
		}
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof ObjectValue)
				return ((ObjectValue)o).equals(v);
			else
				return toString().equals(o.toString());
		}
		public String toString() {
			return v.toJavaString();
		}
		
	} // end public static class ObjectValue
	
	public static class BooleanValue extends VariableValue {
		protected final boolean b;
		
		public BooleanValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.Value v) {
			super(env, fdef);
			b = v.toJavaBoolean().booleanValue();
		}
		
		public boolean value() {
			return b;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "boolean");
			serial.text(Boolean.toString(b));
			serial.endTag("pftt", "boolean");
		}
		
		@Override
		public int hashCode() {
			return b ? 1 : 0;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof BooleanValue)
				return ((BooleanValue)o).b == this.b;
			else
				return toString().equals(o.toString());
		}
		
		@Override
		public String toString() {
			return Boolean.toString(b);
		}
		
	} // end public static class BooleanValue
	
	public static class NumericValue extends VariableValue {
		protected final double v;
		
		public NumericValue(Env env, FunctionDefinition fdef, NumberValue v) {
			super(env, fdef);
			this.v = v.toDouble();
		}
		
		public double value() {
			return v;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "numeric");
			serial.text(Double.toString(v));
			serial.endTag("pftt", "numeric");
		}
		
		@Override
		public int hashCode() {
			return (int) v;
		}
		
		@Override
		public String toString() {
			return Double.toString(v);
		}
		
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof NumericValue)
				return ((NumericValue)o).v == v;
			else
				return toString().equals(o.toString());
		}
		
	} // end public static class NumericValue
	
	public static class StringValue extends VariableValue {
		protected final String v;
		
		public StringValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.StringValue v) {
			super(env, fdef);
			this.v = v.toJavaString();
		}
		
		public String value() {
			return v;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "string");
			serial.text(v);
			serial.endTag("pftt", "string");
		}
		
		@Override
		public int hashCode() {
			return v.hashCode();
		}
		
		@Override
		public String toString() {
			return v;
		}
		
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof StringValue)
				return ((StringValue)o).v.equals(v);
			else
				return toString().equals(o.toString());
		}
		
	} // end public static class StringValue
	
	public static class ArrayValue extends VariableValue implements Iterable<VariableValue> {
		protected final com.caucho.quercus.env.ArrayValue def;
		
		protected ArrayValue(Env env, FunctionDefinition fdef, com.caucho.quercus.env.ArrayValue def) {
			super(env, fdef);
			this.def = def;
		}
		
		public VariableValue[] value() {
			ArrayList<VariableValue> out = new ArrayList<VariableValue>(2);
			for ( com.caucho.quercus.env.Value v : def.values() ) {
				out.add(toVariableValue(env, fdef, v));
			}
			return out.toArray(new VariableValue[out.size()]);
		}
		
		@Override
		public Iterator<VariableValue> iterator() {
			ArrayList<VariableValue> out = new ArrayList<VariableValue>(2);
			for ( com.caucho.quercus.env.Value v : def.values() ) {
				out.add(toVariableValue(env, fdef, v));
			}
			return out.iterator();
		}
		
		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "array");
			for ( VariableValue var : value() ) {
				var.serial(serial);
			}
			serial.endTag("pftt", "array");
		}
		
		@Override
		public int hashCode() {
			return def.hashCode();
		}
		
		@Override
		public String toString() {
			return def.toJavaString();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof ArrayValue)
				return ((ArrayValue)o).def.eq(this.def);
			else
				return toString().equals(o.toString());
		}
		
	} // end public static class ArrayValue
	
	public static abstract class CodeBlock extends PhpCode {
		
		protected CodeBlock(Env env) {
			super(env);
		}
		
		public abstract String getName();
		
	} // end public static abstract class CodeBlock
	
	public static class MainClassDefinition extends ClassDefinition {
		protected final Statement stmt;
		protected MainClassDefinition(Env env, PhpScript ps, Statement stmt) {
			super(env, ps, null);
			this.stmt = stmt;
		}
		public String getName() {
			return "<main>";
		}
		public Field[] getFields() {
			return new Field[]{};
		}
		public FunctionDefinition[] getFunctions() {
			return new FunctionDefinition[] {
				new FunctionDefinition(env, this, stmt)
			};
		}
		public boolean isInterface() {
			return false;
		}
		public boolean isAbstract() {
			return false;
		}
		public boolean isFinal() {
			return false;
		}
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof MainClassDefinition)
				return true;
			else
				return toString().equals(o.toString());
		}
		@Override
		public String toString() {
			return getName();
		}
	}
	
	public static class ClassDefinition extends CodeBlock {
		protected final ClassDef clazz;
		protected final PhpScript ps;
		
		protected ClassDefinition(Env env, PhpScript ps, ClassDef clazz) {
			super(env);
			this.ps = ps;
			this.clazz = clazz;
		}
		
		@Override
		public String getName() {
			return clazz.getName();
		}
		
		/** returns fields in this class
		 * 
		 * @return
		 */
		public Field[] getFields() {
			ArrayList<Field> out = new ArrayList<Field>();
			for (Map.Entry<com.caucho.quercus.env.StringValue,FieldEntry> e : clazz.fieldSet()) {
				out.add(new Field(
						env, 
						e.getKey().toJavaString(), 
						VariableValue.toVariableValue(
								env, 
								null,
								e.getValue().getValue()
							)
					));
			}
			return out.toArray(new Field[out.size()]);
		}
		
		/** returns functions defined by this class
		 * 
		 * @return
		 */
		public FunctionDefinition[] getFunctions() {
			ArrayList<FunctionDefinition> out = new ArrayList<FunctionDefinition>();
			for (Map.Entry<String, AbstractFunction> e : clazz.functionSet()) {
				out.add(new FunctionDefinition(env, this, e.getValue()));
			}
			return out.toArray(new FunctionDefinition[out.size()]);
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "class");
			serial.startTag("pftt", "fields");
			for ( Field f : getFields() )
				f.serial(serial);
			serial.endTag("pftt", "fields");
			serial.startTag("pftt", "functions");
			for ( FunctionDefinition fd : getFunctions() )
				fd.serial(serial);
			serial.endTag("pftt", "functions");
			serial.endTag("pftt", "class");
		}

		public boolean isInterface() {
			return clazz.isInterface();
		}

		public boolean isAbstract() {
			return clazz.isAbstract();
		}
		
		public boolean isFinal() {
			return clazz.isFinal();
		}
		
		@Override
		public int hashCode() {
			return getName().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof ClassDefinition)
				return ((ClassDefinition)o).clazz.equals(this.clazz);
			else
				return toString().equals(o.toString());
		}
		
		@Override
		public String toString() {
			return clazz.toString();
		}

		public PhpScript getPhpScript() {
			return ps;
		}
		
	} // end public static class ClassDefinition
	
	public static class UserFunctionCall extends FunctionCall {
		protected final FunctionDefinition target_func;
		
		protected UserFunctionCall(Env env, FunctionDefinition fdef, Location loc, FunctionDefinition target_func) {
			super(env, fdef, loc, target_func.getName());
			this.target_func = target_func;
		}
		
		public FunctionDefinition getTarget() {
			return target_func;
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "userCall");
			serial.attribute("pftt", "name", getFunctionName());
			serial.attribute("pftt", "class", getClassName());
			serial.attribute("pftt", "file", getFileName());
			serial.attribute("pftt", "line", Integer.toString(getLineNumber()));
			serial.endTag("pftt", "userCall");
		}

		@Override
		public boolean isBuiltin() {
			return false;
		}
	}
	
	public static class BuiltinFunctionCall extends FunctionCall {

		protected BuiltinFunctionCall(Env env, FunctionDefinition fdef, Location loc, String func_name) {
			super(env, fdef, loc, func_name);
		}

		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "builtinCall");
			serial.attribute("pftt", "name", getFunctionName());
			serial.endTag("pftt", "builtinCall");
		}

		@Override
		public boolean isBuiltin() {
			return true;
		}
		
	} // end public static class BuiltinFunctionCall
	
	public static abstract class FunctionCall extends VariableValue {
		protected final String func_name;
		protected final Location loc;
		
		protected FunctionCall(Env env, FunctionDefinition fdef, Location loc, String func_name) {
			super(env, fdef);
			this.loc = loc;
			this.func_name = func_name;
		}
		
		public String getClassName() {
			return loc.getClassName();
		}
		
		public String getFunctionName() {
			return func_name;
		}

		public String getFileName() {
			return loc.getFileName();
		}

		public int getLineNumber() {
			return loc.getLineNumber();
		}
		
		public abstract boolean isBuiltin();
		
		@Override
		public int hashCode() {
			return getFunctionName().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof FunctionCall)
				return ((FunctionCall)o).func_name.equals(this.func_name);
			else
				return toString().equals(o.toString());
		}
		
		@Override
		public String toString() {
			return func_name;
		}
		
	} // end public static abstract class FunctionCall
	
	public static class FunctionDefinition extends CodeBlock {
		protected final AbstractFunction def;
		protected final ClassDefinition clazz;
		protected final BlockStatement stmt;
		
		protected FunctionDefinition(Env env, ClassDefinition clazz, AbstractFunction def) {
			super(env);
			this.def = def;
			this.clazz = clazz;
			this.stmt = def instanceof Function && ((Function)def)._statement instanceof BlockStatement ?
					((BlockStatement)((Function)def)._statement) : null;
		}
		
		public FunctionDefinition(Env env, MainClassDefinition clazz, Statement stmt) {
			super(env);
			this.def = null;
			this.clazz = clazz;
			this.stmt = stmt instanceof BlockStatement ? (BlockStatement) stmt : null;
		}

		public int getLineNumber() {
			return def==null?0:def.getLocation().getLineNumber();
		}
		
		public String getClassName() {
			return def==null?"":def.getLocation().getClassName();
		}

		public String getFileName() {
			return def==null?"":def.getLocation().getFileName();
		}

		public ClassDefinition getClassDefinition() {
			return clazz;
		}
		
		/** the fully qualified name of the function, including class name and \\
		 * 
		 * @return
		 */
		@Override
		public String getName() {
			return def==null?"":def.getName();
		}
		
		public int getArgumentCount() {
			return def==null?0:def.getArgs().length;
		}
		
		/** returns the arguments this function accepts
		 * 
		 * @return
		 */
		public VariableValue[] getArguments() {
			if (def==null)
				return new VariableValue[]{};
			Arg[] args = def.getArgs();
			VariableValue[] val = new VariableValue[args.length];
			int i=0;
			for ( Arg arg : args )
				val[i++] = VariableValue.toVariableValue(
						env, 
						this, 
						arg.getDefault()
					);
			return val;
		}
		
		/** returns all the other functions that this function calls
		 * 
		 * @return
		 */
		public FunctionCall[] getCalls() {
			ArrayList<FunctionCall> out = new ArrayList<FunctionCall>(2);
			if (stmt != null) {
				String call_name, class_name;
				int i;
				AbstractFunction f;
				QuercusClass qc;
				for ( Statement st : stmt._statements ) {
					if (st instanceof ExprStatement) {
						Expr expr = ((ExprStatement)st)._expr;
						if (expr instanceof CallExpr) {
							CallExpr call = (CallExpr) expr;
							call_name = call.getName();
							
							if (call_name.contains("\\")) {
								i = call_name.lastIndexOf('\\');
								class_name = call_name.substring(0, i);
								call_name = call_name.substring(i+1);
								
								qc = env.findClass(class_name);
								if (qc!=null) {
									f = qc.findFunction(call_name);
									if (f!=null) {
										out.add(
												new UserFunctionCall(env, this, call.getLocation(),
														// TODO lookup the class this function actually belongs to
													new FunctionDefinition(env, new ClassDefinition(env, clazz.ps, qc.getClassDef()), f)
												));
										continue;
									}
								}
							} else {
								f = env.findFunction(call_name);
								if (f!=null) {
									out.add(
										new UserFunctionCall(env, this, call.getLocation(),
												// TODO lookup the class this function actually belongs to
											new FunctionDefinition(env, getClassDefinition(), f)
										));
									continue;
								}
							}
							// assume builtin
							out.add(new BuiltinFunctionCall(env, this, call.getLocation(), call.getName()));
						}
					}
				}
			}
			return out.toArray(new FunctionCall[out.size()]);
		}
		
		@Override
		public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag("pftt", "function");
			serial.attribute("pftt", "name", getName());
			final String ds = getDocstring();
			if (StringUtil.isNotEmpty(ds)) {
				serial.startTag("pftt", "docstring");
				serial.text(ds);
				serial.endTag("pftt", "docstring");
			}
			serial.startTag("pftt", "arguments");
			for ( VariableValue arg : getArguments() )
				arg.serial(serial);
			serial.endTag("pftt", "arguments");
			serial.startTag("pftt", "calls");
			for ( FunctionCall fc : getCalls() )
				fc.serial(serial);
			serial.endTag("pftt", "calls");
			serial.endTag("pftt", "function");
		}
		
		@Override
		public int hashCode() {
			return def.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o==this)
				return true;
			else if (o instanceof FunctionDefinition)
				return ((FunctionDefinition)o).def.eq(this.def);
			else
				return toString().equals(o.toString());
		}
		
		@Override
		public String toString() {
			return getName();
		}
		
		/** gets docstring (documentation) for this function
		 * 
		 */
		public String getDocstring() {
			PhpScript ps = clazz.getPhpScript();
			int i = ps.source_code.indexOf("function "+getName());
			if (i==-1)
				return null;
			i = ps.source_code.lastIndexOf("*/", i);
			if (i==-1)
				return null;
			int j = ps.source_code.lastIndexOf("/*", i);
			if (j==-1)
				return null;
			return ps.source_code.substring(j, i);
		}
		
		/** returns names and values of annotations from docstring
		 * 
		 * @return
		 */
		public HashMap<String,String> getAnnotations() {
			String docstring = getDocstring();
			if (StringUtil.isEmpty(docstring))
				return new HashMap<String,String>();
			HashMap<String,String> map = new HashMap<String,String>();
			int i, j;
			for ( String line : StringUtil.splitLines(docstring) ) {
				i = line.indexOf('@');
				if (i==-1)
					continue;
				j = line.indexOf(' ', i+1);
				if (j==-1)
					map.put(line.substring(i+1), "");
				else
					map.put(line.substring(i+1, j), line.substring(j+1));
			}
			return map;
		}
		
		/** returns the value of this annotation or null if annotation does not exist
		 * 
		 * @param name
		 * @return
		 */
		public String getAnnotationValue(String name) {
			return getAnnotations().get(name);
		}
		
	} // end public static class FunctionDefinition
	
} // end public class PhpParser
