import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    public static final String FILL_AND_DEPART_MODE = "Fill-and-Depart (No Fixed Time)";
    private static final double MIN_EARLY_LOAD_RATIO = 0.70;

    private List<Driver> drivers;
    private List<Van> vans;
    private List<Schedule> schedules;
    private Schedule selectedSchedule;
    private final List<Integer> currentSelectedSeats = new ArrayList<>();
    private final Map<String, Booking> bookingDatabase = new HashMap<>();

    private final DriverQueue driverQueue = new DriverQueue();
    private final Map<String, String> queueDates = new HashMap<>();

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JPanel scheduleListSidebar;
    private RegionalMapCanvas regionalMapCanvas;
    private VanBackgroundPanel vanBackgroundCanvas;
    private JLabel lblSelectedSeatsText, lblTotalFareText;
    private JLabel lblBreadcrumb;

    private JLabel lblSumRef, lblSumDest, lblSumTime, lblSumVan, lblSumDriver, lblSumSeats, lblSumTotal;
    private JTextField txtPassengerName, txtPassengerContact;

    private DefaultTableModel modelSchedules, modelVans, modelDrivers, modelQueue, modelDispatch, modelDispatchLog;
    private JComboBox<String> cbAdminQueueRouteFilter;

    private DefaultTableModel modelDriverRouteQueue;
    private JComboBox<Driver> cbActiveDriverLogin;
    private JLabel lblDriverVanName, lblDriverPlate, lblDriverQueueStatus, lblRouteQueueHeader;
    private JComboBox<String> cbDriverQueueDest;
    private JPanel driverTripsContainer;
    private Driver loggedInDriver;

    private JPanel segmentedTabBar;
    private CardLayout tabContentLayout;
    private JPanel tabContentPanel;
    private final Map<String, ClarityButton> tabButtons = new LinkedHashMap<>();
    private JLabel kpiValueReady, kpiValuePending, kpiValueBoarding, kpiValueUtil, kpiValueTrips;

    private String activeMapDestination = "Maasin City";
    private String selectedWaypoint = "Baybay City";

    public VanGoKiosk() {
        initData();
        initUI();
    }

    private String qKey(String dest, Van v) {
        return dest + "|" + v.getId();
    }

    private String getQueueDate(String dest, Van v) {
        return queueDates.computeIfAbsent(qKey(dest, v),
                k -> LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private void setQueueDate(String dest, Van v, String dateStr) {
        queueDates.put(qKey(dest, v), dateStr);
    }

    private void removeQueueDate(String dest, Van v) {
        queueDates.remove(qKey(dest, v));
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

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        Schedule s1 = new Schedule("SCH001", "Tacloban City", FILL_AND_DEPART_MODE, 250.00, v1);
        Schedule s2 = new Schedule("SCH002", "Maasin City",   FILL_AND_DEPART_MODE, 350.00, v2);
        Schedule s3 = new Schedule("SCH003", "Baybay City",   FILL_AND_DEPART_MODE, 180.00, v3);
        Schedule s4 = new Schedule("SCH004", "Tacloban City", FILL_AND_DEPART_MODE, 250.00, v4);

        schedules.add(s1);
        schedules.add(s2);
        schedules.add(s3);
        schedules.add(s4);

        for (Schedule s : schedules) {
            driverQueue.enqueueIfAbsent(s.getDestination(), s.getVan());
            setQueueDate(s.getDestination(), s.getVan(), today);
        }

        s1.bookSeat(1); s1.bookSeat(2); s1.bookSeat(3);
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

        // Pre-initialize JavaFX toolkit to prevent blank map issue
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }

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
            "<html><div style='text-align:center; width:540px;'>Board the #1 queued van on your route — no fixed waiting clock; vans depart as soon as full.</div></html>");
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

        JLabel lblStatus = new JLabel("● TERMINAL KIOSK 01 ONLINE  •  FILL-AND-DEPART FCFS QUEUE ACTIVE");
        lblStatus.setFont(VanGoTheme.font(11, Font.BOLD));
        lblStatus.setForeground(SEMANTIC_GO);
        footer.add(lblStatus);

        root.add(topBar, BorderLayout.NORTH);
        root.add(centerWrapper, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private JPanel buildScheduleView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);

        root.add(createStatusHeader("Step 1 of 3", "Select Destination & Live GPS Route Map", true, true), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(20, 0));
        mainContent.setBorder(new EmptyBorder(20, 25, 20, 25));
        mainContent.setOpaque(false);

        scheduleListSidebar = new JPanel();
        scheduleListSidebar.setLayout(new BoxLayout(scheduleListSidebar, BoxLayout.Y_AXIS));
        scheduleListSidebar.setOpaque(false);
        scheduleListSidebar.setBorder(new EmptyBorder(0, 0, 0, 8));

        JScrollPane sidebarScroll = new JScrollPane(scheduleListSidebar);
        sidebarScroll.setPreferredSize(new Dimension(460, 0));
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setBorder(null);
        sidebarScroll.getViewport().setBackground(BG_CANVAS);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(14);

        JPanel mapWrapper = createClarityCard();
        mapWrapper.setLayout(new BorderLayout(0, 10));

        JPanel mapHeader = new JPanel();
        mapHeader.setLayout(new BoxLayout(mapHeader, BoxLayout.Y_AXIS));
        mapHeader.setOpaque(false);

        JLabel lblMapTitle = new JLabel("Live GPS Route Map — Leyte Region");
        lblMapTitle.setFont(VanGoTheme.h3());
        lblMapTitle.setForeground(BRAND_NAVY);
        lblMapTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblBreadcrumb = new JLabel(" ");
        lblBreadcrumb.setFont(VanGoTheme.font(12, Font.BOLD));
        lblBreadcrumb.setForeground(BRAND_ORANGE);
        lblBreadcrumb.setAlignmentX(Component.LEFT_ALIGNMENT);

        mapHeader.add(lblMapTitle);
        mapHeader.add(Box.createRigidArea(new Dimension(0, 4)));
        mapHeader.add(lblBreadcrumb);

        regionalMapCanvas = new RegionalMapCanvas();
        // Ensures the map panel fills the available space
        regionalMapCanvas.setPreferredSize(new Dimension(800, 600)); 

        mapWrapper.add(mapHeader, BorderLayout.NORTH);
        mapWrapper.add(regionalMapCanvas, BorderLayout.CENTER);

        mainContent.add(sidebarScroll, BorderLayout.WEST);
        mainContent.add(mapWrapper, BorderLayout.CENTER);

        root.add(mainContent, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            if (lblBreadcrumb != null) {
                lblBreadcrumb.setText(RouteCorridor.breadcrumb(activeMapDestination));
            }
            if (regionalMapCanvas != null) {
                regionalMapCanvas.setTargetRoute(activeMapDestination, selectedWaypoint);
            }
        });
        return root;
    }

    private void renderScheduleCards() {
        scheduleListSidebar.removeAll();

        List<Schedule> visible = new ArrayList<>();
        for (Schedule s : schedules) {
            if (!s.isDispatched()) visible.add(s);
        }

        visible.sort((a, b) -> {
            int destCmp = a.getDestination().compareToIgnoreCase(b.getDestination());
            if (destCmp != 0) return destCmp;
            int posA = driverQueue.positionOf(a.getDestination(), a.getVan());
            int posB = driverQueue.positionOf(b.getDestination(), b.getVan());
            if (posA <= 0) posA = 999;
            if (posB <= 0) posB = 999;
            return Integer.compare(posA, posB);
        });

        if (visible.isEmpty()) {
            JLabel empty = new JLabel("No vans queued for boarding right now. Please check back shortly.");
            empty.setFont(VanGoTheme.body());
            empty.setForeground(TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            scheduleListSidebar.add(Box.createRigidArea(new Dimension(0, 20)));
            scheduleListSidebar.add(empty);
            scheduleListSidebar.revalidate();
            scheduleListSidebar.repaint();
            return;
        }

        for (Schedule s : visible) {
            int queuePos = driverQueue.positionOf(s.getDestination(), s.getVan());
            if (queuePos <= 0) {
                driverQueue.enqueueIfAbsent(s.getDestination(), s.getVan());
                queuePos = driverQueue.positionOf(s.getDestination(), s.getVan());
            }
            int queueTotal = driverQueue.size(s.getDestination());
            boolean isQueueLeader = (queuePos == 1);

            JPanel card = new JPanel(new BorderLayout(0, 10)) {
                @Override
                public Dimension getMaximumSize() {
                    Dimension pref = getPreferredSize();
                    return new Dimension(Integer.MAX_VALUE, pref.height);
                }
            };
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setBackground(isQueueLeader ? CARD_SURFACE : VanGoTheme.SURFACE_ALT);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(activeMapDestination.equals(s.getDestination()) && isQueueLeader ? BRAND_ORANGE : BORDER_NEUTRAL,
                            activeMapDestination.equals(s.getDestination()) && isQueueLeader ? 2 : 1, true),
                    new EmptyBorder(14, 16, 14, 16)
            ));

            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);

            String queueTag = isQueueLeader
                    ? "  [QUEUE #1 · BOARDING]"
                    : "  [QUEUE #" + queuePos + " OF " + queueTotal + " · STANDBY]";
            JLabel lblDest = new JLabel(s.getDestination() + queueTag);
            lblDest.setFont(VanGoTheme.font(14, Font.BOLD));
            lblDest.setForeground(isQueueLeader ? BRAND_NAVY : TEXT_MUTED);

            JLabel lblFare = new JLabel("₱" + String.format("%.2f", s.getFare()));
            lblFare.setFont(VanGoTheme.h3());
            lblFare.setForeground(isQueueLeader ? BRAND_ORANGE : TEXT_MUTED);

            head.add(lblDest, BorderLayout.WEST);
            head.add(lblFare, BorderLayout.EAST);

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            createClarityDetailRow(body, "Queue Date", getQueueDate(s.getDestination(), s.getVan()));
            createClarityDetailRow(body, "Dispatch Rule", s.getDepartureTime());
            createClarityDetailRow(body, "Vehicle", s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")");
            createClarityDetailRow(body, "Driver", s.getVan().getDriver().getName());
            createClarityDetailRow(body, "Passenger Load", s.getBookedCount() + " / " + s.getCapacity() + " seats");

            String bc = RouteCorridor.breadcrumb(s.getChosenRoute());
            JLabel lblRoute = new JLabel("<html><div style='width:320px; color:#FF6B00; font-size:11px;'>" +
                    "Corridor: " + bc + "</div></html>");
            lblRoute.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(Box.createRigidArea(new Dimension(0, 4)));
            body.add(lblRoute);
            body.add(Box.createRigidArea(new Dimension(0, 8)));

            JPanel waypointPanel = new JPanel(new BorderLayout(8, 0));
            waypointPanel.setOpaque(false);
            waypointPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            waypointPanel.setPreferredSize(new Dimension(0, 32));
            waypointPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            JLabel lblWp = new JLabel("Highlight Stop:");
            lblWp.setFont(VanGoTheme.font(11, Font.BOLD));
            lblWp.setForeground(TEXT_MUTED);

            List<String> corridor = s.getChosenRoute();
            JComboBox<String> cbWaypoints = new JComboBox<>(corridor.toArray(new String[0]));
            cbWaypoints.setFont(VanGoTheme.font(11, Font.PLAIN));
            cbWaypoints.setSelectedItem(corridor.contains(selectedWaypoint) ? selectedWaypoint : corridor.get(0));
            cbWaypoints.addActionListener(e -> {
                selectedWaypoint = (String) cbWaypoints.getSelectedItem();
                activeMapDestination = s.getDestination();
                regionalMapCanvas.setTargetRoute(activeMapDestination, s.getChosenRoute());
            });

            waypointPanel.add(lblWp, BorderLayout.WEST);
            waypointPanel.add(cbWaypoints, BorderLayout.CENTER);
            body.add(waypointPanel);
            body.add(Box.createRigidArea(new Dimension(0, 6)));

            ClarityButton btn;
            if (!isQueueLeader) {
                btn = new ClarityButton("STANDBY (#" + queuePos + ") — WAITING FOR #1 TO FILL", TEXT_MUTED, Color.WHITE, BORDER_NEUTRAL, 11);
                btn.setEnabled(false);
            } else if (s.isFull()) {
                btn = new ClarityButton("FULL (100%) — READY FOR IMMEDIATE DISPATCH", SEMANTIC_AMBER, Color.WHITE, SEMANTIC_AMBER, 11);
                btn.setEnabled(false);
            } else {
                btn = new ClarityButton("Select This Trip (Boarding #1)  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
                btn.addActionListener(e -> {
                    activeMapDestination = s.getDestination();
                    regionalMapCanvas.setTargetRoute(activeMapDestination, s.getChosenRoute());
                    if (lblBreadcrumb != null) lblBreadcrumb.setText(RouteCorridor.breadcrumb(s.getChosenRoute()));
                    renderScheduleCards();

                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "Trip: Ormoc → " + activeMapDestination + " (Queue #1 Boarding)\n" +
                            "Vehicle: " + s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")\n" +
                            "Corridor: " + RouteCorridor.breadcrumb(s.getChosenRoute()) + "\n\n" +
                            "Proceed to seat selection?",
                            "CONFIRM TRIP",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        selectedSchedule = s;
                        renderSeatGrid();
                        cardLayout.show(mainContainer, "SEATS");
                    }
                });
            }
            btn.setPreferredSize(new Dimension(0, 38));

            card.add(head, BorderLayout.NORTH);
            card.add(body, BorderLayout.CENTER);
            card.add(btn, BorderLayout.SOUTH);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    activeMapDestination = s.getDestination();
                    regionalMapCanvas.setTargetRoute(activeMapDestination, s.getChosenRoute());
                    if (lblBreadcrumb != null) lblBreadcrumb.setText(RouteCorridor.breadcrumb(s.getChosenRoute()));
                    renderScheduleCards();
                }
            });

            scheduleListSidebar.add(card);
            scheduleListSidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        scheduleListSidebar.revalidate();
        scheduleListSidebar.repaint();
    }

    private class RegionalMapCanvas extends JFXPanel {
        private WebEngine webEngine;
        private JSObject bridge;
        private volatile boolean isInitialized = false;
        private volatile boolean fxReady = false;
        private String pendingDest = "Maasin City";
        private String pendingWaypoint = "Baybay City";
        private List<String> pendingCustomRoute = null;

        public RegionalMapCanvas() {
            super();
            // Set a default background so it's not a blank white box while loading
            setBackground(CARD_SURFACE); 
            
            Platform.runLater(() -> {
                try {
                    WebView webView = new WebView();
                    webView.setContextMenuEnabled(false);
                    webEngine = webView.getEngine();

                    webEngine.setOnError(event -> 
                        System.err.println("[Map] WebEngine Error: " + event.getMessage()));

                    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                        System.out.println("[Map] Load Status: " + newState);
                        if (newState == Worker.State.SUCCEEDED) {
                            try {
                                bridge = (JSObject) webEngine.executeScript("window");
                                bridge.setMember("javaBridge", new JavaBridge());
                                isInitialized = true;
                                
                                // Sync initial route if pending
                                if (pendingCustomRoute != null) {
                                    applyCustomRoute(pendingCustomRoute);
                                } else if (pendingDest != null) {
                                    applyRoute(pendingDest, pendingWaypoint);
                                }
                                System.out.println("[Map] Successfully initialized and bridge injected.");
                            } catch (Exception ex) {
                                System.err.println("[Map] Bridge injection failed.");
                                ex.printStackTrace();
                            }
                        } else if (newState == Worker.State.FAILED) {
                            System.err.println("[Map] Failed to load map2.html!");
                            webEngine.loadContent("<html><body style='font-family:sans-serif;padding:20px;'><h2 style='color:#dc2626;'>Map Failed to Load</h2><p>Could not find <b>map2.html</b>. Make sure it is in the root of your classpath (src folder).</p></body></html>");
                        }
                    });

                    // Robust loading fallback mechanism
                    java.net.URL url = VanGoKiosk.class.getResource("/map2.html");
                    if (url == null) url = VanGoKiosk.class.getResource("map2.html");
                    
                    if (url != null) {
                        System.out.println("[Map] Loading from Classpath: " + url.toExternalForm());
                        webEngine.load(url.toExternalForm());
                    } else {
                        File f = new File("map2.html");
                        System.out.println("[Map] Classpath failed. Loading from File: " + f.getAbsolutePath());
                        webEngine.load(f.toURI().toString());
                    }

                    setScene(new Scene(webView));
                    fxReady = true;
                } catch (Throwable t) {
                    System.err.println("[Map] Fatal JavaFX initialization error!");
                    t.printStackTrace();
                }
            });
        }

        public class JavaBridge {
            public void onDestinationPicked(String name) {
                SwingUtilities.invokeLater(() -> {
                    activeMapDestination = name;
                    if (lblBreadcrumb != null) lblBreadcrumb.setText(RouteCorridor.breadcrumb(name));
                    renderScheduleCards();
                    setTargetRoute(name, selectedWaypoint);
                });
            }
        }

        public void setTargetRoute(String dest, String waypoint) {
            pendingCustomRoute = null;
            pendingDest = dest;
            pendingWaypoint = waypoint;
            if (isInitialized && fxReady) {
                applyRoute(dest, waypoint);
            }
        }

        public void setTargetRoute(String dest, List<String> customRoute) {
            pendingDest = dest;
            pendingCustomRoute = (customRoute != null) ? new ArrayList<>(customRoute) : null;
            if (isInitialized && fxReady) {
                applyCustomRoute(customRoute);
            }
        }

        private void applyRoute(String dest, String waypoint) {
            Platform.runLater(() -> {
                if (webEngine == null || !isInitialized) return;
                String js = "updateRoute('" + jsEscape(dest) + "','" + jsEscape(waypoint) + "');";
                try { webEngine.executeScript(js); }
                catch (Exception e) { e.printStackTrace(); }
            });
        }

        private void applyCustomRoute(List<String> route) {
            Platform.runLater(() -> {
                if (webEngine == null || route == null || !isInitialized) return;
                StringBuilder sb = new StringBuilder("previewCustomRoute([");
                for (int i = 0; i < route.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("'").append(jsEscape(route.get(i))).append("'");
                }
                sb.append("]);");
                try { webEngine.executeScript(sb.toString()); }
                catch (Exception e) { e.printStackTrace(); }
            });
        }

        private String jsEscape(String s) {
            return (s == null) ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
        }
    }

    private JLabel createClarityDetailRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(VanGoTheme.font(12, Font.PLAIN));
        lblKey.setForeground(BRAND_NAVY);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(VanGoTheme.font(13, Font.BOLD));
        lblVal.setForeground(TEXT_PRIMARY);

        row.add(lblKey, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);

        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 6)));
        return lblVal;
    }

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
        lblSumTime  = createClarityDetailRow(summaryBody, "Dispatch Rule", "-");
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
                "Present your booking reference code at the payment counter. Once all seats are filled, this van departs immediately.</body></html>");
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
        lblSumDest.setText(selectedSchedule.getDestination() +
                " (" + RouteCorridor.breadcrumb(selectedSchedule.getChosenRoute()) + ")");
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

        boolean justFilled = selectedSchedule.isFull();
        String fullNotice = justFilled
                ? "\n\nNOTICE: This van is now FULL (" + selectedSchedule.getBookedCount() + "/" + selectedSchedule.getCapacity() +
                  ") and cleared for IMMEDIATE DISPATCH so Queue #2 can board next."
                : "";

        JOptionPane.showMessageDialog(this,
                "BOOKING SUBMITTED SUCCESSFULLY!\n\n" +
                "Reference Number: " + generatedReferenceNumber + "\n" +
                "Passenger: " + name + "\n" +
                "Total Due: ₱" + String.format("%.2f", total) + fullNotice,
                "CONFIRMATION PASS GENERATED", JOptionPane.INFORMATION_MESSAGE);

        txtPassengerName.setText("");
        txtPassengerContact.setText("");
        selectedSchedule = null;
        currentSelectedSeats.clear();
        refreshAdminTables();
        renderScheduleCards();
        cardLayout.show(mainContainer, "WELCOME");
    }

    private JPanel buildStaffGatewayView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);
        root.add(createStatusHeader("Authentication Gateway", "Secure Staff Portal Access", false, true), BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel contentBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        contentBox.setOpaque(false);

        JPanel cardAdmin = createClarityCard();
        cardAdmin.setPreferredSize(new Dimension(380, 410));
        cardAdmin.setLayout(new BorderLayout(0, 15));

        JPanel adminHeader = new JPanel();
        adminHeader.setLayout(new BoxLayout(adminHeader, BoxLayout.Y_AXIS));
        adminHeader.setOpaque(false);

        JLabel lblAdminIcon = new JLabel("ADMIN", SwingConstants.CENTER);
        lblAdminIcon.setFont(VanGoTheme.font(14, Font.BOLD));
        lblAdminIcon.setForeground(BRAND_ORANGE);
        lblAdminIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAdminTitle = new JLabel("Administrator Portal");
        lblAdminTitle.setFont(VanGoTheme.h2());
        lblAdminTitle.setForeground(BRAND_NAVY);
        lblAdminTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAdminDesc = new JLabel("<html><center style='color:#475569;'>Inspect route masterlists, edit queue dates, forfeit no-show drivers, and dispatch full vans.</center></html>");
        lblAdminDesc.setFont(VanGoTheme.font(13, Font.PLAIN));
        lblAdminDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        adminHeader.add(lblAdminIcon);
        adminHeader.add(Box.createRigidArea(new Dimension(0, 8)));
        adminHeader.add(lblAdminTitle);
        adminHeader.add(Box.createRigidArea(new Dimension(0, 6)));
        adminHeader.add(lblAdminDesc);

        JPanel adminForm = new JPanel();
        adminForm.setLayout(new BoxLayout(adminForm, BoxLayout.Y_AXIS));
        adminForm.setOpaque(false);

        JLabel lblPassAdmin = new JLabel("Admin Passcode:");
        lblPassAdmin.setFont(VanGoTheme.label());
        lblPassAdmin.setForeground(TEXT_MUTED);
        lblPassAdmin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField txtAdminPass = new JPasswordField();
        txtAdminPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtAdminPass.setFont(VanGoTheme.body());
        txtAdminPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        adminForm.add(lblPassAdmin);
        adminForm.add(Box.createRigidArea(new Dimension(0, 4)));
        adminForm.add(txtAdminPass);

        ClarityButton btnLoginAdmin = new ClarityButton("Access Admin Dashboard  →", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 14);
        btnLoginAdmin.setPreferredSize(new Dimension(0, 46));
        btnLoginAdmin.addActionListener(e -> {
            String pass = new String(txtAdminPass.getPassword());
            if ("admin123".equals(pass) || pass.isEmpty()) {
                txtAdminPass.setText("");
                refreshAdminTables();
                cardLayout.show(mainContainer, "ADMIN_DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect administrator passcode. Try 'admin123' or leave blank for demo.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel adminCenter = new JPanel(new BorderLayout());
        adminCenter.setOpaque(false);
        adminCenter.add(adminHeader, BorderLayout.NORTH);
        adminCenter.add(adminForm, BorderLayout.CENTER);

        cardAdmin.add(adminCenter, BorderLayout.CENTER);
        cardAdmin.add(btnLoginAdmin, BorderLayout.SOUTH);

        JPanel cardDriver = createClarityCard();
        cardDriver.setPreferredSize(new Dimension(380, 410));
        cardDriver.setLayout(new BorderLayout(0, 15));

        JPanel driverHeader = new JPanel();
        driverHeader.setLayout(new BoxLayout(driverHeader, BoxLayout.Y_AXIS));
        driverHeader.setOpaque(false);

        JLabel lblDriverIcon = new JLabel("DRIVER", SwingConstants.CENTER);
        lblDriverIcon.setFont(VanGoTheme.font(14, Font.BOLD));
        lblDriverIcon.setForeground(BRAND_ORANGE);
        lblDriverIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDriverTitle = new JLabel("Driver Console Login");
        lblDriverTitle.setFont(VanGoTheme.h2());
        lblDriverTitle.setForeground(BRAND_NAVY);
        lblDriverTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDriverDesc = new JLabel("<html><center style='color:#475569;'>Line up FCFS on any route without fixed departure clocks, view route masterlists, and request early dispatch.</center></html>");
        lblDriverDesc.setFont(VanGoTheme.font(13, Font.PLAIN));
        lblDriverDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        driverHeader.add(lblDriverIcon);
        driverHeader.add(Box.createRigidArea(new Dimension(0, 8)));
        driverHeader.add(lblDriverTitle);
        driverHeader.add(Box.createRigidArea(new Dimension(0, 6)));
        driverHeader.add(lblDriverDesc);

        JPanel driverForm = new JPanel();
        driverForm.setLayout(new BoxLayout(driverForm, BoxLayout.Y_AXIS));
        driverForm.setOpaque(false);

        JLabel lblPassDriver = new JLabel("Driver PIN / Passcode:");
        lblPassDriver.setFont(VanGoTheme.label());
        lblPassDriver.setForeground(TEXT_MUTED);
        lblPassDriver.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField txtDriverPass = new JPasswordField();
        txtDriverPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtDriverPass.setFont(VanGoTheme.body());
        txtDriverPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        driverForm.add(lblPassDriver);
        driverForm.add(Box.createRigidArea(new Dimension(0, 4)));
        driverForm.add(txtDriverPass);

        ClarityButton btnLoginDriver = new ClarityButton("Access Driver Console  →", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 14);
        btnLoginDriver.setPreferredSize(new Dimension(0, 46));
        btnLoginDriver.addActionListener(e -> {
            String pass = new String(txtDriverPass.getPassword());
            if ("driver123".equals(pass) || pass.isEmpty()) {
                txtDriverPass.setText("");
                openDriverPortalLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect driver passcode. Try 'driver123' or leave blank for demo.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel driverCenter = new JPanel(new BorderLayout());
        driverCenter.setOpaque(false);
        driverCenter.add(driverHeader, BorderLayout.NORTH);
        driverCenter.add(driverForm, BorderLayout.CENTER);

        cardDriver.add(driverCenter, BorderLayout.CENTER);
        cardDriver.add(btnLoginDriver, BorderLayout.SOUTH);

        contentBox.add(cardAdmin);
        contentBox.add(cardDriver);

        centerWrapper.add(contentBox);
        root.add(centerWrapper, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildAdminDashboardView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);
        root.add(createStatusHeader("Administrator Workspace", "Terminal Counter & Operations Control", false, false), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(new EmptyBorder(16, 28, 16, 28));
        body.setOpaque(false);

        JPanel kpiStrip = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiStrip.setOpaque(false);
        kpiStrip.setPreferredSize(new Dimension(0, 92));
        kpiStrip.add(createStatCard("Trips Today", "0", "queued & active", BRAND_NAVY));
        kpiStrip.add(createStatCard("Ready to Dispatch", "0", "full or approved", SEMANTIC_AMBER));
        kpiStrip.add(createStatCard("Pending Approval", "0", "early-departure requests", SEMANTIC_RED));
        kpiStrip.add(createStatCard("Boarding", "0", "#1 vans accepting load", SEMANTIC_GO));
        kpiStrip.add(createStatCard("Fleet Utilization", "0%", "seats booked today", BRAND_ORANGE));
        body.add(kpiStrip, BorderLayout.NORTH);

        segmentedTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        segmentedTabBar.setOpaque(false);
        segmentedTabBar.setBorder(new EmptyBorder(4, 0, 4, 0));

        tabContentLayout = new CardLayout();
        tabContentPanel = new JPanel(tabContentLayout);
        tabContentPanel.setOpaque(false);

        tabContentPanel.add(buildDispatchAdminTab(),    "DISPATCH");
        tabContentPanel.add(buildDriverQueueAdminTab(), "QUEUE");
        tabContentPanel.add(buildDispatchLogTab(),      "LOG");
        tabContentPanel.add(buildCounterTab(),          "COUNTER");
        tabContentPanel.add(buildSchedulesAdminTab(),   "SCHEDULES");
        tabContentPanel.add(buildVansAdminTab(),        "VANS");
        tabContentPanel.add(buildDriversAdminTab(),     "DRIVERS");

        addSegmentedTab("DISPATCH",  "Dispatch Hub");
        addSegmentedTab("QUEUE",     "Route Masterlist & Queue");
        addSegmentedTab("LOG",       "Dispatched Log");
        addSegmentedTab("COUNTER",   "Payment Counter");
        addSegmentedTab("SCHEDULES", "Trip Schedules");
        addSegmentedTab("VANS",      "Fleet Vans");
        addSegmentedTab("DRIVERS",   "Driver Registry");

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(segmentedTabBar, BorderLayout.NORTH);

        JPanel tabFrame = new JPanel(new BorderLayout());
        tabFrame.setBackground(CARD_SURFACE);
        tabFrame.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));
        tabFrame.add(tabContentPanel, BorderLayout.CENTER);
        center.add(tabFrame, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        ClarityButton btnExitAdmin = new ClarityButton("← Logout & Return to Kiosk", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
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
        for (Schedule s : schedules) {
            if (!s.isDispatched() && !s.isFull() && !s.getDriverDepartureRequested()
                    && driverQueue.positionOf(s.getDestination(), s.getVan()) == 1) {
                n++;
            }
        }
        return n;
    }
    private int fleetUtilizationPct() {
        int booked = 0, cap = 0;
        for (Schedule s : schedules) { booked += s.getBookedCount(); cap += s.getCapacity(); }
        return cap == 0 ? 0 : Math.round(booked * 100f / cap);
    }

    private void addSegmentedTab(String key, String label) {
        ClarityButton btn = new ClarityButton(label, CARD_SURFACE, TEXT_MUTED, BORDER_NEUTRAL, 12);
        btn.setPreferredSize(new Dimension(168, 38));
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.addActionListener(e -> {
            refreshAdminTables();
            showTab(key);
        });
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

    private class StatusPillRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            String s = String.valueOf(v);
            Color bg, fg;
            if (s.contains("READY") || s.contains("FULL")) { bg = VanGoTheme.WARN_BG; fg = SEMANTIC_AMBER; }
            else if (s.contains("APPROVAL"))               { bg = VanGoTheme.DANGER_BG; fg = SEMANTIC_RED; }
            else if (s.contains("STANDBY"))                { bg = VanGoTheme.SURFACE_ALT; fg = TEXT_MUTED; }
            else if (s.contains("DISPATCHED"))             { bg = VanGoTheme.SURFACE_ALT; fg = VanGoTheme.TEXT_TERT; }
            else                                           { bg = VanGoTheme.GO_BG; fg = SEMANTIC_GO; }
            return makeStatusPill(shortStatus(s), bg, fg);
        }
        private String shortStatus(String s) {
            if (s.contains("FULL"))       return "● FULL · READY TO DISPATCH";
            if (s.contains("APPROVAL"))   return "● AWAITING APPROVAL";
            if (s.contains("STANDBY"))    return s;
            if (s.contains("DISPATCHED")) return "● DISPATCHED";
            if (s.contains("BOARDING"))   return "● #1 BOARDING NOW";
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

    private JPanel buildDispatchAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"Trip ID", "Destination", "Queue #", "Queue Date", "Occupancy", "Vehicle", "Driver", "Status"};
        modelDispatch = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelDispatch);
        table.setRowHeight(46);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusPillRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel actionCard = createClarityCard();
        actionCard.setLayout(new BoxLayout(actionCard, BoxLayout.Y_AXIS));
        actionCard.setPreferredSize(new Dimension(290, 0));

        JLabel actTitle = new JLabel("Dispatch Actions");
        actTitle.setFont(VanGoTheme.h3());
        actTitle.setForeground(BRAND_NAVY);
        actTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel actHint = new JLabel("<html><div style='width:240px; color:#475569;'>" +
                "Vans depart when <b>100% full</b> (or ≥70% load with approved early departure when fewer vans are queued)." +
                "</div></html>");
        actHint.setFont(VanGoTheme.font(11, Font.PLAIN));
        actHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnApproveDispatch = new ClarityButton("Approve & Dispatch", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
        btnApproveDispatch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnApproveDispatch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnRejectEarly = new ClarityButton("Reject Early Departure", SEMANTIC_RED, Color.WHITE, SEMANTIC_RED, 13);
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
                String reason = JOptionPane.showInputDialog(this, "Enter reason for rejecting early departure:", "Insufficient passengers — wait until van fills up");
                if (reason == null) return;
                if (reason.trim().isEmpty()) reason = "Insufficient passengers — wait until van fills up";

                sch.setDriverDepartureRequested(false);
                sch.setState(Schedule.DispatchState.SCHEDULED);
                sch.setRejectionReason(reason);

                JOptionPane.showMessageDialog(this, "Early departure request rejected.\nVan continues boarding until full.", "REQUEST REJECTED", JOptionPane.INFORMATION_MESSAGE);
                refreshAdminTables();
            } else {
                JOptionPane.showMessageDialog(this, "Selected trip has no pending early departure request.", "INVALID SELECTION", JOptionPane.WARNING_MESSAGE);
            }
        });

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actionCard, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildDispatchLogTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"Trip ID", "Destination", "Dispatch Rule", "Vehicle", "Driver", "Dispatched At", "Corridor"};
        modelDispatchLog = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelDispatchLog);
        table.setRowHeight(40);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void executeDispatchFlow(Schedule schedule) {
        String dest = schedule.getDestination();
        Van frontVan = driverQueue.peek(dest);

        if (frontVan == null) {
            driverQueue.enqueueIfAbsent(dest, schedule.getVan());
            frontVan = driverQueue.peek(dest);
        }

        if (!frontVan.getId().equals(schedule.getVan().getId())) {
            String message = String.format(
                    "FCFS QUEUE ORDER LOCK!\n\n" +
                    "Route: %s\n\n" +
                    "Selected Van: %s (%s) — Queue #%d\n" +
                    "Queue #1 Leader: %s (%s) driven by %s\n\n" +
                    "Queue #1 must depart (or be forfeited if absent) before later vans can depart.",
                    dest,
                    schedule.getVan().getModel(), schedule.getVan().getPlateNumber(), driverQueue.positionOf(dest, schedule.getVan()),
                    frontVan.getModel(), frontVan.getPlateNumber(), frontVan.getDriver().getName()
            );
            JOptionPane.showMessageDialog(this, message, "FCFS QUEUE LOCK", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int booked = schedule.getBookedCount();
        int cap = schedule.getCapacity();
        int minRequired = (int) Math.ceil(cap * MIN_EARLY_LOAD_RATIO);

        if (!schedule.isFull() && !schedule.getDriverDepartureRequested()) {
            JOptionPane.showMessageDialog(this,
                    "CANNOT DISPATCH YET — VAN NOT FULL\n\n" +
                    "Current Load: " + booked + " / " + cap + " passengers.\n" +
                    "Under Fill-and-Depart rules, a van cannot proceed unless it is full (100%),\n" +
                    "or the driver requests Early Departure with at least " + minRequired + " passengers (70% load).",
                    "INSUFFICIENT PASSENGER LOAD", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!schedule.isFull() && booked < minRequired) {
            JOptionPane.showMessageDialog(this,
                    "EARLY DEPARTURE BLOCKED — TOO FEW PASSENGERS\n\n" +
                    "Current Load: " + booked + " / " + cap + " passengers (Minimum required: " + minRequired + " seats / 70%).\n" +
                    "The van cannot proceed on the " + dest + " route with insufficient passengers.",
                    "MINIMUM LOAD NOT MET", JOptionPane.WARNING_MESSAGE);
            return;
        }

        driverQueue.dequeue(dest);
        removeQueueDate(dest, schedule.getVan());
        schedule.setDispatched(true);
        schedule.setDriverDepartureRequested(false);
        schedule.getVan().incrementTripsCompleted();
        schedule.setDispatchedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Van nextInLine = driverQueue.peek(dest);
        String nextMsg = (nextInLine != null)
                ? ("Next Boarding (#1 Now): " + nextInLine.getModel() + " [" + nextInLine.getPlateNumber() + "] — Driver: " + nextInLine.getDriver().getName())
                : ("No standby vans waiting in the " + dest + " queue.");

        JOptionPane.showMessageDialog(this,
                "TRIP DISPATCHED SUCCESSFULLY!\n\n" +
                "Trip ID: " + schedule.getId() + "\n" +
                "Route: " + dest + "\n" +
                "Passenger Load: " + booked + " / " + cap + "\n" +
                "Dispatched Vehicle: " + schedule.getVan().getModel() + " (" + schedule.getVan().getPlateNumber() + ")\n" +
                "Driver: " + schedule.getVan().getDriver().getName() + "\n\n" +
                nextMsg,
                "DISPATCH CONFIRMED", JOptionPane.INFORMATION_MESSAGE);

        refreshAdminTables();
        renderScheduleCards();
    }

    private JPanel buildDriverQueueAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel topFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topFilter.setOpaque(false);

        JLabel lblFilter = new JLabel("Route Masterlist Filter:");
        lblFilter.setFont(VanGoTheme.font(13, Font.BOLD));
        lblFilter.setForeground(BRAND_NAVY);

        List<String> filterItems = new ArrayList<>();
        filterItems.add("All Routes (Masterlist)");
        filterItems.addAll(RouteCorridor.getKnownDestinations());
        cbAdminQueueRouteFilter = new JComboBox<>(filterItems.toArray(new String[0]));
        cbAdminQueueRouteFilter.setFont(VanGoTheme.font(12, Font.PLAIN));
        cbAdminQueueRouteFilter.setPreferredSize(new Dimension(230, 34));
        cbAdminQueueRouteFilter.addActionListener(e -> refreshAdminTables());

        topFilter.add(lblFilter);
        topFilter.add(cbAdminQueueRouteFilter);

        String[] cols = {"Route", "Queue #", "Queue Date", "Van Model", "Plate No", "Driver Name", "Occupancy", "Status"};
        modelQueue = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelQueue);
        table.setRowHeight(42);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        JPanel ctrlCard = createClarityCard();
        ctrlCard.setLayout(new BoxLayout(ctrlCard, BoxLayout.Y_AXIS));
        ctrlCard.setPreferredSize(new Dimension(295, 0));

        JLabel ctrlTitle = new JLabel("Queue Masterlist Controls");
        ctrlTitle.setFont(VanGoTheme.h3());
        ctrlTitle.setForeground(BRAND_NAVY);
        ctrlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ctrlDesc = new JLabel("<html><div style='width:245px; color:#475569;'>" +
                "Select any queued van/driver row to update its <b>Queue Date</b> or <b>Forfeit</b> the slot if the driver is not present." +
                "</div></html>");
        ctrlDesc.setFont(VanGoTheme.font(11, Font.PLAIN));
        ctrlDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnEditDate = new ClarityButton("Edit Queue Date", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 13);
        btnEditDate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnEditDate.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnForfeit = new ClarityButton("Forfeit (Driver Not Present)", SEMANTIC_RED, Color.WHITE, SEMANTIC_RED, 13);
        btnForfeit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnForfeit.setAlignmentX(Component.LEFT_ALIGNMENT);

        ctrlCard.add(ctrlTitle);
        ctrlCard.add(Box.createRigidArea(new Dimension(0, 6)));
        ctrlCard.add(ctrlDesc);
        ctrlCard.add(Box.createRigidArea(new Dimension(0, 22)));
        ctrlCard.add(btnEditDate);
        ctrlCard.add(Box.createRigidArea(new Dimension(0, 10)));
        ctrlCard.add(btnForfeit);

        btnEditDate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a queued van row first.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String route = (String) modelQueue.getValueAt(row, 0);
            String currentDate = (String) modelQueue.getValueAt(row, 2);
            String plate = (String) modelQueue.getValueAt(row, 4);
            Van targetVan = findVanByPlate(plate);
            if (targetVan == null) return;

            String newDate = (String) JOptionPane.showInputDialog(
                    this,
                    "Edit Queue Date for " + targetVan.getModel() + " (" + plate + ") on " + route + " route:\n(Format: YYYY-MM-DD)",
                    "EDIT QUEUE DATE",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    currentDate
            );
            if (newDate != null && !newDate.trim().isEmpty()) {
                setQueueDate(route, targetVan, newDate.trim());
                refreshAdminTables();
                renderScheduleCards();
                JOptionPane.showMessageDialog(this, "Queue date updated to " + newDate.trim() + ".", "QUEUE DATE UPDATED", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnForfeit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a queued van/driver row to forfeit.", "SELECTION REQUIRED", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String route = (String) modelQueue.getValueAt(row, 0);
            String qPosStr = (String) modelQueue.getValueAt(row, 1);
            String plate = (String) modelQueue.getValueAt(row, 4);
            String driverName = (String) modelQueue.getValueAt(row, 5);
            Van targetVan = findVanByPlate(plate);
            if (targetVan == null) return;

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "FORFEIT QUEUE SLOT — DRIVER NOT PRESENT?\n\n" +
                    "Route: " + route + "\n" +
                    "Queue Position: " + qPosStr + "\n" +
                    "Driver: " + driverName + " (" + plate + ")\n\n" +
                    "This will remove the absent driver from the " + route + " queue and immediately promote the next van in line.",
                    "CONFIRM QUEUE FORFEIT",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                forfeitDriverFromRoute(route, targetVan);
            }
        });

        panel.add(topFilter, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(ctrlCard, BorderLayout.EAST);
        return panel;
    }

         private void forfeitDriverFromRoute(String route, Van van) {
        // 1. Remove the trip from the schedules list
        schedules.removeIf(s -> !s.isDispatched()
                && s.getDestination().equalsIgnoreCase(route)
                && s.getVan().getId().equals(van.getId()));

        // 2. Remove from queue (handling both head and non-head positions)
        List<Van> q = driverQueue.getQueue(route);
        Van head = driverQueue.peek(route);
        
        if (head != null && head.getId().equals(van.getId())) {
            driverQueue.dequeue(route);
        } else {
            // If it's not at the head, drain the queue and re-add the remaining ones
            while (driverQueue.dequeue(route) != null) { 
                // drain queue
            }
            for (Van v : q) {
                if (!v.getId().equals(van.getId())) {
                    driverQueue.enqueue(route, v);
                }
            }
        }
        
        removeQueueDate(route, van);

        // 3. Update UI
        Van nextUp = driverQueue.peek(route);
        String promoMsg = (nextUp != null)
                ? ("Promoted to #1 Boarding Now: " + nextUp.getModel() + " (" + nextUp.getPlateNumber() + ") — Driver: " + nextUp.getDriver().getName())
                : ("No other vans currently waiting in the " + route + " queue.");

        refreshAdminTables();
        renderScheduleCards();
        if (loggedInDriver != null) {
            renderDriverDashboard();
        }

        JOptionPane.showMessageDialog(this,
                "QUEUE SLOT FORFEITED (DRIVER ABSENT)\n\n" +
                "Removed: " + van.getDriver().getName() + " (" + van.getPlateNumber() + ") from " + route + "\n" +
                promoMsg,
                "FORFEIT PROCESSED", JOptionPane.INFORMATION_MESSAGE);
    }

    private Van findVanByPlate(String plate) {
        if (plate == null) return null;
        for (Van v : vans) {
            if (v.getPlateNumber().equalsIgnoreCase(plate.trim())) return v;
        }
        return null;
    }

    private JPanel buildCounterTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel left = createClarityCard();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(320, 0));

        JLabel title = new JLabel("Lookup Booking");
        title.setFont(VanGoTheme.h3());
        title.setForeground(BRAND_NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtSearch = createClarityTextField();
        txtSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        txtSearch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnSearch = new ClarityButton("Find Pass", BRAND_NAVY, Color.WHITE, BRAND_NAVY, 13);
        btnSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnSearch.setAlignmentX(Component.LEFT_ALIGNMENT);

        ClarityButton btnConfirmPay = new ClarityButton("Confirm Payment & Issue Ticket", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 13);
        btnConfirmPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnConfirmPay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmPay.setEnabled(false);

        left.add(title);
        left.add(Box.createRigidArea(new Dimension(0, 20)));
        left.add(txtSearch);
        left.add(Box.createRigidArea(new Dimension(0, 8)));
        left.add(btnSearch);
        left.add(Box.createVerticalGlue());
        left.add(btnConfirmPay);

        JTextArea txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtReceipt);

        btnSearch.addActionListener(e -> {
            String ref = txtSearch.getText().trim();
            if (bookingDatabase.containsKey(ref)) {
                Booking b = bookingDatabase.get(ref);
                txtReceipt.setText(
                        "BOOKING REFERENCE: " + b.getReferenceNo() + "\n" +
                        "Passenger: " + b.getPassengerName() + " (" + b.getPassengerContact() + ")\n" +
                        "Destination: " + b.getSchedule().getDestination() + "\n" +
                        "Dispatch Rule: " + b.getSchedule().getDepartureTime() + "\n" +
                        "Seats: " + b.getSelectedSeats() + "\n" +
                        "Total Due: ₱" + String.format("%.2f", b.getTotalAmount()) + "\n" +
                        "Status: " + b.getStatus()
                );
                btnConfirmPay.setEnabled(true);
            } else {
                txtReceipt.setText("No booking found for reference: " + ref);
                btnConfirmPay.setEnabled(false);
            }
        });

        btnConfirmPay.addActionListener(e -> {
            String ref = txtSearch.getText().trim();
            Booking b = bookingDatabase.get(ref);
            if (b != null) {
                b.setStatus("PAID — BOARDING PASS ISSUED");
                txtReceipt.append("\n\n>>> PAYMENT CONFIRMED AT " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " <<<");
                btnConfirmPay.setEnabled(false);
            }
        });

        panel.add(left, BorderLayout.WEST);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSchedulesAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        String[] cols = {"ID", "Destination", "Queue #", "Queue Date", "Dispatch Rule", "Fare", "Assigned Van", "Driver"};
        modelSchedules = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createClarityTable(modelSchedules);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildVansAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);
        String[] cols = {"ID", "Model", "Plate Number", "Capacity", "Driver"};
        modelVans = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        panel.add(new JScrollPane(createClarityTable(modelVans)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDriversAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);
        String[] cols = {"ID", "Full Name", "License Number", "Phone"};
        modelDrivers = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        panel.add(new JScrollPane(createClarityTable(modelDrivers)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDriverDashboardView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CANVAS);
        root.add(createStatusHeader("Driver Operational Console", "Fill-and-Depart FCFS Lineup & Route Masterlist", false, false), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(20, 16));
        content.setBorder(new EmptyBorder(20, 30, 16, 30));
        content.setOpaque(false);

        JPanel driverCard = createClarityCard();
        driverCard.setLayout(new BorderLayout(15, 12));

        JPanel infoGroup = new JPanel();
        infoGroup.setLayout(new BoxLayout(infoGroup, BoxLayout.Y_AXIS));
        infoGroup.setOpaque(false);

        lblDriverVanName = new JLabel("Assigned Vehicle: Loading...");
        lblDriverVanName.setFont(VanGoTheme.h2());
        lblDriverVanName.setForeground(BRAND_NAVY);

        lblDriverPlate = new JLabel("Plate Number: - | License: -");
        lblDriverPlate.setFont(VanGoTheme.body());
        lblDriverPlate.setForeground(TEXT_MUTED);

        lblDriverQueueStatus = new JLabel("Queue Status: Not lined up yet");
        lblDriverQueueStatus.setFont(VanGoTheme.font(12, Font.BOLD));
        lblDriverQueueStatus.setForeground(SEMANTIC_GO);

        infoGroup.add(lblDriverVanName);
        infoGroup.add(Box.createRigidArea(new Dimension(0, 4)));
        infoGroup.add(lblDriverPlate);
        infoGroup.add(Box.createRigidArea(new Dimension(0, 6)));
        infoGroup.add(lblDriverQueueStatus);

        JPanel queueActionBar = new JPanel();
        queueActionBar.setLayout(new BoxLayout(queueActionBar, BoxLayout.Y_AXIS));
        queueActionBar.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        row1.setOpaque(false);

        JLabel lblDestPick = new JLabel("Select Route:");
        lblDestPick.setFont(VanGoTheme.font(12, Font.BOLD));
        lblDestPick.setForeground(BRAND_NAVY);

        List<String> availableDestinations = new ArrayList<>(RouteCorridor.getKnownDestinations());
        cbDriverQueueDest = new JComboBox<>(availableDestinations.toArray(new String[0]));
        cbDriverQueueDest.setFont(VanGoTheme.font(12, Font.PLAIN));
        cbDriverQueueDest.setPreferredSize(new Dimension(210, 36));
        cbDriverQueueDest.addActionListener(e -> refreshDriverRouteQueueTable());

        ClarityButton btnJoinQueue = new ClarityButton("+ List Van into Route Queue (Fill-and-Depart)", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 12);
        btnJoinQueue.setPreferredSize(new Dimension(300, 36));
        btnJoinQueue.addActionListener(e -> handleDriverJoinQueue());

        row1.add(lblDestPick);
        row1.add(cbDriverQueueDest);
        row1.add(btnJoinQueue);

        JLabel lblRuleHint = new JLabel("No fixed departure clock — queue order is strictly FCFS; van departs when full.");
        lblRuleHint.setFont(VanGoTheme.font(11, Font.ITALIC));
        lblRuleHint.setForeground(TEXT_MUTED);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row2.setOpaque(false);
        row2.add(lblRuleHint);

        queueActionBar.add(row1);
        queueActionBar.add(row2);

        driverCard.add(infoGroup, BorderLayout.WEST);
        driverCard.add(queueActionBar, BorderLayout.EAST);

        JPanel splitCenter = new JPanel(new GridLayout(1, 2, 18, 0));
        splitCenter.setOpaque(false);

        driverTripsContainer = new JPanel();
        driverTripsContainer.setLayout(new BoxLayout(driverTripsContainer, BoxLayout.Y_AXIS));
        driverTripsContainer.setOpaque(false);

        JScrollPane scrollTrips = new JScrollPane(driverTripsContainer);
        scrollTrips.setBorder(null);
        scrollTrips.getViewport().setBackground(BG_CANVAS);
        scrollTrips.getVerticalScrollBar().setUnitIncrement(14);

        JPanel routeQueueCard = createClarityCard();
        routeQueueCard.setLayout(new BorderLayout(0, 10));

        lblRouteQueueHeader = new JLabel("Route Queue Masterlist — Tacloban City (FCFS Order)");
        lblRouteQueueHeader.setFont(VanGoTheme.h3());
        lblRouteQueueHeader.setForeground(BRAND_NAVY);

        JLabel lblQueueNote = new JLabel("<html><div style='color:#475569; font-size:11px;'>" +
                "Masterlist of vans lined up on this route. Only <b>#1</b> boards passengers until full; <b>#2+</b> board next once #1 departs.</div></html>");

        JPanel rqHead = new JPanel();
        rqHead.setLayout(new BoxLayout(rqHead, BoxLayout.Y_AXIS));
        rqHead.setOpaque(false);
        rqHead.add(lblRouteQueueHeader);
        rqHead.add(Box.createRigidArea(new Dimension(0, 4)));
        rqHead.add(lblQueueNote);

        String[] rqCols = {"Queue #", "Queue Date", "Van Model", "Plate No.", "Driver Name", "Load", "Status"};
        modelDriverRouteQueue = new DefaultTableModel(rqCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable rqTable = createClarityTable(modelDriverRouteQueue);
        rqTable.setRowHeight(38);
        JScrollPane rqScroll = new JScrollPane(rqTable);
        rqScroll.setBorder(new LineBorder(BORDER_NEUTRAL, 1, true));

        routeQueueCard.add(rqHead, BorderLayout.NORTH);
        routeQueueCard.add(rqScroll, BorderLayout.CENTER);

        splitCenter.add(scrollTrips);
        splitCenter.add(routeQueueCard);

        content.add(driverCard, BorderLayout.NORTH);
        content.add(splitCenter, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        ClarityButton btnLogout = new ClarityButton("← Switch Driver / Logout", CARD_SURFACE, BRAND_NAVY, BORDER_NEUTRAL, 13);
        btnLogout.setPreferredSize(new Dimension(210, 40));
        btnLogout.addActionListener(e -> {
            renderScheduleCards();
            cardLayout.show(mainContainer, "STAFF_GATEWAY");
        });
        footer.add(btnLogout);

        root.add(content, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void refreshDriverRouteQueueTable() {
        if (modelDriverRouteQueue == null || cbDriverQueueDest == null) return;
        String dest = (String) cbDriverQueueDest.getSelectedItem();
        if (dest == null) return;

        if (lblRouteQueueHeader != null) {
            lblRouteQueueHeader.setText("Route Queue Masterlist — " + dest + " (FCFS Order)");
        }

        modelDriverRouteQueue.setRowCount(0);
        List<Van> q = driverQueue.getQueue(dest);
        int pos = 1;
        for (Van v : q) {
            String loadText = "0 / " + v.getCapacity();
            boolean isFull = false;
            for (Schedule s : schedules) {
                if (!s.isDispatched()
                        && s.getDestination().equalsIgnoreCase(dest)
                        && s.getVan().getId().equals(v.getId())) {
                    loadText = s.getBookedCount() + " / " + s.getCapacity();
                    isFull = s.isFull();
                    break;
                }
            }
            String statusText = (pos == 1)
                    ? (isFull ? "FULL · READY TO DEPART" : "#1 BOARDING NOW")
                    : ("STANDBY (#" + pos + " IN LINE)");

            modelDriverRouteQueue.addRow(new Object[]{
                    "#" + pos,
                    getQueueDate(dest, v),
                    v.getModel(),
                    v.getPlateNumber(),
                    v.getDriver().getName(),
                    loadText,
                    statusText
            });
            pos++;
        }

        if (q.isEmpty()) {
            modelDriverRouteQueue.addRow(new Object[]{"-", "-", "No vans queued for " + dest + " yet", "-", "-", "-", "OPEN — BE #1"});
        }
    }

    private void handleDriverJoinQueue() {
        if (loggedInDriver == null) return;
        Van assignedVan = getVanForDriver(loggedInDriver);
        if (assignedVan == null) {
            JOptionPane.showMessageDialog(this, "No vehicle is assigned to your driver profile.", "NO VEHICLE ASSIGNED", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String dest = (String) cbDriverQueueDest.getSelectedItem();
        if (dest == null) return;

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        driverQueue.enqueueIfAbsent(dest, assignedVan);
        if (!queueDates.containsKey(qKey(dest, assignedVan))) {
            setQueueDate(dest, assignedVan, today);
        }

        int pos = driverQueue.positionOf(dest, assignedVan);
        int totalInQueue = driverQueue.size(dest);

        Schedule existing = null;
        for (Schedule s : schedules) {
            if (!s.isDispatched()
                    && s.getVan().getId().equals(assignedVan.getId())
                    && s.getDestination().equalsIgnoreCase(dest)) {
                existing = s;
                break;
            }
        }

        if (existing == null) {
            String newId = String.format("SCH%03d", schedules.size() + 1);
            double fare = estimateFareForDestination(dest);
            Schedule newTrip = new Schedule(newId, dest, FILL_AND_DEPART_MODE, fare, assignedVan);
            schedules.add(newTrip);

            String boardingNote = (pos == 1)
                    ? "You are #1 in line — your van is NOW BOARDING passengers and departs once full!"
                    : "You are #" + pos + " in line — your van will open for boarding as soon as the van(s) ahead fill up and depart.";

            JOptionPane.showMessageDialog(this,
                    "LISTED IN " + dest.toUpperCase() + " QUEUE (FCFS)\n\n" +
                    "Queue Number: #" + pos + " of " + totalInQueue + "\n" +
                    "Queue Date: " + getQueueDate(dest, assignedVan) + "\n" +
                    "Dispatch Rule: " + FILL_AND_DEPART_MODE + "\n" +
                    "Trip ID: " + newId + "\n\n" +
                    boardingNote,
                    "ADDED TO ROUTE QUEUE", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "ALREADY LISTED IN " + dest.toUpperCase() + " QUEUE\n\n" +
                    "Your Queue Number: #" + pos + " of " + totalInQueue + "\n" +
                    "Queue Date: " + getQueueDate(dest, assignedVan) + "\n" +
                    "Trip ID: " + existing.getId(),
                    "QUEUE POSITION CONFIRMED", JOptionPane.INFORMATION_MESSAGE);
        }

        refreshAdminTables();
        renderScheduleCards();
        renderDriverDashboard();
    }

    private double estimateFareForDestination(String dest) {
        if (dest == null) return 250.00;
        switch (dest) {
            case "Albuera": return 80.00;
            case "Kananga": return 70.00;
            case "Merida": return 90.00;
            case "Isabel": return 120.00;
            case "Palompon": return 160.00;
            case "Baybay City": return 180.00;
            case "Inopacan": return 220.00;
            case "Hindang": return 240.00;
            case "Hilongos": return 260.00;
            case "Bato": return 280.00;
            case "Matalom": return 300.00;
            case "Maasin City": return 350.00;
            case "Capoocan":
            case "Carigara": return 150.00;
            case "Tunga":
            case "Jaro":
            case "Alangalang":
            case "Santa Fe": return 200.00;
            case "Palo":
            case "Tanauan":
            case "Tacloban City": return 250.00;
            case "Leyte":
            case "Biliran":
            case "Naval": return 350.00;
            default: return 380.00;
        }
    }

    private Van getVanForDriver(Driver driver) {
        if (driver == null) return null;
        for (Van v : vans) {
            if (v.getDriver() != null && v.getDriver().getId().equals(driver.getId())) {
                return v;
            }
        }
        return null;
    }

    private void openDriverPortalLogin() {
        cbActiveDriverLogin = new JComboBox<>(drivers.toArray(new Driver[0]));
        int result = JOptionPane.showConfirmDialog(this, cbActiveDriverLogin, "DRIVER LOGIN", JOptionPane.OK_CANCEL_OPTION);
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
        Van assignedVan = getVanForDriver(loggedInDriver);

        if (assignedVan != null) {
            lblDriverVanName.setText("Assigned Vehicle: " + assignedVan.getModel());
            lblDriverPlate.setText("Plate Number: " + assignedVan.getPlateNumber() + "  |  License: " + loggedInDriver.getLicenseNo());

            List<String> activeQueues = new ArrayList<>();
            for (Map.Entry<String, Deque<Van>> entry : driverQueue.getAllQueues().entrySet()) {
                int pos = driverQueue.positionOf(entry.getKey(), assignedVan);
                if (pos > 0) {
                    activeQueues.add(entry.getKey() + " (Queue #" + pos + " of " + entry.getValue().size() + ")");
                }
            }
            if (activeQueues.isEmpty()) {
                lblDriverQueueStatus.setText("Queue Status: Not lined up in any route queue yet");
                lblDriverQueueStatus.setForeground(SEMANTIC_AMBER);
            } else {
                lblDriverQueueStatus.setText("My Queue Lineup: " + String.join("  •  ", activeQueues));
                lblDriverQueueStatus.setForeground(SEMANTIC_GO);
            }
        }

        driverTripsContainer.removeAll();

        int tripCount = 0;
        for (Schedule s : schedules) {
            if (assignedVan != null && s.getVan().getId().equals(assignedVan.getId())) {
                tripCount++;
                int qPos = driverQueue.positionOf(s.getDestination(), assignedVan);
                boolean isLeader = (qPos == 1);
                int booked = s.getBookedCount();
                int cap = s.getCapacity();
                int minEarlySeats = (int) Math.ceil(cap * MIN_EARLY_LOAD_RATIO);

                JPanel card = new JPanel(new BorderLayout(15, 10)) {
                    @Override
                    public Dimension getMaximumSize() {
                        Dimension pref = getPreferredSize();
                        return new Dimension(Integer.MAX_VALUE, pref.height);
                    }
                };
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setBackground(CARD_SURFACE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER_NEUTRAL, 1, true),
                        new EmptyBorder(14, 16, 14, 16)
                ));

                JPanel top = new JPanel(new BorderLayout());
                top.setOpaque(false);

                JLabel lblTrip = new JLabel("Trip " + s.getId() + " → " + s.getDestination());
                lblTrip.setFont(VanGoTheme.font(14, Font.BOLD));
                lblTrip.setForeground(BRAND_NAVY);

                JLabel lblStatus = new JLabel();
                if (s.isDispatched()) {
                    lblStatus.setText("DISPATCHED");
                    lblStatus.setForeground(VanGoTheme.TEXT_TERT);
                } else if (isLeader) {
                    lblStatus.setText(s.isFull() ? "FULL · READY" : "#1 BOARDING NOW");
                    lblStatus.setForeground(s.isFull() ? SEMANTIC_AMBER : SEMANTIC_GO);
                } else {
                    lblStatus.setText("STANDBY (#" + qPos + ")");
                    lblStatus.setForeground(TEXT_MUTED);
                }
                lblStatus.setFont(VanGoTheme.font(11, Font.BOLD));

                top.add(lblTrip, BorderLayout.WEST);
                top.add(lblStatus, BorderLayout.EAST);

                JPanel body = new JPanel();
                body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                body.setOpaque(false);

                createClarityDetailRow(body, "Queue Date", getQueueDate(s.getDestination(), assignedVan));
                createClarityDetailRow(body, "Vehicle", s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")");
                createClarityDetailRow(body, "Occupancy", booked + " / " + cap + " seats" + (s.getDriverDepartureRequested() ? "  (Early Request Pending)" : ""));

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                btnPanel.setOpaque(false);

                if (!s.isDispatched() && isLeader && !s.getDriverDepartureRequested()) {
                    ClarityButton btnReqEarly = new ClarityButton("Request Early Departure", BRAND_ORANGE, Color.WHITE, BRAND_ORANGE, 11);
                    btnReqEarly.setPreferredSize(new Dimension(200, 32));
                    btnReqEarly.addActionListener(e -> {
                        if (booked < minEarlySeats) {
                            JOptionPane.showMessageDialog(this, "Cannot request early departure yet.\nMust have at least " + minEarlySeats + " passengers (70% load).", "MINIMUM LOAD NOT MET", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        s.setDriverDepartureRequested(true);
                        JOptionPane.showMessageDialog(this, "Early departure request submitted to Admin.\nAwaiting approval.", "REQUEST SUBMITTED", JOptionPane.INFORMATION_MESSAGE);
                        refreshAdminTables();
                        renderDriverDashboard();
                    });
                    btnPanel.add(btnReqEarly);
                } else if (!s.isDispatched() && s.getDriverDepartureRequested()) {
                    JLabel lblWait = new JLabel("Awaiting Admin Approval...");
                    lblWait.setFont(VanGoTheme.font(11, Font.ITALIC));
                    lblWait.setForeground(SEMANTIC_AMBER);
                    btnPanel.add(lblWait);
                }

                if (s.getRejectionReason() != null && !s.getRejectionReason().isEmpty() && !s.isDispatched()) {
                    JLabel lblRej = new JLabel("<html><i>Reason: " + s.getRejectionReason() + "</i></html>");
                    lblRej.setFont(VanGoTheme.font(11, Font.PLAIN));
                    lblRej.setForeground(SEMANTIC_RED);
                    body.add(Box.createRigidArea(new Dimension(0, 4)));
                    body.add(lblRej);
                }

                card.add(top, BorderLayout.NORTH);
                card.add(body, BorderLayout.CENTER);
                card.add(btnPanel, BorderLayout.SOUTH);

                driverTripsContainer.add(card);
                driverTripsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        if (tripCount == 0) {
            JLabel empty = new JLabel("No active trips assigned to your vehicle right now.");
            empty.setFont(VanGoTheme.body());
            empty.setForeground(TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            driverTripsContainer.add(Box.createRigidArea(new Dimension(0, 20)));
            driverTripsContainer.add(empty);
        }

        driverTripsContainer.revalidate();
        driverTripsContainer.repaint();

        refreshDriverRouteQueueTable();
    }

    private void refreshAdminTables() {
        if (modelDispatch != null) {
            modelDispatch.setRowCount(0);

            String filter = cbAdminQueueRouteFilter != null
                    ? (String) cbAdminQueueRouteFilter.getSelectedItem() : null;
            boolean filterAll = (filter == null || filter.startsWith("All Routes"));

            for (Schedule s : schedules) {
                if (s.isDispatched()) continue;
                int qPos = driverQueue.positionOf(s.getDestination(), s.getVan());
                if (filterAll || s.getDestination().equalsIgnoreCase(filter)) {
                    String status;
                    if (s.getDriverDepartureRequested()) status = "AWAITING APPROVAL";
                    else if (s.isFull()) status = "FULL · READY";
                    else if (qPos == 1) status = "#1 BOARDING NOW";
                    else status = "STANDBY (#" + qPos + ")";

                    modelDispatch.addRow(new Object[]{
                            s.getId(),
                            s.getDestination(),
                            "#" + (qPos <= 0 ? "-" : qPos),
                            getQueueDate(s.getDestination(), s.getVan()),
                            s.getBookedCount() + " / " + s.getCapacity(),
                            s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")",
                            s.getVan().getDriver().getName(),
                            status
                    });
                }
            }
        }

        if (modelQueue != null) {
            modelQueue.setRowCount(0);
            String filter = cbAdminQueueRouteFilter != null
                    ? (String) cbAdminQueueRouteFilter.getSelectedItem() : null;
            boolean filterAll = (filter == null || filter.startsWith("All Routes"));

            for (Map.Entry<String, Deque<Van>> entry : driverQueue.getAllQueues().entrySet()) {
                String route = entry.getKey();
                if (!filterAll && !route.equalsIgnoreCase(filter)) continue;

                int pos = 1;
                for (Van v : entry.getValue()) {
                    Schedule s = findScheduleByVanAndRoute(v, route);
                    String load = (s != null) ? s.getBookedCount() + " / " + s.getCapacity() : "0 / " + v.getCapacity();
                    String status = (pos == 1) ? "#1 BOARDING NOW"
                                  : (s != null && s.isFull()) ? "FULL · READY"
                                  : "STANDBY (#" + pos + ")";
                    modelQueue.addRow(new Object[]{
                            route, "#" + pos, getQueueDate(route, v),
                            v.getModel(), v.getPlateNumber(),
                            v.getDriver().getName(), load, status
                    });
                    pos++;
                }
            }
        }

        if (modelDispatchLog != null) {
            modelDispatchLog.setRowCount(0);
            for (Schedule s : schedules) {
                if (!s.isDispatched()) continue;
                modelDispatchLog.addRow(new Object[]{
                        s.getId(), s.getDestination(), s.getDepartureTime(),
                        s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")",
                        s.getVan().getDriver().getName(),
                        s.getDispatchedAt(),
                        RouteCorridor.breadcrumb(s.getChosenRoute())
                });
            }
        }

        if (modelSchedules != null) {
            modelSchedules.setRowCount(0);
            for (Schedule s : schedules) {
                int qPos = driverQueue.positionOf(s.getDestination(), s.getVan());
                modelSchedules.addRow(new Object[]{
                        s.getId(), s.getDestination(),
                        "#" + (qPos <= 0 ? "-" : qPos),
                        getQueueDate(s.getDestination(), s.getVan()),
                        s.getDepartureTime(),
                        "₱" + String.format("%.2f", s.getFare()),
                        s.getVan().getModel() + " (" + s.getVan().getPlateNumber() + ")",
                        s.getVan().getDriver().getName()
                });
            }
        }

        if (modelVans != null) {
            modelVans.setRowCount(0);
            for (Van v : vans) {
                modelVans.addRow(new Object[]{
                        v.getId(), v.getModel(), v.getPlateNumber(),
                        v.getCapacity(),
                        v.getDriver() != null ? v.getDriver().getName() : "-"
                });
            }
        }

        if (modelDrivers != null) {
            modelDrivers.setRowCount(0);
            for (Driver d : drivers) {
                modelDrivers.addRow(new Object[]{
                        d.getId(), d.getName(), d.getLicenseNo(), d.getPhone()
                });
            }
        }

        refreshKpiCards();
    }

    private Schedule findScheduleById(String id) {
        for (Schedule s : schedules) if (s.getId().equals(id)) return s;
        return null;
    }

    private Schedule findScheduleByVanAndRoute(Van van, String route) {
        for (Schedule s : schedules) {
            if (!s.isDispatched()
                    && s.getVan().getId().equals(van.getId())
                    && s.getDestination().equalsIgnoreCase(route)) {
                return s;
            }
        }
        return null;
    }

    private JTable createClarityTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(VanGoTheme.body());
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(CARD_SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(VanGoTheme.SURFACE_ALT);
        table.setSelectionForeground(BRAND_NAVY);
        table.getTableHeader().setFont(VanGoTheme.font(11, Font.BOLD));
        table.getTableHeader().setBackground(VanGoTheme.SURFACE_ALT);
        table.getTableHeader().setForeground(BRAND_NAVY);
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_NEUTRAL));
        table.setFillsViewportHeight(true);
        return table;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new VanGoKiosk().setVisible(true);
        });
    }
}