package com.company;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] s) throws IOException, InterruptedException {
        String inputFile = "files/to_download.txt";
        String outputFile = "files/downloads/";
        Downloader.downloadFromFile(7, inputFile, outputFile);
    }
}