package com.raketlabs.tts;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextToSpeech {

    // Create an ExecutorService with a fixed thread pool size of 1
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LocalMaryInterface mary = null;


    public TextToSpeech() {

        // init mary
        try {
            mary = new LocalMaryInterface();
        } catch (MaryConfigurationException e) {
            System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
            System.exit(1);
        }

        JFrame frame = new JFrame("Text To Speech");
        frame.setSize(640, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create text area
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Create scroll pane and add text area to it
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Create Translate button
        JButton translateButton = new JButton("Translate");

        // Set layout for the frame
        frame.setLayout(new BorderLayout());

        // Add components to the frame
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(translateButton, BorderLayout.SOUTH);

        // Set frame visibility
        frame.setVisible(true);

        // Add a WindowAdapter to handle the closing event
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Perform any cleanup or actions before closing the frame
                // Shutdown the ExecutorService to release resources
                executor.shutdown();
                // You can call dispose() to close the frame
                frame.dispose();
            }
        });

        // Add action listener to the translate button
        translateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textArea.getText();
                if (!text.isEmpty()) {

                    // Submit the Runnable task to the ExecutorService
                    executor.submit(() -> synthesize(text));

                } else {
                    JOptionPane.showMessageDialog(frame, "Please enter text in the text area.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void synthesize(String text) {
        // synthesize
        AudioInputStream audio = null;
        try {
            audio = mary.generateAudio(text);
            play(audio);
        } catch (SynthesisException e) {
            System.err.println("Synthesis failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void play(AudioInputStream audioInputStream) {
        try {
            // Open the audio input stream
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            // Create a buffer for reading from the input stream
            byte[] buffer = new byte[4096];
            int bytesRead = 0;

            // Read from the input stream and write to the audio line
            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
            }

            // Close the audio line and input stream
            line.drain();
            line.close();
            audioInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Create GUI on the event dispatching thread
        SwingUtilities.invokeLater(() -> new TextToSpeech());
    }
}
