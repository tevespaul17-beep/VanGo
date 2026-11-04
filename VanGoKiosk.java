import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class VanGoKiosk extends JFrame {

    // =========================================================================
    // PREMIUM DESIGN SYSTEM - DEEP NAVY & GOLD ACCENT
    // =========================================================================
    
    // Background & Surface System
    private static final Color BG_DEEP        = new Color(10, 15, 30);       // Deep midnight navy
    private static final Color BG_SURFACE     = new Color(20, 28, 48);       // Rich navy surface
    private static final Color BG_ELEVATED    = new Color(28, 38, 62);       // Elevated card surface
    private static final Color BG_GLASS       = new Color(35, 45, 72);       // Glass-like overlay
    
    // Gold Accent System (Premium feel)
    private static final Color GOLD_PRIMARY   = new Color(212, 175, 55);     // Classic gold
    private static final Color GOLD_LIGHT     = new Color(240, 210, 120);    // Light gold highlight
    private static final Color GOLD_DIM       = new Color(160, 130, 40);     // Dimmed gold
    
    // Text Colors
    private static final Color TEXT_PRIMARY   = new Color(248, 250, 252);    // Near white
    private static final Color TEXT_SECONDARY = new Color(180, 190, 210);    // Soft silver
    private static final Color TEXT_MUTED     = new Color(120, 135, 160);    // Muted slate
    
    // Border & Divider
    private static final Color BORDER_SUBTLE  = new Color(50, 65, 95);       // Subtle border
    private static final Color BORDER_GOLD    = new Color(80, 70, 40);       // Gold-tinted border

    // Semantic Status Colors
    private static final Color SEMANTIC_SUCCESS = new Color(34, 197, 94);    // Emerald green
    private static final Color SEMANTIC_INFO    = new Color(59, 130, 246);   // Bright blue
    private static final Color SEMANTIC_DANGER  = new Color(239, 68, 68);    // Red
    private static final Color SEMANTIC_WARNING = new Color(245, 158, 11);   // Amber
    private static final Color SEMANTIC_PREMIUM = new Color(168, 85, 247);   // Purple

    // =========================================================================
    // DATA MODELS
    // =========================================================================
    
    public static class Driver {
        String id, name, licenseNo, phone;
        public Driver(String id, String name, String licenseNo, String phone) {
            this.id = id; this.name = name; this.licenseNo = licenseNo; this.phone = phone;
        }
        @Override public String toString() { return name + " (" + licenseNo + ")"; }
    }

    public static class Van {
        String id, model, plateNumber;
        int capacity;
        Driver driver;
        public Van(String id, String model, String plateNumber, int capacity, Driver driver) {
            this.id = id; this.model = model; this.plateNumber = plateNumber;
            this.capacity = capacity; this.driver = driver;
        }
        @Override public String toString() { return model + " [" + plateNumber + "]"; }
    }

    public static class Schedule {
        String id, destination, departureTime;
        double fare;
        Van van;
        Set<Integer> bookedSeats;
        public Schedule(String id, String destination, String departureTime, double fare, Van van) {
            this.id = id; this.destination = destination; this.departureTime = departureTime;
            this.fare = fare; this.van = van; this.bookedSeats = new HashSet<>();
        }
    }

    public static class Booking {
        String referenceNo;
        Schedule schedule;
        List<Integer> selectedSeats;
        String passengerName, passengerContact;
        double totalAmount;
        String status;
        public Booking(String referenceNo, Schedule schedule, List<Integer> selectedSeats,
                       String passengerName, String passengerContact, double totalAmount) {
            this.referenceNo = referenceNo; this.schedule = schedule;
            this.selectedSeats = selectedSeats; this.passengerName = passengerName;
            this.passengerContact = passengerContact; this.totalAmount = totalAmount;
            this.status = "PENDING PAYMENT";
        }
    }

    // =========================================================================
    // STATE & DATA
    // =========================================================================
    private List<Driver> drivers;
    private List<Van> vans;
    private List<Schedule> schedules;
    private Schedule selectedSchedule;
    private final List<Integer> currentSelectedSeats = new ArrayList<>();
    private final Map<String, Booking> bookingDatabase = new HashMap<>();

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JPanel welcomeView, scheduleView, seatSelectionView, summaryView, adminView;

    private JPanel scheduleGridContainer, seatGridContainer;
    private JLabel lblSelectedSeatsText, lblTotalFareText;

    private JLabel lblSumRef, lblSumDest, lblSumTime, lblSumVan, lblSumDriver, lblSumSeats, lblSumTotal;
    private JTextField txtPassengerName, txtPassengerContact;

    private DefaultTableModel modelSchedules, modelVans, modelDrivers;
    private JComboBox<Van> cbScheduleVan;
    private JComboBox<Driver> cbVanDriver;

    public VanGoKiosk() {
        initData();
        initUI();
    }

    private void initData() {
        drivers = new ArrayList<>();
        vans = new ArrayList<>();
        schedules = new ArrayList<>();

        Driver d1 = new Driver("DRV01", "Juan Dela Cruz", "D01-23-098765", "0917-123-4567");
        Driver d2 = new Driver("DRV02", "Ricardo Dalisay", "D02-19-112233", "0918-987-6543");
        Driver d3 = new Driver("DRV03", "Johanna Nicole Bacalso", "D05-21-445566", "0920-555-0199");
        Driver d4 = new Driver("DRV04", "Albert Xyron Cabillo", "D03-22-778899", "0921-777-8899");
        drivers.add(d1); drivers.add(d2); drivers.add(d3); drivers.add(d4);

        Van v1 = new Van("VAN01", "Toyota HiAce Commuter", "HA-9821", 14, d1);
        Van v2 = new Van("VAN02", "Toyota HiAce Deluxe Commuter", "HD-5512", 13, d2);
        Van v3 = new Van("VAN03", "Nissan NV350 Urvan", "NV-4410", 12, d3);
        Van v4 = new Van("VAN04", "Nissan Urvan Cargo", "NC-1092", 5, d4);

        vans.add(v1); vans.add(v2); vans.add(v3); vans.add(v4);

        Schedule s1 = new Schedule("SCH001", "Tacloban City", "06:00 AM", 250.00, v1);
        Schedule s2 = new Schedule("SCH002", "Maasin City", "08:30 AM", 300.00, v2);
        Schedule s3 = new Schedule("SCH003", "Cebu City (via Port)", "10:00 AM", 450.00, v3);
        Schedule s4 = new Schedule("SCH004", "Ormoc City", "01:00 PM", 200.00, v4);
        Schedule s5 = new Schedule("SCH005", "Tacloban City", "03:30 PM", 250.00, v1);

        s1.bookedSeats.add(1); s1.bookedSeats.add(2);
        s2.bookedSeats.add(4); s2.bookedSeats.add(8);

        schedules.add(s1); schedules.add(s2); schedules.add(s3); schedules.add(s4); schedules.add(s5);
    }

    private void initUI() {
        setTitle("VANGO • Premium Terminal Kiosk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 860);
        setMinimumSize(new Dimension(1024, 760));
        setLocationRelativeTo(null);
        setBackground(BG_DEEP);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setBackground(BG_DEEP);

        welcomeView = buildWelcomeView();
        scheduleView = buildScheduleView();
        seatSelectionView = buildSeatSelectionView();
        summaryView = buildSummaryView();
        adminView = buildAdminView();

        mainContainer.add(welcomeView, "WELCOME");
        mainContainer.add(scheduleView, "SCHEDULE");
        mainContainer.add(seatSelectionView, "SEATS");
        mainContainer.add(summaryView, "SUMMARY");
        mainContainer.add(adminView, "ADMIN");

        add(mainContainer);
        cardLayout.show(mainContainer, "WELCOME");
    }

    // =========================================================================
    // PREMIUM UI COMPONENTS
    // =========================================================================

    private JButton createPremiumButton(String text, Color baseColor, boolean outlined, int fontSize) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int arc = 12;

                if (outlined) {
                    GradientPaint gp = new GradientPaint(0, 0, BG_ELEVATED, 0, h, BG_GLASS);
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
                    g2.setColor(baseColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
                } else {
                    Color topColor = lighten(baseColor, 0.15f);
                    Color bottomColor = darken(baseColor, 0.1f);
                    GradientPaint gp = new GradientPaint(0, 0, topColor, 0, h, bottomColor);
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h / 2, arc, arc));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        btn.setForeground(outlined ? baseColor : BG_DEEP);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(12, 24, 12, 24));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(outlined ? GOLD_LIGHT : Color.WHITE);
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(outlined ? baseColor : BG_DEEP);
                btn.repaint();
            }
        });

        return btn;
    }

    private static Color lighten(Color c, float factor) {
        return new Color(
            Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor)),
            Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor)),
            Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor))
        );
    }

    private static Color darken(Color c, float factor) {
        return new Color(
            (int) (c.getRed() * (1 - factor)),
            (int) (c.getGreen() * (1 - factor)),
            (int) (c.getBlue() * (1 - factor))
        );
    }

    private JPanel createPremiumCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0, BG_ELEVATED, 0, h, BG_SURFACE);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));

                g2.setColor(BORDER_GOLD);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 16, 16));

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        return card;
    }

    private JPanel createPremiumHeader(String stepText, String titleText, boolean showAdmin, boolean showHome) {
        JPanel header = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_SURFACE, getWidth(), 0, BG_DEEP);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(GOLD_PRIMARY);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 32, 22, 32));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel lblStep = new JLabel("◆ " + stepText.toUpperCase());
        lblStep.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStep.setForeground(GOLD_PRIMARY);

        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(TEXT_PRIMARY);

        textGroup.add(lblStep);
        textGroup.add(Box.createRigidArea(new Dimension(0, 6)));
        textGroup.add(lblTitle);

        header.add(textGroup, BorderLayout.WEST);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        navBtns.setOpaque(false);

        if (showHome) {
            JButton btnHome = createPremiumButton("← Start Over", GOLD_PRIMARY, true, 13);
            btnHome.setPreferredSize(new Dimension(150, 42));
            btnHome.addActionListener(e -> cardLayout.show(mainContainer, "WELCOME"));
            navBtns.add(btnHome);
        }
        if (showAdmin) {
            JButton btnAdmin = createPremiumButton("◆ Staff Portal", GOLD_PRIMARY, true, 13);
            btnAdmin.setPreferredSize(new Dimension(150, 42));
            btnAdmin.addActionListener(e -> {
                refreshAdminTables();
                cardLayout.show(mainContainer, "ADMIN");
            });
            navBtns.add(btnAdmin);
        }

        header.add(navBtns, BorderLayout.EAST);
        return header;
    }

    private JTextField createPremiumTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DEEP);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(hasFocus() ? GOLD_PRIMARY : BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(GOLD_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBorder(new EmptyBorder(10, 14, 10, 14));
        return field;
    }

    // =========================================================================
    // SCREEN 0: WELCOME VIEW
    // =========================================================================
    private JPanel buildWelcomeView() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint bg = new GradientPaint(0, 0, BG_DEEP, getWidth(), getHeight(), new Color(5, 8, 18));
                g2.setPaint(bg);
                g2.fillRect(0, 0, getWidth(), getHeight());

                RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(getWidth() / 2f, getHeight() / 2f - 50f),
                    getWidth() / 2f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(212, 175, 55, 18), new Color(0, 0, 0, 0)}
                );
                g2.setPaint(glow);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();
            }
        };

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(28, 40, 0, 40));

        JLabel brand = new JLabel("◆ VANGO PREMIUM TERMINAL");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 15));
        brand.setForeground(GOLD_PRIMARY);

        JButton btnAdmin = createPremiumButton("Staff Portal", GOLD_PRIMARY, true, 13);
        btnAdmin.setPreferredSize(new Dimension(160, 42));
        btnAdmin.addActionListener(e -> {
            refreshAdminTables();
            cardLayout.show(mainContainer, "ADMIN");
        });

        topBar.add(brand, BorderLayout.WEST);
        topBar.add(btnAdmin, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel tagline = new JLabel("PREMIUM VAN TRAVEL");
        tagline.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tagline.setForeground(GOLD_PRIMARY);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Travel in First Class Comfort");
        title.setFont(new Font("Segoe UI", Font.BOLD, 46));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Reserve your seat, choose your journey, and arrive in style.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnStart = createPremiumButton("RESERVE YOUR SEAT  →", GOLD_PRIMARY, false, 17);
        btnStart.setPreferredSize(new Dimension(340, 62));
        btnStart.setMaximumSize(new Dimension(340, 62));
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStart.addActionListener(e -> {
            renderScheduleCards();
            cardLayout.show(mainContainer, "SCHEDULE");
        });

        center.add(Box.createVerticalGlue());
        center.add(tagline);
        center.add(Box.createRigidArea(new Dimension(0, 16)));
        center.add(title);
        center.add(Box.createRigidArea(new Dimension(0, 14)));
        center.add(subtitle);
        center.add(Box.createRigidArea(new Dimension(0, 50)));
        center.add(btnStart);
        center.add(Box.createVerticalGlue());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 0, 30, 0));

        JLabel status = new JLabel("● LIVE  ·  ALL DEPARTURES ON SCHEDULE  ·  SECURE BOOKING");
        status.setFont(new Font("Segoe UI", Font.BOLD, 11));
        status.setForeground(SEMANTIC_SUCCESS);
        footer.add(status);

        root.add(topBar, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    // =========================================================================
    // SCREEN 1: SCHEDULE VIEW
    // =========================================================================
    private JPanel buildScheduleView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DEEP);
        root.add(createPremiumHeader("Step 1 of 3", "Select Your Journey", true, true), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBorder(new EmptyBorder(24, 32, 24, 32));
        mainContent.setOpaque(false);

        scheduleGridContainer = new JPanel(new GridLayout(0, 2, 22, 22));
        scheduleGridContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(scheduleGridContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setBackground(BG_DEEP);

        mainContent.add(scroll, BorderLayout.CENTER);
        root.add(mainContent, BorderLayout.CENTER);
        return root;
    }

    private void renderScheduleCards() {
        scheduleGridContainer.removeAll();

        for (Schedule s : schedules) {
            JPanel card = createPremiumCard();
            card.setLayout(new BorderLayout(0, 16));

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);

            JPanel destGroup = new JPanel();
            destGroup.setLayout(new BoxLayout(destGroup, BoxLayout.Y_AXIS));
            destGroup.setOpaque(false);

            JLabel dest = new JLabel(s.destination);
            dest.setFont(new Font("Segoe UI", Font.BOLD, 22));
            dest.setForeground(TEXT_PRIMARY);

            JLabel timeLabel = new JLabel("◆ " + s.departureTime);
            timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            timeLabel.setForeground(GOLD_PRIMARY);

            destGroup.add(dest);
            destGroup.add(Box.createRigidArea(new Dimension(0, 4)));
            destGroup.add(timeLabel);

            JLabel fare = new JLabel("₱" + String.format("%,.2f", s.fare));
            fare.setFont(new Font("Segoe UI", Font.BOLD, 24));
            fare.setForeground(GOLD_LIGHT);

            headerRow.add(destGroup, BorderLayout.WEST);
            headerRow.add(fare, BorderLayout.EAST);

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);

            addPremiumDetail(body, "Vehicle", s.van.model);
            addPremiumDetail(body, "Plate", s.van.plateNumber);
            addPremiumDetail(body, "Driver", s.van.driver.name);
            addPremiumDetail(body, "Available", availableSeats(s) + " seats");

            JButton btnSelect = createPremiumButton("Select This Journey  →", GOLD_PRIMARY, false, 14);
            btnSelect.setPreferredSize(new Dimension(0, 48));
            btnSelect.addActionListener(e -> {
                selectedSchedule = s;
                renderSeatGrid();
                cardLayout.show(mainContainer, "SEATS");
            });

            card.add(headerRow, BorderLayout.NORTH);
            card.add(body, BorderLayout.CENTER);
            card.add(btnSelect, BorderLayout.SOUTH);
            scheduleGridContainer.add(card);
        }

        scheduleGridContainer.revalidate();
        scheduleGridContainer.repaint();
    }

    private int availableSeats(Schedule s) {
        return s.van.capacity - s.bookedSeats.size();
    }

    private void addPremiumDetail(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel key = new JLabel(label);
        key.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        key.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 13));
        val.setForeground(TEXT_SECONDARY);

        row.add(key, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    // =========================================================================
    // SCREEN 2: SEAT SELECTION
    // =========================================================================
    private JPanel buildSeatSelectionView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DEEP);
        root.add(createPremiumHeader("Step 2 of 3", "Choose Your Seats", false, true), BorderLayout.NORTH);

        JPanel mainBody = new JPanel(new BorderLayout(0, 18));
        mainBody.setBorder(new EmptyBorder(20, 32, 20, 32));
        mainBody.setOpaque(false);

        JPanel layoutWrapper = createPremiumCard();
        layoutWrapper.setLayout(new BorderLayout(0, 18));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 0));
        legend.setOpaque(false);
        legend.add(createLegend("Available", SEMANTIC_SUCCESS));
        legend.add(createLegend("Selected", SEMANTIC_INFO));
        legend.add(createLegend("Occupied", SEMANTIC_DANGER));
        legend.add(createLegend("Jump Seat", SEMANTIC_WARNING));

        layoutWrapper.add(legend, BorderLayout.NORTH);

        seatGridContainer = new JPanel(new GridBagLayout());
        seatGridContainer.setOpaque(false);

        JScrollPane seatScroll = new JScrollPane(seatGridContainer);
        seatScroll.setBorder(null);
        seatScroll.setOpaque(false);
        seatScroll.getViewport().setOpaque(false);
        layoutWrapper.add(seatScroll, BorderLayout.CENTER);

        JPanel bottom = createPremiumCard();
        bottom.setLayout(new BorderLayout());

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        lblSelectedSeatsText = new JLabel("No seats selected");
        lblSelectedSeatsText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSelectedSeatsText.setForeground(TEXT_MUTED);

        lblTotalFareText = new JLabel("₱0.00");
        lblTotalFareText.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTotalFareText.setForeground(GOLD_LIGHT);

        info.add(lblSelectedSeatsText);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(lblTotalFareText);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton btnBack = createPremiumButton("← Change Journey", GOLD_PRIMARY, true, 14);
        btnBack.setPreferredSize(new Dimension(170, 50));
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "SCHEDULE"));

        JButton btnProceed = createPremiumButton("Continue  →", GOLD_PRIMARY, false, 14);
        btnProceed.setPreferredSize(new Dimension(180, 50));
        btnProceed.addActionListener(e -> {
            if (currentSelectedSeats.isEmpty()) {
                showPremiumDialog("Please select at least one seat to continue.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            populateSummaryDetails();
            cardLayout.show(mainContainer, "SUMMARY");
        });

        actions.add(btnBack);
        actions.add(btnProceed);

        bottom.add(info, BorderLayout.WEST);
        bottom.add(actions, BorderLayout.EAST);

        mainBody.add(layoutWrapper, BorderLayout.CENTER);
        mainBody.add(bottom, BorderLayout.SOUTH);
        root.add(mainBody, BorderLayout.CENTER);
        return root;
    }

    private JPanel createLegend(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        dot.setOpaque(false);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_SECONDARY);

        p.add(dot);
        p.add(lbl);
        return p;
    }

    private void renderSeatGrid() {
        seatGridContainer.removeAll();
        currentSelectedSeats.clear();
        updateSeatSummary();

        String model = selectedSchedule.van.model;
        if (model.contains("Deluxe")) renderDeluxeLayout();
        else if (model.contains("NV350")) renderNV350Layout();
        else if (model.contains("Cargo")) renderCargoLayout();
        else renderCommuterLayout();

        seatGridContainer.revalidate();
        seatGridContainer.repaint();
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.BOTH;
        return gbc;
    }

    private void addDriver(GridBagConstraints gbc, int col, int row) {
        JLabel lbl = new JLabel("DRIVER", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_GLASS);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(GOLD_DIM);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setForeground(GOLD_PRIMARY);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setPreferredSize(new Dimension(110, 60));
        gbc.gridx = col; gbc.gridy = row;
        seatGridContainer.add(lbl, gbc);
    }

    private void addSeat(GridBagConstraints gbc, int col, int row, int seatNum, boolean jump) {
        if (seatNum > selectedSchedule.van.capacity) return;

        boolean occupied = selectedSchedule.bookedSeats.contains(seatNum);
        Color baseColor = occupied ? SEMANTIC_DANGER : (jump ? SEMANTIC_WARNING : SEMANTIC_SUCCESS);
        String label = jump ? "J" + seatNum : String.valueOf(seatNum);

        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                Color currentBase = getBackground();
                Color top = lighten(currentBase, 0.2f);
                Color bottom = darken(currentBase, 0.15f);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));

                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h / 2, 12, 12));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setText(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(BG_DEEP);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(2, 2, 2, 2));
        btn.setPreferredSize(new Dimension(110, 60));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (occupied) {
            btn.setEnabled(false);
            btn.setBackground(SEMANTIC_DANGER);
            btn.setForeground(new Color(255, 255, 255, 180));
        } else {
            btn.addActionListener(e -> {
                if (currentSelectedSeats.contains(seatNum)) {
                    currentSelectedSeats.remove(Integer.valueOf(seatNum));
                    btn.setBackground(baseColor);
                    btn.setForeground(BG_DEEP);
                } else {
                    currentSelectedSeats.add(seatNum);
                    btn.setBackground(SEMANTIC_INFO);
                    btn.setForeground(Color.WHITE);
                }
                updateSeatSummary();
                btn.repaint();
            });
        }

        gbc.gridx = col;
        gbc.gridy = row;
        seatGridContainer.add(btn, gbc);
    }

    private void renderDeluxeLayout() {
        GridBagConstraints g = baseGbc();
        addDriver(g, 0, 0); addSeat(g, 1, 0, 1, false); addSeat(g, 2, 0, 2, false);
        addSeat(g, 0, 1, 3, false); addSeat(g, 1, 1, 4, false); addSeat(g, 2, 1, 5, false);
        addSeat(g, 0, 2, 6, false); addSeat(g, 1, 2, 7, false); addSeat(g, 2, 2, 8, true);
        addSeat(g, 0, 3, 9, false); addSeat(g, 1, 3, 10, false); addSeat(g, 2, 3, 11, true);
        addSeat(g, 0, 4, 12, false); addSeat(g, 1, 4, 13, false);
    }

    private void renderCommuterLayout() {
        GridBagConstraints g = baseGbc();
        addDriver(g, 0, 0); addSeat(g, 1, 0, 1, false); addSeat(g, 2, 0, 2, false);
        addSeat(g, 0, 1, 3, false); addSeat(g, 1, 1, 4, false); addSeat(g, 2, 1, 5, false);
        addSeat(g, 0, 2, 6, false); addSeat(g, 1, 2, 7, false); addSeat(g, 2, 2, 8, true);
        addSeat(g, 0, 3, 9, false); addSeat(g, 1, 3, 10, false); addSeat(g, 2, 3, 11, true);
        addSeat(g, 0, 4, 12, false); addSeat(g, 1, 4, 13, false); addSeat(g, 2, 4, 14, false);
    }

    private void renderNV350Layout() {
        GridBagConstraints g = baseGbc();
        addDriver(g, 0, 0); addSeat(g, 1, 0, 1, false); addSeat(g, 2, 0, 2, false);
        addSeat(g, 0, 1, 3, false); addSeat(g, 1, 1, 4, false); addSeat(g, 2, 1, 5, false);
        addSeat(g, 0, 2, 6, false); addSeat(g, 1, 2, 7, false); addSeat(g, 2, 2, 8, true);
        addSeat(g, 0, 3, 9, false); addSeat(g, 1, 3, 10, false); addSeat(g, 2, 3, 11, false); addSeat(g, 3, 3, 12, false);
    }

    private void renderCargoLayout() {
        GridBagConstraints g = baseGbc();
        addDriver(g, 0, 0); addSeat(g, 1, 0, 1, false); addSeat(g, 2, 0, 2, false);
        addSeat(g, 0, 1, 3, false); addSeat(g, 1, 1, 4, false); addSeat(g, 2, 1, 5, false);

        JLabel cargo = new JLabel("CARGO HOLD", SwingConstants.CENTER);
        cargo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cargo.setForeground(TEXT_MUTED);
        cargo.setOpaque(true);
        cargo.setBackground(BG_GLASS);
        cargo.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        cargo.setPreferredSize(new Dimension(340, 120));

        g.gridx = 0; g.gridy = 2; g.gridwidth = 3; g.gridheight = 2;
        seatGridContainer.add(cargo, g);
    }

    private void updateSeatSummary() {
        if (currentSelectedSeats.isEmpty()) {
            lblSelectedSeatsText.setText("No seats selected");
            lblTotalFareText.setText("₱0.00");
        } else {
            Collections.sort(currentSelectedSeats);
            lblSelectedSeatsText.setText("Seats: " + currentSelectedSeats.toString());
            double total = currentSelectedSeats.size() * selectedSchedule.fare;
            lblTotalFareText.setText("₱" + String.format("%,.2f", total));
        }
    }

    // =========================================================================
    // SCREEN 3: SUMMARY
    // =========================================================================
    private JPanel buildSummaryView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DEEP);
        root.add(createPremiumHeader("Step 3 of 3", "Confirm & Checkout", false, false), BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 22, 0));
        content.setBorder(new EmptyBorder(24, 32, 24, 32));
        content.setOpaque(false);

        JPanel summaryCard = createPremiumCard();
        summaryCard.setLayout(new BorderLayout(0, 16));

        JLabel lblTitle = new JLabel("Journey Summary");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(GOLD_PRIMARY);

        JPanel summaryBody = new JPanel();
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));
        summaryBody.setOpaque(false);

        lblSumRef = addPremiumRow(summaryBody, "Booking Reference", "—");
        lblSumDest = addPremiumRow(summaryBody, "Destination", "—");
        lblSumTime = addPremiumRow(summaryBody, "Departure", "—");
        lblSumVan = addPremiumRow(summaryBody, "Vehicle", "—");
        lblSumDriver = addPremiumRow(summaryBody, "Driver", "—");
        lblSumSeats = addPremiumRow(summaryBody, "Seats", "—");

        summaryBody.add(Box.createRigidArea(new Dimension(0, 14)));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_GOLD);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        summaryBody.add(sep);
        summaryBody.add(Box.createRigidArea(new Dimension(0, 14)));

        lblSumTotal = addPremiumRow(summaryBody, "Total Due", "₱0.00");

        summaryCard.add(lblTitle, BorderLayout.NORTH);
        summaryCard.add(summaryBody, BorderLayout.CENTER);

        JPanel formCard = createPremiumCard();
        formCard.setLayout(new BorderLayout(0, 16));

        JLabel lblFormTitle = new JLabel("Passenger Details");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFormTitle.setForeground(GOLD_PRIMARY);

        JPanel formFields = new JPanel();
        formFields.setLayout(new BoxLayout(formFields, BoxLayout.Y_AXIS));
        formFields.setOpaque(false);

        JLabel lbl1 = new JLabel("FULL NAME");
        lbl1.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl1.setForeground(TEXT_MUTED);

        txtPassengerName = createPremiumTextField();
        txtPassengerName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel lbl2 = new JLabel("MOBILE NUMBER");
        lbl2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl2.setForeground(TEXT_MUTED);

        txtPassengerContact = createPremiumTextField();
        txtPassengerContact.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel notice = new JLabel("<html><div style='width:300px; color:#B4BED2;'>" +
            "Present your booking reference at the counter to complete payment and claim your ticket.</div></html>");
        notice.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        formFields.add(lbl1);
        formFields.add(Box.createRigidArea(new Dimension(0, 6)));
        formFields.add(txtPassengerName);
        formFields.add(Box.createRigidArea(new Dimension(0, 18)));
        formFields.add(lbl2);
        formFields.add(Box.createRigidArea(new Dimension(0, 6)));
        formFields.add(txtPassengerContact);
        formFields.add(Box.createRigidArea(new Dimension(0, 22)));
        formFields.add(notice);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton btnBack = createPremiumButton("← Change Seats", GOLD_PRIMARY, true, 14);
        btnBack.setPreferredSize(new Dimension(160, 50));
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "SEATS"));

        JButton btnSubmit = createPremiumButton("Confirm Booking  →", GOLD_PRIMARY, false, 14);
        btnSubmit.setPreferredSize(new Dimension(200, 50));
        btnSubmit.addActionListener(e -> handleBooking());

        actions.add(btnBack);
        actions.add(btnSubmit);

        formCard.add(lblFormTitle, BorderLayout.NORTH);
        formCard.add(formFields, BorderLayout.CENTER);
        formCard.add(actions, BorderLayout.SOUTH);

        content.add(summaryCard);
        content.add(formCard);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private JLabel addPremiumRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel k = new JLabel(label);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        k.setForeground(TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 14));
        v.setForeground(TEXT_SECONDARY);

        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 6)));
        return v;
    }

    private String generatedRef = "";

    private void populateSummaryDetails() {
        generatedRef = "VG-" + (100000 + new Random().nextInt(900000));
        lblSumRef.setText(generatedRef);
        lblSumDest.setText(selectedSchedule.destination);
        lblSumTime.setText(selectedSchedule.departureTime);
        lblSumVan.setText(selectedSchedule.van.model);
        lblSumDriver.setText(selectedSchedule.van.driver.name);
        lblSumSeats.setText(currentSelectedSeats.toString());
        double total = currentSelectedSeats.size() * selectedSchedule.fare;
        lblSumTotal.setText("₱" + String.format("%,.2f", total));
        lblSumTotal.setForeground(GOLD_LIGHT);
    }

    private void handleBooking() {
        String name = txtPassengerName.getText().trim();
        String phone = txtPassengerContact.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            showPremiumDialog("Please enter your full name and mobile number.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int seat : currentSelectedSeats) {
            if (selectedSchedule.bookedSeats.contains(seat)) {
                showPremiumDialog("Seat " + seat + " is no longer available.",
                    "Seat Conflict", JOptionPane.ERROR_MESSAGE);
                cardLayout.show(mainContainer, "SEATS");
                return;
            }
        }

        selectedSchedule.bookedSeats.addAll(currentSelectedSeats);
        double total = currentSelectedSeats.size() * selectedSchedule.fare;
        Booking b = new Booking(generatedRef, selectedSchedule,
            new ArrayList<>(currentSelectedSeats), name, phone, total);
        bookingDatabase.put(generatedRef, b);

        showPremiumDialog(
            "━━━ BOOKING CONFIRMED ━━━\n\n" +
            "Reference:  " + generatedRef + "\n" +
            "Passenger:  " + name + "\n" +
            "Total Due:  ₱" + String.format("%,.2f", total) + "\n\n" +
            "Please proceed to the counter to complete payment.",
            "Reservation Successful", JOptionPane.INFORMATION_MESSAGE);

        txtPassengerName.setText("");
        txtPassengerContact.setText("");
        selectedSchedule = null;
        currentSelectedSeats.clear();
        cardLayout.show(mainContainer, "WELCOME");
    }

    private void showPremiumDialog(String message, String title, int type) {
        UIManager.put("OptionPane.background", BG_SURFACE);
        UIManager.put("Panel.background", BG_SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    // =========================================================================
    // ADMIN VIEW
    // =========================================================================
    private JPanel buildAdminView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DEEP);
        root.add(createPremiumHeader("Staff Portal", "Terminal Operations Center", false, false), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(BG_SURFACE);
        tabs.setForeground(TEXT_PRIMARY);

        tabs.addTab("Payment Counter", buildCounterTab());
        tabs.addTab("Schedules", buildSchedulesTab());
        tabs.addTab("Fleet", buildVansTab());
        tabs.addTab("Drivers", buildDriversTab());

        JPanel body = createPremiumCard();
        body.setLayout(new BorderLayout(0, 14));
        body.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton btnExit = createPremiumButton("← Return to Kiosk", GOLD_PRIMARY, true, 13);
        btnExit.setPreferredSize(new Dimension(190, 42));
        btnExit.addActionListener(e -> {
            renderScheduleCards();
            cardLayout.show(mainContainer, "WELCOME");
        });
        footer.add(btnExit);

        body.add(footer, BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildCounterTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setOpaque(false);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        searchBar.setOpaque(false);

        JLabel lbl = new JLabel("Reference:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_SECONDARY);

        JTextField txt = createPremiumTextField();
        txt.setPreferredSize(new Dimension(220, 40));

        JButton btnSearch = createPremiumButton("Look Up", SEMANTIC_INFO, false, 13);
        btnSearch.setPreferredSize(new Dimension(120, 40));

        searchBar.add(lbl);
        searchBar.add(txt);
        searchBar.add(btnSearch);

        JTextArea receipt = new JTextArea();
        receipt.setEditable(false);
        receipt.setBackground(BG_DEEP);
        receipt.setForeground(TEXT_PRIMARY);
        receipt.setFont(new Font("Consolas", Font.PLAIN, 14));
        receipt.setBorder(new EmptyBorder(16, 16, 16, 16));

        JScrollPane scroll = new JScrollPane(receipt);
        scroll.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton btnPay = createPremiumButton("Confirm Payment & Issue Ticket", SEMANTIC_SUCCESS, false, 14);
        btnPay.setPreferredSize(new Dimension(300, 46));
        btnPay.setEnabled(false);
        bottom.add(btnPay);

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> {
            String ref = txt.getText().trim();
            if (bookingDatabase.containsKey(ref)) {
                Booking b = bookingDatabase.get(ref);
                receipt.setText(
                    "════════════════════════════════════════\n" +
                    "         VANGO PREMIUM TERMINAL          \n" +
                    "════════════════════════════════════════\n" +
                    " Reference : " + b.referenceNo + "\n" +
                    " Status    : " + b.status + "\n" +
                    " Passenger : " + b.passengerName + "\n" +
                    " Contact   : " + b.passengerContact + "\n" +
                    "────────────────────────────────────────\n" +
                    " Destination : " + b.schedule.destination + "\n" +
                    " Departure   : " + b.schedule.departureTime + "\n" +
                    " Vehicle     : " + b.schedule.van.model + " [" + b.schedule.van.plateNumber + "]\n" +
                    " Driver      : " + b.schedule.van.driver.name + "\n" +
                    " Seats       : " + b.selectedSeats.toString() + "\n" +
                    " Total       : ₱" + String.format("%,.2f", b.totalAmount) + "\n" +
                    "════════════════════════════════════════\n"
                );
                btnPay.setEnabled(b.status.equals("PENDING PAYMENT"));
            } else {
                receipt.setText("No booking found for reference: " + ref);
                btnPay.setEnabled(false);
            }
        });

        btnPay.addActionListener(e -> {
            String ref = txt.getText().trim();
            if (bookingDatabase.containsKey(ref)) {
                bookingDatabase.get(ref).status = "CONFIRMED & PAID";
                showPremiumDialog("Payment approved. Ticket confirmed!",
                    "Transaction Approved", JOptionPane.INFORMATION_MESSAGE);
                btnSearch.doClick();
            }
        });

        return panel;
    }

    private JPanel buildSchedulesTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setOpaque(false);

        String[] cols = {"ID", "Destination", "Departure", "Fare", "Van", "Driver"};
        modelSchedules = new DefaultTableModel(cols, 0);
        JTable table = createPremiumTable(modelSchedules);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));

        JPanel form = new JPanel(new GridLayout(10, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        JTextField txtDest = createPremiumTextField();
        JTextField txtTime = createPremiumTextField();
        JTextField txtFare = createPremiumTextField();
        cbScheduleVan = new JComboBox<>();

        form.add(formLabel("Destination"));
        form.add(txtDest);
        form.add(formLabel("Departure Time"));
        form.add(txtTime);
        form.add(formLabel("Fare (₱)"));
        form.add(txtFare);
        form.add(formLabel("Assigned Van"));
        form.add(cbScheduleVan);

        JButton btnAdd = createPremiumButton("+ Add Schedule", SEMANTIC_INFO, false, 12);
        btnAdd.addActionListener(e -> {
            try {
                String dest = txtDest.getText().trim();
                String time = txtTime.getText().trim();
                double fare = Double.parseDouble(txtFare.getText().trim());
                Van v = (Van) cbScheduleVan.getSelectedItem();
                if (dest.isEmpty() || time.isEmpty() || v == null) {
                    showPremiumDialog("Complete all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = "SCH00" + (schedules.size() + 1);
                schedules.add(new Schedule(id, dest, time, fare, v));
                txtDest.setText(""); txtTime.setText(""); txtFare.setText("");
                refreshAdminTables();
            } catch (Exception ex) {
                showPremiumDialog("Invalid fare amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnDel = createPremiumButton("Delete Selected", SEMANTIC_DANGER, false, 12);
        btnDel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                schedules.remove(row);
                refreshAdminTables();
            } else {
                showPremiumDialog("Select a schedule to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        form.add(btnAdd);
        form.add(btnDel);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildVansTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setOpaque(false);

        String[] cols = {"ID", "Model", "Plate", "Capacity", "Driver"};
        modelVans = new DefaultTableModel(cols, 0);
        JTable table = createPremiumTable(modelVans);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));

        JPanel form = new JPanel(new GridLayout(9, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        JTextField txtModel = createPremiumTextField();
        JTextField txtPlate = createPremiumTextField();
        JTextField txtCap = createPremiumTextField();
        txtCap.setText("13");
        cbVanDriver = new JComboBox<>();

        form.add(formLabel("Van Model"));
        form.add(txtModel);
        form.add(formLabel("Plate Number"));
        form.add(txtPlate);
        form.add(formLabel("Seating Capacity"));
        form.add(txtCap);
        form.add(formLabel("Assigned Driver"));
        form.add(cbVanDriver);

        JButton btnAdd = createPremiumButton("+ Add Van", SEMANTIC_INFO, false, 12);
        btnAdd.addActionListener(e -> {
            try {
                String model = txtModel.getText().trim();
                String plate = txtPlate.getText().trim();
                int cap = Integer.parseInt(txtCap.getText().trim());
                Driver d = (Driver) cbVanDriver.getSelectedItem();
                if (model.isEmpty() || plate.isEmpty() || d == null) {
                    showPremiumDialog("Complete all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String id = "VAN0" + (vans.size() + 1);
                vans.add(new Van(id, model, plate, cap, d));
                txtModel.setText(""); txtPlate.setText("");
                refreshAdminTables();
            } catch (Exception ex) {
                showPremiumDialog("Invalid capacity.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.add(btnAdd);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildDriversTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setOpaque(false);

        String[] cols = {"ID", "Name", "License", "Phone"};
        modelDrivers = new DefaultTableModel(cols, 0);
        JTable table = createPremiumTable(modelDrivers);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));

        JPanel form = new JPanel(new GridLayout(7, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        JTextField txtName = createPremiumTextField();
        JTextField txtLic = createPremiumTextField();
        JTextField txtPhone = createPremiumTextField();

        form.add(formLabel("Full Name"));
        form.add(txtName);
        form.add(formLabel("License Number"));
        form.add(txtLic);
        form.add(formLabel("Contact Phone"));
        form.add(txtPhone);

        JButton btnAdd = createPremiumButton("+ Register Driver", SEMANTIC_INFO, false, 12);
        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            String lic = txtLic.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty() || lic.isEmpty() || phone.isEmpty()) {
                showPremiumDialog("Complete all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = "DRV0" + (drivers.size() + 1);
            drivers.add(new Driver(id, name, lic, phone));
            txtName.setText(""); txtLic.setText(""); txtPhone.setText("");
            refreshAdminTables();
        });

        form.add(btnAdd);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JTable createPremiumTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(BG_SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_SUBTLE);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(SEMANTIC_INFO);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(BG_GLASS);
        table.getTableHeader().setForeground(GOLD_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(JLabel.LEFT);
        r.setBackground(BG_SURFACE);
        r.setForeground(TEXT_PRIMARY);
        table.setDefaultRenderer(Object.class, r);
        return table;
    }

    private void refreshAdminTables() {
        modelSchedules.setRowCount(0);
        for (Schedule s : schedules) {
            modelSchedules.addRow(new Object[]{
                s.id, s.destination, s.departureTime,
                String.format("%,.2f", s.fare),
                s.van.model + " [" + s.van.plateNumber + "]",
                s.van.driver.name
            });
        }

        modelVans.setRowCount(0);
        cbScheduleVan.removeAllItems();
        for (Van v : vans) {
            modelVans.addRow(new Object[]{
                v.id, v.model, v.plateNumber, v.capacity, v.driver.name
            });
            cbScheduleVan.addItem(v);
        }

        modelDrivers.setRowCount(0);
        cbVanDriver.removeAllItems();
        for (Driver d : drivers) {
            modelDrivers.addRow(new Object[]{
                d.id, d.name, d.licenseNo, d.phone
            });
            cbVanDriver.addItem(d);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new VanGoKiosk().setVisible(true);
        });
    }
}