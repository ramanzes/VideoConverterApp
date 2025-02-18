package org.example;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoConverterApp extends JFrame {
    private JTextField directoryField;
    private JButton convertButton;
    private JTextArea logArea;
    private ExecutorService executor;

    public VideoConverterApp() {
        setTitle("Video Converter");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        directoryField = new JTextField();
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> chooseDirectory());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(directoryField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);

        convertButton = new JButton("Convert");
        convertButton.addActionListener(e -> startConversion());

        logArea = new JTextArea();
        logArea.setEditable(false);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(convertButton, BorderLayout.SOUTH);

        executor = Executors.newFixedThreadPool(5);
    }

    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            directoryField.setText(selectedDirectory.getAbsolutePath());
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

        for (File aviFile : aviFiles) {
            executor.submit(() -> convertFile(aviFile));
        }
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

        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logArea.append("Successfully converted: " + aviFile.getName() + "\n");
            } else {
                logArea.append("Failed to convert: " + aviFile.getName() + "\n");
            }
        } catch (IOException | InterruptedException e) {
            logArea.append("Error converting " + aviFile.getName() + ": " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VideoConverterApp().setVisible(true));
    }
}