package raspbootin.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import raspbootin.Raspbootin64Client;
import raspbootin.util.IniFile;
import raspbootin.util.WindowUtil;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = -2333246467400263743L;
	public static byte[] readBytes = null;
	IniFile ini;

	private JPanel pnlCenter = new JPanel();
	private JLabel lImgFile = new JLabel("Image file: ");
	private JTextField tfImgFile = new JTextField();
	private JButton btnBrowse = new JButton("Browse");
	private JButton btnLoad = new JButton("Load&Execute");
	private JButton btnOpenSerial = new JButton("Open terminal");
	private JTextArea taIo = new JTextArea();
	private JScrollPane sc = new JScrollPane(taIo);

	private JPanel pnlBottom = new JPanel();
	private JCheckBox chkRunAuto = new JCheckBox("Auto Run FPGA");
	private JButton btnRunFpga = new JButton("Run FPGA manually");
	private JCheckBox chk32bit = new JCheckBox("32-bit");
	private JButton btnSettings = new JButton("Settings");
	private JButton btnExit = new JButton("Exit");

	// Path to the SOF file
	public static String sofPath;
	// Path to the quartus_pgm file
	public static String qpfPath;

	private SettingsDialog settings;

	SerialPort serialPort;

	public MainFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			this.ini = new IniFile("raspbootin.ini");
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Problem with opening ini file: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		setTitle("FPGA Raspbootin client");
		WindowUtil.setLocation(ini.getInt("main", "x", 100), ini.getInt("main", "y", 100), ini.getInt("main", "w", 800), ini.getInt("main", "h", 600), this);
		chk32bit.setSelected(ini.getInt("main", "32-bit", 1) == 1);
		chkRunAuto.setSelected(ini.getInt("main", "autorunfpga", 0) == 1);

		serialPort = new SerialPort(ini.getString("serial", "port", "COM5"));
		
		sofPath = ini.getString("sof", "path", "c:\\Prj\\Altera\\computer32.2\\computer.sof");
		qpfPath = ini.getString("qpf", "path", "C:\\altera\\13.0\\quartus\\bin\\quartus_pgm.exe");


		settings = new SettingsDialog(this);
		WindowUtil.setLocation(ini.getInt("settings", "x", 100), ini.getInt("settings", "y", 100), ini.getInt("settings", "w", 800), ini.getInt("settings", "h", 600), settings);
				
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("Closed");
				ini.setInt("main", "x", getLocation().x);
				ini.setInt("main", "y", getLocation().y);
				ini.setInt("main", "h", getSize().height);
				ini.setInt("main", "w", getSize().width);
				ini.setInt("main", "32-bit", chk32bit.isSelected()?1:0);
				ini.setInt("main", "autorunfpga", chkRunAuto.isSelected()?1:0);

				ini.setInt("settings", "x", settings.getLocation().x);
				ini.setInt("settings", "y", settings.getLocation().y);
				ini.setInt("settings", "h", settings.getSize().height);
				ini.setInt("settings", "w", settings.getSize().width);

				ini.saveIni();

				try {
					if (serialPort.isOpened()) {
						serialPort.closePort();
					}
				} catch (SerialPortException e1) {
					e1.printStackTrace();
				}
			}
		});
		if (SerialPortList.getPortNames().length == 0 ) {
			JOptionPane.showMessageDialog(this, "No serial ports detected", "Problem", JOptionPane.WARNING_MESSAGE);
			System.exit(1);
		}
		setUpLayout();
		setVisible(true);
	}

	private void setUpLayout() {
		GridBagLayout gbl = new GridBagLayout();
		pnlCenter.setLayout(gbl);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		pnlCenter.add(lImgFile, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		pnlCenter.add(tfImgFile, gbc);
		tfImgFile.setText(ini.getString("image", "fileName", ""));

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		pnlCenter.add(btnBrowse, gbc);
		btnBrowse.addActionListener(e -> browseForImgFile());

		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 0;
		pnlCenter.add(btnLoad, gbc);
		btnLoad.addActionListener(e -> loadImage());

		gbc = new GridBagConstraints();
		gbc.gridx = 4;
		gbc.gridy = 0;
		pnlCenter.add(btnOpenSerial, gbc);
		btnOpenSerial.addActionListener(e -> openSerialTerminal());

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 5;
		gbc.weighty = 1;
		pnlCenter.add(sc, gbc);
		taIo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				e.consume();
				if (e.getKeyChar() == '\n') {
					return;
				}
				try {
					if (serialPort != null && serialPort.isOpened()) {
						serialPort.writeByte((byte) e.getKeyChar());
					}
				} catch (SerialPortException e1) {
					e1.printStackTrace();
				}
			}
		});

		getContentPane().add(pnlCenter, BorderLayout.CENTER);

		pnlBottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(chkRunAuto);
		pnlBottom.add(btnRunFpga);
		pnlBottom.add(chk32bit);
		pnlBottom.add(btnSettings);
		pnlBottom.add(btnExit);
		btnRunFpga.addActionListener(e -> runFpga());
		btnSettings.addActionListener(e -> settings.setVisible(true));
		btnExit.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

		getContentPane().add(pnlBottom, BorderLayout.SOUTH);
	}

	public static void runFpga() {
		Process process;
		try {
			process = new ProcessBuilder(qpfPath,
					"-c", "usb-blaster",
					"-m", "jtag",
					"-o", "P;" + sofPath).start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			while ((line = br.readLine()) != null) {
			  System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initSerialCommunication() throws SerialPortException {
		if (!this.serialPort.isOpened()) {
			this.serialPort.openPort();
			this.serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		}
		serialPort.addEventListener(new SerialPortEventListener() {
			@Override
			public void serialEvent(SerialPortEvent e) {
				if (e.isRXCHAR() && e.getEventValue() > 0) {
					try {
						String receivedData = serialPort.readString(e.getEventValue());
						taIo.append(receivedData);
						taIo.setCaretPosition(taIo.getText().length());
						byte[] buff = receivedData.getBytes();
						for (int i = 0; i < buff.length; i++) {
							System.out.printf("%02x ", buff[i]);
						}
					} catch (SerialPortException ex) {
						ex.printStackTrace();
					}
				}
			}
		}, SerialPort.MASK_RXCHAR);
	}

	private void openSerialTerminal() {
		if (!running) {
			running = true;
			btnOpenSerial.setText("Close terminal");
			btnLoad.setEnabled(false);
			taIo.requestFocus();
			try {
				if (!this.serialPort.isOpened()) {
					initSerialCommunication();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			running = false;
			btnOpenSerial.setText("Open terminal");
			btnLoad.setEnabled(true);
			try {
				if (serialPort.isOpened())
					serialPort.closePort();
			} catch (SerialPortException e) {
				e.printStackTrace();
			}
		}
	}

	boolean running = false;
	SwingWorker<String, String> worker;

	private void loadImage() {
		if (!running) {
			running = true;
			btnLoad.setText("Stop");
			btnOpenSerial.setEnabled(false);
			taIo.requestFocus();
			worker = new SwingWorker<String, String>() {
				@Override
				protected String doInBackground() throws Exception {
					try {
						File imgFile = new File(tfImgFile.getText());
						if (imgFile.exists() && imgFile.isFile()) {
							publish("\nPower on your FPGA...\n");
							Raspbootin64Client.connectAndSend(MainFrame.this.serialPort, tfImgFile.getText(),
									toPrint -> publish(toPrint), chk32bit.isSelected(), chkRunAuto.isSelected());
							initSerialCommunication();
						} else {
							JOptionPane.showMessageDialog(MainFrame.this,
									imgFile.getCanonicalPath() + " does not exist, or is a folder!", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return "Done";
				}

				@Override
				protected void process(java.util.List<String> chunks) {
					taIo.append(chunks.get(0));
					taIo.setCaretPosition(taIo.getText().length());
					System.out.println(chunks.get(0));
				}

				/**
				 * Poziva se kada se zavrsi izvrsenje (milom ili silom).
				 */
				@Override
				protected void done() {
					System.out.println("WORKER DONE");
				}
			};
			worker.execute();
		} else {
			synchronized(MainFrame.this) {
				MainFrame.this.notify();
			}
			worker.cancel(true);
			running = false;
			btnLoad.setText("Load&Execute");
			btnOpenSerial.setEnabled(true);
			try {
				if (serialPort.isOpened())
					serialPort.closePort();
			} catch (SerialPortException e) {
				e.printStackTrace();
			}
		}

	}

	private void browseForImgFile() {
		try {
			JFileChooser fc = new JFileChooser(ini.getString("image", "fileName", "."));
			fc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) return true;
					if (f.getName().endsWith(".bin"))
						return true;
					return false;
				}
				@Override
				public String getDescription() {
					return "Binary executables";
				}
			});
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File chosen = fc.getSelectedFile();
				tfImgFile.setText(chosen.getCanonicalPath());
				ini.setString("image", "fileName", chosen.getCanonicalPath());
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Problem with image file");
		}
	}

	public static void main(String[] args) {
		new MainFrame();
	}
}
