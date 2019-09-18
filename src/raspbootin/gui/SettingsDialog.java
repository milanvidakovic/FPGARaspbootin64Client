package raspbootin.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import jssc.SerialPort;
import jssc.SerialPortList;
import raspbootin.util.IniFile;

public class SettingsDialog extends JDialog {

	private static final long serialVersionUID = -8308516394161893346L;

	private JPanel pnlSettings = new JPanel(); 
	private JLabel lComPort = new JLabel("Serial port:");
	private JComboBox<String> cbPorts = new JComboBox<String>();

	JLabel lblQpfPath = new JLabel("quartus_pgm path:");
	JTextField tfQpfPath = new JTextField("");
	JButton btnQpfPath = new JButton("Browse");

	JLabel lblSofPath = new JLabel("SOF file path:");
	JTextField tfSofPath = new JTextField("");
	JButton btnSofPath = new JButton("Browse");

	private String sofPath;
	private String qpfPath;
	
	private JPanel pnlBottom = new JPanel(); 
	private JButton btnOk = new JButton("Ok");
	private JButton btnCancel = new JButton("Cancel");
	
	public SettingsDialog(MainFrame parent) {
		super(parent, true);
		setSize(400, 300);
		
		GridBagLayout gbl = new GridBagLayout();
		pnlSettings.setLayout(gbl);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		pnlSettings.add(lComPort, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		pnlSettings.add(cbPorts, gbc);
		getContentPane().add(pnlSettings, BorderLayout.NORTH);
		
		fillPortsCombo(parent.ini);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		pnlSettings.add(lblQpfPath, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		// gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.weightx = 1;
		qpfPath = parent.ini.getString("qpf", "path", "C:\\altera\\13.0\\quartus\\bin\\quartus_pgm.exe");
		tfQpfPath.setText(qpfPath);
		pnlSettings.add(tfQpfPath, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		pnlSettings.add(btnQpfPath, gbc);
		btnQpfPath.addActionListener(e -> findQpfFile());

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		pnlSettings.add(lblSofPath, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		// gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.weightx = 1;
		sofPath = parent.ini.getString("sof", "path", "c:\\Prj\\Altera\\computer32.2\\computer.sof");
		tfSofPath.setText(sofPath);
		pnlSettings.add(tfSofPath, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 2;
		pnlSettings.add(btnSofPath, gbc);
		btnSofPath.addActionListener(e -> findSofFile());
		
		pnlBottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(btnOk);
		pnlBottom.add(btnCancel);
		
		getContentPane().add(pnlBottom, BorderLayout.SOUTH);
		
		btnOk.addActionListener( e -> saveToIni(parent));
		btnCancel.addActionListener( e -> this.setVisible(false));
	}

	private void findSofFile() {
		String ret = browseForImgFile(sofPath, ".sof", "SOF file"); 
		if (ret != null) {
			sofPath = ret;
			tfSofPath.setText(sofPath);
		}
	}

	private void findQpfFile() {
		String ret = browseForImgFile(qpfPath, ".exe", "Quartus quarts_pgm exetutable file"); 
		if (ret != null) {
			qpfPath = ret;
			tfQpfPath.setText(qpfPath);
		}
	}

	private void fillPortsCombo(IniFile ini) {
		String[] portNames = SerialPortList.getPortNames();
		for (String port : portNames) {
			cbPorts.addItem(port);
		}
		cbPorts.setSelectedItem(ini.getString("serial", "port", "COM5"));
	}

	private void saveToIni(MainFrame parent) {
		MainFrame.qpfPath = qpfPath;
		MainFrame.sofPath = sofPath;

		parent.ini.setString("qpf", "path", qpfPath);
		parent.ini.setString("sof", "path", sofPath);

		parent.ini.setString("serial", "port", cbPorts.getSelectedItem().toString());
		parent.serialPort = new SerialPort(cbPorts.getSelectedItem().toString());
		parent.ini.setInt("settings", "x", getLocation().x);
		parent.ini.setInt("settings", "y", getLocation().y);
		parent.ini.setInt("settings", "h", getSize().height);
		parent.ini.setInt("settings", "w", getSize().width);

		parent.ini.saveIni();
		setVisible(false);
	}

	private String browseForImgFile(String filePath, String filter, String desc) {
		try {
			JFileChooser fc = new JFileChooser(filePath);
			fc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) return true;
					if (f.getName().endsWith(filter))
						return true;
					return false;
				}
				@Override
				public String getDescription() {
					return desc;
				}
			});
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setSelectedFile(new File(filePath));
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File chosen = fc.getSelectedFile();
				return chosen.getCanonicalPath();
			} else return null;
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Problem with file");
			return null;
		}
	}
}
