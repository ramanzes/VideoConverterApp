package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoConverterApp extends JFrame {
    private JTextField directoryField;
    private JButton browseButton;
    private JButton convertButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private ExecutorService executor;
    private AtomicInteger completedFiles;

    public VideoConverterApp() {
        setTitle("Video Converter");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Панель для выбора директории
        directoryField = new JTextField();
        directoryField.setEditable(false);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> chooseDirectory());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(directoryField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);

        // Список файлов
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JScrollPane fileListScrollPane = new JScrollPane(fileList);

        // Прогресс бар
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // Лог
        logArea = new JTextArea();
        logArea.setEditable(false);

        // Кнопка конвертации
        convertButton = new JButton("Convert");
        convertButton.addActionListener(e -> startConversion());

        // Панель для прогресса и кнопки
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(convertButton, BorderLayout.SOUTH);

        // Добавление компонентов на форму
        add(topPanel, BorderLayout.NORTH);
        add(fileListScrollPane, BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Пул потоков для конвертации
        executor = Executors.newFixedThreadPool(5);
        completedFiles = new AtomicInteger(0);
    }

    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            directoryField.setText(selectedDirectory.getAbsolutePath());
            updateFileList(selectedDirectory);
        }
    }

    private void updateFileList(File directory) {
        fileListModel.clear();
        File[] aviFiles = directory.listFiles((dir, name) -> name.endsWith(".avi"));
        if (aviFiles != null) {
            for (File file : aviFiles) {
                fileListModel.addElement(file.getName());
            }
        }
    }

    private void startConversion() {
        String directoryPath = directoryField.getText();
        if (directoryPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a directory first.");
            return;
        }

        File directory = new File(directoryPath);
        File[] aviFiles = directory.listFiles((dir, name) -> name.endsWith(".avi"));

        if (aviFiles == null || aviFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No AVI files found in the selected directory.");
            return;
        }

        // Уведомление о начале конвертации
        JOptionPane.showMessageDialog(this, "Конвертация началась! Результаты будут в том же каталоге, с теми же именами, как исходные файлы. Дождитесь завершения процесса.");

        // Блокировка кнопок
        browseButton.setEnabled(false);
        convertButton.setEnabled(false);

        // Сброс прогресса
        progressBar.setValue(0);
        completedFiles.set(0);

        // Запуск конвертации
        for (File aviFile : aviFiles) {
            executor.submit(() -> {
                convertFileWithProgress(aviFile);
                int progress = (int) ((completedFiles.incrementAndGet() / (double) aviFiles.length) * 100);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(progress);
                    if (completedFiles.get() == aviFiles.length) {
                        // Разблокировка кнопок после завершения
                        browseButton.setEnabled(true);
                        convertButton.setEnabled(true);
                        JOptionPane.showMessageDialog(VideoConverterApp.this, "Конвертация завершена!");
                    }
                });
            });
        }
    }

    private void convertFileWithProgress(File aviFile) {
        String outputFileName = aviFile.getName().replace(".avi", ".mp4");
        File outputFile = new File(aviFile.getParent(), outputFileName);

        String[] command = {
                "ffmpeg",
                "-i", aviFile.getAbsolutePath(),
                "-c:v", "libx264",
                "-preset", "slow",
                "-crf", "22",
                "-c:a", "aac",
                "-b:a", "192k",
                "-movflags", "+faststart",
                outputFile.getAbsolutePath()
        };

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            double totalDuration = getVideoDuration(aviFile); // Получаем общую длительность видео

            while ((line = reader.readLine()) != null) {
                if (line.contains("time=")) {
                    double currentTime = parseTime(line); // Парсим текущее время
                    int progress = (int) ((currentTime / totalDuration) * 100);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                SwingUtilities.invokeLater(() -> logArea.append("Successfully converted: " + aviFile.getName() + "\n"));
            } else {
                SwingUtilities.invokeLater(() -> logArea.append("Failed to convert: " + aviFile.getName() + "\n"));
            }
        } catch (IOException | InterruptedException e) {
            SwingUtilities.invokeLater(() -> logArea.append("Error converting " + aviFile.getName() + ": " + e.getMessage() + "\n"));
        }
    }

    private double getVideoDuration(File aviFile) {
        String[] command = {
                "ffmpeg",
                "-i", aviFile.getAbsolutePath()
        };

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration:")) {
                    String duration = line.split("Duration: ")[1].split(",")[0];
                    return parseTime(duration);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double parseTime(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length == 3) {
            double hours = Double.parseDouble(parts[0]);
            double minutes = Double.parseDouble(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        }
        return 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VideoConverterApp().setVisible(true));
    }
}
