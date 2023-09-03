package com.mrx;

import com.mrx.yapi.ast.AstParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        logger.debug("starting");
        String javaFile = "D:\\工程\\Java\\CityInfo";
        String targetClass = "com.mrx.cityinfo.controller.CityInfoController";
        AstParser.parse(Path.of(javaFile), targetClass);
    }
}