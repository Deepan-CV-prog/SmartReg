import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ParticipantFileHandler
 * CSV schema:
 * ID,Name,Email,Phone,Department,College,Year,Events,Attendance
 *
 * Provides: save, load, search, edit, delete, markAttendance, updateParticipantsForEvent, export
 */
public class ParticipantFileHandler {
    private final String filePath;
    private final String backupDir = "backups";
    private int counter = 1000;

    public ParticipantFileHandler() {
        this("participants.csv");
    }

    public ParticipantFileHandler(String filePath) {
        this.filePath = filePath;
        ensureFileAndBackup();
        initCounter();
    }

    private void ensureFileAndBackup() {
        File f = new File(filePath);
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
                pw.println("ID,Name,Email,Phone,Department,College,Year,Events,Attendance");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File bd = new File(backupDir);
        if (!bd.exists()) bd.mkdirs();
    }

    private void initCounter() {
        File f = new File(filePath);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int max = counter;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] p = splitCSV(line);
                if (p.length > 0 && p[0].startsWith("P")) {
                    try {
                        int n = Integer.parseInt(p[0].substring(1));
                        if (n >= max) max = n + 1;
                    } catch (NumberFormatException ignored) {}
                }
            }
            counter = max;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateID() {
        return "P" + (counter++);
    }

    private void backupFile() {
        File src = new File(filePath);
        if (!src.exists()) return;
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dest = new File(backupDir + File.separator + "participants_backup_" + ts + ".csv");
        try (BufferedReader br = new BufferedReader(new FileReader(src));
             PrintWriter pw = new PrintWriter(new FileWriter(dest))) {
            String line;
            while ((line = br.readLine()) != null) pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Save a new participant (appends)
    public void saveParticipant(String id, String name, String email, String phone,
                                String dept, String college, String year, String events) {
        backupFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(escape(id) + "," + escape(name) + "," + escape(email) + "," + escape(phone) + ","
                    + escape(dept) + "," + escape(college) + "," + escape(year) + "," + escape(events) + ",Absent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Overload for when events provided as list
    public void saveParticipant(String name, String email, String phone,
                                String dept, String college, String year, List<String> eventsList) {
        String id = generateID();
        String events = String.join(" | ", eventsList);
        saveParticipant(id, name, email, phone, dept, college, year, events);
    }

    // Load all participants as list of string arrays (matching table columns)
    public List<String[]> loadParticipants() {
        List<String[]> out = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) return out;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] parts = splitCSV(line);
                // ensure 9 columns
                if (parts.length < 9) parts = Arrays.copyOf(parts, 9);
                out.add(parts);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    // Search by keyword in ID, name, email, phone, dept, college, year, events
    public List<String[]> search(String keyword) {
        List<String[]> res = new ArrayList<>();
        if (keyword == null) return res;
        String kw = keyword.toLowerCase();
        for (String[] row : loadParticipants()) {
            for (String field : row) {
                if (field != null && field.toLowerCase().contains(kw)) {
                    res.add(row);
                    break;
                }
            }
        }
        return res;
    }

    // Edit participant by ID (replace fields). attendanceParam may be null to keep existing
    public boolean edit(String id, String name, String email, String phone,
                        String dept, String college, String year, String events, String attendanceParam) {
        File f = new File(filePath);
        if (!f.exists()) return false;
        List<String[]> rows = new ArrayList<>();
        boolean found = false;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] p = splitCSV(line);
                if (p.length > 0 && p[0].equals(id)) {
                    String att = attendanceParam != null ? attendanceParam : (p.length >= 9 ? p[8] : "Absent");
                    String[] newRow = new String[]{id, name, email, phone, dept, college, year, events, att};
                    rows.add(newRow);
                    found = true;
                } else {
                    rows.add(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (found) writeAll(rows);
        return found;
    }

    public boolean delete(String id) {
        File f = new File(filePath);
        if (!f.exists()) return false;
        List<String[]> rows = new ArrayList<>();
        boolean removed = false;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] p = splitCSV(line);
                if (p.length > 0 && p[0].equals(id)) {
                    removed = true;
                } else {
                    rows.add(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (removed) writeAll(rows);
        return removed;
    }

    public boolean markAttendance(String id, String status) {
        File f = new File(filePath);
        if (!f.exists()) return false;
        List<String[]> rows = new ArrayList<>();
        boolean updated = false;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] p = splitCSV(line);
                if (p.length > 0 && p[0].equals(id)) {
                    if (p.length < 9) p = Arrays.copyOf(p, 9);
                    p[8] = status;
                    updated = true;
                }
                rows.add(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (updated) writeAll(rows);
        return updated;
    }

    // Update all participant event strings when an event is renamed or removed (replace occurrences)
    public void updateParticipantsForEvent(String oldEvent, String newEvent) {
        List<String[]> rows = loadParticipants();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String events = r.length > 7 ? r[7] : "";
            if (events != null && !events.isEmpty()) {
                String updated = events.replace(oldEvent, newEvent);
                r[7] = updated;
            }
        }
        writeAll(rows);
    }

    // Export current participants CSV to provided destination path
    public boolean exportTo(String destPath) {
        File src = new File(filePath);
        if (!src.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(src));
             PrintWriter pw = new PrintWriter(new FileWriter(destPath, false))) {
            String line;
            while ((line = br.readLine()) != null) pw.println(line);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Analytics: total, attendance rate, count per event
    public Map<String, Object> analytics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<String[]> rows = loadParticipants();
        int total = rows.size();
        int present = 0;
        Map<String, Integer> byEvent = new HashMap<>();
        for (String[] r : rows) {
            if (r.length >= 9 && "Present".equalsIgnoreCase(r[8])) present++;
            if (r.length >= 8) {
                String evs = r[7];
                if (evs != null && !evs.isEmpty()) {
                    String[] split = evs.split("\\|");
                    for (String s : split) {
                        String key = s.trim();
                        if (key.isEmpty()) continue;
                        byEvent.put(key, byEvent.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }
        stats.put("totalParticipants", total);
        stats.put("attendanceRate", total == 0 ? 0.0 : (100.0 * present / total));
        stats.put("byEvent", byEvent);
        return stats;
    }

    // ---------- Private utilities ----------

    private void writeAll(List<String[]> rows) {
        backupFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            // header
            pw.println("ID,Name,Email,Phone,Department,College,Year,Events,Attendance");
            for (String[] r : rows) {
                String[] rr = Arrays.copyOf(r, 9);
                for (int i = 0; i < rr.length; i++) if (rr[i] == null) rr[i] = "";
                pw.println(escape(rr[0]) + "," + escape(rr[1]) + "," + escape(rr[2]) + "," +
                        escape(rr[3]) + "," + escape(rr[4]) + "," + escape(rr[5]) + "," +
                        escape(rr[6]) + "," + escape(rr[7]) + "," + escape(rr[8]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Basic CSV escaping (quotes if needed)
    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    // Rudimentary CSV splitter that respects quoted commas
    private String[] splitCSV(String line) {
        if (line == null) return new String[0];
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
}
