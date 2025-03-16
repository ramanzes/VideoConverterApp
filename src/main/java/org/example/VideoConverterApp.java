package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoConverterApp extends JFrame {
    private JTextField directoryField;
    private JButton browseButton;
    private JButton convertButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private JTextArea logArea;
    private ExecutorService executor;
    private AtomicInteger completedFiles;
    private JLabel statusLabel;
    private Timer timer;
    private int dotCount = 0;

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

        // Лог
        logArea = new JTextArea();
        logArea.setEditable(false);

        // Кнопка конвертации
        convertButton = new JButton("Convert");
        convertButton.addActionListener(e -> startConversion());

        // Статус процесса
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Панель для статуса и кнопки
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(convertButton, BorderLayout.SOUTH);

        // Добавление компонентов на форму
        add(topPanel, BorderLayout.NORTH);
        add(fileListScrollPane, BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Пул потоков для конвертации
        executor = Executors.newFixedThreadPool(5);
        completedFiles = new AtomicInteger(0);

        // Таймер для обновления статуса
        timer = new Timer(60000, e -> updateStatus()); // 60000 мс = 1 минута
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
        JOptionPane.showMessageDialog(this, "Конвертация началась! Результаты будут в том же каталоге, с теми же именами, как исходные файлы. Дождитесь завершения процесса.\n (Konvertatsiya nachalas'! Rezultaty budut v tom zhe kataloge, s temi zhe imenami, kak iskhodnye fayly. Dozhidites' zaversheniya protsessa.)");

        // Блокировка кнопок
        browseButton.setEnabled(false);
        convertButton.setEnabled(false);

        // Сброс счетчика завершенных файлов
        completedFiles.set(0);

        // Запуск таймера для обновления статуса
        dotCount = 0;
        statusLabel.setText("PROCESS IS IN WORK");
        timer.start();

        // Запуск конвертации
        for (File aviFile : aviFiles) {
            executor.submit(() -> {
                convertFile(aviFile);
                int completed = completedFiles.incrementAndGet();
                if (completed == aviFiles.length) {
                    // Остановка таймера и разблокировка кнопок после завершения
                    SwingUtilities.invokeLater(() -> {
                        timer.stop();
                        statusLabel.setText("");
                        browseButton.setEnabled(true);
                        convertButton.setEnabled(true);
                        JOptionPane.showMessageDialog(VideoConverterApp.this, "Конвертация завершена!\n(Konvertatsiya zavershena.)");
                    });
                }
            });
        }
    }

    private void updateStatus() {
        dotCount = (dotCount + 1) % 4; // Ограничиваем количество точек до 3
        String dots = ".".repeat(dotCount);
        statusLabel.setText("PROCESS IS IN WORK" + dots);
    }

    private void convertFile(File aviFile) {
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

//        String[] command = {
//                "ffmpeg",
//                "-i", aviFile.getAbsolutePath(),
//                "-c:v", "libx264",           // Кодек видео
//                "-profile:v", "baseline",    // Профиль Baseline для максимальной совместимости
//                "-level", "3.0",             // Уровень H.264
//                "-preset", "slow",           // Скорость сжатия
//                "-crf", "22",                // Качество видео (меньше значение = лучше качество)
//                "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", // Убедитесь, что разрешение кратно 2
//                "-c:a", "aac",               // Кодек аудио
//                "-b:a", "192k",              // Битрейт аудио
//                "-movflags", "+faststart",   // Для потоковой передачи
//                outputFile.getAbsolutePath()
//        };



        try {
            Process process = new ProcessBuilder(command).start();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VideoConverterApp().setVisible(true));
    }
}