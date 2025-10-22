import javax.swing.*;
import javax.swing.table.DefaultTableModel;  // <-- for table model
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;  // <-- for Arrays.asList()

/**
 * SmartRegisterGUI - main GUI class tying everything together
 * Requires EventFileHandler.java and ParticipantFileHandler.java in same folder.
 */
public class SmartRegisterGUI {
    // handlers
    private final EventFileHandler eventHandler = new EventFileHandler();
    private final ParticipantFileHandler participantHandler = new ParticipantFileHandler();

    // GUI components
    private JFrame frame;
    private DefaultTableModel tableModel;
    private JTable participantTable;

    // registration fields
    private JTextField nameField, emailField, phoneField, deptField, collegeField, yearField, searchField;
    private JPanel eventPanel;
    private java.util.List<JCheckBox> regEventCBs;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SmartRegisterGUI gui = new SmartRegisterGUI();
            gui.createAndShow();
        });
    }

    private void createAndShow() {
        frame = new JFrame("SmartRegister - Participant Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout(8, 8));

        // Top: Registration panel
        JPanel regPanel = new JPanel(new BorderLayout());
        regPanel.setBorder(BorderFactory.createTitledBorder("Register Participant"));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        deptField = new JTextField();
        collegeField = new JTextField();
        yearField = new JTextField();

        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Email:")); form.add(emailField);
        form.add(new JLabel("Phone:")); form.add(phoneField);
        form.add(new JLabel("Department:")); form.add(deptField);
        form.add(new JLabel("College:")); form.add(collegeField);
        form.add(new JLabel("Year:")); form.add(yearField);
        form.add(new JLabel("Events:"));

        eventPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buildEventCheckboxes();
        JScrollPane eventScroll = new JScrollPane(eventPanel);
        eventScroll.setPreferredSize(new Dimension(600, 60));
        form.add(eventScroll);

        regPanel.add(form, BorderLayout.CENTER);

        JPanel regBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton registerBtn = new JButton("Register");
        JButton importBtn = new JButton("Import CSV");
        JButton clearBtn = new JButton("Clear");
        regBtns.add(registerBtn); regBtns.add(importBtn); regBtns.add(clearBtn);
        regPanel.add(regBtns, BorderLayout.SOUTH);

        frame.add(regPanel, BorderLayout.NORTH);

        // Center: table
        String[] cols = {"ID","Name","Email","Phone","Department","College","Year","Events","Attendance"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        participantTable = new JTable(tableModel);
        participantTable.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(participantTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Registered Participants"));
        frame.add(tableScroll, BorderLayout.CENTER);

        // Right side controls
        JPanel right = new JPanel(new GridLayout(0,1,8,8));
        right.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Search
        JPanel searchP = new JPanel(new FlowLayout());
        searchField = new JTextField(12);
        JButton searchBtn = new JButton("Search");
        JButton resetBtn = new JButton("Reset");
        searchP.add(new JLabel("Search:")); searchP.add(searchField); searchP.add(searchBtn); searchP.add(resetBtn);
        right.add(searchP);

        // Edit/Delete/Attendance
        JPanel editP = new JPanel(new FlowLayout());
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton markPresent = new JButton("Mark Present");
        JButton markAbsent = new JButton("Mark Absent");
        editP.add(editBtn); editP.add(deleteBtn); editP.add(markPresent); editP.add(markAbsent);
        right.add(editP);

        // Event management
        JPanel evP = new JPanel(new FlowLayout());
        JButton addEv = new JButton("Add Event");
        JButton renameEv = new JButton("Rename Event");
        JButton removeEv = new JButton("Remove Event");
        evP.add(addEv); evP.add(renameEv); evP.add(removeEv);
        right.add(evP);

        // Export / Analytics / Admin / Exit
        JPanel utilP = new JPanel(new FlowLayout());
        JButton exportBtn = new JButton("Export CSV");
        JButton analyticsBtn = new JButton("Analytics");
        JButton adminBtn = new JButton("Admin Login");
        JButton exitBtn = new JButton("Exit");
        utilP.add(exportBtn); utilP.add(analyticsBtn); utilP.add(adminBtn); utilP.add(exitBtn);
        right.add(utilP);

        frame.add(right, BorderLayout.EAST);

        // Load participants initially
        reloadTable();

        // ---------- Actions ----------

        registerBtn.addActionListener(e -> doRegister());
        clearBtn.addActionListener(e -> clearForm());
        importBtn.addActionListener(e -> doImport());

        searchBtn.addActionListener(e -> {
            String kw = searchField.getText().trim();
            if (kw.isEmpty()) { reloadTable(); return; }
            List<String[]> r = participantHandler.search(kw);
            tableModel.setRowCount(0);
            for (String[] arr : r) tableModel.addRow(arr);
        });
        resetBtn.addActionListener(e -> { searchField.setText(""); reloadTable(); });

        editBtn.addActionListener(e -> doEdit());
        deleteBtn.addActionListener(e -> doDelete());
        markPresent.addActionListener(e -> changeAttendance("Present"));
        markAbsent.addActionListener(e -> changeAttendance("Absent"));

        addEv.addActionListener(e -> {
            String ev = JOptionPane.showInputDialog(frame, "Enter new event name:");
            if (ev == null || ev.trim().isEmpty()) return;
            boolean ok = eventHandler.addEvent(ev.trim());
            if (ok) { buildEventCheckboxes(); reloadTable(); JOptionPane.showMessageDialog(frame, "Event added."); }
            else JOptionPane.showMessageDialog(frame, "Event exists or invalid.");
        });

        renameEv.addActionListener(e -> {
            List<String> evs = eventHandler.loadEvents();
            if (evs.isEmpty()) { JOptionPane.showMessageDialog(frame, "No events to rename."); return; }
            String oldEv = (String) JOptionPane.showInputDialog(frame, "Select event to rename:", "Rename", JOptionPane.QUESTION_MESSAGE, null, evs.toArray(), evs.get(0));
            if (oldEv == null) return;
            String newEv = JOptionPane.showInputDialog(frame, "New name for " + oldEv + ":");
            if (newEv == null || newEv.trim().isEmpty()) return;
            boolean ok = eventHandler.renameEvent(oldEv, newEv.trim());
            if (ok) {
                participantHandler.updateParticipantsForEvent(oldEv, newEv.trim());
                buildEventCheckboxes();
                reloadTable();
                JOptionPane.showMessageDialog(frame, "Event renamed globally.");
            } else JOptionPane.showMessageDialog(frame, "Rename failed (duplicate or invalid).");
        });

        removeEv.addActionListener(e -> {
            List<String> evs = eventHandler.loadEvents();
            if (evs.isEmpty()) { JOptionPane.showMessageDialog(frame, "No events to remove."); return; }
            String ev = (String) JOptionPane.showInputDialog(frame, "Select event to remove:", "Remove", JOptionPane.QUESTION_MESSAGE, null, evs.toArray(), evs.get(0));
            if (ev == null) return;
            int c = JOptionPane.showConfirmDialog(frame, "Remove event '" + ev + "'? This will remove the tag from participants.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            boolean ok = eventHandler.removeEvent(ev);
            if (ok) {
                participantHandler.updateParticipantsForEvent(ev, "");
                buildEventCheckboxes();
                reloadTable();
                JOptionPane.showMessageDialog(frame, "Event removed.");
            } else JOptionPane.showMessageDialog(frame, "Remove failed.");
        });

        exportBtn.addActionListener(e -> {
            String dest = JOptionPane.showInputDialog(frame, "Enter destination path (e.g. out.csv):");
            if (dest == null || dest.trim().isEmpty()) return;
            boolean ok = participantHandler.exportTo(dest.trim());
            JOptionPane.showMessageDialog(frame, ok ? "Exported to " + dest : "Export failed.");
        });

        analyticsBtn.addActionListener(e -> {
            Map<String,Object> stats = participantHandler.analytics();
            int total = (int) stats.getOrDefault("totalParticipants", 0);
            double rate = (double) stats.getOrDefault("attendanceRate", 0.0);
            @SuppressWarnings("unchecked")
            Map<String,Integer> byEvent = (Map<String,Integer>) stats.getOrDefault("byEvent", new HashMap<String,Integer>());
            StringBuilder sb = new StringBuilder();
            sb.append("Total participants: ").append(total).append("\n");
            sb.append(String.format("Attendance rate: %.2f%%\n\n", rate));
            sb.append("Participants per event:\n");
            if (byEvent.isEmpty()) sb.append(" (none)\n"); else byEvent.forEach((k,v)-> sb.append("  ").append(k).append(": ").append(v).append("\n"));
            JOptionPane.showMessageDialog(frame, sb.toString(), "Analytics", JOptionPane.INFORMATION_MESSAGE);
        });

        adminBtn.addActionListener(e -> {
            String pw = JOptionPane.showInputDialog(frame, "Enter admin password:");
            // demo password: admin123 (in production replace with hashed storage)
            if ("admin123".equals(pw)) JOptionPane.showMessageDialog(frame, "Admin access granted (demo).");
            else JOptionPane.showMessageDialog(frame, "Invalid password.");
        });

        exitBtn.addActionListener(e -> System.exit(0));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------- Helper UI & actions ----------

    private void buildEventCheckboxes() {
        eventPanel.removeAll();
        regEventCBs = new ArrayList<>();
        List<String> events = eventHandler.loadEvents();
        for (String ev : events) {
            JCheckBox cb = new JCheckBox(ev);
            regEventCBs.add(cb);
            eventPanel.add(cb);
        }
        eventPanel.revalidate();
        eventPanel.repaint();
    }

    private void reloadTable() {
        tableModel.setRowCount(0);
        List<String[]> rows = participantHandler.loadParticipants();
        for (String[] r : rows) tableModel.addRow(r);
    }

    private void doRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String dept = deptField.getText().trim();
        String college = collegeField.getText().trim();
        String year = yearField.getText().trim();
        List<String> chosen = new ArrayList<>();
        for (JCheckBox cb : regEventCBs) if (cb.isSelected()) chosen.add(cb.getText());
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || chosen.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill Name, Email, Phone and select at least one event.");
            return;
        }
        // duplicate detection by email or phone
        for (String[] r : participantHandler.loadParticipants()) {
            if (r.length > 2 && email.equalsIgnoreCase(r[2])) {
                int opt = JOptionPane.showConfirmDialog(frame, "A participant with same email exists. Continue?", "Duplicate", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
                break;
            }
            if (r.length > 3 && phone.equals(r[3])) {
                int opt = JOptionPane.showConfirmDialog(frame, "A participant with same phone exists. Continue?", "Duplicate", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
                break;
            }
        }
        // save
        participantHandler.saveParticipant(name, email, phone, dept, college, year, chosen);
        reloadTable();
        clearForm();
        JOptionPane.showMessageDialog(frame, "Participant registered.");
    }

    private void clearForm() {
        nameField.setText(""); emailField.setText(""); phoneField.setText("");
        deptField.setText(""); collegeField.setText(""); yearField.setText("");
        for (JCheckBox cb : regEventCBs) cb.setSelected(false);
    }

    private void doImport() {
        JFileChooser fc = new JFileChooser();
        int res = fc.showOpenDialog(frame);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; boolean first=true;
            int added=0;
            while ((line = br.readLine()) != null) {
                if (first) { first=false; continue; } // skip header
                String[] p = splitCSV(line);
                // expected: Name,Email,Phone,Department,College,Year,Events
                if (p.length >= 7) {
                    String name = p[0].trim(), email = p[1].trim(), phone = p[2].trim();
                    String dept = p[3].trim(), college = p[4].trim(), year = p[5].trim();
                    String events = p[6].trim();
                    List<String> evs = new ArrayList<>();
                    for (String s : events.split("[;|]")) {
                        String t = s.trim();
                        if (!t.isEmpty()) evs.add(t);
                    }
                    participantHandler.saveParticipant(name, email, phone, dept, college, year, evs);
                    added++;
                }
            }
            reloadTable();
            JOptionPane.showMessageDialog(frame, "Imported " + added + " participants.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Import failed: " + ex.getMessage());
        }
    }

    private void doEdit() {
        int row = participantTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(frame, "Select a participant to edit."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String curName = getSafe(row,1), curEmail = getSafe(row,2), curPhone = getSafe(row,3);
        String curDept = getSafe(row,4), curCollege = getSafe(row,5), curYear = getSafe(row,6);
        String curEvents = getSafe(row,7);
        String curAttendance = getSafe(row,8);

        JTextField nameF = new JTextField(curName);
        JTextField emailF = new JTextField(curEmail);
        JTextField phoneF = new JTextField(curPhone);
        JTextField deptF = new JTextField(curDept);
        JTextField collegeF = new JTextField(curCollege);
        JTextField yearF = new JTextField(curYear);

        JPanel evPanel = new JPanel(new GridLayout(0,1));
        List<String> evList = eventHandler.loadEvents();
        List<JCheckBox> editCBs = new ArrayList<>();
        for (String ev : evList) {
            JCheckBox cb = new JCheckBox(ev);
            if (curEvents != null && curEvents.contains(ev)) cb.setSelected(true);
            editCBs.add(cb); evPanel.add(cb);
        }

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Name:")); panel.add(nameF);
        panel.add(new JLabel("Email:")); panel.add(emailF);
        panel.add(new JLabel("Phone:")); panel.add(phoneF);
        panel.add(new JLabel("Department:")); panel.add(deptF);
        panel.add(new JLabel("College:")); panel.add(collegeF);
        panel.add(new JLabel("Year:")); panel.add(yearF);
        panel.add(new JLabel("Events:")); panel.add(new JScrollPane(evPanel));

        int res = JOptionPane.showConfirmDialog(frame, panel, "Edit Participant: " + id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        List<String> sel = new ArrayList<>();
        for (JCheckBox cb : editCBs) if (cb.isSelected()) sel.add(cb.getText());
        String evs = String.join(" | ", sel);
        boolean ok = participantHandler.edit(id, nameF.getText().trim(), emailF.getText().trim(), phoneF.getText().trim(),
                deptF.getText().trim(), collegeF.getText().trim(), yearF.getText().trim(), evs, curAttendance);
        if (ok) reloadTable();
        JOptionPane.showMessageDialog(frame, ok ? "Updated." : "Update failed.");
    }

    private void doDelete() {
        int row = participantTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(frame, "Select a participant to delete."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        int c = JOptionPane.showConfirmDialog(frame, "Delete " + id + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        boolean ok = participantHandler.delete(id);
        if (ok) { reloadTable(); JOptionPane.showMessageDialog(frame, "Deleted."); }
        else JOptionPane.showMessageDialog(frame, "Delete failed.");
    }

    private void changeAttendance(String status) {
        int row = participantTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(frame, "Select a participant."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        boolean ok = participantHandler.markAttendance(id, status);
        if (ok) reloadTable();
    }

    // safe getter
    private String getSafe(int row, int col) {
        Object o = tableModel.getValueAt(row,col);
        return o == null ? "" : o.toString();
    }

    // split CSV line with basic quoting support
    private String[] splitCSV(String line) {
        List<String> parts = new ArrayList<>();
        if (line == null) return new String[0];
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') { cur.append('"'); i++; }
                else inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString()); cur.setLength(0);
            } else cur.append(c);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
}
