/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.ac.itb.gui.config;

import javax.swing.*;

/**
 *
 * @author elvanowen
 */
public class CrawlerConfig extends javax.swing.JFrame {

    /**
     * Creates new form CrawlerConfig
     */
    public CrawlerConfig() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        maxPagesToFetchLabel = new javax.swing.JLabel();
        maxPagesToFetchTextField = new javax.swing.JTextField();
        numberOfCrawlersLabel = new javax.swing.JLabel();
        numberOfCrawlersTextField = new javax.swing.JTextField();
        maxDepthofCrawlingLabel = new javax.swing.JLabel();
        maxDepthofCrawlingTextField = new javax.swing.JTextField();
        userAgentStringTextField = new javax.swing.JTextField();
        userAgentStringLabel = new javax.swing.JLabel();
        regexFilterPatternLabel = new javax.swing.JLabel();
        regexFilterPatternTextField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        maxPagesToFetchLabel.setText("Max Pages to Fetch");

        maxPagesToFetchTextField.setText("jTextField1");

        numberOfCrawlersLabel.setText("Number of Crawlers");

        numberOfCrawlersTextField.setText("jTextField1");

        maxDepthofCrawlingLabel.setText("Max Depth of Crawling");

        maxDepthofCrawlingTextField.setText("jTextField1");

        userAgentStringTextField.setText("jTextField1");

        userAgentStringLabel.setText("User Agent String");

        regexFilterPatternLabel.setText("Regex Filter Pattern");

        regexFilterPatternTextField.setText("jTextField1");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(regexFilterPatternLabel)
                        .addGap(34, 34, 34)
                        .addComponent(regexFilterPatternTextField))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(maxPagesToFetchLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(maxPagesToFetchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(numberOfCrawlersLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                        .addComponent(numberOfCrawlersTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(maxDepthofCrawlingLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(maxDepthofCrawlingTextField))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(userAgentStringLabel)
                        .addGap(46, 46, 46)
                        .addComponent(userAgentStringTextField)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxPagesToFetchLabel)
                    .addComponent(maxPagesToFetchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfCrawlersLabel)
                    .addComponent(numberOfCrawlersTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxDepthofCrawlingLabel)
                    .addComponent(maxDepthofCrawlingTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userAgentStringLabel)
                    .addComponent(userAgentStringTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(regexFilterPatternLabel)
                    .addComponent(regexFilterPatternTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
        setTitle("Crawler Configuration");
    }// </editor-fold>//GEN-END:initComponents

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
                new CrawlerConfig().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel maxDepthofCrawlingLabel;
    private javax.swing.JTextField maxDepthofCrawlingTextField;
    private javax.swing.JLabel maxPagesToFetchLabel;
    private javax.swing.JTextField maxPagesToFetchTextField;
    private javax.swing.JLabel numberOfCrawlersLabel;
    private javax.swing.JTextField numberOfCrawlersTextField;
    private javax.swing.JLabel regexFilterPatternLabel;
    private javax.swing.JTextField regexFilterPatternTextField;
    private javax.swing.JLabel userAgentStringLabel;
    private javax.swing.JTextField userAgentStringTextField;
    // End of variables declaration//GEN-END:variables
}
