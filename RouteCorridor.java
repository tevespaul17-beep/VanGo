import java.util.*;

/**
 * Canonical road corridors for VanGo terminal trips.
 * Origin is always Ormoc City (terminal hub).
 * Order matters: index 0 is Ormoc, last index is the destination.
 */
public final class RouteCorridor {

    private static final String HUB = "Ormoc City";

    private static final Map<String, List<String>> CORRIDORS = new LinkedHashMap<>();
    // Alternative corridors (driver may pick) - optional
    private static final Map<String, Map<String, List<String>>> ALTERNATIVES = new LinkedHashMap<>();

        static {
        // 1. Ormoc to Maasin
        add("Maasin City",       "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Matalom","Maasin City");
        add("Bato",              "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato");
        add("Hilongos",          "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos");
        add("Hindang",           "Ormoc City","Albuera","Baybay City","Inopacan","Hindang");
        add("Inopacan",          "Ormoc City","Albuera","Baybay City","Inopacan");
        add("Baybay City",       "Ormoc City","Albuera","Baybay City");
        add("Matalom",           "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Matalom");
        add("Albuera",           "Ormoc City","Albuera");

        // 2. Ormoc to Tacloban
        add("Tacloban City",     "Ormoc City","Kananga","Capoocan","Carigara","Tunga","Jaro","Alangalang","Santa Fe","Palo","Tacloban City");
        add("Palo",              "Ormoc City","Kananga","Capoocan","Carigara","Tunga","Jaro","Alangalang","Santa Fe","Palo");
        add("Tanauan",           "Ormoc City","Kananga","Capoocan","Carigara","Tunga","Jaro","Alangalang","Santa Fe","Palo","Tanauan");

        // 3. Ormoc to Naval
        add("Naval",             "Ormoc City","Kananga","Capoocan","Leyte","Biliran","Naval");
        add("Palompon",          "Ormoc City","Merida","Isabel","Palompon");

        // 4. Ormoc to Sogod
        add("Sogod",             "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod");
        add("Sogod (So. Leyte)", "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod");
        add("Libagon",           "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod","Libagon");
        add("Saint Bernard",     "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod","Libagon","Saint Bernard");
        add("San Juan (So. Leyte)", "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod","Libagon","Saint Bernard","San Juan");
        add("Hinundayan",        "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod","Libagon","Saint Bernard","San Juan","Hinundayan");
        add("Hinunangan",        "Ormoc City","Albuera","Baybay City","Inopacan","Hindang","Hilongos","Bato","Bontoc","Sogod","Libagon","Saint Bernard","San Juan","Hinundayan","Hinunangan");
    }

    private static void add(String destination, String... path) {
        CORRIDORS.put(destination, Collections.unmodifiableList(Arrays.asList(path)));
    }

    /** Returns the canonical ordered corridor from Ormoc to destination. */
    public static List<String> getCanonicalRoute(String destination) {
        if (destination == null) return Collections.singletonList(HUB);
        List<String> c = CORRIDORS.get(destination);
        if (c != null) return c;
        // Fallback: Ormoc -> destination
        return Arrays.asList(HUB, destination);
    }

    /** True if the destination has a predefined corridor. */
    public static boolean hasCorridor(String destination) {
        return destination != null && CORRIDORS.containsKey(destination);
    }

    /** All destinations that have corridors (for the dropdown). */
    public static Set<String> getKnownDestinations() {
        return Collections.unmodifiableSet(CORRIDORS.keySet());
    }

    /**
     * Driver-facing option list: canonical first, then detour variants if any.
     * Currently returns { "Suggested Corridor", "Custom" } — the UI renders
     * Suggested Corridor with the full ordered breadcrumb.
     */
    public static List<String> getRouteOptionsForDriver(String destination) {
        List<String> opts = new ArrayList<>();
        opts.add("Suggested Corridor (recommended)");
        Map<String, List<String>> alts = ALTERNATIVES.get(destination);
        if (alts != null) opts.addAll(alts.keySet());
        opts.add("Custom");
        return opts;
    }

    /** Look up an alternative corridor by name, or null. */
    public static List<String> getAlternativeRoute(String destination, String optionName) {
        Map<String, List<String>> alts = ALTERNATIVES.get(destination);
        if (alts == null) return null;
        return alts.get(optionName);
    }

    /** Breadcrumb string, e.g. "Ormoc › Albuera › Baybay › …". */
    public static String breadcrumb(String destination) {
        List<String> path = getCanonicalRoute(destination);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" › ");
            sb.append(shortName(path.get(i)));
        }
        return sb.toString();
    }

    /** Breadcrumb for an explicit route list. */
    public static String breadcrumb(List<String> route) {
        if (route == null || route.isEmpty()) return "(no route)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) sb.append(" › ");
            sb.append(shortName(route.get(i)));
        }
        return sb.toString();
    }

    private static String shortName(String name) {
        if (name == null) return "";
        return name.replace(" City", "").replace(" (So. Leyte)", "");
    }

    private RouteCorridor() {}
}