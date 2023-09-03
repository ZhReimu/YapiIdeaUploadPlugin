package com.mrx.yapi.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstParser {

    private static final Logger logger = LoggerFactory.getLogger(AstParser.class);

    public static void parse(Path path, String targetClass) {
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(path);
        List<CompilationUnit> compilationUnits = projectRoot.getSourceRoots()
                .stream().flatMap(AstParser::safeParse)
                .collect(Collectors.toList());
        CompilationUnit targetUnit = compilationUnits.stream()
                .filter(it -> it.getTypes().stream()
                        .filter(type -> type.getFullyQualifiedName().isPresent())
                        .anyMatch(type -> type.getFullyQualifiedName().get().endsWith(targetClass))
                ).findFirst().orElseThrow();
        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) targetUnit.getType(0);
        NodeList<AnnotationExpr> clazzAnnotations = clazz.getAnnotations();
        NodeList<BodyDeclaration<?>> members = clazz.getMembers();
        System.out.println(clazz);
    }

    @SneakyThrows
    private static Stream<CompilationUnit> safeParse(SourceRoot sourceRoot) {
        return sourceRoot.tryToParse().stream().map(it -> it.getResult().orElseThrow());
    }
}
