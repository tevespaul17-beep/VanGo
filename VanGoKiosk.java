import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

public class VanGoKiosk extends JFrame {

    private static final Color BRAND_NAVY      = VanGoTheme.NAVY;
    private static final Color BRAND_ORANGE    = VanGoTheme.ORANGE;
    private static final Color BG_CANVAS       = VanGoTheme.BG;
    private static final Color CARD_SURFACE    = VanGoTheme.SURFACE;
    private static final Color BORDER_NEUTRAL  = VanGoTheme.BORDER;
    private static final Color TEXT_PRIMARY    = VanGoTheme.TEXT;
    private static final Color TEXT_MUTED      = VanGoTheme.TEXT_SECOND;
    private static final Color SEMANTIC_GO     = VanGoTheme.GO;
    private static final Color SEMANTIC_RED    = VanGoTheme.DANGER;
    private static final Color SEMANTIC_AMBER  = VanGoTheme.WARN;

    public static final String MODEL_HIACE_DELUXE   = "TOYOTA COMMUTER DELUXE";
    public static final String MODEL_HIACE_COMMUTER = "TOYOTA HIACE COMMUTER";
    public static final String MODEL_NISSAN_NV350   = "NISSAN NV350";
    public static final String MODEL_NISSAN_URVAN   = "NISSAN URVAN";

    private List<Driver> drivers;
    private List<Van> vans;
    private List<Schedule> schedules;
    private Schedule selectedSchedule;
    private final List<Integer> currentSelectedSeats = new ArrayList<>();
    private final Map<String, Booking> bookingDatabase = new HashMap<>();

    private final DriverQueue driverQueue = new DriverQueue();

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JPanel scheduleGridContainer;
    private VanBackgroundPanel vanBackgroundCanvas;
    private JLabel lblSelectedSeatsText, lblTotalFareText;

    private JLabel lblSumRef, lblSumDest, lblSumTime, lblSumVan, lblSumDriver, lblSumSeats, lblSumTotal;
    private JTextField txtPassengerName, txtPassengerContact;

    private DefaultTableModel modelSchedules, modelVans, modelDrivers, modelQueue, modelDispatch;
    private JComboBox<Van> cbScheduleVan;
    private JComboBox<Driver> cbVanDriver;
    private JComboBox<String> cbVanModelSelection;

    private JComboBox<Driver> cbActiveDriverLogin;
    private JLabel lblDriverVanName, lblDriverPlate, lblDriverStatus;
    private JPanel driverTripsContainer;
    private Driver loggedInDriver;

    // ---- Admin dashboard redesign state ----
    private JPanel segmentedTabBar;
    private CardLayout tabContentLayout;
    private JPanel tabContentPanel;
    private final Map<String, ClarityButton> tabButtons = new LinkedHashMap<>();
    private JLabel kpiValueReady, kpiValuePending, kpiValueBoarding, kpiValueUtil, kpiValueTrips;

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

        Van v1 = new Van("VAN01", MODEL_HIACE_COMMUTER, "HA-9821", 14, d1, "");
        Van v2 = new Van("VAN02", MODEL_HIACE_DELUXE,   "HD-5512", 14, d2, "");
        Van v3 = new Van("VAN03", MODEL_NISSAN_NV350,   "NV-4410", 14, d3, "");
        Van v4 = new Van("VAN04", MODEL_NISSAN_URVAN,   "NC-1092", 15, d4, "");
        vans.add(v1); vans.add(v2); vans.add(v3); vans.add(v4);

        // Updated schedules: Cebu City removed, replaced with Baybay City
        Schedule s1 = new Schedule("SCH001", "Tacloban City", "06:00 AM", 250.00, v1);
        Schedule s2 = new Schedule("SCH002", "Maasin City", "08:30 AM", 300.00, v2);
        Schedule s3 = new Schedule("SCH003", "Baybay City", "10:00 AM", 450.00, v3);
        Schedule s4 = new Schedule("SCH004", "Ormoc City", "01:00 PM", 200.00, v4);
        Schedule s5 = new Schedule("SCH005", "Tacloban City", "03:30 PM", 250.00, v1);
        Schedule s6 = new Schedule("SCH006", "Naval", "04:00 PM", 350.00, v2); // Added another Region 8 hub

        schedules.add(s1); schedules.add(s2); schedules.add(s3); schedules.add(s4); schedules.add(s5); schedules.add(s6);

        for (Schedule s : schedules) {
            driverQueue.enqueueIfAbsent(s.getDestination(), s.getVan());
        }

        for (int i = 1; i <= 14; i++) s1.bookSeat(i);
        s2.bookSeat(4); s2.bookSeat(8);
        s3.bookSeat(3); s3.bookSeat(7); s3.bookSeat(12);
    }

    private void initUI() {
        setTitle("VANGO TERMINAL WAYFINDING KIOSK");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 900);
        setMinimumSize(new Dimension(1024, 768));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_CANVAS);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setBackground(BG_CANVAS);

        mainContainer.add(buildWelcomeView(), "WELCOME");
        mainContainer.add(buildScheduleView(), "SCHEDULE");
        mainContainer.add(buildSeatSelectionView(), "SEATS");
        mainContainer.add(buildSummaryView(), "SUMMARY");
        mainContainer.add(buildStaffGatewayView(), "STAFF_GATEWAY");
        mainContainer.add(buildAdminDashboardView(), "ADMIN_DASHBOARD");
        mainContainer.add(buildDriverDashboardView(), "DRIVER_DASHBOARD");

        add(mainContainer);
        cardLayout.show(mainContainer, "WELCOME");
    }

    // --- UTILITIES ---
    private JPanel createStatusHeader(String stepText, String titleText, boolean showStaffAccess, boolean showHomeButton) {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(CARD_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_NEUTRAL),
                new EmptyBorder(14, 28, 14, 28)
        ));

        VanGoLogoPanel headerLogo = new VanGoLogoPanel(true);
        headerLogo.setPreferredSize(new Dimension(150, 44));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel lblStep = new JLabel(stepText.toUpperCase());
        lblStep.setFont(VanGoTheme.font(11, Font.BOLD));
        lblStep.setForeground(BRAND_ORANGE);

        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(VanGoTheme.h2());
        lblTitle.setForeground(BRAND_NAVY);

        textGroup.add(lblStep);
        textGroup.add(Box.createRigidArea(new Dimension(0, 2)));
        textGroup.add(lblTitle);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(headerLogo);

        JPanel divider = new JPanel();
        divider.setBackground(BORDER_NEUTRAL);
        divider.setPreferredSize(new Dimension(1, 40));
        leftGroup.add(divider);
        leftGroup.add(textGroup);

        header.add(leftGroup, BorderLayout.WEST);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navBtns.setOpaque(false);

        if (showHomeButton) {
            ClarityButton btnHome = new ClarityButton("← Start Over", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
            btnHome.setPreferredSize(new Dimension(140, 40));
            btnHome.addActionListener(e -> cardLayout.show(mainContainer, "WELCOME"));
            navBtns.add(btnHome);
        }

        if (showStaffAccess) {
            ClarityButton btnStaff = new ClarityButton("Staff Portal", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 13);
            btnStaff.setPreferredSize(new Dimension(130, 40));
            btnStaff.addActionListener(e -> cardLayout.show(mainContainer, "STAFF_GATEWAY"));
            navBtns.add(btnStaff);
        }

        header.add(navBtns, BorderLayout.EAST);
        return header;
    }

    private JPanel createClarityCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_NEUTRAL, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        return card;
    }

    private JTextField createClarityTextField() {
        JTextField field = new JTextField();
        field.setBackground(CARD_SURFACE);
        field.setForeground(TEXT_PRIMARY);
        field.setFont(VanGoTheme.body());
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_NEUTRAL, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    // --- SCREEN 0: WELCOME ---
    private JPanel buildWelcomeView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(25, 40, 0, 40));

        JLabel brand = new JLabel("VANGO EXPRESS TERMINAL KIOSK SYSTEM");
        brand.setFont(VanGoTheme.font(12, Font.BOLD));
        brand.setForeground(TEXT_MUTED);

        ClarityButton btnStaff = new ClarityButton("Staff Portal Access", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
        btnStaff.setPreferredSize(new Dimension(170, 42));
        btnStaff.addActionListener(e -> cardLayout.show(mainContainer, "STAFF_GATEWAY"));

        topBar.add(brand, BorderLayout.WEST);
        topBar.add(btnStaff, BorderLayout.EAST);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);

        VanGoLogoPanel heroLogo = new VanGoLogoPanel(true);
        heroLogo.setPreferredSize(new Dimension(400, 120));
        heroLogo.setMaximumSize(new Dimension(400, 120));
        heroLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Self-Service Ticket Kiosk");
        lblTitle.setFont(VanGoTheme.font(40, Font.BOLD));
        lblTitle.setForeground(BRAND_NAVY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(
            "<html><div style='text-align:center; width:520px;'>Pick a destination, choose your seats, and get your booking reference — all in under a minute.</div></html>");
        lblSubtitle.setFont(VanGoTheme.font(16, Font.PLAIN));
        lblSubtitle.setForeground(TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        ClarityButton btnStart = new ClarityButton("START BOOKING  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 18);
        btnStart.setPreferredSize(new Dimension(360, 62));
        btnStart.setMaximumSize(new Dimension(360, 62));
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStart.addActionListener(e -> {
            renderScheduleCards();
            cardLayout.show(mainContainer, "SCHEDULE");
        });

        contentBox.add(heroLogo);
        contentBox.add(Box.createRigidArea(new Dimension(0, 18)));
        contentBox.add(lblTitle);
        contentBox.add(Box.createRigidArea(new Dimension(0, 12)));
        contentBox.add(lblSubtitle);
        contentBox.add(Box.createRigidArea(new Dimension(0, 42)));
        contentBox.add(btnStart);

        centerWrapper.add(contentBox);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 0, 30, 0));

        JLabel lblStatus = new JLabel("● TERMINAL KIOSK 01 ONLINE  •  FCFS DISPATCH QUEUE ACTIVE");
        lblStatus.setFont(VanGoTheme.font(11, Font.BOLD));
        lblStatus.setForeground(SEMANTIC_GO);
        footer.add(lblStatus);

        root.add(topBar, BorderLayout.NORTH);
        root.add(centerWrapper, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    // --- SCREEN 1: SCHEDULES ---
    private JPanel buildScheduleView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Step 1 of 3", "Select Destination & Departure Time", true, true), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainContent.setOpaque(false);

        scheduleGridContainer = new JPanel(new GridLayout(0, 2, 20, 20));
        scheduleGridContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(scheduleGridContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CANVAS);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        mainContent.add(scroll, BorderLayout.CENTER);
        root.add(mainContent, BorderLayout.CENTER);

        return root;
    }

    private void renderScheduleCards() {
        scheduleGridContainer.removeAll();

        for (Schedule s : schedules) {
            JPanel card = new JPanel(new BorderLayout(0, 14));
            card.setBackground(CARD_SURFACE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_NEUTRAL, 1, true),
                    new EmptyBorder(20, 20, 18, 20)
            ));

            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);

            JLabel lblDest = new JLabel(s.getDestination());
            lblDest.setFont(VanGoTheme.h2());
            lblDest.setForeground(BRAND_NAVY);

            JLabel lblFare = new JLabel("₱" + String.format("%.2f", s.getFare()));
            lblFare.setFont(VanGoTheme.h2());
            lblFare.setForeground(BRAND_ORANGE);

            head.add(lblDest, BorderLayout.WEST);
            head.add(lblFare, BorderLayout.EAST);

            JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            chipRow.setOpaque(false);
            chipRow.add(buildStatusChip(s));

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            createClarityDetailRow(body, "Departure", s.getDepartureTime());
            createClarityDetailRow(body, "Vehicle", s.getVan().getModel());
            createClarityDetailRow(body, "Plate", s.getVan().getPlateNumber());
            createClarityDetailRow(body, "Driver", s.getVan().getDriver().getName());
            createClarityDetailRow(body, "Occupancy", s.getBookedCount() + " / " + s.getCapacity() + " seats");

            ClarityButton btn;
            if (s.isDispatched()) {
                btn = new ClarityButton("DEPARTED", VanGoTheme.SURFACE_ALT, VanGoTheme.TEXT_TERT, BORDER_NEUTRAL, 14);
                btn.setEnabled(false);
            } else if (s.isFull()) {
                btn = new ClarityButton("FULL — AWAITING DISPATCH", SEMANTIC_AMBER, Color.WHITE, SEMANTIC_AMBER, 13);
                btn.setEnabled(false);
            } else if (s.getDriverDepartureRequested()) {
                btn = new ClarityButton("PENDING EARLY DEPARTURE", SEMANTIC_AMBER, Color.WHITE, SEMANTIC_AMBER, 13);
                btn.setEnabled(false);
            } else {
                btn = new ClarityButton("Select This Trip  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 15);
                btn.addActionListener(e -> {
                    selectedSchedule = s;
                    renderSeatGrid();
                    cardLayout.show(mainContainer, "SEATS");
                });
            }
            btn.setPreferredSize(new Dimension(0, 46));

            card.add(head, BorderLayout.NORTH);
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            center.add(chipRow);
            center.add(Box.createRigidArea(new Dimension(0, 10)));
            center.add(body);
            card.add(center, BorderLayout.CENTER);
            card.add(btn, BorderLayout.SOUTH);

            scheduleGridContainer.add(card);
        }

        scheduleGridContainer.revalidate();
        scheduleGridContainer.repaint();
    }

    private JPanel buildStatusChip(Schedule s) {
        Color bg, fg;
        String text;
        if (s.isDispatched()) { bg = VanGoTheme.SURFACE_ALT; fg = VanGoTheme.TEXT_TERT; text = "● DEPARTED"; }
        else if (s.isFull()) { bg = VanGoTheme.WARN_BG; fg = VanGoTheme.WARN; text = "● FULL — READY FOR DISPATCH"; }
        else if (s.getDriverDepartureRequested()) { bg = VanGoTheme.WARN_BG; fg = VanGoTheme.WARN; text = "● AWAITING APPROVAL"; }
        else { bg = VanGoTheme.GO_BG; fg = VanGoTheme.GO; text = "● NOW BOARDING"; }

        JPanel chip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JLabel lbl = new JLabel(text);
        lbl.setFont(VanGoTheme.font(11, Font.BOLD));
        lbl.setForeground(fg);
        chip.add(lbl);
        return chip;
    }

    private JLabel createClarityDetailRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(VanGoTheme.font(12, Font.PLAIN));
        lblKey.setForeground(TEXT_MUTED);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(VanGoTheme.font(13, Font.BOLD));
        lblVal.setForeground(TEXT_PRIMARY);

        row.add(lblKey, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);

        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 6)));
        return lblVal;
    }

    // --- SCREEN 2: SEATS ---
    private JPanel buildSeatSelectionView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Step 2 of 3", "Interactive Seat Map", false, true), BorderLayout.NORTH);

        JPanel mainBody = new JPanel(new BorderLayout(0, 15));
        mainBody.setBorder(new EmptyBorder(15, 30, 15, 30));
        mainBody.setOpaque(false);

        JPanel layoutWrapper = createClarityCard();
        layoutWrapper.setLayout(new BorderLayout(0, 15));

        JPanel legendBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        legendBar.setOpaque(false);
        legendBar.add(createLegendChip("Available", VanGoTheme.SEAT_AVAILABLE, VanGoTheme.NAVY));
        legendBar.add(createLegendChip("Selected", VanGoTheme.ORANGE, Color.WHITE));
        legendBar.add(createLegendChip("Booked", VanGoTheme.DANGER, Color.WHITE));
        legendBar.add(createLegendChip("Jump Seat", VanGoTheme.SEAT_JUMP, VanGoTheme.NAVY));

        layoutWrapper.add(legendBar, BorderLayout.NORTH);

        vanBackgroundCanvas = new VanBackgroundPanel();

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(vanBackgroundCanvas);

        JScrollPane seatScroll = new JScrollPane(centerWrapper);
        seatScroll.setBorder(null);
        seatScroll.getViewport().setBackground(CARD_SURFACE);
        layoutWrapper.add(seatScroll, BorderLayout.CENTER);

        JPanel bottomBar = createClarityCard();
        bottomBar.setLayout(new BorderLayout());

        JPanel infoText = new JPanel();
        infoText.setLayout(new BoxLayout(infoText, BoxLayout.Y_AXIS));
        infoText.setOpaque(false);

        lblSelectedSeatsText = new JLabel("Selected Seats: None");
        lblSelectedSeatsText.setFont(VanGoTheme.font(13, Font.BOLD));
        lblSelectedSeatsText.setForeground(TEXT_MUTED);

        lblTotalFareText = new JLabel("Total Price: ₱0.00");
        lblTotalFareText.setFont(VanGoTheme.h2());
        lblTotalFareText.setForeground(BRAND_NAVY);

        infoText.add(lblSelectedSeatsText);
        infoText.add(Box.createRigidArea(new Dimension(0, 4)));
        infoText.add(lblTotalFareText);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionBtns.setOpaque(false);

        ClarityButton btnBack = new ClarityButton("← Change Trip", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 14);
        btnBack.setPreferredSize(new Dimension(140, 48));
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "SCHEDULE"));

        ClarityButton btnProceed = new ClarityButton("Proceed to Booking  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 14);
        btnProceed.setPreferredSize(new Dimension(200, 48));
        btnProceed.addActionListener(e -> {
            if (currentSelectedSeats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one seat to continue.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
                return;
            }
            populateSummaryDetails();
            cardLayout.show(mainContainer, "SUMMARY");
        });

        actionBtns.add(btnBack);
        actionBtns.add(btnProceed);

        bottomBar.add(infoText, BorderLayout.WEST);
        bottomBar.add(actionBtns, BorderLayout.EAST);

        mainBody.add(layoutWrapper, BorderLayout.CENTER);
        mainBody.add(bottomBar, BorderLayout.SOUTH);

        root.add(mainBody, BorderLayout.CENTER);
        return root;
    }

    private JPanel createLegendChip(String text, Color bg, Color fg) {
        JPanel chip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new FlowLayout(FlowLayout.CENTER, 14, 6));
        JLabel l = new JLabel(text);
        l.setFont(VanGoTheme.font(12, Font.BOLD));
        l.setForeground(fg);
        chip.add(l);
        return chip;
    }

    private void renderSeatGrid() {
        vanBackgroundCanvas.removeAll();
        currentSelectedSeats.clear();
        updateSeatSummaryDisplay();

        String modelName = selectedSchedule.getVan().getModel().toUpperCase();
        vanBackgroundCanvas.setVanModel(modelName);

        if (modelName.contains("DELUXE")) {
            renderHiAceDeluxeLayout();
        } else {
            renderStandardVanLayout(modelName);
        }

        vanBackgroundCanvas.revalidate();
        vanBackgroundCanvas.repaint();
    }

    private void addDriverLabel(double centerX, double centerY, double width, double height) {
        JLabel lblDriver = new JLabel("DRIVER", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BRAND_NAVY);
                g2.fillRoundRect(2, 6, getWidth() - 4, getHeight() - 8, 12, 12);
                g2.setColor(BRAND_ORANGE);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(2, 6, getWidth() - 4, getHeight() - 8, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblDriver.setOpaque(false);
        lblDriver.setForeground(new Color(254, 240, 138));
        lblDriver.setFont(VanGoTheme.font(9, Font.BOLD));
        setImageRelativeBounds(lblDriver, centerX, centerY, width, height);
        vanBackgroundCanvas.add(lblDriver);
    }

    private void addAnimatedSeatToCanvas(int seatNum, double centerX, double centerY,
                                         double width, double height, boolean isJump) {
        if (seatNum > selectedSchedule.getVan().getCapacity()) return;

        boolean isOccupied = selectedSchedule.isSeatBooked(seatNum);

        AnimatedSeatButton seatBtn = new AnimatedSeatButton(seatNum, isJump, isOccupied, () -> {
            if (currentSelectedSeats.contains(seatNum)) {
                currentSelectedSeats.remove(Integer.valueOf(seatNum));
            } else {
                currentSelectedSeats.add(seatNum);
            }
            updateSeatSummaryDisplay();
        });

        setImageRelativeBounds(seatBtn, centerX, centerY, width, height);
        vanBackgroundCanvas.add(seatBtn);
    }

    private void setImageRelativeBounds(JComponent component, double centerX, double centerY,
                                        double width, double height) {
        java.awt.geom.Rectangle2D bounds = vanBackgroundCanvas.getImageBounds();
        int componentWidth = (int) Math.round(bounds.getWidth() * width);
        int componentHeight = (int) Math.round(bounds.getHeight() * height);
        int x = (int) Math.round(bounds.getX() + bounds.getWidth() * centerX - componentWidth / 2.0);
        int y = (int) Math.round(bounds.getY() + bounds.getHeight() * centerY - componentHeight / 2.0);
        component.setBounds(x, y, componentWidth, componentHeight);
    }

    private void renderStandardVanLayout(String modelName) {
        addDriverLabel(0.728, 0.269, 0.052, 0.105);

        double[][] seats;
        if (modelName.contains("NV350")) {
            seats = new double[][] {
                {0.294, 0.224}, {0.294, 0.424}, {0.294, 0.644},
                {0.444, 0.224}, {0.444, 0.424}, {0.444, 0.644},
                {0.594, 0.224}, {0.594, 0.424}, {0.594, 0.644},
                {0.802, 0.269}, {0.728, 0.507}, {0.802, 0.507},
                {0.728, 0.744}, {0.802, 0.744}
            };
        } else if (modelName.contains("URVAN")) {
            seats = new double[][] {
                {0.294, 0.224}, {0.294, 0.424}, {0.294, 0.644},
                {0.444, 0.224}, {0.444, 0.424}, {0.444, 0.644},
                {0.594, 0.224}, {0.594, 0.424}, {0.594, 0.644},
                {0.802, 0.269}, {0.728, 0.507}, {0.802, 0.507},
                {0.728, 0.744}, {0.802, 0.744},
                {0.144, 0.25}
            };
        } else {
            seats = new double[][] {
                {0.294, 0.224}, {0.294, 0.424}, {0.294, 0.775},
                {0.444, 0.224}, {0.444, 0.424}, {0.444, 0.775},
                {0.594, 0.224}, {0.594, 0.424}, {0.594, 0.644},
                {0.802, 0.269}, {0.728, 0.507}, {0.802, 0.507},
                {0.728, 0.744}, {0.802, 0.744}
            };
        }

        for (int i = 0; i < seats.length; i++) {
            double width = seats[i][0] > 0.7
                    ? (seats[i][0] < 0.76 ? 0.052 : 0.095)
                    : 0.119;
            addAnimatedSeatToCanvas(i + 1, seats[i][0], seats[i][1], width, 0.19, false);
        }
    }

    private void renderHiAceDeluxeLayout() {
        addCabSeats();

        double[][] seats = {
            {0.781, 0.486, 0.15, 0.20, 0},
            {0.781, 0.692, 0.15, 0.20, 0},
            {0.578, 0.196, 0.13, 0.16, 0},
            {0.578, 0.358, 0.13, 0.16, 0},
            {0.578, 0.521, 0.13, 0.16, 0},
            {0.578, 0.684, 0.13, 0.16, 0},
            {0.380, 0.215, 0.13, 0.20, 0},
            {0.380, 0.420, 0.13, 0.20, 0},
            {0.380, 0.590, 0.14, 0.14, 1},
            {0.380, 0.760, 0.13, 0.20, 0},
            {0.185, 0.215, 0.13, 0.20, 0},
            {0.185, 0.420, 0.13, 0.20, 0},
            {0.178, 0.590, 0.14, 0.14, 1},
            {0.185, 0.760, 0.13, 0.20, 0}
        };
        for (int i = 0; i < seats.length; i++) {
            addAnimatedSeatToCanvas(i + 1, seats[i][0], seats[i][1],
                    seats[i][2], seats[i][3], seats[i][4] == 1);
        }
    }

    private void addCabSeats() {
        JComponent cabSeats = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = Math.min(30, h / 3);
                int leftWidth = (int) (w * 0.45);
                int rightX = (int) (w * 0.48);
                int rightWidth = w - rightX - 1;

                g2.setColor(VanGoTheme.DANGER);
                g2.fillRoundRect(1, 1, leftWidth, h - 2, arc, arc);
                g2.fillRoundRect(rightX, 1, rightWidth, h - 2, arc, arc);
                g2.setColor(VanGoTheme.NAVY_DARK);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, leftWidth, h - 2, arc, arc);
                g2.drawRoundRect(rightX, 1, rightWidth, h - 2, arc, arc);

                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect((int) (w * 0.07), (int) (h * 0.35),
                        (int) (w * 0.12), (int) (h * 0.30), arc / 2, arc / 2);
                int wheelDiameter = (int) (h * 0.53);
                int wheelX = rightX + rightWidth - wheelDiameter / 2;
                int wheelY = (h - wheelDiameter) / 2;
                g2.drawOval(wheelX, wheelY, wheelDiameter, wheelDiameter);
                g2.drawLine(wheelX + wheelDiameter / 2, wheelY + 2,
                        wheelX + wheelDiameter / 2, wheelY + wheelDiameter - 2);
                g2.drawLine(wheelX + 2, wheelY + wheelDiameter / 2,
                        wheelX + wheelDiameter - 2, wheelY + wheelDiameter / 2);
                g2.dispose();
            }
        };
        cabSeats.setOpaque(false);
        setImageRelativeBounds(cabSeats, 0.781, 0.260, 0.15, 0.22);
        vanBackgroundCanvas.add(cabSeats);
    }

    private void updateSeatSummaryDisplay() {
        if (currentSelectedSeats.isEmpty()) {
            lblSelectedSeatsText.setText("Selected Seats: None");
            lblTotalFareText.setText("Total Price: ₱0.00");
        } else {
            Collections.sort(currentSelectedSeats);
            lblSelectedSeatsText.setText("Selected Seats: " + currentSelectedSeats.toString());
            double total = currentSelectedSeats.size() * selectedSchedule.getFare();
            lblTotalFareText.setText("Total Price: ₱" + String.format("%.2f", total));
        }
    }

    // --- SCREEN 3: SUMMARY ---
    private JPanel buildSummaryView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Step 3 of 3", "Passenger Information & Pass Confirmation", false, false), BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 25, 0));
        content.setBorder(new EmptyBorder(25, 30, 25, 30));
        content.setOpaque(false);

        JPanel summaryCard = createClarityCard();
        summaryCard.setLayout(new BorderLayout(0, 15));

        JLabel lblTitle = new JLabel("Trip Summary Breakdown");
        lblTitle.setFont(VanGoTheme.h3());
        lblTitle.setForeground(BRAND_NAVY);
        summaryCard.add(lblTitle, BorderLayout.NORTH);

        JPanel summaryBody = new JPanel();
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));
        summaryBody.setOpaque(false);

        lblSumRef   = createClarityDetailRow(summaryBody, "Booking Ref No", "-");
        lblSumDest  = createClarityDetailRow(summaryBody, "Destination", "-");
        lblSumTime  = createClarityDetailRow(summaryBody, "Departure Time", "-");
        lblSumVan   = createClarityDetailRow(summaryBody, "Vehicle Model", "-");
        lblSumDriver= createClarityDetailRow(summaryBody, "Driver", "-");
        lblSumSeats = createClarityDetailRow(summaryBody, "Selected Seats", "-");

        summaryBody.add(Box.createRigidArea(new Dimension(0, 12)));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_NEUTRAL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        summaryBody.add(sep);
        summaryBody.add(Box.createRigidArea(new Dimension(0, 12)));

        lblSumTotal = createClarityDetailRow(summaryBody, "Total Price Due", "₱0.00");

        summaryCard.add(summaryBody, BorderLayout.CENTER);

        JPanel formCard = createClarityCard();
        formCard.setLayout(new BorderLayout(0, 15));

        JLabel lblFormTitle = new JLabel("Passenger Information");
        lblFormTitle.setFont(VanGoTheme.h3());
        lblFormTitle.setForeground(BRAND_NAVY);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel formFields = new JPanel();
        formFields.setLayout(new BoxLayout(formFields, BoxLayout.Y_AXIS));
        formFields.setOpaque(false);

        JLabel lbl1 = new JLabel("Full Legal Name:");
        lbl1.setFont(VanGoTheme.label());
        lbl1.setForeground(TEXT_MUTED);
        formFields.add(lbl1);
        formFields.add(Box.createRigidArea(new Dimension(0, 4)));
        txtPassengerName = createClarityTextField();
        txtPassengerName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        formFields.add(txtPassengerName);

        formFields.add(Box.createRigidArea(new Dimension(0, 15)));

        JLabel lbl2 = new JLabel("Mobile Contact Number:");
        lbl2.setFont(VanGoTheme.label());
        lbl2.setForeground(TEXT_MUTED);
        formFields.add(lbl2);
        formFields.add(Box.createRigidArea(new Dimension(0, 4)));
        txtPassengerContact = createClarityTextField();
        txtPassengerContact.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        formFields.add(txtPassengerContact);

        formFields.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel lblNotice = new JLabel("<html><body style='width: 320px; color: #475569;'>" +
                "Present your booking reference code at the payment counter to claim your ticket.</body></html>");
        lblNotice.setFont(VanGoTheme.font(12, Font.PLAIN));
        formFields.add(lblNotice);

        formCard.add(formFields, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionPanel.setOpaque(false);

        ClarityButton btnBack = new ClarityButton("← Change Seats", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 14);
        btnBack.setPreferredSize(new Dimension(140, 48));
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "SEATS"));

        ClarityButton btnSubmit = new ClarityButton("Submit Booking  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 14);
        btnSubmit.setPreferredSize(new Dimension(200, 48));
        btnSubmit.addActionListener(e -> handleBookingSubmission());

        actionPanel.add(btnBack);
        actionPanel.add(btnSubmit);

        formCard.add(actionPanel, BorderLayout.SOUTH);

        content.add(summaryCard);
        content.add(formCard);
        root.add(content, BorderLayout.CENTER);

        return root;
    }

    private String generatedReferenceNumber = "";

    private void populateSummaryDetails() {
        generatedReferenceNumber = "VG-" + (100000 + new Random().nextInt(900000));

        lblSumRef.setText(generatedReferenceNumber);
        lblSumDest.setText(selectedSchedule.getDestination());
        lblSumTime.setText(selectedSchedule.getDepartureTime());
        lblSumVan.setText(selectedSchedule.getVan().getModel() + " (" + selectedSchedule.getVan().getPlateNumber() + ")");
        lblSumDriver.setText(selectedSchedule.getVan().getDriver().getName());
        lblSumSeats.setText(currentSelectedSeats.toString());

        double total = currentSelectedSeats.size() * selectedSchedule.getFare();
        lblSumTotal.setText("₱" + String.format("%.2f", total));
    }

    private void handleBookingSubmission() {
        String name = txtPassengerName.getText().trim();
        String phone = txtPassengerContact.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter full passenger name and contact number.", "MISSING INFORMATION", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int seat : currentSelectedSeats) {
            if (selectedSchedule.isSeatBooked(seat)) {
                JOptionPane.showMessageDialog(this, "Seat " + seat + " was just booked by another user.", "SEAT CONFLICT", JOptionPane.ERROR_MESSAGE);
                cardLayout.show(mainContainer, "SEATS");
                return;
            }
        }

        for (int seat : currentSelectedSeats) {
            selectedSchedule.bookSeat(seat);
        }

        double total = currentSelectedSeats.size() * selectedSchedule.getFare();
        Booking booking = new Booking(generatedReferenceNumber, selectedSchedule, new ArrayList<>(currentSelectedSeats), name, phone, total);
        bookingDatabase.put(generatedReferenceNumber, booking);

        JOptionPane.showMessageDialog(this,
                "BOOKING SUBMITTED SUCCESSFULLY!\n\n" +
                "Reference Number: " + generatedReferenceNumber + "\n" +
                "Passenger: " + name + "\n" +
                "Total Due: ₱" + String.format("%.2f", total) + "\n\n" +
                "Please proceed to the payment counter with your reference code.",
                "CONFIRMATION PASS GENERATED", JOptionPane.INFORMATION_MESSAGE);

        txtPassengerName.setText("");
        txtPassengerContact.setText("");
        selectedSchedule = null;
        currentSelectedSeats.clear();
        cardLayout.show(mainContainer, "WELCOME");
    }

    // --- STAFF GATEWAY ---
    private JPanel buildStaffGatewayView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Authentication Gateway", "Select Staff Portal Role", false, true), BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel contentBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        contentBox.setOpaque(false);

        // Admin Card
        JPanel cardAdmin = createClarityCard();
        cardAdmin.setPreferredSize(new Dimension(380, 360));
        cardAdmin.setLayout(new BorderLayout(0, 20));

        JPanel adminHeader = new JPanel();
        adminHeader.setLayout(new BoxLayout(adminHeader, BoxLayout.Y_AXIS));
        adminHeader.setOpaque(false);

        JLabel lblAdminIcon = new JLabel("🔑", SwingConstants.CENTER);
        lblAdminIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblAdminIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAdminTitle = new JLabel("Administrator Login");
        lblAdminTitle.setFont(VanGoTheme.h2());
        lblAdminTitle.setForeground(BRAND_NAVY);
        lblAdminTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAdminDesc = new JLabel("<html><center style='color:#475569;'>Full operations access. Manage FCFS queues, approve early departures, process tickets.</center></html>");
        lblAdminDesc.setFont(VanGoTheme.font(13, Font.PLAIN));
        lblAdminDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        adminHeader.add(lblAdminIcon);
        adminHeader.add(Box.createRigidArea(new Dimension(0, 10)));
        adminHeader.add(lblAdminTitle);
        adminHeader.add(Box.createRigidArea(new Dimension(0, 10)));
        adminHeader.add(lblAdminDesc);

        ClarityButton btnLoginAdmin = new ClarityButton("Login as Administrator  →", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 14);
        btnLoginAdmin.setPreferredSize(new Dimension(0, 50));
        btnLoginAdmin.addActionListener(e -> {
            refreshAdminTables();
            cardLayout.show(mainContainer, "ADMIN_DASHBOARD");
        });

        cardAdmin.add(adminHeader, BorderLayout.CENTER);
        cardAdmin.add(btnLoginAdmin, BorderLayout.SOUTH);

        // Driver Card
        JPanel cardDriver = createClarityCard();
        cardDriver.setPreferredSize(new Dimension(380, 360));
        cardDriver.setLayout(new BorderLayout(0, 20));

        JPanel driverHeader = new JPanel();
        driverHeader.setLayout(new BoxLayout(driverHeader, BoxLayout.Y_AXIS));
        driverHeader.setOpaque(false);

        JLabel lblDriverIcon = new JLabel("🚐", SwingConstants.CENTER);
        lblDriverIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblDriverIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDriverTitle = new JLabel("Driver Console Login");
        lblDriverTitle.setFont(VanGoTheme.h2());
        lblDriverTitle.setForeground(BRAND_NAVY);
        lblDriverTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDriverDesc = new JLabel("<html><center style='color:#475569;'>Driver portal. Request early departure overrides, check queue standing, manage vehicle status.</center></html>");
        lblDriverDesc.setFont(VanGoTheme.font(13, Font.PLAIN));
        lblDriverDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        driverHeader.add(lblDriverIcon);
        driverHeader.add(Box.createRigidArea(new Dimension(0, 10)));
        driverHeader.add(lblDriverTitle);
        driverHeader.add(Box.createRigidArea(new Dimension(0, 10)));
        driverHeader.add(lblDriverDesc);

        ClarityButton btnLoginDriver = new ClarityButton("Login as Driver  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 14);
        btnLoginDriver.setPreferredSize(new Dimension(0, 50));
        btnLoginDriver.addActionListener(e -> openDriverPortalLogin());

        cardDriver.add(driverHeader, BorderLayout.CENTER);
        cardDriver.add(btnLoginDriver, BorderLayout.SOUTH);

        contentBox.add(cardAdmin);
        contentBox.add(cardDriver);

        centerWrapper.add(contentBox);
        root.add(centerWrapper, BorderLayout.CENTER);
        return root;
    }

    // =========================================================================
    // ADMIN DASHBOARD — REDESIGNED
    // =========================================================================

    private JPanel buildAdminDashboardView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);
        root.add(createStatusHeader("Administrator Workspace",
                "Terminal Counter & Operations Control", false, false), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(new EmptyBorder(16, 28, 16, 28));
        body.setOpaque(false);

        // ---- KPI STRIP ----
        JPanel kpiStrip = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiStrip.setOpaque(false);
        kpiStrip.setPreferredSize(new Dimension(0, 92));
        kpiStrip.add(createStatCard("Trips Today", "0", "scheduled departures", BRAND_NAVY));
        kpiStrip.add(createStatCard("Ready to Dispatch", "0", "awaiting admin action", SEMANTIC_AMBER));
        kpiStrip.add(createStatCard("Pending Approval", "0", "driver early-departure", SEMANTIC_RED));
        kpiStrip.add(createStatCard("Boarding", "0", "accepting passengers", SEMANTIC_GO));
        kpiStrip.add(createStatCard("Fleet Utilization", "0%", "seats booked today", BRAND_ORANGE));
        body.add(kpiStrip, BorderLayout.NORTH);

        // ---- SEGMENTED TABS ----
        segmentedTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        segmentedTabBar.setOpaque(false);
        segmentedTabBar.setBorder(new EmptyBorder(4, 0, 4, 0));

        tabContentLayout = new CardLayout();
        tabContentPanel = new JPanel(tabContentLayout);
        tabContentPanel.setOpaque(false);

        tabContentPanel.add(buildDispatchAdminTab(),  "DISPATCH");
        tabContentPanel.add(buildDriverQueueAdminTab(), "QUEUE");
        tabContentPanel.add(buildCounterTab(),         "COUNTER");
        tabContentPanel.add(buildSchedulesAdminTab(),  "SCHEDULES");
        tabContentPanel.add(buildVansAdminTab(),       "VANS");
        tabContentPanel.add(buildDriversAdminTab(),    "DRIVERS");

        addSegmentedTab("DISPATCH",  "⚡  Dispatch Hub");
        addSegmentedTab("QUEUE",     "📋  FCFS Queue");
        addSegmentedTab("COUNTER",   "💳  Payment Counter");
        addSegmentedTab("SCHEDULES", "📅  Trip Schedules");
        addSegmentedTab("VANS",      "🚐  Fleet Vans");
        addSegmentedTab("DRIVERS",   "👨‍✈️  Driver Registry");

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(segmentedTabBar, BorderLayout.NORTH);

        JPanel tabFrame = new JPanel(new BorderLayout());
        tabFrame.setBackground(CARD_SURFACE);
        tabFrame.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));
        tabFrame.add(tabContentPanel, BorderLayout.CENTER);
        center.add(tabFrame, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        // ---- FOOTER ----
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        ClarityButton btnExitAdmin = new ClarityButton("← Logout & Return to Kiosk",
                CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
        btnExitAdmin.setPreferredSize(new Dimension(230, 40));
        btnExitAdmin.addActionListener(e -> {
            renderScheduleCards();
            cardLayout.show(mainContainer, "WELCOME");
        });
        footer.add(btnExitAdmin);
        body.add(footer, BorderLayout.SOUTH);

        root.add(body, BorderLayout.CENTER);

        showTab("DISPATCH");
        refreshKpiCards();
        return root;
    }

    // ---- KPI STAT CARD ----
    private JPanel createStatCard(String title, String initialValue, String sub, Color accent) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(CARD_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_NEUTRAL, 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        JPanel stripe = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 4, 5, getHeight() - 8, 5, 5);
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(10, 0));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(VanGoTheme.font(10, Font.BOLD));
        lblTitle.setForeground(TEXT_MUTED);

        JLabel lblValue = new JLabel(initialValue);
        lblValue.setFont(VanGoTheme.font(24, Font.BOLD));
        lblValue.setForeground(BRAND_NAVY);

        JLabel lblSub = new JLabel(sub);
        lblSub.setFont(VanGoTheme.font(10, Font.PLAIN));
        lblSub.setForeground(VanGoTheme.TEXT_TERT);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.add(lblTitle);
        textCol.add(Box.createRigidArea(new Dimension(0, 2)));
        textCol.add(lblValue);
        textCol.add(Box.createRigidArea(new Dimension(0, 1)));
        textCol.add(lblSub);

        // Keep references so we can refresh
        switch (title) {
            case "Trips Today":        kpiValueTrips    = lblValue; break;
            case "Ready to Dispatch":  kpiValueReady    = lblValue; break;
            case "Pending Approval":   kpiValuePending  = lblValue; break;
            case "Boarding":           kpiValueBoarding = lblValue; break;
            case "Fleet Utilization":  kpiValueUtil     = lblValue; break;
        }

        card.add(stripe, BorderLayout.WEST);
        card.add(textCol, BorderLayout.CENTER);
        return card;
    }

    private void refreshKpiCards() {
        if (kpiValueTrips    != null) kpiValueTrips.setText(String.valueOf(schedules.size()));
        if (kpiValueReady    != null) kpiValueReady.setText(String.valueOf(countReady()));
        if (kpiValuePending  != null) kpiValuePending.setText(String.valueOf(countPendingApproval()));
        if (kpiValueBoarding != null) kpiValueBoarding.setText(String.valueOf(countBoarding()));
        if (kpiValueUtil     != null) kpiValueUtil.setText(fleetUtilizationPct() + "%");
    }

    private int countReady() {
        int n = 0;
        for (Schedule s : schedules)
            if (!s.isDispatched() && (s.isFull() || s.getDriverDepartureRequested())) n++;
        return n;
    }
    private int countPendingApproval() {
        int n = 0;
        for (Schedule s : schedules)
            if (s.getDriverDepartureRequested() && !s.isDispatched()) n++;
        return n;
    }
    private int countBoarding() {
        int n = 0;
        for (Schedule s : schedules)
            if (!s.isDispatched() && !s.isFull() && !s.getDriverDepartureRequested()) n++;
        return n;
    }
    private int fleetUtilizationPct() {
        int booked = 0, cap = 0;
        for (Schedule s : schedules) { booked += s.getBookedCount(); cap += s.getCapacity(); }
        return cap == 0 ? 0 : Math.round(booked * 100f / cap);
    }

    // ---- SEGMENTED TABS ----
    private void addSegmentedTab(String key, String label) {
        ClarityButton btn = new ClarityButton(label, CARD_SURFACE, TEXT_MUTED, BORDER_NEUTRAL, 12);
        btn.setPreferredSize(new Dimension(170, 38));
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.addActionListener(e -> showTab(key));
        tabButtons.put(key, btn);
        segmentedTabBar.add(btn);
    }

    private void showTab(String key) {
        if (tabContentLayout == null) return;
        tabContentLayout.show(tabContentPanel, key);
        for (Map.Entry<String, ClarityButton> e : tabButtons.entrySet()) {
            boolean active = e.getKey().equals(key);
            e.getValue().setBaseBackground(active ? BRAND_NAVY : CARD_SURFACE);
            e.getValue().setForeground(active ? Color.WHITE : TEXT_MUTED);
            e.getValue().repaint();
        }
    }

    // ---- STATUS PILL RENDERER ----
    private class StatusPillRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            String s = String.valueOf(v);
            Color bg, fg;
            if (s.contains("READY") || s.contains("FULL")) { bg = VanGoTheme.WARN_BG; fg = SEMANTIC_AMBER; }
            else if (s.contains("APPROVAL"))               { bg = VanGoTheme.DANGER_BG; fg = SEMANTIC_RED; }
            else if (s.contains("DISPATCHED"))             { bg = VanGoTheme.SURFACE_ALT; fg = VanGoTheme.TEXT_TERT; }
            else                                           { bg = VanGoTheme.GO_BG; fg = SEMANTIC_GO; }
            return makeStatusPill(shortStatus(s), bg, fg);
        }
        private String shortStatus(String s) {
            if (s.contains("FULL"))       return "● FULL · READY";
            if (s.contains("APPROVAL"))   return "● AWAITING APPROVAL";
            if (s.contains("DISPATCHED")) return "● DISPATCHED";
            if (s.contains("BOARDING"))   return "● BOARDING";
            return s;
        }
    }

    private JLabel makeStatusPill(String text, Color bg, Color fg) {
        JLabel pill = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setFont(VanGoTheme.font(10, Font.BOLD));
        pill.setForeground(fg);
        pill.setBorder(new EmptyBorder(4, 10, 4, 10));
        return pill;
    }

    // ---- TAB: DISPATCH HUB ----
    private JPanel buildDispatchAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"Trip ID", "Destination", "Departure", "Occupancy", "Vehicle", "Driver", "Status"};
        modelDispatch = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelDispatch);
        table.setRowHeight(46);
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusPillRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        // Contextual action card on the right
        JPanel actionCard = createClarityCard();
        actionCard.setLayout(new BoxLayout(actionCard, BoxLayout.Y_AXIS));
        actionCard.setPreferredSize(new Dimension(280, 0));

        JLabel actTitle = new JLabel("Dispatch Actions");
        actTitle.setFont(VanGoTheme.h3());
        actTitle.setForeground(BRAND_NAVY);
        actTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel actHint = new JLabel("<html><div style='width:230px; color:#94A3B8;'>" +
                "Select a trip row, then approve dispatch or reject a pending early-departure request." +
                "</div></html>");
        actHint.setFont(VanGoTheme.font(11, Font.PLAIN));
        actHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnApproveDispatch = new ClarityButton("✓  Approve & Dispatch", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
        btnApproveDispatch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnApproveDispatch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnRejectEarly = new ClarityButton("✕  Reject Early Departure", SEMANTIC_RED, Color.WHITE, SEMANTIC_RED, 13);
        btnRejectEarly.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnRejectEarly.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionCard.add(actTitle);
        actionCard.add(Box.createRigidArea(new Dimension(0, 6)));
        actionCard.add(actHint);
        actionCard.add(Box.createRigidArea(new Dimension(0, 22)));
        actionCard.add(btnApproveDispatch);
        actionCard.add(Box.createRigidArea(new Dimension(0, 10)));
        actionCard.add(btnRejectEarly);

        btnApproveDispatch.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a trip from the dispatch queue first.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String tripId = (String) modelDispatch.getValueAt(selectedRow, 0);
            Schedule sch = findScheduleById(tripId);
            if (sch != null) executeDispatchFlow(sch);
        });

        btnRejectEarly.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a trip to reject.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String tripId = (String) modelDispatch.getValueAt(selectedRow, 0);
            Schedule sch = findScheduleById(tripId);
            if (sch != null && sch.getDriverDepartureRequested()) {
                String reason = JOptionPane.showInputDialog(this, "Enter reason for rejecting early departure:", "Admin rejected: insufficient passengers");
                if (reason == null) return;
                if (reason.trim().isEmpty()) reason = "Admin rejected: insufficient passengers";

                sch.setDriverDepartureRequested(false);
                sch.setState(Schedule.DispatchState.SCHEDULED);
                sch.setRejectionReason(reason);

                JOptionPane.showMessageDialog(this, "Early departure request rejected.\nTrip returned to SCHEDULED state.", "REQUEST REJECTED", JOptionPane.INFORMATION_MESSAGE);
                refreshAdminTables();
            } else {
                JOptionPane.showMessageDialog(this, "Selected trip has no pending early departure request.", "INVALID SELECTION", JOptionPane.WARNING_MESSAGE);
            }
        });

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actionCard, BorderLayout.EAST);
        return panel;
    }

    private void executeDispatchFlow(Schedule schedule) {
        String dest = schedule.getDestination();
        Van frontVan = driverQueue.peek(dest);

        if (frontVan == null) {
            JOptionPane.showMessageDialog(this, "Queue error: No van found in queue for " + dest, "QUEUE ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!frontVan.getId().equals(schedule.getVan().getId())) {
            String message = String.format(
                    "FCFS QUEUE VIOLATION DETECTED!\n\n" +
                    "Destination: %s\n\n" +
                    "Assigned Trip Van: %s (%s) driven by %s\n" +
                    "Front of Queue Van: %s (%s) driven by %s\n\n" +
                    "Rule 3 requires the queue leader to depart first.\n" +
                    "Would you like to SWAP the assigned van with the queue leader and dispatch immediately?",
                    dest,
                    schedule.getVan().getModel(), schedule.getVan().getPlateNumber(), schedule.getVan().getDriver().getName(),
                    frontVan.getModel(), frontVan.getPlateNumber(), frontVan.getDriver().getName()
            );

            int choice = JOptionPane.showOptionDialog(
                    this,
                    message,
                    "FCFS QUEUE VIOLATION — SWAP OPTION",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new String[]{"Swap Van & Dispatch Now", "Cancel Dispatch"},
                    "Swap Van & Dispatch Now"
            );

            if (choice == JOptionPane.YES_OPTION) {
                schedule.setVan(frontVan);
            } else {
                JOptionPane.showMessageDialog(this, "Dispatch aborted. Trip remains in current state.", "DISPATCH CANCELLED", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        driverQueue.dequeue(dest);
        schedule.setDispatched(true);
        schedule.setDriverDepartureRequested(false);
        schedule.getVan().incrementTripsCompleted();

        driverQueue.enqueue(dest, schedule.getVan());

        JOptionPane.showMessageDialog(this,
                "TRIP DISPATCHED SUCCESSFULLY!\n\n" +
                "Trip ID: " + schedule.getId() + "\n" +
                "Destination: " + dest + "\n" +
                "Dispatched Vehicle: " + schedule.getVan().getModel() + " (" + schedule.getVan().getPlateNumber() + ")\n" +
                "Driver: " + schedule.getVan().getDriver().getName() + "\n\n" +
                "Vehicle re-enqueued at back of the " + dest + " queue.",
                "DISPATCH CONFIRMED", JOptionPane.INFORMATION_MESSAGE);

        refreshAdminTables();
    }

        // ---- TAB: FCFS QUEUE ----
    private JPanel buildDriverQueueAdminTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"Destination Queue", "Position", "Van Model", "Plate No", "Driver Name", "Trips Today", "Current Assignment"};
        modelQueue = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        // 1. Create the standard table
        JTable table = createClarityTable(modelQueue);
        table.setRowHeight(42);

        // 2. Apply a custom renderer safely to highlight the FRONT of the queue
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                
                // Standard zebra striping logic from your base theme
                if (isSelected) {
                    c.setBackground(VanGoTheme.ICE);
                    c.setForeground(VanGoTheme.NAVY);
                } else if (row % 2 == 0) {
                    c.setBackground(VanGoTheme.SURFACE);
                    c.setForeground(VanGoTheme.TEXT);
                } else {
                    c.setBackground(new Color(0xFA, 0xFB, 0xFD));
                    c.setForeground(VanGoTheme.TEXT);
                }

                // Override for FRONT of the queue
                String pos = (String) modelQueue.getValueAt(row, 1);
                if (pos != null && pos.contains("FRONT")) {
                    c.setBackground(new Color(0xFFF7ED)); // Soft orange tint
                    c.setForeground(BRAND_NAVY);
                    c.setFont(VanGoTheme.font(13, Font.BOLD));
                } else {
                    c.setFont(VanGoTheme.font(13, Font.PLAIN));
                }
                
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hint.setOpaque(false);
        JLabel lblHint = new JLabel("● Orange rows mark the FRONT of each destination's FCFS queue — they must depart before any later van.");
        lblHint.setFont(VanGoTheme.font(11, Font.PLAIN));
        lblHint.setForeground(TEXT_MUTED);
        hint.add(lblHint);

        panel.add(hint, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    // ---- TAB: PAYMENT COUNTER ----
    private JPanel buildCounterTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        // LEFT — lookup + action
        JPanel left = createClarityCard();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(320, 0));

        JLabel title = new JLabel("Lookup Booking");
        title.setFont(VanGoTheme.h3());
        title.setForeground(BRAND_NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("<html><div style='width:260px;'>Enter the passenger's reference number to pull up their pending booking.</div></html>");
        hint.setFont(VanGoTheme.font(11, Font.PLAIN));
        hint.setForeground(TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSearch = new JLabel("Reference Number:");
        lblSearch.setFont(VanGoTheme.font(12, Font.BOLD));
        lblSearch.setForeground(TEXT_MUTED);
        lblSearch.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtSearch = createClarityTextField();
        txtSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        txtSearch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnSearch = new ClarityButton("🔍  Find Pass", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 13);
        btnSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnSearch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnConfirmPay = new ClarityButton("Confirm Payment & Issue Ticket", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
        btnConfirmPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnConfirmPay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmPay.setEnabled(false);

        left.add(title);
        left.add(Box.createRigidArea(new Dimension(0, 4)));
        left.add(hint);
        left.add(Box.createRigidArea(new Dimension(0, 20)));
        left.add(lblSearch);
        left.add(Box.createRigidArea(new Dimension(0, 4)));
        left.add(txtSearch);
        left.add(Box.createRigidArea(new Dimension(0, 8)));
        left.add(btnSearch);
        left.add(Box.createVerticalGlue());
        left.add(btnConfirmPay);

        // RIGHT — receipt paper
        JTextArea txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setBackground(new Color(0xFBFCFE));
        txtReceipt.setForeground(TEXT_PRIMARY);
        txtReceipt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReceipt.setBorder(new EmptyBorder(18, 18, 18, 18));

        JScrollPane scroll = new JScrollPane(txtReceipt);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));
        scroll.setBackground(CARD_SURFACE);

        btnSearch.addActionListener(e -> {
            String ref = txtSearch.getText().trim();
            if (bookingDatabase.containsKey(ref)) {
                Booking b = bookingDatabase.get(ref);
                txtReceipt.setText(
                        "====================================================\n" +
                        "           VANGO TERMINAL OFFICIAL RECEIPT          \n" +
                        "====================================================\n" +
                        " BOOKING REF NO : " + b.getReferenceNo() + "\n" +
                        " PAYMENT STATUS : " + b.getStatus() + "\n" +
                        " PASSENGER NAME : " + b.getPassengerName() + "\n" +
                        " CONTACT NUMBER : " + b.getPassengerContact() + "\n" +
                        "----------------------------------------------------\n" +
                        " DESTINATION    : " + b.getSchedule().getDestination() + "\n" +
                        " DEPARTURE TIME : " + b.getSchedule().getDepartureTime() + "\n" +
                        " VEHICLE MODEL  : " + b.getSchedule().getVan().getModel() + " [" + b.getSchedule().getVan().getPlateNumber() + "]\n" +
                        " DRIVER NAME    : " + b.getSchedule().getVan().getDriver().getName() + " (" + b.getSchedule().getVan().getDriver().getLicenseNo() + ")\n" +
                        " SEATS RESERVED : " + b.getSelectedSeats().toString() + "\n" +
                        " TOTAL PRICE    : ₱" + String.format("%.2f", b.getTotalAmount()) + "\n" +
                        "====================================================\n"
                );
                btnConfirmPay.setEnabled(b.getStatus().equals("PENDING PAYMENT"));
            } else {
                txtReceipt.setText("ERROR: No booking found matches Reference Code: " + ref);
                btnConfirmPay.setEnabled(false);
            }
        });

        btnConfirmPay.addActionListener(e -> {
            String ref = txtSearch.getText().trim();
            if (bookingDatabase.containsKey(ref)) {
                Booking b = bookingDatabase.get(ref);
                b.setStatus("CONFIRMED & PAID");
                JOptionPane.showMessageDialog(this, "Payment approved. Ticket confirmed!", "TRANSACTION APPROVED", JOptionPane.INFORMATION_MESSAGE);
                btnSearch.doClick();
            }
        });

        panel.add(left, BorderLayout.WEST);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ---- TAB: TRIP SCHEDULES ----
        // ---- TAB: TRIP SCHEDULES ----
    private JPanel buildSchedulesAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"ID", "Destination", "Departure", "Fare", "Assigned Van", "Driver"};
        modelSchedules = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelSchedules);
        table.setRowHeight(42);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel form = new JPanel(new GridLayout(10, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        // CHANGED: Use an editable JComboBox for Destination to allow searching Region 8
        JComboBox<String> cbDest = new JComboBox<>(Region8Destinations.DESTINATIONS.toArray(new String[0]));
        cbDest.setEditable(true);
        cbDest.setBackground(CARD_SURFACE);
        cbDest.setFont(VanGoTheme.body());

        JTextField txtTime = createClarityTextField();
        JTextField txtFare = createClarityTextField();
        cbScheduleVan = new JComboBox<>();

        form.add(createFormLabel("Destination (Type to search):"));
        form.add(cbDest);
        form.add(createFormLabel("Departure Time:"));
        form.add(txtTime);
        form.add(createFormLabel("Fare Price (₱):"));
        form.add(txtFare);
        form.add(createFormLabel("Assign Van & Driver:"));
        form.add(cbScheduleVan);

        ClarityButton btnAddSch = new ClarityButton("+ Register New Schedule", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 12);
        btnAddSch.addActionListener(e -> {
            try {
                // Get text whether selected from dropdown or typed manually
                String dest = cbDest.getEditor().getItem().toString().trim();
                String time = txtTime.getText().trim();
                double fare = Double.parseDouble(txtFare.getText().trim());
                Van v = (Van) cbScheduleVan.getSelectedItem();

                if (dest.isEmpty() || time.isEmpty() || v == null) {
                    JOptionPane.showMessageDialog(this, "Complete all trip fields.", "INPUT ERROR", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String id = "SCH00" + (schedules.size() + 1);
                Schedule newSch = new Schedule(id, dest, time, fare, v);
                schedules.add(newSch);

                driverQueue.enqueueIfAbsent(dest, v);

                txtTime.setText(""); 
                txtFare.setText("");
                cbDest.setSelectedItem(""); // Clear selection
                
                refreshAdminTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid fare amount entered.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        ClarityButton btnDeleteSch = new ClarityButton("Delete Selected Schedule", SEMANTIC_RED, Color.WHITE, SEMANTIC_RED, 12);
        btnDeleteSch.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                schedules.remove(selectedRow);
                refreshAdminTables();
            } else {
                JOptionPane.showMessageDialog(this, "Select a trip from the table to delete.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
            }
        });

        form.add(btnAddSch);
        form.add(btnDeleteSch);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    // ---- TAB: FLEET VANS ----
    private JPanel buildVansAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"ID", "Model", "Plate Number", "Capacity", "Driver"};
        modelVans = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelVans);
        table.setRowHeight(42);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel form = new JPanel(new GridLayout(10, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        cbVanModelSelection = new JComboBox<>(new String[]{
                MODEL_HIACE_DELUXE,
                MODEL_HIACE_COMMUTER,
                MODEL_NISSAN_NV350,
                MODEL_NISSAN_URVAN
        });

        JTextField txtPlate = createClarityTextField();
        JTextField txtCap = createClarityTextField();
        txtCap.setText("14");

        cbVanModelSelection.addActionListener(e -> {
            String selectedModel = (String) cbVanModelSelection.getSelectedItem();
            if (selectedModel != null) {
                if (selectedModel.equals(MODEL_HIACE_DELUXE)) txtCap.setText("14");
                else if (selectedModel.equals(MODEL_NISSAN_URVAN)) txtCap.setText("15");
                else txtCap.setText("14");
            }
        });

        cbVanDriver = new JComboBox<>();

        form.add(createFormLabel("Select Van Model:"));
        form.add(cbVanModelSelection);
        form.add(createFormLabel("Plate Number:"));
        form.add(txtPlate);
        form.add(createFormLabel("Seating Capacity:"));
        form.add(txtCap);
        form.add(createFormLabel("Assign Driver:"));
        form.add(cbVanDriver);

        ClarityButton btnAddVan = new ClarityButton("+ Register New Van", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 12);
        btnAddVan.addActionListener(e -> {
            try {
                String model = (String) cbVanModelSelection.getSelectedItem();
                String plate = txtPlate.getText().trim();
                int cap = Integer.parseInt(txtCap.getText().trim());
                Driver d = (Driver) cbVanDriver.getSelectedItem();

                if (model == null || plate.isEmpty() || d == null) {
                    JOptionPane.showMessageDialog(this, "Complete vehicle parameters.", "INPUT ERROR", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String id = "VAN0" + (vans.size() + 1);
                Van newVan = new Van(id, model, plate, cap, d, "");
                vans.add(newVan);

                txtPlate.setText("");
                refreshAdminTables();
                JOptionPane.showMessageDialog(this, "New van added to fleet registry.", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid capacity format.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.add(btnAddVan);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    // ---- TAB: DRIVER REGISTRY ----
    private JPanel buildDriversAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"ID", "Full Name", "License Number", "Phone"};
        modelDrivers = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelDrivers);
        table.setRowHeight(42);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel form = new JPanel(new GridLayout(8, 1, 5, 5));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(280, 0));

        JTextField txtName = createClarityTextField();
        JTextField txtLic = createClarityTextField();
        JTextField txtPhone = createClarityTextField();

        form.add(createFormLabel("Driver Full Name:"));
        form.add(txtName);
        form.add(createFormLabel("License Number:"));
        form.add(txtLic);
        form.add(createFormLabel("Contact Phone:"));
        form.add(txtPhone);

        ClarityButton btnAddDriver = new ClarityButton("+ Register Driver", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 12);
        btnAddDriver.addActionListener(e -> {
            String name = txtName.getText().trim();
            String lic = txtLic.getText().trim();
            String phone = txtPhone.getText().trim();

            if (name.isEmpty() || lic.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete driver information records.", "INPUT ERROR", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String id = "DRV0" + (drivers.size() + 1);
            Driver newDrv = new Driver(id, name, lic, phone);
            drivers.add(newDrv);

            txtName.setText(""); txtLic.setText(""); txtPhone.setText("");
            refreshAdminTables();
            JOptionPane.showMessageDialog(this, "Driver registered successfully.", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
        });

        form.add(btnAddDriver);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    // --- DRIVER DASHBOARD ---
    private JPanel buildDriverDashboardView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Driver Operational Console", "Manage Assigned Vehicle & Departure Requests", false, false), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBorder(new EmptyBorder(25, 30, 25, 30));
        content.setOpaque(false);

        JPanel driverCard = createClarityCard();
        driverCard.setLayout(new BorderLayout(15, 0));

        JPanel infoGroup = new JPanel();
        infoGroup.setLayout(new BoxLayout(infoGroup, BoxLayout.Y_AXIS));
        infoGroup.setOpaque(false);

        lblDriverVanName = new JLabel("Assigned Van: Loading...");
        lblDriverVanName.setFont(VanGoTheme.h2());
        lblDriverVanName.setForeground(BRAND_NAVY);

        lblDriverPlate = new JLabel("Plate Number: - | License: -");
        lblDriverPlate.setFont(VanGoTheme.body());
        lblDriverPlate.setForeground(TEXT_MUTED);

        infoGroup.add(lblDriverVanName);
        infoGroup.add(Box.createRigidArea(new Dimension(0, 4)));
        infoGroup.add(lblDriverPlate);
        infoGroup.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel statusPill = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VanGoTheme.GO_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusPill.setOpaque(false);
        statusPill.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 6));
        lblDriverStatus = new JLabel("● READY FOR BOARDING");
        lblDriverStatus.setFont(VanGoTheme.font(11, Font.BOLD));
        lblDriverStatus.setForeground(SEMANTIC_GO);
        statusPill.add(lblDriverStatus);
        infoGroup.add(statusPill);

        JPanel statusBtnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statusBtnGroup.setOpaque(false);

        ClarityButton btnReady = new ClarityButton("● Ready for Trip", SEMANTIC_GO, Color.WHITE, SEMANTIC_GO, 13);
        btnReady.setPreferredSize(new Dimension(140, 42));
        btnReady.addActionListener(e -> updateDriverVanStatus("READY FOR BOARDING", SEMANTIC_GO));

        ClarityButton btnInTransit = new ClarityButton("🚍 In Transit", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 13);
        btnInTransit.setPreferredSize(new Dimension(130, 42));
        btnInTransit.addActionListener(e -> updateDriverVanStatus("IN TRANSIT / ON THE WAY", BRAND_NAVY));

        ClarityButton btnMaintenance = new ClarityButton("🛠 Off-Duty / Service", SEMANTIC_AMBER, Color.WHITE, SEMANTIC_AMBER, 13);
        btnMaintenance.setPreferredSize(new Dimension(170, 42));
        btnMaintenance.addActionListener(e -> updateDriverVanStatus("OFF-DUTY / MAINTENANCE", SEMANTIC_AMBER));

        statusBtnGroup.add(btnReady);
        statusBtnGroup.add(btnInTransit);
        statusBtnGroup.add(btnMaintenance);

        driverCard.add(infoGroup, BorderLayout.WEST);
        driverCard.add(statusBtnGroup, BorderLayout.EAST);

        driverTripsContainer = new JPanel(new GridLayout(0, 1, 15, 15));
        driverTripsContainer.setOpaque(false);

        JScrollPane scrollTrips = new JScrollPane(driverTripsContainer);
        scrollTrips.setBorder(null);
        scrollTrips.getViewport().setBackground(BG_CANVAS);

        content.add(driverCard, BorderLayout.NORTH);
        content.add(scrollTrips, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        ClarityButton btnLogout = new ClarityButton("← Switch Driver / Logout", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
        btnLogout.setPreferredSize(new Dimension(190, 42));
        btnLogout.addActionListener(e -> cardLayout.show(mainContainer, "STAFF_GATEWAY"));
        footer.add(btnLogout);

        root.add(content, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void openDriverPortalLogin() {
        cbActiveDriverLogin = new JComboBox<>(drivers.toArray(new Driver[0]));
        int result = JOptionPane.showConfirmDialog(
                this,
                cbActiveDriverLogin,
                "DRIVER AUTHENTICATION — SELECT PROFILE",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            loggedInDriver = (Driver) cbActiveDriverLogin.getSelectedItem();
            if (loggedInDriver != null) {
                renderDriverDashboard();
                cardLayout.show(mainContainer, "DRIVER_DASHBOARD");
            }
        }
    }

    private void renderDriverDashboard() {
        if (loggedInDriver == null) return;

        Van assignedVan = null;
        for (Van v : vans) {
            if (v.getDriver().getId().equals(loggedInDriver.getId())) {
                assignedVan = v;
                break;
            }
        }

        if (assignedVan != null) {
            lblDriverVanName.setText("Assigned Vehicle: " + assignedVan.getModel());
            lblDriverPlate.setText("Plate Number: " + assignedVan.getPlateNumber() + "  |  License: " + loggedInDriver.getLicenseNo());
        } else {
            lblDriverVanName.setText("Assigned Vehicle: None Assigned");
            lblDriverPlate.setText("Contact Fleet Management to assign a vehicle.");
        }

        driverTripsContainer.removeAll();

        int count = 0;
        for (Schedule s : schedules) {
            if (assignedVan != null && s.getVan().getId().equals(assignedVan.getId())) {
                count++;
                JPanel card = createClarityCard();
                card.setLayout(new BorderLayout(15, 0));

                JPanel info = new JPanel();
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                info.setOpaque(false);

                JLabel lblDest = new JLabel("TRIP #" + s.getId() + " — " + s.getDestination());
                lblDest.setFont(VanGoTheme.h3());
                lblDest.setForeground(BRAND_NAVY);

                JLabel lblTime = new JLabel(String.format("Departure: %s  |  Booked: %d / %d seats  |  Remaining Empty: %d",
                        s.getDepartureTime(), s.getBookedCount(), s.getCapacity(), s.getRemainingSeats()));
                lblTime.setFont(VanGoTheme.body());
                lblTime.setForeground(TEXT_MUTED);

                String statusLabelText = "Status: " + s.getState().name();
                if (!s.getRejectionReason().isEmpty()) {
                    statusLabelText += " (" + s.getRejectionReason() + ")";
                }
                JLabel lblStatus = new JLabel(statusLabelText);
                lblStatus.setFont(VanGoTheme.font(12, Font.BOLD));
                lblStatus.setForeground(s.isDispatched() ? SEMANTIC_GO : (s.getDriverDepartureRequested() ? SEMANTIC_AMBER : BRAND_NAVY));

                info.add(lblDest);
                info.add(Box.createRigidArea(new Dimension(0, 4)));
                info.add(lblTime);
                info.add(Box.createRigidArea(new Dimension(0, 4)));
                info.add(lblStatus);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                btnPanel.setOpaque(false);

                ClarityButton btnEarlyReq = new ClarityButton("Request Early Departure", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
                btnEarlyReq.setPreferredSize(new Dimension(190, 42));

                boolean canRequest = !s.isDispatched() && !s.getDriverDepartureRequested() && !s.isFull();
                btnEarlyReq.setEnabled(canRequest);

                if (s.isDispatched()) {
                    btnEarlyReq.setText("DISPATCHED");
                } else if (s.isFull()) {
                    btnEarlyReq.setText("FULL (AUTO-ELIGIBLE)");
                } else if (s.getDriverDepartureRequested()) {
                    btnEarlyReq.setText("PENDING ADMIN APPROVAL");
                }

                btnEarlyReq.addActionListener(e -> handleDriverEarlyDepartureRequest(s));

                btnPanel.add(btnEarlyReq);

                card.add(info, BorderLayout.CENTER);
                card.add(btnPanel, BorderLayout.EAST);

                driverTripsContainer.add(card);
            }
        }

        if (count == 0) {
            JPanel emptyCard = createClarityCard();
            JLabel lblEmpty = new JLabel("No active departure schedules assigned for your vehicle today.");
            lblEmpty.setFont(VanGoTheme.body());
            lblEmpty.setForeground(TEXT_MUTED);
            emptyCard.add(lblEmpty);
            driverTripsContainer.add(emptyCard);
        }

        driverTripsContainer.revalidate();
        driverTripsContainer.repaint();
    }

    private void handleDriverEarlyDepartureRequest(Schedule schedule) {
        String dialogMessage = String.format(
                "You are about to request early departure for trip %s to %s with only %d/%d seats booked.\n" +
                "%d seats will depart empty.\n\n" +
                "This action will be logged and requires admin approval. Continue?",
                schedule.getId(),
                schedule.getDestination(),
                schedule.getBookedCount(),
                schedule.getCapacity(),
                schedule.getRemainingSeats()
        );

        int choice = JOptionPane.showConfirmDialog(
                this,
                dialogMessage,
                "CONFIRM EARLY DEPARTURE OVERRIDE REQUEST",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            schedule.setDriverDepartureRequested(true);
            schedule.setRejectionReason("");
            JOptionPane.showMessageDialog(
                    this,
                    "Early departure request submitted!\nStatus updated to: AWAITING DRIVER DEPARTURE APPROVAL.\n\nPlease instruct terminal admin to approve dispatch.",
                    "REQUEST SUBMITTED",
                    JOptionPane.INFORMATION_MESSAGE
            );
            renderDriverDashboard();
        }
    }

    private void updateDriverVanStatus(String statusText, Color color) {
        lblDriverStatus.setText("● " + statusText);
        lblDriverStatus.setForeground(color);
        JOptionPane.showMessageDialog(this, "Van status updated: " + statusText, "STATUS UPDATED", JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(VanGoTheme.label());
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JTable createClarityTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(VanGoTheme.ICE);
                    c.setForeground(VanGoTheme.NAVY);
                } else if (row % 2 == 0) {
                    c.setBackground(VanGoTheme.SURFACE);
                    c.setForeground(VanGoTheme.TEXT);
                } else {
                    c.setBackground(new Color(0xFA, 0xFB, 0xFD));
                    c.setForeground(VanGoTheme.TEXT);
                }
                return c;
            }
        };
        table.setRowHeight(38);
        table.setFont(VanGoTheme.body());
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(VanGoTheme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(VanGoTheme.ICE);

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setBackground(VanGoTheme.SURFACE);
        header.setForeground(VanGoTheme.TEXT_SECOND);
        header.setFont(VanGoTheme.font(11, Font.BOLD));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, VanGoTheme.BORDER));
        ((javax.swing.table.DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(JLabel.LEFT);

        return table;
    }

    private Schedule findScheduleById(String id) {
        for (Schedule s : schedules) {
            if (s.getId().equalsIgnoreCase(id)) return s;
        }
        return null;
    }

    private void refreshAdminTables() {
        modelSchedules.setRowCount(0);
        for (Schedule s : schedules) {
            modelSchedules.addRow(new Object[]{
                    s.getId(), s.getDestination(), s.getDepartureTime(), String.format("%.2f", s.getFare()),
                    s.getVan().getModel() + " [" + s.getVan().getPlateNumber() + "]", s.getVan().getDriver().getName()
            });
        }

        modelVans.setRowCount(0);
        cbScheduleVan.removeAllItems();
        for (Van v : vans) {
            modelVans.addRow(new Object[]{v.getId(), v.getModel(), v.getPlateNumber(), v.getCapacity(), v.getDriver().getName()});
            cbScheduleVan.addItem(v);
        }

        modelDrivers.setRowCount(0);
        cbVanDriver.removeAllItems();
        for (Driver d : drivers) {
            modelDrivers.addRow(new Object[]{d.getId(), d.getName(), d.getLicenseNo(), d.getPhone()});
            cbVanDriver.addItem(d);
        }

        modelDispatch.setRowCount(0);
        for (Schedule s : schedules) {
            String status;
            if (s.isDispatched())                     status = "DISPATCHED";
            else if (s.isFull())                      status = "FULL · READY";
            else if (s.getDriverDepartureRequested()) status = "AWAITING APPROVAL";
            else                                      status = "BOARDING";

            modelDispatch.addRow(new Object[]{
                    s.getId(),
                    s.getDestination(),
                    s.getDepartureTime(),
                    s.getBookedCount() + " / " + s.getCapacity(),
                    s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")",
                    s.getVan().getDriver().getName(),
                    status
            });
        }

        modelQueue.setRowCount(0);
        for (Map.Entry<String, Deque<Van>> entry : driverQueue.getAllQueues().entrySet()) {
            String dest = entry.getKey();
            List<Van> queueList = driverQueue.getQueue(dest);
            for (int i = 0; i < queueList.size(); i++) {
                Van v = queueList.get(i);
                int pos = i + 1;

                String assignedTrip = "Unassigned";
                for (Schedule s : schedules) {
                    if (!s.isDispatched() && s.getVan().getId().equals(v.getId())) {
                        assignedTrip = s.getId() + " (" + s.getDestination() + ")";
                        break;
                    }
                }

                modelQueue.addRow(new Object[]{
                        dest,
                        "#" + pos + (pos == 1 ? " (FRONT)" : ""),
                        v.getModel(),
                        v.getPlateNumber(),
                        v.getDriver().getName(),
                        v.getTripsCompletedToday(),
                        assignedTrip
                });
            }
        }

        refreshKpiCards();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            VanGoTheme.applyGlobalDefaults();
            new VanGoKiosk().setVisible(true);
        });
    }
}