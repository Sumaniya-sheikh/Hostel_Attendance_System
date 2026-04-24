package com.mycompany.Hostel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;

public class ManageStudents extends JFrame {

    private JTable table;
    private DefaultTableModel model;

    private JTextField nameField, enrollField, contactField;
    private JComboBox<String> categoryCombo, roomCombo;

    private final HashMap<String, Integer> categoryMap = new HashMap<>();
    private final HashMap<String, Integer> roomMap = new HashMap<>();

    private final int floorId;

    public ManageStudents() {
        this(-1);
    }

    public ManageStudents(int floorId) {
        this.floorId = floorId;

        setTitle("Manage Students");
        setSize(980, 620);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(WardenDashboard.BG);
        setLayout(new BorderLayout());

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        loadDropdowns();
        loadStudents();

        setVisible(true);
    }

    // ================= FORM =================
    private JPanel buildFormPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WardenDashboard.CARD);
        panel.setBorder(BorderFactory.createMatteBorder(0,0,1,0, WardenDashboard.BORDER));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
        form.setBackground(WardenDashboard.CARD);

        nameField = styledField(12);
        enrollField = styledField(12);
        contactField = styledField(10);

        categoryCombo = new JComboBox<>();
        roomCombo = new JComboBox<>();

        styleCombo(categoryCombo);
        styleCombo(roomCombo);

        form.add(lbl("Name")); form.add(nameField);
        form.add(lbl("Enroll")); form.add(enrollField);
        form.add(lbl("Contact")); form.add(contactField);
        form.add(lbl("Category")); form.add(categoryCombo);
        form.add(lbl("Room")); form.add(roomCombo);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
        btnRow.setBackground(WardenDashboard.CARD);

        JButton addBtn = WardenDashboard.accentBtn("➕ Add", WardenDashboard.SUCCESS);
        JButton editBtn = WardenDashboard.accentBtn("✏️ Edit", WardenDashboard.ACCENT);
        JButton deleteBtn = WardenDashboard.accentBtn("🗑 Delete", WardenDashboard.DANGER);

        btnRow.add(addBtn);
        btnRow.add(editBtn);
        btnRow.add(deleteBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnRow, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addStudent());
        editBtn.addActionListener(e -> editStudent());
        deleteBtn.addActionListener(e -> deleteStudent());

        return panel;
    }

    // ================= TABLE =================
    private JPanel buildTablePanel() {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WardenDashboard.BG);
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        model = new DefaultTableModel(
            new String[]{"ID","Name","Enrollment","Category","Room"},0
        );

        table = new JTable(model);

        table.setRowHeight(28);
        table.setBackground(WardenDashboard.ROW_EVEN);
        table.setForeground(WardenDashboard.TEXT);
        table.setGridColor(WardenDashboard.BORDER);

        // hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        // AUTO FILL
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if(row >= 0){
                nameField.setText(model.getValueAt(row,1).toString());
                enrollField.setText(model.getValueAt(row,2).toString());
                categoryCombo.setSelectedItem(model.getValueAt(row,3));
                roomCombo.setSelectedItem("Room " + model.getValueAt(row,4).toString());
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ================= LOAD =================
    private void loadDropdowns() {
        try(Connection con = DBConnection.getConnection()){

            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
            while(cat.next()){
                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
                categoryCombo.addItem(cat.getString("category_name"));
            }

            String sql = (floorId==-1)
                ? "SELECT * FROM Room"
                : "SELECT * FROM Room WHERE floor_id="+floorId;

            ResultSet room = con.createStatement().executeQuery(sql);
            while(room.next()){
                String r = "Room " + room.getString("room_number");
                roomMap.put(r, room.getInt("room_id"));
                roomCombo.addItem(r);
            }

        }catch(Exception e){ e.printStackTrace(); }
    }

    private void loadStudents(){
        model.setRowCount(0);

        try(Connection con = DBConnection.getConnection()){

            String sql =
                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number " +
                "FROM Student s " +
                "JOIN Category c ON s.category_id=c.category_id " +
                "JOIN Room r ON s.room_id=r.room_id";

            if(floorId!=-1) sql += " WHERE r.floor_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            if(floorId!=-1) ps.setInt(1,floorId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)
                });
            }

        }catch(Exception e){ e.printStackTrace(); }
    }

    // ================= ADD =================
    private void addStudent(){
        try(Connection con = DBConnection.getConnection()){

            String name = nameField.getText().trim();
            String enroll = enrollField.getText().trim();

            if(name.isEmpty() || enroll.isEmpty()){
                JOptionPane.showMessageDialog(this,"Name & Enrollment required!");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Student(name,enrollment_no,category_id,room_id) VALUES(?,?,?,?)"
            );

            ps.setString(1,name);
            ps.setString(2,enroll);
            ps.setInt(3,categoryMap.get(categoryCombo.getSelectedItem()));
            ps.setInt(4,roomMap.get(roomCombo.getSelectedItem()));

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,"Student Added");
            clear();
            loadStudents();

        }catch(Exception e){
            JOptionPane.showMessageDialog(this,e.getMessage());
        }
    }

    // ================= EDIT =================
    private void editStudent(){
        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select student");
            return;
        }

        String name = nameField.getText().trim();
        String enroll = enrollField.getText().trim();

        if(name.isEmpty() || enroll.isEmpty()){
            JOptionPane.showMessageDialog(this,"Fields cannot be empty!");
            return;
        }

        try(Connection con = DBConnection.getConnection()){

            PreparedStatement ps = con.prepareStatement(
                "UPDATE Student SET name=?, enrollment_no=?, category_id=?, room_id=? WHERE student_id=?"
            );

            ps.setString(1,name);
            ps.setString(2,enroll);
            ps.setInt(3,categoryMap.get(categoryCombo.getSelectedItem()));
            ps.setInt(4,roomMap.get(roomCombo.getSelectedItem()));
            ps.setInt(5,(int)model.getValueAt(row,0));

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,"Updated");
            loadStudents();

        }catch(Exception e){
            JOptionPane.showMessageDialog(this,e.getMessage());
        }
    }

    // ================= DELETE =================
    private void deleteStudent(){
        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select student");
            return;
        }

        int id = (int) model.getValueAt(row,0);

        try(Connection con = DBConnection.getConnection()){

            // remove attendance first
            PreparedStatement ps1 = con.prepareStatement(
                "DELETE FROM Attendance WHERE student_id=?"
            );
            ps1.setInt(1,id);
            ps1.executeUpdate();

            // remove student
            PreparedStatement ps2 = con.prepareStatement(
                "DELETE FROM Student WHERE student_id=?"
            );
            ps2.setInt(1,id);
            ps2.executeUpdate();

            JOptionPane.showMessageDialog(this,"Deleted");
            loadStudents();

        }catch(Exception e){
            JOptionPane.showMessageDialog(this,e.getMessage());
        }
    }

    private void clear(){
        nameField.setText("");
        enrollField.setText("");
        contactField.setText("");
    }

    // ================= UI =================
    private JTextField styledField(int cols){
        JTextField f = new JTextField(cols);
        f.setBackground(new Color(0x0F1B2D));
        f.setForeground(WardenDashboard.TEXT);
        f.setCaretColor(WardenDashboard.TEXT);
        f.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
        return f;
    }
private void styleCombo(JComboBox<String> cb) {

    Color bg = new Color(0x0F1B2D);
    Color border = WardenDashboard.BORDER;

    // Closed box base style
    cb.setBackground(bg);
    cb.setForeground(Color.black);
    cb.setOpaque(true);

    // 🔥 FIX 1: Make the closed text editor (WHITE BOX) dark
    if (cb.getEditor() != null && cb.getEditor().getEditorComponent() instanceof JTextField editor) {
        editor.setBackground(bg);
        editor.setForeground(Color.WHITE);
        editor.setCaretColor(Color.WHITE);
        editor.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
    }

    // 🔥 FIX 2: Arrow button styling
    cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
        @Override
        protected JButton createArrowButton() {
            JButton button = super.createArrowButton();
            button.setBackground(bg);
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }
    });

    // 🔥 FIX 3: Dropdown list styling
    cb.setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(new Color(0x2D4A6B));
            } else {
                label.setBackground(bg);
            }

            label.setForeground(Color.WHITE);

            return label;
        }
    });

    // Border
    cb.setBorder(BorderFactory.createLineBorder(border));
}
    
//    private void styleCombo(JComboBox<String> cb) {
//
//    Color bg = new Color(0x0F1B2D);
//
//    // Closed box fix
//    cb.setBackground(bg);
//    cb.setForeground(Color.WHITE);
//
//    // Force UI background (IMPORTANT)
//    cb.setOpaque(true);
//
//    // Fix arrow button
//    cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
//        @Override
//        protected JButton createArrowButton() {
//            JButton button = super.createArrowButton();
//            button.setBackground(bg);
//            button.setBorder(BorderFactory.createEmptyBorder());
//            return button;
//        }
//    });
//
//    // FIX: renderer for dropdown list
//    cb.setRenderer(new DefaultListCellRenderer() {
//        @Override
//        public Component getListCellRendererComponent(
//                JList<?> list, Object value, int index,
//                boolean isSelected, boolean cellHasFocus) {
//
//            JLabel label = (JLabel) super.getListCellRendererComponent(
//                    list, value, index, isSelected, cellHasFocus);
//
//            if (isSelected) {
//                label.setBackground(new Color(0x2D4A6B));
//            } else {
//                label.setBackground(bg);
//            }
//
//            label.setForeground(Color.WHITE);
//            label.setOpaque(true);
//            return label;
//        }
//    });
//
//    cb.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
//}
//    
    
//    private void styleCombo(JComboBox<String> cb){
//        cb.setBackground(new Color(0x0F1B2D));
//        cb.setForeground(WardenDashboard.TEXT);
//    }

//    private void styleCombo(JComboBox<String> cb){
//
//    // Closed combo (selected value)
//    cb.setBackground(new Color(0x0F1B2D));
//    cb.setForeground(Color.WHITE); // ✅ make text clearly visible
//
//    // Dropdown list styling
//    cb.setRenderer(new DefaultListCellRenderer() {
//        @Override
//        public Component getListCellRendererComponent(
//                JList<?> list, Object value, int index,
//                boolean isSelected, boolean cellHasFocus) {
//
//            JLabel label = (JLabel) super.getListCellRendererComponent(
//                    list, value, index, isSelected, cellHasFocus);
//
//            if (isSelected) {
//                label.setBackground(new Color(0x2D4A6B)); // ✅ highlight (visible)
//                label.setForeground(Color.WHITE);         // ✅ white text
//            } else {
//                label.setBackground(new Color(0x0F1B2D)); // dark background
//                label.setForeground(Color.WHITE);         // ✅ FIX: force white text
//            }
//
//            return label;
//        }
//    });
//    // Optional: improve border visibility
//    cb.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
//}
//    private void styleCombo(JComboBox<String> cb){
//
//    // Selected item (closed box)
//    cb.setBackground(new Color(0x0F1B2D));
//    cb.setForeground(WardenDashboard.TEXT);
//
//    // Dropdown list styling
//    cb.setRenderer(new DefaultListCellRenderer() {
//        @Override
//        public Component getListCellRendererComponent(
//                JList<?> list, Object value, int index,
//                boolean isSelected, boolean cellHasFocus) {
//
//            Component c = super.getListCellRendererComponent(
//                    list, value, index, isSelected, cellHasFocus);
//
//            if (isSelected) {
//                c.setBackground(new Color(0x0F1B2D)); // selection color
//                c.setForeground(Color.WHITE);
//            } else {
//                c.setBackground(new Color(0x0F1B2D)); // dropdown background
//                c.setForeground(WardenDashboard.TEXT); // text color
//            }
//            return c;
//        }
//    });
//}
    private JLabel lbl(String t){
        JLabel l = new JLabel(t);
        l.setForeground(WardenDashboard.MUTED);
        return l;
    }
}
//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.border.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField, contactField;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;
//
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle("Manage Students");
//        setSize(980, 620);
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setLocationRelativeTo(null);
//        getContentPane().setBackground(WardenDashboard.BG);
//        setLayout(new BorderLayout());
//
//        add(buildFormPanel(), BorderLayout.NORTH);
//        add(buildTablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        loadStudents();
//
//        setVisible(true);
//    }
//
//    // ───────────────── FORM PANEL ─────────────────
//    private JPanel buildFormPanel() {
//
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBackground(WardenDashboard.CARD);
//        panel.setBorder(BorderFactory.createMatteBorder(0,0,1,0, WardenDashboard.BORDER));
//
//        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
//        form.setBackground(WardenDashboard.CARD);
//
//        nameField = styledField(12);
//        enrollField = styledField(12);
//        contactField = styledField(10);
//
//        categoryCombo = new JComboBox<>();
//        roomCombo = new JComboBox<>();
//
//        styleCombo(categoryCombo);
//        styleCombo(roomCombo);
//
//        form.add(lbl("Name")); form.add(nameField);
//        form.add(lbl("Enroll")); form.add(enrollField);
//        form.add(lbl("Contact")); form.add(contactField);
//        form.add(lbl("Category")); form.add(categoryCombo);
//        form.add(lbl("Room")); form.add(roomCombo);
//
//        // BUTTONS
//        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
//        btnRow.setBackground(WardenDashboard.CARD);
//
//        JButton addBtn = WardenDashboard.accentBtn("➕ Add", WardenDashboard.SUCCESS);
//        JButton editBtn = WardenDashboard.accentBtn("✏️ Edit", WardenDashboard.ACCENT);
//        JButton deleteBtn = WardenDashboard.accentBtn("🗑 Delete", WardenDashboard.DANGER);
//
//        btnRow.add(addBtn);
//        btnRow.add(editBtn);
//        btnRow.add(deleteBtn);
//
//        panel.add(form, BorderLayout.NORTH);
//        panel.add(btnRow, BorderLayout.SOUTH);
//
//        // ACTIONS
//        addBtn.addActionListener(e -> addStudent());
//        editBtn.addActionListener(e -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//
//        return panel;
//    }
//
//    // ───────────────── TABLE PANEL ─────────────────
//    private JPanel buildTablePanel() {
//
//        JPanel p = new JPanel(new BorderLayout());
//        p.setBackground(WardenDashboard.BG);
//        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
//
//        model = new DefaultTableModel(
//            new String[]{"ID","Name","Enrollment","Category","Room"},0
//        );
//
//        table = new JTable(model);
//
//        table.setRowHeight(28);
//        table.setBackground(WardenDashboard.ROW_EVEN);
//        table.setForeground(WardenDashboard.TEXT);
//        table.setGridColor(WardenDashboard.BORDER);
//
//        // hide ID
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        // ✅ AUTO-FILL FIX
//        table.getSelectionModel().addListSelectionListener(e -> {
//            int row = table.getSelectedRow();
//            if(row >= 0){
//                nameField.setText(model.getValueAt(row,1).toString());
//                enrollField.setText(model.getValueAt(row,2).toString());
//                categoryCombo.setSelectedItem(model.getValueAt(row,3));
//                roomCombo.setSelectedItem("Room " + model.getValueAt(row,4));
//            }
//        });
//
//        JScrollPane scroll = new JScrollPane(table);
//        p.add(scroll, BorderLayout.CENTER);
//
//        return p;
//    }
//
//    // ───────────────── LOAD DATA ─────────────────
//    private void loadDropdowns() {
//        try(Connection con = DBConnection.getConnection()){
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while(cat.next()){
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String sql = (floorId==-1)
//                ? "SELECT * FROM Room"
//                : "SELECT * FROM Room WHERE floor_id="+floorId;
//
//            ResultSet room = con.createStatement().executeQuery(sql);
//            while(room.next()){
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//
//        }catch(Exception e){ e.printStackTrace(); }
//    }
//
//    private void loadStudents(){
//        model.setRowCount(0);
//
//        try(Connection con = DBConnection.getConnection()){
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id ";
//
//            if(floorId!=-1) sql += "WHERE r.floor_id=?";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//
//            if(floorId!=-1) ps.setInt(1,floorId);
//
//            ResultSet rs = ps.executeQuery();
//
//            while(rs.next()){
//                model.addRow(new Object[]{
//                    rs.getInt(1),
//                    rs.getString(2),
//                    rs.getString(3),
//                    rs.getString(4),
//                    rs.getString(5)
//                });
//            }
//
//        }catch(Exception e){ e.printStackTrace(); }
//    }
//
//    // ───────────────── ADD ─────────────────
//    private void addStudent(){
//        try(Connection con = DBConnection.getConnection()){
//
//            PreparedStatement ps = con.prepareStatement(
//                "INSERT INTO Student(name,enrollment_no,category_id,room_id) VALUES(?,?,?,?)"
//            );
//
//            ps.setString(1,nameField.getText());
//            ps.setString(2,enrollField.getText());
//            ps.setInt(3,categoryMap.get(categoryCombo.getSelectedItem()));
//            ps.setInt(4,roomMap.get(roomCombo.getSelectedItem()));
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this,"Student Added");
//            clear();
//            loadStudents();
//
//        }catch(Exception e){
//            JOptionPane.showMessageDialog(this,e.getMessage());
//        }
//    }
//
//    // ───────────────── EDIT ─────────────────
//    private void editStudent(){
//        int row = table.getSelectedRow();
//        if(row<0){ JOptionPane.showMessageDialog(this,"Select student"); return; }
//
//        try(Connection con = DBConnection.getConnection()){
//
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET name=?, enrollment_no=? WHERE student_id=?"
//            );
//
//            ps.setString(1,nameField.getText());
//            ps.setString(2,enrollField.getText());
//            ps.setInt(3,(int)model.getValueAt(row,0));
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this,"Updated");
//            loadStudents();
//
//        }catch(Exception e){ e.printStackTrace(); }
//    }
//
//    // ───────────────── DELETE ─────────────────
//    private void deleteStudent(){
//        int row = table.getSelectedRow();
//        if(row<0){ JOptionPane.showMessageDialog(this,"Select student"); return; }
//
//        int id = (int) model.getValueAt(row,0);
//
//        try(Connection con = DBConnection.getConnection()){
//
//            // delete attendance first
//            PreparedStatement ps1 = con.prepareStatement(
//                "DELETE FROM Attendance WHERE student_id=?"
//            );
//            ps1.setInt(1,id);
//            ps1.executeUpdate();
//
//            // delete student
//            PreparedStatement ps2 = con.prepareStatement(
//                "DELETE FROM Student WHERE student_id=?"
//            );
//            ps2.setInt(1,id);
//            ps2.executeUpdate();
//
//            JOptionPane.showMessageDialog(this,"Deleted");
//            loadStudents();
//
//        }catch(Exception e){
//            JOptionPane.showMessageDialog(this,e.getMessage());
//        }
//    }
//
//    private void clear(){
//        nameField.setText("");
//        enrollField.setText("");
//        contactField.setText("");
//    }
//
//    // ───────────────── UI HELPERS ─────────────────
//    private JTextField styledField(int cols){
//        JTextField f = new JTextField(cols);
//        f.setBackground(new Color(0x0F1B2D));
//        f.setForeground(WardenDashboard.TEXT);
//        f.setCaretColor(WardenDashboard.TEXT);
//        f.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
//        return f;
//    }
//
//    private void styleCombo(JComboBox<String> cb){
//        cb.setBackground(new Color(0x0F1B2D));
//        cb.setForeground(WardenDashboard.TEXT);
//    }
//
//    private JLabel lbl(String t){
//        JLabel l = new JLabel(t);
//        l.setForeground(WardenDashboard.MUTED);
//        return l;
//    }
//}


//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;
//
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle("Manage Students");
//        setSize(900, 550);
//        setLayout(new BorderLayout());
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//
//        add(topPanel(), BorderLayout.NORTH);
//        add(tablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        loadStudents();
//
//        setLocationRelativeTo(null);
//        setVisible(true);
//    }
//
//    // ─────────────── TOP PANEL ───────────────
//    private JPanel topPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//
//        JPanel form = new JPanel();
//
//        nameField   = new JTextField(10);
//        enrollField = new JTextField(10);
//
//        categoryCombo = new JComboBox<>();
//        roomCombo     = new JComboBox<>();
//
//        form.add(new JLabel("Name:"));
//        form.add(nameField);
//
//        form.add(new JLabel("Enroll:"));
//        form.add(enrollField);
//
//        form.add(new JLabel("Category:"));
//        form.add(categoryCombo);
//
//        form.add(new JLabel("Room:"));
//        form.add(roomCombo);
//
//        panel.add(form, BorderLayout.NORTH);
//
//        // BUTTONS
//        JPanel buttons = new JPanel();
//
//        JButton addBtn    = new JButton("Add");
//        JButton editBtn   = new JButton("Edit");
//        JButton deleteBtn = new JButton("Delete");
//
//        buttons.add(addBtn);
//        buttons.add(editBtn);
//        buttons.add(deleteBtn);
//
//        panel.add(buttons, BorderLayout.SOUTH);
//
//        // ACTIONS
//        addBtn.addActionListener(e -> addStudent());
//        editBtn.addActionListener(e -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//
//        return panel;
//    }
//
//    // ─────────────── TABLE ───────────────
//    private JScrollPane tablePanel() {
//
//        model = new DefaultTableModel(
//                new String[]{"ID", "Name", "Enrollment", "Category", "Room"}, 0
//        );
//
//        table = new JTable(model);
//
//        // Hide ID column
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        // ✅ AUTO-FILL ON CLICK (CRITICAL FIX)
//        table.getSelectionModel().addListSelectionListener(e -> {
//            int row = table.getSelectedRow();
//
//            if (row >= 0) {
//                nameField.setText(model.getValueAt(row, 1).toString());
//                enrollField.setText(model.getValueAt(row, 2).toString());
//
//                categoryCombo.setSelectedItem(model.getValueAt(row, 3));
//                roomCombo.setSelectedItem("Room " + model.getValueAt(row, 4));
//            }
//        });
//
//        return new JScrollPane(table);
//    }
//
//    // ─────────────── LOAD DROPDOWNS ───────────────
//    private void loadDropdowns() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while (cat.next()) {
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String sql = (floorId == -1)
//                    ? "SELECT * FROM Room"
//                    : "SELECT * FROM Room WHERE floor_id=" + floorId;
//
//            ResultSet room = con.createStatement().executeQuery(sql);
//            while (room.next()) {
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ─────────────── LOAD STUDENTS ───────────────
//    private void loadStudents() {
//        model.setRowCount(0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id ";
//
//            if (floorId != -1) sql += "WHERE r.floor_id=?";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//
//            if (floorId != -1) ps.setInt(1, floorId);
//
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                        rs.getInt(1),
//                        rs.getString(2),
//                        rs.getString(3),
//                        rs.getString(4),
//                        rs.getString(5)
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ─────────────── ADD ───────────────
//    private void addStudent() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            if (nameField.getText().isEmpty() || enrollField.getText().isEmpty()) {
//                JOptionPane.showMessageDialog(this, "Fill all fields");
//                return;
//            }
//
//            PreparedStatement ps = con.prepareStatement(
//                "INSERT INTO Student(name, enrollment_no, category_id, room_id) VALUES(?,?,?,?)"
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, categoryMap.get(categoryCombo.getSelectedItem()));
//            ps.setInt(4, roomMap.get(roomCombo.getSelectedItem()));
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Student Added");
//
//            clear();
//            loadStudents();
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, e.getMessage());
//        }
//    }
//
//    // ─────────────── EDIT ───────────────
//    private void editStudent() {
//        int row = table.getSelectedRow();
//
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select student");
//            return;
//        }
//
//        String name = nameField.getText().trim();
//        String enroll = enrollField.getText().trim();
//
//        if (name.isEmpty() || enroll.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Fields cannot be empty");
//            return;
//        }
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET name=?, enrollment_no=? WHERE student_id=?"
//            );
//
//            ps.setString(1, name);
//            ps.setString(2, enroll);
//            ps.setInt(3, id);
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Updated");
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
//    
//    private void deleteStudent() {
//    int row = table.getSelectedRow();
//
//    if (row < 0) {
//        JOptionPane.showMessageDialog(this, "Select student");
//        return;
//    }
//
//    int confirm = JOptionPane.showConfirmDialog(this,
//            "Are you sure you want to delete?",
//            "Confirm",
//            JOptionPane.YES_NO_OPTION);
//
//    if (confirm != JOptionPane.YES_OPTION) return;
//
//    int id = (int) model.getValueAt(row, 0);
//
//    try (Connection con = DBConnection.getConnection()) {
//
//        // ✅ DELETE attendance first (IMPORTANT)
//        PreparedStatement ps1 = con.prepareStatement(
//                "DELETE FROM Attendance WHERE student_id=?"
//        );
//        ps1.setInt(1, id);
//        ps1.executeUpdate();
//
//        // ✅ THEN delete student
//        PreparedStatement ps2 = con.prepareStatement(
//                "DELETE FROM Student WHERE student_id=?"
//        );
//        ps2.setInt(1, id);
//        ps2.executeUpdate();
//
//        JOptionPane.showMessageDialog(this, "Deleted Successfully");
//
//        loadStudents();
//
//    } catch (Exception e) {
//        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//    }
//}

    // ─────────────── DELETE ───────────────
//    private void deleteStudent() {
//        int row = table.getSelectedRow();
//
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select student");
//            return;
//        }
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            con.createStatement().executeUpdate(
//                "DELETE FROM Student WHERE student_id=" + id
//            );
//
//            JOptionPane.showMessageDialog(this, "Deleted");
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // ─────────────── CLEAR ───────────────
//    private void clear() {
//        nameField.setText("");
//        enrollField.setText("");
//    }
//}
//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;
//
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle("Manage Students");
//        setSize(900, 550);
//        setLayout(new BorderLayout());
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//
//        add(topPanel(), BorderLayout.NORTH);
//        add(tablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        loadStudents();
//
//        setLocationRelativeTo(null);
//        setVisible(true);
//    }
//
//    // ───────────────────────── TOP PANEL ─────────────────────────
//    private JPanel topPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//
//        JPanel form = new JPanel();
//
//        nameField   = new JTextField(10);
//        enrollField = new JTextField(10);
//
//        categoryCombo = new JComboBox<>();
//        roomCombo     = new JComboBox<>();
//
//        form.add(new JLabel("Name:"));
//        form.add(nameField);
//
//        form.add(new JLabel("Enroll:"));
//        form.add(enrollField);
//
//        form.add(new JLabel("Category:"));
//        form.add(categoryCombo);
//
//        form.add(new JLabel("Room:"));
//        form.add(roomCombo);
//
//        panel.add(form, BorderLayout.NORTH);
//
//        // BUTTON ROW (FIXED)
//        JPanel buttons = new JPanel();
//
//        JButton addBtn    = new JButton("Add");
//        JButton editBtn   = new JButton("Edit");
//        JButton deleteBtn = new JButton("Delete");
//
//        buttons.add(addBtn);
//        buttons.add(editBtn);
//        buttons.add(deleteBtn);
//
//        panel.add(buttons, BorderLayout.SOUTH);
//
//        // ACTIONS
//        addBtn.addActionListener(e -> addStudent());
//        editBtn.addActionListener(e -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//
//        return panel;
//    }
//
//    // ───────────────────────── TABLE ─────────────────────────
//    private JScrollPane tablePanel() {
//
//        model = new DefaultTableModel(
//                new String[]{"ID", "Name", "Enrollment", "Category", "Room"}, 0
//        );
//
//        table = new JTable(model);
//
//        // Hide ID column
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        return new JScrollPane(table);
//    }
//
//    // ───────────────────────── LOAD DATA ─────────────────────────
//    private void loadDropdowns() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while (cat.next()) {
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String sql = (floorId == -1)
//                    ? "SELECT * FROM Room"
//                    : "SELECT * FROM Room WHERE floor_id=" + floorId;
//
//            ResultSet room = con.createStatement().executeQuery(sql);
//            while (room.next()) {
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void loadStudents() {
//        model.setRowCount(0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id ";
//
//            if (floorId != -1) sql += "WHERE r.floor_id=?";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//
//            if (floorId != -1) ps.setInt(1, floorId);
//
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                        rs.getInt(1),
//                        rs.getString(2),
//                        rs.getString(3),
//                        rs.getString(4),
//                        rs.getString(5)
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────────────────────── CRUD ─────────────────────────
//    private void addStudent() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            if (nameField.getText().isEmpty() || enrollField.getText().isEmpty()) {
//                JOptionPane.showMessageDialog(this, "Fill all fields");
//                return;
//            }
//
//            PreparedStatement ps = con.prepareStatement(
//                "INSERT INTO Student(name, enrollment_no, category_id, room_id) VALUES(?,?,?,?)"
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, categoryMap.get(categoryCombo.getSelectedItem()));
//            ps.setInt(4, roomMap.get(roomCombo.getSelectedItem()));
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Student Added");
//
//            clear();
//            loadStudents();
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, e.getMessage());
//        }
//    }
//
//    private void editStudent() {
//        int row = table.getSelectedRow();
//
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select student");
//            return;
//        }
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET name=?, enrollment_no=? WHERE student_id=?"
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, id);
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Updated");
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void deleteStudent() {
//        int row = table.getSelectedRow();
//
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select student");
//            return;
//        }
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            con.createStatement().executeUpdate(
//                "DELETE FROM Student WHERE student_id=" + id
//            );
//
//            JOptionPane.showMessageDialog(this, "Deleted");
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void clear() {
//        nameField.setText("");
//        enrollField.setText("");
//    }
//}

//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField, contactField;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;
//
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle("Manage Students");
//        setSize(900, 600);
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setLocationRelativeTo(null);
//        setLayout(new BorderLayout());
//
//        add(buildFormPanel(), BorderLayout.NORTH);
//        add(buildTablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        enableRoomSearch();
//        loadStudents();
//
//        setVisible(true);
//    }
//
//    // ───────── FORM ─────────
//    private JPanel buildFormPanel() {
//
//        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
//
//        nameField    = new JTextField(12);
//        enrollField  = new JTextField(12);
//        contactField = new JTextField(10);
//
//        categoryCombo = new JComboBox<>();
//        roomCombo     = new JComboBox<>();
//        roomCombo.setEditable(true);
//
//        panel.add(new JLabel("Name:"));       panel.add(nameField);
//        panel.add(new JLabel("Enrollment:")); panel.add(enrollField);
//        panel.add(new JLabel("Contact:"));    panel.add(contactField);
//        panel.add(new JLabel("Category:"));   panel.add(categoryCombo);
//        panel.add(new JLabel("Room:"));       panel.add(roomCombo);
//
//        JButton addBtn    = new JButton("Add");
//        JButton editBtn   = new JButton("Edit");
//        JButton deleteBtn = new JButton("Delete");
//
//        panel.add(addBtn);
//        panel.add(editBtn);
//        panel.add(deleteBtn);
//
//        addBtn.addActionListener(e -> addStudent());
//        editBtn.addActionListener(e -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//
//        return panel;
//    }
//
//    // ───────── TABLE ─────────
//    private JScrollPane buildTablePanel() {
//
//        model = new DefaultTableModel(
//                new String[]{"ID","Name","Enrollment","Category","Room","Status"}, 0
//        ){
//            public boolean isCellEditable(int r,int c){ return false; }
//        };
//
//        table = new JTable(model);
//
//        // hide ID column
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        // ✅ row click → fill form
//        table.getSelectionModel().addListSelectionListener(e -> {
//            if (!e.getValueIsAdjusting()) fillFormFromTable();
//        });
//
//        return new JScrollPane(table);
//    }
//
//    // ───────── LOAD DROPDOWNS ─────────
//    private void loadDropdowns() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while (cat.next()) {
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String sql = (floorId == -1)
//                    ? "SELECT * FROM Room"
//                    : "SELECT * FROM Room WHERE floor_id=" + floorId;
//
//            ResultSet room = con.createStatement().executeQuery(sql);
//
//            while (room.next()) {
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────── ROOM SEARCH ─────────
//    private void enableRoomSearch() {
//        JTextField editor = (JTextField) roomCombo.getEditor().getEditorComponent();
//
//        editor.addKeyListener(new KeyAdapter() {
//            public void keyReleased(KeyEvent e) {
//                String text = editor.getText();
//
//                roomCombo.removeAllItems();
//
//                for (String r : roomMap.keySet()) {
//                    if (r.toLowerCase().contains(text.toLowerCase())) {
//                        roomCombo.addItem(r);
//                    }
//                }
//
//                editor.setText(text);
//                roomCombo.showPopup();
//            }
//        });
//    }
//
//    // ───────── LOAD STUDENTS ─────────
//    private void loadStudents() {
//
//        model.setRowCount(0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number, s.status " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id ";
//
//            if (floorId != -1) sql += "WHERE r.floor_id=? ";
//
//            sql += "ORDER BY s.name";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//
//            if (floorId != -1) ps.setInt(1, floorId);
//
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                        rs.getInt(1),
//                        rs.getString(2),
//                        rs.getString(3),
//                        rs.getString(4),
//                        rs.getString(5),
//                        rs.getString(6)
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────── ADD ─────────
//    private void addStudent() {
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            if (nameField.getText().trim().isEmpty() || enrollField.getText().trim().isEmpty()) {
//                JOptionPane.showMessageDialog(this, "Name and Enrollment required!");
//                return;
//            }
//
//            Object cat  = categoryCombo.getSelectedItem();
//            Object room = roomCombo.getSelectedItem();
//
//            if (cat == null || room == null || !roomMap.containsKey(room.toString())) {
//                JOptionPane.showMessageDialog(this, "Select valid Category & Room!");
//                return;
//            }
//
//            PreparedStatement ps = con.prepareStatement(
//                "INSERT INTO Student(name,enrollment_no,category_id,room_id,status) VALUES(?,?,?,?,?)",
//                Statement.RETURN_GENERATED_KEYS
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, categoryMap.get(cat));
//            ps.setInt(4, roomMap.get(room.toString()));
//            ps.setString(5, "Active");
//
//            ps.executeUpdate();
//
//            ResultSet rs = ps.getGeneratedKeys();
//            int id = 0;
//            if (rs.next()) id = rs.getInt(1);
//
//            model.addRow(new Object[]{
//                id,
//                nameField.getText(),
//                enrollField.getText(),
//                cat.toString(),
//                room.toString().replace("Room ", ""),
//                "Active"
//            });
//
//            JOptionPane.showMessageDialog(this, "Student Added!");
//            clearFields();
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, e.getMessage());
//        }
//    }
//
//    // ───────── EDIT ─────────
//    private void editStudent() {
//
//        int row = table.getSelectedRow();
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select a student!");
//            return;
//        }
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            Object cat  = categoryCombo.getSelectedItem();
//            Object room = roomCombo.getSelectedItem();
//
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET name=?, enrollment_no=?, category_id=?, room_id=? WHERE student_id=?"
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, categoryMap.get(cat));
//            ps.setInt(4, roomMap.get(room.toString()));
//            ps.setInt(5, id);
//
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Updated!");
//            loadStudents();
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, e.getMessage());
//        }
//    }
//
//    // ───────── DELETE ─────────
//    private void deleteStudent() {
//
//        int row = table.getSelectedRow();
//        if (row < 0) {
//            JOptionPane.showMessageDialog(this, "Select a student!");
//            return;
//        }
//
//        int confirm = JOptionPane.showConfirmDialog(this,
//            "Delete student?", "Confirm", JOptionPane.YES_NO_OPTION);
//
//        if (confirm != JOptionPane.YES_OPTION) return;
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            PreparedStatement ps = con.prepareStatement(
//                "DELETE FROM Student WHERE student_id=?"
//            );
//            ps.setInt(1, id);
//            ps.executeUpdate();
//
//            JOptionPane.showMessageDialog(this, "Deleted!");
//            loadStudents();
//
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, e.getMessage());
//        }
//    }
//
//    // ───────── FILL FORM ─────────
//    private void fillFormFromTable() {
//
//        int row = table.getSelectedRow();
//        if (row < 0) return;
//
//        nameField.setText(model.getValueAt(row, 1).toString());
//        enrollField.setText(model.getValueAt(row, 2).toString());
//
//        categoryCombo.setSelectedItem(model.getValueAt(row, 3).toString());
//        roomCombo.setSelectedItem("Room " + model.getValueAt(row, 4).toString());
//    }
//
//    // ───────── CLEAR ─────────
//    private void clearFields() {
//        nameField.setText("");
//        enrollField.setText("");
//        contactField.setText("");
//        categoryCombo.setSelectedIndex(0);
//        roomCombo.setSelectedIndex(0);
//    }
//}
//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField, contactField;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;
//
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle(floorId == -1 ? "Manage Students (All Floors)" : "Manage Students (My Floor)");
//        setSize(900, 600);
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setLocationRelativeTo(null);
//        setLayout(new BorderLayout());
//
//        add(buildFormPanel(), BorderLayout.NORTH);
//        add(buildTablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        enableRoomSearch();
//        loadStudents();
//
//        setVisible(true);
//    }
//
//    // ───────── FORM ─────────
//    private JPanel buildFormPanel() {
//
//        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
//
//        nameField    = new JTextField(12);
//        enrollField  = new JTextField(12);
//        contactField = new JTextField(10);
//
//        categoryCombo = new JComboBox<>();
//        roomCombo = new JComboBox<>();
//        roomCombo.setEditable(true);
//
//        panel.add(new JLabel("Name:"));       panel.add(nameField);
//        panel.add(new JLabel("Enrollment:")); panel.add(enrollField);
//        panel.add(new JLabel("Contact:"));    panel.add(contactField);
//        panel.add(new JLabel("Category:"));   panel.add(categoryCombo);
//        panel.add(new JLabel("Room:"));       panel.add(roomCombo);
//
//        JButton addBtn    = new JButton("Add");
//        JButton editBtn   = new JButton("Edit");
//        JButton deleteBtn = new JButton("Delete");
//
//        panel.add(addBtn);
//        panel.add(editBtn);
//        panel.add(deleteBtn);
//
//        addBtn.addActionListener(e -> addStudent());
//        editBtn.addActionListener(e -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//
//        return panel;
//    }
//
//    // ───────── TABLE ─────────
//    private JScrollPane buildTablePanel() {
//
//        model = new DefaultTableModel(
//                new String[]{"ID","Name","Enrollment","Category","Room","Status"}, 0
//        ){
//            public boolean isCellEditable(int r,int c){ return false; }
//        };
//
//        table = new JTable(model);
//
//        // hide ID column
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        return new JScrollPane(table);
//    }
//
//    // ───────── LOAD DROPDOWNS ─────────
//    private void loadDropdowns() {
//        try (Connection con = DBConnection.getConnection()) {
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while (cat.next()) {
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String sql = (floorId == -1)
//                    ? "SELECT * FROM Room"
//                    : "SELECT * FROM Room WHERE floor_id=" + floorId;
//
//            ResultSet room = con.createStatement().executeQuery(sql);
//
//            while (room.next()) {
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────── SEARCHABLE ROOM ─────────
//    private void enableRoomSearch() {
//        JTextField editor = (JTextField) roomCombo.getEditor().getEditorComponent();
//
//        editor.addKeyListener(new KeyAdapter() {
//            public void keyReleased(KeyEvent e) {
//                String text = editor.getText();
//
//                roomCombo.removeAllItems();
//
//                for (String r : roomMap.keySet()) {
//                    if (r.toLowerCase().contains(text.toLowerCase())) {
//                        roomCombo.addItem(r);
//                    }
//                }
//
//                editor.setText(text);
//                roomCombo.showPopup();
//            }
//        });
//    }
//
//    // ───────── LOAD STUDENTS ─────────
//    private void loadStudents() {
//
//        model.setRowCount(0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            String sql =
//                    "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number, s.status " +
//                    "FROM Student s " +
//                    "JOIN Category c ON s.category_id=c.category_id " +
//                    "JOIN Room r ON s.room_id=r.room_id ";
//
//            if (floorId != -1) sql += "WHERE r.floor_id=? ";
//
//            sql += "ORDER BY s.name";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//
//            if (floorId != -1) ps.setInt(1, floorId);
//
//            ResultSet rs = ps.executeQuery();
//
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                        rs.getInt(1),
//                        rs.getString(2),
//                        rs.getString(3),
//                        rs.getString(4),
//                        rs.getString(5),
//                        rs.getString(6)
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
//    private void addStudent() {
//
//    try (Connection con = DBConnection.getConnection()) {
//
//        if (nameField.getText().trim().isEmpty() || enrollField.getText().trim().isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Name and Enrollment required!");
//            return;
//        }
//
//        Object cat  = categoryCombo.getSelectedItem();
//        Object room = roomCombo.getSelectedItem();
//
//        if (cat == null || room == null || !roomMap.containsKey(room.toString())) {
//            JOptionPane.showMessageDialog(this, "Select valid Category & Room!");
//            return;
//        }
//
//        PreparedStatement ps = con.prepareStatement(
//            "INSERT INTO Student(name, enrollment_no, category_id, room_id) VALUES(?,?,?,?)",
//            Statement.RETURN_GENERATED_KEYS
//        );
//
//        ps.setString(1, nameField.getText().trim());
//        ps.setString(2, enrollField.getText().trim());
//        ps.setInt(3, categoryMap.get(cat));
//        ps.setInt(4, roomMap.get(room.toString()));
//
//        ps.executeUpdate();
//
//        ResultSet rs = ps.getGeneratedKeys();
//        int id = 0;
//        if (rs.next()) id = rs.getInt(1);
//
//        // show instantly in table
//        model.addRow(new Object[]{
//            id,
//            nameField.getText().trim(),
//            enrollField.getText().trim(),
//            cat.toString(),
//            room.toString().replace("Room ", ""),
//            "Active"   // just for display
//        });
//
//        JOptionPane.showMessageDialog(this, "✅ Student Added!");
//        clearFields();
//
//    } catch (Exception e) {
//        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//    }
//}
//
//    // ───────── ADD STUDENT ─────────
////    private void addStudent() {
////
////        try (Connection con = DBConnection.getConnection()) {
////
////            if (nameField.getText().trim().isEmpty() || enrollField.getText().trim().isEmpty()) {
////                JOptionPane.showMessageDialog(this, "Name and Enrollment required!");
////                return;
////            }
////
////            Object cat  = categoryCombo.getSelectedItem();
////            Object room = roomCombo.getSelectedItem();
////
////            if (cat == null || room == null || !roomMap.containsKey(room.toString())) {
////                JOptionPane.showMessageDialog(this, "Select valid Category & Room!");
////                return;
////            }
////
////            PreparedStatement ps = con.prepareStatement(
////                    "INSERT INTO Student(name,enrollment_no,category_id,room_id,status) VALUES(?,?,?,?,?)",
////                    Statement.RETURN_GENERATED_KEYS
////            );
////
////            ps.setString(1, nameField.getText());
////            ps.setString(2, enrollField.getText());
////            ps.setInt(3, categoryMap.get(cat));
////            ps.setInt(4, roomMap.get(room.toString()));
////            ps.setString(5, "Active");
////
////            ps.executeUpdate();
////
////            ResultSet rs = ps.getGeneratedKeys();
////            int id = 0;
////            if (rs.next()) id = rs.getInt(1);
////
////            // instant UI update
////            model.addRow(new Object[]{
////                    id,
////                    nameField.getText(),
////                    enrollField.getText(),
////                    cat.toString(),
////                    room.toString().replace("Room ", ""),
////                    "Active"
////            });
////
////            JOptionPane.showMessageDialog(this, "Student Added!");
////
////            clearFields();
////
////        } catch (SQLIntegrityConstraintViolationException e) {
////            JOptionPane.showMessageDialog(this, "Enrollment already exists!");
////        } catch (Exception e) {
////            JOptionPane.showMessageDialog(this, e.getMessage());
////        }
////    }
////
//    // ───────── EDIT ─────────
//    private void editStudent() {
//
//        int row = table.getSelectedRow();
//        if (row < 0) return;
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            PreparedStatement ps = con.prepareStatement(
//                    "UPDATE Student SET name=?, enrollment_no=? WHERE student_id=?"
//            );
//
//            ps.setString(1, nameField.getText());
//            ps.setString(2, enrollField.getText());
//            ps.setInt(3, id);
//
//            ps.executeUpdate();
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────── DELETE ─────────
//    private void deleteStudent() {
//
//        int row = table.getSelectedRow();
//        if (row < 0) return;
//
//        int id = (int) model.getValueAt(row, 0);
//
//        try (Connection con = DBConnection.getConnection()) {
//
//            con.createStatement().executeUpdate(
//                    "DELETE FROM Student WHERE student_id=" + id
//            );
//
//            loadStudents();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ───────── CLEAR ─────────
//    private void clearFields() {
//        nameField.setText("");
//        enrollField.setText("");
//        contactField.setText("");
//        categoryCombo.setSelectedIndex(0);
//        roomCombo.setSelectedIndex(0);
//    }
//}

//package com.mycompany.Hostel;
//
//import javax.swing.*;
//import javax.swing.border.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.sql.*;
//import java.util.HashMap;
//
//public class ManageStudents extends JFrame {
//
//    private JTable table;
//    private DefaultTableModel model;
//
//    private JTextField nameField, enrollField, contactField, searchField;
//    private JTextField roomSearch, floorSearch;
////    blockSearch;
//    private JComboBox<String> categoryCombo, roomCombo;
//
//    private final HashMap<String, Integer> categoryMap = new HashMap<>();
//    private final HashMap<String, Integer> roomMap     = new HashMap<>();
//
//    private final int floorId;   // -1 = all floors (Warden), else HeadGirl floor
//
//    // ── No-arg constructor for Warden ─────────────────────────
//    public ManageStudents() {
//        this(-1);
//    }
//
//    public ManageStudents(int floorId) {
//        this.floorId = floorId;
//
//        setTitle(floorId == -1 ? "Manage Students (All Floors)" : "Manage Students (My Floor)");
//        setSize(980, 640);
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setLocationRelativeTo(null);
//        getContentPane().setBackground(WardenDashboard.BG);
//        setLayout(new BorderLayout(0, 0));
//
//        add(buildFormPanel(), BorderLayout.NORTH);
//        add(buildTablePanel(), BorderLayout.CENTER);
//
//        loadDropdowns();
//        enableRoomSearch();
//        loadStudents();
//
//        setVisible(true);
//    }
//
//    // ── FORM PANEL ────────────────────────────────────────────
//    private JPanel buildFormPanel() {
//        JPanel wrap = new JPanel(new BorderLayout());
//        wrap.setBackground(WardenDashboard.CARD);
//        wrap.setBorder(BorderFactory.createMatteBorder(0,0,1,0, WardenDashboard.BORDER));
//
//        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
//        form.setBackground(WardenDashboard.CARD);
//
//        nameField    = styledField(12);
//        enrollField  = styledField(12);
//        contactField = styledField(10);
//        categoryCombo = new JComboBox<>();
//        styleCombo(categoryCombo);
//        roomCombo = new JComboBox<>();
//        roomCombo.setEditable(true);
//        styleCombo(roomCombo);
//
//        form.add(lbl("Name:"));        form.add(nameField);
//        form.add(lbl("Enrollment:"));  form.add(enrollField);
//        form.add(lbl("Contact:"));     form.add(contactField);
//        form.add(lbl("Category:"));    form.add(categoryCombo);
//        form.add(lbl("Room:"));        form.add(roomCombo);
//
//        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
//        btnRow.setBackground(WardenDashboard.CARD);
//
//        JButton addBtn    = WardenDashboard.accentBtn("➕ Add",    WardenDashboard.SUCCESS);
//        JButton editBtn   = WardenDashboard.accentBtn("✏️ Edit",   WardenDashboard.ACCENT);
//        JButton deleteBtn = WardenDashboard.accentBtn("🗑 Delete",  WardenDashboard.DANGER);
//        JButton markBtn   = WardenDashboard.accentBtn("🏷 Status",  WardenDashboard.WARNING);
//
//        for (JButton b : new JButton[]{addBtn, editBtn, deleteBtn, markBtn}) {
//            b.setPreferredSize(new Dimension(110, 32));
//            btnRow.add(b);
//        }
//
//        // Search & filter row
//        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
//        filterRow.setBackground(WardenDashboard.CARD);
//
//        searchField = styledField(14); filterRow.add(lbl("Search:")); filterRow.add(searchField);
////        blockSearch = styledField(8);  filterRow.add(lbl("Block:"));  filterRow.add(blockSearch);
//        floorSearch = styledField(8);  filterRow.add(lbl("Floor:"));  filterRow.add(floorSearch);
//        roomSearch  = styledField(8);  filterRow.add(lbl("Room:"));   filterRow.add(roomSearch);
//
//        JButton filterBtn  = WardenDashboard.accentBtn("Filter",   WardenDashboard.ACCENT);
//        JButton showAllBtn = WardenDashboard.accentBtn("Show All", WardenDashboard.MUTED);
//        filterBtn.setPreferredSize(new Dimension(80, 28));
//        showAllBtn.setPreferredSize(new Dimension(90, 28));
//        filterRow.add(filterBtn);
//        filterRow.add(showAllBtn);
//
//        addBtn.addActionListener(e    -> addStudent());
//        editBtn.addActionListener(e   -> editStudent());
//        deleteBtn.addActionListener(e -> deleteStudent());
//        markBtn.addActionListener(e   -> markStudent());
//        filterBtn.addActionListener(e -> filterStudents());
//        showAllBtn.addActionListener(e-> loadStudents());
//
//        JPanel inner = new JPanel(new BorderLayout());
//        inner.setBackground(WardenDashboard.CARD);
//        inner.setBorder(BorderFactory.createEmptyBorder(4,12,4,12));
//        inner.add(form, BorderLayout.NORTH);
//        inner.add(btnRow, BorderLayout.CENTER);
//        inner.add(filterRow, BorderLayout.SOUTH);
//
//        wrap.add(inner, BorderLayout.CENTER);
//        return wrap;
//    }
//
//    // ── TABLE PANEL ───────────────────────────────────────────
//    private JPanel buildTablePanel() {
//        JPanel p = new JPanel(new BorderLayout());
//        p.setBackground(WardenDashboard.BG);
//        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
//
//        model = new DefaultTableModel(
//            new String[]{"ID","Name","Enrollment","Category","Room","Status"}, 0) {
//            public boolean isCellEditable(int r, int c) { return false; }
//        };
//
//        table = new JTable(model);
//        table.setBackground(WardenDashboard.ROW_EVEN);
//        table.setForeground(WardenDashboard.TEXT);
//        table.setGridColor(WardenDashboard.BORDER);
//        table.setRowHeight(28);
//        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
//        table.setSelectionBackground(new Color(0x2D4A6B));
//        table.setShowVerticalLines(false);
//        table.setIntercellSpacing(new Dimension(0,1));
//
//        // Hide ID
//        table.getColumnModel().getColumn(0).setMinWidth(0);
//        table.getColumnModel().getColumn(0).setMaxWidth(0);
//
//        // Status row coloring
//        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
//            @Override public Component getTableCellRendererComponent(JTable t, Object val,
//                boolean sel, boolean foc, int row, int col) {
//                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
//                setForeground(WardenDashboard.TEXT);
//                if (!sel) {
//                    String status = String.valueOf(t.getModel().getValueAt(row, 5));
//                    if ("Inactive".equals(status))     c.setBackground(new Color(0x2A1414));
//                    else if ("Flagged".equals(status)) c.setBackground(new Color(0x2A2010));
//                    else c.setBackground(row%2==0 ? WardenDashboard.ROW_EVEN : WardenDashboard.ROW_ODD);
//                    c.setForeground(WardenDashboard.TEXT);
//                }
//                return c;
//            }
//        });
//
//        JTableHeader header = table.getTableHeader();
//        header.setBackground(new Color(0x0D1926));
//        header.setForeground(WardenDashboard.MUTED);
//        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
//        header.setBorder(BorderFactory.createMatteBorder(0,0,1,0, WardenDashboard.BORDER));
//
//        JScrollPane scroll = new JScrollPane(table);
//        scroll.getViewport().setBackground(WardenDashboard.ROW_EVEN);
//        scroll.setBorder(BorderFactory.createLineBorder(WardenDashboard.BORDER));
//
//        p.add(scroll, BorderLayout.CENTER);
//        return p;
//    }
//
//    // ── DROPDOWN LOADERS ──────────────────────────────────────
//    private void loadDropdowns() {
//        try (Connection con = DBConnection.getConnection()) {
//            if (con == null) return;
//
//            ResultSet cat = con.createStatement().executeQuery("SELECT * FROM Category");
//            while (cat.next()) {
//                categoryMap.put(cat.getString("category_name"), cat.getInt("category_id"));
//                categoryCombo.addItem(cat.getString("category_name"));
//            }
//
//            String roomSql = floorId == -1
//                ? "SELECT * FROM Room"
//                : "SELECT * FROM Room WHERE floor_id=" + floorId;
//
//            ResultSet room = con.createStatement().executeQuery(roomSql);
//            while (room.next()) {
//                String r = "Room " + room.getString("room_number");
//                roomMap.put(r, room.getInt("room_id"));
//                roomCombo.addItem(r);
//            }
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    private void enableRoomSearch() {
//        JTextField editor = (JTextField) roomCombo.getEditor().getEditorComponent();
//        editor.addKeyListener(new KeyAdapter() {
//            public void keyReleased(KeyEvent e) {
//                String text = editor.getText();
//                roomCombo.removeAllItems();
//                for (String r : roomMap.keySet())
//                    if (r.toLowerCase().contains(text.toLowerCase()))
//                        roomCombo.addItem(r);
//                editor.setText(text);
//                roomCombo.showPopup();
//            }
//        });
//    }
//
//    // ── CRUD ──────────────────────────────────────────────────
//    private void loadStudents() {
//        model.setRowCount(0);
//        try (Connection con = DBConnection.getConnection()) {
//            if (con == null) return;
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number, s.status " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id ";
//
//            if (floorId != -1) sql += "WHERE r.floor_id=? ";
//            sql += "ORDER BY s.name";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//            if (floorId != -1) ps.setInt(1, floorId);
//
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                    rs.getInt(1), rs.getString(2), rs.getString(3),
//                    rs.getString(4), rs.getString(5), rs.getString(6)
//                });
//            }
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    private void addStudent() {
//    try (Connection con = DBConnection.getConnection()) {
//        if (con == null) return;
//
//        // Validation
//        if (nameField.getText().trim().isEmpty() || enrollField.getText().trim().isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Name and Enrollment are required.");
//            return;
//        }
//
//        Object selCat  = categoryCombo.getSelectedItem();
//        Object selRoom = roomCombo.getSelectedItem();
//
//        if (selCat == null || selRoom == null || !roomMap.containsKey(selRoom.toString())) {
//            JOptionPane.showMessageDialog(this, "Please select a valid Category and Room.");
//            return;
//        }
//
//        // ✅ No status column
//        PreparedStatement ps = con.prepareStatement(
//            "INSERT INTO Student(name, enrollment_no, category_id, room_id) VALUES(?,?,?,?)"
//        );
//
//        ps.setString(1, nameField.getText().trim());
//        ps.setString(2, enrollField.getText().trim());
//        ps.setInt(3, categoryMap.get(selCat));
//        ps.setInt(4, roomMap.get(selRoom.toString()));
//
//        ps.executeUpdate();
//
//        JOptionPane.showMessageDialog(this, "✅ Student added successfully.");
//
//        clearFields();
//        loadStudents();
//
//    } catch (SQLIntegrityConstraintViolationException e) {
//        JOptionPane.showMessageDialog(this, "Enrollment number already exists!");
//    } catch (Exception e) {
//        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//    }
//}
// 
//    private void clearFields() {
//    nameField.setText("");
//    enrollField.setText("");
//    contactField.setText("");
//    categoryCombo.setSelectedIndex(0);
//    roomCombo.setSelectedIndex(0);
//}
//
//
//    private void editStudent() {
//        int row = table.getSelectedRow();
//        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a student to edit."); return; }
//
//        int id = (int) model.getValueAt(row, 0);
//        try (Connection con = DBConnection.getConnection()) {
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET name=?, enrollment_no=? WHERE student_id=?");
//            ps.setString(1, nameField.getText().trim());
//            ps.setString(2, enrollField.getText().trim());
//            ps.setInt(3, id);
//            ps.executeUpdate();
//            JOptionPane.showMessageDialog(this, "Student updated.");
//            loadStudents();
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//        }
//    }
//
//    private void deleteStudent() {
//        int row = table.getSelectedRow();
//        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a student to delete."); return; }
//
//        int confirm = JOptionPane.showConfirmDialog(this,
//            "Delete student '" + model.getValueAt(row,1) + "'?", "Confirm Delete",
//            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
//        if (confirm != JOptionPane.YES_OPTION) return;
//
//        int id = (int) model.getValueAt(row, 0);
//        try (Connection con = DBConnection.getConnection()) {
//            con.createStatement().executeUpdate("DELETE FROM Student WHERE student_id=" + id);
//            loadStudents();
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//        }
//    }
//
//    private void markStudent() {
//        int row = table.getSelectedRow();
//        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a student."); return; }
//
//        int id = (int) model.getValueAt(row, 0);
//        String[] options = {"Active", "Inactive", "Flagged"};
//        String status = (String) JOptionPane.showInputDialog(this,
//            "Select Status:", "Mark Student",
//            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
//        if (status == null) return;
//
//        try (Connection con = DBConnection.getConnection()) {
//            PreparedStatement ps = con.prepareStatement(
//                "UPDATE Student SET status=? WHERE student_id=?");
//            ps.setString(1, status);
//            ps.setInt(2, id);
//            ps.executeUpdate();
//            loadStudents();
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
//        }
//    }
//
//    private void filterStudents() {
//        model.setRowCount(0);
//        try (Connection con = DBConnection.getConnection()) {
//            if (con == null) return;
//
//            String sql =
//                "SELECT s.student_id, s.name, s.enrollment_no, c.category_name, r.room_number, s.status " +
//                "FROM Student s " +
//                "JOIN Category c ON s.category_id=c.category_id " +
//                "JOIN Room r ON s.room_id=r.room_id " +
//                "JOIN Floor f ON r.floor_id=f.floor_id " +
//                "WHERE s.name LIKE ? " +
//                "AND r.room_number LIKE ? " +
//                "AND f.floor_name LIKE ? " ;
////                "AND f.block LIKE ? "
//
//            if (floorId != -1) sql += "AND r.floor_id=? ";
//
//            PreparedStatement ps = con.prepareStatement(sql);
//            ps.setString(1, "%" + searchField.getText() + "%");
//            ps.setString(2, "%" + roomSearch.getText() + "%");
//            ps.setString(3, "%" + floorSearch.getText() + "%");
////            ps.setString(4, "%" + blockSearch.getText() + "%");
//            if (floorId != -1) ps.setInt(5, floorId);
//
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                    rs.getInt(1), rs.getString(2), rs.getString(3),
//                    rs.getString(4), rs.getString(5), rs.getString(6)
//                });
//            }
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    // ── Helpers ───────────────────────────────────────────────
//    private JTextField styledField(int cols) {
//        JTextField f = new JTextField(cols);
//        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
//        f.setForeground(WardenDashboard.TEXT);
//        f.setBackground(new Color(0x0F1B2D));
//        f.setCaretColor(WardenDashboard.TEXT);
//        f.setBorder(BorderFactory.createCompoundBorder(
//            new LoginFrame.RoundBorder(6, WardenDashboard.BORDER),
//            BorderFactory.createEmptyBorder(2,6,2,6)
//        ));
//        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 28));
//        return f;
//    }
//
//    private void styleCombo(JComboBox<String> cb) {
//        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
//        cb.setBackground(new Color(0x0F1B2D));
//        cb.setForeground(WardenDashboard.TEXT);
//        cb.setPreferredSize(new Dimension(130, 28));
//    }
//
//    private JLabel lbl(String text) {
//        JLabel l = new JLabel(text);
//        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
//        l.setForeground(WardenDashboard.MUTED);
//        return l;
//    }
//}
