package com.notfound.beatbox;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class BeatBox {
    private JFrame theFrame;
    private ArrayList<JCheckBox> checkBoxList;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private JLabel tempoLabel = new JLabel("Tempo: 1,0");
    private String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
                                        "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas",
                                        "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
                                        "High Agogo", "Open Hi Conga"};
    private int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    protected void setUpGui() {
        theFrame = new JFrame("Beat Box");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);

        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton buttonStart = new JButton("Start");
        buttonStart.addActionListener(e -> buildTrackAndStart());
        buttonBox.add(buttonStart);

        JButton buttonStop = new JButton("Stop");
        buttonStop.addActionListener(e -> sequencer.stop());
        buttonBox.add(buttonStop);

        JButton buttonTempoUp = new JButton("Tempo Up");
        buttonTempoUp.addActionListener(e -> {
            float tempo = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempo * 1.03));
            tempoLabel.setText("Tempo: " + String.format("%.1f", tempo));
        });
        buttonBox.add(buttonTempoUp);

        JButton buttonToStartTempo = new JButton("To Start Tempo");
        buttonToStartTempo.addActionListener(e -> {
            float tempo = sequencer.getTempoFactor();
            sequencer.setTempoFactor(1.0f);
            tempoLabel.setText("Tempo: " + String.format("%.1f", tempo));
        });
        buttonBox.add(buttonToStartTempo);

        JButton buttonTempoDown = new JButton("Tempo Down");
        buttonTempoDown.addActionListener(e -> {
            float tempo = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempo * 0.97));
            tempoLabel.setText("Tempo: " + String.format("%.1f", tempo));
        });
        buttonBox.add(buttonTempoDown);

        JButton buttonSave = new JButton("Save");
        buttonSave.addActionListener(e -> {
            boolean[] checkbox = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox ch = checkBoxList.get(i);
                if (ch.isSelected()) {
                    checkbox[i] = true;
                }
            }
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);
            File file = fileSave.getSelectedFile();
            try {
                ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
                os.writeObject(checkbox);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        buttonBox.add(buttonSave);

        JButton buttonLoad = new JButton("Load");
        buttonLoad.addActionListener(e -> {
            boolean[] checkbox = null;
            JFileChooser fileLoad = new JFileChooser();
            fileLoad.showOpenDialog(theFrame);
            File file = fileLoad.getSelectedFile();
            try {
                ObjectInputStream os = new ObjectInputStream(new FileInputStream(file));
                checkbox = (boolean[]) os.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            for (int i = 0; i < 256; i++) {
                JCheckBox ch = checkBoxList.get(i);
                assert checkbox != null;
                if (checkbox[i]) {
                    ch.setSelected(true);
                } else {
                    ch.setSelected(false);
                }
            }
            sequencer.stop();
        });
        buttonBox.add(buttonLoad);
        buttonBox.add(tempoLabel);

        Box nameBox = new Box(BoxLayout.Y_AXIS);

        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.WEST, nameBox);
        background.add(BorderLayout.EAST, buttonBox);
        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox ch = new JCheckBox();
            ch.setSelected(false);
            checkBoxList.add(ch);
            mainPanel.add(ch);
        }
        setUpMidi();
        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    private void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void buildTrackAndStart() {
        int[] trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new int[16];
            int key = instruments[i];

            for (int j = 0; j < 16; j++) {
                JCheckBox ch = checkBoxList.get(j + (16 * i));
                if (ch.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void makeTracks(int[] list) {
        for (int i = 0; i < 16; i++) {
            int key = list[i];
            if (key != 0) {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    private MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return event;
    }
}