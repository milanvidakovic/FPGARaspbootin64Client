package raspbootin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortException;
import raspbootin.gui.MainFrame;

public class Raspbootin64Client {

	public static SerialPort connectAndSend(SerialPort serialPort, String fileName, Consumer<String> print,
			boolean is32bit, boolean startFpga, boolean fast) throws Exception {
		try {
			long startAddr;
			if (is32bit) {
				startAddr = 0xB000L;
			} else {
				startAddr = 1024L;
			}

			if (!serialPort.isOpened()) {
				// Open serial port
				serialPort.openPort();
				// Set params. Also you can set params by this string:
				// serialPort.setParams(9600, 8, 1, 0);
				serialPort.setParams(fast?500000:SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} else {
				serialPort.removeEventListener();
			}
			print.accept("Connecting to the Raspbootin...");
			if (startFpga) {
				MainFrame.runFpga();
			}
			while (true) {
				print.accept("Waiting bytes from FPGA...");
				// Read 8+3 bytes from the serial port (name + load_ready)
				byte[] buffer;
				String s = "";
				do {
					buffer = serialPort.readBytes(1);
					if (buffer[0] != '\r' && buffer[0] != '\n' && buffer[0] != 3) {
						s += new String(buffer);
					}
				} while (buffer[0] != 3);

				Thread.sleep(100);

				buffer = new byte[2];
				// get two more 0x03 bytes
				buffer = serialPort.readBytes(2);
				System.out.println(Arrays.toString(buffer));
				print.accept("\nRaspbootin ready: [" + s + "]");

				Thread.sleep(100);

				if (buffer[0] == 3 && buffer[1] == 3) {
					// Send boot program (kernel) to the FPGA computer
					print.accept("\nReady to send kernel file " + fileName);
					// ready to send file
					File f = new File(fileName);

					if (f.exists() && f.isFile()) {

						long size = f.length();
						size -= startAddr;

						long sizeToSend = size;

						Thread.sleep(100);
						print.accept("\nReady to send " + size + " bytes.");

						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);

						// Read 2 bytes from the serial port (file size acknowledge or error)
						buffer = serialPort.readBytes(2);

						Thread.sleep(1000);

						int returnSize = calcSize(buffer);
						print.accept("\nGot the response (returned size) from Raspbootin64: " + returnSize);

//					if (buffer[0] == 'O' && buffer[1] == 'K') {
						if (returnSize == (sizeToSend & 0xFFFF)) {
							// File length OK, proceed with upload.
							FileInputStream in = new FileInputStream(f);
							buffer = new byte[1024];
							int read, total = 0;
//						for (int i = 0; i < startAddr; i++) {
//							in.read();
//						}
							in.skip(startAddr);
							short sum = 0;
							byte b;
							while ((read = in.read(buffer)) != -1) {
								System.out.print("#");
								for (int i = 0; i < read; i++) {
									b = buffer[i];
									serialPort.writeByte(b);
									// Thread.sleep(1);
									sum += b < 0 ? 256 + b : b;
								}
								total += read;
							}
							in.close();
							System.out.println();

							Thread.sleep(100);

							print.accept("\nSent " + total + " bytes.\n");
							Thread.sleep(100);
							print.accept("Calculated checksum: " + (sum < 0 ? 65536 + sum : sum) + "\n");
							Thread.sleep(100);
							int bytesLeft = serialPort.getInputBufferBytesCount();
							System.out.println("Bytes left: " + bytesLeft);
							if (bytesLeft >= 2) {
								buffer = serialPort.readBytes(2); // read two bytes which have checksum
								for (int i = 0; i < buffer.length; i++) {
									System.out.printf("%02x ", buffer[i]);
								}
								int checksum = calcSize(buffer);
								int isum = (sum > 0) ? sum : 65536 + sum;
								if (checksum == isum) {
									print.accept(
											"OK, received checksum is the same as the calculated checksum: " + isum);
								} else {
									print.accept("ERROR, received checksum WRONG: " + checksum
											+ ". Calculated checksum: " + isum);
								}
							} else {
								print.accept(
										"\nERROR, did not receive file checksum from FPGA, or wrong number of bytes received.");
								buffer = serialPort.readBytes(bytesLeft);
								for (int i = 0; i < buffer.length; i++) {
									System.out.printf("%02x ", buffer[i]);
								}
							}
						} else {
							Thread.sleep(100);
							print.accept("\nERROR, file size error reported by the FPGA: [" + returnSize + "]");
						}
					} else {
						Thread.sleep(100);
						print.accept("\nERROR, File " + fileName + " does not exist, or is a folder!");
					}
				} else if(buffer[0] == 'f' ) {
					print.accept("\nFILE MANAGEMENT COMMAND");
					System.out.println("FILE COMMAND: " + buffer[1]);
					// file management
					switch (buffer[1])  {
					case 'r':
						// READ FILE COMMAND
						StringBuilder sb = new StringBuilder();
						while(true) {
							buffer = serialPort.readBytes(1);
							System.out.printf("%d, %02X, %c ", buffer[0], buffer[0], buffer[0]);
							if (buffer[0] == 0)
								break;
							sb.append((char)buffer[0]);
						}
						String str = sb.toString();
						System.out.println("\nREAD FILE: '" + str + "'");
						File currFolder = new File(fileName).getParentFile().getParentFile();
						System.out.println(currFolder.getCanonicalPath());
						File file = new File(currFolder, str);
						System.out.println(file.getCanonicalPath());
						if (file.exists() && file.isFile()) {
							System.out.println("FILE EXISTS, size: " + file.length());
							serialPort.writeByte((byte) 1);  // OK
							
							Thread.sleep(100);
							
							long size = file.length();
							serialPort.writeByte((byte) (size & 255));
							Thread.sleep(100);
							size >>= 8;
							serialPort.writeByte((byte) (size & 255));
							Thread.sleep(100);
							size >>= 8;
							serialPort.writeByte((byte) (size & 255));
							Thread.sleep(100);
							size >>= 8;
							serialPort.writeByte((byte) (size & 255));
							
							Thread.sleep(100);
							
							FileInputStream fin = new FileInputStream(file);
							buffer = new byte[1024];
							int read;
							while ((read = fin.read(buffer)) != -1) {
								System.out.print("#");
								for (int k = 0; k < read; k++) {
									serialPort.writeByte(buffer[k]);
//									Thread.sleep(10000);
//									System.out.print(".");
								}
							}
							fin.close();
							System.out.println();
						} else {
							// Error with file to be read
							serialPort.writeByte((byte) 10);  // ERROR
						}
						break;
					case 'w':
						// write file
						Thread.sleep(100);
						print.accept("\nWRITE FILE");
						// file name
						sb = new StringBuilder();
						do {
							buffer = serialPort.readBytes(1);
							//System.out.print(buffer[0]);
							if (buffer[0] != 0)
								sb.append((char)buffer[0]);
						} while (buffer[0] != 0);
						String name = sb.toString();
						
						// file size
						buffer = serialPort.readBytes(1);
						int B1 = buffer[0] < 0 ? 256 + buffer[0] : buffer[0];
						System.out.println(B1);
						int size = B1;
						buffer = serialPort.readBytes(1);
						B1 = buffer[0] < 0 ? 256 + buffer[0] : buffer[0];
						System.out.println(B1);
						size += (B1 << 8);
						buffer = serialPort.readBytes(1);
						B1 = buffer[0] < 0 ? 256 + buffer[0] : buffer[0];
						System.out.println(B1);
						size += (B1 << 16);
						buffer = serialPort.readBytes(1);
						B1 = buffer[0] < 0 ? 256 + buffer[0] : buffer[0];
						System.out.println(B1);
						size += (B1 << 24);
						System.out.printf("File name: %s, file size: %d\n", name, size);
						
						currFolder = new File(fileName).getParentFile().getParentFile();
						System.out.println(currFolder.getCanonicalPath());
						file = new File(currFolder, name);
						FileOutputStream fout = new FileOutputStream(file);
						for (int i = 0; i < size; i++) {
							buffer = serialPort.readBytes(1);
							fout.write(buffer[0]);
							//System.out.println((int)buffer[0]);
							if (i % 1024 == 0)
								System.out.print("#");

						}
						fout.close();
						System.out.println();
						break;
					case 'd':
						// list files
						Thread.sleep(100);
						print.accept("\nLIST FILES");
						File currFile = new File(fileName);
						File dir = currFile.getParentFile().getParentFile();
						File[] files = dir.listFiles();
						sb = new StringBuilder();
						for (File f : files) {
							if (f.isFile()) {
								sb.append(f.getName());
								sb.append("\n");
							}
						}
						str = sb.toString();
						System.out.println(String.format("%04X", str.length()));
						size = str.length();
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						
						Thread.sleep(100);
						
						for (int i = 0; i < str.length(); i++) {
							char c = str.charAt(i);
							serialPort.writeByte((byte) c);
							if (i % 1024 == 0)
								System.out.print("#");
						}
						System.out.println();
						break;
					case 'f':
						// list folders
						Thread.sleep(100);
						print.accept("\nLIST FOLDERS");
						file = new File(fileName);
						dir = file.getParentFile().getParentFile();
						files = dir.listFiles();
						sb = new StringBuilder();
						for (File f : files) {
							if (f.isDirectory()) {
								sb.append("<" + f.getName() + ">");
								sb.append("\n");
							}
						}
						str = sb.toString();
						System.out.println(String.format("%04X", str.length()));
						size = str.length();
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						Thread.sleep(100);
						size >>= 8;
						serialPort.writeByte((byte) (size & 255));
						
						Thread.sleep(100);
						
						for (int i = 0; i < str.length(); i++) {
							char c = str.charAt(i);
							serialPort.writeByte((byte) c);
							if (i % 1024 == 0)
								System.out.print("#");
						}
						System.out.println();
						break;
					} 
				} else {
					Thread.sleep(100);
					print.accept("\nERROR, got wrong bytes from FPGA Raspbootin: [" + new String(buffer) + "]");
				}
			}
		} catch (SerialPortException ex) {
			System.out.println(ex);
		}
		return serialPort;
	}

	private static int calcSize(byte[] buffer) {
		byte b1 = buffer[0];
		byte b2 = buffer[1];
		int B1 = b1 < 0 ? 256 + b1 : b1;
		int B2 = b2 < 0 ? 256 + b2 : b2;
		return B1 + B2 * 256;
	}

	public static void main(String[] args) {
		try {
			if (args.length == 2) {
				Raspbootin64Client.connectAndSend(new SerialPort(args[0]), args[1],
						toPrint -> System.out.print(toPrint), true, false, true).closePort();
			} else if (args.length == 0 || args.length > 3) {
				System.err.println("Usage: java FPGARaspbootin64Client <com_port> <file_path> 32-bit|16-bit [gui]");
				System.err.println("Example: java FPGARaspbootin64Client COM3 C:\\Temp\\kernel8.img 32");
			} else if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
				new MainFrame();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
