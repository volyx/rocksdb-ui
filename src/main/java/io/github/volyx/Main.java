package io.github.volyx;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class Main {

	public static final String NO_SELECTION = "No Selection";
	public static final List<String> COLUMNS = Arrays.asList("Key", "Value");

	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Name");
		EventQueue.invokeLater(() -> {

			JFrame frame = createFrame();

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//				frame.pack();
			frame.setLocationByPlatform(true);


			JMenuBar menuBar = new JMenuBar();
			JMenu fileMenu = new JMenu("File");
			JMenuItem item = new JMenuItem("Item");
			fileMenu.add(item);
			item.addActionListener(e -> System.out.println("Here"));
			menuBar.add(fileMenu);
			frame.setJMenuBar(menuBar);
			frame.setVisible(true);

			RocksDbWrapper rocksDbWrapper = new RocksDbWrapper();

			DefaultTableModel tableModel = createTableModel();
			JTable table = new JTable(tableModel);

			JTextField filterField = RowFilterUtil.createRowFilter(table);
			JPanel jp = new JPanel();
			jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
			JButton go = new JButton("Select rocksdb folder");
			JLabel label = new JLabel();
			label.setText(NO_SELECTION);
			go.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				chooser.setDialogTitle("Select");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				chooser.setAcceptAllFileFilterUsed(false);
				//
				final String selection;
				if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					System.out.println("getCurrentDirectory(): "
							+  chooser.getCurrentDirectory());
					System.out.println("getSelectedFile() : " +  chooser.getSelectedFile());

					selection = chooser.getSelectedFile().getPath();

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
//							tableModel.setRowCount(0);
//							rows.clear();
							Vector<Vector<Object>> rows = new Vector<>();
							rocksDbWrapper.openDatabase(selection, rows);
//							rows.add(new Vector<Object>(Arrays.asList("1", "2")));
							tableModel.getDataVector().removeAllElements();
							tableModel.getDataVector().addAll(rows);
							tableModel.fireTableDataChanged();
						}
					});


				} else {
					selection = NO_SELECTION;
				}
				SwingUtilities.invokeLater(() -> label.setText(selection));
			});
			jp.add(go);

			jp.add(label);
			jp.add(filterField);
			frame.add(jp, BorderLayout.NORTH);

			JScrollPane pane = new JScrollPane(table);
			frame.add(pane, BorderLayout.CENTER);
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
		JFrame frame = new JFrame("Rockssb UI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			final URL url = Main.class.getClassLoader().getResource("doc.png");
			Objects.requireNonNull(url);
			File pathToFile = new File(url.getPath());
			Image image = ImageIO.read(pathToFile);
//			frame.setIconImage(image);
			Taskbar.getTaskbar().setIconImage(image);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		frame.setSize(new Dimension(600, 450));


//		Application.getApplication().setDockIconImage(new ImageIcon(getClass().getResource("/logo.png")).getImage());
//		Application.getApplication().setDockIconImage(
//				new ImageIcon("app.png").getImage());
		return frame;
	}
}
