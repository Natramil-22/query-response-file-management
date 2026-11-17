import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Encapsulation: file details are private and accessed through methods.
class FileRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id, name, type, location;

    public FileRecord(String id, String name, String type, String location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getLocation() { return location; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setLocation(String location) { this.location = location; }
}

// Abstraction: the GUI uses these operations without handling storage details.
interface RecordOperations {
    boolean addRecord(FileRecord record);
    boolean updateRecord(String oldId, FileRecord updated);
    boolean deleteRecord(String id);
    FileRecord findById(String id);
    List<FileRecord> search(String query);
}

// FileManager implements the operations and stores records in an ArrayList.
class FileManager implements RecordOperations {
    private final ArrayList<FileRecord> records = new ArrayList<>();
    private static final String DATA_FILE = "files.dat";

    public FileManager() { loadRecords(); }

    public ArrayList<FileRecord> getRecords() { return records; }

    @Override
    public boolean addRecord(FileRecord record) {
        if (record == null || record.getId().trim().isEmpty()
                || findById(record.getId()) != null) return false;
        records.add(record);
        saveRecords();
        return true;
    }

    @Override
    public boolean updateRecord(String oldId, FileRecord updated) {
        FileRecord existing = findById(oldId);
        if (existing == null || updated == null) return false;
        FileRecord duplicate = findById(updated.getId());
        if (duplicate != null && duplicate != existing) return false;
        existing.setId(updated.getId());
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setLocation(updated.getLocation());
        saveRecords();
        return true;
    }

    @Override
    public boolean deleteRecord(String id) {
        boolean removed = records.removeIf(
            r -> r.getId().equalsIgnoreCase(id));
        if (removed) saveRecords();
        return removed;
    }

    @Override
    public FileRecord findById(String id) {
        for (FileRecord r : records)
            if (r.getId().equalsIgnoreCase(id)) return r;
        return null;
    }

    @Override
    public List<FileRecord> search(String query) {
        List<FileRecord> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (FileRecord r : records) {
            if (r.getId().toLowerCase().contains(q)
                    || r.getName().toLowerCase().contains(q)
                    || r.getType().toLowerCase().contains(q)
                    || r.getLocation().toLowerCase().contains(q))
                result.add(r);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void loadRecords() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(file))) {
            Object data = in.readObject();
            if (data instanceof ArrayList<?>)
                records.addAll((ArrayList<FileRecord>) data);
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                "Saved data could not be loaded. A new record list will be used.",
                "Storage Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveRecords() {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(records);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                "Unable to save records: " + ex.getMessage(),
                "Storage Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// Inheritance: MainFrame extends JFrame.
// Polymorphism: refreshTable() overrides the base class method.
class MainFrame extends JFrame {
    private final FileManager manager = new FileManager();
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField searchField;
    private final JLabel totalLabel;

    MainFrame() {
        setTitle("Query Response System | File Management");
        setSize(1000, 650);
        setMinimumSize(new Dimension(820, 540));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 248, 252));
        setLayout(new BorderLayout(16, 16));

        JLabel heading = new JLabel("QUERY RESPONSE SYSTEM", SwingConstants.LEFT);
        heading.setFont(new Font("SansSerif", Font.BOLD, 25));
        heading.setForeground(new Color(25, 60, 120));
        heading.setBorder(BorderFactory.createEmptyBorder(22, 24, 4, 10));

        JLabel subheading = new JLabel("FILE MANAGEMENT • SEARCH • RETRIEVAL");
        subheading.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subheading.setForeground(new Color(100, 116, 139));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(heading);
        header.add(subheading);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new String[]{"FILE ID", "FILE NAME", "FILE TYPE", "LOCATION"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 12));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.setOpaque(false);
        searchField = new JTextField(25);
        JButton searchBtn = button("Search");
        JButton clearBtn = button("Clear");
        searchPanel.add(new JLabel("Query:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        JButton addBtn = button("Register File");
        JButton browseBtn = button("Browse File");
        JButton folderBtn = button("Browse Folder");
        JButton updateBtn = button("Update Selected");
        JButton deleteBtn = button("Delete Selected");
        JButton allBtn = button("Show All");
        actions.add(addBtn);
        actions.add(browseBtn);
        actions.add(folderBtn);
        actions.add(updateBtn);
        actions.add(deleteBtn);
        actions.add(allBtn);

        totalLabel = new JLabel("Total files: 0");
        totalLabel.setForeground(new Color(51, 65, 85));

        bottom.add(searchPanel, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.CENTER);
        bottom.add(totalLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addFile());
        browseBtn.addActionListener(e -> browseSingleFile());
        folderBtn.addActionListener(e -> browseFolder());
        updateBtn.addActionListener(e -> updateFile());
        deleteBtn.addActionListener(e -> deleteFile());
        allBtn.addActionListener(e -> refreshTable());
        searchBtn.addActionListener(e -> searchFiles());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        searchField.addActionListener(e -> searchFiles());

        refreshTable();
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(new Color(37, 99, 235));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        return b;
    }

    // Overriding JFrame's repaint is unnecessary; this method refreshes our data view.
    public void refreshTable() {
        showRecords(manager.getRecords());
    }

    private void showRecords(List<FileRecord> records) {
        model.setRowCount(0);
        for (FileRecord r : records)
            model.addRow(new Object[]{r.getId(), r.getName(),
                                      r.getType(), r.getLocation()});
        totalLabel.setText("Total files displayed: " + records.size()
                           + "     |     Stored records: "
                           + manager.getRecords().size());
    }

    private JTextField[] formFields(FileRecord old) {
        JTextField id = new JTextField(old == null ? "" : old.getId());
        JTextField name = new JTextField(old == null ? "" : old.getName());
        JTextField type = new JTextField(old == null ? "" : old.getType());
        JTextField location = new JTextField(old == null ? "" : old.getLocation());
        Object[] fields = {"File ID:", id, "File Name:", name,
                           "File Type:", type, "Location:", location};
        int option = JOptionPane.showConfirmDialog(this, fields,
            old == null ? "Register File" : "Update File",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return null;
        if (id.getText().trim().isEmpty() || name.getText().trim().isEmpty()
                || type.getText().trim().isEmpty()
                || location.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all fields.");
            return null;
        }
        return new JTextField[]{id, name, type, location};
    }

    private FileRecord toRecord(JTextField[] f) {
        return new FileRecord(f[0].getText().trim(), f[1].getText().trim(),
                              f[2].getText().trim(), f[3].getText().trim());
    }

    private void addFile() {
        JTextField[] f = formFields(null);
        if (f == null) return;
        if (manager.addRecord(toRecord(f))) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "File registered successfully.");
        } else {
            JOptionPane.showMessageDialog(this,
                "File ID already exists or is invalid.",
                "Registration Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Select a file, detect an existing filename, and offer replacement.
    private void browseSingleFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a file to register");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        String name = selected.getName();
        String type = getFileType(selected);
        String location = selected.getAbsolutePath();

        FileRecord duplicate = findByFileName(name);
        if (duplicate != null) {
            Object[] options = {"Replace Existing Record", "Choose Another File", "Cancel"};
            String duplicateReason = duplicate.getLocation().equalsIgnoreCase(location)
                    ? "Reason: This exact file location is already registered."
                    : "Reason: A file with the same filename is already registered.";
            int choice = JOptionPane.showOptionDialog(this,
                    "DUPLICATE FILE DETECTED\n\n"
                    + "The selected file is already present in the system.\n\n"
                    + duplicateReason + "\n\n"
                    + "Existing File ID: " + duplicate.getId() + "\n"
                    + "Existing File Name: " + duplicate.getName() + "\n"
                    + "Existing File Type: " + duplicate.getType() + "\n"
                    + "Existing Location: " + duplicate.getLocation() + "\n\n"
                    + "Do you want to replace the existing registered record "
                    + "with the newly selected file details?",
                    "Duplicate File - Action Required", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                // Keep the existing ID and replace its registered file details.
                FileRecord replacement = new FileRecord(
                        duplicate.getId(), name, type, location);
                if (manager.updateRecord(duplicate.getId(), replacement)) {
                    refreshTable();
                    JOptionPane.showMessageDialog(this,
                            "Replacement successful!\n\n"
                            + "Existing File ID: " + duplicate.getId() + "\n"
                            + "Updated File Name: " + name + "\n"
                            + "Updated File Type: " + type + "\n"
                            + "Updated Location: " + location,
                            "File Replaced", JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (choice == 1) {
                // Open the file picker again so the user can choose a different file.
                browseSingleFile();
            }
            return;
        }

        String id = JOptionPane.showInputDialog(this,
                "Enter a unique File ID:", generateNextId());
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty() || manager.findById(id) != null) {
            JOptionPane.showMessageDialog(this,
                    "File ID is empty or already exists.",
                    "Registration Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (manager.addRecord(new FileRecord(id, name, type, location))) {
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "File registered successfully!\nName: " + name
                    + "\nType: " + type + "\nLocation: " + location);
        }
    }

    // Find an existing record by filename (case-insensitive).
    private FileRecord findByFileName(String fileName) {
        for (FileRecord record : manager.getRecords()) {
            if (record.getName().equalsIgnoreCase(fileName)) return record;
        }
        return null;
    }

    // Register files in the selected folder and its subfolders.
    private void browseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a folder to register");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File folder = chooser.getSelectedFile();
        List<File> files = new ArrayList<>();
        collectFiles(folder, files);
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files found in the selected folder.");
            return;
        }

        int added = 0;
        int duplicates = 0;
        for (File file : files) {
            if (findByFileName(file.getName()) != null) {
                duplicates++;
                continue;
            }
            String id = generateNextId();
            FileRecord record = new FileRecord(
                    id, file.getName(), getFileType(file), file.getAbsolutePath());
            if (manager.addRecord(record)) added++;
        }
        refreshTable();
        JOptionPane.showMessageDialog(this,
                "Folder registration completed!\nFiles registered: " + added
                + "\nDuplicates skipped: " + duplicates
                + "\nFolder: " + folder.getAbsolutePath());
    }

    private void collectFiles(File folder, List<File> files) {
        File[] contents = folder.listFiles();
        if (contents == null) return;
        for (File item : contents) {
            if (item.isFile()) files.add(item);
            else if (item.isDirectory()) collectFiles(item, files);
        }
    }

    private String getFileType(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1)
            return name.substring(dot + 1).toUpperCase();
        return "UNKNOWN";
    }

    private String generateNextId() {
        int number = 1;
        String id;
        do { id = String.format("AUTO%03d", number++); }
        while (manager.findById(id) != null);
        return id;
    }

    private void updateFile() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        String oldId = model.getValueAt(row, 0).toString();
        JTextField[] f = formFields(manager.findById(oldId));
        if (f == null) return;
        if (manager.updateRecord(oldId, toRecord(f))) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "File updated successfully.");
        } else {
            JOptionPane.showMessageDialog(this,
                "Update failed. Check the File ID for duplicates.");
        }
    }

    private void deleteFile() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        int answer = JOptionPane.showConfirmDialog(this,
            "Delete file record " + id + "?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            manager.deleteRecord(id);
            refreshTable();
        }
    }

    private void searchFiles() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            refreshTable();
            return;
        }
        showRecords(manager.search(query));
    }
}

public class FileManagementSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
