package com.company;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Downloader {
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    static private Map<URL, List<String>> store = new HashMap<>();

    private static long download(URL url, String outputFile) throws IOException {
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1)
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        return Files.size(Paths.get(outputFile)) / 1024;
    }

    private static void copyFile(String originalFilePath, String copyPathFile) throws IOException {
        Files.copy(Paths.get(originalFilePath), Paths.get(copyPathFile), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String getExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i >= 0)
            extension = fileName.substring(i);
        return extension;
    }

    private static void parseToStore(File inputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(" ");
                URL url = new URL(temp[0]);
                if (store.containsKey(url))
                    store.get(url)
                            .add(temp[1] + getExtension(temp[0]));
                else
                    store.put(url, new ArrayList<>() {{add(temp[1] + getExtension(temp[0]));}});
            }

    }

    public static void downloadFromFile(int threads, String inputFile, String outputFolder) throws IOException, InterruptedException {
        final long[] totalTime = {0};
        final long[] totalSize = {0};
        File inputtedFile = new File(inputFile);
        Path path = Paths.get(outputFolder);

        if (!Files.isDirectory(path))
            Files.createDirectory(path);

//        if (!Files.exists(Paths.get(outputFolder)))
//            Files.createDirectories(Paths.get(outputFolder));



        parseToStore(inputtedFile);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for(Map.Entry<URL, List<String>> entry : store.entrySet()) {
            URL url = entry.getKey();
            List<String> list = entry.getValue();
            executor.execute(() -> {
                long time = System.currentTimeMillis();
                long size = 0;
                try {
                    System.out.printf("%sЗагружается файл %s%s\n", YELLOW, list.get(0), RESET);
                    size = download(url, outputFolder + list.get(0));
                }
                catch (IOException e) { e.printStackTrace(); }
                time = (System.currentTimeMillis() - time) / 1000;
                System.out.printf("%s[Готово!] Загрузка файла %s завершена: %d KB за %d секунд%s\n", GREEN, list.get(0), size, time, RESET);
                if (totalTime[0] < time) totalTime[0] = time;
                // если есть одинаковые ссылки, то мы копируем уже скачанный файл
                // столько раз, сколько одинаковых ссылок
                if (list.size() > 1) {
                    for (int i = 1; i < list.size(); i++) {
                        try { copyFile(outputFolder + list.get(0), outputFolder + list.get(i)); }
                        catch (IOException e) { e.printStackTrace(); }
                        System.out.printf("%s[Инфо] Файл %s уже был загружен ранее, но сохранен как копия под указанным именем: %s%s\n",
                                CYAN, list.get(0), list.get(i), RESET);
                    }
                }
                totalSize[0] += size;
            });
        }

        executor.shutdown();
        executor.awaitTermination(24, TimeUnit.HOURS);

        System.out.printf("\nЗавершено: 100%%\nЗагружено: %d файлов, %.1f MB\nВремя: %d секунд\nСредняя скорость: %.1f MB/s\n",
                store.size(), totalSize[0]/1024d, totalTime[0], totalSize[0]/1024d/totalTime[0]);

    }
}
