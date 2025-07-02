package com.example.javapractice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.javapractice.model.Problem;
import com.example.javapractice.model.Submission;
import com.example.javapractice.repository.ProblemRepository;
import com.example.javapractice.repository.SubmissionRepository;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProblemController {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @GetMapping("/problems")
    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    @GetMapping("/problems/{id}")
    public Problem getProblemById(@PathVariable Long id) {
        return problemRepository.findById(id).orElse(null);
    }

    @PostMapping("/submit/{problemId}")
    public Submission submitCode(@PathVariable Long problemId, @RequestBody String code) throws IOException {
        Problem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            throw new RuntimeException("Problem not found");
        }

        // Save code to temporary file
        String className = "Solution";
        File sourceFile = new File(className + ".java");
        Files.write(sourceFile.toPath(), code.getBytes(StandardCharsets.UTF_8));

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream compileOut = new ByteArrayOutputStream();
        int compileResult = compiler.run(null, compileOut, compileOut, sourceFile.getPath());
        String compileOutput = compileOut.toString();

        String runOutput = "";
        String status;

        if (compileResult == 0) {
            // Run
            Process process = Runtime.getRuntime().exec("java " + className);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            runOutput = sb.toString().trim();

            String expectedOutput = problem.getExpectedOutput() != null ? problem.getExpectedOutput().trim() : "";

            if (runOutput.equals(expectedOutput)) {
                status = "Success";
            } else {
                status = "Failed";
            }
        } else {
            runOutput = compileOutput;
            status = "Compilation Error";
        }

        // Save submission
        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setCode(code);
        submission.setOutput(runOutput);
        submission.setStatus(status);
        submission.setCreatedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        // Clean up
        sourceFile.delete();
        new File(className + ".class").delete();

        return submission;
    }

    
    @GetMapping("/problems/difficulty/{level}")
    public List<Problem> getProblemsByDifficulty(@PathVariable String level) {
        return problemRepository.findByDifficulty(level);
    }

}
