package io.github.volyx;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

	public static final String NO_SELECTION = "No Selection";
	public static final List<String> COLUMNS = Arrays.asList("Key", "Value");

	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Name");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(() -> {

			JFrame frame = createFrame();

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//				frame.pack();
			frame.setLocationByPlatform(true);

			AtomicReference<String> dbPath = new AtomicReference<>();
			JMenuBar menuBar = new JMenuBar();

			JMenu fileMenu = new JMenu("File");
			JMenuItem item = new JMenuItem("Open folder");
			fileMenu.add(item);
			menuBar.add(fileMenu);
			frame.setJMenuBar(menuBar);
			frame.setVisible(true);

			RocksDbWrapper rocksDbWrapper = new RocksDbWrapper();


			DefaultTableModel tableModel = createTableModel();
			JTable table = new JTable(tableModel);

			JTextField filterField = RowFilterUtil.createRowFilter(table);


			JPanel rightPanel = new JPanel(new BorderLayout());
			JPanel leftPanel = new JPanel(new BorderLayout());
			JSplitPane mainJPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
			//create the root node
			DefaultMutableTreeNode root = new DefaultMutableTreeNode("Columns Family");

			//create the tree by passing in the root node
			JTree tree = new JTree(root);

			tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
//					selectedLabel.setText(selectedNode.getUserObject().toString());
					if (dbPath.get() != null && selectedNode.getParent() != null) {
						System.out.println("select '" + selectedNode.getUserObject().toString() + "'");
						final Map<String, String> keyValue = rocksDbWrapper.openDatabase(dbPath.get(), selectedNode.getUserObject().toString());
						Vector<Vector<Object>> rows = new Vector<>();
						for (Map.Entry<String, String> entry : keyValue.entrySet()) {
							rows.add(new Vector<>(Arrays.asList(entry.getKey(), entry.getValue())));
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								tableModel.getDataVector().removeAllElements();
								tableModel.getDataVector().addAll(rows);
								tableModel.fireTableDataChanged();
							}
						});
					}
				}
			});


			leftPanel.add(new JScrollPane(tree), BorderLayout.CENTER);

			rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
			JLabel label = new JLabel();
			label.setText(NO_SELECTION);

			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(new File("."));
					chooser.setDialogTitle("Select");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

					chooser.setAcceptAllFileFilterUsed(false);
					//
					final String selection;
					if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
//						System.out.println("getCurrentDirectory(): " +  chooser.getCurrentDirectory());
//						System.out.println("getSelectedFile() : " +  chooser.getSelectedFile());

						selection = chooser.getSelectedFile().getPath();
						dbPath.set(selection);

						final List<String> columnFamilies = rocksDbWrapper.listColumnFamilies(selection);

						System.out.println("find " + columnFamilies);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								root.removeAllChildren();

								for (String c : columnFamilies) {
									DefaultMutableTreeNode fruitNode = new DefaultMutableTreeNode(c);
									root.add(fruitNode);
									tree.expandPath(new TreePath(fruitNode.getPath()));
								}
								tree.treeDidChange();

								label.setText(selection);
							}
						});


					} else {
						selection = NO_SELECTION;
					}

				}
			});

			rightPanel.add(label, BorderLayout.CENTER);
			rightPanel.add(filterField, BorderLayout.CENTER);
			frame.add(mainJPanel, BorderLayout.NORTH);


			JScrollPane pane = new JScrollPane(table);
			rightPanel.add(pane, BorderLayout.CENTER);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	private static DefaultTableModel createTableModel() {
		Vector<Vector<Object>> rows = new Vector<>();
		Vector<String> columns = new Vector<>(COLUMNS);


		return new DefaultTableModel(rows, columns) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
//				return columnIndex == 2 ? Integer.class : String.class;
				return String.class;
			}
		};
	}

	private static JFrame createFrame() {
		JFrame frame = new JFrame("RocksDB UI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream("doc.png");) {
			Objects.requireNonNull(is);
			Image image = ImageIO.read(is);
//			frame.setIconImage(image);
			if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
				Taskbar.getTaskbar().setIconImage(image);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		frame.setSize(new Dimension(600, 450));


//		Application.getApplication().setDockIconImage(new ImageIcon(getClass().getResource("/logo.png")).getImage());
//		Application.getApplication().setDockIconImage(
//				new ImageIcon("app.png").getImage());
		return frame;
	}
}
