package com.mostc.pftt.model.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.FileReadStream;
import com.caucho.vfs.ReadStream;

public class PhpParser {
	/*
	public static PhptTestCase toPhpt(PhpCodeCoverage cc, PhpProgram prog) {
		
	}
	
	public static void toSingleFile(PhpCodeCoverage cc, PhpProgram prog) {
		PrintStream ps;
		ps.println("Test Name");
		ps.println("--CREDITS--");
		ps.println("--FILE--");
		ps.println("--EXPECT--");
		SinglePhpClass clazz;
		for ( FunctionCall func : getBuiltinFunctions(cc, prog) ) {
			ps.print(clazz.ensureDefined(func));
			ps.print('(');
			for ( VariableValue val : func.getArguments() ) {
				Value last = func.getLastVal(val);
				if (last instanceof ArrayValue) {
					// do this recursively
				} else if (last instanceof FunctionCall) {
					// NOTE: FunctionDefinition and ClassDefinition won't be here
					//       there will be a FunctionCall (even to instantiate a class)
					//
					// ensure defined
					// add function call
					// TODO do this recursively too
					ps.print(clazz.ensureDefined(func));
				} else if (last instanceof VariableValue) {
					// include value
				}
				
				val.getName();
			}
			ps.println(");");
		}
	}
	
	protected static class SinglePhpClass {
		protected final HashMap<FunctionDefinition,String> single_file_names;
		
		public SinglePhpClass() {
			single_file_names = new HashMap<FunctionDefinition,String>();
		}
		
		public String ensureDefined(FunctionDefinition func) {
			String func_name = func.getName();
			if (single_file_names.containsValue(func.getName())) {
				// generate unique function name
				for ( int i=2 ; ; i++ ) {
					if (!single_file_names.containsValue(func_name+i)) {
						func_name = func_name+i;
						break;
					}
				}
			}
			single_file_names.put(func, func_name);
			return func_name;
		}
	}
	
	public static List<String> getBuiltinFunctionNames(PhpProgram prog) {
		return getBuiltinFunctionNames(null, prog);
	}
	
	public static List<String> getBuiltinFunctionNames(PhpCodeCoverage cc, PhpProgram prog) {
		List<FunctionCall> in = getBuiltinFunctions(cc, prog);
		ArrayList<String> out = new ArrayList<String>(in.size());
		for ( FunctionCall func : in )
			out.add(func.getName());
		return out;
	}
	
	public static List<FunctionCall> getBuiltinFunctions(PhpProgram prog) {
		return getBuiltinFunctions(null, prog);
	}
	
	public static List<FunctionCall> getBuiltinFunctions(PhpCodeCoverage cc, PhpProgram prog) {
		LinkedList<FunctionCall> out = new LinkedList<FunctionCall>();
		for ( ClassDefinition clazz : prog.getClasses() ) {
			for ( FunctionCall call : clazz.getFunctions() ) {
				if (call.isBuiltin()) {
					if (cc==null||cc.isExecuted(call.getFileName(), call.getLineNumber())) {
						out.add(call);
					}
				}
			}
		}
		return out;
	}
	
	public static PhpProgram parseProgram(File file) {
		QuercusParser p = new QuercusParser(qctx, new FilePath(file.getAbsolutePath()), new ReadStream(new FileReadStream(fin)));
		QuercusProgram prog = p.parse();
	}

	public static abstract class PhpSerial {
		public abstract void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException;
	}
	public static class PhpProgram extends PhpSerial {
		public ClassDefinition[] getClasses() {
			
		}
	}
	public abstract static class Value extends PhpSerial {
		public String getName() {
			
		}
		public FunctionDefinition getParent() {
			
		}
	}
	public static abstract class VariableValue extends Value {
		
	}
	public static class NumericValue extends VariableValue {
		@Override
		public void serialize(XmlSerializer serial) {
			
		}
	}
	public static class StringValue extends VariableValue {
		@Override
		public void serialize(XmlSerializer serial) {
			
		}
	}
	public static class ArrayValue extends VariableValue {
		public VariableValue[] getVariables() {
			
		}
		@Override
		public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag(null, "array");
			for ( VariableValue var : getVariables() ) {
				var.serialize(serial);
			}
			serial.endTag(null, "array");
		}
	}
	public static abstract class CodeBlock extends Value {
		
	}
	public static abstract class ClassDefinition extends CodeBlock {
		public abstract boolean isBuiltin();
		public boolean isAbstract() {
			
		}
		public boolean isInterface() {
			
		}
		public FunctionDefinition[] getFunctions() {
			
		}
	}
	public static class UserClassDefinition extends ClassDefinition {

	}
	public static class BuiltinClassDefinition extends ClassDefinition {
		
	}
	public static abstract class Function extends CodeBlock {
		
	}
	public static class FunctionCall extends Function {

		@Override
		public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
			serial.startTag(null, "call");
			serial.endTag(null, "call");
		}

		public Value getLastVal(VariableValue val) {
		}

		public String getFileName() {

		}

		public int getLineNumber() {

		}
		
	}
	public static class FunctionDefinition extends Function {
		public boolean isBuiltin() {
			
		}
		public int getArgumentCount() {
			
		}
		public VariableValue[] getArguments() {
			
		}
		public FunctionCall[] getCalls() {
			
		}
	}
	
*/	
}
