/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.ac.itb.gui;

import id.ac.itb.gui.alert.Alert;
import id.ac.itb.gui.config.ConfigDialog;
import id.ac.itb.gui.progressbar.CrawlerProgress;
import id.ac.itb.gui.progressbar.ExtractorProgress;
import id.ac.itb.gui.progressbar.PostprocessorProgress;
import id.ac.itb.gui.progressbar.PreprocessorProgress;
import id.ac.itb.gui.viewer.EvaluationViewer;
import id.ac.itb.gui.viewer.ExtractionViewer;
import id.ac.itb.openie.config.Config;
import id.ac.itb.openie.crawler.*;
import id.ac.itb.openie.evaluation.ExtractionsEvaluation;
import id.ac.itb.openie.evaluation.ExtractionsEvaluationLabeller;
import id.ac.itb.openie.evaluation.ExtractionsEvaluationModel;
import id.ac.itb.openie.evaluation.ExtractionsEvaluationResult;
import id.ac.itb.openie.extractor.*;
import id.ac.itb.openie.pipeline.OpenIePipeline;
import id.ac.itb.openie.plugins.PluginLoader;
import id.ac.itb.openie.postprocess.*;
import id.ac.itb.openie.preprocess.*;
import id.ac.itb.openie.relation.Relation;
import id.ac.itb.openie.relation.Relations;
import id.ac.itb.util.UnzipUtility;
import org.apache.commons.lang3.SerializationUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 *
 * @author elvanowen
 */
public class OpenIeJFrame extends javax.swing.JFrame {

    private DefaultListModel openIePipelineListModel;
    private PluginLoader pluginLoader;
    private ExtractionsEvaluationLabeller extractionsEvaluationLabeller;

    /**
     * Creates new form CustomizeCrawlerJFrame
     */
    public OpenIeJFrame() {
        setupFolders();
        initPlugins();
        initComponents();
    }

    private void setupFolders() {
        File crawlerDirectory = new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("CRAWLER_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator"))));
        File preprocessDirectory = new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("PREPROCESSES_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator"))));
        File extractionDirectory = new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("EXTRACTIONS_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator"))));
        File postprocessDirectory = new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("POSTPROCESSES_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator"))));
        File labelDirectory = new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("EVALUATION_LABEL_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator"))));

        if (!crawlerDirectory.exists()) crawlerDirectory.mkdir();
        if (!preprocessDirectory.exists()) preprocessDirectory.mkdir();
        if (!extractionDirectory.exists()) extractionDirectory.mkdir();
        if (!postprocessDirectory.exists()) postprocessDirectory.mkdir();
        if (!labelDirectory.exists()) labelDirectory.mkdir();
    }

    private void initPlugins() {
        Properties props = System.getProperties();
        props.setProperty("pf4j.mode", "development");
        props.setProperty("pf4j.pluginsDir", "plugins");

        pluginLoader = new PluginLoader();

        pluginLoader
                .registerAvailableExtensions(ICrawlerHandler.class)
                .registerAvailableExtensions(IPreprocessorHandler.class)
                .registerAvailableExtensions(IExtractorHandler.class)
                .registerAvailableExtensions(IPostprocessorHandler.class);
    }

    private void loadPlugin() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                String target = System.getProperty("pf4j.pluginsDir", "plugins") + File.separator + selectedFile.getName();
                File targetZip = new File(target);
                String UnzipTarget = target.replaceFirst("[.][^.]+$", "");
                Files.copy(selectedFile.toPath(), targetZip.toPath(), StandardCopyOption.REPLACE_EXISTING);

                UnzipUtility unzipUtility = new UnzipUtility();
                unzipUtility.unzip(target, System.getProperty("pf4j.pluginsDir", "plugins"));
                targetZip.delete();

                try {
                    Runtime rt = Runtime.getRuntime();
                    Process pr = rt.exec("ant");

                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

                    String line=null;

                    System.out.println("Rebuilding app using ant.");

                    while((line=input.readLine()) != null) {
                        System.out.println(line);
                    }

                    int exitVal = pr.waitFor();

                    if (exitVal == 0) {
                        new Alert("Plugins loaded successfully. Restart required to take effect.").setVisible(true);
                    } else {
                        throw new Error("Error loading plugin.");
                    }
                } catch(Exception e) {
                    new Alert(e.getMessage()).setVisible(true);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void browseStartingDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        Preprocessor fileReaderPreprocessor = new Preprocessor();

        for (Object iPreprocessorHandler: pluginLoader.getAllExtensions(IPreprocessorHandler.class)) {
            IPreprocessorHandler preprocessorHandler = (IPreprocessorHandler) iPreprocessorHandler;
            String pluginName = preprocessorHandler.getPluginName();

            if (pluginName.equalsIgnoreCase("Preprocessor File Reader")) {
                fileReaderPreprocessor = new Preprocessor().setPreprocessorHandler(SerializationUtils.clone(preprocessorHandler));
            }
        }

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            fileReaderPreprocessor.getPreprocessorHandler().setAvailableConfigurations("Input Directory", selectedFile.getAbsolutePath());
            openIePipelineListModel.addElement(fileReaderPreprocessor);
        }
    }

    private void refreshEvaluationFilesList() {
        extractionsEvaluationLabeller = new ExtractionsEvaluationLabeller();

        evaluationSectionFilesjList.setModel(new javax.swing.AbstractListModel<String>() {
            public int getSize() { return extractionsEvaluationLabeller.getDocuments().size(); }
            public String getElementAt(int i) { return (i+1) + ". " + extractionsEvaluationLabeller.getDocuments().get(i).getName(); }
        });
    }

    private void refreshEvaluationRelationsList() {
        if (evaluationSectionFilesjList.getSelectedIndex() >= 0) {
            evaluationSectionRelationsjList.setModel(new javax.swing.AbstractListModel<String>() {
                Relations relations = extractionsEvaluationLabeller.getRelationsFromDocument(extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex()));

                public int getSize() {
                    return relations.getRelations().size();
                }

                public String getElementAt(int i) {
                    Relation selectedRelation = relations.getRelations().get(i);
                    return String.format("%s. #%s: %s(%s, %s)\n", (i+1), selectedRelation.getSentenceIndex() + 1, selectedRelation.getRelationTriple().getMiddle(), selectedRelation.getRelationTriple().getLeft(), selectedRelation.getRelationTriple().getRight());
                }
            });
        }
    }

    private void refreshEvaluationSentencesList() {
        evaluationSectionSentencesjList.setModel(new javax.swing.AbstractListModel<String>() {
            public int getSize() {
                ArrayList<String> sentences = new ArrayList<>();

                if (evaluationSectionFilesjList.getSelectedIndex() >= 0) {
                    File selectedDocument = extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex());
                    sentences = extractionsEvaluationLabeller.getDocumentSentences(selectedDocument);
                }

                return sentences.size();
            }

            public String getElementAt(int i) {
                ArrayList<String> sentences = new ArrayList<>();

                if (evaluationSectionFilesjList.getSelectedIndex() >= 0) {
                    File selectedDocument = extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex());
                    sentences = extractionsEvaluationLabeller.getDocumentSentences(selectedDocument);
                }

                if (sentences.size() > 0) {
                    return (i+1) + ". " + sentences.get(i);
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        openIePipelineListModel = new DefaultListModel();
        extractionsEvaluationLabeller = new ExtractionsEvaluationLabeller();
        jSeparator1 = new javax.swing.JSeparator();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        loadPluginsLabel = new javax.swing.JLabel();
        loadPluginsButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        openIESectionPreprocessLabel = new javax.swing.JLabel();
        openIESectionPreprocessComboBox = new javax.swing.JComboBox<>();
        openIESectionAddPreprocessesButton = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JSeparator();
        openIESectionExtractionLabel = new javax.swing.JLabel();
        openIESectionExtractionComboBox = new javax.swing.JComboBox<>();
        openIESectionAddExtractionButton = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JSeparator();
        openIESectionPostprocessLabel = new javax.swing.JLabel();
        openIESectionPostprocessComboBox = new javax.swing.JComboBox<>();
        openIESectionAddPostprocessesButton = new javax.swing.JButton();
        openIESectionExecutionPipelineLabel = new javax.swing.JLabel();
        openIESectionRemovePipelineElementButton = new javax.swing.JButton();
        openIePipelineDragDropList = new id.ac.itb.gui.dragdroplist.DragDropList(openIePipelineListModel);
        jSeparator12 = new javax.swing.JSeparator();
        jSeparator13 = new javax.swing.JSeparator();
        jScrollPane6 = new javax.swing.JScrollPane();
        openIESectionExecutePipelineElementButton = new javax.swing.JButton();
        openIESectionConfigurePipelineElementButton1 = new javax.swing.JButton();
        openIESectionCrawlerLabel = new javax.swing.JLabel();
        openIESectionCrawlerComboBox = new javax.swing.JComboBox<>();
        openIESectionAddCrawlersButton = new javax.swing.JButton();
        openExtractionViewerLabel = new javax.swing.JLabel();
        openExtractionPostprocessedViewerLabel = new javax.swing.JLabel();
        startingDirectoryLabel = new javax.swing.JLabel();
        browseStartingDirectoryButton = new javax.swing.JButton();
        jSeparator14 = new javax.swing.JSeparator();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        evaluationSectionFilesjList = new javax.swing.JList<>();
        jScrollPane7 = new javax.swing.JScrollPane();
        evaluationSectionSentencesjList = new javax.swing.JList<>();
        sentencesLabel = new javax.swing.JLabel();
        addNewRelationsLabel = new javax.swing.JLabel();
        argument1EvaluationTextField = new javax.swing.JTextField();
        relationEvaluationTextField = new javax.swing.JTextField();
        argument2EvaluationTextField = new javax.swing.JTextField();
        addEvaluationRelationButton = new javax.swing.JButton();
        addedRelationsLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        evaluationSectionRelationsjList = new javax.swing.JList<>();
        removeEvaluationButton = new javax.swing.JButton();
        runEvaluationButton = new javax.swing.JButton();
        saveEvaluationButton = new javax.swing.JButton();
        evaluationFilesLabel = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        loadPluginsLabel.setText("Load Plugins");

        loadPluginsButton.setText("Browse");
        loadPluginsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPluginsButtonActionPerformed(evt);
            }
        });

        openIESectionPreprocessLabel.setText("Preprocesses");

        openIESectionPreprocessComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(pluginLoader.getImplementedExtensions(IPreprocessorHandler.class).toArray()));

        openIESectionAddPreprocessesButton.setText("+");
        openIESectionAddPreprocessesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionAddPreprocessesButtonActionPerformed(evt);
            }
        });

        jSeparator10.setOrientation(javax.swing.SwingConstants.VERTICAL);

        openIESectionExtractionLabel.setText("Extraction");

        openIESectionExtractionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(pluginLoader.getImplementedExtensions(IExtractorHandler.class).toArray()));

        openIESectionAddExtractionButton.setText("+");
        openIESectionAddExtractionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionAddExtractionButtonActionPerformed(evt);
            }
        });

        openIESectionPostprocessLabel.setText("Postprocesses");

        openIESectionPostprocessComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(pluginLoader.getImplementedExtensions(IPostprocessorHandler.class).toArray()));

        openIESectionAddPostprocessesButton.setText("+");
        openIESectionAddPostprocessesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionAddPostprocessesButtonActionPerformed(evt);
            }
        });

        openIESectionExecutionPipelineLabel.setText("Execution Pipeline");

        openIESectionRemovePipelineElementButton.setText("Remove");
        openIESectionRemovePipelineElementButton.setEnabled(false);
        openIESectionRemovePipelineElementButton.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        openIESectionRemovePipelineElementButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionRemovePipelineElementButtonActionPerformed(evt);
            }
        });

        jSeparator12.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator13.setOrientation(javax.swing.SwingConstants.VERTICAL);

        openIESectionExecutePipelineElementButton.setText("Execute");
        openIESectionExecutePipelineElementButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionExecutePipelineElementButtonActionPerformed(evt);
            }
        });

        openIESectionConfigurePipelineElementButton1.setText("Configure");
        openIESectionConfigurePipelineElementButton1.setEnabled(false);
        openIESectionConfigurePipelineElementButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionConfigurePipelineElementButton1ActionPerformed(evt);
            }
        });

        openIESectionCrawlerLabel.setText("Crawlers");
        openIESectionCrawlerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(pluginLoader.getImplementedExtensions(ICrawlerHandler.class).toArray()));

        openIESectionAddCrawlersButton.setText("+");
        openIESectionAddCrawlersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIESectionAddCrawlersButtonActionPerformed(evt);
            }
        });

        jScrollPane6.setViewportView(openIePipelineDragDropList);
        openExtractionViewerLabel.setFont(new java.awt.Font("Lucida Grande", 2, 13)); // NOI18N
        openExtractionViewerLabel.setForeground(new java.awt.Color(0, 102, 255));
        openExtractionViewerLabel.setText("(open viewer)");
        openExtractionViewerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        openExtractionViewerLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFrame extractionViewer = new ExtractionViewer(new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("EXTRACTIONS_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator")))));
                extractionViewer.setVisible(true);
            }
        });

        startingDirectoryLabel.setText("Starting Directory");

        browseStartingDirectoryButton.setText("Browse");
        browseStartingDirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseStartingDirectoryButtonActionPerformed(evt);
            }
        });

        jSeparator14.setOrientation(javax.swing.SwingConstants.VERTICAL);

        openExtractionPostprocessedViewerLabel.setFont(new java.awt.Font("Lucida Grande", 2, 13)); // NOI18N
        openExtractionPostprocessedViewerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        openExtractionPostprocessedViewerLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFrame extractionViewer = new ExtractionViewer(new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("POSTPROCESSES_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator")))));
                extractionViewer.setVisible(true);
            }
        });

        openIePipelineDragDropList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {

                Object selectedPipelineElement = openIePipelineDragDropList.getSelectedValue();
                HashMap<String, String> availableConfigurations = null;

                if (selectedPipelineElement != null) {
                    if (selectedPipelineElement instanceof Crawler) {
                        availableConfigurations = ((Crawler)selectedPipelineElement).getCrawlerhandler().getAvailableConfigurations();
                    } else if (selectedPipelineElement instanceof Preprocessor) {
                        availableConfigurations = ((Preprocessor)selectedPipelineElement).getPreprocessorHandler().getAvailableConfigurations();
                    } else if (selectedPipelineElement instanceof Extractor) {
                        availableConfigurations = ((Extractor)selectedPipelineElement).getExtractorHandler().getAvailableConfigurations();
                    } else if (selectedPipelineElement instanceof Postprocessor) {
                        availableConfigurations = ((Postprocessor)selectedPipelineElement).getPostprocessorHandler().getAvailableConfigurations();
                    }

                    if (availableConfigurations != null && availableConfigurations.size() > 0) {
                        openIESectionConfigurePipelineElementButton1.setEnabled(true);
                    } else {
                        openIESectionConfigurePipelineElementButton1.setEnabled(false);
                    }

                    openIESectionRemovePipelineElementButton.setEnabled(true);
                }
            }
        });
        openExtractionPostprocessedViewerLabel.setForeground(new java.awt.Color(0, 102, 255));
        openExtractionPostprocessedViewerLabel.setText("(open viewer)");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(openIESectionPreprocessLabel)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionPreprocessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(openIESectionAddPreprocessesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionExtractionLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openExtractionViewerLabel)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionExtractionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(openIESectionAddExtractionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionPostprocessLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openExtractionPostprocessedViewerLabel))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionPostprocessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openIESectionAddPostprocessesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator11)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(openIESectionConfigurePipelineElementButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(openIESectionRemovePipelineElementButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(openIESectionExecutePipelineElementButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(openIESectionExecutionPipelineLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(loadPluginsLabel)
                            .addComponent(loadPluginsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(startingDirectoryLabel)
                            .addComponent(browseStartingDirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionCrawlerComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openIESectionAddCrawlersButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionCrawlerLabel)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(openIESectionCrawlerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(openIESectionAddCrawlersButton))
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(startingDirectoryLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(browseStartingDirectoryButton))
                            .addComponent(jSeparator14, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(loadPluginsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(loadPluginsButton))
                            .addComponent(jSeparator12, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(openIESectionCrawlerLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(openIESectionPreprocessLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(openIESectionPreprocessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(openIESectionAddPreprocessesButton)))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(openIESectionExtractionLabel)
                                    .addComponent(openExtractionViewerLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(openIESectionExtractionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(openIESectionAddExtractionButton)))
                            .addComponent(jSeparator10, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                            .addComponent(jSeparator13)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(7, 7, 7)
                                .addComponent(openIESectionPostprocessLabel))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(openExtractionPostprocessedViewerLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(openIESectionPostprocessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(openIESectionAddPostprocessesButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator11, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openIESectionExecutionPipelineLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(openIESectionRemovePipelineElementButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openIESectionConfigurePipelineElementButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 145, Short.MAX_VALUE)
                        .addComponent(openIESectionExecutePipelineElementButton))
                    .addComponent(jScrollPane6))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Open IE", jPanel5);

        jScrollPane2.setViewportView(evaluationSectionFilesjList);

        evaluationSectionFilesjList.setModel(new javax.swing.AbstractListModel<String>() {
            public int getSize() { return extractionsEvaluationLabeller.getDocuments().size(); }
            public String getElementAt(int i) { return (i+1) + ". " + extractionsEvaluationLabeller.getDocuments().get(i).getName(); }
        });
        jScrollPane7.setViewportView(evaluationSectionSentencesjList);

        sentencesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        sentencesLabel.setText("Sentences:");

        addNewRelationsLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        addNewRelationsLabel.setText("Add New Relations:");

        argument1EvaluationTextField.setText("1st Argument");
        argument1EvaluationTextField.setEnabled(false);
        argument1EvaluationTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                argument1EvaluationTextFieldActionPerformed(evt);
            }
        });

        argument1EvaluationTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (argument1EvaluationTextField.getText().equalsIgnoreCase("1st Argument")) {
                    argument1EvaluationTextField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (argument1EvaluationTextField.getText().equalsIgnoreCase("")) {
                    argument1EvaluationTextField.setText("1st Argument");
                }
            }
        });

        evaluationSectionFilesjList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshEvaluationSentencesList();
                refreshEvaluationRelationsList();
            }
        });

        evaluationSectionSentencesjList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (evaluationSectionSentencesjList.getSelectedIndex() >= 0) {
                    addEvaluationRelationButton.setEnabled(true);
                    argument1EvaluationTextField.setEnabled(true);
                    relationEvaluationTextField.setEnabled(true);
                    argument2EvaluationTextField.setEnabled(true);
                } else {
                    addEvaluationRelationButton.setEnabled(false);
                    argument1EvaluationTextField.setEnabled(false);
                    relationEvaluationTextField.setEnabled(false);
                    argument2EvaluationTextField.setEnabled(false);
                }

            }
        });

        relationEvaluationTextField.setText("Relation");
        relationEvaluationTextField.setEnabled(false);
        relationEvaluationTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (relationEvaluationTextField.getText().equalsIgnoreCase("Relation")) {
                    relationEvaluationTextField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (relationEvaluationTextField.getText().equalsIgnoreCase("")) {
                    relationEvaluationTextField.setText("Relation");
                }
            }
        });

        argument2EvaluationTextField.setText("2nd Argument");
        argument2EvaluationTextField.setEnabled(false);
        argument2EvaluationTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (argument2EvaluationTextField.getText().equalsIgnoreCase("2nd Argument")) {
                    argument2EvaluationTextField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (argument2EvaluationTextField.getText().equalsIgnoreCase("")) {
                    argument2EvaluationTextField.setText("2nd Argument");
                }
            }
        });

        addEvaluationRelationButton.setText("+");
        addEvaluationRelationButton.setEnabled(false);
        addEvaluationRelationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEvaluationRelationButtonActionPerformed(evt);
            }
        });

        addedRelationsLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        addedRelationsLabel.setText("Added Relations:");

        refreshEvaluationRelationsList();
        jScrollPane1.setViewportView(evaluationSectionRelationsjList);

        evaluationSectionRelationsjList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (evaluationSectionRelationsjList.getSelectedIndex() >= 0) {
                    removeEvaluationButton.setEnabled(true);
                } else {
                    removeEvaluationButton.setEnabled(false);
                }
            }
        });

        removeEvaluationButton.setText("Remove");
        removeEvaluationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeEvaluationButtonActionPerformed(evt);
            }
        });

        runEvaluationButton.setText("Run Evaluation");
        runEvaluationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runEvaluationButtonActionPerformed(evt);
            }
        });

        saveEvaluationButton.setText("Save");
        removeEvaluationButton.setEnabled(false);
        saveEvaluationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveEvaluationButtonActionPerformed(evt);
            }
        });

        evaluationFilesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        evaluationFilesLabel.setText("Files:");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(evaluationFilesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(argument1EvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(relationEvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(argument2EvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(addEvaluationRelationButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sentencesLabel)
                                    .addComponent(addNewRelationsLabel)
                                    .addComponent(addedRelationsLabel))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(removeEvaluationButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(saveEvaluationButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jSeparator4))))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addComponent(runEvaluationButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sentencesLabel)
                    .addComponent(evaluationFilesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addNewRelationsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(argument1EvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(relationEvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(argument2EvaluationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addEvaluationRelationButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addedRelationsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(removeEvaluationButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(saveEvaluationButton))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(runEvaluationButton))
        );

        jPanel6.addComponentListener ( new ComponentAdapter() {
            public void componentShown ( ComponentEvent e ) {
                // reload files list in case user just do some crawling
                refreshEvaluationFilesList();
            }
        } );

        jTabbedPane1.addTab("Evaluation", jPanel6);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 675, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
        setLocationRelativeTo(null);
        setTitle("Sistem Open IE Bahasa Indonesia");
    }// </editor-fold>//GEN-END:initComponents

    private void loadPluginsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadPluginsButtonActionPerformed
        // TODO add your handling code here:

        loadPlugin();
    }//GEN-LAST:event_loadPluginsButtonActionPerformed

    private void openIESectionAddCrawlersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionAddCrawlersButtonActionPerformed
        // TODO add your handling code here:

        ICrawlerHandler crawlerHandler = (ICrawlerHandler) pluginLoader.getImplementedExtensions(ICrawlerHandler.class).get(openIESectionCrawlerComboBox.getSelectedIndex());
        Crawler crawler = new Crawler().setCrawlerhandler(SerializationUtils.clone(crawlerHandler));

        openIePipelineListModel.addElement(crawler);
        openIePipelineDragDropList.printItems();

    }//GEN-LAST:event_openIESectionAddCrawlersButtonActionPerformed

    private void openIESectionAddPreprocessesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionAddPreprocessesButtonActionPerformed
        // TODO add your handling code here:

        IPreprocessorHandler preprocessorHandler = (IPreprocessorHandler) pluginLoader.getImplementedExtensions(IPreprocessorHandler.class).get(openIESectionPreprocessComboBox.getSelectedIndex());
        Preprocessor preprocessor = new Preprocessor().setPreprocessorHandler(SerializationUtils.clone(preprocessorHandler));

        openIePipelineListModel.addElement(preprocessor);
        openIePipelineDragDropList.printItems();

    }//GEN-LAST:event_openIESectionAddPreprocessesButtonActionPerformed

    private void openIESectionAddExtractionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionAddExtractionButtonActionPerformed
        // TODO add your handling code here:

        IExtractorHandler extractorHandler = (IExtractorHandler) pluginLoader.getImplementedExtensions(IExtractorHandler.class).get(openIESectionExtractionComboBox.getSelectedIndex());
        Extractor extractor = new Extractor().setExtractorHandler(SerializationUtils.clone(extractorHandler));

        openIePipelineListModel.addElement(extractor);
        openIePipelineDragDropList.printItems();

    }//GEN-LAST:event_openIESectionAddExtractionButtonActionPerformed

    private void openIESectionAddPostprocessesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionAddPostprocessesButtonActionPerformed
        // TODO add your handling code here:

        IPostprocessorHandler postprocessorHandler = (IPostprocessorHandler) pluginLoader.getImplementedExtensions(IPostprocessorHandler.class).get(openIESectionPostprocessComboBox.getSelectedIndex());
        Postprocessor postprocessor = new Postprocessor().setPostprocessorHandler(SerializationUtils.clone(postprocessorHandler));

        openIePipelineListModel.addElement(postprocessor);
        openIePipelineDragDropList.printItems();

    }//GEN-LAST:event_openIESectionAddPostprocessesButtonActionPerformed

    private void openIESectionRemovePipelineElementButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionRemovePipelineElementButtonActionPerformed
        // TODO add your handling code here:

        openIePipelineListModel.removeElement(openIePipelineDragDropList.getSelectedValue());
    }//GEN-LAST:event_openIESectionRemovePipelineElementButtonActionPerformed

    private void openIESectionConfigurePipelineElementButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionConfigurePipelineElementButton1ActionPerformed
        // TODO add your handling code here:

        Object selectedPipelineElement = openIePipelineDragDropList.getSelectedValue();

        if (selectedPipelineElement != null) {
            if (selectedPipelineElement instanceof Extractor) {
                new ConfigDialog(((Extractor)selectedPipelineElement).getExtractorHandler().getAvailableConfigurations()).setVisible(true);
            } else if (selectedPipelineElement instanceof Preprocessor) {
                new ConfigDialog(((Preprocessor)selectedPipelineElement).getPreprocessorHandler().getAvailableConfigurations()).setVisible(true);
            } else if (selectedPipelineElement instanceof Crawler) {
                new ConfigDialog(((Crawler)selectedPipelineElement).getCrawlerhandler().getAvailableConfigurations()).setVisible(true);
            } else if (selectedPipelineElement instanceof Postprocessor) {
                new ConfigDialog(((Postprocessor)selectedPipelineElement).getPostprocessorHandler().getAvailableConfigurations()).setVisible(true);
            }
        }

    }//GEN-LAST:event_openIESectionConfigurePipelineElementButton1ActionPerformed

    private void openIESectionExecutePipelineElementButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIESectionExecutePipelineElementButtonActionPerformed
        // TODO add your handling code here:

        CrawlerPipeline crawlerPipeline = new CrawlerPipeline();
        PreprocessorPipeline preprocessorPipeline = new PreprocessorPipeline();
        PostprocessorPipeline postprocessorPipeline = new PostprocessorPipeline();
        ExtractorPipeline extractorPipeline = new ExtractorPipeline();
        OpenIePipeline openIePipeline = new OpenIePipeline();

        for (int i = 0; i< openIePipelineListModel.size(); i++) {
            Object selectedPipelineElement = openIePipelineListModel.get(i);

            if (selectedPipelineElement instanceof IExtractorPipelineElement) {
                IExtractorPipelineElement extractorPipelineElement = (IExtractorPipelineElement) selectedPipelineElement;
                extractorPipeline.addPipelineElement(extractorPipelineElement);
            } else if (selectedPipelineElement instanceof IPreprocessorPipelineElement) {
                IPreprocessorPipelineElement preprocessorPipelineElement = (IPreprocessorPipelineElement) selectedPipelineElement;
                preprocessorPipeline.addPipelineElement(preprocessorPipelineElement);
            } else if (selectedPipelineElement instanceof ICrawlerPipelineElement) {
                ICrawlerPipelineElement crawlerPipelineElement = (ICrawlerPipelineElement) selectedPipelineElement;
                crawlerPipeline.addPipelineElement(crawlerPipelineElement);
            } else if (selectedPipelineElement instanceof IPostprocessorPipelineElement) {
                IPostprocessorPipelineElement postprocessorPipelineElement = (IPostprocessorPipelineElement) selectedPipelineElement;
                postprocessorPipeline.addPipelineElement(postprocessorPipelineElement);
            }
        }

        openIePipeline.addPipelineElement(crawlerPipeline);
        openIePipeline.addPipelineElement(preprocessorPipeline);
        openIePipeline.addPipelineElement(extractorPipeline);
        openIePipeline.addPipelineElement(postprocessorPipeline);

        crawlerPipeline.setCrawlerPipelineHook(new ICrawlerPipelineHook() {

            JFrame crawlerProgress = new CrawlerProgress(crawlerPipeline);

            @Override
            public void willExecute() {
                crawlerProgress.setVisible(true);
            }

            @Override
            public void didExecute() {
                ((CrawlerProgress) crawlerProgress).stopTimer();
                crawlerProgress.dispose();
            }
        });

        preprocessorPipeline.setPreprocessorPipelineHook(new IPreprocessorPipelineHook() {

            JFrame preprocessorProgress = new PreprocessorProgress(preprocessorPipeline);

            @Override
            public void willExecute() {
                preprocessorProgress.setVisible(true);
            }

            @Override
            public void didExecute() {
                ((PreprocessorProgress) preprocessorProgress).stopTimer();
                preprocessorProgress.dispose();
            }
        });

        extractorPipeline.setExtractorPipelineHook(new IExtractorPipelineHook() {

            JFrame extractorProgress = new ExtractorProgress(extractorPipeline);

            @Override
            public void willExecute() {
                extractorProgress.setVisible(true);
            }

            @Override
            public void didExecute() {
                ((ExtractorProgress) extractorProgress).stopTimer();
                extractorProgress.dispose();

                if (postprocessorPipeline.getNumberOfPostprocessors() == 0) {
                    JFrame extractionViewer = new ExtractionViewer(new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("EXTRACTIONS_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator")))));
                    extractionViewer.setVisible(true);
                }
            }
        });

        postprocessorPipeline.setPostprocessorPipelineHook(new IPostprocessorPipelineHook() {

            JFrame postprocessorProgress = new PostprocessorProgress(postprocessorPipeline);

            @Override
            public void willExecute() {
                postprocessorProgress.setVisible(true);
            }

            @Override
            public void didExecute() {
                ((PostprocessorProgress) postprocessorProgress).stopTimer();
                postprocessorProgress.dispose();

                if (postprocessorPipeline.getNumberOfPostprocessors() > 0) {
                    JFrame extractionViewer = new ExtractionViewer(new File(System.getProperty("user.dir") + File.separator + new Config().getProperty("POSTPROCESSES_OUTPUT_RELATIVE_PATH").replaceAll("\\.", Matcher.quoteReplacement(System.getProperty("file.separator")))));
                    extractionViewer.setVisible(true);
                }
            }
        });

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws InterruptedException {
                try {
                    openIePipeline.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return "";
            }
        };

        worker.execute();

    }//GEN-LAST:event_openIESectionExecutePipelineElementButtonActionPerformed

    private void browseStartingDirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseStartingDirectoryButtonActionPerformed
        // TODO add your handling code here:

        browseStartingDirectory();

    }//GEN-LAST:event_browseStartingDirectoryButtonActionPerformed

    private void addEvaluationRelationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addEvaluationRelationButtonActionPerformed
        // TODO add your handling code here:

        File selectedDocument = extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex());
        String selectedSentence = extractionsEvaluationLabeller.getDocumentSentences(selectedDocument).get(evaluationSectionSentencesjList.getSelectedIndex());
        Relations relations = extractionsEvaluationLabeller.getRelationsFromDocument(selectedDocument);

        relations.addRelation(
                new Relation(
                        argument1EvaluationTextField.getText(),
                        relationEvaluationTextField.getText(),
                        argument2EvaluationTextField.getText(),
                        selectedDocument.getName(),
                        evaluationSectionSentencesjList.getSelectedIndex(),
                        selectedSentence
                ));

        argument1EvaluationTextField.setText("1st Argument");
        relationEvaluationTextField.setText("Relation");
        argument2EvaluationTextField.setText("2nd Argument");

        refreshEvaluationRelationsList();

    }//GEN-LAST:event_addEvaluationRelationButtonActionPerformed

    private void removeEvaluationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeEvaluationButtonActionPerformed
        // TODO add your handling code here:

        File selectedDocument = extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex());
        Relations relations = extractionsEvaluationLabeller.getRelationsFromDocument(selectedDocument);
        relations.removeRelation(evaluationSectionRelationsjList.getSelectedIndex());

        refreshEvaluationRelationsList();

    }//GEN-LAST:event_removeEvaluationButtonActionPerformed

    private void saveEvaluationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveEvaluationButtonActionPerformed
        // TODO add your handling code here:

        extractionsEvaluationLabeller.persist(extractionsEvaluationLabeller.getDocuments().get(evaluationSectionFilesjList.getSelectedIndex()));

    }//GEN-LAST:event_saveEvaluationButtonActionPerformed

    private void runEvaluationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runEvaluationButtonActionPerformed
        // TODO add your handling code here:

        ExtractionsEvaluation extractionsEvaluation = new ExtractionsEvaluation(new ExtractionsEvaluationModel());
        extractionsEvaluation.evaluate();

        ExtractionsEvaluationResult extractionsEvaluationResult = extractionsEvaluation.getExtractionsEvaluationResult();

        EvaluationViewer evaluationViewer = new EvaluationViewer(extractionsEvaluationResult);
        evaluationViewer.setVisible(true);

    }//GEN-LAST:event_runEvaluationButtonActionPerformed

    private void argument1EvaluationTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_argument1EvaluationTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_argument1EvaluationTextFieldActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e){ }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OpenIeJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addEvaluationRelationButton;
    private javax.swing.JLabel addNewRelationsLabel;
    private javax.swing.JLabel addedRelationsLabel;
    private javax.swing.JTextField argument1EvaluationTextField;
    private javax.swing.JTextField argument2EvaluationTextField;
    private javax.swing.JButton browseStartingDirectoryButton;
    private javax.swing.JLabel evaluationFilesLabel;
    private javax.swing.JList<String> evaluationSectionFilesjList;
    private javax.swing.JList<String> evaluationSectionSentencesjList;
    private javax.swing.JList<String> evaluationSectionRelationsjList;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator13;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private id.ac.itb.gui.dragdroplist.DragDropList openIePipelineDragDropList;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton loadPluginsButton;
    private javax.swing.JLabel loadPluginsLabel;
    private javax.swing.JLabel openExtractionPostprocessedViewerLabel;
    private javax.swing.JLabel openExtractionViewerLabel;
    private javax.swing.JButton openIESectionAddCrawlersButton;
    private javax.swing.JButton openIESectionAddExtractionButton;
    private javax.swing.JButton openIESectionAddPostprocessesButton;
    private javax.swing.JButton openIESectionAddPreprocessesButton;
    private javax.swing.JButton openIESectionConfigurePipelineElementButton1;
    private javax.swing.JComboBox<Object> openIESectionCrawlerComboBox;
    private javax.swing.JLabel openIESectionCrawlerLabel;
    private javax.swing.JButton openIESectionExecutePipelineElementButton;
    private javax.swing.JLabel openIESectionExecutionPipelineLabel;
    private javax.swing.JComboBox<Object> openIESectionExtractionComboBox;
    private javax.swing.JLabel openIESectionExtractionLabel;
    private javax.swing.JComboBox<Object> openIESectionPostprocessComboBox;
    private javax.swing.JLabel openIESectionPostprocessLabel;
    private javax.swing.JComboBox<Object> openIESectionPreprocessComboBox;
    private javax.swing.JLabel openIESectionPreprocessLabel;
    private javax.swing.JButton openIESectionRemovePipelineElementButton;
    private javax.swing.JTextField relationEvaluationTextField;
    private javax.swing.JButton removeEvaluationButton;
    private javax.swing.JButton runEvaluationButton;
    private javax.swing.JButton saveEvaluationButton;
    private javax.swing.JLabel sentencesLabel;
    private javax.swing.JLabel startingDirectoryLabel;
    // End of variables declaration//GEN-END:variables
}
