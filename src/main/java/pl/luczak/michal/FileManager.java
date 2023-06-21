package pl.luczak.michal;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileManager extends JFrame {
    private final JList<File> leftList;
    private final JList<File> rightList;
    private final DefaultListModel<File> leftListModel;
    private final DefaultListModel<File> rightListModel;
    private File leftCurrentPath = new File("C:\\");
    private File rightCurrentPath = new File("E:\\");

    public FileManager() {
        setTitle("File Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        leftListModel = new DefaultListModel<>();
        leftList = new JList<>(leftListModel);
        leftList.setCellRenderer(new FileListCellRenderer());
        JScrollPane leftScrollPane = new JScrollPane(leftList);
        add(leftScrollPane);

        rightListModel = new DefaultListModel<>();
        rightList = new JList<>(rightListModel);
        rightList.setCellRenderer(new FileListCellRenderer());
        JScrollPane rightScrollPane = new JScrollPane(rightList);
        add(rightScrollPane);

        // Drag and drop support
        leftList.setDragEnabled(true);
        rightList.setDragEnabled(true);
        leftList.setTransferHandler(new FileTransferHandler(leftListModel));
        rightList.setTransferHandler(new FileTransferHandler(rightListModel));

        rightList.addMouseListener(new CustomMouseAdapter(rightList, true));
        leftList.addMouseListener(new CustomMouseAdapter(leftList, false));

        rightList.setDropMode(DropMode.INSERT);

        leftList.addKeyListener(new FileManagerKeyListener());
        rightList.addKeyListener(new FileManagerKeyListener());

        setSize(1280, 720);
        setVisible(true);

        loadFiles(leftCurrentPath.getAbsolutePath(), leftListModel);
        loadFiles(rightCurrentPath.getAbsolutePath(), rightListModel);
    }

    private void loadFiles(String directoryPath, DefaultListModel<File> listModel) {
        listModel.clear();
        listModel.add(0, new File(".."));
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                listModel.addElement(file);
            }
        }
    }


    private void copyFile(File source, Path targetPath) throws IOException {
        FileOperationExecutor.copyFile(source, targetPath);
        refreshFileLists();
    }

    private void deleteFile(File file) throws IOException {
        FileOperationExecutor.deleteFile(file);
        if (!file.exists()) {
            refreshFileLists();
            JOptionPane.showMessageDialog(this, "File/folder deleted successfully!");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete the file/folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewFolder(String directory) {
        String newFolderName = JOptionPane.showInputDialog(this, "Enter the name of the new folder:", "New Folder");
        if (newFolderName != null && !newFolderName.trim().isEmpty()) {
            if (new File(Paths.get(directory, newFolderName).toUri()).exists()) {
                JOptionPane.showMessageDialog(this, "Failed to create the folder because file with this name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            FileOperationExecutor.createDirectory(directory, newFolderName);
        }
        refreshFileLists();
    }

    private void refreshFileLists() {
        loadFiles(rightCurrentPath.getAbsolutePath(), rightListModel);
        loadFiles(leftCurrentPath.getAbsolutePath(), leftListModel);
    }

    private void changeDirectory(boolean isRightDirectory, File path) {
        if (isRightDirectory) {
            rightCurrentPath = path;
        } else {
            leftCurrentPath = path;
        }
        refreshFileLists();
    }

    private void goBackToPreviousDirectory(boolean isRightDirectory) {
        if (isRightDirectory) {
            if (rightCurrentPath.getParentFile() == null) {
                return;
            }
            if (!rightCurrentPath.getParentFile().exists()) {
                return;
            }
            rightCurrentPath = rightCurrentPath.getParentFile();
        } else {
            if (leftCurrentPath.getParentFile() == null) {
                return;
            }
            if (!leftCurrentPath.getParentFile().exists()) {
                return;
            }
            leftCurrentPath = leftCurrentPath.getParentFile();
        }
        refreshFileLists();
    }

    @RequiredArgsConstructor
    private class FileTransferHandler extends TransferHandler {
        private int[] indices;
        private final DefaultListModel<?> sourceListModel;

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<File> list = (JList<File>) c;
            indices = list.getSelectedIndices();
            ArrayList<File> selectedValues = new ArrayList<>();
            for (int index : indices) {
                selectedValues.add(list.getModel().getElementAt(index));
            }
            return new ElementTransferable(selectedValues);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == MOVE) {
                for (int i = indices.length - 1; i >= 0; i--) {
                    sourceListModel.remove(indices[i]);
                }
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            Transferable transferable = support.getTransferable();
            File file;
            try {
                file = (File) transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                return false;
            }

            if (sourceListModel == rightListModel) {
                try {
                    copyFile(file, Paths.get(rightCurrentPath.getAbsolutePath(), file.getName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    copyFile(file, Paths.get(leftCurrentPath.getAbsolutePath(), file.getName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


            return true;
        }
    }

    private class ElementTransferable implements Transferable {
        private ArrayList<File> elements;

        public ElementTransferable(ArrayList<File> elements) {
            this.elements = elements;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            if (isDataFlavorSupported(flavor)) {
                return elements.get(0);
            } else {
                return null;
            }
        }
    }

    private class FileManagerKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F8) {
                JList<?> sourceList = (JList<?>) e.getSource();
                DefaultListModel<?> sourceListModel = (DefaultListModel<?>) sourceList.getModel();
                int selectedIndex = sourceList.getSelectedIndex();

                if (selectedIndex != -1) {
                    int confirm = JOptionPane.showConfirmDialog(FileManager.this, "Are you sure you want to delete this file/folder?", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        File selectedFile = (File) sourceListModel.getElementAt(selectedIndex);
                        try {
                            deleteFile(selectedFile);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            } else if (e.getKeyCode() == KeyEvent.VK_F7) {
                String[] options = new String[]{"C", "E"};
                var selection = JOptionPane.showOptionDialog(null, "Select one:", "Choose the disk!",
                        0, 3, null, options, options[0]);
                if (selection == 0) {
                    createNewFolder(leftCurrentPath.getAbsolutePath());
                }
                if (selection == 1) {
                    createNewFolder(rightCurrentPath.getAbsolutePath());
                }
            }
        }
    }

    private class FileListCellRenderer extends DefaultListCellRenderer {
        private FileSystemView fileSystemView;

        public FileListCellRenderer() {
            fileSystemView = FileSystemView.getFileSystemView();
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            File file = (File) value;
            setText(fileSystemView.getSystemDisplayName(file));

            Icon icon = fileSystemView.getSystemIcon(file);
            setIcon(icon);

            return this;
        }
    }

    @AllArgsConstructor
    private class CustomMouseAdapter extends MouseAdapter {

        private final JList<File> list;
        private final boolean isRight;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2) {
                return;
            }
            int index = list.locationToIndex(e.getPoint());
            if (!(e.getSource() instanceof JList<?> jPanel)) {
                return;
            }
            if (jPanel != list) {
                return;
            }
            File file = list.getModel().getElementAt(index);
            if (file.getName().equals("..")) {
                goBackToPreviousDirectory(isRight);
                return;
            }
            if (!file.isDirectory()) {
                return;
            }
            changeDirectory(isRight, list.getModel().getElementAt(index));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileManager::new);
    }
}
