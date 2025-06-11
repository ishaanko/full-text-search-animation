import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FullTextSearchAnimation extends JPanel {
    private final String[] docs = {
        "The quick brown fox jumps over the lazy dog",
        "A lazy cat sleeps peacefully in the warm sun",
        "The brown dog runs quickly through the green park",
        "Quick reflexes help the clever fox survive in nature",
    };
    
    private final Set<String> stopWords = new HashSet<>(Arrays.asList(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", 
        "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", 
        "will", "with", "or", "but", "not", "this", "these", "they", "them", 
        "their", "have", "had", "do", "does", "did", "can", "could", "should", 
        "would", "may", "might", "must", "shall", "we", "you", "i", "me", "my", 
        "our", "us", "his", "her", "him", "she"
    ));
    
    private final Map<String, Set<Integer>> invertedIndex = new HashMap<>();
    private String query = "";
    private final JTextField searchField = new JTextField(15);
    private final JButton buildButton = new JButton("Build Index");
    private final JButton clearButton = new JButton("Clear");
    private boolean indexBuilt = false;
    private Set<Integer> currentResults = new HashSet<>();
    
    // stuff for the animation
    private boolean isBuilding = false;
    private int currentDocIndex = -1;
    private String currentToken = "";
    private Set<String> currentTokens = new HashSet<>();
    private final javax.swing.Timer animationTimer;
    private final java.util.List<String> animationSteps = new ArrayList<>();
    private int currentStep = 0;
    private long stepStartTime = 0;
    private final int STEP_DURATION = 800;
    
    // colors
    private final Color PRIMARY_COLOR = new Color(51, 122, 183);
    private final Color SUCCESS_COLOR = new Color(92, 184, 92);
    private final Color HIGHLIGHT_COLOR = new Color(255, 235, 59, 150);
    private final Color DOCUMENT_COLOR = new Color(248, 249, 250);
    private final Color BORDER_COLOR = new Color(222, 226, 230);

    public FullTextSearchAnimation() {
        setPreferredSize(new Dimension(1600, 1000));
        setBackground(Color.WHITE);
        setLayout(null);
        
        // timer for the stepping animation
        animationTimer = new javax.swing.Timer(50, e -> {
            if (isBuilding && System.currentTimeMillis() - stepStartTime > STEP_DURATION) {
                nextAnimationStep();
            }
            repaint();
        });

        // set up search field
        searchField.setBounds(50, 20, 300, 40);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });

        // buttons
        buildButton.setBounds(370, 20, 160, 40);
        clearButton.setBounds(550, 20, 120, 40);
        
        // just use system fonts
        buildButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        clearButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        buildButton.addActionListener(e -> {
            if (!isBuilding) {
                startAnimatedIndexBuilding();
            }
        });
        
        clearButton.addActionListener(e -> {
            if (!isBuilding) {
                resetAnimation();
            }
        });

        add(searchField);
        add(buildButton);
        add(clearButton);
        
        // placeholder text stuff
        searchField.setText("Enter search term...");
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Enter search term...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Enter search term...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
    }
    
    private void startAnimatedIndexBuilding() {
        isBuilding = true;
        indexBuilt = false;
        invertedIndex.clear();
        currentResults.clear();
        currentDocIndex = -1;
        currentToken = "";
        currentTokens.clear();
        animationSteps.clear();
        currentStep = 0;
        
        // build the list of animation steps
        for (int i = 0; i < docs.length; i++) {
            animationSteps.add("PROCESS_DOC:" + i);
            Set<String> tokens = tokenizeAndFilter(docs[i]);
            for (String token : tokens) {
                animationSteps.add("ADD_TOKEN:" + i + ":" + token);
            }
        }
        animationSteps.add("COMPLETE");
        
        buildButton.setText("Building...");
        buildButton.setEnabled(false);
        searchField.setEnabled(false);
        
        stepStartTime = System.currentTimeMillis();
        animationTimer.start();
    }
    
    private void nextAnimationStep() {
        if (currentStep >= animationSteps.size()) {
            completeAnimation();
            return;
        }
        
        String step = animationSteps.get(currentStep);
        String[] parts = step.split(":");
        
        switch (parts[0]) {
            case "PROCESS_DOC":
                currentDocIndex = Integer.parseInt(parts[1]);
                currentTokens = tokenizeAndFilter(docs[currentDocIndex]);
                currentToken = "";
                break;
                
            case "ADD_TOKEN":
                int docId = Integer.parseInt(parts[1]);
                currentToken = parts[2];
                invertedIndex.computeIfAbsent(currentToken, k -> new HashSet<>()).add(docId);
                break;
                
            case "COMPLETE":
                completeAnimation();
                return;
        }
        
        currentStep++;
        stepStartTime = System.currentTimeMillis();
    }
    
    private void completeAnimation() {
        isBuilding = false;
        indexBuilt = true;
        currentDocIndex = -1;
        currentToken = "";
        currentTokens.clear();
        
        buildButton.setText("Rebuild Index");
        buildButton.setEnabled(true);
        searchField.setEnabled(true);
        
        animationTimer.stop();
        performSearch();
    }
    
    private void resetAnimation() {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        isBuilding = false;
        indexBuilt = false;
        invertedIndex.clear();
        currentResults.clear();
        currentDocIndex = -1;
        currentToken = "";
        currentTokens.clear();
        animationSteps.clear();
        currentStep = 0;
        
        searchField.setText("");
        query = "";
        buildButton.setText("Build Index");
        buildButton.setEnabled(true);
        searchField.setEnabled(true);
        
        repaint();
    }

    private void performSearch() {
        if (!indexBuilt) return;
        
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty() || searchText.equals("enter search term...")) {
            query = "";
            currentResults.clear();
        } else {
            // use the same tokenization for search too
            Set<String> queryTokens = tokenizeAndFilter(searchText);
            
            if (queryTokens.isEmpty()) {
                query = searchText; // keep original if no tokens left
                currentResults.clear();
            } else {
                // just use first token for now (TODO: handle multiple words)
                query = queryTokens.iterator().next();
                currentResults = invertedIndex.getOrDefault(query, new HashSet<>());
            }
        }
        repaint();
    }

    private void buildIndex() {
        invertedIndex.clear();
        for (int i = 0; i < docs.length; i++) {
            Set<String> tokens = tokenizeAndFilter(docs[i]);
            for (String token : tokens) {
                invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(i);
            }
        }
    }
    
    /**
     * break text into words and filter out stop words
     */
    private Set<String> tokenizeAndFilter(String text) {
        Set<String> tokens = new HashSet<>();
        
        // lowercase and split on anything that's not a word character
        String[] words = text.toLowerCase().split("\\W+");
        
        for (String word : words) {
            // skip empty strings, stop words, and single letters
            if (!word.isEmpty() && !stopWords.contains(word) && word.length() > 1) {
                // basic stemming
                String stemmed = simpleStem(word);
                tokens.add(stemmed);
            }
        }
        
        return tokens;
    }
    
    /**
     * really basic stemming - just chops off common endings
     */
    private String simpleStem(String word) {
        // strip off common suffixes
        if (word.endsWith("ing") && word.length() > 5) {
            return word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ed") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("er") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("est") && word.length() > 5) {
            return word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ly") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && word.length() > 3 && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        drawHeader(g2d);
        drawDocuments(g2d);
        drawInvertedIndex(g2d);
        drawSearchResults(g2d);
    }
    
    private void drawHeader(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(PRIMARY_COLOR);
        g.drawString("Full-Text Search with Stop Words & Stemming", 700, 40);
        
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        if (isBuilding) {
            g.setColor(new Color(255, 152, 0)); // orange while building
            g.drawString("Building index... Processing Doc " + (currentDocIndex + 1) + 
                        (currentToken.isEmpty() ? "" : " - Token: \"" + currentToken + "\""), 700, 65);
        } else if (!indexBuilt) {
            g.setColor(Color.GRAY);
            g.drawString("Click 'Build Index' to start (removes stop words, applies stemming)", 700, 65);
        } else {
            g.setColor(SUCCESS_COLOR);
            g.drawString("Index built! Filtered stop words and applied basic stemming...", 700, 65);
        }
    }
    
    private void drawDocuments(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.BLACK);
        g.drawString("Documents:", 50, 120);
        
        // only show a few docs so they fit nicely on screen
        int maxDocsToShow = Math.min(docs.length, 4); 
        int docHeight = 100; 
        int spacing = 120; 
        
        for (int i = 0; i < maxDocsToShow; i++) {
            int y = 150 + i * spacing;
            
            // figure out what state this doc is in
            boolean isCurrentlyProcessing = isBuilding && i == currentDocIndex;
            boolean isProcessed = isBuilding ? i < currentDocIndex : false;
            boolean isMatching = currentResults.contains(i);
            
            // draw the background based on state
            if (isCurrentlyProcessing) {
                // pulse effect for the one being processed
                long time = System.currentTimeMillis();
                float pulse = (float) (0.5 + 0.5 * Math.sin(time * 0.01));
                Color processingColor = new Color(255, 193, 7, (int)(100 + 50 * pulse));
                g.setColor(processingColor);
                g.fillRoundRect(50, y, 600, docHeight, 12, 12);
                g.setColor(new Color(255, 152, 0));
                g.setStroke(new BasicStroke(4));
                g.drawRoundRect(50, y, 600, docHeight, 12, 12);
            } else if (isProcessed) {
                // green for done
                g.setColor(new Color(200, 230, 200));
                g.fillRoundRect(50, y, 600, docHeight, 12, 12);
                g.setColor(new Color(76, 175, 80));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(50, y, 600, docHeight, 12, 12);
            } else if (isMatching) {
                g.setColor(HIGHLIGHT_COLOR);
                g.fillRoundRect(50, y, 600, docHeight, 12, 12);
                g.setColor(SUCCESS_COLOR);
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(50, y, 600, docHeight, 12, 12);
            } else {
                g.setColor(DOCUMENT_COLOR);
                g.fillRoundRect(50, y, 600, docHeight, 12, 12);
                g.setColor(BORDER_COLOR);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(50, y, 600, docHeight, 12, 12);
            }
            
            // doc title
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(PRIMARY_COLOR);
            g.drawString("Doc " + (i + 1), 70, y + 30);
            
            // if we're processing this doc, show the tokens
            if (isCurrentlyProcessing && !currentTokens.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.setColor(new Color(255, 87, 34));
                StringBuilder tokensStr = new StringBuilder("Tokens: ");
                for (String token : currentTokens) {
                    if (token.equals(currentToken)) {
                        tokensStr.append("[").append(token).append("] ");
                    } else {
                        tokensStr.append(token).append(" ");
                    }
                }
                String tokenDisplay = tokensStr.toString();
                if (tokenDisplay.length() > 60) {
                    tokenDisplay = tokenDisplay.substring(0, 57) + "...";
                }
                g.drawString(tokenDisplay, 70, y + 55);
                
                // show original text too
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.setColor(Color.GRAY);
                String docText = docs[i];
                if (docText.length() > 55) {
                    docText = docText.substring(0, 52) + "...";
                }
                g.drawString("Original: " + docText, 70, y + 75);
            } else {
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.setColor(Color.BLACK);
                
                // cut off long text and highlight matches
                String docText = docs[i];
                if (docText.length() > 60) {
                    docText = docText.substring(0, 57) + "...";
                }
                
                if (!query.isEmpty() && currentResults.contains(i)) {
                    drawHighlightedText(g, docText, query, 70, y + 55);
                } else {
                    g.drawString(docText, 70, y + 55);
                }
                
                // wrap to second line if needed
                if (!query.isEmpty() && currentResults.contains(i) && docs[i].length() > 60) {
                    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    String remainingText = docs[i].substring(57);
                    if (remainingText.length() > 60) {
                        remainingText = remainingText.substring(0, 57) + "...";
                    }
                    g.drawString(remainingText, 70, y + 75);
                }
            }
        }
        
        // show if there are more docs
        if (docs.length > maxDocsToShow) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 16));
            g.setColor(Color.GRAY);
            g.drawString("... and " + (docs.length - maxDocsToShow) + " more documents", 70, 150 + maxDocsToShow * spacing + 20);
        }
    }
    
    private void drawHighlightedText(Graphics2D g, String text, String highlight, int x, int y) {
        String[] words = text.split(" ");
        int currentX = x;
        FontMetrics fm = g.getFontMetrics();
        
        for (String word : words) {
            if (word.toLowerCase().contains(highlight.toLowerCase())) {
                // Draw highlighted background
                int wordWidth = fm.stringWidth(word);
                g.setColor(new Color(255, 193, 7, 100));
                g.fillRect(currentX - 2, y - fm.getAscent(), wordWidth + 4, fm.getHeight());
                
                // Draw word in bold
                Font originalFont = g.getFont();
                g.setFont(originalFont.deriveFont(Font.BOLD));
                g.setColor(Color.BLACK);
                g.drawString(word, currentX, y);
                g.setFont(originalFont);
            } else {
                g.setColor(Color.BLACK);
                g.drawString(word, currentX, y);
            }
            currentX += fm.stringWidth(word + " ");
        }
    }
    
    private void drawInvertedIndex(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.BLACK);
        g.drawString("Inverted Index:", 700, 120);
        
        if (!indexBuilt && !isBuilding) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 16));
            g.setColor(Color.GRAY);
            g.drawString("Index will appear here during building...", 720, 155);
            return;
        }
        
        // keep them in order
        List<Map.Entry<String, Set<Integer>>> sortedEntries = new ArrayList<>(invertedIndex.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        
        int y = 155;
        int maxEntries = 20; // don't show too many or they won't fit
        int entriesShown = 0;
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        for (Map.Entry<String, Set<Integer>> entry : sortedEntries) {
            if (entriesShown >= maxEntries) break;
            
            String word = entry.getKey();
            Set<Integer> docIds = entry.getValue();
            
            // highlight the current token or search term
            boolean isCurrentToken = isBuilding && word.equals(currentToken);
            boolean isQueryToken = word.equals(query);
            
            if (isCurrentToken) {
                // pulse the one being added
                long time = System.currentTimeMillis();
                float pulse = (float) (0.5 + 0.5 * Math.sin(time * 0.02));
                Color tokenColor = new Color(255, 193, 7, (int)(80 + 70 * pulse));
                g.setColor(tokenColor);
                g.fillRect(700, y - 16, 350, 20);
                g.setColor(new Color(255, 87, 34));
                g.setFont(g.getFont().deriveFont(Font.BOLD));
            } else if (isQueryToken) {
                g.setColor(new Color(255, 193, 7, 120));
                g.fillRect(700, y - 16, 350, 20);
                g.setColor(PRIMARY_COLOR);
                g.setFont(g.getFont().deriveFont(Font.BOLD));
            } else {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.PLAIN, 14));
            }
            
            String indexEntry = String.format("%-15s → %s", word, docIds.toString());
            g.drawString(indexEntry, 720, y);
            y += 22;
            entriesShown++;
        }
        
        // show if there are more terms
        if (sortedEntries.size() > maxEntries) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g.setColor(Color.GRAY);
            g.drawString("... and " + (sortedEntries.size() - maxEntries) + " more terms", 720, y + 15);
        }
        
        // progress bar
        if (isBuilding) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(255, 152, 0));
            int progress = (int) ((double) currentStep / animationSteps.size() * 100);
            g.drawString("Building progress: " + progress + "%", 700, y + 40);
        }
    }
    
    private void drawSearchResults(Graphics2D g) {
        if (query.isEmpty() || (!indexBuilt && !isBuilding)) return;
        
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(SUCCESS_COLOR);
        
        if (currentResults.isEmpty()) {
            if (isBuilding) {
                g.setColor(Color.GRAY);
                g.drawString("Search will be available after indexing completes...", 700, 880);
            } else {
                g.drawString("No results found for: \"" + query + "\"", 700, 880);
            }
        } else {
            g.drawString("Found \"" + query + "\" in " + currentResults.size() + 
                        " document(s): " + currentResults, 700, 880);
            
            // list which docs matched
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(Color.DARK_GRAY);
            StringBuilder matchingDocs = new StringBuilder("Matching documents: ");
            for (Integer docId : currentResults) {
                matchingDocs.append("Doc ").append(docId + 1).append(" ");
            }
            g.drawString(matchingDocs.toString(), 700, 910);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Full-Text Search Animation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new FullTextSearchAnimation());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}
