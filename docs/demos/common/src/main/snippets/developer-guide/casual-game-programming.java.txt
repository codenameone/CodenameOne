// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::casual-game-programming-java-001[]
public class Poker {
    private static final char SUITE_SPADE = 's';
    private static final char SUITE_HEART = 'h';
    private static final char SUITE_DIAMOND = 'd';
    private static final char SUITE_CLUB = 'c';

    private Resources cards;
    private Form current;
    private final static Card[] deck;

    static {
        // we initialize constant card values that will be useful later on in the game
        deck = new Card[52];
        for(int iter = 0 ; iter < 13 ; iter++) {
            deck[iter] = new Card(SUITE_SPADE, iter + 2);
            deck[iter + 13] = new Card(SUITE_HEART, iter + 2);
            deck[iter + 26] = new Card(SUITE_DIAMOND, iter + 2);
            deck[iter + 39] = new Card(SUITE_CLUB, iter + 2);
        }
    }

    /**
     * We use this method to calculate a "fake" DPI based on screen resolution rather than its actual DPI
     * this is useful so we can have large images on a tablet
     */
    private int calculateDPI() {
        int pixels = Display.getInstance().getDisplayHeight() * Display.getInstance().getDisplayWidth();
        if(pixels > 1000000) {
            return Display.DENSITY_HD;
        }
        if(pixels > 340000) {
            return Display.DENSITY_VERY_HIGH;
        }
        if(pixels > 150000) {
            return Display.DENSITY_HIGH;
        }
        return Display.DENSITY_MEDIUM;
    }

    /**
     * This method is invoked by Codename One once when the application loads
     */
    public void init(Object context) {
        try{
            // after loading the default theme we load the card images as a resource with
            // a fake DPI so they will be large enough. We store them in a resource rather
            // than as files so we can use the MultiImage functionality
            Resources theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
            cards = Resources.open("/gamedata.res", calculateDPI());
       } catch(IOException e) {
            e.printStackTrace();
       }
    }

    /**
     * This method is invoked by Codename One once when the application loads and when it is restarted
     */
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        showSplashScreen();
    }

    /**
     * The splash screen is relatively bare-bones. Its important to have a splash screen for iOS
     * since the build process generates a screenshot of this screen to speed up perceived performance
     */
    public void showSplashScreen() {
        final Form splash = new Form();

        // a border layout places components in the center and the 4 sides.
        // by default it scales the center component so here we configure
        // it to place the component in the actual center
        BorderLayout border = new BorderLayout();
        border.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        splash.setLayout(border);

        // by default the form's content pane is scrollable on the Y axis
        // we need to disable it here
        splash.setScrollable(false);
        Label title = new Label("Poker Ace");

        // The UIID is used to determine the appearance of the component in the theme
        title.setUIID("SplashTitle");
        Label subtitle = new Label("By Codename One");
        subtitle.setUIID("SplashSubTitle");

        splash.addComponent(BorderLayout.NORTH, title);
        splash.addComponent(BorderLayout.SOUTH, subtitle);
        Label as = new Label(cards.getImage("as.png"));
        Label ah = new Label(cards.getImage("ah.png"));
        Label ac = new Label(cards.getImage("ac.png"));
        Label ad = new Label(cards.getImage("ad.png"));

        // a layered layout places components one on top of the other in the same dimension, it is
        // useful for transparency but in this case we are using it for an animation
        final Container center = new Container(new LayeredLayout());
        center.addComponent(as);
        center.addComponent(ah);
        center.addComponent(ac);
        center.addComponent(ad);

        splash.addComponent(BorderLayout.CENTER, center);

        splash.show();
        splash.setTransitionOutAnimator(CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 800));

        // postpone the animation to the next cycle of the EDT to allow the UI to render fully once
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                // We replace the layout so the cards will be laid out in a line and animate the hierarchy
                // over 2 seconds, this effectively creates the effect of cards spreading out
                center.setLayout(new BoxLayout(BoxLayout.X_AXIS));
                center.setShouldCalcPreferredSize(true);
                splash.getContentPane().animateHierarchy(2000);

                // after showing the animation we wait for 2.5 seconds and then show the game with a nice
                // transition, notice that we use UI timer which is invoked on the Codename One EDT thread!
                new UITimer(new Runnable() {
                    public void run() {
                        showGameUI();
                    }
                }).schedule(2500, false, splash);
            }
        });
    }

    /**
     * This is the method that shows the game running, it is invoked to start or restart the game
     */
    private void showGameUI() {
        // we use the java.util classes to shuffle a new instance of the deck
        final List<Card> shuffledDeck = new ArrayList<Card>(Arrays.asList(deck));
        Collections.shuffle(shuffledDeck);

        final Form gameForm = new Form();
        gameForm.setTransitionOutAnimator(CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 800));
        Container gameFormBorderLayout = new Container(new BorderLayout());

        // while flow layout is the default in this case we want it to center into the middle of the screen
        FlowLayout fl = new FlowLayout(Component.CENTER);
        fl.setValign(Component.CENTER);
        final Container gameUpperLayer = new Container(fl);
        gameForm.setScrollable(false);

        // we place two layers in the game form, one contains the contents of the game and another one on top contains instructions
        // and overlays. In this case we only use it to write a hint to the user when he needs to swap his cards
        gameForm.setLayout(new LayeredLayout());
        gameForm.addComponent(gameFormBorderLayout);
        gameForm.addComponent(gameUpperLayer);

        // The game itself comprises 3 containers, one for each player containing a grid of 5 cards (grid layout
        // divides space evenly) and the deck of cards/dealer. Initially we show an animation where all the cards
        // gather into the deck, that is why we set the initial deck layout to show the whole deck 4×13
        final Container deckContainer = new Container(new GridLayout(4, 13));
        final Container playerContainer = new Container(new GridLayout(1, 5));
        final Container rivalContainer = new Container(new GridLayout(1, 5));

        // we place all the card images within the deck container for the initial animation
        for(int iter = 0 ; iter < deck.length ; iter++) {
            Label face = new Label(cards.getImage(deck[iter].getFileName()));

            // containers have no padding or margin this effectively removes redundant spacing
            face.setUIID("Container");
            deckContainer.addComponent(face);
        }

        // we place our cards at the bottom, the deck at the center and our rival on the north
        gameFormBorderLayout.addComponent(BorderLayout.CENTER, deckContainer);
        gameFormBorderLayout.addComponent(BorderLayout.NORTH, rivalContainer);
        gameFormBorderLayout.addComponent(BorderLayout.SOUTH, playerContainer);
        gameForm.show();

        // we wait 1.8 seconds to start the opening animation, otherwise it might start while the transition is still running
        new UITimer(new Runnable() {
            public void run() {
                // we add a card back component and make it a drop target so later players
                // can drag their cards here
                final Button cardBack = new Button(cards.getImage("card_back.png"));
                cardBack.setDropTarget(true);

                // we remove the button styling so it doesn't look like a button by using setUIID.
                cardBack.setUIID("Label");
                deckContainer.addComponent(cardBack);

                // we set the layout to layered layout which places all components one on top of the other then animate
                // the layout into place, this will cause the spread out deck to "flow" into place
                // Notice we are using the AndWait variant which will block the event dispatch thread (legally) while
                // performing the animation, normally you can't block the dispatch thread (EDT)
                deckContainer.setLayout(new LayeredLayout());
                deckContainer.animateLayoutAndWait(3000);


                // we don't need all the card images/labels in the deck, so we place the card back
                // on top then remove all the other components
                deckContainer.removeAll();
                deckContainer.addComponent(cardBack);

                // Now we iterate over the cards and deal the top card from the deck to each player
                for(int iter = 0 ; iter < 5 ; iter++) {
                    Card currentCard = shuffledDeck.get(0);
                    shuffledDeck.remove(0);
                    dealCard(cardBack, playerContainer, cards.getImage(currentCard.getFileName()), currentCard);
                    currentCard = shuffledDeck.get(0);
                    shuffledDeck.remove(0);
                    dealCard(cardBack, rivalContainer, cards.getImage("card_back.png"), currentCard);
                }

                // After dealing we place a notice in the upper layer by fade in. The trick is in adding a blank component
                // and replacing it with a fade transition
                TextArea notice = new TextArea("Drag cards to the deck to swap\ntap the deck to finish");
                notice.setEditable(false);
                notice.setFocusable(false);
                notice.setUIID("Label");
                notice.getUnselectedStyle().setAlignment(Component.CENTER);
                gameUpperLayer.addComponent(notice);
                gameUpperLayer.layoutContainer();

                // we place the notice then remove it without the transition, we need to do this since a text area
                // might resize itself so we need to know its size in advance to fade it in.
                Label temp = new Label(" ");
                temp.setPreferredSize(new Dimension(notice.getWidth(), notice.getHeight()));
                gameUpperLayer.replace(notice, temp, null);

                gameUpperLayer.layoutContainer();
                gameUpperLayer.replace(temp, notice, CommonTransitions.createFade(1500));

                // when the user taps the card back (the deck) we finish the game
                cardBack.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        // we clear the notice text
                        gameUpperLayer.removeAll();

                        // we deal the new cards to the player (the rival never takes new cards)
                        while(playerContainer.getComponentCount() < 5) {
                            Card currentCard = shuffledDeck.get(0);
                            shuffledDeck.remove(0);
                            dealCard(cardBack, playerContainer, cards.getImage(currentCard.getFileName()), currentCard);
                        }

                        // expose the rivals deck then offer the chance to play again...
                        for(int iter = 0 ; iter < 5 ; iter++) {
                            Button cardButton = (Button)rivalContainer.getComponentAt(iter);

                            // when creating a card we save the state into the component itself which is very convenient
                            Card currnetCard = (Card)cardButton.getClientProperty("card");
                            Label l = new Label(cards.getImage(currnetCard.getFileName()));
                            rivalContainer.replaceAndWait(cardButton, l, CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 300));
                        }

                        // notice dialogs are blocking by default so its pretty easy to write this logic
                        if(!Dialog.show("Again?", "Ready to play Again", "Yes", "Exit")) {
                            Display.getInstance().exitApplication();
                        }

                        // play again
                        showGameUI();
                    }
                });
            }
        }).schedule(1800, false, gameForm);
    }

    /**
     * A blocking method that creates the card deal animation and binds the drop logic when cards are dropped on the deck
     */
    private void dealCard(Component deck, final Container destination, Image cardImage, Card currentCard) {
        final Button card = new Button();
        card.setUIID("Label");
        card.setIcon(cardImage);

        // Components are normally placed by layout managers so setX/Y/Width/Height shouldn't be invoked. However,
        // in this case we want the layout animation to deal from a specific location. Notice that we use absoluteX/Y
        // since the default X/Y are relative to their parent container.
        card.setX(deck.getAbsoluteX());
        int deckAbsY = deck.getAbsoluteY();
        if(destination.getY() > deckAbsY) {
            card.setY(deckAbsY - destination.getAbsoluteY());
        } else {
            card.setY(deckAbsY);
        }
        card.setWidth(deck.getWidth());
        card.setHeight(deck.getHeight());
        destination.addComponent(card);

        // we save the model data directly into the component so we don't need to keep track of it. Later when we
        // need to check the card type a user touched we can just use getClientProperty
        card.putClientProperty("card", currentCard);
        destination.getParent().animateHierarchyAndWait(400);
        card.setDraggable(true);

        // when the user drops a card on a drop target (currently only the deck) we remove it and animate it out
        card.addDropListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
                card.getParent().removeComponent(card);
                destination.animateLayout(300);
            }
        });
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }


    static class Card {
        private char suite;
        private int rank;

        public Card(char suite, int rank) {
            this.suite = suite;
            this.rank = rank;
        }

        private String rankToString() {
            if(rank > 10) {
                switch(rank) {
                    case 11:
                        return "j";
                    case 12:
                        return "q";
                    case 13:
                        return "k";
                    case 14:
                        return "a";
                }
            }
            return "" + rank;
        }

        public String getFileName() {
            return rankToString() + suite + ".png";
        }
    }
}
// end::casual-game-programming-java-001[]
