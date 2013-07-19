package net.sourceforge.retroweaver.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.objectweb.asm.commons.EmptyVisitor;

import net.sourceforge.retroweaver.RefVerifier;
import net.sourceforge.retroweaver.RetroWeaver;
import net.sourceforge.retroweaver.Weaver;
import net.sourceforge.retroweaver.event.VerifierListener;
import net.sourceforge.retroweaver.event.WeaveListener;

/**
 * A simple graphical user interface for Retroweaver.
 */
public class RetroWeaverGui extends JPanel implements WeaveListener,
		VerifierListener {

	public static void main(String[] args) {
		String defaultPath = "";

		if (args.length > 0) {
			if (args[0].equals("-console")) {
				String a[] = new String[args.length - 1];
				System.arraycopy(args, 1, a, 0, args.length - 1);
				Weaver.main(a);
				return;
			}
			defaultPath = args[0];
		}

		String version = Weaver.getVersion();
		showInJFrame("RetroWeaver " + version, new RetroWeaverGui(defaultPath));
	}

	private static void showInJFrame(String title, Component contents) {
		JFrame frame = new JFrame(title);
		frame.getContentPane().add(contents);
		frame.setSize(400, 300);
		centerOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	private static void centerOnScreen(JFrame frame) {
		Dimension frameSize = frame.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - frameSize.width) / 2;
		int y = (screenSize.height - frameSize.height) / 2;
		frame.setLocation(x, y);
	}

	public RetroWeaverGui(String defaultPath) {
		super(new BorderLayout());
		refClassPath = new JTextField();
		add(createParameterSection(), BorderLayout.NORTH);
		add(createMessageArea(), BorderLayout.CENTER);
		add(createSouthSection(), BorderLayout.SOUTH);
		pathField.setText(defaultPath);
	}

	public void weavingStarted(String msg) {
		displayWeavingMessage(msg);
	}

	public void weavingPath(String sourcePath) {
		String pathAsString = path.getAbsolutePath();
		String p;
		if (sourcePath.startsWith(pathAsString)) {
			p = sourcePath.substring(pathAsString.length());
		} else {
			p = sourcePath;
		}
		displayWeavingMessage(p);
	}

	public void weavingError(String msg) {
		displayWeavingMessage(msg);
	}

	public void weavingCompleted(String msg) {
		displayWeavingMessage(msg);
	}

	private void displayWeavingMessage(String msg) {
		messages.append("[RetroWeaver] " + msg + "\n");
		messageScrollPane.getVerticalScrollBar().setValue(
				messageScrollPane.getVerticalScrollBar().getMaximum());	
	}

	public void verifyPathStarted(String msg) {
		displayVerifierMessage(msg);
	}

	public void verifyClassStarted(String msg) {
		displayVerifierMessage(msg);
	}

	public void acceptWarning(String msg) {
		displayVerifierMessage(msg);
	}

	public void displaySummary(int warningCount) {
		displayVerifierMessage("Verification complete, " + warningCount
				+ " warning(s).");
	}

	private void displayVerifierMessage(String msg) {
		messages.append("[RefVerifier] " + msg + "\n");
		messageScrollPane.getVerticalScrollBar().setValue(
				messageScrollPane.getVerticalScrollBar().getMaximum());
	}

	private Component createParameterSection() {
		JPanel ret = new JPanel(new GridBagLayout());
		addAt(ret, 0, 0, new JLabel("Source:"));
		addAt(ret, 1, 0, GridBagConstraints.HORIZONTAL, 2, 0.0,
				createPathField());
		addAt(ret, 3, 0, createBrowse());
		addAt(ret, 0, 1, new JLabel("Target:"));
		addAt(ret, 1, 1, createTargetChooser());
		addAt(ret, 2, 1, GridBagConstraints.HORIZONTAL, 1, 1.0, new JPanel());
		addAt(ret, 0, 2, new JLabel("Ref Verify Classpath:"));
		addAt(ret, 1, 2, GridBagConstraints.HORIZONTAL, 2, 0.0, refClassPath);
		addAt(ret, 3, 2, GridBagConstraints.HORIZONTAL, 1, 1.0, new JPanel());
		return ret;
	}

	private Component createSouthSection() {
		JPanel ret = new JPanel(new BorderLayout());
		ret.add(createActionButtons(), BorderLayout.CENTER);
		ret.add(createStatus(), BorderLayout.SOUTH);
		ret.setBorder(BorderFactory.createEmptyBorder(INSET_SIZE, INSET_SIZE,
				INSET_SIZE, INSET_SIZE));
		return ret;
	}

	private Component createActionButtons() {
		JPanel ret = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		ret.add(createTransform());
		ret.add(createExit());
		return ret;
	}

	private Component createPathField() {
		pathField = new JTextField();
		pathField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				doPathChanged();
			}

			public void removeUpdate(DocumentEvent e) {
				doPathChanged();
			}

			public void changedUpdate(DocumentEvent e) {
				// do nothing
			}
		});
		return pathField;
	}

	private Component createBrowse() {
		JButton browse = new JButton("Browse...");
		browse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doBrowse();
			}
		});
		return browse;
	}

	private Component createTargetChooser() {
		targetCombo = new JComboBox(new String[] { "1.4", "1.3", "1.2" });
		targetCombo.setSelectedIndex(0);
		targetCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				status.setText(READY);
			}
		});
		return targetCombo;
	}

	private Component createTransform() {
		JButton transform = new JButton("Transform");
		transform.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doTransform();
			}
		});
		return transform;
	}

	private Component createExit() {
		JButton exit = new JButton("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doExit();
			}
		});
		return exit;
	}

	private Component createStatus() {
		status = new JLabel(READY);
		return status;
	}

	private Component createMessageArea() {
		JPanel ret = new JPanel(new BorderLayout());
		ret.setBorder(BorderFactory.createTitledBorder("Messages"));
		messages = new JTextArea();
		messageScrollPane = new JScrollPane(messages);
		ret.add(messageScrollPane, BorderLayout.CENTER);
		return ret;
	}

	private void addAt(Container container, int x, int y, Component component) {
		addAt(container, x, y, 0, 1, 0.0, component);
	}

	private void addAt(Container container, int x, int y, int fill,
			int gridwidth, double weightx, Component component) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.fill = fill;
		constraints.insets = INSETS;
		constraints.gridwidth = gridwidth;
		constraints.weightx = weightx;
		container.add(component, constraints);
	}

	private void doTransform() {

		if (runWeaverThread != null && runWeaverThread.isAlive()) {
			JOptionPane
					.showMessageDialog(this,
							"RetroWeaver is already running, wait until it has finished ");
			return;
		}

		path = new File(pathField.getText());

		if (!path.exists()) {
			status.setText("Error: Path \"" + path + "\" does not exist");
			return;
		}

		version = VERSION_NUMBERS[targetCombo.getSelectedIndex()];
		runWeaverThread = createWeaverThread();
		runWeaverThread.start();
	}

	private void doBrowse() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
		status.setText(READY);
	}

	private void doExit() {
		JFrame frame = (JFrame) getTopLevelAncestor();
		frame.dispose();
	}

	private void doPathChanged() {
		status.setText(READY);
	}

	private JTextField pathField;

	private final JTextField refClassPath;

	private JComboBox targetCombo;

	private JLabel status;

	private JTextArea messages;

	private int version;

	private File path;

	private JScrollPane messageScrollPane;

	private static final String READY = "Ready";

	private static final int INSET_SIZE = 3;

	private static final Insets INSETS = new Insets(INSET_SIZE, INSET_SIZE,
			INSET_SIZE, INSET_SIZE);

	private static final int[] VERSION_NUMBERS = new int[] {
			Weaver.VERSION_1_4, Weaver.VERSION_1_3, Weaver.VERSION_1_2 };

	private volatile Thread runWeaverThread;

	private Thread createWeaverThread() {
		return new Thread(new Runnable() {
			public void run() {
				Cursor oldCursor = getTopLevelAncestor().getCursor();
				getTopLevelAncestor().setCursor(
						Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				try {
					status.setText("Running");
					RetroWeaver weaver = new RetroWeaver(version);
					messages.setText("");
					weaver.setListener(RetroWeaverGui.this);

					String refCp = refClassPath.getText();

					if (refCp.length() != 0) {
						java.util.List<String> classpath = new ArrayList<String>();
						StringTokenizer st = new StringTokenizer(refCp,
								File.pathSeparator);
						while (st.hasMoreTokens()) {
							classpath.add(st.nextToken());
						}
						RefVerifier verifier = new RefVerifier(version, new EmptyVisitor(), classpath,
								RetroWeaverGui.this);
						weaver.setVerifier(verifier);
					}

					weaver.weave(path);

					status.setText("Done");
				} catch (Exception ex) {
					status.setText("Error: " + ex.getMessage());
				} finally {
					getTopLevelAncestor().setCursor(oldCursor);
				}
			}
		});
	}
}
