package com.mycompany.Hostel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.*;

/**
 * ReportService – Generates monthly and floor-level attendance reports.
 * Fixes: null-safe DB connection, proper CSV export, correct SQL,
 *        improved dialog styling.
 */
public class ReportService {

    // ── FULL MONTHLY REPORT (Warden) ──────────────────────────
    public static void generateReport() {
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return;

            String query =
                "SELECT s.name, s.enrollment_no, f.floor_name, " +
                "COUNT(CASE WHEN a.status='Present' THEN 1 END) AS present_days, " +
                "COUNT(*) AS total_days, " +
                "ROUND(COUNT(CASE WHEN a.status='Present' THEN 1 END)*100.0/NULLIF(COUNT(*),0), 2) AS percentage " +
                "FROM Attendance a " +
                "JOIN Student s ON a.student_id=s.student_id " +
                "JOIN Room r ON s.room_id=r.room_id " +
                "JOIN Floor f ON r.floor_id=f.floor_id " +
                "WHERE MONTH(a.date)=MONTH(CURDATE()) AND YEAR(a.date)=YEAR(CURDATE()) " +
                "GROUP BY s.student_id, s.name, s.enrollment_no, f.floor_name " +
                "ORDER BY percentage ASC";

            ResultSet rs = con.createStatement().executeQuery(query);

            Month month = LocalDate.now().getMonth();
            int year = LocalDate.now().getYear();

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("   MONTHLY ATTENDANCE REPORT\n");
            sb.append("   Begum Azeezun Nisa Hall, AMU\n");
            sb.append("   Month: ").append(month.name()).append(" ").append(year).append("\n");
            sb.append("========================================\n\n");
            sb.append(String.format("%-28s %-14s %-22s %10s %7s\n",
                "Name","Enrollment","Floor","Present","  %"));
            sb.append("-".repeat(85)).append("\n");

            int defaulterCount = 0, totalStudents = 0;
            StringBuilder defaulters = new StringBuilder();

            while (rs.next()) {
                double pct     = rs.getDouble("percentage");
                int present    = rs.getInt("present_days");
                int total      = rs.getInt("total_days");
                totalStudents++;

                String flag = pct < 75 ? "  *** DEFAULTER" : "";
                if (pct < 75) {
                    defaulterCount++;
                    defaulters.append("  • ").append(rs.getString("name"))
                        .append(" (").append(rs.getString("enrollment_no"))
                        .append(") — ").append(String.format("%.1f", pct)).append("%\n");
                }

                sb.append(String.format("%-28s %-14s %-22s %3d/%-5d %5.1f%%%s\n",
                    rs.getString("name"), rs.getString("enrollment_no"),
                    rs.getString("floor_name"), present, total, pct, flag));
            }

            sb.append("\n========================================\n");
            sb.append("SUMMARY\n");
            sb.append("Total Students : ").append(totalStudents).append("\n");
            sb.append("Defaulters (<75%): ").append(defaulterCount).append("\n");
            sb.append("========================================\n");
            if (defaulterCount > 0)
                sb.append("\nDEFAULTERS:\n").append(defaulters);

            showReport(sb.toString(), "Monthly Attendance Report — " + month + " " + year);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error generating report: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── FLOOR REPORT (HeadGirl / Warden per floor) ────────────
    public static void generateFloorReport(int floorId) {
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return;

            PreparedStatement fpq = con.prepareStatement(
                "SELECT floor_name FROM Floor WHERE floor_id=?");
            fpq.setInt(1, floorId);
            ResultSet frs = fpq.executeQuery();
            String floorName = frs.next() ? frs.getString("floor_name") : "Floor #" + floorId;

            String query =
                "SELECT s.name, s.enrollment_no, " +
                "COUNT(CASE WHEN a.status='Present' THEN 1 END) AS present_days, " +
                "COUNT(*) AS total_days, " +
                "ROUND(COUNT(CASE WHEN a.status='Present' THEN 1 END)*100.0/NULLIF(COUNT(*),0), 2) AS percentage " +
                "FROM Attendance a " +
                "JOIN Student s ON a.student_id=s.student_id " +
                "JOIN Room r ON s.room_id=r.room_id " +
                "WHERE r.floor_id=? " +
                "AND MONTH(a.date)=MONTH(CURDATE()) AND YEAR(a.date)=YEAR(CURDATE()) " +
                "GROUP BY s.student_id, s.name, s.enrollment_no " +
                "ORDER BY percentage ASC";

            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, floorId);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("  FLOOR REPORT: ").append(floorName).append("\n");
            sb.append("  Month: ").append(LocalDate.now().getMonth()).append(" ").append(LocalDate.now().getYear()).append("\n");
            sb.append("========================================\n\n");
            sb.append(String.format("%-28s %-14s %7s %8s\n","Name","Enrollment","Present","%"));
            sb.append("-".repeat(62)).append("\n");

            while (rs.next()) {
                double pct  = rs.getDouble("percentage");
                int present = rs.getInt("present_days");
                int total   = rs.getInt("total_days");
                String flag = pct < 75 ? "  *** DEFAULTER" : "";
                sb.append(String.format("%-28s %-14s %3d/%-3d  %5.1f%%%s\n",
                    rs.getString("name"), rs.getString("enrollment_no"),
                    present, total, pct, flag));
            }

            showReport(sb.toString(), "Floor Report — " + floorName);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error generating floor report: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── DISPLAY REPORT ────────────────────────────────────────
    private static void showReport(String content, String title) {
        // Dark-themed report dialog
        JDialog dlg = new JDialog((Frame) null, title, true);
        dlg.setSize(720, 500);
        dlg.setLocationRelativeTo(null);
        dlg.getContentPane().setBackground(WardenDashboard.BG);
        dlg.setLayout(new BorderLayout(8, 8));

        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(WardenDashboard.CARD);
        area.setForeground(WardenDashboard.TEXT);
        area.setCaretColor(WardenDashboard.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
        scroll.getViewport().setBackground(WardenDashboard.CARD);
        dlg.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnPanel.setBackground(WardenDashboard.CARD);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1,0,0,0, WardenDashboard.BORDER));

        JButton exportBtn = WardenDashboard.accentBtn("📥 Export to CSV", WardenDashboard.ACCENT);
        JButton closeBtn  = WardenDashboard.accentBtn("✖ Close",           WardenDashboard.MUTED);
        exportBtn.setPreferredSize(new Dimension(160, 34));
        closeBtn.setPreferredSize(new Dimension(100, 34));

        btnPanel.add(exportBtn);
        btnPanel.add(closeBtn);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        exportBtn.addActionListener(e -> exportToCSV(content, title));
        closeBtn.addActionListener(e  -> dlg.dispose());

        dlg.setVisible(true);
    }

    // ── CSV EXPORT ────────────────────────────────────────────
    public static void exportToCSV(String data, String suggestedTitle) {
        JFileChooser chooser = new JFileChooser();
        String fname = suggestedTitle.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
        chooser.setSelectedFile(new File(fname));

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
            String[] lines = data.split("\n");

            // First pass: write header line from the formatted table header
            boolean headerWritten = false;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("=") || trimmed.startsWith("-")) continue;

                // Convert to CSV by splitting on 2+ spaces
                String csv = trimmed.replaceAll(" {2,}", ",").replaceAll(",+", ",");

                // Mark header (first data line after separators)
                if (!headerWritten) {
                    writer.write("sep=,\n");
                    headerWritten = true;
                }
                writer.write(csv + "\n");
            }

            JOptionPane.showMessageDialog(null,
                "Report exported successfully to:\n" + chooser.getSelectedFile().getAbsolutePath());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Export failed: " + e.getMessage());
        }
    }

    // Legacy overload (called from old code)
    public static void exportToCSV(String data) {
        exportToCSV(data, "attendance_report");
    }
}


//
//
//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.border.*;
//import java.awt.*;
//import java.io.*;
//import java.sql.*;
//import java.time.*;
//
//public class ReportService {
//
//    // ───────── MONTHLY REPORT ─────────
//    public static void generateReport() {
//
//        try (Connection con = DBConnection.getConnection()) {
//            if (con == null) {
//                JOptionPane.showMessageDialog(null, "Database not connected");
//                return;
//            }
//
//            Month month = LocalDate.now().getMonth();
//            int year = LocalDate.now().getYear();
//
//            StringBuilder sb = new StringBuilder();
//
//            sb.append("========================================\n");
//            sb.append("   MONTHLY ATTENDANCE REPORT\n");
//            sb.append("   Begum Azeezun Nisa Hall\n");
//            sb.append("   Month: ").append(month).append(" ").append(year).append("\n");
//            sb.append("========================================\n\n");
//
//            // ───────── STUDENT SUMMARY ─────────
//            String studentQuery =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, f.floor_name, " +
//                "COUNT(CASE WHEN a.status='Present' THEN 1 END) present_days, " +
//                "COUNT(*) total_days, " +
//                "ROUND(COUNT(CASE WHEN a.status='Present' THEN 1 END)*100.0/NULLIF(COUNT(*),0),2) percentage " +
//                "FROM Attendance a " +
//                "JOIN Student s ON a.student_id=s.student_id " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id " +
//                "JOIN Floor f ON r.floor_id=f.floor_id " +
//                "WHERE MONTH(a.date)=MONTH(CURDATE()) AND YEAR(a.date)=YEAR(CURDATE()) " +
//                "GROUP BY s.student_id";
//
//            ResultSet rs = con.createStatement().executeQuery(studentQuery);
//
//            sb.append("STUDENT SUMMARY\n");
//            sb.append(String.format("%-25s %-12s %-10s %-10s %-8s\n",
//                    "Name","Enroll","Floor","Present","%"));
//            sb.append("-".repeat(70)).append("\n");
//
//            StringBuilder defaulters = new StringBuilder();
//            int defCount = 0;
//
//            while (rs.next()) {
//
//                double pct = rs.getDouble("percentage");
//
//                if (pct < 75) {
//                    defCount++;
//                    defaulters.append("• ")
//                            .append(rs.getString("name"))
//                            .append(" (").append(rs.getString("enrollment_no"))
//                            .append(") - ").append(pct).append("%\n");
//                }
//
//                sb.append(String.format("%-25s %-12s %-10s %3d/%-3d %5.1f%%\n",
//                        rs.getString("name"),
//                        rs.getString("enrollment_no"),
//                        rs.getString("floor_name"),
//                        rs.getInt("present_days"),
//                        rs.getInt("total_days"),
//                        pct));
//            }
//
//            // ───────── DEFAULTERS ─────────
//            sb.append("\nDEFAULTERS (<75%)\n");
//            sb.append(defCount == 0 ? "None\n" : defaulters.toString());
//
//            // ───────── DATE-WISE SUMMARY ─────────
//            sb.append("\nDATE-WISE SUMMARY\n");
//
//            String dateQuery =
//                "SELECT date, " +
//                "COUNT(CASE WHEN status='Present' THEN 1 END) present, " +
//                "COUNT(*) total " +
//                "FROM Attendance " +
//                "WHERE MONTH(date)=MONTH(CURDATE()) AND YEAR(date)=YEAR(CURDATE()) " +
//                "GROUP BY date ORDER BY date";
//
//            ResultSet drs = con.createStatement().executeQuery(dateQuery);
//
//            while (drs.next()) {
//                sb.append(drs.getDate("date"))
//                  .append(" -> ")
//                  .append(drs.getInt("present"))
//                  .append("/")
//                  .append(drs.getInt("total"))
//                  .append("\n");
//            }
//
//            // ───────── FLOOR SUMMARY ─────────
//            sb.append("\nFLOOR-WISE SUMMARY\n");
//
//            String floorQuery =
//                "SELECT f.floor_name, " +
//                "COUNT(CASE WHEN a.status='Present' THEN 1 END) present, " +
//                "COUNT(*) total " +
//                "FROM Attendance a " +
//                "JOIN Student s ON a.student_id=s.student_id " +
//                "JOIN Room r ON s.room_id=r.room_id " +
//                "JOIN Floor f ON r.floor_id=f.floor_id " +
//                "WHERE MONTH(a.date)=MONTH(CURDATE()) AND YEAR(a.date)=YEAR(CURDATE()) " +
//                "GROUP BY f.floor_name";
//
//            ResultSet frs = con.createStatement().executeQuery(floorQuery);
//
//            while (frs.next()) {
//                sb.append(frs.getString("floor_name"))
//                  .append(" -> ")
//                  .append(frs.getInt("present"))
//                  .append("/")
//                  .append(frs.getInt("total"))
//                  .append("\n");
//            }
//
//            // ───────── CATEGORY SUMMARY ─────────
//            sb.append("\nCATEGORY-WISE SUMMARY\n");
//
//            String catQuery =
//                "SELECT c.category_name, " +
//                "COUNT(CASE WHEN a.status='Present' THEN 1 END) present, " +
//                "COUNT(*) total " +
//                "FROM Attendance a " +
//                "JOIN Student s ON a.student_id=s.student_id " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "WHERE MONTH(a.date)=MONTH(CURDATE()) AND YEAR(a.date)=YEAR(CURDATE()) " +
//                "GROUP BY c.category_name";
//
//            ResultSet crs = con.createStatement().executeQuery(catQuery);
//
//            while (crs.next()) {
//                sb.append(crs.getString("category_name"))
//                  .append(" -> ")
//                  .append(crs.getInt("present"))
//                  .append("/")
//                  .append(crs.getInt("total"))
//                  .append("\n");
//            }
//
//            // ───────── SAFE HEAD GIRL SECTION ─────────
//            sb.append("\nHEAD GIRL ACTIVITY\n");
//
//            try {
//                String hgQuery =
//                    "SELECT a.date, u.name " +
//                    "FROM Attendance a " +
//                    "JOIN User u ON a.marked_by=u.user_id " +
//                    "GROUP BY a.date, u.name ORDER BY a.date";
//
//                ResultSet hrs = con.createStatement().executeQuery(hgQuery);
//
//                while (hrs.next()) {
//                    sb.append(hrs.getDate("date"))
//                      .append(" -> ")
//                      .append(hrs.getString("name"))
//                      .append("\n");
//                }
//
//            } catch (Exception e) {
//                sb.append("Head Girl tracking not available.\n");
//            }
//
//            showReport(sb.toString(), "Monthly Report");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
//        }
//    }
//
//    // ───────── UI DISPLAY ─────────
//    private static void showReport(String content, String title) {
//
//        JDialog dlg = new JDialog((Frame) null, title, true);
//        dlg.setSize(720, 500);
//        dlg.setLocationRelativeTo(null);
//        dlg.setLayout(new BorderLayout());
//
//        JTextArea area = new JTextArea(content);
//        area.setEditable(false);
//        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
//
//        JScrollPane scroll = new JScrollPane(area);
//        dlg.add(scroll, BorderLayout.CENTER);
//
//        JPanel panel = new JPanel();
//
//        JButton export = new JButton("Export CSV");
//        JButton close  = new JButton("Close");
//
//        panel.add(export);
//        panel.add(close);
//
//        dlg.add(panel, BorderLayout.SOUTH);
//
//        export.addActionListener(e -> exportToCSV(content));
//        close.addActionListener(e -> dlg.dispose());
//
//        dlg.setVisible(true);
//    }
//
//    // ───────── CSV EXPORT ─────────
//    public static void exportToCSV(String data) {
//
//        JFileChooser chooser = new JFileChooser();
//        chooser.setSelectedFile(new File("report.csv"));
//
//        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
//
//        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//
//            for (String line : data.split("\n")) {
//                writer.write(line.replaceAll(" {2,}", ",") + "\n");
//            }
//
//            JOptionPane.showMessageDialog(null, "Exported Successfully!");
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null, "Export Failed: " + e.getMessage());
//        }
//    }
//}