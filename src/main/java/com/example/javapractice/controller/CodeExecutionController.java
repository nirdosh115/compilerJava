package com.example.javapractice.controller;

import com.example.javapractice.model.CodeExecutionRequest;
import com.example.javapractice.model.CodeExecutionResponse;
import org.springframework.web.bind.annotation.*;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class CodeExecutionController {

    @PostMapping("/execute")
    public CodeExecutionResponse executeCode(@RequestBody CodeExecutionRequest request) {
        String className = "Solution";
        String fullSourceCode = "public class " + className + " { public static void main(String[] args) { " 
                                + request.getCode() + " } }";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringWriter writer = new StringWriter();

        SimpleJavaFileObject fileObject = new SimpleJavaFileObject(URI.create(className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return fullSourceCode;
            }
        };

        JavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        JavaCompiler.CompilationTask task = compiler.getTask(writer, fileManager, null, null, null, Arrays.asList(fileObject));

        boolean success = task.call();
        if (!success) {
            return new CodeExecutionResponse("Compilation failed:\n" + writer.toString());
        }

        try {
            File dir = new File(".");
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { dir.toURI().toURL() });
            Class<?> cls = Class.forName(className, true, classLoader);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);

            cls.getMethod("main", String[].class).invoke(null, (Object) new String[]{});

            System.out.flush();
            System.setOut(oldOut);

            return new CodeExecutionResponse(baos.toString());

        } catch (Exception e) {
            return new CodeExecutionResponse("Execution error: " + e.getMessage());
        }
    }
}
